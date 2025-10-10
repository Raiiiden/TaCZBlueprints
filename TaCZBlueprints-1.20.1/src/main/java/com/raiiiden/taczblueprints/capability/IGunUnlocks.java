package com.raiiiden.taczblueprints.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public interface IGunUnlocks {

    void unlockGun(String gunId);

    boolean isUnlocked(String gunId);

    void clearAll();

    Set<String> getUnlockedGuns();

    void setUnlockedGuns(Set<String> guns);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag nbt);
}
