package com.skyfall.airstrike.network.packet;

import com.skyfall.airstrike.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CAirstrikeStatusPacket {
    private final int phase; // 0=IDLE/CLEAR, 1=WARNING, 2=INBOUND, 3=COMPLETE
    private final String modeId;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final float radius;
    private final int dronesRemaining;
    private final int totalDrones;
    private final int countdownTicks;

    public S2CAirstrikeStatusPacket(int phase, String modeId, double targetX, double targetY, double targetZ,
                                    float radius, int dronesRemaining, int totalDrones, int countdownTicks) {
        this.phase = phase;
        this.modeId = modeId;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.radius = radius;
        this.dronesRemaining = dronesRemaining;
        this.totalDrones = totalDrones;
        this.countdownTicks = countdownTicks;
    }

    public static void encode(S2CAirstrikeStatusPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phase);
        buf.writeUtf(msg.modeId, 32);
        buf.writeDouble(msg.targetX);
        buf.writeDouble(msg.targetY);
        buf.writeDouble(msg.targetZ);
        buf.writeFloat(msg.radius);
        buf.writeInt(msg.dronesRemaining);
        buf.writeInt(msg.totalDrones);
        buf.writeInt(msg.countdownTicks);
    }

    public static S2CAirstrikeStatusPacket decode(FriendlyByteBuf buf) {
        return new S2CAirstrikeStatusPacket(
                buf.readInt(),
                buf.readUtf(32),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(S2CAirstrikeStatusPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.handleAirstrikeStatus(
                        msg.phase, msg.modeId, msg.targetX, msg.targetY, msg.targetZ,
                        msg.radius, msg.dronesRemaining, msg.totalDrones, msg.countdownTicks)));
        ctx.setPacketHandled(true);
    }
}
