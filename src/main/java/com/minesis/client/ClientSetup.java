package com.minesis.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side setup has been moved to ClientInitializer
 * This file is kept for backward compatibility
 */
@Mod.EventBusSubscriber(modid = "minesis", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    // All client initialization is handled by ClientInitializer
}
