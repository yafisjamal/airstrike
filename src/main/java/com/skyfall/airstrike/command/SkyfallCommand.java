package com.skyfall.airstrike.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.skyfall.airstrike.config.SkyfallConfig;
import com.skyfall.airstrike.item.StrikeDesignatorItem;
import com.skyfall.airstrike.strike.AirstrikeManager;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class SkyfallCommand {
    private static final String[] MODE_SUGGESTIONS = new String[]{
            "precision", "cluster", "carpet", "suppression", "emp"
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skyfall")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("strike")
                        .then(Commands.argument("target", Vec3Argument.vec3())
                                .executes(ctx -> executeStrike(ctx, Vec3Argument.getVec3(ctx, "target"), -1.0F))
                                .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0F, 256.0F))
                                        .executes(ctx -> executeStrike(
                                                ctx,
                                                Vec3Argument.getVec3(ctx, "target"),
                                                FloatArgumentType.getFloat(ctx, "radius")
                                        )))))
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.asList(MODE_SUGGESTIONS), builder))
                                .executes(SkyfallCommand::executeSetMode)))
                .then(Commands.literal("drones")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(SkyfallCommand::executeSetDrones)))
                .then(Commands.literal("radius")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(1.0F, 256.0F))
                                .executes(SkyfallCommand::executeSetRadius)))
                .then(Commands.literal("altitude")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(30.0D, 220.0D))
                                .executes(SkyfallCommand::executeSetAltitude)))
                .then(Commands.literal("power")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(1.0F, 32.0F))
                                .executes(SkyfallCommand::executeSetPower)))
                .then(Commands.literal("cancel")
                        .executes(SkyfallCommand::executeCancel))
                .then(Commands.literal("reload")
                        .executes(SkyfallCommand::executeReload))
                .then(Commands.literal("help")
                        .executes(SkyfallCommand::executeHelp))
        );
    }

    private static int executeStrike(CommandContext<CommandSourceStack> ctx, Vec3 target, float radiusOverride) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getPlayer();

        AirstrikeManager.PlayerStrikeSettings settings = player != null
                ? AirstrikeManager.getSettings(player.getUUID())
                : new AirstrikeManager.PlayerStrikeSettings();

        float effectiveRadius = radiusOverride > 0.0F ? radiusOverride : settings.radius;
        AirstrikeManager.initiateStrike(
                level,
                player,
                target,
                settings.mode,
                effectiveRadius,
                settings.droneCount,
                settings.spawnAltitude,
                settings.explosionPower
        );

        source.sendSuccess(() -> Component.literal(String.format(
                "[SKYFALL] Initiated %s at [%.1f, %.1f, %.1f] | Radius: %.1fm | UAVs: %d",
                settings.mode.getDisplayName(), target.x, target.y, target.z, effectiveRadius, settings.droneCount
        )).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeSetMode(CommandContext<CommandSourceStack> ctx) {
        String modeStr = StringArgumentType.getString(ctx, "mode");
        AirstrikeMode mode = AirstrikeMode.byId(modeStr);
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            AirstrikeManager.PlayerStrikeSettings settings = AirstrikeManager.getSettings(player.getUUID());
            settings.applyModeDefaults(mode);
            syncHeldDesignator(player, settings);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[SKYFALL] Airstrike mode set to: " + mode.getDisplayName()
        ).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int executeSetDrones(CommandContext<CommandSourceStack> ctx) {
        int count = Math.min(IntegerArgumentType.getInteger(ctx, "amount"), SkyfallConfig.MAX_DRONE_COUNT.get());
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            AirstrikeManager.PlayerStrikeSettings settings = AirstrikeManager.getSettings(player.getUUID());
            settings.droneCount = count;
            syncHeldDesignator(player, settings);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[SKYFALL] Incoming UAV count set to: " + count
        ).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int executeSetRadius(CommandContext<CommandSourceStack> ctx) {
        float radius = (float) Math.min(FloatArgumentType.getFloat(ctx, "amount"), SkyfallConfig.MAX_STRIKE_RADIUS.get());
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            AirstrikeManager.PlayerStrikeSettings settings = AirstrikeManager.getSettings(player.getUUID());
            settings.radius = radius;
            syncHeldDesignator(player, settings);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("[SKYFALL] Strike radius set to: %.1f blocks", radius)
        ).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int executeSetAltitude(CommandContext<CommandSourceStack> ctx) {
        double altitude = DoubleArgumentType.getDouble(ctx, "amount");
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            AirstrikeManager.getSettings(player.getUUID()).spawnAltitude = altitude;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("[SKYFALL] UAV deployment altitude set to: %.1f blocks above target", altitude)
        ).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int executeSetPower(CommandContext<CommandSourceStack> ctx) {
        float power = FloatArgumentType.getFloat(ctx, "amount");
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            AirstrikeManager.getSettings(player.getUUID()).explosionPower = power;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("[SKYFALL] Warhead explosion power set to: %.1f", power)
        ).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int executeCancel(CommandContext<CommandSourceStack> ctx) {
        int removed = AirstrikeManager.cancelAllStrikes(ctx.getSource().getLevel());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[SKYFALL] All active airstrikes aborted (" + removed + " sessions/UAVs cleared)."
        ).withStyle(ChatFormatting.RED), true);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            AirstrikeManager.PlayerStrikeSettings settings = AirstrikeManager.getSettings(player.getUUID());
            settings.spawnAltitude = SkyfallConfig.DEFAULT_SPAWN_ALTITUDE.get();
            settings.explosionPower = SkyfallConfig.EXPLOSION_POWER.get().floatValue();
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "[SKYFALL] Tactical configuration reloaded from skyfall-common.toml."
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("=== SKYFALL TACTICAL AIRSTRIKE COMMANDS ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        src.sendSuccess(() -> Component.literal("/skyfall strike <x> <y> <z> [radius] - Call in UAV airstrike at coordinates").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("/skyfall mode <precision|cluster|carpet|suppression|emp> - Set strike pattern").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("/skyfall drones <amount> - Configure incoming UAV count").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("/skyfall radius <amount> - Configure strike target radius").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("/skyfall altitude <amount> - Configure UAV spawn altitude").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("/skyfall power <amount> - Configure warhead explosion intensity").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("/skyfall cancel - Abort all active airstrikes & clear UAVs").withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("/skyfall reload - Sync player settings with skyfall-common.toml").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static void syncHeldDesignator(ServerPlayer player, AirstrikeManager.PlayerStrikeSettings settings) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof StrikeDesignatorItem) {
            StrikeDesignatorItem.setMode(main, settings.mode);
            StrikeDesignatorItem.setRadius(main, settings.radius);
            StrikeDesignatorItem.setDroneCount(main, settings.droneCount);
        }
    }
}
