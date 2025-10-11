package com.raiiiden.taczblueprints.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.raiiiden.taczblueprints.config.BlueprintConfig;
import com.raiiiden.taczblueprints.item.BlueprintRegistrar;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
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

        // CRITICAL: Only apply to chest loot tables
        if (lootTableId == null || !lootTableId.getPath().contains("chests/")) {
            return generatedLoot;
        }

        TaCZBlueprints.LOGGER.info("[Blueprint Loot] Modifier triggered for loot table: {}", lootTableId);

        // Get config values
        float chance = BlueprintConfig.SERVER.lootChestChance.get().floatValue();
        int minCount = BlueprintConfig.SERVER.lootChestMinCount.get();
        int maxCount = BlueprintConfig.SERVER.lootChestMaxCount.get();

        TaCZBlueprints.LOGGER.debug("[Blueprint Loot] Config - chance: {}, min: {}, max: {}", chance, minCount, maxCount);

        // Roll once for the chance
        float roll = random.nextFloat();
        if (roll > chance) {
            TaCZBlueprints.LOGGER.debug("[Blueprint Loot] Failed chance roll: {} > {}", roll, chance);
            return generatedLoot;
        }

        TaCZBlueprints.LOGGER.info("[Blueprint Loot] Passed chance roll: {} <= {}", roll, chance);

        // Get all available guns
        List<ResourceLocation> allGuns = BlueprintRegistrar.getAllGunIds();
        if (allGuns.isEmpty()) {
            TaCZBlueprints.LOGGER.warn("[Blueprint Loot] No guns available for loot generation!");
            return generatedLoot;
        }

        TaCZBlueprints.LOGGER.debug("[Blueprint Loot] Available guns: {}", allGuns.size());

        // Determine count
        int count = minCount;
        if (maxCount > minCount) {
            count = minCount + random.nextInt(maxCount - minCount + 1);
        }

        TaCZBlueprints.LOGGER.info("[Blueprint Loot] Adding {} blueprint(s)", count);

        // Add blueprints
        for (int i = 0; i < count; i++) {
            ResourceLocation randomGun = allGuns.get(random.nextInt(allGuns.size()));
            ItemStack blueprint = BlueprintRegistrar.createBlueprintForGun(randomGun);
            generatedLoot.add(blueprint);
            TaCZBlueprints.LOGGER.info("[Blueprint Loot] Added blueprint for gun: {}", randomGun);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CHEST_CODEC.get();
    }
}