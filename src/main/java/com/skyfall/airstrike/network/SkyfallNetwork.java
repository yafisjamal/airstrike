package com.skyfall.airstrike.network;

import com.skyfall.airstrike.SkyfallMod;
import com.skyfall.airstrike.network.packet.S2CAirstrikeStatusPacket;
import com.skyfall.airstrike.network.packet.S2CExplosionCinematicPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class SkyfallNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SkyfallMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                S2CExplosionCinematicPacket.class,
                S2CExplosionCinematicPacket::encode,
                S2CExplosionCinematicPacket::decode,
                S2CExplosionCinematicPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                packetId++,
                S2CAirstrikeStatusPacket.class,
                S2CAirstrikeStatusPacket::encode,
                S2CAirstrikeStatusPacket::decode,
                S2CAirstrikeStatusPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendToNearby(ServerLevel level, Vec3 pos, double radius, Object packet) {
        CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        pos.x, pos.y, pos.z, radius, level.dimension()
                )),
                packet
        );
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
