package com.raiiiden.taczblueprints.network;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.raiiiden.taczblueprints.capability.GunUnlocksProvider;
import com.raiiiden.taczblueprints.capability.IGunUnlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;

@Mod.EventBusSubscriber(modid = TaCZBlueprints.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerBlueprintsSync {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        original.reviveCaps();

        try {
            original.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(oldCap -> {
                newPlayer.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(newCap -> {
                    Set<String> unlocked = oldCap.getUnlockedGuns();
                    newCap.setUnlockedGuns(unlocked);

                    if (newPlayer instanceof ServerPlayer serverPlayer) {
                        serverPlayer.getServer().execute(() -> {
                            ModNetworking.CHANNEL.send(
                                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                                    new SyncUnlockedGunsPacket(unlocked)
                            );
                        });
                    }
                });
            });
        } finally {
            original.invalidateCaps();
        }
    }

    // Sync blueprints on login
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        TaCZBlueprints.LOGGER.debug("[Blueprint] Player logged in, syncing unlocked guns...");

        player.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(unlocks -> {
            Set<String> unlockedGuns = unlocks.getUnlockedGuns();
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncUnlockedGunsPacket(unlockedGuns)
            );
        });
    }

    // Sync when player changes dimension (optional but recommended)
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        player.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(unlocks -> {
            Set<String> unlockedGuns = unlocks.getUnlockedGuns();
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncUnlockedGunsPacket(unlockedGuns)
            );
        });
    }
}