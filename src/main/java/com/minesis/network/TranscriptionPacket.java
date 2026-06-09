package com.minesis.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.minesis.entity.MinesisEntity;
import com.minesis.voice.FrenchTextAnalyzer;
import com.minesis.voice.MinesisVoiceChatPlugin;
import com.minesis.voice.VoiceContext;
import com.minesis.voice.VoicePlaybackService;
import com.minesis.voice.VoiceStorage;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Sent by the client when Vosk recognises speech while a MinesisEntity is nearby.
 *
 * Intelligence rules:
 *  1. Only respond to INTERACTIVE speech (greetings, questions, commands).
 *     Pure first-person statements ("je suis fatigué") and third-person ambient
 *     phrases ("il fait beau") are ignored.
 *  2. Per-entity cooldown of 8 seconds prevents rapid-fire responses.
 *  3. Clip selection uses transcript similarity first, context fallback second.
 *  4. After responding, the entity's autonomous playback cooldown is reset so a
 *     random clip doesn't immediately follow the targeted response.
 */
public class TranscriptionPacket {

    private final String text;

    public TranscriptionPacket(String text) {
        this.text = text;
    }

    // ─── Codec ────────────────────────────────────────────────────────────

    public static void encode(TranscriptionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.text, 512);
    }

    public static TranscriptionPacket decode(FriendlyByteBuf buf) {
        return new TranscriptionPacket(buf.readUtf(512));
    }

    // ─── Server handler ───────────────────────────────────────────────────

    /** Milliseconds the entity must stay silent between triggered responses. */
    private static final long RESPONSE_COOLDOWN_MS = 4_000L;
    private static final Map<UUID, Long> lastResponseTime = new ConcurrentHashMap<>();

    public static void handle(TranscriptionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            ServerLevel level = sender.serverLevel();

            // 2. Find closest MinesisEntity within 20 blocks
            MinesisEntity entity = level.getEntitiesOfClass(
                    MinesisEntity.class,
                    sender.getBoundingBox().inflate(20.0)
            ).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(sender)))
                    .orElse(null);
            if (entity == null) return;

            // 3. Per-entity response cooldown
            long now = System.currentTimeMillis();
            Long last = lastResponseTime.get(entity.getUUID());
            if (last != null && now - last < RESPONSE_COOLDOWN_MS) return;

            // 4. Resolve voice source
            UUID targetUUID = entity.getTargetPlayerUUID();
            if (targetUUID == null) targetUUID = entity.getAppearancePlayerUUID();
            if (targetUUID == null) return;
            if (!VoiceStorage.hasVoiceClips(targetUUID)) return;

            // 5. Playback — "tu fais quoi?" uses entity's current activity + first-person clip selection
            VoicechatServerApi api = MinesisVoiceChatPlugin.getVoicechatApi();
            boolean played;
            if (isSelfDescriptionQuery(msg.text)) {
                VoiceContext entityContext = entity.computePlaybackContext();
                played = VoicePlaybackService.playVoiceClipForSelfDescription(entity, targetUUID, api, entityContext);
            } else {
                VoiceContext context = textToContext(msg.text);
                played = VoicePlaybackService.playVoiceClipForQuery(entity, targetUUID, api, context, msg.text);
            }

            // 6. Post-response bookkeeping — only if a clip actually started
            if (played) {
                lastResponseTime.put(entity.getUUID(), now);
                entity.notifyResponseTriggered();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // ─── Intent classification ────────────────────────────────────────────

    enum SpeechIntent { INTERACTIVE, AMBIENT }

    // Stems of words that signal a question directed at someone
    private static final Set<String> QUESTION_STEMS = new HashSet<>(Arrays.asList(
        "quoi","qui","où","quand","comment","pourquoi","combien","lequel","laquelle",
        "quel","quelle","est-ce","qu","comment"
    ));
    // Synonym-group keys that indicate an action command
    // (all members of their synonym group count as commands too)
    private static final Set<String> COMMAND_GROUP_KEYS = new HashSet<>(Arrays.asList(
        "cour","march","min","attaqu","nag","fabriqu","constru","explo","chas","tir"
    ));

    static SpeechIntent classifyIntent(String text) {
        if (text == null || text.isBlank()) return SpeechIntent.AMBIENT;

        String low = text.toLowerCase(Locale.ROOT).trim();

        // Hard signal: question mark → always interactive
        if (low.contains("?")) return SpeechIntent.INTERACTIVE;

        // Get stems via the real analyzer
        List<String> stems = FrenchTextAnalyzer.tokenize(text);

        // Check for question stems or command stems in the tokenized form
        boolean hasQuestion = stems.stream().anyMatch(QUESTION_STEMS::contains)
                || containsAny(low, new String[]{"est-ce","qu'est","c'est quoi"});
        boolean hasCommand  = stems.stream().anyMatch(s -> {
            if (COMMAND_GROUP_KEYS.contains(s)) return true;
            // check if this stem is in any command synonym group
            Set<String> group = FrenchTextAnalyzer.synonymGroup(s);
            if (group == null) return false;
            return group.stream().anyMatch(COMMAND_GROUP_KEYS::contains);
        });
        boolean hasGreeting = stems.stream().anyMatch(s -> {
            // "bonjour","salut","coucou","hey" all stem to themselves or their group
            Set<String> group = FrenchTextAnalyzer.synonymGroup(s);
            return group != null && (group.contains("bonjour") || group.contains("salut")
                    || group.contains("aurevoir") || group.contains("merci"));
        });

        if (hasQuestion || hasCommand || hasGreeting) return SpeechIntent.INTERACTIVE;

        // ── Ambient detection ──────────────────────────────────────────────
        String[] rawWords = low.replaceAll("[''']"," ").split("\\s+");
        if (rawWords.length == 0) return SpeechIntent.AMBIENT;
        String first = rawWords[0];

        // First-person statements: "je …" / "j' …"  with no interactive signal above
        if (first.equals("je") || first.equals("j")) return SpeechIntent.AMBIENT;

        // Impersonal / third-person statements: "il …", "elle …", "ça …", "c'est …"
        if (first.equals("il") || first.equals("elle")
                || first.equals("ça") || first.equals("c") // c'est after split
                || first.equals("ya") || first.equals("y")) {
            return SpeechIntent.AMBIENT;
        }

        // Very short utterance with a known stem → interactive
        if (stems.size() <= 2) return SpeechIntent.INTERACTIVE;

        return SpeechIntent.INTERACTIVE;
    }

    private static boolean containsAny(String text, String[] tokens) {
        for (String t : tokens) if (text.contains(t)) return true;
        return false;
    }

    // ─── Keyword → VoiceContext ───────────────────────────────────────────

    private static final Map<String, VoiceContext> KEYWORDS = new LinkedHashMap<>();
    static {
        // ── French ────────────────────────────────────────────────────────
        for (String w : new String[]{"bonjour","salut","coucou","bonsoir","allô","ça va","merci","au revoir","à plus"})
            KEYWORDS.put(w, VoiceContext.IDLE);
        for (String w : new String[]{"viens","allons","marche","avance"})
            KEYWORDS.put(w, VoiceContext.WALKING);
        for (String w : new String[]{"cours","fuite","dépêche"})
            KEYWORDS.put(w, VoiceContext.RUNNING);
        for (String w : new String[]{"mine","creuse","casse","pioche"})
            KEYWORDS.put(w, VoiceContext.MINING);
        for (String w : new String[]{"coupe","tronc","arbre","forêt","bûcheron","abat"})
            KEYWORDS.put(w, VoiceContext.WOODCUTTING);
        for (String w : new String[]{"craft","fabrique","artisanat"})
            KEYWORDS.put(w, VoiceContext.CRAFTING);
        for (String w : new String[]{"four","cuit","cuire","fondu","fumoir"})
            KEYWORDS.put(w, VoiceContext.SMELTING);
        for (String w : new String[]{"construit","bâtit","pose","installe","bâtiment"})
            KEYWORDS.put(w, VoiceContext.BUILDING);
        for (String w : new String[]{"cultive","récolte","plante","blé","carotte"})
            KEYWORDS.put(w, VoiceContext.FARMING);
        for (String w : new String[]{"attaque","combat","bats","tue","frappe"})
            KEYWORDS.put(w, VoiceContext.COMBAT);
        for (String w : new String[]{"nage","plonge","baigne"})
            KEYWORDS.put(w, VoiceContext.SWIMMING);
        for (String w : new String[]{"fuis","fuir","échappe","danger","monstre","squelette"})
            KEYWORDS.put(w, VoiceContext.FLEEING);
        for (String w : new String[]{"blessé","aïe","douleur","touché","souffre"})
            KEYWORDS.put(w, VoiceContext.HURT);

        // ── English ───────────────────────────────────────────────────────
        for (String w : new String[]{"hello","hi","hey","goodbye","bye","thanks","thank you","good morning","good evening"})
            KEYWORDS.put(w, VoiceContext.IDLE);
        for (String w : new String[]{"walk","come here","follow me","move","go"})
            KEYWORDS.put(w, VoiceContext.WALKING);
        for (String w : new String[]{"run","sprint","hurry","rush","fast"})
            KEYWORDS.put(w, VoiceContext.RUNNING);
        for (String w : new String[]{"mine","dig","break","pickaxe"})
            KEYWORDS.put(w, VoiceContext.MINING);
        for (String w : new String[]{"chop","cut","axe","tree","wood","lumber","log"})
            KEYWORDS.put(w, VoiceContext.WOODCUTTING);
        for (String w : new String[]{"craft","crafting","table","workbench","make","create"})
            KEYWORDS.put(w, VoiceContext.CRAFTING);
        for (String w : new String[]{"smelt","furnace","cook","bake","forge","smoker"})
            KEYWORDS.put(w, VoiceContext.SMELTING);
        for (String w : new String[]{"build","place","construct","wall","house"})
            KEYWORDS.put(w, VoiceContext.BUILDING);
        for (String w : new String[]{"farm","harvest","plant","crop","wheat","carrot","potato"})
            KEYWORDS.put(w, VoiceContext.FARMING);
        for (String w : new String[]{"attack","fight","kill","hit","combat","strike"})
            KEYWORDS.put(w, VoiceContext.COMBAT);
        for (String w : new String[]{"swim","dive","water","drown"})
            KEYWORDS.put(w, VoiceContext.SWIMMING);
        for (String w : new String[]{"run away","escape","flee","danger","zombie","skeleton","monster","creeper"})
            KEYWORDS.put(w, VoiceContext.FLEEING);
        for (String w : new String[]{"ouch","hurt","pain","injured","ow","damaged"})
            KEYWORDS.put(w, VoiceContext.HURT);
    }

    static VoiceContext textToContext(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, VoiceContext> e : KEYWORDS.entrySet())
            if (lower.contains(e.getKey())) return e.getValue();
        return VoiceContext.IDLE;
    }

    // ─── Self-description query detection ────────────────────────────────

    /**
     * Returns true when the player is asking what the entity is currently doing
     * ("tu fais quoi ?", "qu'est-ce que tu fais ?", etc.).
     * The entity will respond with a first-person clip matching its current activity.
     */
    static boolean isSelfDescriptionQuery(String text) {
        if (text == null || text.isBlank()) return false;
        String low = text.toLowerCase(Locale.ROOT)
                .replaceAll("[''`´]", " ")
                .replaceAll("[?!.,;]", " ")
                .replaceAll("\\s+", " ").trim();
        // French
        if (low.contains("tu fais quoi")
            || low.contains("tu fous quoi")
            || low.contains("que fais tu")
            || low.contains("qu est ce que tu fais")
            || low.contains("tu es en train de quoi")
            || low.contains("t es en train de quoi")
            || low.contains("c est quoi ce que tu fais")
            || low.contains("kesketufe")
            || low.contains("t fais quoi")) return true;
        // English
        return low.contains("what are you doing")
            || low.contains("what are you up to")
            || low.contains("what do you do")
            || low.contains("what are u doing")
            || low.contains("watcha doing")
            || low.contains("whatcha doing")
            || low.contains("wdyd")
            || low.contains("what r u doing");
    }
}
