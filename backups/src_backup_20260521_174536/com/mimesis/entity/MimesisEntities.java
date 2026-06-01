/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.MobCategory
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.RegistryObject
 */
package com.mimesis.entity;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public class MimesisEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.ENTITY_TYPES, (String)"mimesis");
    public static final RegistryObject<EntityType<MimesisEntity>> MIMESIS_ENTITY = ENTITY_TYPES.register("mimesis_entity", () -> EntityType.Builder.m_20704_(MimesisEntity::new, (MobCategory)MobCategory.MONSTER).m_20699_(0.6f, 1.8f).m_20702_(10).m_20717_(3).m_20712_("mimesis_entity"));
}

