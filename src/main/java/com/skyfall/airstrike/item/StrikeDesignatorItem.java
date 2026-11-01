package com.skyfall.airstrike.item;

import com.skyfall.airstrike.config.SkyfallConfig;
import com.skyfall.airstrike.registry.ModSounds;
import com.skyfall.airstrike.strike.AirstrikeManager;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StrikeDesignatorItem extends Item {
    public static final double MAX_TARGET_RANGE = 160.0D;
    private static final float[] RADIUS_PRESETS = new float[]{8.0F, 14.0F, 24.0F, 36.0F, 48.0F};

    public StrikeDesignatorItem(Properties properties) {
        super(properties);
    }

    public static AirstrikeMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("StrikeMode")) {
            tag.putString("StrikeMode", AirstrikeMode.CLUSTER.getId());
        }
        return AirstrikeMode.byId(tag.getString("StrikeMode"));
    }

    public static void setMode(ItemStack stack, AirstrikeMode mode) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("StrikeMode", mode.getId());
        tag.putFloat("StrikeRadius", mode.getDefaultRadius());
        tag.putInt("DroneCount", mode.getDefaultDrones());
    }

    public static float getRadius(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("StrikeRadius")) {
            tag.putFloat("StrikeRadius", getMode(stack).getDefaultRadius());
        }
        return tag.getFloat("StrikeRadius");
    }

    public static void setRadius(ItemStack stack, float radius) {
        stack.getOrCreateTag().putFloat("StrikeRadius", radius);
    }

    public static int getDroneCount(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("DroneCount")) {
            tag.putInt("DroneCount", getMode(stack).getDefaultDrones());
        }
        return tag.getInt("DroneCount");
    }

    public static void setDroneCount(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt("DroneCount", count);
    }

    public static BlockHitResult raycastTarget(Level level, Player player, double maxRange) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxRange));
        return level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                AirstrikeMode nextMode = getMode(stack).next();
                setMode(stack, nextMode);
                AirstrikeManager.PlayerStrikeSettings settings = AirstrikeManager.getSettings(serverPlayer.getUUID());
                settings.applyModeDefaults(nextMode);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8F, 1.35F);

                serverPlayer.displayClientMessage(
                        Component.literal("[SKYFALL] ")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                .append(Component.literal("MODE: " + nextMode.getDisplayName())
                                        .withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(String.format(" (UAVs: %d | Radius: %.0fm)",
                                                nextMode.getDefaultDrones(), nextMode.getDefaultRadius()))
                                        .withStyle(ChatFormatting.GRAY)),
                        true
                );
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        BlockHitResult hit = raycastTarget(level, player, MAX_TARGET_RANGE);
        if (hit.getType() == HitResult.Type.MISS) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.literal("[SKYFALL] NO TARGET LOCK — OUT OF RANGE (>160m)")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        true
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            AirstrikeMode mode = getMode(stack);
            float radius = getRadius(stack);
            int drones = getDroneCount(stack);
            double altitude = SkyfallConfig.DEFAULT_SPAWN_ALTITUDE.get();
            float power = SkyfallConfig.EXPLOSION_POWER.get().floatValue();

            Vec3 targetVec = Vec3.atBottomCenterOf(hit.getBlockPos().above());
            AirstrikeManager.initiateStrike(serverLevel, serverPlayer, targetVec, mode, radius, drones, altitude, power);

            int cooldown = SkyfallConfig.COOLDOWN_TICKS.get();
            if (!serverPlayer.isCreative() && cooldown > 0) {
                serverPlayer.getCooldowns().addCooldown(this, cooldown);
            }

            serverPlayer.displayClientMessage(
                    Component.literal("[SKYFALL] ")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                            .append(Component.literal(String.format(
                                    "TARGET LOCKED [X:%d Y:%d Z:%d] — %s INBOUND!",
                                    hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ(),
                                    mode.getDisplayName()))
                                    .withStyle(ChatFormatting.YELLOW)),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && player.isShiftKeyDown()) {
            float current = getRadius(stack);
            float nextRadius = RADIUS_PRESETS[0];
            for (int i = 0; i < RADIUS_PRESETS.length; i++) {
                if (Math.abs(current - RADIUS_PRESETS[i]) < 1.0F) {
                    nextRadius = RADIUS_PRESETS[(i + 1) % RADIUS_PRESETS.length];
                    break;
                }
            }
            setRadius(stack, nextRadius);
            AirstrikeManager.getSettings(player.getUUID()).radius = nextRadius;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.TARGET_LOCK.get(), SoundSource.PLAYERS, 0.6F, 1.5F);
            player.displayClientMessage(
                    Component.literal(String.format("[SKYFALL] STRIKE RADIUS SET TO %.0f BLOCKS", nextRadius))
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    true
            );
            return true;
        }
        return super.onEntitySwing(stack, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        AirstrikeMode mode = getMode(stack);
        float radius = getRadius(stack);
        int drones = getDroneCount(stack);

        tooltip.add(Component.literal("MODE: " + mode.getDisplayName()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal(String.format("UAV Count: %d | Strike Radius: %.1fm", drones, radius)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.skyfall.strike_designator.desc1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.skyfall.strike_designator.desc2").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.skyfall.strike_designator.desc3").withStyle(ChatFormatting.AQUA));
    }
}
