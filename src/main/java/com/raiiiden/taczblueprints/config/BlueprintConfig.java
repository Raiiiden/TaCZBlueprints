package com.raiiiden.taczblueprints.config;

import com.raiiiden.taczblueprints.TaCZBlueprints;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class BlueprintConfig {

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Server, ForgeConfigSpec> serverConfig = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = serverConfig.getLeft();
        SERVER_SPEC = serverConfig.getRight();

        Pair<Client, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = clientConfig.getLeft();
        CLIENT_SPEC = clientConfig.getRight();
    }

    // ============================================================
    // === SERVER CONFIG ==========================================
    // ============================================================
    public static class Server {
        // === Table toggles ===
        public final ForgeConfigSpec.BooleanValue enableGunTable;
        public final ForgeConfigSpec.BooleanValue enableAttachmentTable;
        public final ForgeConfigSpec.BooleanValue enableAmmoTable;

        // === Whitelists ===
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledGuns;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledAttachments;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledAmmo;

        // === Loot global defaults ===
        public final ForgeConfigSpec.DoubleValue lootChestChance;
        public final ForgeConfigSpec.IntValue lootChestMinCount;
        public final ForgeConfigSpec.IntValue lootChestMaxCount;

        // === Weapon type weights ===
        public final ForgeConfigSpec.IntValue weightPistol;
        public final ForgeConfigSpec.IntValue weightSmg;
        public final ForgeConfigSpec.IntValue weightRifle;
        public final ForgeConfigSpec.IntValue weightShotgun;
        public final ForgeConfigSpec.IntValue weightSniper;
        public final ForgeConfigSpec.IntValue weightMg;
        public final ForgeConfigSpec.IntValue weightRpg;
        public final ForgeConfigSpec.IntValue weightDefault;

        // === Loot overrides per chest ===
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> lootTableOverrides;

        public Server(ForgeConfigSpec.Builder builder) {
            // ------------------------------
            // 🔹 Table toggles
            // ------------------------------
            builder.push("tables");
            enableGunTable = builder
                    .comment("If false, disables the GunSmith table recipes for guns.")
                    .define("enableGunTable", false);

            enableAttachmentTable = builder
                    .comment("If false, disables the GunSmith table recipes for attachments.")
                    .define("enableAttachmentTable", true);

            enableAmmoTable = builder
                    .comment("If false, disables the GunSmith table recipes for ammo.")
                    .define("enableAmmoTable", true);
            builder.pop();

            // ------------------------------
            // 🔹 Unlock whitelists
            // ------------------------------
            builder.push("unlocks");

            enabledGuns = builder
                    .comment("List of gun IDs enabled by default. Example: [\"tacz:ak47\", \"tacz:m4a1\"]")
                    .defineListAllowEmpty("enabledGuns", new ArrayList<>(), o -> o instanceof String);

            enabledAttachments = builder
                    .comment("List of attachment IDs enabled by default. Example: [\"tacz:scope_4x\"]")
                    .defineListAllowEmpty("enabledAttachments", new ArrayList<>(), o -> o instanceof String);

            enabledAmmo = builder
                    .comment("List of ammo IDs enabled by default. Example: [\"tacz:556mm\"]")
                    .defineListAllowEmpty("enabledAmmo", new ArrayList<>(), o -> o instanceof String);

            builder.pop();

            // ------------------------------
            // 🔹 Loot settings
            // ------------------------------
            builder.push("loot");
            builder.comment("Global loot generation settings for blueprints in structure chests.");

            lootChestChance = builder
                    .comment("Chance (0.0 to 1.0) that blueprints will be added to a chest")
                    .defineInRange("chestChance", 0.05, 0.0, 1.0);

            lootChestMinCount = builder
                    .comment("Minimum number of blueprints to add when loot triggers")
                    .defineInRange("chestMinCount", 1, 1, 64);

            lootChestMaxCount = builder
                    .comment("Maximum number of blueprints to add when loot triggers")
                    .defineInRange("chestMaxCount", 1, 1, 64);

            lootTableOverrides = builder.comment(
                    "Optional per-loot-table overrides in format: minecraft:chests/jungle_temple=1.0,4,6"
            ).defineListAllowEmpty("lootTableOverrides", new ArrayList<>(), o -> o instanceof String);

            builder.pop();

            // ------------------------------
            // 🔹 Weapon type weights
            // ------------------------------
            builder.push("weaponTypeWeights");
            builder.comment(
                    "Relative weights for each weapon type when generating blueprint loot.",
                    "Higher values = more likely to drop. Set to 0 to disable a type.",
                    "Example: If Pistol=100 and Rifle=50, pistols are 2x more likely than rifles."
            );

            weightPistol = builder
                    .comment("Weight for Pistol blueprints")
                    .defineInRange("pistolWeight", 100, 0, 1000);

            weightSmg = builder
                    .comment("Weight for SMG blueprints")
                    .defineInRange("smgWeight", 80, 0, 1000);

            weightRifle = builder
                    .comment("Weight for Rifle blueprints")
                    .defineInRange("rifleWeight", 70, 0, 1000);

            weightShotgun = builder
                    .comment("Weight for Shotgun blueprints")
                    .defineInRange("shotgunWeight", 60, 0, 1000);

            weightSniper = builder
                    .comment("Weight for Sniper blueprints")
                    .defineInRange("sniperWeight", 40, 0, 1000);

            weightMg = builder
                    .comment("Weight for Machine Gun blueprints")
                    .defineInRange("mgWeight", 30, 0, 1000);

            weightRpg = builder
                    .comment("Weight for RPG blueprints")
                    .defineInRange("rpgWeight", 20, 0, 1000);

            weightDefault = builder
                    .comment("Weight for unrecognized weapon types")
                    .defineInRange("defaultWeight", 50, 0, 1000);

            builder.pop();
        }

        // --- Helper record ---
        public record LootOverride(float chance, int min, int max) {}

        // --- Get lists ---
        private List<String> getList(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
            List<String> list = new ArrayList<>();
            for (Object obj : value.get()) list.add(String.valueOf(obj));
            return list;
        }

        // --- Accessors ---
        public List<String> getEnabledGuns() { return getList(enabledGuns); }
        public List<String> getEnabledAttachments() { return getList(enabledAttachments); }
        public List<String> getEnabledAmmo() { return getList(enabledAmmo); }

        // --- Get weapon type weight ---
        public int getWeightForType(String type) {
            return switch (type) {
                case "Pistol" -> weightPistol.get();
                case "Smg" -> weightSmg.get();
                case "Rifle" -> weightRifle.get();
                case "Shotgun" -> weightShotgun.get();
                case "Sniper" -> weightSniper.get();
                case "Mg" -> weightMg.get();
                case "Rpg" -> weightRpg.get();
                default -> weightDefault.get();
            };
        }

        // --- Parse loot table overrides ---
        public Map<ResourceLocation, LootOverride> getLootOverrides() {
            Map<ResourceLocation, LootOverride> map = new HashMap<>();
            for (String entry : lootTableOverrides.get()) {
                try {
                    String[] split = entry.split("=");
                    if (split.length != 2) continue;
                    ResourceLocation id = new ResourceLocation(split[0].trim());
                    String[] vals = split[1].split(",");
                    if (vals.length < 3) continue;

                    float chance = Float.parseFloat(vals[0]);
                    int min = Integer.parseInt(vals[1]);
                    int max = Integer.parseInt(vals[2]);

                    map.put(id, new LootOverride(chance, min, max));
                } catch (Exception e) {
                    TaCZBlueprints.LOGGER.warn("[Blueprint Loot] Invalid loot override entry: {}", entry, e);
                }
            }
            return map;
        }
    }

    // ============================================================
    // === CLIENT CONFIG ==========================================
    // ============================================================
    public static class Client {
        public Client(ForgeConfigSpec.Builder builder) {}
    }
}