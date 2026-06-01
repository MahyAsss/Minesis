/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EntityType
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 */
package com.mimesis.client;

import com.mimesis.client.MimesisEntityRenderer;
import com.mimesis.entity.MimesisEntities;
import com.mimesis.network.VoiceNetworking;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="mimesis", bus=Mod.EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class ClientInitializer {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        VoiceNetworking.init();
        event.enqueueWork(() -> {});
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)MimesisEntities.MIMESIS_ENTITY.get(), ctx -> new MimesisEntityRenderer(ctx));
    }
}

