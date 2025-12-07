package com.enormeboze.crushingwheelrecipeselector;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("crushingwheelrecipeselector")
public class CrushingWheelRecipeSelector {

    public static final String MOD_ID = "crushingwheelrecipeselector";
    public static final Logger LOGGER = LoggerFactory.getLogger(CrushingWheelRecipeSelector.class);

    public CrushingWheelRecipeSelector(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Crushing Recipe Selector is initializing...");

        // Register the common setup method
        modEventBus.addListener(this::commonSetup);

        // Initialize config
        Config.init();

        LOGGER.info("Crushing Recipe Selector initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup complete!");
    }
}