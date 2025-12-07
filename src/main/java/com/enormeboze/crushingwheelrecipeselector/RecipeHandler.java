package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class RecipeHandler {

    // Cache of conflicting recipes per input item ID
    private static Map<String, List<RecipeConflict>> conflictingRecipes = new HashMap<>();

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            CrushingWheelRecipeSelector.LOGGER.info("World loaded, scanning for conflicting crushing recipes...");
            scanForConflicts(level.getRecipeManager(), level.registryAccess());
        }
    }

    private static void scanForConflicts(RecipeManager recipeManager, RegistryAccess registryAccess) {
        conflictingRecipes.clear();

        // Get all recipes and filter for Create crushing recipes
        var allRecipes = recipeManager.getRecipes();

        // Group recipes by their input item
        Map<String, List<RecipeHolder<CrushingRecipe>>> crushingRecipesByInput = new HashMap<>();

        for (RecipeHolder<?> recipeHolder : allRecipes) {
            // Check if this is specifically a Create crushing recipe
            if (recipeHolder.value() instanceof CrushingRecipe crushingRecipe) {
                try {
                    // Get the recipe's ingredients
                    var ingredients = crushingRecipe.getIngredients();

                    if (!ingredients.isEmpty()) {
                        var ingredient = ingredients.get(0);
                        var items = ingredient.getItems();

                        for (ItemStack item : items) {
                            String itemId = getItemId(item);
                            crushingRecipesByInput.computeIfAbsent(itemId, k -> new ArrayList<>())
                                    .add((RecipeHolder<CrushingRecipe>) recipeHolder);
                        }
                    }
                } catch (Exception e) {
                    CrushingWheelRecipeSelector.LOGGER.error("Error processing recipe: {}", recipeHolder.id(), e);
                }
            }
        }

        // Find conflicts (items with more than one recipe)
        for (Map.Entry<String, List<RecipeHolder<CrushingRecipe>>> entry : crushingRecipesByInput.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<RecipeConflict> conflicts = entry.getValue().stream()
                        .map(holder -> new RecipeConflict(
                                holder.id(),
                                getRecipeOutputs(holder, registryAccess)
                        ))
                        .collect(Collectors.toList());

                conflictingRecipes.put(entry.getKey(), conflicts);

                CrushingWheelRecipeSelector.LOGGER.warn("Found {} conflicting recipes for item: {}",
                        entry.getValue().size(), entry.getKey());

                for (RecipeConflict conflict : conflicts) {
                    CrushingWheelRecipeSelector.LOGGER.info("  - Recipe: {} -> {}",
                            conflict.recipeId(), conflict.outputsAsString());
                }

                // Auto-populate config with first recipe as default if not already set
                if (Config.getPreferredOutput(entry.getKey()) == null) {
                    String firstOutput = conflicts.get(0).primaryOutput();
                    Config.setPreferredOutput(entry.getKey(), firstOutput);
                    CrushingWheelRecipeSelector.LOGGER.info("Auto-set default output for {}: {}",
                            entry.getKey(), firstOutput);
                }
            }
        }

        if (conflictingRecipes.isEmpty()) {
            CrushingWheelRecipeSelector.LOGGER.info("No conflicting crushing recipes found.");
        } else {
            CrushingWheelRecipeSelector.LOGGER.info("Found {} items with conflicting recipes.",
                    conflictingRecipes.size());
        }
    }

    private static String getItemId(ItemStack stack) {
        ResourceLocation registryName = stack.getItem().builtInRegistryHolder().key().location();
        return registryName.toString();
    }

    private static List<String> getRecipeOutputs(RecipeHolder<CrushingRecipe> holder, RegistryAccess registryAccess) {
        List<String> outputs = new ArrayList<>();
        try {
            CrushingRecipe recipe = holder.value();

            // Get the main result
            ItemStack result = recipe.getResultItem(registryAccess);
            if (!result.isEmpty()) {
                outputs.add(getItemId(result) + " x" + result.getCount());
            }

            // Get secondary/rolled results if any
            var rollableResults = recipe.getRollableResults();
            for (var rollable : rollableResults) {
                ItemStack stack = rollable.getStack();
                if (!stack.isEmpty()) {
                    float chance = rollable.getChance();
                    outputs.add(getItemId(stack) + " x" + stack.getCount() + " (" + (int)(chance * 100) + "%)");
                }
            }
        } catch (Exception e) {
            CrushingWheelRecipeSelector.LOGGER.error("Error getting recipe outputs", e);
        }
        return outputs;
    }

    public static Map<String, List<RecipeConflict>> getConflictingRecipes() {
        return new HashMap<>(conflictingRecipes);
    }

    public static List<RecipeConflict> getConflictsForItem(String itemId) {
        return conflictingRecipes.getOrDefault(itemId, new ArrayList<>());
    }

    /**
     * Represents a conflicting recipe with its outputs
     */
    public record RecipeConflict(ResourceLocation recipeId, List<String> outputs) {

        public String primaryOutput() {
            return outputs.isEmpty() ? "unknown" : outputs.get(0).split(" ")[0];
        }

        public String outputsAsString() {
            return String.join(", ", outputs);
        }
    }
}