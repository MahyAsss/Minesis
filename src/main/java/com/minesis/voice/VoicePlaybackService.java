package com.minesis.voice;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.minesis.entity.MinesisEntity;
import com.minesis.utils.AudioUtils;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

public class VoicePlaybackService {

    // Keyed by entity UUID — presence in map means "currently playing"
    private static final Map<UUID, AudioPlayer> activePlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, java.util.Deque<Integer>> clipHistoryByEntity = new ConcurrentHashMap<>();
    private static final int CLIP_HISTORY_SIZE = 3;

    public static void setVoicechatApi(VoicechatServerApi api) { /* kept for API compat */ }

    /** True if this entity already has a clip playing. */
    public static boolean isPlaying(UUID entityId) {
        return activePlayers.containsKey(entityId);
    }

    // ─── Public entry points ──────────────────────────────────────────────

    /** Autonomous tick-based playback — selects by activity context. */
    public static void playVoiceClip(MinesisEntity entity, UUID sourceUUID,
            VoicechatServerApi api, VoiceContext context) {
        if (!canPlay(entity, api)) return;

        java.util.Deque<Integer> history = historyFor(entity.getUUID());
        byte[] audio = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            byte[] candidate = VoiceStorage.getClipForContext(sourceUUID, context);
            if (candidate == null || candidate.length == 0) return;
            int h = java.util.Arrays.hashCode(candidate);
            if (!history.contains(h)) {
                history.addLast(h);
                if (history.size() > CLIP_HISTORY_SIZE) history.removeFirst();
                audio = candidate;
                break;
            }
        }
        if (audio == null) audio = VoiceStorage.getClipForContext(sourceUUID, context);
        if (audio == null || audio.length == 0) return;

        doPlay(entity, api, audio);
    }

    /** Triggered by a player's voice query — uses transcript-based selection.
     *  Returns true if a clip actually started playing. */
    public static boolean playVoiceClipForQuery(MinesisEntity entity, UUID sourceUUID,
            VoicechatServerApi api, VoiceContext context, String queryText) {
        if (!canPlay(entity, api)) return false;

        List<byte[]> candidates = VoiceStorage.getCandidatesForQuery(sourceUUID, context, queryText);
        if (candidates.isEmpty()) return false;

        java.util.Deque<Integer> history = historyFor(entity.getUUID());
        byte[] audio = null;
        for (byte[] candidate : candidates) {
            int h = java.util.Arrays.hashCode(candidate);
            if (!history.contains(h)) {
                history.addLast(h);
                if (history.size() > CLIP_HISTORY_SIZE) history.removeFirst();
                audio = candidate;
                break;
            }
        }
        if (audio == null) audio = candidates.get(0); // all already in history, play anyway

        doPlay(entity, api, audio);
        return true;
    }

    /** Triggered by "what are you doing?" — picks first-person clips matching entity's current activity.
     *  Returns true if a clip actually started playing. */
    public static boolean playVoiceClipForSelfDescription(MinesisEntity entity, UUID sourceUUID,
            VoicechatServerApi api, VoiceContext entityContext) {
        if (!canPlay(entity, api)) return false;

        List<byte[]> candidates = VoiceStorage.getCandidatesForSelfDescription(sourceUUID, entityContext);
        if (candidates.isEmpty()) return false;

        java.util.Deque<Integer> history = historyFor(entity.getUUID());
        byte[] audio = null;
        for (byte[] candidate : candidates) {
            int h = java.util.Arrays.hashCode(candidate);
            if (!history.contains(h)) {
                history.addLast(h);
                if (history.size() > CLIP_HISTORY_SIZE) history.removeFirst();
                audio = candidate;
                break;
            }
        }
        if (audio == null) audio = candidates.get(0);
        doPlay(entity, api, audio);
        return true;
    }

    // ─── Internal helpers ─────────────────────────────────────────────────

    private static boolean canPlay(MinesisEntity entity, VoicechatServerApi api) {
        if (api == null || entity == null || entity.level().isClientSide) return false;
        return !isPlaying(entity.getUUID()); // reject if already speaking
    }

    private static java.util.Deque<Integer> historyFor(UUID entityId) {
        return clipHistoryByEntity.computeIfAbsent(entityId, k -> new java.util.ArrayDeque<>());
    }

    private static void doPlay(MinesisEntity entity, VoicechatServerApi api, byte[] audioData) {
        try {
            UUID entityId = entity.getUUID();
            de.maxhenkel.voicechat.api.Entity vcEntity = api.fromEntity(entity);
            AudioChannel channel = api.createEntityAudioChannel(UUID.randomUUID(), vcEntity);
            if (channel == null) return;

            OpusEncoder encoder = api.createEncoder();
            if (encoder == null || encoder.isClosed()) return;

            short[] pcm = AudioUtils.bytesToShorts(audioData);
            if (entity.isHostileModeActive()) pcm = applyHostileVoiceEffect(pcm);

            AudioPlayer player = api.createAudioPlayer(channel, encoder, pcm);
            if (player == null) { encoder.close(); return; }

            activePlayers.put(entityId, player);
            player.setOnStopped(() -> {
                activePlayers.remove(entityId);
                encoder.close();
            });
            player.startPlaying();
        } catch (Exception e) {
            System.err.println("[Minesis] Error playing voice clip: " + e.getMessage());
        }
    }

    // ─── Misc ─────────────────────────────────────────────────────────────

    public static void stopAllPlayback() {
        for (AudioPlayer p : activePlayers.values()) {
            if (p != null && p.isPlaying()) p.stopPlaying();
        }
        activePlayers.clear();
    }

    public static int getActivePlayerCount() { return activePlayers.size(); }

    private static short[] applyHostileVoiceEffect(short[] pcm) {
        short[] out = new short[pcm.length];
        int prev = 0;
        int[] reverb = new int[2205];
        for (int i = 0; i < pcm.length; i++) {
            int s = pcm[i];
            int filtered = (int) (prev * 0.68 + s * 0.32);
            filtered = (int) (filtered * 0.84) + (reverb[i % reverb.length] / 3);
            filtered = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, filtered));
            if ((i & 63) == 0) filtered = (int) (filtered * 0.92);
            out[i] = (short) filtered;
            prev = filtered;
            reverb[i % reverb.length] = (int) (filtered * 0.45);
        }
        return out;
    }
}
