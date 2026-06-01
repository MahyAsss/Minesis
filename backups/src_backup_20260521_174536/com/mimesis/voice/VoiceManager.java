/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.mimesis.voice;

import com.mimesis.voice.VoiceCaptureHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="mimesis", bus=Mod.EventBusSubscriber.Bus.FORGE, value={Dist.CLIENT})
public class VoiceManager {
    private static boolean voiceChatAvailable = false;
    private static final Map<UUID, VoiceCaptureHandler> activeCaptureHandlers = new HashMap<UUID, VoiceCaptureHandler>();

    public static void init() {
        try {
            Class.forName("de.maxhenkel.voicechat.api.VoicechatPlugin");
            voiceChatAvailable = true;
        }
        catch (ClassNotFoundException e) {
            voiceChatAvailable = false;
        }
    }

    public static boolean isVoiceChatAvailable() {
        return voiceChatAvailable;
    }

    public static void registerCaptureHandler(UUID playerUUID, VoiceCaptureHandler handler) {
        if (voiceChatAvailable) {
            activeCaptureHandlers.put(playerUUID, handler);
        }
    }

    public static void unregisterCaptureHandler(UUID playerUUID) {
        VoiceCaptureHandler handler = activeCaptureHandlers.remove(playerUUID);
        if (handler != null) {
            handler.stop();
        }
    }

    public static VoiceCaptureHandler getCaptureHandler(UUID playerUUID) {
        return activeCaptureHandlers.get(playerUUID);
    }

    public static VoiceCaptureHandler getOrCreateCaptureHandler(UUID playerUUID, String playerName) {
        VoiceCaptureHandler handler = activeCaptureHandlers.computeIfAbsent(playerUUID, id -> new VoiceCaptureHandler((UUID)id, playerName));
        if (!handler.isCapturing()) {
            handler.start();
        }
        return handler;
    }

    public static void stopAllCaptures() {
        for (VoiceCaptureHandler handler : activeCaptureHandlers.values()) {
            handler.stop();
        }
        activeCaptureHandlers.clear();
    }
}

