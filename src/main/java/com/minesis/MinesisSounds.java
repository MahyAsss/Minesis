package com.minesis;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MinesisSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MinesisMod.MOD_ID);

    public static final RegistryObject<SoundEvent> MIMESIS_SCREAM =
            SOUNDS.register("minesis_scream", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(MinesisMod.MOD_ID, "minesis_scream")));

    public static final RegistryObject<SoundEvent> MIMESIS_HURT =
            SOUNDS.register("minesis_hurt", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(MinesisMod.MOD_ID, "minesis_hurt")));

    public static final RegistryObject<SoundEvent> MIMESIS_SINGING =
            SOUNDS.register("minesis_singing", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(MinesisMod.MOD_ID, "minesis_singing")));

    public static final RegistryObject<SoundEvent> MIMESIS_TRANSFORM =
            SOUNDS.register("minesis_transform", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(MinesisMod.MOD_ID, "minesis_transform")));

    public static final RegistryObject<SoundEvent> MIMESIS_AMBIENT =
            SOUNDS.register("minesis_ambient", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(MinesisMod.MOD_ID, "minesis_ambient")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
