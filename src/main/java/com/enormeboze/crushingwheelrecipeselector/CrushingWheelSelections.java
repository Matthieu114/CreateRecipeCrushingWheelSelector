package com.enormeboze.crushingwheelrecipeselector;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-saved data for crushing wheel links and recipe preferences.
 *
 * Wheels are LINKED in pairs using a shared group UUID.
 * Linked wheels share recipe preferences.
 * 
 * PERFORMANCE OPTIMIZATION:
 * We cache controller positions (the block between linked wheel pairs) so that
 * the mixin can do a single O(1) HashSet lookup to skip processing for unlinked wheels.
 */
public class CrushingWheelSelections extends SavedData {

    private static final String DATA_NAME = CrushingWheelRecipeSelector.MOD_ID + "_selections";

    // Map: BlockPos -> Group UUID (wheels in same group are linked)
    private final Map<BlockPos, UUID> wheelGroups = new HashMap<>();

    // Map: Group UUID -> Map<InputItemId, PreferredRecipeId>
    private final Map<UUID, Map<String, ResourceLocation>> groupPreferences = new HashMap<>();

    // PERFORMANCE CACHE: Set of controller positions that have linked wheels nearby
    private final Set<BlockPos> activeControllerPositions = new HashSet<>();

    public CrushingWheelSelections() {
    }

    // ==================== CONTROLLER CACHE ====================

    /**
     * Fast check for the mixin - returns true if this controller position
     * is between linked crushing wheels.
     */
    public boolean isControllerActive(BlockPos controllerPos) {
        return activeControllerPositions.contains(controllerPos);
    }

    /**
     * Rebuild the controller position cache from wheel groups.
     */
    private void rebuildControllerCache() {
        activeControllerPositions.clear();

        // Group wheels by their group UUID
        Map<UUID, List<BlockPos>> wheelsByGroup = new HashMap<>();
        for (Map.Entry<BlockPos, UUID> entry : wheelGroups.entrySet()) {
            wheelsByGroup.computeIfAbsent(entry.getValue(), k -> new ArrayList<>(2))
                    .add(entry.getKey());
        }

        // For each group with exactly 2 wheels, calculate the controller position
        for (Map.Entry<UUID, List<BlockPos>> entry : wheelsByGroup.entrySet()) {
            List<BlockPos> wheels = entry.getValue();
            if (wheels.size() == 2) {
                BlockPos wheel1 = wheels.get(0);
                BlockPos wheel2 = wheels.get(1);

                BlockPos controllerPos = CrushingWheelPairHelper.getControllerPosition(wheel1, wheel2);
                activeControllerPositions.add(controllerPos);
            }
        }

        CrushingWheelRecipeSelector.LOGGER.debug("Rebuilt controller cache: {} active controller(s)", 
                activeControllerPositions.size());
    }

    // ==================== LINKING ====================

    public void linkWheel(BlockPos pos, UUID groupId) {
        wheelGroups.put(pos, groupId);
        groupPreferences.computeIfAbsent(groupId, k -> new HashMap<>());
        rebuildControllerCache();
        setDirty();
        CrushingWheelRecipeSelector.LOGGER.debug("Linked wheel at {} to group {}", pos, groupId);
    }

    public void unlinkWheel(BlockPos pos) {
        UUID groupId = wheelGroups.get(pos);
        if (groupId != null) {
            List<BlockPos> wheelsInGroup = new ArrayList<>();
            for (Map.Entry<BlockPos, UUID> entry : wheelGroups.entrySet()) {
                if (entry.getValue().equals(groupId)) {
                    wheelsInGroup.add(entry.getKey());
                }
            }

            for (BlockPos wheelPos : wheelsInGroup) {
                wheelGroups.remove(wheelPos);
                CrushingWheelRecipeSelector.LOGGER.debug("Unlinked wheel at {} (group {} dissolved)", wheelPos, groupId);
            }

            groupPreferences.remove(groupId);
            rebuildControllerCache();
            setDirty();

            CrushingWheelRecipeSelector.LOGGER.debug("Dissolved group {} - {} wheel(s) unlinked", groupId, wheelsInGroup.size());
        }
    }

    public void removeWheel(BlockPos pos) {
        unlinkWheel(pos);
    }

    public boolean isWheelLinked(BlockPos pos) {
        return wheelGroups.containsKey(pos);
    }

    public UUID getWheelGroup(BlockPos pos) {
        return wheelGroups.get(pos);
    }

    // ==================== PREFERENCES ====================

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

    private static final Map<String, ResourceLocation> EMPTY_PREFERENCES = Collections.emptyMap();

    public Map<String, ResourceLocation> getAllPreferences(BlockPos wheelPos) {
        UUID groupId = wheelGroups.get(wheelPos);
        if (groupId == null) {
            return EMPTY_PREFERENCES;
        }

        Map<String, ResourceLocation> prefs = groupPreferences.get(groupId);
        return prefs != null ? Collections.unmodifiableMap(prefs) : EMPTY_PREFERENCES;
    }

    // ==================== PERSISTENCE ====================

    public static CrushingWheelSelections load(CompoundTag tag) {
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
                prefs.put(inputItem, new ResourceLocation(recipeId));
            }

            data.groupPreferences.put(groupId, prefs);
        }

        data.rebuildControllerCache();

        CrushingWheelRecipeSelector.LOGGER.info("Loaded {} wheel groups, {} group preferences, {} active controllers",
                data.wheelGroups.size(), data.groupPreferences.size(), data.activeControllerPositions.size());

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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
                    CrushingWheelSelections::load,
                    CrushingWheelSelections::new,
                    DATA_NAME
            );
        }
        return null;
    }

    public static CrushingWheelSelections get(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                CrushingWheelSelections::load,
                CrushingWheelSelections::new,
                DATA_NAME
        );
    }
}
