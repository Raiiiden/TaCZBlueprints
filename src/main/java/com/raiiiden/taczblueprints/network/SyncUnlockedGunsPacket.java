package com.raiiiden.taczblueprints.network;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.raiiiden.taczblueprints.capability.GunUnlocksProvider;
import net.minecraft.client.Minecraft;
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

        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) {
                    // TaCZBlueprints.LOGGER.warn("[Blueprint] Received sync packet but player is null!");
                    return;
                }

                // Wait for capability to be attached before updating
                mc.player.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(unlocks -> {
                    unlocks.setUnlockedGuns(pkt.unlockedGuns);
                    // TaCZBlueprints.LOGGER.info("[Blueprint] Client synced {} unlocked guns", pkt.unlockedGuns.size());

                    // Debug log what was synced
                    if (!pkt.unlockedGuns.isEmpty()) {
                        // TaCZBlueprints.LOGGER.debug("[Blueprint] Unlocked guns: {}", pkt.unlockedGuns);
                    }
                });
            });
        }

        context.setPacketHandled(true);
    }
}