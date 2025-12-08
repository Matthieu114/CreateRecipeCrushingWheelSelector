package com.enormeboze.crushingwheelrecipeselector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-saved data for crushing wheel links and recipe preferences.
 *
 * Wheels are LINKED in pairs using a shared group UUID.
 * Linked wheels share recipe preferences.
 */
public class CrushingWheelSelections extends SavedData {

    private static final String DATA_NAME = CrushingWheelRecipeSelector.MOD_ID + "_selections";

    // Map: BlockPos -> Group UUID (wheels in same group are linked)
    private final Map<BlockPos, UUID> wheelGroups = new HashMap<>();

    // Map: Group UUID -> Map<InputItemId, PreferredRecipeId>
    private final Map<UUID, Map<String, ResourceLocation>> groupPreferences = new HashMap<>();

    public CrushingWheelSelections() {
    }

    // ==================== LINKING ====================

    /**
     * Link a wheel to a group
     */
    public void linkWheel(BlockPos pos, UUID groupId) {
        wheelGroups.put(pos, groupId);
        groupPreferences.computeIfAbsent(groupId, k -> new HashMap<>());
        setDirty();
        CrushingWheelRecipeSelector.LOGGER.debug("Linked wheel at {} to group {}", pos, groupId);
    }

    /**
     * Unlink a wheel (internal method, doesn't affect pair)
     */
    private void unlinkWheelInternal(BlockPos pos) {
        UUID groupId = wheelGroups.remove(pos);
        if (groupId != null) {
            // Check if any other wheels still use this group
            boolean groupStillUsed = wheelGroups.containsValue(groupId);
            if (!groupStillUsed) {
                groupPreferences.remove(groupId);
            }
            setDirty();
        }
    }

    /**
     * Unlink a wheel and its pair (called when wheel is broken)
     * Both wheels in a pair must be unlinked when one is broken
     */
    public void unlinkWheel(BlockPos pos) {
        UUID groupId = wheelGroups.get(pos);
        if (groupId != null) {
            // Find and unlink ALL wheels in this group (the pair)
            List<BlockPos> wheelsInGroup = new ArrayList<>();
            for (Map.Entry<BlockPos, UUID> entry : wheelGroups.entrySet()) {
                if (entry.getValue().equals(groupId)) {
                    wheelsInGroup.add(entry.getKey());
                }
            }

            // Remove all wheels from this group
            for (BlockPos wheelPos : wheelsInGroup) {
                wheelGroups.remove(wheelPos);
                CrushingWheelRecipeSelector.LOGGER.debug("Unlinked wheel at {} (group {} dissolved)", wheelPos, groupId);
            }

            // Remove the group's preferences
            groupPreferences.remove(groupId);
            setDirty();

            CrushingWheelRecipeSelector.LOGGER.info("Dissolved group {} - {} wheel(s) unlinked", groupId, wheelsInGroup.size());
        }
    }

    /**
     * Remove a wheel completely (alias for unlinkWheel, for compatibility)
     */
    public void removeWheel(BlockPos pos) {
        unlinkWheel(pos);
    }

    /**
     * Check if a wheel is linked to any group
     */
    public boolean isWheelLinked(BlockPos pos) {
        return wheelGroups.containsKey(pos);
    }

    /**
     * Get the group ID for a wheel
     */
    public UUID getWheelGroup(BlockPos pos) {
        return wheelGroups.get(pos);
    }

    // ==================== PREFERENCES ====================

    /**
     * Set a recipe preference for a wheel (applies to entire group)
     */
    public void setPreferredRecipe(BlockPos wheelPos, String inputItemId, ResourceLocation recipeId) {
        UUID groupId = wheelGroups.get(wheelPos);
        if (groupId == null) {
            CrushingWheelRecipeSelector.LOGGER.warn("Cannot set preference - wheel at {} is not linked", wheelPos);
            return;
        }

        Map<String, ResourceLocation> prefs = groupPreferences.computeIfAbsent(groupId, k -> new HashMap<>());
        prefs.put(inputItemId, recipeId);
        setDirty();

        CrushingWheelRecipeSelector.LOGGER.debug("Set preference for group {}: {} -> {}", groupId, inputItemId, recipeId);
    }

    /**
     * Get the preferred recipe for a wheel and input item
     */
    public ResourceLocation getPreferredRecipe(BlockPos wheelPos, String inputItemId) {
        UUID groupId = wheelGroups.get(wheelPos);
        if (groupId == null) {
            return null;
        }

        Map<String, ResourceLocation> prefs = groupPreferences.get(groupId);
        if (prefs == null) {
            return null;
        }

        return prefs.get(inputItemId);
    }

    /**
     * Clear a recipe preference for a wheel
     */
    public void clearPreferredRecipe(BlockPos wheelPos, String inputItemId) {
        UUID groupId = wheelGroups.get(wheelPos);
        if (groupId == null) {
            return;
        }

        Map<String, ResourceLocation> prefs = groupPreferences.get(groupId);
        if (prefs != null) {
            prefs.remove(inputItemId);
            setDirty();
        }
    }

    /**
     * Get all preferences for a wheel's group
     */
    public Map<String, ResourceLocation> getAllPreferences(BlockPos wheelPos) {
        UUID groupId = wheelGroups.get(wheelPos);
        if (groupId == null) {
            return new HashMap<>();
        }

        Map<String, ResourceLocation> prefs = groupPreferences.get(groupId);
        return prefs != null ? new HashMap<>(prefs) : new HashMap<>();
    }

    // ==================== PERSISTENCE ====================

    public static CrushingWheelSelections load(CompoundTag tag, HolderLookup.Provider registries) {
        CrushingWheelSelections data = new CrushingWheelSelections();

        // Load wheel groups
        ListTag groupsList = tag.getList("wheelGroups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groupsList.size(); i++) {
            CompoundTag entry = groupsList.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            UUID groupId = entry.getUUID("group");
            data.wheelGroups.put(pos, groupId);
        }

        // Load group preferences
        ListTag prefsList = tag.getList("groupPreferences", Tag.TAG_COMPOUND);
        for (int i = 0; i < prefsList.size(); i++) {
            CompoundTag groupTag = prefsList.getCompound(i);
            UUID groupId = groupTag.getUUID("groupId");

            Map<String, ResourceLocation> prefs = new HashMap<>();
            ListTag itemPrefs = groupTag.getList("preferences", Tag.TAG_COMPOUND);
            for (int j = 0; j < itemPrefs.size(); j++) {
                CompoundTag prefTag = itemPrefs.getCompound(j);
                String inputItem = prefTag.getString("input");
                String recipeId = prefTag.getString("recipe");
                prefs.put(inputItem, ResourceLocation.parse(recipeId));
            }

            data.groupPreferences.put(groupId, prefs);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // Save wheel groups
        ListTag groupsList = new ListTag();
        for (Map.Entry<BlockPos, UUID> entry : wheelGroups.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("x", entry.getKey().getX());
            entryTag.putInt("y", entry.getKey().getY());
            entryTag.putInt("z", entry.getKey().getZ());
            entryTag.putUUID("group", entry.getValue());
            groupsList.add(entryTag);
        }
        tag.put("wheelGroups", groupsList);

        // Save group preferences
        ListTag prefsList = new ListTag();
        for (Map.Entry<UUID, Map<String, ResourceLocation>> groupEntry : groupPreferences.entrySet()) {
            CompoundTag groupTag = new CompoundTag();
            groupTag.putUUID("groupId", groupEntry.getKey());

            ListTag itemPrefs = new ListTag();
            for (Map.Entry<String, ResourceLocation> pref : groupEntry.getValue().entrySet()) {
                CompoundTag prefTag = new CompoundTag();
                prefTag.putString("input", pref.getKey());
                prefTag.putString("recipe", pref.getValue().toString());
                itemPrefs.add(prefTag);
            }
            groupTag.put("preferences", itemPrefs);

            prefsList.add(groupTag);
        }
        tag.put("groupPreferences", prefsList);

        return tag;
    }

    // ==================== ACCESS ====================

    public static CrushingWheelSelections get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(CrushingWheelSelections::new, CrushingWheelSelections::load),
                    DATA_NAME
            );
        }
        return null;
    }

    public static CrushingWheelSelections get(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CrushingWheelSelections::new, CrushingWheelSelections::load),
                DATA_NAME
        );
    }
}