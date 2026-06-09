package com.minesis.voice.vosk;

import com.minesis.network.ClipAnnotationPacket;
import com.minesis.network.TranscriptionPacket;
import com.minesis.network.VoiceNetworking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Manages the Vosk ASR lifecycle on the client.
 *
 * Both the French and English models are loaded simultaneously so the mod
 * works regardless of which language players speak during a session.
 * Each recognizer fires independently when it detects end-of-utterance;
 * the server-side cooldown in TranscriptionPacket de-duplicates rapid-fire
 * results coming from both models for the same spoken phrase.
 *
 * Memory footprint: ~94 MB (FR) + ~40 MB (EN) = ~134 MB on disk;
 * RAM usage depends on the Vosk native layer (both models are small).
 */
@OnlyIn(Dist.CLIENT)
public class VoskManager {

    private static final Logger LOGGER = LogManager.getLogger("Minesis/Vosk");
    private static final float SAMPLE_RATE = 16000.0f;

    // French recognizer
    private static volatile Model      frModel;
    private static volatile Recognizer frRecognizer;
    private static volatile boolean    frReady = false;

    // English recognizer
    private static volatile Model      enModel;
    private static volatile Recognizer enRecognizer;
    private static volatile boolean    enReady = false;

    private static volatile boolean    anyReady = false;
    private static volatile boolean    failed   = false;
    private static MicTranscriber      micTranscriber;

    // ─── Init ─────────────────────────────────────────────────────────────

    public static CompletableFuture<Void> initialize(File gameDir) {
        CompletableFuture<Void> frFuture = CompletableFuture.runAsync(() -> loadModel(gameDir, VoskModelManager.Language.FRENCH));
        CompletableFuture<Void> enFuture = CompletableFuture.runAsync(() -> loadModel(gameDir, VoskModelManager.Language.ENGLISH));

        // Start the microphone once at least one model is ready
        return CompletableFuture.allOf(frFuture, enFuture).whenComplete((v, ex) -> {
            if (!frReady && !enReady) {
                LOGGER.error("[Minesis Vosk] Both models failed — transcription disabled.");
                failed = true;
                return;
            }
            anyReady = true;
            if (frReady) LOGGER.info("[Minesis Vosk] French recognizer active.");
            if (enReady) LOGGER.info("[Minesis Vosk] English recognizer active.");

            micTranscriber = new MicTranscriber(SAMPLE_RATE, VoskManager::onAudio);
            micTranscriber.start();
        });
    }

    private static void loadModel(File gameDir, VoskModelManager.Language lang) {
        String label = lang == VoskModelManager.Language.ENGLISH ? "English" : "French";
        try {
            String modelPath = VoskModelManager.ensureModel(gameDir, lang);
            if (modelPath == null) {
                LOGGER.warn("[Minesis Vosk] {} model setup failed — this language will be skipped.", label);
                return;
            }
            LOGGER.info("[Minesis Vosk] Loading {} model...", label);
            Model m = new Model(modelPath);
            Recognizer r = new Recognizer(m, SAMPLE_RATE);
            r.setMaxAlternatives(0);
            r.setWords(false);
            if (lang == VoskModelManager.Language.FRENCH) {
                frModel = m; frRecognizer = r; frReady = true;
            } else {
                enModel = m; enRecognizer = r; enReady = true;
            }
            LOGGER.info("[Minesis Vosk] {} model ready at {} Hz.", label, (int) SAMPLE_RATE);
        } catch (Exception e) {
            LOGGER.error("[Minesis Vosk] Failed to load {} model", label, e);
        }
    }

    public static boolean isReady()   { return anyReady; }
    public static boolean hasFailed() { return failed;   }

    // ─── Audio feed ───────────────────────────────────────────────────────

    private static synchronized void onAudio(byte[] pcmBytes) {
        if (!anyReady) return;
        feedRecognizer(frRecognizer, frReady, pcmBytes);
        feedRecognizer(enRecognizer, enReady, pcmBytes);
    }

    private static void feedRecognizer(Recognizer rec, boolean ready, byte[] pcmBytes) {
        if (!ready || rec == null) return;
        try {
            if (rec.acceptWaveForm(pcmBytes, pcmBytes.length)) {
                handleResult(rec.getResult());
            }
        } catch (Exception e) {
            LOGGER.warn("[Minesis Vosk] Recognition error: {}", e.getMessage());
        }
    }

    private static void handleResult(String json) {
        if (json == null || json.isBlank()) return;
        try {
            JsonObject obj  = JsonParser.parseString(json).getAsJsonObject();
            String     text = obj.has("text") ? obj.get("text").getAsString().trim() : "";
            if (text.isEmpty()) return;
            LOGGER.debug("[Minesis Vosk] Recognized: \"{}\"", text);

            VoiceNetworking.INSTANCE.sendToServer(new ClipAnnotationPacket(text));
            sendToServer(text);
        } catch (Exception e) {
            LOGGER.warn("[Minesis Vosk] Could not parse result JSON: {}", json);
        }
    }

    // ─── Packet dispatch ───────────────────────────────────────────────────

    private static void sendToServer(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        boolean minesisNearby = !mc.level.getEntitiesOfClass(
                com.minesis.entity.MinesisEntity.class,
                mc.player.getBoundingBox().inflate(20.0)
        ).isEmpty();
        if (!minesisNearby) return;

        VoiceNetworking.INSTANCE.sendToServer(new TranscriptionPacket(text));
    }

    // ─── Shutdown ─────────────────────────────────────────────────────────

    public static void shutdown() {
        anyReady = false; frReady = false; enReady = false;
        if (micTranscriber != null) { micTranscriber.stopCapture(); micTranscriber = null; }
        if (frRecognizer   != null) { frRecognizer.close();         frRecognizer   = null; }
        if (enRecognizer   != null) { enRecognizer.close();         enRecognizer   = null; }
        if (frModel        != null) { frModel.close();              frModel        = null; }
        if (enModel        != null) { enModel.close();              enModel        = null; }
    }
}
