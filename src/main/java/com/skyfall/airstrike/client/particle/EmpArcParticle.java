package com.skyfall.airstrike.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class EmpArcParticle extends TextureSheetParticle {
    protected EmpArcParticle(ClientLevel level, double x, double y, double z,
                             double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx * 0.25D;
        this.yd = vy * 0.25D;
        this.zd = vz * 0.25D;
        this.lifetime = 10 + this.random.nextInt(10);
        this.quadSize = 0.65F + this.random.nextFloat() * 0.85F;
        this.rCol = 0.45F;
        this.gCol = 0.92F;
        this.bCol = 1.0F;
        this.roll = this.random.nextFloat() * ((float) Math.PI * 2F);
        this.oRoll = this.roll;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.roll += (this.random.nextFloat() - 0.5F) * 0.8F;
        super.tick();
        this.alpha = (this.age % 2 == 0) ? 1.0F : 0.45F;
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
            return new EmpArcParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
