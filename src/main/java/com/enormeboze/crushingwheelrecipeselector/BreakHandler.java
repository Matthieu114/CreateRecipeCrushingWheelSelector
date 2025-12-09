package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.CancelLinkingPacket;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class BreakHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Only process on server side
        if (event.getLevel().isClientSide()) return;

        // Check if the broken block is a crushing wheel
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
        // Check ALL players to see if anyone was trying to link this wheel
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                BlockPos pendingPos = WrenchHandler.getPendingLink(player.getUUID());

                if (pendingPos != null && pendingPos.equals(brokenPos)) {
                    // This player's pending link wheel was broken
                    WrenchHandler.clearPendingLink(player.getUUID());

                    // Send packet to clear the highlight on client
                    PacketDistributor.sendToPlayer(player, new CancelLinkingPacket());

                    // Notify the player
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cLinking cancelled - wheel was broken"),
                            true
                    );

                    CrushingWheelRecipeSelector.LOGGER.debug("Cancelled pending link for player {} - wheel at {} was broken",
                            player.getName().getString(), brokenPos);
                }
            }
        }
    }
}