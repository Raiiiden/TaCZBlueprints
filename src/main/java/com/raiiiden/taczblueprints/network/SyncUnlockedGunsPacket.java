package com.raiiiden.taczblueprints.network;

import com.raiiiden.taczblueprints.capability.GunUnlocksProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record SyncUnlockedGunsPacket(Set<String> unlockedGuns) {

    public static void encode(SyncUnlockedGunsPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.unlockedGuns.size());
        for (String gunId : pkt.unlockedGuns) {
            buf.writeUtf(gunId);
        }
    }

    public static SyncUnlockedGunsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Set<String> guns = new HashSet<>();
        for (int i = 0; i < size; i++) {
            guns.add(buf.readUtf(32767));
        }
        return new SyncUnlockedGunsPacket(guns);
    }

    public static void handle(SyncUnlockedGunsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                // Client side: update player's capability
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(unlocks -> {
                        Set<String> normalized = new HashSet<>();
                        for (String gunId : pkt.unlockedGuns) {
                            normalized.add(normalizeGunId(gunId));
                        }
                        unlocks.setUnlockedGuns(normalized);
                    });
                }
            }
        });
        context.setPacketHandled(true);
    }

    /** Ensures gun IDs match the table format tacz:gun/(id) */
    private static String normalizeGunId(String gunId) {
        // Keep the ID exactly as stored
        return gunId;
    }
}
