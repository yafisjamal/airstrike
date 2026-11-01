package com.skyfall.airstrike.registry;

import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.item.StrikeDesignatorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SkyfallMod.MOD_ID);

    public static final RegistryObject<Item> STRIKE_DESIGNATOR =
            ITEMS.register("strike_designator", () ->
                    new StrikeDesignatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
