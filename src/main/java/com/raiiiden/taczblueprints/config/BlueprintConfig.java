package com.raiiiden.taczblueprints.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

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

    public static class Server {
        // === Table toggles ===
        public final ForgeConfigSpec.BooleanValue enableGunTable;
        public final ForgeConfigSpec.BooleanValue enableAttachmentTable;
        public final ForgeConfigSpec.BooleanValue enableAmmoTable;

        // === Whitelists ===
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledGuns;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledAttachments;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledAmmo;

        // === Loot ===
        public final ForgeConfigSpec.DoubleValue lootChestChance;
        public final ForgeConfigSpec.IntValue lootChestMinCount;
        public final ForgeConfigSpec.IntValue lootChestMaxCount;

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
            builder.comment("Chest loot generation settings");
            lootChestChance = builder.defineInRange("chestChance", 0.05, 0.0, 1.0);
            lootChestMinCount = builder.defineInRange("chestMinCount", 1, 1, 64);
            lootChestMaxCount = builder.defineInRange("chestMaxCount", 1, 1, 64);
            builder.pop();
        }

        private List<String> getList(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
            List<String> list = new ArrayList<>();
            for (Object obj : value.get()) list.add(String.valueOf(obj));
            return list;
        }

        public List<String> getEnabledGuns() { return getList(enabledGuns); }
        public List<String> getEnabledAttachments() { return getList(enabledAttachments); }
        public List<String> getEnabledAmmo() { return getList(enabledAmmo); }
    }

    public static class Client {
        public Client(ForgeConfigSpec.Builder builder) {}
    }
}
