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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
                    .icon(() -> {
                        if (!ALL_GUN_IDS.isEmpty()) {
                            return createBlueprintForGun(ALL_GUN_IDS.get(0));
                        }
                        return new ItemStack(BLUEPRINT_DEFAULT.get());
                    })
                    .displayItems((params, output) -> {
                        List<String> typeOrder = Arrays.asList("Sniper", "Mg", "Rifle", "Shotgun", "Smg", "Pistol", "Rpg");
                        Map<String, List<ResourceLocation>> gunsByType = new HashMap<>();

                        for (ResourceLocation gunId : ALL_GUN_IDS) {
                            String path = gunId.getPath();
                            if (path.startsWith("gun/")) {
                                path = path.substring(4);
                            }
                            ResourceLocation lookupId = new ResourceLocation(gunId.getNamespace(), path);

                            CommonGunIndex index = CommonAssetsManager.getInstance() != null
                                    ? CommonAssetsManager.getInstance().getGunIndex(lookupId)
                                    : null;

                            String type = "Gun";
                            if (index != null && index.getType() != null && !index.getType().isEmpty()) {
                                type = capitalize(index.getType());
                            } else {
                                TaCZBlueprints.LOGGER.warn("[Blueprint] Missing gun index or type for: {}", gunId);
                            }
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
        FMLJavaModLoadingContext.get().getModEventBus().addListener(BlueprintRegistrar::onCommonSetup);
        TaCZBlueprints.LOGGER.info("[{}] Blueprint registry initialized", TaCZBlueprints.MODID);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BlueprintRegistrar::populateGunIds);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        populateGunIds();
    }

    private static void populateGunIds() {
        CommonAssetsManager assetsManager = CommonAssetsManager.getInstance();

        ALL_GUN_IDS.clear();

        if (assetsManager == null) {
            TaCZBlueprints.LOGGER.warn("[{}] CommonAssetsManager instance is null! Using test gun.", TaCZBlueprints.MODID);
            ALL_GUN_IDS.add(new ResourceLocation(TaCZBlueprints.MODID, "test_gun"));
        } else {
            for (Map.Entry<ResourceLocation, CommonGunIndex> entry : assetsManager.getAllGuns()) {
                ResourceLocation id = entry.getKey();
                if (!id.getPath().startsWith("gun/")) {
                    id = new ResourceLocation(id.getNamespace(), "gun/" + id.getPath());
                }
                ALL_GUN_IDS.add(id);
            }
        }

        TaCZBlueprints.LOGGER.info("[{}] Total guns loaded: {}", TaCZBlueprints.MODID, ALL_GUN_IDS.size());
    }

    public static List<ResourceLocation> getAllGunIds() {
        return ALL_GUN_IDS;
    }

    public static ItemStack createBlueprintForGun(ResourceLocation gunId) {
        String path = gunId.getPath();
        if (path.startsWith("gun/")) {
            path = path.substring(4);
        }
        ResourceLocation lookupId = new ResourceLocation(gunId.getNamespace(), path);

        CommonGunIndex index = CommonAssetsManager.getInstance() != null
                ? CommonAssetsManager.getInstance().getGunIndex(lookupId)
                : null;

        String type = "Gun";
        if (index != null && index.getType() != null && !index.getType().isEmpty()) {
            type = capitalize(index.getType());
        }

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
