package com.minesis.voice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceStorage {
    private static final Map<UUID, List<VoiceClip>> playerVoiceClips = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastPlaybackTime = new ConcurrentHashMap<>();
    private static final Map<UUID, AudioClipStats> clipStatistics = new ConcurrentHashMap<>();

    // 10 seconds at 48 kHz 16-bit mono
    public static final int MAX_CLIP_BYTES = 960_000;

    /** Loads a clip from disk into memory — does NOT trigger disk persistence again. */
    public static void loadVoiceClip(UUID playerUUID, byte[] audioData, VoiceContext context) {
        if (audioData == null || audioData.length < 960) return;
        int len = Math.min(audioData.length, MAX_CLIP_BYTES);
        byte[] stored = Arrays.copyOf(audioData, len);
        VoiceClip clip = new VoiceClip(stored, context == null ? VoiceContext.IDLE : context);
        playerVoiceClips.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(clip);
        updateStatistics(playerUUID, stored);
    }

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
     * Used by autonomous ambient playback — white noise clips are included.
     */
    public static byte[] getClipForContext(UUID playerUUID, VoiceContext context) {
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        if (clips == null || clips.isEmpty()) return null;
        List<byte[]> candidates = collectWithFallback(clips, context, false);
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    /**
     * Collects clips for the given context with a semantic fallback chain:
     *   WOODCUTTING→MINING, SMELTING→CRAFTING, FLEEING→RUNNING,
     *   BUILDING/FARMING→WALKING, HURT/RUNNING→WALKING swap, then IDLE, then any.
     *
     * @param speechOnly if true, skips clips whose RMS is below SPEECH_RMS_THRESHOLD
     *                   (white noise / background noise captured by SVC).
     */
    private static List<byte[]> collectWithFallback(List<VoiceClip> clips, VoiceContext context, boolean speechOnly) {
        List<byte[]> out = collectAudio(clips, context, speechOnly);
        if (!out.isEmpty()) return out;

        VoiceContext alias = null;
        switch (context) {
            case WOODCUTTING: alias = VoiceContext.MINING;    break;
            case SMELTING:    alias = VoiceContext.CRAFTING;  break;
            case FLEEING:     alias = VoiceContext.RUNNING;   break;
            case BUILDING:
            case FARMING:     alias = VoiceContext.WALKING;   break;
            case RUNNING:     alias = VoiceContext.WALKING;   break;
            case WALKING:     alias = VoiceContext.RUNNING;   break;
            default:          break;
        }
        if (alias != null) out = collectAudio(clips, alias, speechOnly);

        if (out.isEmpty() && context != VoiceContext.IDLE)
            out = collectAudio(clips, VoiceContext.IDLE, speechOnly);

        // Last resort: any clip (still respecting speechOnly)
        if (out.isEmpty())
            for (VoiceClip c : clips)
                if (!speechOnly || c.isSpeech()) out.add(c.audio);

        // If speechOnly wiped everything, fall back to all clips (better to respond than stay silent)
        if (out.isEmpty())
            for (VoiceClip c : clips) out.add(c.audio);

        return out;
    }

    /**
     * Annotates the most recent unannotated clip for this player with the given transcript.
     * Called server-side when a ClipAnnotationPacket arrives from the recording client.
     */
    public static void annotateLastClip(UUID playerUUID, String transcript) {
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        if (clips == null || clips.isEmpty()) return;
        synchronized (clips) {
            for (int i = clips.size() - 1; i >= 0; i--) {
                if (clips.get(i).transcript.isEmpty()) {
                    clips.get(i).transcript = transcript;
                    return;
                }
            }
            // All already annotated — overwrite the last one
            clips.get(clips.size() - 1).transcript = transcript;
        }
    }

    /**
     * Returns candidates for playback ordered by semantic similarity to queryText.
     *
     * Uses FrenchTextAnalyzer for stemming + synonym-aware Jaccard similarity.
     * Clips with similarity >= TRANSCRIPT_THRESHOLD are returned sorted best-first.
     * Falls back to context-based selection if no transcripts pass the threshold.
     */
    private static final float TRANSCRIPT_THRESHOLD = 0.05f; // minimum score to count as a match

    public static List<byte[]> getCandidatesForQuery(UUID playerUUID, VoiceContext context, String queryText) {
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        if (clips == null || clips.isEmpty()) return Collections.emptyList();

        // Score every annotated clip with the semantic similarity function
        List<float[]> scored = new ArrayList<>(); // [score, index]
        for (int i = 0; i < clips.size(); i++) {
            VoiceClip clip = clips.get(i);
            if (clip.transcript.isEmpty()) continue;
            float sim = FrenchTextAnalyzer.similarity(queryText, clip.transcript);
            if (sim >= TRANSCRIPT_THRESHOLD) scored.add(new float[]{sim, i});
        }

        if (!scored.isEmpty()) {
            // Sort descending by score; randomise within equal-score tiers
            scored.sort((a, b) -> Float.compare(b[0], a[0]));
            // Build candidate list: clips within 15% of the best score are all included
            float best = scored.get(0)[0];
            List<byte[]> candidates = new ArrayList<>();
            for (float[] entry : scored) {
                if (entry[0] >= best * 0.85f) candidates.add(clips.get((int) entry[1]).audio);
            }
            Collections.shuffle(candidates);
            return candidates;
        }

        // Fallback: same cascade as getClipForContext, speech-only for responses
        List<byte[]> fallback = collectWithFallback(clips, context, true);
        Collections.shuffle(fallback);
        return fallback;
    }

    /**
     * Selects clips for a "what are you doing?" reply.
     *
     * Priority:
     *  1. Clips whose transcript starts with "je" / "j'" AND match the entity's current context.
     *  2. Any clip starting with "je" / "j'" regardless of context.
     *  3. Best context match (no transcript filter) as a last resort.
     */
    public static List<byte[]> getCandidatesForSelfDescription(UUID playerUUID, VoiceContext entityContext) {
        List<VoiceClip> clips = playerVoiceClips.get(playerUUID);
        if (clips == null || clips.isEmpty()) return Collections.emptyList();

        List<byte[]> contextJe = new ArrayList<>();
        List<byte[]> anyJe     = new ArrayList<>();

        for (VoiceClip clip : clips) {
            if (!clip.isSpeech()) continue; // skip white noise even for self-description
            String t = clip.transcript.trim().toLowerCase(java.util.Locale.ROOT);
            boolean startsJe = t.startsWith("je ") || t.startsWith("j'") || t.startsWith("j ");
            if (!startsJe) continue;
            anyJe.add(clip.audio);
            if (contextCompatible(clip.context, entityContext))
                contextJe.add(clip.audio);
        }

        if (!contextJe.isEmpty()) { Collections.shuffle(contextJe); return contextJe; }
        if (!anyJe.isEmpty())     { Collections.shuffle(anyJe);     return anyJe;     }
        // No transcribed first-person clips → best context match, speech-only
        List<byte[]> fallback = collectWithFallback(clips, entityContext, true);
        Collections.shuffle(fallback);
        return fallback;
    }

    /** True if a clip's context is considered a match for the query context (loose). */
    private static boolean contextCompatible(VoiceContext clip, VoiceContext query) {
        if (clip == query) return true;
        switch (query) {
            case WOODCUTTING: return clip == VoiceContext.MINING;
            case SMELTING:    return clip == VoiceContext.CRAFTING;
            case FLEEING:     return clip == VoiceContext.RUNNING;
            case BUILDING:
            case FARMING:     return clip == VoiceContext.WALKING;
            case HURT:        return clip == VoiceContext.IDLE;
            default:          return false;
        }
    }

    private static List<byte[]> collectAudio(List<VoiceClip> clips, VoiceContext ctx, boolean speechOnly) {
        List<byte[]> out = new ArrayList<>();
        for (VoiceClip c : clips)
            if (c.context == ctx && (!speechOnly || c.isSpeech())) out.add(c.audio);
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
