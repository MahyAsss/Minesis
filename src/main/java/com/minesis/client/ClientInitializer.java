package com.minesis.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.minesis.entity.MinesisEntities;
import com.minesis.voice.vosk.VoskManager;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(modid = "minesis", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInitializer {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Start Vosk ASR in background — downloads native lib + model on first run,
        // then begins microphone capture. Non-blocking; logs progress to console.
        event.enqueueWork(() -> {
            java.io.File gameDir = Minecraft.getInstance().gameDirectory;
            VoskManager.initialize(gameDir);
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MinesisHuntModel.LAYER_LOCATION, MinesisHuntModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MinesisEntities.MIMESIS_ENTITY.get(),
                ctx -> new MinesisEntityRenderer(ctx));
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("jumpscare_flash", JumpscareOverlay.FLASH);
    }
}
