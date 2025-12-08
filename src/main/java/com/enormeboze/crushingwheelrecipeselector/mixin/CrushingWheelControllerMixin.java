package com.enormeboze.crushingwheelrecipeselector.mixin;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class CrushingWheelControllerMixin {

    /**
     * Target findRecipe WITHOUT parameters - let mixin figure it out
     */
    @Inject(
            method = "findRecipe",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void onFindRecipe(CallbackInfoReturnable<Optional<RecipeHolder<? extends AbstractCrushingRecipe>>> cir) {
        try {
            CrushingWheelRecipeSelector.LOGGER.info("🔍 MIXIN CALLED!");

            CrushingWheelControllerBlockEntity blockEntity = (CrushingWheelControllerBlockEntity) (Object) this;

            if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
                CrushingWheelRecipeSelector.LOGGER.info("❌ Client side");
                return;
            }

            CrushingWheelRecipeSelector.LOGGER.info("✅ Server side!");

            Optional<RecipeHolder<? extends AbstractCrushingRecipe>> currentRecipe = cir.getReturnValue();
            if (currentRecipe.isEmpty()) {
                CrushingWheelRecipeSelector.LOGGER.info("❌ No recipe found");
                return;
            }

            CrushingWheelRecipeSelector.LOGGER.info("✅ Current recipe: {}", currentRecipe.get().id());

            BlockPos pos = blockEntity.getBlockPos();

            // Get item from recipe ingredients
            AbstractCrushingRecipe recipe = (AbstractCrushingRecipe) currentRecipe.get().value();
            if (recipe.getIngredients().isEmpty()) {
                CrushingWheelRecipeSelector.LOGGER.info("❌ No ingredients");
                return;
            }

            ItemStack[] possibleInputs = recipe.getIngredients().get(0).getItems();
            if (possibleInputs.length == 0) {
                CrushingWheelRecipeSelector.LOGGER.info("❌ No possible inputs");
                return;
            }

            String inputItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(possibleInputs[0].getItem()).toString();

            CrushingWheelRecipeSelector.LOGGER.info("📍 Controller Position: {}, Item: {}", pos, inputItemId);

            CrushingWheelSelections selections = CrushingWheelSelections.get(serverLevel);

            // CRITICAL FIX: The controller block is BETWEEN the wheels
            // We need to check the adjacent wheel positions (not the controller itself)
            // because that's where the player configured the recipes

            List<BlockPos> wheelPositions = new java.util.ArrayList<>();

            // Check all 4 horizontal directions for actual wheel blocks
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos adjacentPos = pos.relative(direction);

                // Check if this position has a linked group (is configured)
                if (selections.isWheelLinked(adjacentPos)) {
                    wheelPositions.add(adjacentPos);
                    CrushingWheelRecipeSelector.LOGGER.info("  Found linked wheel at: {}", adjacentPos);
                }
            }

            CrushingWheelRecipeSelector.LOGGER.info("🔗 Found {} linked wheel(s)", wheelPositions.size());

            // Try to find a preference from any of the linked wheels
            ResourceLocation preferredRecipeId = null;
            BlockPos foundAtPos = null;

            for (BlockPos wheelPos : wheelPositions) {
                ResourceLocation pref = selections.getPreferredRecipe(wheelPos, inputItemId);
                if (pref != null) {
                    preferredRecipeId = pref;
                    foundAtPos = wheelPos;
                    CrushingWheelRecipeSelector.LOGGER.info("✅ Found preference at {}: {}", wheelPos, pref);
                    break; // Found it!
                }
            }

            CrushingWheelRecipeSelector.LOGGER.info("🔍 Final Preferred: {} (from {})", preferredRecipeId, foundAtPos);

            if (preferredRecipeId == null) {
                CrushingWheelRecipeSelector.LOGGER.info("❌ No preference");
                return;
            }

            // Make final for lambda usage
            final ResourceLocation finalPreferredRecipeId = preferredRecipeId;

            ResourceLocation currentRecipeId = currentRecipe.get().id();
            if (currentRecipeId.equals(finalPreferredRecipeId)) {
                CrushingWheelRecipeSelector.LOGGER.info("✅ Already correct!");
                return;
            }

            CrushingWheelRecipeSelector.LOGGER.info("🔄 Need to switch from {} to {}", currentRecipeId, finalPreferredRecipeId);

            // Get ALL crushing recipes
            @SuppressWarnings("unchecked")
            List<RecipeHolder<AbstractCrushingRecipe>> allRecipes = (List<RecipeHolder<AbstractCrushingRecipe>>) (List<?>)
                    serverLevel.getRecipeManager()
                            .getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType<AbstractCrushingRecipe>)
                                    (net.minecraft.world.item.crafting.RecipeType<?>) currentRecipe.get().value().getType())
                            .stream()
                            .filter(holder -> {
                                var ingredients = ((AbstractCrushingRecipe) holder.value()).getIngredients();
                                if (ingredients.isEmpty()) return false;
                                return ingredients.get(0).test(possibleInputs[0]);
                            })
                            .toList();

            CrushingWheelRecipeSelector.LOGGER.info("🔍 Found {} total recipes", allRecipes.size());

            // Log all recipes
            for (RecipeHolder<AbstractCrushingRecipe> r : allRecipes) {
                CrushingWheelRecipeSelector.LOGGER.info("  - {}", r.id());
            }

            Optional<RecipeHolder<AbstractCrushingRecipe>> preferredRecipe = allRecipes.stream()
                    .filter(holder -> holder.id().equals(finalPreferredRecipeId))
                    .findFirst();

            if (preferredRecipe.isPresent()) {
                @SuppressWarnings("unchecked")
                Optional<RecipeHolder<? extends AbstractCrushingRecipe>> result =
                        (Optional<RecipeHolder<? extends AbstractCrushingRecipe>>) (Optional<?>) preferredRecipe;

                cir.setReturnValue(result);

                CrushingWheelRecipeSelector.LOGGER.info("🎉🎉🎉 RECIPE OVERRIDDEN! Set to: {}", finalPreferredRecipeId);
                CrushingWheelRecipeSelector.LOGGER.info("🎉 Return value is now: {}", cir.getReturnValue().get().id());
            } else {
                CrushingWheelRecipeSelector.LOGGER.warn("⚠️ Preferred recipe {} NOT FOUND in recipe list!", finalPreferredRecipeId);
            }

        } catch (Throwable t) {
            CrushingWheelRecipeSelector.LOGGER.error("❌ Mixin error!", t);
        }
    }
}