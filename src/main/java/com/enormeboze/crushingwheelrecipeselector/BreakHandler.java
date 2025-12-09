package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.CancelLinkingPacket;
import com.enormeboze.crushingwheelrecipeselector.network.ModNetworking;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class BreakHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (!(event.getState().getBlock() instanceof CrushingWheelBlock) &&
            !(event.getState().getBlock() instanceof CrushingWheelControllerBlock)) {
            return;
        }

        BlockPos brokenPos = event.getPos();

        // Handle linked wheel cleanup
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            CrushingWheelSelections.get(serverLevel).removeWheel(brokenPos);
        }

        // Handle pending link cancellation
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                BlockPos pendingPos = WrenchHandler.getPendingLink(player.getUUID());
                
                if (pendingPos != null && pendingPos.equals(brokenPos)) {
                    WrenchHandler.clearPendingLink(player.getUUID());
                    
                    ModNetworking.sendToPlayer(player, new CancelLinkingPacket());
                    
                    player.displayClientMessage(
                            Component.literal("§cLinking cancelled - wheel was broken"),
                            true
                    );
                    
                    CrushingWheelRecipeSelector.LOGGER.debug("Cancelled pending link for player {} - wheel at {} was broken",
                            player.getName().getString(), brokenPos);
                }
            }
        }
    }
}
