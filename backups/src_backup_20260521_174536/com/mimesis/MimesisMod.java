/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EntityType
 *  net.minecraftforge.event.entity.EntityAttributeCreationEvent
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
 */
package com.mimesis;

import com.mimesis.MimesisSounds;
import com.mimesis.entity.MimesisEntities;
import com.mimesis.entity.MimesisEntity;
import com.mimesis.voice.VoiceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(value="mimesis")
public class MimesisMod {
    public static final String MOD_ID = "mimesis";

    public MimesisMod() {
        VoiceManager.init();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MimesisEntities.ENTITY_TYPES.register(modEventBus);
        MimesisSounds.register(modEventBus);
        modEventBus.addListener(this::onEntityAttributeCreation);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put((EntityType)MimesisEntities.MIMESIS_ENTITY.get(), MimesisEntity.createAttributes().m_22265_());
    }
}

