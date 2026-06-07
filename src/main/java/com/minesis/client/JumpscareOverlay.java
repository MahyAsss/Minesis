package com.minesis.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "minesis", value = Dist.CLIENT)
public class JumpscareOverlay {

    private static volatile int flashTimer = 0;
    private static volatile int shakeTimer = 0;

    public static final IGuiOverlay FLASH = (ForgeGui gui, GuiGraphics guiGraphics,
            float partialTick, int screenWidth, int screenHeight) -> {
        if (flashTimer <= 0) return;
        float t = (float) flashTimer / 60.0f;
        int alpha = (int) (t * t * 205);
        guiGraphics.fill(0, 0, screenWidth, screenHeight, (alpha << 24) | 0x00CC0000);
        flashTimer--;
    };

    public static void trigger() {
        flashTimer = 60;
        shakeTimer = 70;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTimer <= 0) return;
        float intensity = Math.min(shakeTimer, 30) / 30.0f;
        float shake = (float) Math.sin(shakeTimer * 1.9) * intensity * 5.0f;
        event.setPitch(event.getPitch() + shake);
        event.setRoll(event.getRoll() + shake * 0.4f);
        shakeTimer--;
    }
}
