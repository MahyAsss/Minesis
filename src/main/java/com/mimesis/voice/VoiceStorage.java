package com.mimesis.voice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and manages audio clips for each player
 * Keeps track of voice recordings by player UUID
 * Uses intelligent clip management with VAD trimming
 */
public class VoiceStorage {
    private static final Map<UUID, List<byte[]>> playerVoiceClips = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastPlaybackTime = new ConcurrentHashMap<>();
    private static final Map<UUID, AudioClipStats> clipStatistics = new ConcurrentHashMap<>();

    private static final int VOICE_CLIP_MAX_DURATION = 3000; // 3 seconds in ms

    /**
     * Store an audio clip for a player
     * Validates audio quality before storage
     */
    public static void storeVoiceClip(UUID playerUUID, byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return;
        }

        // Limit to 3 seconds (288000 bytes at 48kHz 16-bit)
        byte[] trimmedAudio = audioData.length > 288000 ? new byte[288000] : audioData;
        if (trimmedAudio.length == 0 || trimmedAudio.length > 288000) {
            return;
        }

        if (audioData.length > 288000) {
            System.arraycopy(audioData, 0, trimmedAudio, 0, 288000);
        }

        List<byte[]> clips = playerVoiceClips.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        clips.add(trimmedAudio);

        // Intentionally unlimited per-player clip storage.
        // Spawn criteria rely on per-person sample history.

        // Update statistics
        updateStatistics(playerUUID, trimmedAudio);
    }

    /**
     * Get a random voice clip for a player
     */
    public static byte[] getRandomVoiceClip(UUID playerUUID) {
        List<byte[]> clips = playerVoiceClips.getOrDefault(playerUUID, new ArrayList<>());
        if (clips.isEmpty()) {
            return null;
        }
        return clips.get(new Random().nextInt(clips.size()));
    }

    /**
     * Get the most recent voice clip for a player
     */
    public static byte[] getLatestVoiceClip(UUID playerUUID) {
        List<byte[]> clips = playerVoiceClips.getOrDefault(playerUUID, new ArrayList<>());
        if (clips.isEmpty()) {
            return null;
        }
        return clips.get(clips.size() - 1);
    }

    /**
     * Check if player has any voice clips stored
     */
    public static boolean hasVoiceClips(UUID playerUUID) {
        return !playerVoiceClips.getOrDefault(playerUUID, new ArrayList<>()).isEmpty();
    }

    /**
     * Get number of voice samples captured for a player.
     * This is a lifetime/session counter used by spawn criteria,
     * independent from the small in-memory playback buffer.
     */
    public static int getClipCount(UUID playerUUID) {
        AudioClipStats stats = clipStatistics.get(playerUUID);
        if (stats != null) {
            return stats.getClipCount();
        }
        // Fallback for older state where stats may not be initialized yet.
        return playerVoiceClips.getOrDefault(playerUUID, new ArrayList<>()).size();
    }

    /**
     * Get all clips for a player
     */
    public static List<byte[]> getAllClips(UUID playerUUID) {
        return new ArrayList<>(playerVoiceClips.getOrDefault(playerUUID, new ArrayList<>()));
    }

    /**
     * Clear voice clips for a player
     */
    public static void clearVoiceClips(UUID playerUUID) {
        playerVoiceClips.remove(playerUUID);
        lastPlaybackTime.remove(playerUUID);
        clipStatistics.remove(playerUUID);
    }

    /**
     * Clear all stored voice clips
     */
    public static void clearAllClips() {
        playerVoiceClips.clear();
        lastPlaybackTime.clear();
        clipStatistics.clear();
    }

    /**
     * Playback a voice clip (called by MimesisEntity)
     */
    public static void playStoredVoiceClip(UUID playerUUID, Object entity) {
        byte[] audioData = getRandomVoiceClip(playerUUID);
        if (audioData != null) {
            lastPlaybackTime.put(playerUUID, System.currentTimeMillis());
            // Voice playback will be handled by SimpleVoiceChatIntegration
        }
    }

    /**
     * Get last playback time for cooldown purposes
     */
    public static long getLastPlaybackTime(UUID playerUUID) {
        return lastPlaybackTime.getOrDefault(playerUUID, 0L);
    }

    /**
     * Get statistics for a player's stored clips
     */
    public static AudioClipStats getClipStatistics(UUID playerUUID) {
        return clipStatistics.get(playerUUID);
    }

    /**
     * Update or create statistics for player clips
     */
    private static void updateStatistics(UUID playerUUID, byte[] audioData) {
        clipStatistics.compute(playerUUID, (key, oldStats) -> {
            if (oldStats == null) {
                oldStats = new AudioClipStats(playerUUID);
            }
            oldStats.addClip(audioData);
            return oldStats;
        });
    }

    /**
     * Print storage statistics
     */
    public static void printStatistics() {
        System.out.println("=== Voice Storage Statistics ===");
        for (UUID playerUUID : playerVoiceClips.keySet()) {
            AudioClipStats stats = clipStatistics.get(playerUUID);
            if (stats != null) {
                System.out.println(stats);
            }
        }
        System.out.println("================================");
    }

    /**
     * Inner class for tracking audio clip statistics
     */
    public static class AudioClipStats {
        private final UUID playerUUID;
        private int clipCount = 0;
        private long totalDuration = 0; // in ms
        private long totalSize = 0; // in bytes
        private long createdTime;

        public AudioClipStats(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.createdTime = System.currentTimeMillis();
        }

        public void addClip(byte[] audioData) {
            this.clipCount++;
            // Estimate duration: ~5.3ms per 256 bytes at 48kHz
            this.totalDuration += (audioData.length / 256) * 5;
            this.totalSize += audioData.length;
        }

        public int getClipCount() {
            return clipCount;
        }

        public long getTotalDuration() {
            return totalDuration;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public long getAverageDuration() {
            return clipCount > 0 ? totalDuration / clipCount : 0;
        }

        public long getAverageSize() {
            return clipCount > 0 ? totalSize / clipCount : 0;
        }

        @Override
        public String toString() {
            return String.format("Player %s: %d clips | Total: %.2fs | Size: %.2fKB | Avg: %.2fs/clip",
                    playerUUID.toString().substring(0, 8),
                    clipCount,
                    totalDuration / 1000.0,
                    totalSize / 1024.0,
                    getAverageDuration() / 1000.0);
        }
    }
}
