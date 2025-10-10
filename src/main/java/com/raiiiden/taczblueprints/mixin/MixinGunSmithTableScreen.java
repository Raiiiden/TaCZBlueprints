package com.raiiiden.taczblueprints.mixin;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.raiiiden.taczblueprints.capability.GunUnlocksProvider;
import com.raiiiden.taczblueprints.capability.IGunUnlocks;
import com.raiiiden.taczblueprints.config.BlueprintConfig;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(value = GunSmithTableScreen.class, remap = false)
public class MixinGunSmithTableScreen {

    @Inject(method = "classifyRecipes", at = @At("RETURN"))
    private void onClassifyRecipes(CallbackInfo ci) {
        GunSmithTableScreen screen = (GunSmithTableScreen) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            TaCZBlueprints.LOGGER.warn("[Blueprint] Cannot classify recipes - player is null");
            return;
        }

        try {
            // Access private fields via reflection
            Field selectedRecipeListField = GunSmithTableScreen.class.getDeclaredField("selectedRecipeList");
            Field selectedRecipeField = GunSmithTableScreen.class.getDeclaredField("selectedRecipe");
            selectedRecipeListField.setAccessible(true);
            selectedRecipeField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<ResourceLocation> recipes = (List<ResourceLocation>) selectedRecipeListField.get(screen);
            if (recipes == null) {
                TaCZBlueprints.LOGGER.warn("[Blueprint] Recipe list is null");
                return;
            }

            RecipeManager recipeManager = mc.level.getRecipeManager();
            LazyOptional<IGunUnlocks> unlocksCap = mc.player.getCapability(GunUnlocksProvider.UNLOCKS);

            // Check if capability exists
            if (!unlocksCap.isPresent()) {
                TaCZBlueprints.LOGGER.warn("[Blueprint] Client capability not attached when GUI opened!");
                return;
            }

            unlocksCap.ifPresent(unlocks -> {
                Set<String> unlockedGuns = unlocks.getUnlockedGuns();
                List<String> defaultEnabled = BlueprintConfig.SERVER.getEnabledGuns();

                TaCZBlueprints.LOGGER.debug("[Blueprint] Filtering recipes - {} unlocked guns, {} default enabled",
                        unlockedGuns.size(), defaultEnabled.size());

                // Log unlocked guns for debugging
                if (!unlockedGuns.isEmpty()) {
                    TaCZBlueprints.LOGGER.debug("[Blueprint] Unlocked guns: {}", unlockedGuns);
                }

                Iterator<ResourceLocation> it = recipes.iterator();
                int removedCount = 0;

                while (it.hasNext()) {
                    ResourceLocation recipeId = it.next();
                    var recipeOpt = recipeManager.byKey(recipeId);

                    if (recipeOpt.isEmpty() || !(recipeOpt.get() instanceof GunSmithTableRecipe gunRecipe)) {
                        it.remove();
                        continue;
                    }

                    ItemStack out = gunRecipe.getOutput();
                    ResourceLocation baseItemId = ForgeRegistries.ITEMS.getKey(out.getItem());
                    String baseItem = baseItemId == null ? "null" : baseItemId.toString();

                    CompoundTag tag = out.getTag();
                    String nbtGunId = (tag != null && tag.contains("GunId")) ? tag.getString("GunId") : null;

                    // Normalize gun ID to always include "gun/" prefix
                    String normalizedGunId = null;
                    if (nbtGunId != null && !nbtGunId.isEmpty()) {
                        ResourceLocation rl = new ResourceLocation(nbtGunId);
                        String path = rl.getPath();
                        if (!path.startsWith("gun/")) {
                            path = "gun/" + path;
                        }
                        normalizedGunId = new ResourceLocation(rl.getNamespace(), path).toString();
                    } else if (!"null".equals(baseItem)) {
                        ResourceLocation rl = new ResourceLocation(baseItem);
                        String path = rl.getPath();
                        if (!path.startsWith("gun/")) {
                            path = "gun/" + path;
                        }
                        normalizedGunId = new ResourceLocation(rl.getNamespace(), path).toString();
                    }

                    // Check if gun is allowed (either default enabled or unlocked)
                    boolean allowed = normalizedGunId != null &&
                            (defaultEnabled.contains(normalizedGunId) || unlockedGuns.contains(normalizedGunId));

                    if (!allowed) {
                        TaCZBlueprints.LOGGER.debug("[Blueprint] Removing recipe {} (gun: {})", recipeId, normalizedGunId);
                        it.remove();
                        removedCount++;
                    }
                }

                TaCZBlueprints.LOGGER.debug("[Blueprint] Filtered out {} recipes, {} remaining", removedCount, recipes.size());

                // Preserve selectedRecipe if it still exists, otherwise select first
                try {
                    Object currentSelectedRecipe = selectedRecipeField.get(screen);
                    if (currentSelectedRecipe instanceof GunSmithTableRecipe currentRecipe) {
                        ResourceLocation id = currentRecipe.getId();
                        if (recipes.contains(id)) {
                            // Current selection is still valid
                            return;
                        }
                    }

                    // Select first available recipe or null
                    if (recipes.isEmpty()) {
                        selectedRecipeField.set(screen, null);
                    } else {
                        ResourceLocation firstRecipeId = recipes.get(0);
                        Method getSelectedRecipeMethod = GunSmithTableScreen.class.getDeclaredMethod("getSelectedRecipe", ResourceLocation.class);
                        getSelectedRecipeMethod.setAccessible(true);
                        Object selectedRecipe = getSelectedRecipeMethod.invoke(screen, firstRecipeId);
                        selectedRecipeField.set(screen, selectedRecipe);
                    }
                } catch (ReflectiveOperationException e) {
                    TaCZBlueprints.LOGGER.error("[Blueprint] Error updating selected recipe", e);
                }
            });

        } catch (NoSuchFieldException | IllegalAccessException e) {
            TaCZBlueprints.LOGGER.error("[Blueprint] Error accessing GunSmithTableScreen fields", e);
        }
    }
}