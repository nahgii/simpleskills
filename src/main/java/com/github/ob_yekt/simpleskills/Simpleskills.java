package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.data.DatabaseManager;
import com.github.ob_yekt.simpleskills.requirements.RequirementLoader;
import com.github.ob_yekt.simpleskills.simpleclasses.BlockInteractionHandler;
import com.github.ob_yekt.simpleskills.simpleclasses.PerkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simpleskills implements ModInitializer {
	public static final String MOD_ID = "simpleskills";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[simpleskills] Initializing");

		// Register world load event (critical functionality: database initialization)
		ServerWorldEvents.LOAD.register(this::onWorldLoad);

		// Load requirements on startup
		RequirementLoader.loadRequirements();
		LOGGER.info("[simpleskills] Loaded requirements from JSON files!");

		// Load base config on startup
		com.github.ob_yekt.simpleskills.requirements.ConfigLoader.loadFeatureConfig(); // Load the JSON configuration
		LOGGER.info("[simpleskills] Base config registered!");

		// Load XP values on startup
		com.github.ob_yekt.simpleskills.requirements.ConfigLoader.loadBaseXPConfig(); // Load the JSON configuration
		LOGGER.info("[simpleskills] Base XP config registered!");

		// Register event handlers
		PlayerEventHandlers.registerEvents();
		LOGGER.info("[simpleskills] Events registered!");

		// Load requirements on startup
		BlockInteractionHandler.register();
		LOGGER.info("[simpleskills] Block interaction handler registered!");

		// Register attributes
		AttributeUpdater.registerPlayerEvents();
		LOGGER.info("[simpleskills] Attributes registered!");

		// Register perks
		PerkHandler.registerPerkHandlers();
		LOGGER.info("[simpleskills] Perks registered!");
		// Register trades
		VillagerTrades.registerCustomTrades();
		LOGGER.info("[simpleskills] Trades registered!");

		// Register commands
		SimpleskillsCommands.registerCommands();
		LOGGER.info("[simpleskills] Commands registered!");

		// Add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			DatabaseManager.getInstance().close();
			LOGGER.info("[simpleskills] Database connection closed.");
		}));
	}

	// Method invoked when a world is loaded
	private void onWorldLoad(MinecraftServer server, ServerWorld world) {
		LOGGER.info("[simpleskills] World loaded: {}", world.getRegistryKey().getValue());
		DatabaseManager.getInstance().initializeDatabase(server);
		LOGGER.info("[simpleskills] Database initialized for world: {}", world.getRegistryKey().getValue());
	}
}