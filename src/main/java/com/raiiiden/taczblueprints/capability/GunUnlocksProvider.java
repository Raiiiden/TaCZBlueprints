package com.raiiiden.taczblueprints.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GunUnlocksProvider implements ICapabilitySerializable<CompoundTag> {

    // Create the capability reference
    public static final Capability<IGunUnlocks> UNLOCKS = CapabilityManager.get(new CapabilityToken<>() {});

    private IGunUnlocks instance = null;
    private final LazyOptional<IGunUnlocks> optional = LazyOptional.of(this::createOrGetInstance);

    private IGunUnlocks createOrGetInstance() {
        if (instance == null) {
            instance = new GunUnlocks();
        }
        return instance;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == UNLOCKS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createOrGetInstance().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createOrGetInstance().deserializeNBT(nbt);
    }

    /**
     * Convenience method to get capability from player
     */
    public static LazyOptional<IGunUnlocks> get(Player player) {
        return player.getCapability(UNLOCKS);
    }

    /**
     * Invalidate the optional when capability is removed
     */
    public void invalidate() {
        optional.invalidate();
    }
}