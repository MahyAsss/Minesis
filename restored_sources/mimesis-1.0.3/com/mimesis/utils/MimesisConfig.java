/*
 * Decompiled with CFR 0.152.
 */
package com.mimesis.utils;

import java.util.HashMap;
import java.util.Map;

public class MimesisConfig {
    public static final int HOSTILE_ACTIVATION_TIME_MIN = 600;
    public static final int HOSTILE_ACTIVATION_TIME_MAX = 2400;
    public static final int MAX_VOICE_CLIPS_PER_PLAYER = 5;
    public static final int MAX_VOICE_CLIP_DURATION_MS = 3000;
    public static final int VOICE_REPLAY_INTERVAL = 100;
    public static final float ENTITY_WIDTH = 0.6f;
    public static final float ENTITY_HEIGHT = 1.8f;
    public static final int CLIENT_TRACKING_RANGE = 10;
    public static final int UPDATE_INTERVAL = 3;
    public static final int XP_REWARD = 50;
    public static final int SPAWN_DISTANCE_MIN = 10;
    public static final int SPAWN_DISTANCE_MAX = 20;
    public static final double RENDER_SCALE = 1.0;
    public static final double FOLLOW_SPEED = 1.0;
    public static final double FOLLOW_DISTANCE = 32.0;
    public static final double STOP_DISTANCE = 2.0;
    public static final double ATTACK_RANGE = 16.0;
    private static final Map<String, Object> CUSTOM_CONFIG = new HashMap<String, Object>();

    public static Object getConfig(String key, Object defaultValue) {
        return CUSTOM_CONFIG.getOrDefault(key, defaultValue);
    }

    public static void setConfig(String key, Object value) {
        CUSTOM_CONFIG.put(key, value);
    }

    public static void printConfig() {
        System.out.println("=== Mimesis Config ===");
        System.out.println("HOSTILE_ACTIVATION_TIME: 600-2400 ticks");
        System.out.println("MAX_VOICE_CLIPS: 5");
        System.out.println("VOICE_CLIP_DURATION: 3000ms");
        System.out.println("VOICE_REPLAY_INTERVAL: 100 ticks");
        System.out.println("FOLLOW_DISTANCE: 32.0 blocks");
        System.out.println("=======================");
    }
}

