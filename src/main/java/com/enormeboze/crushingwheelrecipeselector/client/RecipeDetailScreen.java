package com.enormeboze.crushingwheelrecipeselector.client;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import com.enormeboze.crushingwheelrecipeselector.RecipeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for selecting which recipe to use for a specific item
 */
public class RecipeDetailScreen extends Screen {

    private static final int RECIPE_BOX_WIDTH = 240;
    private static final int RECIPE_BOX_HEIGHT = 50;
    private static final int RECIPE_SPACING = 8;

    private final Screen parent;
    private final BlockPos wheelPosition;
    private final String inputItemId;
    private final ItemStack inputItemStack;
    private final List<RecipeHandler.RecipeConflict> recipes;

    private ResourceLocation selectedRecipeId;
    private int scrollOffset = 0;

    public RecipeDetailScreen(Screen parent, BlockPos wheelPosition, String inputItemId,
                              ItemStack inputItemStack, List<RecipeHandler.RecipeConflict> recipes) {
        super(Component.literal("Select Recipe"));
        this.parent = parent;
        this.wheelPosition = wheelPosition;
        this.inputItemId = inputItemId;
        this.inputItemStack = inputItemStack;
        this.recipes = recipes;

        // Load current selection (we'll implement this properly with networking later)
        // For now, just leave it null
        this.selectedRecipeId = null;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int centerX = this.width / 2;

        // Back button
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("← Back"),
                                button -> this.onClose()
                        )
                        .bounds(centerX - buttonWidth - 5, this.height - 30, buttonWidth, buttonHeight)
                        .build()
        );

        // Clear selection button
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Clear Selection"),
                                button -> this.clearSelection()
                        )
                        .bounds(centerX + 5, this.height - 30, buttonWidth, buttonHeight)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render transparent darkened background (no blur on text)
        this.renderTransparentBackground(graphics);

        // Draw a solid panel for the GUI content
        int panelWidth = Math.min(this.width - 40, 300);
        int panelHeight = this.height - 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;

        // Draw panel background (solid dark gray)
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0101010);

        // Draw panel border
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF8B8B8B); // Top
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF8B8B8B); // Bottom
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF8B8B8B); // Left
        graphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF8B8B8B); // Right

        // Draw title
        graphics.drawCenteredString(this.font, "Select Recipe", this.width / 2, panelY + 10, 0xFFFFFF);

        // Draw input item
        graphics.drawString(this.font, "Configure recipes for:", this.width / 2 - 80, panelY + 30, 0xCCCCCC);
        graphics.renderItem(inputItemStack, this.width / 2 + 20, panelY + 27);
        graphics.drawString(this.font, inputItemStack.getHoverName().getString(),
                this.width / 2 + 42, panelY + 32, 0xFFFFFF);

        // Draw recipes
        renderRecipeList(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRecipeList(GuiGraphics graphics, int mouseX, int mouseY) {
        int startY = 70;
        int centerX = this.width / 2;

        for (int i = 0; i < recipes.size(); i++) {
            RecipeHandler.RecipeConflict recipe = recipes.get(i);
            int boxY = startY + i * (RECIPE_BOX_HEIGHT + RECIPE_SPACING);

            // Check if out of bounds
            if (boxY > this.height - 70) {
                break;
            }

            int boxX = centerX - RECIPE_BOX_WIDTH / 2;

            // Check if this recipe is selected
            boolean isSelected = recipe.recipeId().equals(selectedRecipeId);

            // Check if mouse is hovering
            boolean isHovered = mouseX >= boxX && mouseX < boxX + RECIPE_BOX_WIDTH &&
                    mouseY >= boxY && mouseY < boxY + RECIPE_BOX_HEIGHT;

            // Draw box background
            int backgroundColor = isSelected ? 0x8000FF00 : (isHovered ? 0x80666666 : 0x80333333);
            graphics.fill(boxX, boxY, boxX + RECIPE_BOX_WIDTH, boxY + RECIPE_BOX_HEIGHT, backgroundColor);

            // Draw border
            int borderColor = isSelected ? 0xFF00FF00 : 0xFF888888;
            graphics.fill(boxX, boxY, boxX + RECIPE_BOX_WIDTH, boxY + 1, borderColor); // Top
            graphics.fill(boxX, boxY + RECIPE_BOX_HEIGHT - 1, boxX + RECIPE_BOX_WIDTH, boxY + RECIPE_BOX_HEIGHT, borderColor); // Bottom
            graphics.fill(boxX, boxY, boxX + 1, boxY + RECIPE_BOX_HEIGHT, borderColor); // Left
            graphics.fill(boxX + RECIPE_BOX_WIDTH - 1, boxY, boxX + RECIPE_BOX_WIDTH, boxY + RECIPE_BOX_HEIGHT, borderColor); // Right

            // Draw recipe info
            String modName = getModName(recipe.recipeId());
            graphics.drawString(this.font, "§7From: §f" + modName, boxX + 5, boxY + 5, 0xFFFFFF);

            // Draw output items
            List<String> outputs = recipe.outputs();
            int itemX = boxX + 5;
            int itemY = boxY + 20;

            for (int j = 0; j < Math.min(outputs.size(), 5); j++) {
                String output = outputs.get(j);
                ItemStack outputStack = getItemStackFromOutput(output);

                if (!outputStack.isEmpty()) {
                    graphics.renderItem(outputStack, itemX, itemY);

                    // Draw count/chance
                    String countText = getOutputText(output);
                    graphics.drawString(this.font, countText, itemX + 20, itemY + 4, 0xFFFFFF);

                    itemX += 45;
                }
            }

            // Draw checkmark if selected
            if (isSelected) {
                graphics.drawString(this.font, "§a§l✓", boxX + RECIPE_BOX_WIDTH - 20, boxY + 5, 0x00FF00);
            }

            // Draw tooltip on hover
            if (isHovered && !isSelected) {
                graphics.renderTooltip(this.font,
                        List.of(Component.literal("§eClick to select this recipe").getVisualOrderText()),
                        mouseX, mouseY);
            }
        }
    }

    private String getModName(ResourceLocation recipeId) {
        String namespace = recipeId.getNamespace();
        // Capitalize and format nicely
        if (namespace.equals("create")) return "Create";
        if (namespace.equals("minecraft")) return "Minecraft";
        // Format mod IDs nicely (e.g., "create_garnished" -> "Create Garnished")
        return namespace.replace("_", " ")
                .replace("-", " ")
                .substring(0, 1).toUpperCase() + namespace.substring(1).replace("_", " ");
    }

    private ItemStack getItemStackFromOutput(String output) {
        try {
            // Parse format: "minecraft:sand x1" or "create:iron_nugget x3 (75%)"
            String itemId = output.split(" ")[0];
            ResourceLocation location = ResourceLocation.parse(itemId);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(location);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            CrushingWheelRecipeSelector.LOGGER.error("Failed to parse output: {}", output, e);
        }
        return ItemStack.EMPTY;
    }

    private String getOutputText(String output) {
        try {
            // Extract count and chance from format
            String[] parts = output.split(" ", 2);
            if (parts.length > 1) {
                return parts[1]; // "x1" or "x3 (75%)"
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int clickedRecipe = getClickedRecipeIndex((int) mouseX, (int) mouseY);
            if (clickedRecipe >= 0 && clickedRecipe < recipes.size()) {
                selectRecipe(recipes.get(clickedRecipe));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getClickedRecipeIndex(int mouseX, int mouseY) {
        int startY = 70;
        int centerX = this.width / 2;
        int boxX = centerX - RECIPE_BOX_WIDTH / 2;

        for (int i = 0; i < recipes.size(); i++) {
            int boxY = startY + i * (RECIPE_BOX_HEIGHT + RECIPE_SPACING);

            if (boxY > this.height - 70) {
                break;
            }

            if (mouseX >= boxX && mouseX < boxX + RECIPE_BOX_WIDTH &&
                    mouseY >= boxY && mouseY < boxY + RECIPE_BOX_HEIGHT) {
                return i;
            }
        }

        return -1;
    }

    private void selectRecipe(RecipeHandler.RecipeConflict recipe) {
        this.selectedRecipeId = recipe.recipeId();

        CrushingWheelRecipeSelector.LOGGER.info("Selected recipe {} for item {} at wheel {}",
                recipe.recipeId(), inputItemId, wheelPosition);

        // TODO: Send packet to server to save selection
        // For now, we'll just log it
        // When we add networking in Phase 2.5, this will actually save the selection

        // Play click sound
        Minecraft.getInstance().player.playSound(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                1.0F, 1.0F
        );

        // Show feedback message
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§aSelected: §f" + recipe.primaryOutput()),
                    true // Action bar
            );
        }
    }

    private void clearSelection() {
        this.selectedRecipeId = null;

        CrushingWheelRecipeSelector.LOGGER.info("Cleared selection for item {} at wheel {}",
                inputItemId, wheelPosition);

        // TODO: Send packet to server to clear selection

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§7Selection cleared"),
                    true
            );
        }
    }

    @Override
    public void onClose() {
        // Go back to parent screen
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}