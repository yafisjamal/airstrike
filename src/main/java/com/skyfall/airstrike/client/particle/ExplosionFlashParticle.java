package com.skyfall.airstrike.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class ExplosionFlashParticle extends TextureSheetParticle {
    private final float maxQuadSize;

    protected ExplosionFlashParticle(ClientLevel level, double x, double y, double z,
                                     double power, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.lifetime = 8;
        this.maxQuadSize = (float) Math.max(3.5D, power * 0.95D);
        this.quadSize = this.maxQuadSize * 0.35F;
        this.rCol = 1.0F;
        this.gCol = 0.96F;
        this.bCol = 0.82F;
        this.alpha = 1.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float progress = (float) this.age / (float) this.lifetime;
        this.quadSize = this.maxQuadSize * (0.4F + 0.85F * progress);
        this.alpha = Math.max(0.0F, 1.0F - progress * progress);
        this.gCol = Math.max(0.45F, 0.96F - progress * 0.45F);
        this.bCol = Math.max(0.15F, 0.82F - progress * 0.65F);
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            double power = xSpeed > 0.1D ? xSpeed : 6.0D;
            return new ExplosionFlashParticle(level, x, y, z, power, this.sprites);
        }
    }
}
