package com.raiiiden.taczblueprints.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.LazyOptional;

import java.util.HashSet;
import java.util.Set;

public class GunUnlocks implements IGunUnlocks {

    private final Set<String> unlockedGuns = new HashSet<>();

    @Override
    public void unlockGun(String gunId) {
        unlockedGuns.add(gunId);
    }

    @Override
    public void setUnlockedGuns(Set<String> guns) {
        unlockedGuns.clear();
        unlockedGuns.addAll(guns);
    }


    @Override
    public boolean isUnlocked(String gunId) {
        return unlockedGuns.contains(gunId);
    }

    @Override
    public void clearAll() {
        unlockedGuns.clear();
    }

    @Override
    public Set<String> getUnlockedGuns() {
        return new HashSet<>(unlockedGuns);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        int i = 0;
        for (String gunId : unlockedGuns) {
            tag.putString("Gun" + i, gunId);
            i++;
        }
        tag.putInt("GunCount", unlockedGuns.size());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        unlockedGuns.clear();
        int count = nbt.getInt("GunCount");
        for (int i = 0; i < count; i++) {
            String gunId = nbt.getString("Gun" + i);
            unlockedGuns.add(gunId);
        }
    }
}
