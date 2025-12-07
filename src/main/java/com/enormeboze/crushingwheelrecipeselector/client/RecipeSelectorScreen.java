package com.enormeboze.crushingwheelrecipeselector.client;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.RecipeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main screen showing all items with conflicting crushing recipes
 * Player clicks an item to configure its recipes
 */
public class RecipeSelectorScreen extends Screen {

    private static final int ITEM_SLOT_SIZE = 24;
    private static final int SLOT_SPACING = 28;
    private static final int SLOTS_PER_ROW = 7;

    private final BlockPos wheelPosition;
    private List<ItemEntry> conflictingItems;
    private int scrollOffset = 0;

    public RecipeSelectorScreen(BlockPos wheelPosition) {
        super(Component.literal("Crushing Recipe Selector"));
        this.wheelPosition = wheelPosition;
        this.conflictingItems = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();

        // Load conflicting items
        loadConflictingItems();

        // Add close button
        int buttonWidth = 100;
        int buttonHeight = 20;
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Close"),
                                button -> this.onClose()
                        )
                        .bounds(this.width / 2 - buttonWidth / 2, this.height - 30, buttonWidth, buttonHeight)
                        .build()
        );

        CrushingWheelRecipeSelector.LOGGER.info("Opened recipe selector for wheel at {}", wheelPosition);
        CrushingWheelRecipeSelector.LOGGER.info("Found {} items with conflicts", conflictingItems.size());
    }

    private void loadConflictingItems() {
        conflictingItems.clear();

        Map<String, List<RecipeHandler.RecipeConflict>> conflicts = RecipeHandler.getConflictingRecipes();

        for (Map.Entry<String, List<RecipeHandler.RecipeConflict>> entry : conflicts.entrySet()) {
            String itemId = entry.getKey();
            int conflictCount = entry.getValue().size();

            // Try to get the actual item
            ItemStack itemStack = getItemStackFromId(itemId);

            conflictingItems.add(new ItemEntry(itemId, itemStack, conflictCount));
        }

        CrushingWheelRecipeSelector.LOGGER.info("Loaded {} conflicting items", conflictingItems.size());
    }

    private ItemStack getItemStackFromId(String itemId) {
        try {
            ResourceLocation location = ResourceLocation.parse(itemId);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(location);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            CrushingWheelRecipeSelector.LOGGER.error("Failed to get item for ID: {}", itemId, e);
        }
        // Fallback to barrier (indicates missing item)
        return new ItemStack(Items.BARRIER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render transparent darkened background (no blur on text)
        this.renderTransparentBackground(graphics);

        // Draw a solid panel for the GUI content
        int panelWidth = Math.min(this.width - 40, 400);
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
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 10, 0xFFFFFF);

        // Draw wheel position
        String posText = "Wheel: [" + wheelPosition.getX() + ", " + wheelPosition.getY() + ", " + wheelPosition.getZ() + "]";
        graphics.drawCenteredString(this.font, posText, this.width / 2, panelY + 25, 0xAAAAAA);

        // Draw instruction text
        if (conflictingItems.isEmpty()) {
            String noConflicts = "No conflicting recipes found!";
            graphics.drawCenteredString(this.font, noConflicts, this.width / 2, this.height / 2, 0xFFFF00);
        } else {
            String instruction = "Click an item to configure its crushing recipe:";
            graphics.drawCenteredString(this.font, instruction, this.width / 2, panelY + 45, 0xCCCCCC);

            // Draw items in a grid
            renderItemGrid(graphics, mouseX, mouseY);
        }

        // Render buttons and other widgets
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderItemGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int startX = this.width / 2 - (SLOTS_PER_ROW * SLOT_SPACING) / 2;
        int startY = 75;

        int index = 0;
        for (ItemEntry entry : conflictingItems) {
            int row = index / SLOTS_PER_ROW;
            int col = index % SLOTS_PER_ROW;

            int x = startX + col * SLOT_SPACING;
            int y = startY + row * SLOT_SPACING;

            // Check if we're out of bounds
            if (y > this.height - 60) {
                break; // Don't render items that would go off screen
            }

            // Draw slot background
            boolean isHovered = mouseX >= x && mouseX < x + ITEM_SLOT_SIZE &&
                    mouseY >= y && mouseY < y + ITEM_SLOT_SIZE;

            int backgroundColor = isHovered ? 0x80FFFFFF : 0x80000000;
            graphics.fill(x, y, x + ITEM_SLOT_SIZE, y + ITEM_SLOT_SIZE, backgroundColor);

            // Draw item
            graphics.renderItem(entry.itemStack, x + 4, y + 4);

            // Draw conflict count badge
            String countText = String.valueOf(entry.conflictCount);
            int textWidth = this.font.width(countText);
            graphics.drawString(this.font, countText, x + ITEM_SLOT_SIZE - textWidth - 2, y + 2, 0xFFFF00);

            // Draw tooltip on hover
            if (isHovered) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(entry.itemStack.getHoverName());
                tooltip.add(Component.literal("§7" + entry.conflictCount + " recipes available"));
                tooltip.add(Component.literal("§eClick to configure"));

                // Convert Components to FormattedCharSequence for rendering
                graphics.renderTooltip(this.font,
                        tooltip.stream()
                                .map(c -> c.getVisualOrderText())
                                .collect(java.util.stream.Collectors.toList()),
                        mouseX, mouseY);
            }

            index++;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check if clicked on an item
            int clickedIndex = getClickedItemIndex((int) mouseX, (int) mouseY);
            if (clickedIndex >= 0 && clickedIndex < conflictingItems.size()) {
                ItemEntry entry = conflictingItems.get(clickedIndex);
                openRecipeDetailScreen(entry);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getClickedItemIndex(int mouseX, int mouseY) {
        int startX = this.width / 2 - (SLOTS_PER_ROW * SLOT_SPACING) / 2;
        int startY = 75;

        for (int i = 0; i < conflictingItems.size(); i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            int x = startX + col * SLOT_SPACING;
            int y = startY + row * SLOT_SPACING;

            if (y > this.height - 60) {
                break;
            }

            if (mouseX >= x && mouseX < x + ITEM_SLOT_SIZE &&
                    mouseY >= y && mouseY < y + ITEM_SLOT_SIZE) {
                return i;
            }
        }

        return -1;
    }

    private void openRecipeDetailScreen(ItemEntry entry) {
        CrushingWheelRecipeSelector.LOGGER.info("Opening recipe detail for: {}", entry.itemId);

        // Get the recipes for this item
        List<RecipeHandler.RecipeConflict> recipes = RecipeHandler.getConflictsForItem(entry.itemId);

        // Open the detail screen
        Minecraft.getInstance().setScreen(
                new RecipeDetailScreen(this, wheelPosition, entry.itemId, entry.itemStack, recipes)
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    /**
     * Represents an item with conflicting recipes
     */
    private static class ItemEntry {
        final String itemId;
        final ItemStack itemStack;
        final int conflictCount;

        ItemEntry(String itemId, ItemStack itemStack, int conflictCount) {
            this.itemId = itemId;
            this.itemStack = itemStack;
            this.conflictCount = conflictCount;
        }
    }
}