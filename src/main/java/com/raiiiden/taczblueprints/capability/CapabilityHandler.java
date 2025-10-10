package com.raiiiden.taczblueprints.capability;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles capability registration and attachment
 * Combines both MOD bus and FORGE bus events in one class
 */
public class CapabilityHandler {

    private static final ResourceLocation CAPABILITY_ID = new ResourceLocation(TaCZBlueprints.MODID, "gun_unlocks");

    /**
     * MOD Bus Events - Register capability types during mod setup
     */
    @Mod.EventBusSubscriber(modid = TaCZBlueprints.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(IGunUnlocks.class);
            TaCZBlueprints.LOGGER.info("[{}] Registered IGunUnlocks capability", TaCZBlueprints.MODID);
        }
    }

    /**
     * FORGE Bus Events - Attach capabilities to entities during gameplay
     */
    @Mod.EventBusSubscriber(modid = TaCZBlueprints.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {

        @SubscribeEvent
        public static void onAttachPlayerCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                // Create a new provider with a fresh GunUnlocks instance
                GunUnlocksProvider provider = new GunUnlocksProvider();
                event.addCapability(CAPABILITY_ID, provider);

                TaCZBlueprints.LOGGER.debug("[{}] Attached gun unlocks capability to player", TaCZBlueprints.MODID);
            }
        }
    }
}