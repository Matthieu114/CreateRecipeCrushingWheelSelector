package com.enormeboze.crushingwheelrecipeselector.mixin;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mixin to intercept Create's crushing wheel recipe selection.
 *
 * PERFORMANCE OPTIMIZATIONS:
 * 1. Early exit for unlinked controllers using O(1) HashSet lookup
 * 2. Reusable ThreadLocal list to avoid allocations
 * 3. No debug logging in hot path
 * 4. Cached direction array
 */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class CrushingWheelControllerMixin {

    // Reusable list to avoid allocations on every recipe lookup
    @Unique
    private static final ThreadLocal<List<BlockPos>> crushingwheelrecipeselector$wheelPositions =
            ThreadLocal.withInitial(() -> new ArrayList<>(4));

    // Cache all directions for checking adjacent wheels
    @Unique
    private static final Direction[] crushingwheelrecipeselector$allDirections = Direction.values();

    @Inject(
            method = "findRecipe",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void crushingwheelrecipeselector$onFindRecipe(CallbackInfoReturnable<Optional<RecipeHolder<? extends AbstractCrushingRecipe>>> cir) {
        try {
            CrushingWheelControllerBlockEntity blockEntity = (CrushingWheelControllerBlockEntity) (Object) this;

            // Only process on server side
            if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }

            BlockPos controllerPos = blockEntity.getBlockPos();
            CrushingWheelSelections selections = CrushingWheelSelections.get(serverLevel);
            if (selections == null) {
                return;
            }

            // ============================================================
            // PERFORMANCE OPTIMIZATION: Early exit for unlinked controllers
            // Single O(1) HashSet lookup - unlinked wheels skip everything
            // ============================================================
            if (!selections.isControllerActive(controllerPos)) {
                return;
            }

            // Check if there's a recipe to potentially override
            Optional<RecipeHolder<? extends AbstractCrushingRecipe>> currentRecipe = cir.getReturnValue();
            if (currentRecipe.isEmpty()) {
                return;
            }

            // Get the recipe and validate it has ingredients
            AbstractCrushingRecipe recipe = currentRecipe.get().value();
            var ingredients = recipe.getIngredients();
            if (ingredients.isEmpty()) {
                return;
            }

            ItemStack[] possibleInputs = ingredients.get(0).getItems();
            if (possibleInputs.length == 0) {
                return;
            }

            // Get the input item ID for preference lookup
            String inputItemId = BuiltInRegistries.ITEM.getKey(possibleInputs[0].getItem()).toString();

            // Reuse list to avoid garbage collection pressure
            List<BlockPos> wheelPositions = crushingwheelrecipeselector$wheelPositions.get();
            wheelPositions.clear();

            // Check all 6 adjacent positions for linked wheels
            // (The controller is between the wheels, so wheels are adjacent to it)
            for (Direction direction : crushingwheelrecipeselector$allDirections) {
                BlockPos adjacentPos = controllerPos.relative(direction);
                if (selections.isWheelLinked(adjacentPos)) {
                    wheelPositions.add(adjacentPos);
                }
            }

            if (wheelPositions.isEmpty()) {
                return;
            }

            // Find a preference from any of the linked wheels
            ResourceLocation preferredRecipeId = null;
            for (int i = 0; i < wheelPositions.size(); i++) {
                ResourceLocation pref = selections.getPreferredRecipe(wheelPositions.get(i), inputItemId);
                if (pref != null) {
                    preferredRecipeId = pref;
                    break;
                }
            }

            // No preference set for this input item
            if (preferredRecipeId == null) {
                return;
            }

            // Check if we're already using the preferred recipe
            ResourceLocation currentRecipeId = currentRecipe.get().id();
            if (currentRecipeId.equals(preferredRecipeId)) {
                return;
            }

            // Need to find and switch to the preferred recipe
            final ResourceLocation finalPreferredRecipeId = preferredRecipeId;

            @SuppressWarnings("unchecked")
            RecipeType<AbstractCrushingRecipe> recipeType = (RecipeType<AbstractCrushingRecipe>)
                    (RecipeType<?>) recipe.getType();

            // Search for the preferred recipe by ID
            Optional<RecipeHolder<AbstractCrushingRecipe>> preferredRecipe = serverLevel.getRecipeManager()
                    .getAllRecipesFor(recipeType)
                    .stream()
                    .filter(holder -> holder.id().equals(finalPreferredRecipeId))
                    .findFirst();

            if (preferredRecipe.isPresent()) {
                @SuppressWarnings("unchecked")
                Optional<RecipeHolder<? extends AbstractCrushingRecipe>> result =
                        (Optional<RecipeHolder<? extends AbstractCrushingRecipe>>) (Optional<?>) preferredRecipe;
                cir.setReturnValue(result);
            }

        } catch (Throwable t) {
            // Only log actual errors - these should be rare
            CrushingWheelRecipeSelector.LOGGER.error("Error in recipe selection mixin", t);
        }
    }
}