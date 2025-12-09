package com.enormeboze.crushingwheelrecipeselector.mixin;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Optional;

/**
 * Mixin to intercept recipe lookups and apply user's preferred recipe selection.
 *
 * PERFORMANCE OPTIMIZATIONS:
 * - Early exit for unlinked wheels via O(1) HashSet lookup
 * - Cached direction array
 * - No debug logging in hot path
 */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public class CrushingWheelControllerMixin {

    @Unique
    private static final Direction[] crushingWheelRecipeSelector$directions = Direction.values();

    @Inject(method = "findRecipe", at = @At("RETURN"), cancellable = true)
    private void crushingWheelRecipeSelector$onFindRecipe(CallbackInfoReturnable<Optional<ProcessingRecipe<?>>> cir) {
        CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity) (Object) this;
        Level level = self.getLevel();

        if (level == null || level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos controllerPos = self.getBlockPos();

        // PERFORMANCE: Quick check if this controller has linked wheels
        CrushingWheelSelections selections = CrushingWheelSelections.get(serverLevel);
        if (selections == null || !selections.isControllerActive(controllerPos)) {
            return;
        }

        // Find adjacent crushing wheel
        BlockPos wheelPos = null;
        for (Direction dir : crushingWheelRecipeSelector$directions) {
            BlockPos adjacent = controllerPos.relative(dir);
            if (selections.isWheelLinked(adjacent)) {
                wheelPos = adjacent;
                break;
            }
        }

        if (wheelPos == null) {
            return;
        }

        // Get input item from the processing inventory
        var inventory = self.inventory;
        if (inventory == null || inventory.getStackInSlot(0).isEmpty()) {
            return;
        }

        String inputItemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(inventory.getStackInSlot(0).getItem()).toString();

        // Check for user preference
        ResourceLocation preferredRecipeId = selections.getPreferredRecipe(wheelPos, inputItemId);
        if (preferredRecipeId == null) {
            return;
        }

        // Get all recipes and find the preferred one
        // In 1.20.1, we iterate through all recipes directly
        Collection<Recipe<?>> allRecipes = level.getRecipeManager().getRecipes();

        for (Recipe<?> recipe : allRecipes) {
            // Check if this is our preferred recipe
            if (recipe.getId().equals(preferredRecipeId)) {
                // Verify it's a crushing recipe
                if (recipe instanceof CrushingRecipe crushingRecipe) {
                    // The user selected this recipe for this input, so we trust it matches
                    cir.setReturnValue(Optional.of(crushingRecipe));
                    return;
                }
            }
        }
    }
}