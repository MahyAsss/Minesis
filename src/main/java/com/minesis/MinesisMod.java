package com.minesis;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import com.minesis.voice.VoiceManager;
import com.minesis.MinesisSounds;
import com.minesis.entity.MinesisEntities;
import com.minesis.entity.MinesisEntity;
import com.minesis.network.VoiceNetworking;

@Mod("minesis")
public class MinesisMod {
    public static final String MOD_ID = "minesis";

    public MinesisMod() {
        // Initialize Voice Manager
        VoiceManager.init();

        // Register deferred registries
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinesisEntities.ENTITY_TYPES.register(modEventBus);
        MinesisSounds.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(VoiceNetworking::init);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(MinesisEntities.MIMESIS_ENTITY.get(), MinesisEntity.createAttributes().build());
    }
}
