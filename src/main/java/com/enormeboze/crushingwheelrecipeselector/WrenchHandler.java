package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.SyncSelectionsPacket;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

/**
 * Handles wrench interactions with crushing wheels for LINKING and GUI opening.
 *
 * CONTROLS:
 * - Right-click with wrench (NOT shift):
 *     - If wheel is not linked: Start/complete linking process
 *     - If wheel is linked: Open recipe selector GUI
 * - Shift + Right-click with wrench: DEFAULT Create behavior (pick up block)
 */
@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class WrenchHandler {

    // Pending link starts: Player UUID -> First wheel position
    private static final Map<UUID, BlockPos> pendingLinks = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        // Only process main hand to avoid double-triggering
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        // Check if the block is a Create crushing wheel
        if (!(state.getBlock() instanceof CrushingWheelBlock)) {
            return;
        }

        // Check if player is holding a wrench
        ItemStack heldItem = player.getItemInHand(hand);
        if (!isWrench(heldItem)) {
            return;
        }

        // CRITICAL: DO NOT intercept shift+click - that's Create's pickup behavior!
        if (player.isShiftKeyDown()) {
            return; // Let Create handle shift+right-click (breaks the wheel)
        }

        // Cancel default wrench behavior (rotation)
        event.setCanceled(true);

        // Handle on server side for linking logic
        if (!level.isClientSide()) {
            handleWrenchInteraction(player, pos, level);
        }
    }

    private static void handleWrenchInteraction(Player player, BlockPos clickedPos, Level level) {
        UUID playerId = player.getUUID();

        // Get the selections data
        CrushingWheelSelections selections = CrushingWheelSelections.get(level);

        // Check if this wheel is already linked
        boolean isLinked = selections.isWheelLinked(clickedPos);

        // Check if player has a pending link
        BlockPos pendingPos = pendingLinks.get(playerId);

        if (pendingPos != null) {
            // Player is completing a link
            if (pendingPos.equals(clickedPos)) {
                // Clicked same wheel - cancel linking
                pendingLinks.remove(playerId);
                player.displayClientMessage(Component.literal("§cLinking cancelled"), true);
                CrushingWheelRecipeSelector.LOGGER.debug("Player {} cancelled linking", player.getName().getString());
            } else {
                // Complete the link between two wheels
                completeLink(player, pendingPos, clickedPos, level, selections);
            }
        } else if (isLinked) {
            // Wheel is already linked - open GUI
            openGui(player, clickedPos);
        } else {
            // Start linking process
            startLink(player, clickedPos);
        }
    }

    private static void startLink(Player player, BlockPos pos) {
        pendingLinks.put(player.getUUID(), pos);
        player.displayClientMessage(Component.literal("§eSelect second crushing wheel to link..."), true);
        CrushingWheelRecipeSelector.LOGGER.debug("Player {} started linking at {}", player.getName().getString(), pos);
    }

    private static void completeLink(Player player, BlockPos firstPos, BlockPos secondPos, Level level, CrushingWheelSelections selections) {
        pendingLinks.remove(player.getUUID());

        // Create a new group UUID for both wheels
        UUID groupId = UUID.randomUUID();

        // Link both wheels to the same group
        selections.linkWheel(firstPos, groupId);
        selections.linkWheel(secondPos, groupId);

        player.displayClientMessage(Component.literal("§aCrushing wheels linked! Right-click to configure recipes."), true);
        CrushingWheelRecipeSelector.LOGGER.info("Player {} linked wheels at {} and {} with group {}",
                player.getName().getString(), firstPos, secondPos, groupId);
    }

    private static void openGui(Player player, BlockPos wheelPos) {
        // This is called on SERVER side from handleWrenchInteraction
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Get current selections for this wheel's group
            CrushingWheelSelections selections = CrushingWheelSelections.get(serverPlayer.serverLevel());
            Map<String, ResourceLocation> prefs = selections != null
                    ? selections.getAllPreferences(wheelPos)
                    : Map.of();

            // Send sync packet with openGui=true directly to the player
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncSelectionsPacket(wheelPos, prefs, true));

        }
    }

    private static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        // Check for Create wrench specifically
        if (itemId.equals("create:wrench")) {
            return true;
        }

        // Check for other common wrenches
        String itemName = itemId.toLowerCase();
        return itemName.contains("wrench");
    }

    /**
     * Clear pending link for a player (e.g., on disconnect)
     */
    public static void clearPendingLink(UUID playerId) {
        pendingLinks.remove(playerId);
    }

    /**
     * Check if a player has a pending link
     */
    public static boolean hasPendingLink(UUID playerId) {
        return pendingLinks.containsKey(playerId);
    }
}