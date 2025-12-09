package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.Set;

/**
 * Handles client-side packet processing.
 * This class is separate to avoid classloading issues with client-only classes on servers.
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleSyncSelections(BlockPos wheelPos, Map<String, ResourceLocation> selections, boolean openGui) {
        // Update client cache
        ClientSelectionCache.updateSelections(wheelPos, selections);

        // Open GUI if requested
        if (openGui) {
            Minecraft.getInstance().execute(() -> {
                ClientScreenHelper.openRecipeSelectorScreen(wheelPos);
            });
        }
    }

    public static void handleStartLinking(BlockPos selectedWheel, Set<BlockPos> validTargets, Set<BlockPos> invalidTargets) {
        Minecraft.getInstance().execute(() -> {
            WheelHighlightRenderer.setSelectedWheel(selectedWheel, validTargets, invalidTargets);
        });
    }

    public static void handleCancelLinking() {
        Minecraft.getInstance().execute(() -> {
            WheelHighlightRenderer.clearSelection();
        });
    }

    public static void handleLinkResult(boolean success, BlockPos pos1, BlockPos pos2) {
        Minecraft.getInstance().execute(() -> {
            // Clear highlighting
            WheelHighlightRenderer.clearSelection();

            // Spawn appropriate particles
            if (success) {
                LinkingParticles.spawnLinkSuccessParticles(pos1, pos2);
            } else {
                LinkingParticles.spawnLinkErrorParticles(pos1);
            }
        });
    }
}
