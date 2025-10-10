package com.raiiiden.taczblueprints.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1.0";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("taczblueprints", "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPackets() {
        int id = 0;
        CHANNEL.registerMessage(id++,
                SyncUnlockedGunsPacket.class,
                SyncUnlockedGunsPacket::encode,
                SyncUnlockedGunsPacket::decode,
                SyncUnlockedGunsPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, SyncUnlockedGunsPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(ServerPlayer sender, SyncUnlockedGunsPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
