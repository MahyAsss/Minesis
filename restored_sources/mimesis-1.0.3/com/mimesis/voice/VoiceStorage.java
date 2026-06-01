/*
 * Decompiled with CFR 0.152.
 */
package com.mimesis.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceStorage {
    private static final Map<UUID, List<byte[]>> playerVoiceClips = new ConcurrentHashMap<UUID, List<byte[]>>();
    private static final Map<UUID, Long> lastPlaybackTime = new ConcurrentHashMap<UUID, Long>();
    private static final Map<UUID, AudioClipStats> clipStatistics = new ConcurrentHashMap<UUID, AudioClipStats>();
    private static final int VOICE_CLIP_MAX_DURATION = 3000;

    public static void storeVoiceClip(UUID playerUUID, byte[] audioData) {
        byte[] trimmedAudio;
        if (audioData == null || audioData.length == 0) {
            return;
        }
        byte[] byArray = trimmedAudio = audioData.length > 288000 ? new byte[288000] : audioData;
        if (trimmedAudio.length == 0 || trimmedAudio.length > 288000) {
            return;
        }
        if (audioData.length > 288000) {
            System.arraycopy(audioData, 0, trimmedAudio, 0, 288000);
        }
        List clips = playerVoiceClips.computeIfAbsent(playerUUID, k -> new ArrayList());
        clips.add(trimmedAudio);
        VoiceStorage.updateStatistics(playerUUID, trimmedAudio);
    }

    public static byte[] getRandomVoiceClip(UUID playerUUID) {
        List clips = playerVoiceClips.getOrDefault(playerUUID, new ArrayList());
        if (clips.isEmpty()) {
            return null;
        }
        return (byte[])clips.get(new Random().nextInt(clips.size()));
    }

    public static byte[] getLatestVoiceClip(UUID playerUUID) {
        List clips = playerVoiceClips.getOrDefault(playerUUID, new ArrayList());
        if (clips.isEmpty()) {
            return null;
        }
        return (byte[])clips.get(clips.size() - 1);
    }

    public static boolean hasVoiceClips(UUID playerUUID) {
        return !((List)playerVoiceClips.getOrDefault(playerUUID, new ArrayList())).isEmpty();
    }

    public static int getClipCount(UUID playerUUID) {
        AudioClipStats stats = clipStatistics.get(playerUUID);
        if (stats != null) {
            return stats.getClipCount();
        }
        return ((List)playerVoiceClips.getOrDefault(playerUUID, new ArrayList())).size();
    }

    public static List<byte[]> getAllClips(UUID playerUUID) {
        return new ArrayList<byte[]>(playerVoiceClips.getOrDefault(playerUUID, new ArrayList()));
    }

    public static void clearVoiceClips(UUID playerUUID) {
        playerVoiceClips.remove(playerUUID);
        lastPlaybackTime.remove(playerUUID);
        clipStatistics.remove(playerUUID);
    }

    public static void clearAllClips() {
        playerVoiceClips.clear();
        lastPlaybackTime.clear();
        clipStatistics.clear();
    }

    public static void playStoredVoiceClip(UUID playerUUID, Object entity) {
        byte[] audioData = VoiceStorage.getRandomVoiceClip(playerUUID);
        if (audioData != null) {
            lastPlaybackTime.put(playerUUID, System.currentTimeMillis());
        }
    }

    public static long getLastPlaybackTime(UUID playerUUID) {
        return lastPlaybackTime.getOrDefault(playerUUID, 0L);
    }

    public static AudioClipStats getClipStatistics(UUID playerUUID) {
        return clipStatistics.get(playerUUID);
    }

    private static void updateStatistics(UUID playerUUID, byte[] audioData) {
        clipStatistics.compute(playerUUID, (key, oldStats) -> {
            if (oldStats == null) {
                oldStats = new AudioClipStats(playerUUID);
            }
            oldStats.addClip(audioData);
            return oldStats;
        });
    }

    public static void printStatistics() {
        System.out.println("=== Voice Storage Statistics ===");
        for (UUID playerUUID : playerVoiceClips.keySet()) {
            AudioClipStats stats = clipStatistics.get(playerUUID);
            if (stats == null) continue;
            System.out.println(stats);
        }
        System.out.println("================================");
    }

    public static class AudioClipStats {
        private final UUID playerUUID;
        private int clipCount = 0;
        private long totalDuration = 0L;
        private long totalSize = 0L;
        private long createdTime;

        public AudioClipStats(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.createdTime = System.currentTimeMillis();
        }

        public void addClip(byte[] audioData) {
            ++this.clipCount;
            this.totalDuration += (long)(audioData.length / 256 * 5);
            this.totalSize += (long)audioData.length;
        }

        public int getClipCount() {
            return this.clipCount;
        }

        public long getTotalDuration() {
            return this.totalDuration;
        }

        public long getTotalSize() {
            return this.totalSize;
        }

        public long getAverageDuration() {
            return this.clipCount > 0 ? this.totalDuration / (long)this.clipCount : 0L;
        }

        public long getAverageSize() {
            return this.clipCount > 0 ? this.totalSize / (long)this.clipCount : 0L;
        }

        public String toString() {
            return String.format("Player %s: %d clips | Total: %.2fs | Size: %.2fKB | Avg: %.2fs/clip", this.playerUUID.toString().substring(0, 8), this.clipCount, (double)this.totalDuration / 1000.0, (double)this.totalSize / 1024.0, (double)this.getAverageDuration() / 1000.0);
        }
    }
}

