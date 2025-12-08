package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side storage for synced selections from server
 * This is temporary storage used by the GUI to display saved selections
 */
public class ClientSelectionCache {

    // Map: BlockPos -> Map<InputItemId, PreferredRecipeId>
    private static final Map<BlockPos, Map<String, ResourceLocation>> cachedSelections = new HashMap<>();

    /**
     * Update all cached selections for a wheel (called when receiving sync packet)
     */
    public static void updateSelections(BlockPos wheelPos, Map<String, ResourceLocation> selections) {
        if (selections.isEmpty()) {
            cachedSelections.remove(wheelPos);
        } else {
            cachedSelections.put(wheelPos, new HashMap<>(selections));
        }
    }

    /**
     * Update a single selection for a wheel and item (called when confirming)
     */
    public static void updateSelection(BlockPos wheelPos, String inputItemId, ResourceLocation recipeId) {
        Map<String, ResourceLocation> selections = cachedSelections.computeIfAbsent(wheelPos, k -> new HashMap<>());
        selections.put(inputItemId, recipeId);
    }

    /**
     * Clear a single selection for a wheel and item (called when clearing)
     */
    public static void clearSelection(BlockPos wheelPos, String inputItemId) {
        Map<String, ResourceLocation> selections = cachedSelections.get(wheelPos);
        if (selections != null) {
            selections.remove(inputItemId);
            if (selections.isEmpty()) {
                cachedSelections.remove(wheelPos);
            }
        }
    }

    /**
     * Get the cached selection for a specific wheel and item
     */
    public static ResourceLocation getSelection(BlockPos wheelPos, String inputItemId) {
        Map<String, ResourceLocation> selections = cachedSelections.get(wheelPos);
        if (selections == null) {
            return null;
        }
        return selections.get(inputItemId);
    }

    /**
     * Check if there's a cached selection for this wheel and item
     */
    public static boolean hasSelection(BlockPos wheelPos, String inputItemId) {
        return getSelection(wheelPos, inputItemId) != null;
    }

    /**
     * Clear cached selections for a wheel
     */
    public static void clearWheel(BlockPos wheelPos) {
        cachedSelections.remove(wheelPos);
    }

    /**
     * Clear all cached selections (e.g., when disconnecting from server)
     */
    public static void clearAll() {
        cachedSelections.clear();
    }
}