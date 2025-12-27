package com.enormeboze.crushingwheelrecipeselector.client;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI screen for selecting preferred crushing wheel recipes.
 * Uses a grid layout to display items with conflicting recipes.
 */
@OnlyIn(Dist.CLIENT)
public class RecipeSelectorScreen extends Screen {

    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int HOVER_OVERLAY = 0x80FFFFFF;
    private static final int SELECTED_TINT = 0x4000FF00;

    private static final int ITEM_SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 20;
    private static final int SLOTS_PER_ROW = 7;

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;

    private final BlockPos wheelPosition;
    private List<ItemEntry> conflictingItems;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public RecipeSelectorScreen(BlockPos wheelPosition) {
        super(Component.literal("Crushing Recipes"));
        this.wheelPosition = wheelPosition;
        this.conflictingItems = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        loadConflictingItems();

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), button -> this.onClose())
                        .bounds(panelX + PANEL_WIDTH / 2 - 30, panelY + PANEL_HEIGHT - 24, 60, 20)
                        .build()
        );
    }

    private void loadConflictingItems() {
        conflictingItems.clear();
        Map<String, List<RecipeHandler.RecipeConflict>> conflicts = RecipeHandler.getConflictingRecipes();

        for (Map.Entry<String, List<RecipeHandler.RecipeConflict>> entry : conflicts.entrySet()) {
            String itemId = entry.getKey();
            int conflictCount = entry.getValue().size();
            ItemStack itemStack = getItemStackFromId(itemId);
            boolean hasSelection = ClientSelectionCache.hasSelection(wheelPosition, itemId);
            conflictingItems.add(new ItemEntry(itemId, itemStack, conflictCount, hasSelection));
        }
    }

    private ItemStack getItemStackFromId(String itemId) {
        try {
            ResourceLocation location = new ResourceLocation(itemId);
            var item = ForgeRegistries.ITEMS.getValue(location);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            CrushingWheelRecipeSelector.LOGGER.error("Failed to get item for ID: {}", itemId, e);
        }
        return new ItemStack(Items.BARRIER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark overlay
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        drawPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Title
        String title = "Crushing Recipes";
        int titleX = panelX + PANEL_WIDTH / 2 - this.font.width(title) / 2;
        int titleY = panelY + 6;
        graphics.drawString(this.font, title, titleX, titleY, TEXT_COLOR, false);

        if (conflictingItems.isEmpty()) {
            String noConflicts = "No conflicts found";
            graphics.drawString(this.font, noConflicts, panelX + PANEL_WIDTH / 2 - this.font.width(noConflicts) / 2,
                    panelY + PANEL_HEIGHT / 2, 0xFF666666, false);
        } else {
            renderItemGrid(graphics, mouseX, mouseY, panelX, panelY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        // Tooltips rendered last (on top of everything)
        renderTooltips(graphics, mouseX, mouseY, panelX, panelY);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // Main fill
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

    private void drawSlot(GuiGraphics graphics, int x, int y, int size, boolean hovered, boolean selected) {
        graphics.fill(x, y, x + size, y + size, SLOT_BG);
        // Inset border
        graphics.fill(x, y, x + size - 1, y + 1, BORDER_DARK);
        graphics.fill(x, y, x + 1, y + size - 1, BORDER_DARK);
        graphics.fill(x + 1, y + size - 1, x + size, y + size, BORDER_LIGHT);
        graphics.fill(x + size - 1, y + 1, x + size, y + size, BORDER_LIGHT);

        if (hovered) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, HOVER_OVERLAY);
        }
        if (selected) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, SELECTED_TINT);
        }
    }

    private void renderItemGrid(GuiGraphics graphics, int mouseX, int mouseY, int panelX, int panelY) {
        int gridStartY = panelY + 20;
        int gridHeight = PANEL_HEIGHT - 50;

        int totalGridWidth = SLOTS_PER_ROW * SLOT_SPACING;
        int startX = panelX + (PANEL_WIDTH - totalGridWidth) / 2;

        int rows = (conflictingItems.size() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
        int contentHeight = rows * SLOT_SPACING;
        maxScroll = Math.max(0, contentHeight - gridHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(panelX + 5, gridStartY, panelX + PANEL_WIDTH - 5, gridStartY + gridHeight);

        int index = 0;
        for (ItemEntry entry : conflictingItems) {
            int row = index / SLOTS_PER_ROW;
            int col = index % SLOTS_PER_ROW;

            int x = startX + col * SLOT_SPACING;
            int y = gridStartY + row * SLOT_SPACING - scrollOffset;

            if (y + ITEM_SLOT_SIZE < gridStartY || y > gridStartY + gridHeight) {
                index++;
                continue;
            }

            boolean isHovered = mouseX >= x && mouseX < x + ITEM_SLOT_SIZE &&
                    mouseY >= y && mouseY < y + ITEM_SLOT_SIZE &&
                    mouseY >= gridStartY && mouseY <= gridStartY + gridHeight;

            drawSlot(graphics, x, y, ITEM_SLOT_SIZE, isHovered, entry.hasSelection);

            // Render item
            graphics.renderItem(entry.itemStack, x + 1, y + 1);

            // Render count using renderItemDecorations which handles z-ordering correctly
            if (entry.conflictCount > 1) {
                ItemStack fakeStack = entry.itemStack.copy();
                fakeStack.setCount(entry.conflictCount);
                graphics.renderItemDecorations(this.font, fakeStack, x + 1, y + 1);
            }

            index++;
        }

        graphics.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int scrollbarX = panelX + PANEL_WIDTH - 8;
            int scrollbarY = gridStartY;
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + gridHeight, 0xFF555555);

            int thumbHeight = Math.max(20, (int) ((float) gridHeight / (float) contentHeight * gridHeight));
            int thumbY = scrollbarY + (int) ((float) scrollOffset / (float) maxScroll * (gridHeight - thumbHeight));
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY, int panelX, int panelY) {
        int gridStartY = panelY + 20;
        int gridHeight = PANEL_HEIGHT - 50;
        int totalGridWidth = SLOTS_PER_ROW * SLOT_SPACING;
        int startX = panelX + (PANEL_WIDTH - totalGridWidth) / 2;

        int index = 0;
        for (ItemEntry entry : conflictingItems) {
            int row = index / SLOTS_PER_ROW;
            int col = index % SLOTS_PER_ROW;

            int x = startX + col * SLOT_SPACING;
            int y = gridStartY + row * SLOT_SPACING - scrollOffset;

            if (y + ITEM_SLOT_SIZE < gridStartY || y > gridStartY + gridHeight) {
                index++;
                continue;
            }

            boolean isHovered = mouseX >= x && mouseX < x + ITEM_SLOT_SIZE &&
                    mouseY >= y && mouseY < y + ITEM_SLOT_SIZE &&
                    mouseY >= gridStartY && mouseY <= gridStartY + gridHeight;

            if (isHovered) {
                graphics.renderTooltip(this.font, entry.itemStack, mouseX, mouseY);
                break;
            }

            index++;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        scrollOffset -= (int) (scrollY * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int gridStartY = panelY + 20;
        int gridHeight = PANEL_HEIGHT - 50;

        int totalGridWidth = SLOTS_PER_ROW * SLOT_SPACING;
        int startX = panelX + (PANEL_WIDTH - totalGridWidth) / 2;

        for (int i = 0; i < conflictingItems.size(); i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            int x = startX + col * SLOT_SPACING;
            int y = gridStartY + row * SLOT_SPACING - scrollOffset;

            if (y + ITEM_SLOT_SIZE < gridStartY || y > gridStartY + gridHeight) continue;

            if (mouseX >= x && mouseX < x + ITEM_SLOT_SIZE &&
                    mouseY >= y && mouseY < y + ITEM_SLOT_SIZE &&
                    mouseY >= gridStartY && mouseY <= gridStartY + gridHeight) {
                return i;
            }
        }
        return -1;
    }

    private void openRecipeDetailScreen(ItemEntry entry) {
        List<RecipeHandler.RecipeConflict> recipes = RecipeHandler.getConflictsForItem(entry.itemId);
        Minecraft.getInstance().setScreen(
                new RecipeDetailScreen(this, wheelPosition, entry.itemId, entry.itemStack, recipes)
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Empty - we draw our own background
    }

    private static class ItemEntry {
        final String itemId;
        final ItemStack itemStack;
        final int conflictCount;
        final boolean hasSelection;

        ItemEntry(String itemId, ItemStack itemStack, int conflictCount, boolean hasSelection) {
            this.itemId = itemId;
            this.itemStack = itemStack;
            this.conflictCount = conflictCount;
            this.hasSelection = hasSelection;
        }
    }
}
