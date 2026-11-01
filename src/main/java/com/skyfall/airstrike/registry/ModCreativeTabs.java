package com.skyfall.airstrike.registry;

import com.skyfall.airstrike.SkyfallMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkyfallMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SKYFALL_TAB =
            CREATIVE_MODE_TABS.register("skyfall_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.STRIKE_DESIGNATOR.get()))
                    .title(Component.translatable("itemGroup.skyfall"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.STRIKE_DESIGNATOR.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
