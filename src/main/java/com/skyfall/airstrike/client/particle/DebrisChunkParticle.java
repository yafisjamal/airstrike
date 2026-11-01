package com.skyfall.airstrike.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class DebrisChunkParticle extends TextureSheetParticle {
    private final float rotSpeed;

    protected DebrisChunkParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 1.25F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.lifetime = 45 + this.random.nextInt(35);
        this.quadSize = 0.22F + this.random.nextFloat() * 0.25F;
        this.rotSpeed = (this.random.nextFloat() - 0.5F) * 0.35F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.roll += this.rotSpeed;
        super.tick();
        if (this.onGround) {
            this.xd *= 0.65D;
            this.zd *= 0.65D;
        }
        if (this.age > this.lifetime - 12) {
            this.alpha = Math.max(0.0F, (float) (this.lifetime - this.age) / 12.0F);
        }
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
            return new DebrisChunkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
