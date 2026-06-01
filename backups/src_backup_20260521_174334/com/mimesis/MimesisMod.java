package com.mimesis;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import com.mimesis.voice.VoiceManager;
import com.mimesis.MimesisSounds;
import com.mimesis.entity.MimesisEntities;
import com.mimesis.entity.MimesisEntity;

@Mod("mimesis")
public class MimesisMod {
    public static final String MOD_ID = "mimesis";

    public MimesisMod() {
        // Initialize Voice Manager
        VoiceManager.init();

        // Register deferred registries
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MimesisEntities.ENTITY_TYPES.register(modEventBus);
        MimesisSounds.register(modEventBus);
        modEventBus.addListener(this::onEntityAttributeCreation);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(MimesisEntities.MIMESIS_ENTITY.get(), MimesisEntity.createAttributes().build());
    }
}
