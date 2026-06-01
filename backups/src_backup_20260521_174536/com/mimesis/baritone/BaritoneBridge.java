/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  baritone.api.BaritoneAPI
 *  baritone.api.IBaritone
 *  baritone.api.command.manager.ICommandManager
 *  net.minecraft.world.phys.Vec3
 */
package com.mimesis.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.command.manager.ICommandManager;
import com.mimesis.entity.MimesisEntity;
import java.lang.reflect.Method;
import net.minecraft.world.phys.Vec3;

public class BaritoneBridge {
    private static boolean initialized = false;
    private static boolean available = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            if (BaritoneAPI.getProvider() != null) {
                available = true;
                System.out.println("[Mimesis] BaritoneBridge: Baritone API initialized.");
            }
        }
        catch (Exception | NoClassDefFoundError e) {
            available = false;
            System.out.println("[Mimesis] BaritoneBridge: Baritone API not available: " + e.getMessage());
        }
    }

    public static boolean isAvailable() {
        BaritoneBridge.init();
        return available;
    }

    public static void moveTo(MimesisEntity entity, Vec3 dest) {
        if (!BaritoneBridge.isAvailable()) {
            return;
        }
        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            ICommandManager cm = baritone.getCommandManager();
            String cmd = String.format("goto %.2f %.2f %.2f", dest.f_82479_, dest.f_82480_, dest.f_82481_);
            Method m = cm.getClass().getMethod("execute", String.class);
            m.invoke(cm, cmd);
        }
        catch (Exception | NoClassDefFoundError e) {
            available = false;
            System.out.println("[Mimesis] BaritoneBridge: moveTo failed, disabling bridge: " + e.getMessage());
        }
    }

    public static void stop(MimesisEntity entity) {
        if (!BaritoneBridge.isAvailable()) {
            return;
        }
        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            ICommandManager cm = baritone.getCommandManager();
            Method m = cm.getClass().getMethod("execute", String.class);
            m.invoke(cm, "stop");
        }
        catch (Exception | NoClassDefFoundError e) {
            available = false;
            System.out.println("[Mimesis] BaritoneBridge: stop failed, disabling bridge: " + e.getMessage());
        }
    }
}

