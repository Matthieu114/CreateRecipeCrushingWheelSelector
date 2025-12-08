package com.enormeboze.crushingwheelrecipeselector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Stores recipe selections by "Link Group" (UUID).
 * Multiple wheels can point to the same UUID, sharing the same config.
 */
public class CrushingWheelSelections extends SavedData {

    private static final String DATA_NAME = "crushing_wheel_selections";

    // Map: BlockPos -> Group UUID
    private final Map<BlockPos, UUID> wheelToGroup = new HashMap<>();

    // Map: Group UUID -> (InputItemId -> PreferredRecipeId)
    private final Map<UUID, Map<String, ResourceLocation>> groupRecipes = new HashMap<>();

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
     * Creates a link between two positions.
     * If one already has a group, the other joins it.
     * If both have different groups, group A is merged into group B.
     */
    public void linkWheels(BlockPos pos1, BlockPos pos2) {
        UUID id1 = wheelToGroup.get(pos1);
        UUID id2 = wheelToGroup.get(pos2);

        if (id1 == null && id2 == null) {
            // New group for both
            UUID newGroup = UUID.randomUUID();
            wheelToGroup.put(pos1, newGroup);
            wheelToGroup.put(pos2, newGroup);
        } else if (id1 != null && id2 == null) {
            // 2 joins 1
            wheelToGroup.put(pos2, id1);
        } else if (id1 == null && id2 != null) {
            // 1 joins 2
            wheelToGroup.put(pos1, id2);
        } else if (!id1.equals(id2)) {
            // Merge 1 into 2
            Map<String, ResourceLocation> recipes1 = groupRecipes.getOrDefault(id1, new HashMap<>());
            Map<String, ResourceLocation> recipes2 = groupRecipes.computeIfAbsent(id2, k -> new HashMap<>());

            // Merge recipes (target group takes priority, but keep unique ones from source)
            recipes1.forEach(recipes2::putIfAbsent);

            // Reassign all wheels from group 1 to group 2
            List<BlockPos> wheelsToMigrate = new ArrayList<>();
            for (Map.Entry<BlockPos, UUID> entry : wheelToGroup.entrySet()) {
                if (entry.getValue().equals(id1)) {
                    wheelsToMigrate.add(entry.getKey());
                }
            }
            for (BlockPos p : wheelsToMigrate) {
                wheelToGroup.put(p, id2);
            }

            // Remove old group data
            groupRecipes.remove(id1);
        }
        setDirty();
    }

    /**
     * Set preference for the group this wheel belongs to.
     * If wheel has no group, a new one is created.
     */
    public void setPreferredRecipe(BlockPos wheelPos, String inputItemId, ResourceLocation recipeId) {
        UUID groupId = wheelToGroup.computeIfAbsent(wheelPos, k -> UUID.randomUUID());
        groupRecipes.computeIfAbsent(groupId, k -> new HashMap<>()).put(inputItemId, recipeId);
        setDirty();
    }

    public ResourceLocation getPreferredRecipe(BlockPos wheelPos, String inputItemId) {
        UUID groupId = wheelToGroup.get(wheelPos);
        if (groupId == null) return null;

        Map<String, ResourceLocation> recipes = groupRecipes.get(groupId);
        return recipes != null ? recipes.get(inputItemId) : null;
    }

    public Map<String, ResourceLocation> getWheelSelections(BlockPos wheelPos) {
        UUID groupId = wheelToGroup.get(wheelPos);
        if (groupId == null) return new HashMap<>();
        return new HashMap<>(groupRecipes.getOrDefault(groupId, new HashMap<>()));
    }

    /**
     * Removes a wheel from the tracking system. If the link is a pair or a group,
     * breaking one wheel destroys the entire link by removing all associated mappings.
     */
    public void removeWheel(BlockPos wheelPos) {
        // 1. Get the group ID of the broken wheel and remove its position mapping
        UUID groupId = wheelToGroup.remove(wheelPos);

        if (groupId != null) {
            // 2. Find all remaining wheels that belonged to this group ID
            List<BlockPos> remainingWheels = new ArrayList<>();
            for (Map.Entry<BlockPos, UUID> entry : wheelToGroup.entrySet()) {
                if (entry.getValue().equals(groupId)) {
                    remainingWheels.add(entry.getKey());
                }
            }

            // 3. Remove mappings for ALL remaining wheels in the group to "unlink" them.
            for (BlockPos remainingPos : remainingWheels) {
                wheelToGroup.remove(remainingPos);
            }

            // 4. Remove the group's recipe data since the link is fully destroyed.
            groupRecipes.remove(groupId);

            setDirty();

            CrushingWheelRecipeSelector.LOGGER.info(
                    "Link destroyed by breaking wheel at {}. Unlinked {} remaining wheel(s) from group {}.",
                    wheelPos, remainingWheels.size(), groupId
            );
        }
    }

    public void clearPreferredRecipe(BlockPos wheelPos, String inputItemId) {
        UUID groupId = wheelToGroup.get(wheelPos);
        if (groupId != null) {
            Map<String, ResourceLocation> recipes = groupRecipes.get(groupId);
            if (recipes != null) {
                recipes.remove(inputItemId);
                if (recipes.isEmpty()) {
                    // We don't delete the group ID here so the link persists even if empty
                    // but we could if we wanted stricter cleanup
                }
                setDirty();
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag wheelsTag = new ListTag();
        for (Map.Entry<BlockPos, UUID> entry : wheelToGroup.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", entry.getKey().asLong());
            t.putUUID("group", entry.getValue());
            wheelsTag.add(t);
        }
        tag.put("wheel_mappings", wheelsTag);

        ListTag groupsTag = new ListTag();
        for (Map.Entry<UUID, Map<String, ResourceLocation>> entry : groupRecipes.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", entry.getKey());

            CompoundTag recipes = new CompoundTag();
            entry.getValue().forEach((item, recipe) -> recipes.putString(item, recipe.toString()));
            t.put("recipes", recipes);

            groupsTag.add(t);
        }
        tag.put("group_data", groupsTag);

        return tag;
    }



    /**
     * Check if the given wheel position belongs to any link group.
     */
    public boolean isWheelLinked(BlockPos wheelPos) {
        // If a wheel has a group UUID, it means it's been explicitly configured/linked.
        return wheelToGroup.containsKey(wheelPos);
    }

    public static CrushingWheelSelections load(CompoundTag tag, HolderLookup.Provider provider) {
        CrushingWheelSelections data = new CrushingWheelSelections();

        // Load Mappings
        ListTag wheelsTag = tag.getList("wheel_mappings", Tag.TAG_COMPOUND);
        for (Tag t : wheelsTag) {
            CompoundTag ct = (CompoundTag) t;
            data.wheelToGroup.put(BlockPos.of(ct.getLong("pos")), ct.getUUID("group"));
        }

        // Load Recipes
        ListTag groupsTag = tag.getList("group_data", Tag.TAG_COMPOUND);
        for (Tag t : groupsTag) {
            CompoundTag ct = (CompoundTag) t;
            UUID id = ct.getUUID("id");
            CompoundTag recipesTag = ct.getCompound("recipes");

            Map<String, ResourceLocation> recipes = new HashMap<>();
            for (String key : recipesTag.getAllKeys()) {
                try {
                    recipes.put(key, ResourceLocation.parse(recipesTag.getString(key)));
                } catch (Exception ignored) {}
            }
            data.groupRecipes.put(id, recipes);
        }
        return data;
    }
}