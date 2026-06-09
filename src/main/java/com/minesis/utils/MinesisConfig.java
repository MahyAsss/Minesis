package com.minesis.utils;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class MinesisConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public  static final ForgeConfigSpec         SPEC;

    // ── Hostile behaviour ─────────────────────────────────────────────────────
    public static final ForgeConfigSpec.IntValue     MIN_HOSTILE_DELAY_SECONDS;
    public static final ForgeConfigSpec.IntValue     MAX_HOSTILE_DELAY_SECONDS;
    public static final ForgeConfigSpec.BooleanValue TRANSFORMATION_SCREEN_EFFECTS;

    // ── Voice ─────────────────────────────────────────────────────────────────
    public static final ForgeConfigSpec.IntValue    VOICE_REPLAY_MIN_SECONDS;
    public static final ForgeConfigSpec.IntValue    VOICE_REPLAY_MAX_SECONDS;
    public static final ForgeConfigSpec.DoubleValue SPEECH_RMS_THRESHOLD;

    // ── World interaction ─────────────────────────────────────────────────────
    public static final ForgeConfigSpec.DoubleValue CHEST_DISC_DROP_CHANCE;
    public static final ForgeConfigSpec.IntValue    NATURAL_APPEARANCE_MIN_CLIPS;

    static {
        BUILDER.push("hostile_behavior");

        MIN_HOSTILE_DELAY_SECONDS = BUILDER
            .comment("Minimum time in seconds before Minesis can turn hostile after spawning. Default: 60")
            .defineInRange("min_hostile_delay_seconds", 60, 10, 3600);

        MAX_HOSTILE_DELAY_SECONDS = BUILDER
            .comment("Maximum time in seconds before Minesis turns hostile. Must be >= min. Default: 180")
            .defineInRange("max_hostile_delay_seconds", 180, 10, 7200);

        TRANSFORMATION_SCREEN_EFFECTS = BUILDER
            .comment("Whether the transformation applies blindness/darkness/nausea to nearby players. Default: true")
            .define("transformation_screen_effects", true);

        BUILDER.pop();
        BUILDER.push("voice");

        VOICE_REPLAY_MIN_SECONDS = BUILDER
            .comment("Minimum time in seconds between autonomous ambient voice clips. Default: 600 (10 min)")
            .defineInRange("voice_replay_min_seconds", 600, 30, 86400);

        VOICE_REPLAY_MAX_SECONDS = BUILDER
            .comment("Maximum time in seconds between autonomous ambient voice clips. Default: 1200 (20 min)")
            .defineInRange("voice_replay_max_seconds", 1200, 30, 86400);

        SPEECH_RMS_THRESHOLD = BUILDER
            .comment("RMS energy threshold [0.0-1.0] below which an audio clip is treated as background noise",
                     "and never used as a direct response to player speech. Default: 0.02")
            .defineInRange("speech_rms_threshold", 0.02, 0.0, 1.0);

        BUILDER.pop();
        BUILDER.push("world_interaction");

        CHEST_DISC_DROP_CHANCE = BUILDER
            .comment("Probability [0.0-1.0] that Minesis deposits an Echoes Below disc when opening a chest. Default: 0.30")
            .defineInRange("chest_disc_drop_chance", 0.30, 0.0, 1.0);

        NATURAL_APPEARANCE_MIN_CLIPS = BUILDER
            .comment("Minimum number of recorded voice clips required before Minesis can appear naturally. Default: 50")
            .defineInRange("natural_appearance_min_clips", 50, 1, 500);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    // ── Convenience tick-unit helpers ─────────────────────────────────────────

    public static int minHostileDelayTicks() {
        return MIN_HOSTILE_DELAY_SECONDS.get() * 20;
    }

    public static int maxHostileDelayTicks() {
        return MAX_HOSTILE_DELAY_SECONDS.get() * 20;
    }

    public static int voiceReplayMinTicks() {
        return VOICE_REPLAY_MIN_SECONDS.get() * 20;
    }

    public static int voiceReplayMaxTicks() {
        return VOICE_REPLAY_MAX_SECONDS.get() * 20;
    }
}
