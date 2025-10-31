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
import net.minecraftforge.event.server.ServerStartedEvent;
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

    // Map to store gun ID -> gun type for when assets aren't available
    private static final Map<ResourceLocation, String> GUN_TYPE_CACHE = new HashMap<>();
    private static final List<ResourceLocation> ALL_GUN_IDS = new ArrayList<>();

    public static final RegistryObject<Item> BLUEPRINT_PISTOL = ITEMS.register("blueprint_pistol",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Pistol"));

    public static final RegistryObject<Item> BLUEPRINT_SMG = ITEMS.register("blueprint_smg",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Smg"));

    public static final RegistryObject<Item> BLUEPRINT_RIFLE = ITEMS.register("blueprint_rifle",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Rifle"));

    public static final RegistryObject<Item> BLUEPRINT_SHOTGUN = ITEMS.register("blueprint_shotgun",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Shotgun"));

    public static final RegistryObject<Item> BLUEPRINT_SNIPER = ITEMS.register("blueprint_sniper",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Sniper"));

    public static final RegistryObject<Item> BLUEPRINT_MG = ITEMS.register("blueprint_mg",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Mg"));

    public static final RegistryObject<Item> BLUEPRINT_RPG = ITEMS.register("blueprint_rpg",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Rpg"));

    public static final RegistryObject<Item> BLUEPRINT_DEFAULT = ITEMS.register("blueprint_default",
            () -> new GunBlueprintItem(new Item.Properties().stacksTo(1), "Gun"));

    private static final Map<String, RegistryObject<Item>> TYPE_TO_BLUEPRINT = new HashMap<>();

    static {
        TYPE_TO_BLUEPRINT.put("Pistol", BLUEPRINT_PISTOL);
        TYPE_TO_BLUEPRINT.put("Smg", BLUEPRINT_SMG);
        TYPE_TO_BLUEPRINT.put("Rifle", BLUEPRINT_RIFLE);
        TYPE_TO_BLUEPRINT.put("Shotgun", BLUEPRINT_SHOTGUN);
        TYPE_TO_BLUEPRINT.put("Sniper", BLUEPRINT_SNIPER);
        TYPE_TO_BLUEPRINT.put("Mg", BLUEPRINT_MG);
        TYPE_TO_BLUEPRINT.put("Rpg", BLUEPRINT_RPG);
    }

    public static final RegistryObject<CreativeModeTab> BLUEPRINT_TAB = CREATIVE_MODE_TABS.register("blueprints",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("TaCZ Blueprints"))
                    .icon(() -> new ItemStack(BLUEPRINT_DEFAULT.get()))
                    .displayItems((params, output) -> {
                        // ALWAYS try to populate when the tab is displayed
                        populateGunsFromResourceProvider();

                        // If still empty after trying, don't show anything
                        if (ALL_GUN_IDS.isEmpty()) {
                            TaCZBlueprints.LOGGER.warn("[{}] No guns available for blueprint tab", TaCZBlueprints.MODID);
                            return;
                        }

                        List<String> typeOrder = Arrays.asList("Sniper", "Mg", "Rifle", "Shotgun", "Smg", "Pistol", "Rpg");
                        Map<String, List<ResourceLocation>> gunsByType = new HashMap<>();

                        // Group guns by their cached type
                        for (ResourceLocation gunId : ALL_GUN_IDS) {
                            String type = GUN_TYPE_CACHE.getOrDefault(gunId, "Gun");
                            gunsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(gunId);
                        }

                        // Display guns by preferred order
                        for (String type : typeOrder) {
                            List<ResourceLocation> ids = gunsByType.get(type);
                            if (ids != null) {
                                ids.sort(Comparator.comparing(ResourceLocation::getPath));
                                for (ResourceLocation id : ids) {
                                    output.accept(createBlueprintForGun(id));
                                }
                            }
                        }

                        // Display any leftover types
                        gunsByType.keySet().stream()
                                .filter(type -> !typeOrder.contains(type))
                                .sorted()
                                .forEach(type -> {
                                    List<ResourceLocation> ids = gunsByType.get(type);
                                    ids.sort(Comparator.comparing(ResourceLocation::getPath));
                                    for (ResourceLocation id : ids) {
                                        output.accept(createBlueprintForGun(id));
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
    public static void onServerStarted(ServerStartedEvent event) {
        populateGunsFromResourceProvider();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        populateGunsFromResourceProvider();
    }

    /**
     * Populates gun IDs from TaCZ's resource provider (works on both client and server now)
     */
    private static void populateGunsFromResourceProvider() {
        var resourceProvider = CommonAssetsManager.get();

        ALL_GUN_IDS.clear();
        GUN_TYPE_CACHE.clear();

        if (resourceProvider == null) {
            TaCZBlueprints.LOGGER.debug("[{}] Resource provider not available yet", TaCZBlueprints.MODID);
            return;
        }

        try {
            for (var entry : resourceProvider.getAllGuns()) {
                ResourceLocation id = entry.getKey();
                CommonGunIndex index = entry.getValue();

                // Normalize ID to include "gun/" prefix
                if (!id.getPath().startsWith("gun/")) {
                    id = new ResourceLocation(id.getNamespace(), "gun/" + id.getPath());
                }

                // Cache the gun type
                String type = "Gun";
                if (index != null && index.getType() != null && !index.getType().isEmpty()) {
                    type = capitalize(index.getType());
                }

                ALL_GUN_IDS.add(id);
                GUN_TYPE_CACHE.put(id, type);
            }

            TaCZBlueprints.LOGGER.info("[{}] Loaded {} guns for blueprints", TaCZBlueprints.MODID, ALL_GUN_IDS.size());
        } catch (Exception e) {
            TaCZBlueprints.LOGGER.error("[{}] Failed to populate gun IDs", TaCZBlueprints.MODID, e);
        }
    }

    public static List<ResourceLocation> getAllGunIds() {
        // If empty, try to populate
        if (ALL_GUN_IDS.isEmpty()) {
            populateGunsFromResourceProvider();
        }
        return ALL_GUN_IDS;
    }

    public static String getGunType(ResourceLocation gunId) {
        // Check cache first
        if (GUN_TYPE_CACHE.containsKey(gunId)) {
            return GUN_TYPE_CACHE.get(gunId);
        }

        // Try to look up from resource provider
        var resourceProvider = CommonAssetsManager.get();
        if (resourceProvider != null) {
            String path = gunId.getPath();
            if (path.startsWith("gun/")) {
                path = path.substring(4);
            }
            ResourceLocation lookupId = new ResourceLocation(gunId.getNamespace(), path);
            CommonGunIndex index = resourceProvider.getGunIndex(lookupId);

            if (index != null && index.getType() != null && !index.getType().isEmpty()) {
                String type = capitalize(index.getType());
                GUN_TYPE_CACHE.put(gunId, type); // Cache it
                return type;
            }
        }

        return "Gun"; // Default fallback
    }

    public static ItemStack createBlueprintForGun(ResourceLocation gunId) {
        // Get the gun type (from cache or lookup)
        String type = getGunType(gunId);

        // Get the appropriate blueprint item for this type
        RegistryObject<Item> blueprintItem = TYPE_TO_BLUEPRINT.getOrDefault(type, BLUEPRINT_DEFAULT);

        return GunBlueprintItem.createBlueprint(blueprintItem.get(), gunId);
    }

    public static Item getBlueprintItemForType(String type) {
        RegistryObject<Item> blueprintItem = TYPE_TO_BLUEPRINT.get(type);
        return blueprintItem != null ? blueprintItem.get() : BLUEPRINT_DEFAULT.get();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Gun";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}