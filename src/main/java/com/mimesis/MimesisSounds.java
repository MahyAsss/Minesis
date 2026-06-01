package com.mimesis;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MimesisSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MimesisMod.MOD_ID);

    public static final RegistryObject<SoundEvent> MIMESIS_SCREAM =
            SOUNDS.register("mimesis_scream", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MimesisMod.MOD_ID, "mimesis_scream")));

    public static final RegistryObject<SoundEvent> MIMESIS_HURT =
            SOUNDS.register("mimesis_hurt", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MimesisMod.MOD_ID, "mimesis_hurt")));

    public static final RegistryObject<SoundEvent> MIMESIS_SINGING =
            SOUNDS.register("mimesis_singing", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MimesisMod.MOD_ID, "mimesis_singing")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
