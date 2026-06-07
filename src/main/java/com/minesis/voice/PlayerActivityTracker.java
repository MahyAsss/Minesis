package com.minesis.voice;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerActivityTracker {
    private static final ConcurrentHashMap<UUID, Long> lastMineTime  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastSpeakTime = new ConcurrentHashMap<>();
    private static final long MINING_WINDOW_MS   = 3000L;

    public static void markMining(UUID uuid) {
        lastMineTime.put(uuid, System.currentTimeMillis());
    }

    public static boolean isMining(UUID uuid) {
        Long t = lastMineTime.get(uuid);
        return t != null && System.currentTimeMillis() - t < MINING_WINDOW_MS;
    }

    public static void markSpeaking(UUID uuid) {
        lastSpeakTime.put(uuid, System.currentTimeMillis());
    }

    public static boolean hasRecentlySpoken(UUID uuid, long windowMs) {
        Long t = lastSpeakTime.get(uuid);
        return t != null && System.currentTimeMillis() - t < windowMs;
    }
}
