package com.enormeboze.crushingwheelrecipeselector.client;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.RecipeHandler;
import com.enormeboze.crushingwheelrecipeselector.network.ClearRecipePacket;
import com.enormeboze.crushingwheelrecipeselector.network.SelectRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailScreen extends Screen {

    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int RECIPE_BG = 0xFFB8B8B8;
    private static final int RECIPE_SELECTED = 0xFFAAEEAA;
    private static final int RECIPE_HOVER = 0xFFD0D0D0;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int TEXT_COLOR = 0xFF404040;

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 200;
    private static final int RECIPE_HEIGHT = 50;
    private static final int RECIPE_SPACING = 4;
    private static final int SLOT_SIZE = 18;

    private final Screen parent;
    private final BlockPos wheelPosition;
    private final String inputItemId;
    private final ItemStack inputItemStack;
    private final List<RecipeHandler.RecipeConflict> recipes;

    private ResourceLocation selectedRecipeId;
    private ResourceLocation pendingSelection; // The recipe user clicked on (not yet confirmed)
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public RecipeDetailScreen(Screen parent, BlockPos wheelPosition, String inputItemId,
                              ItemStack inputItemStack, List<RecipeHandler.RecipeConflict> recipes) {
        super(Component.literal("Select Recipe"));
        this.parent = parent;
        this.wheelPosition = wheelPosition;
        this.inputItemId = inputItemId;
        this.inputItemStack = inputItemStack;
        this.recipes = recipes;
        this.selectedRecipeId = ClientSelectionCache.getSelection(wheelPosition, inputItemId);
        this.pendingSelection = this.selectedRecipeId; // Start with current selection
    }

    @Override
    protected void init() {
        super.init();

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Back button (left)
        this.addRenderableWidget(
                Button.builder(Component.literal("Back"), b -> goBack())
                        .bounds(panelX + 8, panelY + PANEL_HEIGHT - 24, 45, 20)
                        .build()
        );

        // Confirm button (right side, left of Clear)
        this.addRenderableWidget(
                Button.builder(Component.literal("Confirm"), b -> confirmSelection())
                        .bounds(panelX + PANEL_WIDTH - 103, panelY + PANEL_HEIGHT - 24, 50, 20)
                        .build()
        );

        // Clear button (far right)
        this.addRenderableWidget(
                Button.builder(Component.literal("Clear"), b -> clearSelection())
                        .bounds(panelX + PANEL_WIDTH - 50, panelY + PANEL_HEIGHT - 24, 42, 20)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark overlay
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        drawPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Title - no shadow, just clean text like JEI
        String title = "Select Recipe";
        int titleX = panelX + PANEL_WIDTH / 2 - this.font.width(title) / 2;
        graphics.drawString(this.font, title, titleX, panelY + 6, TEXT_COLOR, false);

        // Input item
        graphics.renderItem(inputItemStack, panelX + PANEL_WIDTH / 2 - 8, panelY + 18);

        renderRecipeList(graphics, mouseX, mouseY, panelX, panelY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // Main fill with rounded corners
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_COLOR);
        graphics.fill(x + 3, y + 1, x + width - 3, y + height - 1, PANEL_COLOR);
        graphics.fill(x + 1, y + 3, x + width - 1, y + height - 3, PANEL_COLOR);

        // Top border (light)
        graphics.fill(x + 3, y, x + width - 3, y + 1, BORDER_LIGHT);
        graphics.fill(x + 1, y + 1, x + 3, y + 2, BORDER_LIGHT);
        graphics.fill(x, y + 3, x + 1, y + height - 3, BORDER_LIGHT);
        graphics.fill(x + 1, y + 2, x + 2, y + 3, BORDER_LIGHT);

        // Bottom border (dark)
        graphics.fill(x + 3, y + height - 1, x + width - 3, y + height, BORDER_DARK);
        graphics.fill(x + width - 3, y + height - 2, x + width - 1, y + height - 1, BORDER_DARK);
        graphics.fill(x + width - 1, y + 3, x + width, y + height - 3, BORDER_DARK);
        graphics.fill(x + width - 2, y + height - 3, x + width - 1, y + height - 2, BORDER_DARK);

        // Corner pixels
        graphics.fill(x + 1, y + height - 3, x + 2, y + height - 2, BORDER_DARK);
        graphics.fill(x + 2, y + height - 2, x + 3, y + height - 1, BORDER_DARK);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 2, BORDER_LIGHT);
        graphics.fill(x + width - 3, y + 2, x + width - 2, y + 3, BORDER_LIGHT);
    }

    private void drawRecipeBox(GuiGraphics graphics, int x, int y, int width, int height, boolean selected, boolean hovered, int bgColor) {
        // Main fill with rounded corners
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, bgColor);
        graphics.fill(x + 3, y + 1, x + width - 3, y + height - 1, bgColor);
        graphics.fill(x + 1, y + 3, x + width - 1, y + height - 3, bgColor);

        int borderColor = selected ? 0xFF00AA00 : BORDER_DARK;

        // Top border
        graphics.fill(x + 3, y, x + width - 3, y + 1, borderColor);
        graphics.fill(x + 1, y + 1, x + 3, y + 2, borderColor);
        graphics.fill(x + 1, y + 2, x + 2, y + 3, borderColor);

        // Left border
        graphics.fill(x, y + 3, x + 1, y + height - 3, borderColor);

        // Bottom border
        graphics.fill(x + 3, y + height - 1, x + width - 3, y + height, borderColor);
        graphics.fill(x + width - 3, y + height - 2, x + width - 1, y + height - 1, borderColor);
        graphics.fill(x + width - 2, y + height - 3, x + width - 1, y + height - 2, borderColor);

        // Right border
        graphics.fill(x + width - 1, y + 3, x + width, y + height - 3, borderColor);

        // Corner pixels
        graphics.fill(x + 1, y + height - 3, x + 2, y + height - 2, borderColor);
        graphics.fill(x + 2, y + height - 2, x + 3, y + height - 1, borderColor);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 2, borderColor);
        graphics.fill(x + width - 3, y + 2, x + width - 2, y + 3, borderColor);
    }

    private void drawItemSlot(GuiGraphics graphics, int x, int y) {
        // Rounded slot
        graphics.fill(x + 1, y, x + SLOT_SIZE - 1, y + SLOT_SIZE, SLOT_BG);
        graphics.fill(x, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, SLOT_BG);

        // Inset border
        graphics.fill(x + 1, y, x + SLOT_SIZE - 1, y + 1, BORDER_DARK);
        graphics.fill(x, y + 1, x + 1, y + SLOT_SIZE - 1, BORDER_DARK);
        graphics.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE - 1, y + SLOT_SIZE, BORDER_LIGHT);
        graphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, BORDER_LIGHT);
    }

    private void renderRecipeList(GuiGraphics graphics, int mouseX, int mouseY, int panelX, int panelY) {
        int listStartY = panelY + 40;
        int listHeight = PANEL_HEIGHT - 70;
        int recipeWidth = PANEL_WIDTH - 20;
        int recipeX = panelX + 10;

        int contentHeight = recipes.size() * (RECIPE_HEIGHT + RECIPE_SPACING);
        maxScroll = Math.max(0, contentHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(panelX + 5, listStartY, panelX + PANEL_WIDTH - 5, listStartY + listHeight);

        // Track hovered output for tooltip (render after loop)
        ItemStack hoveredStack = null;
        String hoveredOutput = null;
        int tooltipX = 0, tooltipY = 0;

        for (int i = 0; i < recipes.size(); i++) {
            RecipeHandler.RecipeConflict recipe = recipes.get(i);
            int boxY = listStartY + i * (RECIPE_HEIGHT + RECIPE_SPACING) - scrollOffset;

            if (boxY + RECIPE_HEIGHT < listStartY || boxY > listStartY + listHeight) continue;

            boolean isSelected = recipe.recipeId().equals(selectedRecipeId);
            boolean isPending = recipe.recipeId().equals(pendingSelection) && !isSelected;
            boolean isHovered = mouseX >= recipeX && mouseX < recipeX + recipeWidth &&
                    mouseY >= boxY && mouseY < boxY + RECIPE_HEIGHT &&
                    mouseY >= listStartY && mouseY <= listStartY + listHeight;

            // Use different colors: green for confirmed, yellow tint for pending
            int bgColor = isSelected ? RECIPE_SELECTED : (isPending ? 0xFFEEEEAA : (isHovered ? RECIPE_HOVER : RECIPE_BG));
            drawRecipeBox(graphics, recipeX, boxY, recipeWidth, RECIPE_HEIGHT, isSelected, isHovered, bgColor);

            // Mod name - clean text, no shadow
            String modName = getModName(recipe.recipeId());
            graphics.drawString(this.font, modName, recipeX + 4, boxY + 4, TEXT_COLOR, false);

            // Outputs
            List<String> outputs = recipe.outputs();
            int slotX = recipeX + 6;
            int slotY = boxY + 18;

            for (int j = 1; j < Math.min(outputs.size(), 9); j++) {
                String output = outputs.get(j);
                ItemStack outStack = getItemStackFromOutput(output);
                if (outStack.isEmpty()) continue;

                drawItemSlot(graphics, slotX, slotY);
                graphics.renderItem(outStack, slotX + 1, slotY + 1);

                // Use renderItemDecorations for count - this renders ON TOP of item correctly
                int count = getCountFromOutput(output);
                if (count > 1) {
                    ItemStack fakeStack = outStack.copy();
                    fakeStack.setCount(count);
                    graphics.renderItemDecorations(this.font, fakeStack, slotX + 1, slotY + 1);
                }

                // Check if hovering this slot
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE &&
                        mouseY >= listStartY && mouseY <= listStartY + listHeight) {
                    hoveredStack = outStack;
                    hoveredOutput = output;
                    tooltipX = mouseX;
                    tooltipY = mouseY;
                }

                slotX += SLOT_SIZE + 2;
            }

            if (outputs.size() <= 1) {
                graphics.drawString(this.font, "No secondaries", recipeX + 6, slotY + 4, 0xFF888888, false);
            }

            // Checkmark if this is the confirmed selection
            if (recipe.recipeId().equals(selectedRecipeId)) {
                graphics.drawString(this.font, "\u2714", recipeX + recipeWidth - 12, boxY + 4, 0xFF00DD00, false);
            }
            // Show pending indicator (yellow) if selected but not yet confirmed
            else if (recipe.recipeId().equals(pendingSelection)) {
                graphics.drawString(this.font, "\u25CF", recipeX + recipeWidth - 12, boxY + 4, 0xFFFFAA00, false);
            }
        }

        graphics.disableScissor();

        // Render tooltip for hovered output (after everything else)
        if (hoveredStack != null && hoveredOutput != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(hoveredStack.getHoverName());
            String percentage = getPercentageText(hoveredOutput);
            if (!percentage.isEmpty()) {
                tooltip.add(Component.literal("§7Chance: " + percentage));
            }
            graphics.renderComponentTooltip(this.font, tooltip, tooltipX, tooltipY);
        }

        // Scrollbar
        if (maxScroll > 0) {
            int scrollbarX = panelX + PANEL_WIDTH - 8;
            int scrollbarY = listStartY;
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + listHeight, 0xFF555555);

            int thumbHeight = Math.max(20, (int) ((float) listHeight / (float) contentHeight * listHeight));
            int thumbY = scrollbarY + (int) ((float) scrollOffset / (float) maxScroll * (listHeight - thumbHeight));
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) (scrollY * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelX = (this.width - PANEL_WIDTH) / 2;
            int panelY = (this.height - PANEL_HEIGHT) / 2;
            int listStartY = panelY + 40;
            int listHeight = PANEL_HEIGHT - 70;
            int recipeWidth = PANEL_WIDTH - 20;
            int recipeX = panelX + 10;

            for (int i = 0; i < recipes.size(); i++) {
                int boxY = listStartY + i * (RECIPE_HEIGHT + RECIPE_SPACING) - scrollOffset;

                if (boxY + RECIPE_HEIGHT < listStartY || boxY > listStartY + listHeight) continue;

                if (mouseX >= recipeX && mouseX < recipeX + recipeWidth &&
                        mouseY >= boxY && mouseY < boxY + RECIPE_HEIGHT &&
                        mouseY >= listStartY && mouseY <= listStartY + listHeight) {
                    selectRecipe(recipes.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String getModName(ResourceLocation recipeId) {
        String namespace = recipeId.getNamespace();
        if (namespace.equals("create")) return "Create";
        if (namespace.equals("minecraft")) return "Minecraft";
        String name = namespace.replace("_", " ").replace("-", " ");
        if (name.isEmpty()) return namespace;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private ItemStack getItemStackFromOutput(String output) {
        try {
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

    private int getCountFromOutput(String output) {
        try {
            String[] parts = output.split(" ");
            if (parts.length > 1) {
                String part = parts[1];
                if (part.startsWith("x") && !part.contains("(")) {
                    return Integer.parseInt(part.substring(1));
                }
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    private String getPercentageText(String output) {
        try {
            if (output.contains("(") && output.contains("%)")) {
                int start = output.indexOf("(");
                int end = output.indexOf("%)", start);
                return output.substring(start + 1, end + 1);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void selectRecipe(RecipeHandler.RecipeConflict recipe) {
        // Only set as pending - don't send to server until confirmed
        this.pendingSelection = recipe.recipeId();
        Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.8F);
    }

    private void confirmSelection() {
        if (pendingSelection == null) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§cNo recipe selected"), true);
            }
            return;
        }

        // Save the selection
        this.selectedRecipeId = pendingSelection;

        // Send to server
        PacketDistributor.sendToServer(new SelectRecipePacket(wheelPosition, inputItemId, pendingSelection));

        // Update client cache immediately so it reflects the change
        ClientSelectionCache.updateSelection(wheelPosition, inputItemId, pendingSelection);

        Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.2F);

        if (Minecraft.getInstance().player != null) {
            // Find the recipe name for the message
            String recipeName = pendingSelection.toString();
            for (RecipeHandler.RecipeConflict recipe : recipes) {
                if (recipe.recipeId().equals(pendingSelection)) {
                    recipeName = recipe.primaryOutput();
                    break;
                }
            }
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§aConfirmed: §f" + recipeName), true);
        }

        // Don't close GUI - user can continue selecting other recipes
    }

    private void clearSelection() {
        this.selectedRecipeId = null;
        this.pendingSelection = null;

        // Send to server
        PacketDistributor.sendToServer(new ClearRecipePacket(wheelPosition, inputItemId));

        // Update client cache immediately
        ClientSelectionCache.clearSelection(wheelPosition, inputItemId);

        Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.8F);

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§7Selection cleared"), true);
        }

        // Don't close GUI
    }

    /**
     * Go back to selector screen (Back button)
     */
    private void goBack() {
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Called when pressing Escape - completely exit GUI
     */
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Empty - we draw our own background
    }
}