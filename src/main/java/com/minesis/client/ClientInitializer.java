package com.minesis.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.minesis.entity.MinesisEntities;

@Mod.EventBusSubscriber(modid = "minesis", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInitializer {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Nametag compatibility with mods like YDM's MobHealthBar is handled via
        // MinesisEntityRenderer.shouldShowName(), which overrides any mixin that
        // LivingEntityRenderer-level mods may inject. No event hook needed here.
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
