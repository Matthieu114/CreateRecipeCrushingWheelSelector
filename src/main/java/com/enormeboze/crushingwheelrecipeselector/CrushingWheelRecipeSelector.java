package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.ModNetworking;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CrushingWheelRecipeSelector.MOD_ID)
public class CrushingWheelRecipeSelector {

    public static final String MOD_ID = "crushingwheelrecipeselector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrushingWheelRecipeSelector(IEventBus modEventBus) {
        LOGGER.info("Crushing Wheel Recipe Selector initializing...");

        // Register network packets on MOD bus
        ModNetworking.register(modEventBus);

        // Other handlers use @EventBusSubscriber on GAME bus (default) so they auto-register:
        // - WrenchHandler (wrench interaction)
        // - RecipeHandler (recipe scanning)
        // - BreakHandler (wheel break cleanup)

        LOGGER.info("Crushing Wheel Recipe Selector initialized!");
    }
}