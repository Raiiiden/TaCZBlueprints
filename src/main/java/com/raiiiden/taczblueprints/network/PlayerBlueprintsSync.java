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

    /**
     * Persist blueprints on death - CRITICAL for keeping unlocks
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Only handle death (not dimension changes)
        if (!event.isWasDeath()) {
            return;
        }

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        TaCZBlueprints.LOGGER.debug("[Blueprint] Player died, cloning capability data...");

        // Revive old capabilities temporarily to access them
        original.reviveCaps();

        try {
            // Get capability from both old and new player
            original.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(oldCap -> {
                newPlayer.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(newCap -> {
                    // Copy all unlocked guns
                    Set<String> unlocked = oldCap.getUnlockedGuns();
                    newCap.setUnlockedGuns(unlocked);

                    TaCZBlueprints.LOGGER.info("[Blueprint] Cloned {} unlocked guns to new player instance", unlocked.size());

                    // Sync to client with slight delay to ensure capability is attached on client
                    if (newPlayer instanceof ServerPlayer serverPlayer) {
                        // Schedule sync for next tick to ensure client capability is ready
                        serverPlayer.getServer().execute(() -> {
                            ModNetworking.CHANNEL.send(
                                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                                    new SyncUnlockedGunsPacket(unlocked)
                            );
                            TaCZBlueprints.LOGGER.debug("[Blueprint] Synced unlocked guns to client after death (delayed)");
                        });
                    }
                });
            });
        } finally {
            // Always invalidate old capabilities
            original.invalidateCaps();
        }
    }

    /**
     * Sync blueprints on login
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // Only run on server side
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        TaCZBlueprints.LOGGER.debug("[Blueprint] Player logged in, syncing unlocked guns...");

        player.getCapability(GunUnlocksProvider.UNLOCKS).ifPresent(unlocks -> {
            Set<String> unlockedGuns = unlocks.getUnlockedGuns();

            // Send to client
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncUnlockedGunsPacket(unlockedGuns)
            );

            TaCZBlueprints.LOGGER.info("[Blueprint] Synced {} unlocked guns to {} on login",
                    unlockedGuns.size(), serverPlayer.getName().getString());
        });
    }

    /**
     * Sync when player changes dimension (optional but recommended)
     */
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
            TaCZBlueprints.LOGGER.debug("[Blueprint] Re-synced unlocked guns after dimension change");
        });
    }
}