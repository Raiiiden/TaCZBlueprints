package com.raiiiden.taczblueprints.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.raiiiden.taczblueprints.config.BlueprintConfig;
import com.raiiiden.taczblueprints.item.BlueprintRegistrar;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class BlueprintLootModifier extends LootModifier {

    public static final Supplier<Codec<BlueprintLootModifier>> CHEST_CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst)
                    .and(Codec.STRING.fieldOf("loot_type").forGetter(m -> m.lootType))
                    .apply(inst, BlueprintLootModifier::new)
            )
    );

    private final String lootType;
    private final Random random = new Random();

    public BlueprintLootModifier(LootItemCondition[] conditionsIn, String lootType) {
        super(conditionsIn);
        this.lootType = lootType;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation lootTableId = context.getQueriedLootTableId();

        // Only apply to chest loot tables
        if (lootTableId == null || !lootTableId.getPath().contains("chests/")) {
            return generatedLoot;
        }

        // --- Lookup override or fallback to global config ---
        Map<ResourceLocation, BlueprintConfig.Server.LootOverride> overrides = BlueprintConfig.SERVER.getLootOverrides();
        BlueprintConfig.Server.LootOverride override = overrides.get(lootTableId);

        float chance;
        int minCount;
        int maxCount;

        if (override != null) {
            chance = override.chance();
            minCount = override.min();
            maxCount = override.max();
            // TaCZBlueprints.LOGGER.debug("[Blueprint Loot] Using override for {}: {}", lootTableId, override);
        } else {
            chance = BlueprintConfig.SERVER.lootChestChance.get().floatValue();
            minCount = BlueprintConfig.SERVER.lootChestMinCount.get();
            maxCount = BlueprintConfig.SERVER.lootChestMaxCount.get();
        }

        // --- Roll chance ---
        float roll = random.nextFloat();
        if (roll > chance) {
            // TaCZBlueprints.LOGGER.debug("[Blueprint Loot] {}: failed roll ({:.2f} > {:.2f})", lootTableId, roll, chance);
            return generatedLoot;
        }

        // --- Determine count ---
        int count = (maxCount > minCount)
                ? minCount + random.nextInt(maxCount - minCount + 1)
                : minCount;

        // --- Get all guns and organize by type ---
        List<ResourceLocation> allGuns = BlueprintRegistrar.getAllGunIds();
        if (allGuns.isEmpty()) {
            TaCZBlueprints.LOGGER.warn("[Blueprint Loot] No guns available for loot generation!");
            return generatedLoot;
        }

        // Group guns by type with their weights
        Map<String, List<ResourceLocation>> gunsByType = new HashMap<>();
        Map<String, Integer> typeWeights = new HashMap<>();

        for (ResourceLocation gunId : allGuns) {
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

            gunsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(gunId);

            // Store weight for this type (only once per type)
            if (!typeWeights.containsKey(type)) {
                int weight = BlueprintConfig.SERVER.getWeightForType(type);
                typeWeights.put(type, weight);
            }
        }

        // Remove types with 0 weight
        typeWeights.entrySet().removeIf(entry -> entry.getValue() <= 0);
        gunsByType.keySet().retainAll(typeWeights.keySet());

        if (gunsByType.isEmpty()) {
            TaCZBlueprints.LOGGER.warn("[Blueprint Loot] No gun types with positive weights available!");
            return generatedLoot;
        }

        // Calculate total weight
        int totalWeight = typeWeights.values().stream().mapToInt(Integer::intValue).sum();

        // Pick random blueprints based on weighted selection
        for (int i = 0; i < count; i++) {
            String selectedType = selectWeightedType(typeWeights, totalWeight);
            List<ResourceLocation> gunsOfType = gunsByType.get(selectedType);

            if (gunsOfType == null || gunsOfType.isEmpty()) {
                TaCZBlueprints.LOGGER.warn("[Blueprint Loot] No guns available for type: {}", selectedType);
                continue;
            }

            ResourceLocation randomGun = gunsOfType.get(random.nextInt(gunsOfType.size()));
            ItemStack blueprint = BlueprintRegistrar.createBlueprintForGun(randomGun);
            generatedLoot.add(blueprint);
            // TaCZBlueprints.LOGGER.debug("[Blueprint Loot] Added {} blueprint for gun: {}", selectedType, randomGun);
        }

        // TaCZBlueprints.LOGGER.info("[Blueprint Loot] Added {} blueprint(s) to {}", count, lootTableId);
        return generatedLoot;
    }

    private String selectWeightedType(Map<String, Integer> typeWeights, int totalWeight) {
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<String, Integer> entry : typeWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }

        // Fallback (shouldn't happen but just in case)
        return typeWeights.keySet().iterator().next();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Gun";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CHEST_CODEC.get();
    }
}