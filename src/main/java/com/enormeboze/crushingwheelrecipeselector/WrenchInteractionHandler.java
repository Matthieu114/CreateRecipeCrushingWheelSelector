package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles wrench interactions with crushing wheels
 */
@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class WrenchInteractionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        InteractionHand hand = event.getHand();
        ItemStack itemInHand = player.getItemInHand(hand);
        BlockState clickedBlock = level.getBlockState(pos);

        // Only process once per interaction (main hand only)
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        // Check if player is holding a wrench
        if (!isWrench(itemInHand)) {
            return;
        }

        // Check if clicked block is a crushing wheel
        if (clickedBlock.getBlock() instanceof CrushingWheelBlock || 
            clickedBlock.getBlock() instanceof CrushingWheelControllerBlock) {
            
            CrushingWheelRecipeSelector.LOGGER.info("✓✓✓ WRENCH CLICKED CRUSHING WHEEL! Player: {}, Pos: {}, ClientSide: {}", 
                player.getName().getString(), pos, level.isClientSide());

            // Open GUI on client side
            if (level.isClientSide()) {
                openRecipeSelectorScreen(pos);
            }

            // Cancel the event so Create doesn't do its normal wrench behavior
            event.setCanceled(true);
        }
    }

    /**
     * Opens the recipe selector screen (client-side only)
     */
    private static void openRecipeSelectorScreen(BlockPos wheelPos) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        minecraft.setScreen(new com.enormeboze.crushingwheelrecipeselector.client.RecipeSelectorScreen(wheelPos));
    }

    /**
     * Check if an item is a wrench
     * Create's wrench has the tag "create:wrench"
     */
    private static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check by item ID directly (most reliable)
        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        return itemId.equals("create:wrench");
    }
}
