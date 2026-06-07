package com.minesis.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the Minesis mod
 */
public class MinesisConfig {
    
    // Behavior timing (in ticks, 1 second = 20 ticks)
    public static final int HOSTILE_ACTIVATION_TIME_MIN = 600;  // 30 seconds
    public static final int HOSTILE_ACTIVATION_TIME_MAX = 2400; // 120 seconds
    
    // Voice settings
    public static final int MAX_VOICE_CLIPS_PER_PLAYER = 5;
    public static final int MAX_VOICE_CLIP_DURATION_MS = 3000; // 3 seconds
    public static final int VOICE_REPLAY_INTERVAL = 100;       // 5 seconds between replays
    
    // Entity settings
    public static final float ENTITY_WIDTH = 0.6f;
    public static final float ENTITY_HEIGHT = 1.8f;
    public static final int CLIENT_TRACKING_RANGE = 10;
    public static final int UPDATE_INTERVAL = 3;
    public static final int XP_REWARD = 50;
    
    // Spawn settings
    public static final int SPAWN_DISTANCE_MIN = 10;
    public static final int SPAWN_DISTANCE_MAX = 20;
    
    // Rendering
    public static final double RENDER_SCALE = 1.0D;
    
    // AI settings
    public static final double FOLLOW_SPEED = 1.0D;
    public static final double FOLLOW_DISTANCE = 32.0D;
    public static final double STOP_DISTANCE = 2.0D;
    public static final double ATTACK_RANGE = 16.0D;
    
    private static final Map<String, Object> CUSTOM_CONFIG = new HashMap<>();
    
    /**
     * Get a config value, or default if not set
     */
    public static Object getConfig(String key, Object defaultValue) {
        return CUSTOM_CONFIG.getOrDefault(key, defaultValue);
    }
    
    /**
     * Set a custom config value
     */
    public static void setConfig(String key, Object value) {
        CUSTOM_CONFIG.put(key, value);
    }
    
    /**
     * Print all current config values
     */
    public static void printConfig() {
        System.out.println("=== Minesis Config ===");
        System.out.println("HOSTILE_ACTIVATION_TIME: " + HOSTILE_ACTIVATION_TIME_MIN + "-" + HOSTILE_ACTIVATION_TIME_MAX + " ticks");
        System.out.println("MAX_VOICE_CLIPS: " + MAX_VOICE_CLIPS_PER_PLAYER);
        System.out.println("VOICE_CLIP_DURATION: " + MAX_VOICE_CLIP_DURATION_MS + "ms");
        System.out.println("VOICE_REPLAY_INTERVAL: " + VOICE_REPLAY_INTERVAL + " ticks");
        System.out.println("FOLLOW_DISTANCE: " + FOLLOW_DISTANCE + " blocks");
        System.out.println("=======================");
    }
}
