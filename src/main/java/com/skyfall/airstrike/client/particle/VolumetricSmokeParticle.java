package com.skyfall.airstrike.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class VolumetricSmokeParticle extends TextureSheetParticle {
    private final float baseSize;
    private final float maxScale;

    protected VolumetricSmokeParticle(ClientLevel level, double x, double y, double z,
                                      double vx, double vy, double vz, boolean darkCore, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy + 0.025D + this.random.nextDouble() * 0.035D;
        this.zd = vz;
        this.friction = 0.95F;
        this.gravity = -0.015F; // Slowly rises like heated battlefield smoke
        this.lifetime = (darkCore ? 130 : 165) + this.random.nextInt(55);
        this.baseSize = (darkCore ? 1.8F : 2.3F) + this.random.nextFloat() * 1.1F;
        this.maxScale = this.baseSize * 2.35F;
        this.quadSize = this.baseSize * 0.55F;

        float shade = darkCore
                ? (0.18F + this.random.nextFloat() * 0.14F)
                : (0.58F + this.random.nextFloat() * 0.22F);
        this.rCol = shade;
        this.gCol = shade * 0.98F;
        this.bCol = shade * 0.95F;
        this.alpha = darkCore ? 0.92F : 0.78F;
        this.roll = this.random.nextFloat() * ((float) Math.PI * 2F);
        this.oRoll = this.roll;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.roll += 0.012F;
        super.tick();
        float progress = (float) this.age / (float) this.lifetime;
        // Expand smoothly as smoke rises and disperses
        this.quadSize = this.baseSize + (this.maxScale - this.baseSize) * (float) Math.sqrt(progress);
        if (progress > 0.35F) {
            float fade = (progress - 0.35F) / 0.65F;
            this.alpha = Math.max(0.0F, (1.0F - fade * fade) * 0.85F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class DarkProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public DarkProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new VolumetricSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, true, this.sprites);
        }
    }

    public static class LightProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public LightProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new VolumetricSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, false, this.sprites);
        }
    }
}
