package com.minesis.voice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SmartAudioClipManager {

    private static final Map<UUID, AudioPhrase> activeRecordings = new ConcurrentHashMap<>();

    private static final int MIN_PHRASE_BYTES  = 96_000; // 1 second minimum
    private static final int MAX_PHRASE_BYTES  = VoiceStorage.MAX_CLIP_BYTES; // 10 seconds

    public static void startRecording(UUID playerUUID, String playerName, VoiceContext context) {
        activeRecordings.computeIfAbsent(playerUUID,
                k -> new AudioPhrase(playerName, context));
    }

    public static void addAudioChunk(UUID playerUUID, byte[] audioChunk) {
        if (audioChunk == null || audioChunk.length == 0) return;

        AudioPhrase phrase = activeRecordings.get(playerUUID);
        if (phrase == null) return;

        // Auto-finalize if we've hit 10 seconds to avoid unbounded memory growth
        if (phrase.getTotalSize() + audioChunk.length > MAX_PHRASE_BYTES) {
            VoiceContext ctx = phrase.context;
            stopRecording(playerUUID);
            activeRecordings.put(playerUUID, new AudioPhrase("continued", ctx));
            phrase = activeRecordings.get(playerUUID);
            if (phrase == null) return;
        }

        if (VoiceActivityDetector.isSpeech(audioChunk)) {
            phrase.addChunk(audioChunk);
            phrase.setSilenceCounter(0);
        } else {
            phrase.incrementSilenceCounter();
            if (phrase.getSilenceCounter() > 5)
                phrase.addChunk(audioChunk);
        }
    }

    public static void stopRecording(UUID playerUUID) {
        AudioPhrase phrase = activeRecordings.remove(playerUUID);
        if (phrase == null || phrase.isEmpty()) return;

        byte[] combined = phrase.combineChunks();
        byte[] trimmed  = VoiceActivityDetector.trimSilence(combined);

        if (trimmed.length >= MIN_PHRASE_BYTES) {
            VoiceStorage.storeVoiceClip(playerUUID, trimmed, phrase.context);
        }
    }

    public static void finalizeAll() {
        new ArrayList<>(activeRecordings.keySet()).forEach(SmartAudioClipManager::stopRecording);
    }

    public static AudioPhrase getActiveRecording(UUID playerUUID) {
        return activeRecordings.get(playerUUID);
    }

    public static boolean isRecording(UUID playerUUID) {
        return activeRecordings.containsKey(playerUUID);
    }

    public static float getRecordingEnergyLevel(UUID playerUUID) {
        AudioPhrase phrase = activeRecordings.get(playerUUID);
        if (phrase == null || phrase.isEmpty()) return 0.0f;
        return VoiceActivityDetector.getEnergyLevel(phrase.getLastChunk());
    }

    public static class AudioPhrase {
        public final VoiceContext context;
        private final String playerName;
        private final List<byte[]> chunks = new ArrayList<>();
        private int silenceCounter = 0;
        private int totalSize = 0;

        public AudioPhrase(String playerName, VoiceContext context) {
            this.playerName = playerName;
            this.context    = context;
        }

        public void addChunk(byte[] chunk) {
            if (chunk != null && chunk.length > 0) {
                chunks.add(chunk);
                totalSize += chunk.length;
            }
        }

        public void setSilenceCounter(int v)  { this.silenceCounter = v; }
        public void incrementSilenceCounter() { this.silenceCounter++; }
        public int  getSilenceCounter()       { return silenceCounter; }
        public int  getTotalSize()            { return totalSize; }
        public boolean isEmpty()              { return chunks.isEmpty(); }

        public byte[] getLastChunk() {
            return chunks.isEmpty() ? new byte[0] : chunks.get(chunks.size() - 1);
        }

        public long getDurationMs() { return chunks.size() * 10L; }

        public byte[] combineChunks() {
            byte[] out = new byte[totalSize];
            int off = 0;
            for (byte[] c : chunks) { System.arraycopy(c, 0, out, off, c.length); off += c.length; }
            return out;
        }

        public String getPlayerName() { return playerName; }
    }
}
