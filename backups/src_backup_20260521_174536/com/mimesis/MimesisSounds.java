/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.RegistryObject
 */
package com.mimesis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public class MimesisSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.SOUND_EVENTS, (String)"mimesis");
    public static final RegistryObject<SoundEvent> MIMESIS_SCREAM = SOUNDS.register("mimesis_scream", () -> SoundEvent.m_262824_((ResourceLocation)new ResourceLocation("mimesis", "mimesis_scream")));
    public static final RegistryObject<SoundEvent> MIMESIS_HURT = SOUNDS.register("mimesis_hurt", () -> SoundEvent.m_262824_((ResourceLocation)new ResourceLocation("mimesis", "mimesis_hurt")));
    public static final RegistryObject<SoundEvent> MIMESIS_SINGING = SOUNDS.register("mimesis_singing", () -> SoundEvent.m_262824_((ResourceLocation)new ResourceLocation("mimesis", "mimesis_singing")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}

