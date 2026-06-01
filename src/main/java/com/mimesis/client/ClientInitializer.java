package com.mimesis.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.mimesis.entity.MimesisEntities;

@Mod.EventBusSubscriber(modid = "mimesis", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientInitializer {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Setup client-specific features
        event.enqueueWork(() -> {
            // Client initialization
        });
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register the Mimesis entity renderer - using PlayerRenderer for humanoid appearance
        event.registerEntityRenderer(MimesisEntities.MIMESIS_ENTITY.get(),
                (ctx) -> new MimesisEntityRenderer(ctx));
    }
}
