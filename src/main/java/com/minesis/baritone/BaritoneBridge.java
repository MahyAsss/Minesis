package com.minesis.baritone;

import com.minesis.entity.MinesisEntity;
import net.minecraft.world.phys.Vec3;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import java.lang.reflect.Method;

/**
 * Direct Baritone bridge. Uses Baritone API when available. If Baritone isn't present,
 * the bridge disables itself silently and the mod falls back to vanilla navigation.
 */
public class BaritoneBridge {
    private static boolean initialized = false;
    private static boolean available = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            // Simple probe
            if (BaritoneAPI.getProvider() != null) {
                available = true;
                System.out.println("[Minesis] BaritoneBridge: Baritone API initialized.");
            }
        } catch (NoClassDefFoundError | Exception e) {
            available = false;
            System.out.println("[Minesis] BaritoneBridge: Baritone API not available: " + e.getMessage());
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static void moveTo(MinesisEntity entity, Vec3 dest) {
        if (!isAvailable()) return;
        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            Object cm = baritone.getCommandManager();
            String cmd = String.format("goto %.2f %.2f %.2f", dest.x, dest.y, dest.z);
            Method m = cm.getClass().getMethod("execute", String.class);
            m.invoke(cm, cmd);
        } catch (NoClassDefFoundError | Exception e) {
            available = false;
            System.out.println("[Minesis] BaritoneBridge: moveTo failed, disabling bridge: " + e.getMessage());
        }
    }

    public static void stop(MinesisEntity entity) {
        if (!isAvailable()) return;
        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            Object cm = baritone.getCommandManager();
            Method m = cm.getClass().getMethod("execute", String.class);
            m.invoke(cm, "stop");
        } catch (NoClassDefFoundError | Exception e) {
            available = false;
            System.out.println("[Minesis] BaritoneBridge: stop failed, disabling bridge: " + e.getMessage());
        }
    }
}
