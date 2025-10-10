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
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> enabledGuns;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("unlocks");
            enabledGuns = builder
                    .comment("List of gun IDs that are enabled by default (whitelist). All other guns require blueprints.")
                    .defineListAllowEmpty("enabledGuns", new ArrayList<>(), o -> o instanceof String);
            builder.pop();
        }

        public List<String> getEnabledGuns() {
            List<String> list = new ArrayList<>();
            for (Object obj : enabledGuns.get()) list.add(String.valueOf(obj));
            return list;
        }
    }

    public static class Client {
        public Client(ForgeConfigSpec.Builder builder) {}
    }
}
