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

import java.util.Optional;

@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class CrushingWheelControllerMixin {

    @Inject(
            method = "findRecipe",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void onAnyMethod(CallbackInfoReturnable<Optional<RecipeHolder<? extends AbstractCrushingRecipe>>> cir) {
        try {
            // 1. Validation and Context Setup
            if (!(cir.getReturnValue() instanceof Optional<?> optional) || optional.isEmpty()) return;
            if (!(optional.get() instanceof RecipeHolder<?> holder) || !(holder.value() instanceof AbstractCrushingRecipe)) return;

            CrushingWheelControllerBlockEntity blockEntity = (CrushingWheelControllerBlockEntity) (Object) this;
            if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) return;
            BlockPos pos = blockEntity.getBlockPos();

            // 2. Identify the Input Item from the currently found recipe
            Optional<RecipeHolder<? extends AbstractCrushingRecipe>> currentRecipe = cir.getReturnValue();
            AbstractCrushingRecipe recipe = (AbstractCrushingRecipe) currentRecipe.get().value();

            if (recipe.getIngredients().isEmpty()) return;
            ItemStack[] possibleInputs = recipe.getIngredients().get(0).getItems();
            if (possibleInputs == null || possibleInputs.length == 0) return;

            String inputItemId;
            try {
                // We use the item ID of the first possible ingredient as the key
                inputItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(possibleInputs[0].getItem()).toString();
            } catch (Exception e) {
                return;
            }

            // 3. Lookup Preference
            CrushingWheelSelections selections = CrushingWheelSelections.get(serverLevel);
            ResourceLocation preferredRecipeId = selections.getPreferredRecipe(pos, inputItemId);

            CrushingWheelRecipeSelector.LOGGER.info("MIXIN: Processing crush at {} for input {}. Found default recipe {}. Preferred ID in map: {}",
                    pos, inputItemId, currentRecipe.get().id(), preferredRecipeId); // LOG 1: Check if preferred ID is found

            if (preferredRecipeId == null) {
                return;
            }

            // 4. Apply the Preference
            ResourceLocation currentRecipeId = currentRecipe.get().id();

            if (currentRecipeId.equals(preferredRecipeId)) {
                return;
            }

            Optional<? extends RecipeHolder<? extends AbstractCrushingRecipe>> preferredHolderOpt = serverLevel.getRecipeManager().byKey(preferredRecipeId)
                    .filter(h -> h.value() instanceof AbstractCrushingRecipe)
                    .map(h -> (RecipeHolder<? extends AbstractCrushingRecipe>) h);

            if (preferredHolderOpt.isPresent()) {
                // FORCE the return value to be our preferred recipe
                Optional<RecipeHolder<? extends AbstractCrushingRecipe>> result = (Optional) preferredHolderOpt;
                cir.setReturnValue(result);

                CrushingWheelRecipeSelector.LOGGER.info("MIXIN: Applying preferred recipe {} for input {}", preferredRecipeId, inputItemId); // LOG 2: Recipe applied
            } else {
                CrushingWheelRecipeSelector.LOGGER.warn("MIXIN: Preferred recipe {} not found in Recipe Manager.", preferredRecipeId); // LOG 3: Recipe ID not found
            }

        } catch (Throwable t) {
            CrushingWheelRecipeSelector.LOGGER.error("Mixin error in CrushingWheelControllerMixin", t);
        }
    }
}