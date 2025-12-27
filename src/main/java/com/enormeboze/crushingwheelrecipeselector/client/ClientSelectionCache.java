package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache for recipe selections.
 * Updated when server sends sync packets.
 */
@OnlyIn(Dist.CLIENT)
public class ClientSelectionCache {

    private static BlockPos currentWheelPos = null;
    
    // Map: wheelPos -> (inputItemId -> recipeId)
    private static final Map<BlockPos, Map<String, ResourceLocation>> selections = new HashMap<>();

    public static void updateSelections(BlockPos wheelPos, Map<String, ResourceLocation> newSelections) {
        currentWheelPos = wheelPos;
        selections.put(wheelPos, new HashMap<>(newSelections));
    }

    public static void updateSelection(BlockPos wheelPos, String inputItemId, ResourceLocation recipeId) {
        selections.computeIfAbsent(wheelPos, k -> new HashMap<>()).put(inputItemId, recipeId);
    }

    public static void clearSelection(BlockPos wheelPos, String inputItemId) {
        Map<String, ResourceLocation> wheelSelections = selections.get(wheelPos);
        if (wheelSelections != null) {
            wheelSelections.remove(inputItemId);
        }
    }

    public static BlockPos getCurrentWheelPos() {
        return currentWheelPos;
    }

    public static Map<String, ResourceLocation> getCurrentSelections() {
        if (currentWheelPos == null) {
            return new HashMap<>();
        }
        Map<String, ResourceLocation> wheelSelections = selections.get(currentWheelPos);
        return wheelSelections != null ? new HashMap<>(wheelSelections) : new HashMap<>();
    }

    public static ResourceLocation getSelection(String inputItemId) {
        if (currentWheelPos == null) {
            return null;
        }
        Map<String, ResourceLocation> wheelSelections = selections.get(currentWheelPos);
        return wheelSelections != null ? wheelSelections.get(inputItemId) : null;
    }

    public static ResourceLocation getSelection(BlockPos wheelPos, String inputItemId) {
        Map<String, ResourceLocation> wheelSelections = selections.get(wheelPos);
        return wheelSelections != null ? wheelSelections.get(inputItemId) : null;
    }

    public static boolean hasSelection(BlockPos wheelPos, String inputItemId) {
        return getSelection(wheelPos, inputItemId) != null;
    }

    public static void clear() {
        currentWheelPos = null;
        selections.clear();
    }
}
