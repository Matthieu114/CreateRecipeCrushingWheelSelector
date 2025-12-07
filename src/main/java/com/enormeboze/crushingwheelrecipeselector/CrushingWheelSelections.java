package com.enormeboze.crushingwheelrecipeselector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores recipe selections for each crushing wheel position
 * This data is saved with the world
 */
public class CrushingWheelSelections extends SavedData {

    private static final String DATA_NAME = "crushing_wheel_selections";
    
    // Map: BlockPos -> Map<InputItemId, PreferredRecipeId>
    private final Map<BlockPos, Map<String, ResourceLocation>> wheelSelections = new HashMap<>();

    public CrushingWheelSelections() {
    }

    /**
     * Get the saved data for a world
     */
    public static CrushingWheelSelections get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(
                CrushingWheelSelections::new,
                CrushingWheelSelections::load,
                null
            ),
            DATA_NAME
        );
    }

    /**
     * Set the preferred recipe for a specific wheel and input item
     */
    public void setPreferredRecipe(BlockPos wheelPos, String inputItemId, ResourceLocation recipeId) {
        wheelSelections.computeIfAbsent(wheelPos, k -> new HashMap<>())
            .put(inputItemId, recipeId);
        setDirty();
        
        CrushingWheelRecipeSelector.LOGGER.info("Set recipe for wheel at {}: {} -> {}", 
            wheelPos, inputItemId, recipeId);
    }

    /**
     * Get the preferred recipe for a specific wheel and input item
     */
    public ResourceLocation getPreferredRecipe(BlockPos wheelPos, String inputItemId) {
        Map<String, ResourceLocation> selections = wheelSelections.get(wheelPos);
        if (selections == null) {
            return null;
        }
        return selections.get(inputItemId);
    }

    /**
     * Check if a wheel has any selections
     */
    public boolean hasSelections(BlockPos wheelPos) {
        return wheelSelections.containsKey(wheelPos) && !wheelSelections.get(wheelPos).isEmpty();
    }

    /**
     * Get all selections for a specific wheel
     */
    public Map<String, ResourceLocation> getWheelSelections(BlockPos wheelPos) {
        return new HashMap<>(wheelSelections.getOrDefault(wheelPos, new HashMap<>()));
    }

    /**
     * Remove all selections for a wheel (when broken)
     */
    public void removeWheel(BlockPos wheelPos) {
        if (wheelSelections.remove(wheelPos) != null) {
            setDirty();
            CrushingWheelRecipeSelector.LOGGER.info("Removed selections for wheel at {}", wheelPos);
        }
    }

    /**
     * Clear a specific selection for a wheel
     */
    public void clearSelection(BlockPos wheelPos, String inputItemId) {
        Map<String, ResourceLocation> selections = wheelSelections.get(wheelPos);
        if (selections != null) {
            selections.remove(inputItemId);
            if (selections.isEmpty()) {
                wheelSelections.remove(wheelPos);
            }
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag wheelsList = new ListTag();

        for (Map.Entry<BlockPos, Map<String, ResourceLocation>> entry : wheelSelections.entrySet()) {
            CompoundTag wheelTag = new CompoundTag();
            
            // Save position
            BlockPos pos = entry.getKey();
            wheelTag.putLong("pos", pos.asLong());
            
            // Save selections for this wheel
            CompoundTag selectionsTag = new CompoundTag();
            for (Map.Entry<String, ResourceLocation> selection : entry.getValue().entrySet()) {
                selectionsTag.putString(selection.getKey(), selection.getValue().toString());
            }
            wheelTag.put("selections", selectionsTag);
            
            wheelsList.add(wheelTag);
        }

        tag.put("wheels", wheelsList);
        CrushingWheelRecipeSelector.LOGGER.info("Saved {} crushing wheel selections", wheelSelections.size());
        
        return tag;
    }

    public static CrushingWheelSelections load(CompoundTag tag, HolderLookup.Provider provider) {
        CrushingWheelSelections data = new CrushingWheelSelections();
        
        ListTag wheelsList = tag.getList("wheels", Tag.TAG_COMPOUND);
        
        for (Tag wheelTagRaw : wheelsList) {
            CompoundTag wheelTag = (CompoundTag) wheelTagRaw;
            
            // Load position
            BlockPos pos = BlockPos.of(wheelTag.getLong("pos"));
            
            // Load selections for this wheel
            CompoundTag selectionsTag = wheelTag.getCompound("selections");
            Map<String, ResourceLocation> selections = new HashMap<>();
            
            for (String key : selectionsTag.getAllKeys()) {
                String recipeIdString = selectionsTag.getString(key);
                try {
                    ResourceLocation recipeId = ResourceLocation.parse(recipeIdString);
                    selections.put(key, recipeId);
                } catch (Exception e) {
                    CrushingWheelRecipeSelector.LOGGER.error("Failed to parse recipe ID: {}", recipeIdString, e);
                }
            }
            
            if (!selections.isEmpty()) {
                data.wheelSelections.put(pos, selections);
            }
        }
        
        CrushingWheelRecipeSelector.LOGGER.info("Loaded {} crushing wheel selections", data.wheelSelections.size());
        return data;
    }
}
