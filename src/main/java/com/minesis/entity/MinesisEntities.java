package com.minesis.entity;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.minesis.MinesisMod;

public class MinesisEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MinesisMod.MOD_ID);

    public static final RegistryObject<EntityType<MinesisEntity>> MIMESIS_ENTITY =
            ENTITY_TYPES.register("minesis_entity", () ->
                    EntityType.Builder.of(MinesisEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("minesis_entity"));
}
