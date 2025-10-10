package com.raiiiden.taczblueprints.network;

import com.raiiiden.taczblueprints.capability.GunUnlocksProvider;
import com.raiiiden.taczblueprints.capability.IGunUnlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;

@Mod.EventBusSubscriber
public class PlayerBlueprintsSync {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        LazyOptional<IGunUnlocks> unlocksCap = player.getCapability(GunUnlocksProvider.UNLOCKS);
        unlocksCap.ifPresent(unlocks -> {
            Set<String> unlockedGuns = unlocks.getUnlockedGuns();
            // Send to client
            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncUnlockedGunsPacket(unlockedGuns));
        });
    }
}
