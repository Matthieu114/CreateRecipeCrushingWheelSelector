package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID)
public class RecipeHandler {

    // Cache of conflicting recipes per input item ID
    private static Map<String, List<RecipeConflict>> conflictingRecipes = new HashMap<>();

    // Flag to track if we've already scanned
    private static boolean hasScanned = false;

    // Store registry access for later use
    private static RegistryAccess cachedRegistryAccess = null;

    /**
     * Clean up when server stops to prevent memory leaks
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        hasScanned = false;
        conflictingRecipes.clear();
        cachedRegistryAccess = null;
        CrushingWheelRecipeSelector.LOGGER.debug("Cleared recipe cache on server stop");
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            cachedRegistryAccess = level.registryAccess();
            // Don't scan here - wait for recipes to be fully loaded
            CrushingWheelRecipeSelector.LOGGER.info("World loaded, waiting for recipes to be ready...");
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            // Reset when world unloads so we rescan on next world
            hasScanned = false;
            conflictingRecipes.clear();
            cachedRegistryAccess = null;
            CrushingWheelRecipeSelector.LOGGER.info("World unloaded, clearing recipe cache");
        }
    }

    /**
     * Called when recipes are synced to client - this is when all mod recipes are available
     */
    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        // This fires on the CLIENT when recipes are synced from server
        // All mod recipes should be available now
        CrushingWheelRecipeSelector.LOGGER.info("Recipes updated event fired, scanning for conflicts...");

        RecipeManager recipeManager = event.getRecipeManager();
        if (Minecraft.getInstance().level != null) {
            scanForConflicts(recipeManager, Minecraft.getInstance().level.registryAccess());
        }
    }

    /**
     * Force a rescan of recipes - can be called manually if needed
     */
    public static void forceRescan(Level level) {
        hasScanned = false;
        CrushingWheelRecipeSelector.LOGGER.info("Forcing recipe rescan...");
        scanForConflicts(level.getRecipeManager(), level.registryAccess());
    }

    private static void scanForConflicts(RecipeManager recipeManager, RegistryAccess registryAccess) {
        conflictingRecipes.clear();

        // Get all recipes and filter for Create crushing recipes
        var allRecipes = recipeManager.getRecipes();

        int totalRecipes = 0;
        int crushingRecipes = 0;

        // Group recipes by their input item
        Map<String, List<RecipeHolder<CrushingRecipe>>> crushingRecipesByInput = new HashMap<>();

        for (RecipeHolder<?> recipeHolder : allRecipes) {
            totalRecipes++;
            // Check if this is specifically a Create crushing recipe
            if (recipeHolder.value() instanceof CrushingRecipe crushingRecipe) {
                crushingRecipes++;
                try {
                    // Get the recipe's ingredients
                    var ingredients = crushingRecipe.getIngredients();

                    if (!ingredients.isEmpty()) {
                        var ingredient = ingredients.get(0);
                        var items = ingredient.getItems();

                        for (ItemStack item : items) {
                            String itemId = getItemId(item);
                            @SuppressWarnings("unchecked")
                            RecipeHolder<CrushingRecipe> typedHolder = (RecipeHolder<CrushingRecipe>) recipeHolder;
                            crushingRecipesByInput.computeIfAbsent(itemId, k -> new ArrayList<>())
                                    .add(typedHolder);
                        }
                    }
                } catch (Exception e) {
                    CrushingWheelRecipeSelector.LOGGER.error("Error processing recipe: {}", recipeHolder.id(), e);
                }
            }
        }

        CrushingWheelRecipeSelector.LOGGER.info("Scanned {} total recipes, found {} crushing recipes for {} unique items",
                totalRecipes, crushingRecipes, crushingRecipesByInput.size());

        // Find conflicts (items with more than one recipe WITH DIFFERENT OUTPUTS)
        for (Map.Entry<String, List<RecipeHolder<CrushingRecipe>>> entry : crushingRecipesByInput.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Get all recipes with their outputs
                List<RecipeConflict> allConflicts = entry.getValue().stream()
                        .map(holder -> new RecipeConflict(
                                holder.id(),
                                getRecipeOutputs(holder, registryAccess),
                                getOutputSignature(holder, registryAccess)
                        ))
                        .collect(Collectors.toList());

                // Filter to only keep recipes with UNIQUE outputs
                List<RecipeConflict> uniqueConflicts = filterDuplicateOutputs(allConflicts);

                // Only add if there's still more than one unique recipe
                if (uniqueConflicts.size() > 1) {
                    conflictingRecipes.put(entry.getKey(), uniqueConflicts);

                    CrushingWheelRecipeSelector.LOGGER.info("Found {} unique conflicting recipes for item: {} (filtered from {} total)",
                            uniqueConflicts.size(), entry.getKey(), allConflicts.size());

                    for (RecipeConflict conflict : uniqueConflicts) {
                        CrushingWheelRecipeSelector.LOGGER.debug("  - Recipe: {} -> {}",
                                conflict.recipeId(), conflict.outputsAsString());
                    }
                } else if (allConflicts.size() > 1) {
                    CrushingWheelRecipeSelector.LOGGER.debug("Filtered out {} duplicate recipes for item: {} (all have same outputs)",
                            allConflicts.size(), entry.getKey());
                }
            }
        }

        hasScanned = true;

        if (conflictingRecipes.isEmpty()) {
            CrushingWheelRecipeSelector.LOGGER.warn("No conflicting crushing recipes found! This might indicate an issue.");
            CrushingWheelRecipeSelector.LOGGER.warn("  - Total recipes scanned: {}", totalRecipes);
            CrushingWheelRecipeSelector.LOGGER.warn("  - Crushing recipes found: {}", crushingRecipes);
            CrushingWheelRecipeSelector.LOGGER.warn("  - Unique input items: {}", crushingRecipesByInput.size());
        } else {
            CrushingWheelRecipeSelector.LOGGER.info("=== Found {} items with conflicting recipes ===",
                    conflictingRecipes.size());
            for (String itemId : conflictingRecipes.keySet()) {
                CrushingWheelRecipeSelector.LOGGER.info("  - {}: {} recipes", itemId, conflictingRecipes.get(itemId).size());
            }
        }
    }

    /**
     * Filter out recipes that have identical outputs
     * Keep only one recipe per unique output signature
     */
    private static List<RecipeConflict> filterDuplicateOutputs(List<RecipeConflict> conflicts) {
        Map<String, RecipeConflict> uniqueByOutput = new LinkedHashMap<>();

        for (RecipeConflict conflict : conflicts) {
            String signature = conflict.outputSignature();
            // Only keep the first recipe with each unique output signature
            if (!uniqueByOutput.containsKey(signature)) {
                uniqueByOutput.put(signature, conflict);
            } else {
                CrushingWheelRecipeSelector.LOGGER.debug("Filtering duplicate recipe {} (same outputs as {})",
                        conflict.recipeId(), uniqueByOutput.get(signature).recipeId());
            }
        }

        return new ArrayList<>(uniqueByOutput.values());
    }

    /**
     * Create a signature string representing all outputs of a recipe
     * Used to detect recipes with identical outputs
     */
    private static String getOutputSignature(RecipeHolder<CrushingRecipe> holder, RegistryAccess registryAccess) {
        List<String> outputParts = new ArrayList<>();

        try {
            CrushingRecipe recipe = holder.value();

            // Get the main result
            ItemStack result = recipe.getResultItem(registryAccess);
            if (!result.isEmpty()) {
                outputParts.add(getItemId(result) + ":" + result.getCount());
            }

            // Get secondary/rolled results
            var rollableResults = recipe.getRollableResults();
            for (var rollable : rollableResults) {
                ItemStack stack = rollable.getStack();
                if (!stack.isEmpty()) {
                    float chance = rollable.getChance();
                    outputParts.add(getItemId(stack) + ":" + stack.getCount() + ":" + (int)(chance * 1000));
                }
            }
        } catch (Exception e) {
            CrushingWheelRecipeSelector.LOGGER.error("Error getting output signature", e);
        }

        // Sort to ensure consistent ordering
        Collections.sort(outputParts);
        return String.join("|", outputParts);
    }

    private static String getItemId(ItemStack stack) {
        ResourceLocation registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
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

    /**
     * Get cached conflicting recipes - returns unmodifiable view (no copy)
     */
    public static Map<String, List<RecipeConflict>> getConflictingRecipes() {
        return Collections.unmodifiableMap(conflictingRecipes);
    }

    /**
     * Get conflicts for a specific item - returns empty list constant if not found
     */
    private static final List<RecipeConflict> EMPTY_CONFLICTS = Collections.emptyList();

    public static List<RecipeConflict> getConflictsForItem(String itemId) {
        List<RecipeConflict> conflicts = conflictingRecipes.get(itemId);
        return conflicts != null ? conflicts : EMPTY_CONFLICTS;
    }

    /**
     * Check if recipes have been scanned
     */
    public static boolean isScanned() {
        return hasScanned;
    }

    /**
     * Represents a conflicting recipe with its outputs
     */
    public record RecipeConflict(ResourceLocation recipeId, List<String> outputs, String outputSignature) {

        public String primaryOutput() {
            return outputs.isEmpty() ? "unknown" : outputs.get(0).split(" ")[0];
        }

        public String outputsAsString() {
            return String.join(", ", outputs);
        }
    }
}