package com.minesis;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MinesisItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MinesisMod.MOD_ID);

    public static final RegistryObject<RecordItem> MUSIC_DISC_ECHOES_BELOW =
            ITEMS.register("music_disc_echoes_below", () ->
                    new RecordItem(12, () -> MinesisSounds.ECHOES_BELOW.get(),
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5090));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
