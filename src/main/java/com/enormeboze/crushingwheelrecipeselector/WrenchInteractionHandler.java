package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.SyncSelectionsPacket;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class WrenchInteractionHandler {

    // Tracks the first click for linking: PlayerUUID -> BlockPos
    private static final Map<UUID, BlockPos> PENDING_LINKS = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        InteractionHand hand = event.getHand();

        // 1. Basic checks (must be main hand and holding a wrench)
        if (hand != InteractionHand.MAIN_HAND || !isWrench(player.getItemInHand(hand))) return;

        BlockState state = level.getBlockState(pos);
        // Must be a Crushing Wheel block
        if (!(state.getBlock() instanceof CrushingWheelBlock) &&
                !(state.getBlock() instanceof CrushingWheelControllerBlock)) return;

        // 2. Handle Shift-Right-Click: If sneaking, DO NOTHING and allow the event to proceed.
        // This lets Create's wrench dismantling/breaking behavior run.
        if (player.isShiftKeyDown()) {
            return;
        }

        // --- At this point, the player is NOT sneaking, and we take over the interaction. ---

        // 3. Cancel the event so Create doesn't try to rotate the block instead.
        event.setCanceled(true);

        if (level.isClientSide) return; // Server-side logic only

        ServerPlayer serverPlayer = (ServerPlayer) player;
        CrushingWheelSelections selections = CrushingWheelSelections.get(serverPlayer.serverLevel());
        UUID playerId = player.getUUID();

        // 4. Check if the wheel is already linked/configured. If so, open GUI immediately.
        if (selections.isWheelLinked(pos)) {
            openGui(serverPlayer, pos);
            PENDING_LINKS.remove(playerId); // Always clear pending link if they access the GUI
            return;
        }

        // 5. Handle Linking Process (Only runs if wheel is NOT linked and player is NOT sneaking)
        if (PENDING_LINKS.containsKey(playerId)) {
            BlockPos firstPos = PENDING_LINKS.get(playerId);

            if (firstPos.equals(pos)) {
                // Clicked same block twice, cancel
                player.displayClientMessage(Component.literal("Link cancelled. Wheel selection cleared.").withStyle(ChatFormatting.RED), true);
                PENDING_LINKS.remove(playerId);
            } else {
                // Complete the link
                selections.linkWheels(firstPos, pos);
                player.displayClientMessage(Component.literal("Crushing Wheels Linked! Now opening GUI to set shared recipe.").withStyle(ChatFormatting.GREEN), true);
                PENDING_LINKS.remove(playerId);

                // Open GUI for the newly linked pair
                openGui(serverPlayer, pos);
            }
        } else {
            // Start Link
            PENDING_LINKS.put(playerId, pos);
            player.displayClientMessage(Component.literal("First wheel selected. Right-click another wheel to link them.").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private static void openGui(ServerPlayer player, BlockPos pos) {
        CrushingWheelSelections selections = CrushingWheelSelections.get(player.serverLevel());

        // Get the selections for the group the wheel belongs to
        var data = selections.getWheelSelections(pos);

        // The SyncSelectionsPacket triggers the GUI to open on the client
        PacketDistributor.sendToPlayer(player, new SyncSelectionsPacket(pos, data));
    }

    private static boolean isWrench(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().builtInRegistryHolder().key().location().toString().equals("create:wrench");
    }
}