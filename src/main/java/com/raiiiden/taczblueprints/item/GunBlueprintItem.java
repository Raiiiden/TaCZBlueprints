package com.raiiiden.taczblueprints.item;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import com.raiiiden.taczblueprints.capability.GunUnlocksProvider;
import com.raiiiden.taczblueprints.capability.IGunUnlocks;
import com.raiiiden.taczblueprints.network.ModNetworking;
import com.raiiiden.taczblueprints.network.SyncUnlockedGunsPacket;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class GunBlueprintItem extends Item {

    private final String gunType;

    public GunBlueprintItem(Properties properties, String gunType) {
        super(properties);
        this.gunType = gunType;
    }

    public String getGunType() {
        return gunType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            String storedGunId = getGunId(stack);
            if (storedGunId == null || storedGunId.isEmpty()) {
                // TaCZBlueprints.LOGGER.warn("[Blueprint] Invalid blueprint used by {}", player.getName().getString());
                player.displayClientMessage(Component.literal("§cInvalid blueprint!"), true);
                return InteractionResultHolder.fail(stack);
            }

            // TaCZBlueprints.LOGGER.debug("[Blueprint] Using blueprint for storedGunId='{}'", storedGunId);

            final String gunIdForUnlock = storedGunId; // full ID including gun/

            // Get the display name for the gun
            Component gunName = getGunDisplayName(storedGunId);

            LazyOptional<IGunUnlocks> cap = GunUnlocksProvider.get(player);
            cap.ifPresent(unlocks -> {
                if (!unlocks.isUnlocked(gunIdForUnlock)) {
                    unlocks.unlockGun(gunIdForUnlock);
                    // TaCZBlueprints.LOGGER.debug("[Blueprint] Gun unlocked: {}", gunIdForUnlock);
                    player.displayClientMessage(
                            Component.literal("§aUnlocked gun: ").append(gunName),
                            true
                    );

                    if (!player.isCreative()) stack.shrink(1);

                    if (player instanceof ServerPlayer serverPlayer) {
                        ModNetworking.sendToPlayer(serverPlayer, new SyncUnlockedGunsPacket(unlocks.getUnlockedGuns()));
                        // TaCZBlueprints.LOGGER.info("[Blueprint] Synced unlocked guns to {}", player.getName().getString());
                    }
                } else {
                    // TaCZBlueprints.LOGGER.debug("[Blueprint] Gun already unlocked: {}", gunIdForUnlock);
                    player.displayClientMessage(
                            Component.literal("§eGun already unlocked: ").append(gunName),
                            true
                    );
                }
            });
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        String storedGunId = getGunId(stack);
        if (storedGunId == null || storedGunId.isEmpty()) {
            TaCZBlueprints.LOGGER.warn("[Blueprint] Missing GunId for stack {}", stack);
            return Component.literal("§cInvalid Blueprint");
        }

        // Display name is based on the item's gun type
        return Component.literal(gunType + " Blueprint");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        String storedGunId = getGunId(stack);
        if (storedGunId == null || storedGunId.isEmpty()) {
            return;
        }

        // Remove "gun/" prefix for lookup
        ResourceLocation lookupId = getGunIdForLookup(storedGunId);
        CommonGunIndex index = CommonAssetsManager.getInstance().getGunIndex(lookupId);

        if (index != null && index.getPojo() != null && index.getPojo().getName() != null && !index.getPojo().getName().isEmpty()) {
            String translationKey = index.getPojo().getName();
            // Use translatable component to resolve the translation key
            tooltip.add(Component.translatable(translationKey).withStyle(style -> style.withColor(0x808080)));
        } else {
            TaCZBlueprints.LOGGER.debug("[Blueprint] No display name found for gunId={} (lookup: {})", storedGunId, lookupId);
        }
    }

    /**
     * Gets the display name for a gun (translatable component)
     */
    private static Component getGunDisplayName(String storedGunId) {
        ResourceLocation lookupId = getGunIdForLookup(storedGunId);
        CommonGunIndex index = CommonAssetsManager.getInstance().getGunIndex(lookupId);

        if (index != null && index.getPojo() != null && index.getPojo().getName() != null && !index.getPojo().getName().isEmpty()) {
            String translationKey = index.getPojo().getName();
            return Component.translatable(translationKey);
        } else {
            // Fallback to gun ID if no translation found
            return Component.literal(storedGunId);
        }
    }

    /**
     * Gets the stored gun ID from the item stack (includes "gun/" prefix)
     * This is used for unlocking guns
     */
    public static String getGunId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("GunId")) {
            return tag.getString("GunId");
        }
        return null;
    }

    /**
     * Converts the stored gun ID to the format needed for CommonGunIndex lookup (removes "gun/" prefix)
     */
    private static ResourceLocation getGunIdForLookup(String storedGunId) {
        // Remove "gun/" from the path for lookup
        // e.g., "tacz:gun/ak47" -> "tacz:ak47"
        ResourceLocation rl = new ResourceLocation(storedGunId);
        String path = rl.getPath();
        if (path.startsWith("gun/")) {
            path = path.substring(4); // Remove "gun/"
        }
        return new ResourceLocation(rl.getNamespace(), path);
    }

    public static ItemStack createBlueprint(Item blueprintItem, ResourceLocation gunId) {
        ItemStack stack = new ItemStack(blueprintItem);
        CompoundTag tag = stack.getOrCreateTag();
        // Store the full gun ID with "gun/" prefix
        tag.putString("GunId", gunId.toString());
        // TaCZBlueprints.LOGGER.debug("[Blueprint] Creating blueprint for gunId='{}'", gunId);
        return stack;
    }
}