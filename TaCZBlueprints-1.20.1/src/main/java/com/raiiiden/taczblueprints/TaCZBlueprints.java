package com.raiiiden.taczblueprints;

import com.raiiiden.taczblueprints.config.BlueprintConfig;
import com.raiiiden.taczblueprints.item.BlueprintRegistrar;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TaCZBlueprints.MODID)
public class TaCZBlueprints {
  public static final String MODID = "taczblueprints";
  public static final Logger LOGGER = LogManager.getLogger();

  public TaCZBlueprints() {
    LOGGER.info("[{}] TaCZ Blueprints mod initializing...", MODID);

    // Register items and creative tabs
    BlueprintRegistrar.register();

    // Setup mod event bus listeners
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);

    // Register commands on Forge event bus
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

    // Register configs
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BlueprintConfig.SERVER_SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BlueprintConfig.CLIENT_SPEC);
  }


  private void commonSetup(FMLCommonSetupEvent event) {
    com.raiiiden.taczblueprints.network.ModNetworking.registerPackets();
    LOGGER.info("[{}] Common setup complete", MODID);
  }
  private void registerCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
    event.getDispatcher().register(com.raiiiden.taczblueprints.command.BlueprintCommands.register());
  }

  private void onConfigLoad(ModConfigEvent.Loading event) {
    if (event.getConfig().getSpec() == BlueprintConfig.SERVER_SPEC) {
      var enabledGuns = BlueprintConfig.SERVER.getEnabledGuns();
      LOGGER.info("[{}] Config loaded - Enabled guns whitelist size: {}", MODID, enabledGuns.size());
    }
  }
}
