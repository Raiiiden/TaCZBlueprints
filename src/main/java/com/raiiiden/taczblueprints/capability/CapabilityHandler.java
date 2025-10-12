package com.raiiiden.taczblueprints.capability;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class CapabilityHandler {

    private static final ResourceLocation CAPABILITY_ID = new ResourceLocation(TaCZBlueprints.MODID, "gun_unlocks");

    @Mod.EventBusSubscriber(modid = TaCZBlueprints.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(IGunUnlocks.class);
            // TaCZBlueprints.LOGGER.info("[{}] Registered IGunUnlocks capability", TaCZBlueprints.MODID);
        }
    }

    @Mod.EventBusSubscriber(modid = TaCZBlueprints.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {

        @SubscribeEvent
        public static void onAttachPlayerCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                // Create a new provider with a fresh GunUnlocks instance
                GunUnlocksProvider provider = new GunUnlocksProvider();
                event.addCapability(CAPABILITY_ID, provider);

                // TaCZBlueprints.LOGGER.debug("[{}] Attached gun unlocks capability to player", TaCZBlueprints.MODID);
            }
        }
    }
}