package com.mimesis.entity;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.mimesis.MimesisMod;

public class MimesisEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MimesisMod.MOD_ID);

    public static final RegistryObject<EntityType<MimesisEntity>> MIMESIS_ENTITY =
            ENTITY_TYPES.register("mimesis_entity", () ->
                    EntityType.Builder.of(MimesisEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("mimesis_entity"));
}
