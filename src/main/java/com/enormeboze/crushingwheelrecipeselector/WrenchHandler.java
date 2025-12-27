package com.enormeboze.crushingwheelrecipeselector;

import com.enormeboze.crushingwheelrecipeselector.network.ModNetworking;
import com.enormeboze.crushingwheelrecipeselector.network.SyncSelectionsPacket;
import com.enormeboze.crushingwheelrecipeselector.network.StartLinkingPacket;
import com.enormeboze.crushingwheelrecipeselector.network.CancelLinkingPacket;
import com.enormeboze.crushingwheelrecipeselector.network.LinkResultPacket;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class WrenchHandler {

    private static final Map<UUID, BlockPos> pendingLinks = new HashMap<>();

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        pendingLinks.clear();
        CrushingWheelRecipeSelector.LOGGER.debug("Cleared pending links on server stop");
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        if (pendingLinks.remove(playerId) != null) {
            CrushingWheelRecipeSelector.LOGGER.debug("Cleared pending link for disconnected player {}",
                    event.getEntity().getName().getString());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();

        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CrushingWheelBlock)) {
            return;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (!isWrench(heldItem)) {
            return;
        }

        if (player.isShiftKeyDown()) {
            return;
        }

        event.setCanceled(true);

        if (!level.isClientSide()) {
            handleWrenchInteraction(player, pos, level);
        }
    }

    private static void handleWrenchInteraction(Player player, BlockPos clickedPos, Level level) {
        UUID playerId = player.getUUID();

        CrushingWheelSelections selections = CrushingWheelSelections.get(level);

        boolean isLinked = selections != null && selections.isWheelLinked(clickedPos);

        BlockPos pendingPos = pendingLinks.get(playerId);

        if (pendingPos != null) {
            if (pendingPos.equals(clickedPos)) {
                cancelLink(player);
            } else {
                tryCompleteLink(player, pendingPos, clickedPos, level, selections);
            }
        } else if (isLinked) {
            openGui(player, clickedPos);
        } else {
            startLink(player, clickedPos, level);
        }
    }

    private static void startLink(Player player, BlockPos pos, Level level) {
        pendingLinks.put(player.getUUID(), pos);
        player.displayClientMessage(Component.literal("§eSelect second crushing wheel to link..."), true);
        CrushingWheelRecipeSelector.LOGGER.debug("Player {} started linking at {}", player.getName().getString(), pos);

        if (player instanceof ServerPlayer serverPlayer) {
            Set<BlockPos> validTargets = WheelLinkingHelper.findValidLinkTargets(level, pos);
            Set<BlockPos> invalidTargets = WheelLinkingHelper.findInvalidLinkTargets(level, pos, validTargets);
            ModNetworking.sendToPlayer(serverPlayer, new StartLinkingPacket(pos, validTargets, invalidTargets));
        }
    }

    private static void cancelLink(Player player) {
        pendingLinks.remove(player.getUUID());
        player.displayClientMessage(Component.literal("§cLinking cancelled"), true);
        CrushingWheelRecipeSelector.LOGGER.debug("Player {} cancelled linking", player.getName().getString());

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(serverPlayer, new CancelLinkingPacket());
        }
    }

    private static void tryCompleteLink(Player player, BlockPos firstPos, BlockPos secondPos, Level level, CrushingWheelSelections selections) {
        // Check if second wheel is already linked to another wheel
        if (selections != null && selections.isWheelLinked(secondPos)) {
            pendingLinks.remove(player.getUUID());
            player.displayClientMessage(Component.literal("§cCannot link: That wheel is already linked to another wheel"), true);

            CrushingWheelRecipeSelector.LOGGER.debug("Player {} failed to link wheels: second wheel already linked",
                    player.getName().getString());

            if (player instanceof ServerPlayer serverPlayer) {
                ModNetworking.sendToPlayer(serverPlayer, new LinkResultPacket(false, secondPos, secondPos));
            }
            return;
        }

        if (!WheelLinkingHelper.canLink(level, firstPos, secondPos)) {
            String reason = WheelLinkingHelper.getLinkFailureReason(level, firstPos, secondPos);

            pendingLinks.remove(player.getUUID());
            player.displayClientMessage(Component.literal("§cCannot link: " + reason), true);

            CrushingWheelRecipeSelector.LOGGER.debug("Player {} failed to link wheels: {}",
                    player.getName().getString(), reason);

            if (player instanceof ServerPlayer serverPlayer) {
                ModNetworking.sendToPlayer(serverPlayer, new LinkResultPacket(false, secondPos, secondPos));
            }

            return;
        }

        completeLink(player, firstPos, secondPos, level, selections);
    }

    private static void completeLink(Player player, BlockPos firstPos, BlockPos secondPos, Level level, CrushingWheelSelections selections) {
        pendingLinks.remove(player.getUUID());

        UUID groupId = UUID.randomUUID();

        selections.linkWheel(firstPos, groupId);
        selections.linkWheel(secondPos, groupId);

        player.displayClientMessage(Component.literal("§aCrushing wheels linked! Right-click to configure recipes."), true);
        CrushingWheelRecipeSelector.LOGGER.info("Player {} linked wheels at {} and {} with group {}",
                player.getName().getString(), firstPos, secondPos, groupId);

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(serverPlayer, new LinkResultPacket(true, firstPos, secondPos));
        }
    }

    private static void openGui(Player player, BlockPos wheelPos) {
        if (player instanceof ServerPlayer serverPlayer) {
            CrushingWheelSelections selections = CrushingWheelSelections.get(serverPlayer.serverLevel());
            Map<String, ResourceLocation> prefs = selections != null
                    ? selections.getAllPreferences(wheelPos)
                    : Map.of();

            ModNetworking.sendToPlayer(serverPlayer, new SyncSelectionsPacket(wheelPos, prefs, true));

            CrushingWheelRecipeSelector.LOGGER.debug("Sent GUI open packet to player {} for wheel at {}",
                    player.getName().getString(), wheelPos);
        }
    }

    private static boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return false;
        }

        String itemIdString = itemId.toString();

        if (itemIdString.equals("create:wrench")) {
            return true;
        }

        return itemIdString.toLowerCase().contains("wrench");
    }

    public static void clearPendingLink(UUID playerId) {
        pendingLinks.remove(playerId);
    }

    public static boolean hasPendingLink(UUID playerId) {
        return pendingLinks.containsKey(playerId);
    }

    public static BlockPos getPendingLink(UUID playerId) {
        return pendingLinks.get(playerId);
    }
}
