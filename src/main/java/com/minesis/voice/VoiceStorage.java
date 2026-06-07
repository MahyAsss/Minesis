package com.minesis.voice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceStorage {
    private static final Map<UUID, List<VoiceClip>> playerVoiceClips = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastPlaybackTime = new ConcurrentHashMap<>();
    private static final Map<UUID, AudioClipStats> clipStatistics = new ConcurrentHashMap<>();

    // 10 seconds at 48 kHz 16-bit mono
    public static final int MAX_CLIP_BYTES = 960_000;

    public static void storeVoiceClip(UUID playerUUID, byte[] audioData, VoiceContext context) {
        if (audioData == null || audioData.length < 960) return; // <10ms — too short

        int len = Math.min(audioData.length, MAX_CLIP_BYTES);
        byte[] stored = new byte[len];
        System.arraycopy(audioData, 0, stored, 0, len);

        VoiceClip clip = new VoiceClip(stored, context == null ? VoiceContext.IDLE : context);
        playerVoiceClips.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(clip);
        updateStatistics(playerUUID, stored);
        VoicePersistenceManager.saveClip(playerUUID, stored, clip.context);
    }

    /**
     * Returns audio bytes for a clip whose context best matches the requested one.
     * Fallback order: exact match → WALKING/RUNNING swap → IDLE → any.
     */
    public static byte[] getClipForContext(UUID playerUUID, VoiceContext context) {
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        if (clips == null || clips.isEmpty()) return null;

        // 1. Exact match
        List<byte[]> candidates = collectAudio(clips, context);

        // 2. WALKING ↔ RUNNING swap
        if (candidates.isEmpty()) {
            if (context == VoiceContext.RUNNING)  candidates = collectAudio(clips, VoiceContext.WALKING);
            else if (context == VoiceContext.WALKING) candidates = collectAudio(clips, VoiceContext.RUNNING);
        }

        // 3. IDLE fallback
        if (candidates.isEmpty() && context != VoiceContext.IDLE)
            candidates = collectAudio(clips, VoiceContext.IDLE);

        // 4. Any clip
        if (candidates.isEmpty())
            for (VoiceClip c : clips) candidates.add(c.audio);

        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private static List<byte[]> collectAudio(List<VoiceClip> clips, VoiceContext ctx) {
        List<byte[]> out = new ArrayList<>();
        for (VoiceClip c : clips) if (c.context == ctx) out.add(c.audio);
        return out;
    }

    /** Returns a random clip regardless of context (kept for legacy callers). */
    public static byte[] getRandomVoiceClip(UUID playerUUID) {
        return getClipForContext(playerUUID, VoiceContext.IDLE);
    }

    public static byte[] getLatestVoiceClip(UUID playerUUID) {
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        if (clips == null || clips.isEmpty()) return null;
        return clips.get(clips.size() - 1).audio;
    }

    public static boolean hasVoiceClips(UUID playerUUID) {
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        return clips != null && !clips.isEmpty();
    }

    public static int getClipCount(UUID playerUUID) {
        AudioClipStats stats = clipStatistics.get(playerUUID);
        if (stats != null) return stats.getClipCount();
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        return clips == null ? 0 : clips.size();
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

    public static void fakeClipCount(UUID playerUUID, int count) {
        playerVoiceClips.remove(playerUUID);
        clipStatistics.compute(playerUUID, (k, s) -> {
            AudioClipStats stat = new AudioClipStats(k);
            stat.forceClipCount(count);
            return stat;
        });
    }

    public static long getLastPlaybackTime(UUID playerUUID) {
        return lastPlaybackTime.getOrDefault(playerUUID, 0L);
    }

    public static AudioClipStats getClipStatistics(UUID playerUUID) {
        return clipStatistics.get(playerUUID);
    }

    private static void updateStatistics(UUID playerUUID, byte[] audioData) {
        clipStatistics.compute(playerUUID, (key, oldStats) -> {
            if (oldStats == null) oldStats = new AudioClipStats(playerUUID);
            oldStats.addClip(audioData);
            return oldStats;
        });
    }

    public static class AudioClipStats {
        private final UUID playerUUID;
        private int clipCount = 0;
        private long totalDuration = 0;
        private long totalSize = 0;

        public AudioClipStats(UUID playerUUID) { this.playerUUID = playerUUID; }

        public void addClip(byte[] audioData) {
            this.clipCount++;
            this.totalDuration += (audioData.length / 256) * 5;
            this.totalSize += audioData.length;
        }

        public void forceClipCount(int count) { this.clipCount = count; }
        public int getClipCount()       { return clipCount; }
        public long getTotalDuration()  { return totalDuration; }
        public long getTotalSize()      { return totalSize; }
        public long getAverageDuration() { return clipCount > 0 ? totalDuration / clipCount : 0; }
        public long getAverageSize()    { return clipCount > 0 ? totalSize / clipCount : 0; }

        @Override public String toString() {
            return String.format("Player %s: %d clips | Total: %.2fs | Size: %.2fKB | Avg: %.2fs/clip",
                    playerUUID.toString().substring(0, 8), clipCount,
                    totalDuration / 1000.0, totalSize / 1024.0, getAverageDuration() / 1000.0);
        }
    }
}
