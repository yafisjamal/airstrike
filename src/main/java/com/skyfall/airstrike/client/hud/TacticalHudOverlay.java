package com.skyfall.airstrike.client.hud;

import com.skyfall.airstrike.client.ClientStrikeState;
import com.skyfall.airstrike.item.StrikeDesignatorItem;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TacticalHudOverlay implements IGuiOverlay {

    private static final int BG_DARK = 0xB808100C;
    private static final int BORDER_GREEN = 0xFF28FF64;
    private static final int BORDER_RED = 0xFFFF3B30;
    private static final int TEXT_GREEN = 0xFF55FF88;
    private static final int TEXT_AMBER = 0xFFFFCC33;
    private static final int TEXT_RED = 0xFFFF453A;
    private static final int TEXT_MUTED = 0xFF98B0A0;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack designator = mainHand.getItem() instanceof StrikeDesignatorItem ? mainHand
                : (offHand.getItem() instanceof StrikeDesignatorItem ? offHand : ItemStack.EMPTY);

        boolean holdingDesignator = !designator.isEmpty();
        boolean strikeActive = ClientStrikeState.strikePhase > 0;

        if (!holdingDesignator && !strikeActive) {
            return;
        }

        Font font = mc.font;
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        boolean blinkOn = (gameTime / 8L) % 2L == 0L;

        // 1. Render Center Tactical Targeting Reticle when holding Strike Designator
        if (holdingDesignator) {
            BlockHitResult hit = StrikeDesignatorItem.raycastTarget(mc.level, player, StrikeDesignatorItem.MAX_TARGET_RANGE);
            boolean locked = hit.getType() != HitResult.Type.MISS;
            renderCenterReticle(graphics, font, screenWidth, screenHeight, locked, hit, player, designator);
            renderTelemetryPanel(graphics, font, screenWidth, screenHeight, locked, hit, player, designator);
        }

        // 2. Render Top-Center Call of Duty Killstreak Alert Banner when an airstrike is active
        if (strikeActive) {
            renderActiveStrikeBanner(graphics, font, screenWidth, blinkOn);
        }
    }

    private void renderCenterReticle(GuiGraphics graphics, Font font, int sw, int sh,
                                     boolean locked, BlockHitResult hit, Player player, ItemStack stack) {
        int cx = sw / 2;
        int cy = sh / 2;
        int color = locked ? BORDER_GREEN : BORDER_RED;

        // Corner brackets of tactical optical sight
        int bSize = 18;
        int bLen = 6;
        // Top-Left
        graphics.fill(cx - bSize, cy - bSize, cx - bSize + bLen, cy - bSize + 1, color);
        graphics.fill(cx - bSize, cy - bSize, cx - bSize + 1, cy - bSize + bLen, color);
        // Top-Right
        graphics.fill(cx + bSize - bLen, cy - bSize, cx + bSize, cy - bSize + 1, color);
        graphics.fill(cx + bSize - 1, cy - bSize, cx + bSize, cy - bSize + bLen, color);
        // Bottom-Left
        graphics.fill(cx - bSize, cy + bSize - 1, cx - bSize + bLen, cy + bSize, color);
        graphics.fill(cx - bSize, cy + bSize - bLen, cx - bSize + 1, cy + bSize, color);
        // Bottom-Right
        graphics.fill(cx + bSize - bLen, cy + bSize - 1, cx + bSize, cy + bSize, color);
        graphics.fill(cx + bSize - 1, cy + bSize - bLen, cx + bSize, cy + bSize, color);

        // Inner mil-dot ticks
        graphics.fill(cx - 10, cy, cx - 4, cy + 1, color);
        graphics.fill(cx + 5, cy, cx + 11, cy + 1, color);
        graphics.fill(cx, cy - 10, cx + 1, cy - 4, color);
        graphics.fill(cx, cy + 5, cx + 1, cy + 11, color);

        if (locked) {
            double dist = player.getEyePosition().distanceTo(hit.getLocation());
            String rangeText = String.format("RNG %.1fm", dist);
            graphics.drawString(font, rangeText, cx + 22, cy - 4, TEXT_GREEN, true);
        } else {
            graphics.drawString(font, "NO LOCK", cx + 22, cy - 4, TEXT_RED, true);
        }
    }

    private void renderTelemetryPanel(GuiGraphics graphics, Font font, int sw, int sh,
                                      boolean locked, BlockHitResult hit, Player player, ItemStack stack) {
        int panelW = 188;
        int panelH = 86;
        int px = 12;
        int py = sh - panelH - 14;

        AirstrikeMode mode = StrikeDesignatorItem.getMode(stack);
        float radius = StrikeDesignatorItem.getRadius(stack);
        int drones = StrikeDesignatorItem.getDroneCount(stack);

        // Translucent dark military panel with tactical accent bar
        graphics.fill(px, py, px + panelW, py + panelH, BG_DARK);
        graphics.fill(px, py, px + 3, py + panelH, mode.getHudColor());
        graphics.fill(px, py, px + panelW, py + 1, BORDER_GREEN);
        graphics.fill(px, py + panelH - 1, px + panelW, py + panelH, BORDER_GREEN);

        graphics.drawString(font, "SKYFALL // TAC-LINK v2.4", px + 8, py + 6, TEXT_GREEN, true);
        graphics.drawString(font, "MODE: " + mode.getDisplayName(), px + 8, py + 19, mode.getHudColor(), true);

        if (locked) {
            BlockPos bp = hit.getBlockPos();
            graphics.drawString(font, String.format("TGT : X:%d Y:%d Z:%d", bp.getX(), bp.getY(), bp.getZ()),
                    px + 8, py + 32, TEXT_GREEN, true);
        } else {
            graphics.drawString(font, "TGT : --- ACQUIRING ---", px + 8, py + 32, TEXT_RED, true);
        }

        graphics.drawString(font, String.format("RAD : %.0fM  |  UAVs : %d", radius, drones),
                px + 8, py + 45, TEXT_AMBER, true);
        graphics.drawString(font, "[R-CLICK] FIRE   [SNEAK+R] MODE", px + 8, py + 60, TEXT_MUTED, false);
        graphics.drawString(font, "[SNEAK+L-CLICK] CYCLE RADIUS", px + 8, py + 71, TEXT_MUTED, false);
    }

    private void renderActiveStrikeBanner(GuiGraphics graphics, Font font, int sw, boolean blinkOn) {
        int bannerW = 246;
        int bannerH = 44;
        int bx = (sw - bannerW) / 2;
        int by = 10;

        int phase = ClientStrikeState.strikePhase;
        int borderColor = (phase == 1) ? (blinkOn ? BORDER_RED : TEXT_AMBER)
                : (phase == 2 ? BORDER_RED : BORDER_GREEN);

        graphics.fill(bx, by, bx + bannerW, by + bannerH, BG_DARK);
        graphics.fill(bx, by, bx + bannerW, by + 2, borderColor);
        graphics.fill(bx, by + bannerH - 2, bx + bannerW, by + bannerH, borderColor);

        String title;
        int titleColor;
        if (phase == 1) {
            float sec = ClientStrikeState.warningCountdownTicks / 20.0F;
            title = String.format("WARNING: %s INBOUND [T-%.1fs]",
                    ClientStrikeState.activeMode.getDisplayName(), sec);
            titleColor = blinkOn ? TEXT_RED : TEXT_AMBER;
        } else if (phase == 2) {
            title = String.format("STRIKE IN PROGRESS — UAVs ACTIVE [%d/%d]",
                    ClientStrikeState.dronesRemaining, ClientStrikeState.totalDrones);
            titleColor = TEXT_RED;
        } else {
            title = "BDA: STRIKE COMPLETE — GOOD EFFECT ON TARGET";
            titleColor = TEXT_GREEN;
        }

        String coords = String.format("TARGET ZONE: [X:%.0f Y:%.0f Z:%.0f] | RADIUS: %.0fM",
                ClientStrikeState.activeTargetPos.x,
                ClientStrikeState.activeTargetPos.y,
                ClientStrikeState.activeTargetPos.z,
                ClientStrikeState.activeRadius);

        int titleW = font.width(title);
        int coordW = font.width(coords);
        graphics.drawString(font, title, bx + (bannerW - titleW) / 2, by + 9, titleColor, true);
        graphics.drawString(font, coords, bx + (bannerW - coordW) / 2, by + 24, TEXT_MUTED, true);
    }
}
