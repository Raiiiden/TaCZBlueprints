package com.raiiiden.taczblueprints;

import com.raiiiden.taczblueprints.config.BlueprintConfig;
import com.raiiiden.taczblueprints.item.BlueprintRegistrar;
import com.raiiiden.taczblueprints.loot.ModLootModifiers;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
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

    // Register loot modifiers (chest only, entity removed)
    ModLootModifiers.register();

    // Log registered chest loot modifier
    LOGGER.info("[{}] Registered loot modifier type: {}", MODID, ModLootModifiers.BLUEPRINT_CHEST_LOOT.getId());

    // Register configs
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BlueprintConfig.SERVER_SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BlueprintConfig.CLIENT_SPEC);

    // Event bus listeners
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);

    // Commands
    MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    com.raiiiden.taczblueprints.network.ModNetworking.registerPackets();
    LOGGER.info("[{}] Common setup complete", MODID);
    // Config may not be loaded yet, dynamic loot handler will use safe defaults
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
