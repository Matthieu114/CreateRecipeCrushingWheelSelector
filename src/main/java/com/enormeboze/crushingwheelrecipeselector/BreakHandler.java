package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class BreakHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // ... validation to ensure we are on Server and correct block type ...
        if (event.getLevel().isClientSide()) return;

        if (event.getState().getBlock() instanceof CrushingWheelBlock ||
                event.getState().getBlock() instanceof CrushingWheelControllerBlock) {

            if (event.getLevel() instanceof ServerLevel serverLevel) {
                // This call to removeWheel handles the cleanup of the group if it's the last member
                CrushingWheelSelections.get(serverLevel).removeWheel(event.getPos());
            }
        }
    }
}