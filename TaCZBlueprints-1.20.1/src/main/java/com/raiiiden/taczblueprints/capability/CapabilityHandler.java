package com.raiiiden.taczblueprints.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "taczblueprints", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityHandler {

    @SubscribeEvent
    public static void onAttachPlayerCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            LazyOptional<IGunUnlocks> unlocks = LazyOptional.of(GunUnlocks::new);
            event.addCapability(new ResourceLocation("taczblueprints", "gun_unlocks"), new GunUnlocksProvider());
        }
    }
}
