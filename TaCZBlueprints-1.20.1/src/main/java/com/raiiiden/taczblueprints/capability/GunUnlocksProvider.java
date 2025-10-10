package com.raiiiden.taczblueprints.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;

import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

public class GunUnlocksProvider implements ICapabilitySerializable<CompoundTag> {

    public static Capability<IGunUnlocks> UNLOCKS = CapabilityManager.get(new CapabilityToken<>() {});

    private final IGunUnlocks instance;

    public GunUnlocksProvider() {
        this.instance = new GunUnlocks();
    }

    public GunUnlocksProvider(IGunUnlocks instance) {
        this.instance = instance;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == UNLOCKS ? LazyOptional.of(() -> instance).cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }

    public static LazyOptional<IGunUnlocks> get(Player player) {
        return player.getCapability(UNLOCKS);
    }
}
