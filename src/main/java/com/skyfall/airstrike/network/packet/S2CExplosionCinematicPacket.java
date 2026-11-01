package com.skyfall.airstrike.network.packet;

import com.skyfall.airstrike.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CExplosionCinematicPacket {
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final boolean empMode;

    public S2CExplosionCinematicPacket(double x, double y, double z, float power, boolean empMode) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = power;
        this.empMode = empMode;
    }

    public static void encode(S2CExplosionCinematicPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.power);
        buf.writeBoolean(msg.empMode);
    }

    public static S2CExplosionCinematicPacket decode(FriendlyByteBuf buf) {
        return new S2CExplosionCinematicPacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readBoolean()
        );
    }

    public static void handle(S2CExplosionCinematicPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.handleExplosionCinematic(msg.x, msg.y, msg.z, msg.power, msg.empMode)));
        ctx.setPacketHandled(true);
    }
}
