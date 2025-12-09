package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.ModNetworking;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CrushingWheelRecipeSelector.MOD_ID)
public class CrushingWheelRecipeSelector {

    public static final String MOD_ID = "crushingwheelrecipeselector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrushingWheelRecipeSelector() {
        LOGGER.info("Crushing Wheel Recipe Selector initializing...");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register setup method
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for game events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Crushing Wheel Recipe Selector initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Register network packets on main thread
        event.enqueueWork(ModNetworking::register);
    }
}
