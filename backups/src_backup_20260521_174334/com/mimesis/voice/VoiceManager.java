package com.mimesis.voice;

import java.util.*;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * Manages interaction with Simple Voice Chat API
 * Handles voice capture and playback integration
 */
@Mod.EventBusSubscriber(modid = "mimesis", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VoiceManager {
    private static boolean voiceChatAvailable = false;
    private static final Map<UUID, VoiceCaptureHandler> activeCaptureHandlers = new HashMap<>();

    public static void init() {
        // Check if Simple Voice Chat is available
        try {
            Class.forName("de.maxhenkel.voicechat.api.VoicechatPlugin");
            voiceChatAvailable = true;
        } catch (ClassNotFoundException e) {
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
        VoiceCaptureHandler handler = activeCaptureHandlers.computeIfAbsent(playerUUID,
                id -> new VoiceCaptureHandler(id, playerName));
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
