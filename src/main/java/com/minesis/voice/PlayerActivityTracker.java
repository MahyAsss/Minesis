package com.minesis.voice;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerActivityTracker {
    private static final ConcurrentHashMap<UUID, Long> lastMineTime      = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastWoodcutTime   = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastSpeakTime     = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastHurtTime      = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastFleeTime      = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastBuildTime     = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastFarmTime      = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastSmeltTime     = new ConcurrentHashMap<>();

    private static final long MINING_WINDOW_MS    = 3_000L;
    private static final long WOODCUT_WINDOW_MS   = 3_000L;
    private static final long HURT_WINDOW_MS      = 3_000L;
    private static final long FLEE_WINDOW_MS      = 3_000L;
    private static final long BUILD_WINDOW_MS     = 3_000L;
    private static final long FARM_WINDOW_MS      = 3_000L;
    private static final long SMELT_WINDOW_MS     = 5_000L;

    public static void markMining(UUID uuid)      { lastMineTime.put(uuid, System.currentTimeMillis()); }
    public static void markWoodcutting(UUID uuid) { lastWoodcutTime.put(uuid, System.currentTimeMillis()); }
    public static void markHurt(UUID uuid)        { lastHurtTime.put(uuid, System.currentTimeMillis()); }
    public static void markFleeing(UUID uuid)     { lastFleeTime.put(uuid, System.currentTimeMillis()); }
    public static void markBuilding(UUID uuid)    { lastBuildTime.put(uuid, System.currentTimeMillis()); }
    public static void markFarming(UUID uuid)     { lastFarmTime.put(uuid, System.currentTimeMillis()); }
    public static void markSmelting(UUID uuid)    { lastSmeltTime.put(uuid, System.currentTimeMillis()); }
    public static void markSpeaking(UUID uuid)    { lastSpeakTime.put(uuid, System.currentTimeMillis()); }

    public static boolean isMining(UUID uuid)      { return within(lastMineTime, uuid, MINING_WINDOW_MS); }
    public static boolean isWoodcutting(UUID uuid) { return within(lastWoodcutTime, uuid, WOODCUT_WINDOW_MS); }
    public static boolean isHurt(UUID uuid)        { return within(lastHurtTime, uuid, HURT_WINDOW_MS); }
    public static boolean isFleeing(UUID uuid)     { return within(lastFleeTime, uuid, FLEE_WINDOW_MS); }
    public static boolean isBuilding(UUID uuid)    { return within(lastBuildTime, uuid, BUILD_WINDOW_MS); }
    public static boolean isFarming(UUID uuid)     { return within(lastFarmTime, uuid, FARM_WINDOW_MS); }
    public static boolean isSmelting(UUID uuid)    { return within(lastSmeltTime, uuid, SMELT_WINDOW_MS); }

    public static boolean hasRecentlySpoken(UUID uuid, long windowMs) {
        return within(lastSpeakTime, uuid, windowMs);
    }

    private static boolean within(ConcurrentHashMap<UUID, Long> map, UUID uuid, long windowMs) {
        Long t = map.get(uuid);
        return t != null && System.currentTimeMillis() - t < windowMs;
    }
}
