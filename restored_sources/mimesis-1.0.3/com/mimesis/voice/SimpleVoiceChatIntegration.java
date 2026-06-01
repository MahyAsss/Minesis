/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 *  net.minecraftforge.fml.ModList
 */
package com.mimesis.voice;

import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

@OnlyIn(value=Dist.CLIENT)
public class SimpleVoiceChatIntegration {
    private static boolean isVoiceChatLoaded = false;

    public static boolean isSimpleVoiceChatLoaded() {
        return isVoiceChatLoaded;
    }

    public static void registerPlayerVoiceCapture(UUID playerUUID) {
        if (!isVoiceChatLoaded) {
            return;
        }
    }

    public static void playbackVoice(UUID playerUUID, byte[] audioData) {
        if (!isVoiceChatLoaded || audioData == null || audioData.length == 0) {
            return;
        }
    }

    public static void stopAllPlayback() {
        if (!isVoiceChatLoaded) {
            return;
        }
    }

    static {
        isVoiceChatLoaded = ModList.get().isLoaded("voicechat");
    }
}

