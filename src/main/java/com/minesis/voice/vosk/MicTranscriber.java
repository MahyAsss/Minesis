package com.minesis.voice.vosk;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.util.function.Consumer;

/**
 * Captures microphone audio at 16 kHz / 16-bit / mono in a daemon thread and
 * forwards raw PCM byte chunks to a consumer (VoskManager::onAudio).
 *
 * If 16 kHz is not supported by the hardware, falls back to 48 kHz and
 * downsamples with 3:1 decimation before forwarding.
 */
@OnlyIn(Dist.CLIENT)
public class MicTranscriber extends Thread {

    private static final Logger LOGGER = LogManager.getLogger("Minesis/Vosk");

    private static final AudioFormat FORMAT_16K =
            new AudioFormat(16000.0f, 16, 1, true, false);
    private static final AudioFormat FORMAT_48K =
            new AudioFormat(48000.0f, 16, 1, true, false);

    // 100 ms chunks at 16 kHz (16-bit = 2 bytes/sample)
    private static final int CHUNK_SAMPLES = 1600;
    private static final int CHUNK_BYTES   = CHUNK_SAMPLES * 2;

    private final Consumer<byte[]> consumer;
    private volatile boolean running = true;
    private TargetDataLine line;

    public MicTranscriber(float sampleRate, Consumer<byte[]> consumer) {
        super("Minesis-MicTranscriber");
        this.consumer = consumer;
        setDaemon(true);
    }

    @Override
    public void run() {
        AudioFormat format = FORMAT_16K;
        line = openLine(format);
        boolean needsDownsample = false;

        if (line == null) {
            LOGGER.warn("[Minesis Vosk] 16 kHz not supported — trying 48 kHz with decimation");
            format = FORMAT_48K;
            line = openLine(format);
            needsDownsample = true;
        }

        if (line == null) {
            LOGGER.error("[Minesis Vosk] No suitable microphone format found. Transcription disabled.");
            return;
        }

        line.start();
        LOGGER.info("[Minesis Vosk] Microphone capture started at {} Hz",
                (int) format.getSampleRate());

        // Buffer: if downsampling 48k→16k we need 3× the samples
        int captureBytes = needsDownsample ? CHUNK_BYTES * 3 : CHUNK_BYTES;
        byte[] buf = new byte[captureBytes];

        while (running) {
            int read = line.read(buf, 0, buf.length);
            if (read <= 0) continue;

            byte[] chunk = needsDownsample ? downsample3to1(buf, read) : trim(buf, read);
            if (chunk != null && chunk.length > 0) {
                consumer.accept(chunk);
            }
        }

        line.stop();
        line.close();
        LOGGER.info("[Minesis Vosk] Microphone capture stopped.");
    }

    public void stopCapture() {
        running = false;
        if (line != null) line.close();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static TargetDataLine openLine(AudioFormat format) {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) return null;
            TargetDataLine dl = (TargetDataLine) AudioSystem.getLine(info);
            dl.open(format, CHUNK_BYTES * 4);
            return dl;
        } catch (LineUnavailableException e) {
            LOGGER.warn("[Minesis Vosk] Could not open audio line at {} Hz: {}",
                    (int) format.getSampleRate(), e.getMessage());
            return null;
        }
    }

    /** Simple 3:1 decimation (48000 → 16000 Hz). Keeps every 3rd sample. */
    private static byte[] downsample3to1(byte[] src, int srcLen) {
        int outSamples = srcLen / 6; // 2 bytes/sample, keep 1 of 3
        byte[] out = new byte[outSamples * 2];
        int wi = 0;
        for (int i = 0; i < outSamples; i++) {
            int si = i * 6; // skip 2 samples (4 bytes) for every 1 kept
            if (si + 1 < srcLen) {
                out[wi++] = src[si];
                out[wi++] = src[si + 1];
            }
        }
        return out;
    }

    private static byte[] trim(byte[] src, int len) {
        if (len == src.length) return src;
        byte[] out = new byte[len];
        System.arraycopy(src, 0, out, 0, len);
        return out;
    }
}
