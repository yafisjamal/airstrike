package com.skyfall.airstrike.event;

import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.command.SkyfallCommand;
import com.skyfall.airstrike.strike.AirstrikeManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SkyfallMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SkyfallCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AirstrikeManager.tickServer();
        }
    }
}
