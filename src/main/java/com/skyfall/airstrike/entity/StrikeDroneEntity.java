package com.skyfall.airstrike.entity;

import com.skyfall.airstrike.explosion.CinematicExplosionSystem;
import com.skyfall.airstrike.registry.ModEntities;
import com.skyfall.airstrike.registry.ModParticles;
import com.skyfall.airstrike.registry.ModSounds;
import com.skyfall.airstrike.strike.AirstrikeMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class StrikeDroneEntity extends Entity {
    private static final EntityDataAccessor<Float> TARGET_X =
            SynchedEntityData.defineId(StrikeDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y =
            SynchedEntityData.defineId(StrikeDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z =
            SynchedEntityData.defineId(StrikeDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> STRIKE_MODE =
            SynchedEntityData.defineId(StrikeDroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> EXPLOSION_POWER =
            SynchedEntityData.defineId(StrikeDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DESCENT_SPEED =
            SynchedEntityData.defineId(StrikeDroneEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUUID;
    private boolean whistlePlayed = false;
    private boolean approachPlayed = false;
    public float propellerAngle = 0.0F;
    public float prevPropellerAngle = 0.0F;
    public float rollAngle = 0.0F;
    public float prevRollAngle = 0.0F;

    public StrikeDroneEntity(EntityType<? extends StrikeDroneEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.blocksBuilding = true;
    }

    public StrikeDroneEntity(Level level, Vec3 spawnPos, Vec3 targetPos, AirstrikeMode mode, float explosionPower, UUID ownerUUID) {
        this(ModEntities.STRIKE_DRONE.get(), level);
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.setTargetPos(targetPos);
        this.setStrikeMode(mode);
        this.setExplosionPower(explosionPower);
        this.setDescentSpeed(mode.getDescentSpeed());
        this.ownerUUID = ownerUUID;
        this.updateRotationTowardTarget(targetPos.subtract(spawnPos));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_X, 0.0F);
        this.entityData.define(TARGET_Y, 64.0F);
        this.entityData.define(TARGET_Z, 0.0F);
        this.entityData.define(STRIKE_MODE, AirstrikeMode.PRECISION.getId());
        this.entityData.define(EXPLOSION_POWER, 6.0F);
        this.entityData.define(DESCENT_SPEED, 1.5F);
    }

    public void setTargetPos(Vec3 target) {
        this.entityData.set(TARGET_X, (float) target.x);
        this.entityData.set(TARGET_Y, (float) target.y);
        this.entityData.set(TARGET_Z, (float) target.z);
    }

    public Vec3 getTargetPos() {
        return new Vec3(
                this.entityData.get(TARGET_X),
                this.entityData.get(TARGET_Y),
                this.entityData.get(TARGET_Z)
        );
    }

    public void setStrikeMode(AirstrikeMode mode) {
        this.entityData.set(STRIKE_MODE, mode.getId());
    }

    public AirstrikeMode getStrikeMode() {
        return AirstrikeMode.byId(this.entityData.get(STRIKE_MODE));
    }

    public void setExplosionPower(float power) {
        this.entityData.set(EXPLOSION_POWER, power);
    }

    public float getExplosionPower() {
        return this.entityData.get(EXPLOSION_POWER);
    }

    public void setDescentSpeed(float speed) {
        this.entityData.set(DESCENT_SPEED, speed);
    }

    public float getDescentSpeed() {
        return this.entityData.get(DESCENT_SPEED);
    }

    public boolean isTargetingLedLit() {
        return (this.tickCount % 6) < 3;
    }

    @Override
    public void tick() {
        super.tick();

        this.prevPropellerAngle = this.propellerAngle;
        this.propellerAngle = (this.propellerAngle + 72.0F) % 360.0F;
        this.prevRollAngle = this.rollAngle;

        Vec3 currentPos = this.position();
        Vec3 targetPos = this.getTargetPos();
        Vec3 toTarget = targetPos.subtract(currentPos);
        double distToTarget = toTarget.length();

        float speed = this.getDescentSpeed();
        // Accelerate slightly as the drone locks into terminal dive
        if (distToTarget < 35.0D) {
            speed *= 1.2F;
        }

        Vec3 desiredVelocity = distToTarget > 0.001D ? toTarget.normalize().scale(speed) : new Vec3(0, -speed, 0);
        Vec3 currentVel = this.getDeltaMovement();
        Vec3 smoothVel = currentVel.scale(0.65D).add(desiredVelocity.scale(0.35D));
        this.setDeltaMovement(smoothVel);

        this.updateRotationTowardTarget(smoothVel);
        this.rollAngle = Mth.sin(this.tickCount * 0.25F) * 6.5F;

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.level().isClientSide) {
            spawnFlightTrailParticles();
        } else {
            handleServerFlightAudio(distToTarget);

            boolean reachedTarget = distToTarget <= Math.max(1.75D, speed * 1.15D) || this.getY() <= targetPos.y + 0.5D;
            boolean impactedTerrain = this.onGround() || this.horizontalCollision || this.verticalCollision;
            boolean timedOut = this.tickCount > 300;

            if (reachedTarget || impactedTerrain || timedOut) {
                this.detonate();
            }
        }
    }

    private void updateRotationTowardTarget(Vec3 dir) {
        if (dir.lengthSqr() > 1.0E-4D) {
            double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            float targetYaw = (float) (Mth.atan2(dir.x, dir.z) * (double) (180F / (float) Math.PI));
            float targetPitch = (float) (Mth.atan2(dir.y, horizDist) * (double) (180F / (float) Math.PI));
            this.setYRot(Mth.rotLerp(0.4F, this.getYRot(), targetYaw));
            this.setXRot(Mth.rotLerp(0.4F, this.getXRot(), targetPitch));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
    }

    private void spawnFlightTrailParticles() {
        Vec3 vel = this.getDeltaMovement();
        double backX = this.getX() - vel.x * 0.45D;
        double backY = this.getY() + 0.2D - vel.y * 0.45D;
        double backZ = this.getZ() - vel.z * 0.45D;

        // Subtle jet/propeller exhaust trail
        this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                backX + (this.random.nextDouble() - 0.5D) * 0.15D,
                backY,
                backZ + (this.random.nextDouble() - 0.5D) * 0.15D,
                -vel.x * 0.05D, 0.01D, -vel.z * 0.05D);

        if (this.getStrikeMode() == AirstrikeMode.EMP) {
            this.level().addParticle(ModParticles.EMP_ARC.get(),
                    backX, backY, backZ, 0.0D, 0.0D, 0.0D);
        } else {
            this.level().addParticle(ParticleTypes.FLAME,
                    backX, backY, backZ,
                    -vel.x * 0.08D, -vel.y * 0.08D, -vel.z * 0.08D);
        }
    }

    private void handleServerFlightAudio(double distToTarget) {
        if (this.tickCount % 16 == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.DRONE_ENGINE.get(), SoundSource.HOSTILE, 2.2F,
                    0.95F + this.random.nextFloat() * 0.15F);
        }

        if (!this.approachPlayed && distToTarget < 72.0D) {
            this.approachPlayed = true;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.DRONE_APPROACH.get(), SoundSource.HOSTILE, 3.5F, 1.0F);
        }

        if (!this.whistlePlayed && distToTarget < 42.0D) {
            this.whistlePlayed = true;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.DRONE_WHISTLE.get(), SoundSource.HOSTILE, 4.0F,
                    0.92F + this.random.nextFloat() * 0.16F);
        }
    }

    private void detonate() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            CinematicExplosionSystem.detonate(
                    serverLevel,
                    this,
                    this.position(),
                    this.getExplosionPower(),
                    this.getStrikeMode()
            );
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetX")) {
            this.setTargetPos(new Vec3(tag.getFloat("TargetX"), tag.getFloat("TargetY"), tag.getFloat("TargetZ")));
        }
        if (tag.contains("StrikeMode")) {
            this.setStrikeMode(AirstrikeMode.byId(tag.getString("StrikeMode")));
        }
        if (tag.contains("ExplosionPower")) {
            this.setExplosionPower(tag.getFloat("ExplosionPower"));
        }
        if (tag.contains("DescentSpeed")) {
            this.setDescentSpeed(tag.getFloat("DescentSpeed"));
        }
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 target = this.getTargetPos();
        tag.putFloat("TargetX", (float) target.x);
        tag.putFloat("TargetY", (float) target.y);
        tag.putFloat("TargetZ", (float) target.z);
        tag.putString("StrikeMode", this.getStrikeMode().getId());
        tag.putFloat("ExplosionPower", this.getExplosionPower());
        tag.putFloat("DescentSpeed", this.getDescentSpeed());
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D; // Visible up to 256 blocks away
    }
}
