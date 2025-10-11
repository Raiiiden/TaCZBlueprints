package com.raiiiden.taczblueprints.loot;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModLootModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TaCZBlueprints.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> BLUEPRINT_CHEST_LOOT =
            LOOT_MODIFIERS.register("blueprint_chest_loot", BlueprintLootModifier.CHEST_CODEC);

    public static void register() {
        LOOT_MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TaCZBlueprints.LOGGER.info("[{}] Loot modifiers registered", TaCZBlueprints.MODID);
    }
}
