package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(CrushingWheelRecipeSelector.MOD_ID)
public class CrushingWheelRecipeSelector {

    public static final String MOD_ID = "crushingwheelrecipeselector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrushingWheelRecipeSelector(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Crushing Wheel Recipe Selector");
        
        // Register network packets
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        
        LOGGER.info("Crushing Wheel Recipe Selector initialized");
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        ModNetworking.register(event);
    }
}
