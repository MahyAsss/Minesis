/*
 * Decompiled with CFR 0.152.
 */
package com.mimesis.voice;

import com.mimesis.voice.VoiceActivityDetector;
import com.mimesis.voice.VoiceStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SmartAudioClipManager {
    private static final Map<UUID, AudioPhrase> activeRecordings = new HashMap<UUID, AudioPhrase>();
    private static final int CHUNK_BUFFER_SIZE = 480;
    private static final int MIN_PHRASE_SAMPLES = 48000;
    private static final int SILENCE_GAP_SAMPLES = 96000;

    public static void startRecording(UUID playerUUID, String playerName) {
        if (!activeRecordings.containsKey(playerUUID)) {
            activeRecordings.put(playerUUID, new AudioPhrase(playerName));
        }
    }

    public static void addAudioChunk(UUID playerUUID, byte[] audioChunk) {
        if (audioChunk == null || audioChunk.length == 0) {
            return;
        }
        AudioPhrase phrase = activeRecordings.get(playerUUID);
        if (phrase == null) {
            phrase = new AudioPhrase("Unknown");
            activeRecordings.put(playerUUID, phrase);
        }
        if (VoiceActivityDetector.isSpeech(audioChunk)) {
            phrase.addChunk(audioChunk);
            phrase.setSilenceCounter(0);
        } else {
            phrase.incrementSilenceCounter();
            if (phrase.getSilenceCounter() > 5) {
                phrase.addChunk(audioChunk);
            }
        }
    }

    public static void stopRecording(UUID playerUUID) {
        AudioPhrase phrase = activeRecordings.remove(playerUUID);
        if (phrase == null || phrase.isEmpty()) {
            return;
        }
        byte[] completeAudio = phrase.combineChunks();
        byte[] trimmedAudio = VoiceActivityDetector.trimSilence(completeAudio);
        if (trimmedAudio.length >= 96000) {
            byte[] finalAudio;
            byte[] byArray = finalAudio = trimmedAudio.length > 288000 ? new byte[288000] : trimmedAudio;
            if (finalAudio.length > 0 && finalAudio.length <= 288000) {
                System.arraycopy(trimmedAudio, 0, finalAudio, 0, Math.min(trimmedAudio.length, 288000));
            }
            VoiceStorage.storeVoiceClip(playerUUID, finalAudio);
        }
    }

    public static void finalizeAll() {
        ArrayList<UUID> playerIds = new ArrayList<UUID>(activeRecordings.keySet());
        for (UUID playerUUID : playerIds) {
            SmartAudioClipManager.stopRecording(playerUUID);
        }
    }

    public static AudioPhrase getActiveRecording(UUID playerUUID) {
        return activeRecordings.get(playerUUID);
    }

    public static boolean isRecording(UUID playerUUID) {
        return activeRecordings.containsKey(playerUUID);
    }

    public static float getRecordingEnergyLevel(UUID playerUUID) {
        AudioPhrase phrase = activeRecordings.get(playerUUID);
        if (phrase == null || phrase.isEmpty()) {
            return 0.0f;
        }
        byte[] lastChunk = phrase.getLastChunk();
        return VoiceActivityDetector.getEnergyLevel(lastChunk);
    }

    public static class AudioPhrase {
        private final String playerName;
        private final List<byte[]> chunks;
        private int silenceCounter;
        private long startTime;

        public AudioPhrase(String playerName) {
            this.playerName = playerName;
            this.chunks = new ArrayList<byte[]>();
            this.silenceCounter = 0;
            this.startTime = System.currentTimeMillis();
        }

        public void addChunk(byte[] chunk) {
            if (chunk != null && chunk.length > 0) {
                this.chunks.add(chunk);
            }
        }

        public void setSilenceCounter(int count) {
            this.silenceCounter = count;
        }

        public void incrementSilenceCounter() {
            ++this.silenceCounter;
        }

        public int getSilenceCounter() {
            return this.silenceCounter;
        }

        public boolean isEmpty() {
            return this.chunks.isEmpty();
        }

        public byte[] getLastChunk() {
            if (this.chunks.isEmpty()) {
                return new byte[0];
            }
            return this.chunks.get(this.chunks.size() - 1);
        }

        public int getChunkCount() {
            return this.chunks.size();
        }

        public long getDurationMs() {
            return this.chunks.size() * 10;
        }

        public byte[] combineChunks() {
            if (this.chunks.isEmpty()) {
                return new byte[0];
            }
            int totalSize = 0;
            for (byte[] chunk : this.chunks) {
                totalSize += chunk.length;
            }
            byte[] combined = new byte[totalSize];
            int offset = 0;
            for (byte[] chunk : this.chunks) {
                System.arraycopy(chunk, 0, combined, offset, chunk.length);
                offset += chunk.length;
            }
            return combined;
        }

        public String getPlayerName() {
            return this.playerName;
        }
    }
}

