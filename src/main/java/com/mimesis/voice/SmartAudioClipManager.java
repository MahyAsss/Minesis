package com.mimesis.voice;

import java.util.*;

/**
 * Smart audio clip manager that combines voice chunks into coherent phrases
 * Uses VAD to trim silence and ensure quality audio storage
 */
public class SmartAudioClipManager {

    // Track audio being collected per player
    private static final Map<UUID, AudioPhrase> activeRecordings = new HashMap<>();

    // Buffer size for chunking (480 bytes = 10ms at 48kHz 16-bit mono)
    private static final int CHUNK_BUFFER_SIZE = 480;

    // Minimum phrase duration (in samples at 48kHz)
    private static final int MIN_PHRASE_SAMPLES = 48000; // 1 second minimum

    // Minimum silence gap to separate phrases (in samples)
    private static final int SILENCE_GAP_SAMPLES = 96000; // 2 seconds at 48kHz

    /**
     * Start recording a new phrase for a player
     */
    public static void startRecording(UUID playerUUID, String playerName) {
        if (!activeRecordings.containsKey(playerUUID)) {
            activeRecordings.put(playerUUID, new AudioPhrase(playerName));
        }
    }

    /**
     * Add audio chunk to current recording
     */
    public static void addAudioChunk(UUID playerUUID, byte[] audioChunk) {
        if (audioChunk == null || audioChunk.length == 0) {
            return;
        }

        AudioPhrase phrase = activeRecordings.get(playerUUID);
        if (phrase == null) {
            phrase = new AudioPhrase("Unknown");
            activeRecordings.put(playerUUID, phrase);
        }

        // Check if this chunk is speech
        if (VoiceActivityDetector.isSpeech(audioChunk)) {
            phrase.addChunk(audioChunk);
            phrase.setSilenceCounter(0); // Reset silence counter
        } else {
            // Increment silence counter
            phrase.incrementSilenceCounter();

            // If we have a long silence, might be end of phrase
            if (phrase.getSilenceCounter() > 5) {
                // Still add silent chunks for now, we'll trim later
                phrase.addChunk(audioChunk);
            }
        }
    }

    /**
     * Finish recording and store as a clip
     */
    public static void stopRecording(UUID playerUUID) {
        AudioPhrase phrase = activeRecordings.remove(playerUUID);

        if (phrase == null || phrase.isEmpty()) {
            return;
        }

        // Combine all chunks
        byte[] completeAudio = phrase.combineChunks();

        // Trim silence from beginning and end
        byte[] trimmedAudio = VoiceActivityDetector.trimSilence(completeAudio);

        // Only store if significant length
        if (trimmedAudio.length >= MIN_PHRASE_SAMPLES * 2) {
            // Store the cleaned-up audio (max 3 seconds)
            byte[] finalAudio = trimmedAudio.length > 288000 ? new byte[288000] : trimmedAudio;
            if (finalAudio.length > 0 && finalAudio.length <= 288000) {
                System.arraycopy(trimmedAudio, 0, finalAudio, 0, Math.min(trimmedAudio.length, 288000));
            }

            // Store the cleaned-up audio
            VoiceStorage.storeVoiceClip(playerUUID, finalAudio);
        }
    }

    /**
     * Force finalize all active recordings
     */
    public static void finalizeAll() {
        List<UUID> playerIds = new ArrayList<>(activeRecordings.keySet());
        for (UUID playerUUID : playerIds) {
            stopRecording(playerUUID);
        }
    }

    /**
     * Get current recording for a player
     */
    public static AudioPhrase getActiveRecording(UUID playerUUID) {
        return activeRecordings.get(playerUUID);
    }

    /**
     * Check if player is currently recording
     */
    public static boolean isRecording(UUID playerUUID) {
        return activeRecordings.containsKey(playerUUID);
    }

    /**
     * Get audio energy level of current recording
     */
    public static float getRecordingEnergyLevel(UUID playerUUID) {
        AudioPhrase phrase = activeRecordings.get(playerUUID);
        if (phrase == null || phrase.isEmpty()) {
            return 0.0f;
        }

        byte[] lastChunk = phrase.getLastChunk();
        return VoiceActivityDetector.getEnergyLevel(lastChunk);
    }

    /**
     * Inner class representing an audio phrase being recorded
     */
    public static class AudioPhrase {
        private final String playerName;
        private final List<byte[]> chunks;
        private int silenceCounter;
        private long startTime;

        public AudioPhrase(String playerName) {
            this.playerName = playerName;
            this.chunks = new ArrayList<>();
            this.silenceCounter = 0;
            this.startTime = System.currentTimeMillis();
        }

        public void addChunk(byte[] chunk) {
            if (chunk != null && chunk.length > 0) {
                chunks.add(chunk);
            }
        }

        public void setSilenceCounter(int count) {
            this.silenceCounter = count;
        }

        public void incrementSilenceCounter() {
            this.silenceCounter++;
        }

        public int getSilenceCounter() {
            return this.silenceCounter;
        }

        public boolean isEmpty() {
            return chunks.isEmpty();
        }

        public byte[] getLastChunk() {
            if (chunks.isEmpty()) {
                return new byte[0];
            }
            return chunks.get(chunks.size() - 1);
        }

        public int getChunkCount() {
            return chunks.size();
        }

        public long getDurationMs() {
            // Estimate: each chunk is 10ms at 48kHz
            return chunks.size() * 10;
        }

        /**
         * Combine all chunks into single byte array
         */
        public byte[] combineChunks() {
            if (chunks.isEmpty()) {
                return new byte[0];
            }

            // Calculate total size
            int totalSize = 0;
            for (byte[] chunk : chunks) {
                totalSize += chunk.length;
            }

            // Combine
            byte[] combined = new byte[totalSize];
            int offset = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, combined, offset, chunk.length);
                offset += chunk.length;
            }

            return combined;
        }

        public String getPlayerName() {
            return playerName;
        }
    }
}
