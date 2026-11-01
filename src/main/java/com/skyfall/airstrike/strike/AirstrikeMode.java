package com.skyfall.airstrike.strike;

import java.util.Locale;

public enum AirstrikeMode {
    PRECISION("precision", "PRECISION STRIKE", 1, 4.0F, 1.65F, 1.85F, 0, 0xFFFF3B30),
    CLUSTER("cluster", "CLUSTER STRIKE", 6, 14.0F, 1.05F, 1.45F, 5, 0xFFFF9500),
    CARPET("carpet", "CARPET STRIKE", 10, 24.0F, 1.15F, 1.55F, 6, 0xFFFF5E3A),
    SUPPRESSION("suppression", "AREA SUPPRESSION", 14, 28.0F, 0.90F, 1.35F, 10, 0xFFFFCC00),
    EMP("emp", "EMP TACTICAL STRIKE", 3, 18.0F, 1.00F, 1.65F, 4, 0xFF32D7FF);

    private final String id;
    private final String displayName;
    private final int defaultDrones;
    private final float defaultRadius;
    private final float powerMultiplier;
    private final float descentSpeed;
    private final int spawnIntervalTicks;
    private final int hudColor;

    AirstrikeMode(String id, String displayName, int defaultDrones, float defaultRadius,
                  float powerMultiplier, float descentSpeed, int spawnIntervalTicks, int hudColor) {
        this.id = id;
        this.displayName = displayName;
        this.defaultDrones = defaultDrones;
        this.defaultRadius = defaultRadius;
        this.powerMultiplier = powerMultiplier;
        this.descentSpeed = descentSpeed;
        this.spawnIntervalTicks = spawnIntervalTicks;
        this.hudColor = hudColor;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultDrones() {
        return defaultDrones;
    }

    public float getDefaultRadius() {
        return defaultRadius;
    }

    public float getPowerMultiplier() {
        return powerMultiplier;
    }

    public float getDescentSpeed() {
        return descentSpeed;
    }

    public int getSpawnIntervalTicks() {
        return spawnIntervalTicks;
    }

    public int getHudColor() {
        return hudColor;
    }

    public AirstrikeMode next() {
        AirstrikeMode[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }

    public static AirstrikeMode byId(String name) {
        if (name == null) return PRECISION;
        String lower = name.toLowerCase(Locale.ROOT).trim();
        for (AirstrikeMode mode : values()) {
            if (mode.id.equals(lower) || mode.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return mode;
            }
        }
        return PRECISION;
    }
}
