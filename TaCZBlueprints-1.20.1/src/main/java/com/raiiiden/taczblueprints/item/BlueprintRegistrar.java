package com.raiiiden.taczblueprints.item;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;

public class BlueprintRegistrar {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            net.minecraftforge.registries.ForgeRegistries.ITEMS,
            TaCZBlueprints.MODID
    );

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            TaCZBlueprints.MODID
    );

    private static final List<ResourceLocation> ALL_GUN_IDS = new ArrayList<>();

    public static final RegistryObject<Item> GUN_BLUEPRINT = ITEMS.register("gun_blueprint",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<CreativeModeTab> BLUEPRINT_TAB = CREATIVE_MODE_TABS.register("blueprints",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("TaCZ Blueprints"))
                    .icon(() -> {
                        if (!ALL_GUN_IDS.isEmpty()) {
                            return GunBlueprintItem.createBlueprint(GUN_BLUEPRINT.get(), ALL_GUN_IDS.get(0));
                        }
                        return new ItemStack(GUN_BLUEPRINT.get());
                    })
                    .displayItems((params, output) -> {
                        Map<String, List<ResourceLocation>> gunsByType = new TreeMap<>();
                        for (ResourceLocation gunId : ALL_GUN_IDS) {
                            CommonGunIndex index = CommonAssetsManager.getInstance().getGunIndex(gunId);
                            String type = "Gun"; // default fallback
                            if (index != null && index.getType() != null) {
                                type = capitalize(index.getType());
                            } else {
                                TaCZBlueprints.LOGGER.warn("Missing gun index or type for: {}", gunId);
                            }
                            gunsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(gunId);
                        }

                        // Sort and add to creative tab
                        gunsByType.forEach((type, ids) -> {
                            ids.sort(Comparator.comparing(ResourceLocation::getPath));
                            for (ResourceLocation id : ids) {
                                output.accept(GunBlueprintItem.createBlueprint(GUN_BLUEPRINT.get(), id));
                            }
                        });
                    })
                    .build()
    );

    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        CREATIVE_MODE_TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(BlueprintRegistrar.class);
        TaCZBlueprints.LOGGER.info("[{}] Blueprint registry initialized", TaCZBlueprints.MODID);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        CommonAssetsManager assetsManager = CommonAssetsManager.getInstance();
        if (assetsManager == null) {
            TaCZBlueprints.LOGGER.warn("[{}] CommonAssetsManager instance is null!", TaCZBlueprints.MODID);
            return;
        }

        ALL_GUN_IDS.clear();
        for (Map.Entry<ResourceLocation, CommonGunIndex> entry : assetsManager.getAllGuns()) {
            ResourceLocation id = entry.getKey();
            // Ensure consistent prefix
            if (!id.getPath().startsWith("gun/")) {
                id = new ResourceLocation(id.getNamespace(), "gun/" + id.getPath());
            }
            ALL_GUN_IDS.add(id);
        }

        TaCZBlueprints.LOGGER.info("[{}] Total guns found: {}", TaCZBlueprints.MODID, ALL_GUN_IDS.size());
    }

    public static List<ResourceLocation> getAllGunIds() {
        return ALL_GUN_IDS;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Gun";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
