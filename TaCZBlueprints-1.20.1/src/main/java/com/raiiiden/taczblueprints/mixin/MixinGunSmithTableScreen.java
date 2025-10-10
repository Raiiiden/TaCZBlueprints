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

        try {
            // Access private fields
            Field selectedRecipeListField = GunSmithTableScreen.class.getDeclaredField("selectedRecipeList");
            Field selectedRecipeField = GunSmithTableScreen.class.getDeclaredField("selectedRecipe");
            selectedRecipeListField.setAccessible(true);
            selectedRecipeField.setAccessible(true);

            List<ResourceLocation> recipes = (List<ResourceLocation>) selectedRecipeListField.get(screen);
            if (recipes == null) return;

            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            LazyOptional<IGunUnlocks> unlocksCap = GunUnlocksProvider.get(Minecraft.getInstance().player);

            if (!unlocksCap.isPresent()) {
                TaCZBlueprints.LOGGER.warn("[{}] Client capability missing when classifyRecipes ran! The GUI may not see unlocks yet.", TaCZBlueprints.MODID);
            }

            unlocksCap.ifPresent(unlocks -> {
                Set<String> unlockedGuns = unlocks.getUnlockedGuns();
                List<String> defaultEnabled = BlueprintConfig.SERVER.getEnabledGuns();

                Iterator<ResourceLocation> it = recipes.iterator();

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

                    String normalizedGunId = null;
                    if (nbtGunId != null && !nbtGunId.isEmpty()) {
                        ResourceLocation rl = new ResourceLocation(nbtGunId);
                        String path = rl.getPath();
                        if (!path.startsWith("gun/")) path = "gun/" + path;
                        normalizedGunId = new ResourceLocation(rl.getNamespace(), path).toString();
                    } else if (!"null".equals(baseItem)) {
                        ResourceLocation rl = new ResourceLocation(baseItem);
                        String path = rl.getPath();
                        if (!path.startsWith("gun/")) path = "gun/" + path;
                        normalizedGunId = new ResourceLocation(rl.getNamespace(), path).toString();
                    }

                    boolean allowed = normalizedGunId != null &&
                            (defaultEnabled.contains(normalizedGunId) || unlockedGuns.contains(normalizedGunId));

                    if (!allowed) {
                        it.remove();
                    }
                }

                // Preserve selectedRecipe if it still exists
                try {
                    Object currentSelectedRecipe = selectedRecipeField.get(screen);
                    if (currentSelectedRecipe instanceof GunSmithTableRecipe currentRecipe) {
                        ResourceLocation id = currentRecipe.getId();
                        if (recipes.contains(id)) {
                            // Already valid, keep it
                            return;
                        }
                    }

                    // Otherwise select first available recipe or null
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
                    e.printStackTrace();
                }
            });

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
