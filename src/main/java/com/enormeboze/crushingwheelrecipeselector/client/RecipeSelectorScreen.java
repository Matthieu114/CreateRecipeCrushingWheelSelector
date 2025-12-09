package com.enormeboze.crushingwheelrecipeselector.client;

import com.enormeboze.crushingwheelrecipeselector.RecipeHandler;
import com.enormeboze.crushingwheelrecipeselector.network.ClearRecipePacket;
import com.enormeboze.crushingwheelrecipeselector.network.ModNetworking;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI screen for selecting preferred crushing wheel recipes.
 * Uses RecipeHandler to get conflicting recipes instead of Create's RecipeFinder.
 */
@OnlyIn(Dist.CLIENT)
public class RecipeSelectorScreen extends Screen {

    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int ITEM_BG = 0xFFB8B8B8;
    private static final int ITEM_SELECTED = 0xFFAAEEAA;
    private static final int ITEM_HOVER = 0xFFD0D0D0;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int TEXT_COLOR = 0xFF404040;

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 220;
    private static final int ITEM_HEIGHT = 36;
    private static final int ITEM_SPACING = 4;
    private static final int SLOT_SIZE = 18;

    private final BlockPos wheelPos;
    private final Map<String, List<RecipeHandler.RecipeConflict>> recipesByInput = new HashMap<>();
    private final Map<String, ResourceLocation> currentSelections;
    private final List<String> inputItemIds = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;

    public RecipeSelectorScreen(BlockPos wheelPos) {
        super(Component.literal("Crushing Wheel Recipe Selector"));
        this.wheelPos = wheelPos;
        this.currentSelections = new HashMap<>(ClientSelectionCache.getCurrentSelections());
        loadRecipes();
    }

    private void loadRecipes() {
        // Use RecipeHandler's cached conflicting recipes
        Map<String, List<RecipeHandler.RecipeConflict>> conflicts = RecipeHandler.getConflictingRecipes();

        recipesByInput.clear();
        inputItemIds.clear();

        for (Map.Entry<String, List<RecipeHandler.RecipeConflict>> entry : conflicts.entrySet()) {
            if (entry.getValue().size() > 1) {
                recipesByInput.put(entry.getKey(), entry.getValue());
                inputItemIds.add(entry.getKey());
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Close button
        this.addRenderableWidget(
                Button.builder(Component.literal("Close"), b -> onClose())
                        .bounds(panelX + PANEL_WIDTH / 2 - 30, panelY + PANEL_HEIGHT - 24, 60, 20)
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

        // Title
        String title = "Select Input Item";
        int titleX = panelX + PANEL_WIDTH / 2 - this.font.width(title) / 2;
        graphics.drawString(this.font, title, titleX, panelY + 6, TEXT_COLOR, false);

        if (recipesByInput.isEmpty()) {
            graphics.drawCenteredString(this.font, "No recipe conflicts found!",
                    this.width / 2, this.height / 2, 0xAAAAAA);
        } else {
            renderItemList(graphics, mouseX, mouseY, panelX, panelY);
        }

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

    private void drawItemBox(GuiGraphics graphics, int x, int y, int width, int height, boolean selected, boolean hovered) {
        int bgColor = selected ? ITEM_SELECTED : (hovered ? ITEM_HOVER : ITEM_BG);

        // Main fill with rounded corners
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, bgColor);
        graphics.fill(x + 3, y + 1, x + width - 3, y + height - 1, bgColor);
        graphics.fill(x + 1, y + 3, x + width - 1, y + height - 3, bgColor);

        int borderColor = selected ? 0xFF00AA00 : BORDER_DARK;

        // Border
        graphics.fill(x + 3, y, x + width - 3, y + 1, borderColor);
        graphics.fill(x + 1, y + 1, x + 3, y + 2, borderColor);
        graphics.fill(x + 1, y + 2, x + 2, y + 3, borderColor);
        graphics.fill(x, y + 3, x + 1, y + height - 3, borderColor);
        graphics.fill(x + 3, y + height - 1, x + width - 3, y + height, borderColor);
        graphics.fill(x + width - 3, y + height - 2, x + width - 1, y + height - 1, borderColor);
        graphics.fill(x + width - 2, y + height - 3, x + width - 1, y + height - 2, borderColor);
        graphics.fill(x + width - 1, y + 3, x + width, y + height - 3, borderColor);
        graphics.fill(x + 1, y + height - 3, x + 2, y + height - 2, borderColor);
        graphics.fill(x + 2, y + height - 2, x + 3, y + height - 1, borderColor);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 2, borderColor);
        graphics.fill(x + width - 3, y + 2, x + width - 2, y + 3, borderColor);
    }

    private void drawItemSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 1, y, x + SLOT_SIZE - 1, y + SLOT_SIZE, SLOT_BG);
        graphics.fill(x, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, SLOT_BG);

        graphics.fill(x + 1, y, x + SLOT_SIZE - 1, y + 1, BORDER_DARK);
        graphics.fill(x, y + 1, x + 1, y + SLOT_SIZE - 1, BORDER_DARK);
        graphics.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE - 1, y + SLOT_SIZE, BORDER_LIGHT);
        graphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, BORDER_LIGHT);
    }

    private void renderItemList(GuiGraphics graphics, int mouseX, int mouseY, int panelX, int panelY) {
        int listStartY = panelY + 22;
        int listHeight = PANEL_HEIGHT - 52;
        int itemWidth = PANEL_WIDTH - 20;
        int itemX = panelX + 10;

        int contentHeight = inputItemIds.size() * (ITEM_HEIGHT + ITEM_SPACING);
        maxScroll = Math.max(0, contentHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(panelX + 5, listStartY, panelX + PANEL_WIDTH - 5, listStartY + listHeight);

        for (int i = 0; i < inputItemIds.size(); i++) {
            String inputItemId = inputItemIds.get(i);
            int boxY = listStartY + i * (ITEM_HEIGHT + ITEM_SPACING) - scrollOffset;

            if (boxY + ITEM_HEIGHT < listStartY || boxY > listStartY + listHeight) continue;

            boolean hasSelection = currentSelections.containsKey(inputItemId);
            boolean isHovered = mouseX >= itemX && mouseX < itemX + itemWidth &&
                    mouseY >= boxY && mouseY < boxY + ITEM_HEIGHT &&
                    mouseY >= listStartY && mouseY <= listStartY + listHeight;

            drawItemBox(graphics, itemX, boxY, itemWidth, ITEM_HEIGHT, hasSelection, isHovered);

            // Item slot
            int slotX = itemX + 6;
            int slotY = boxY + (ITEM_HEIGHT - SLOT_SIZE) / 2;
            drawItemSlot(graphics, slotX, slotY);

            // Render item
            ItemStack inputStack = getItemStackFromId(inputItemId);
            graphics.renderItem(inputStack, slotX + 1, slotY + 1);

            // Item name
            String displayName = inputStack.getHoverName().getString();
            if (displayName.length() > 18) {
                displayName = displayName.substring(0, 16) + "..";
            }
            graphics.drawString(this.font, displayName, slotX + SLOT_SIZE + 6, boxY + 6, TEXT_COLOR, false);

            // Recipe count
            List<RecipeHandler.RecipeConflict> recipes = recipesByInput.get(inputItemId);
            int recipeCount = recipes != null ? recipes.size() : 0;
            graphics.drawString(this.font, recipeCount + " recipes", slotX + SLOT_SIZE + 6, boxY + 18, 0xFF888888, false);

            // Checkmark if selection exists
            if (hasSelection) {
                graphics.drawString(this.font, "\u2714", itemX + itemWidth - 14, boxY + 6, 0xFF00DD00, false);
            }

            // Arrow indicator
            graphics.drawString(this.font, "\u25B6", itemX + itemWidth - 14, boxY + 18, 0xFF666666, false);
        }

        graphics.disableScissor();

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

    private ItemStack getItemStackFromId(String itemId) {
        try {
            ResourceLocation location = new ResourceLocation(itemId);
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(location);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // Ignore
        }
        return ItemStack.EMPTY;
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
            int panelX = (this.width - PANEL_WIDTH) / 2;
            int panelY = (this.height - PANEL_HEIGHT) / 2;
            int listStartY = panelY + 22;
            int listHeight = PANEL_HEIGHT - 52;
            int itemWidth = PANEL_WIDTH - 20;
            int itemX = panelX + 10;

            for (int i = 0; i < inputItemIds.size(); i++) {
                String inputItemId = inputItemIds.get(i);
                int boxY = listStartY + i * (ITEM_HEIGHT + ITEM_SPACING) - scrollOffset;

                if (boxY + ITEM_HEIGHT < listStartY || boxY > listStartY + listHeight) continue;

                if (mouseX >= itemX && mouseX < itemX + itemWidth &&
                        mouseY >= boxY && mouseY < boxY + ITEM_HEIGHT &&
                        mouseY >= listStartY && mouseY <= listStartY + listHeight) {
                    openDetailScreen(inputItemId);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openDetailScreen(String inputItemId) {
        List<RecipeHandler.RecipeConflict> recipes = recipesByInput.get(inputItemId);
        if (recipes != null && !recipes.isEmpty()) {
            ItemStack inputStack = getItemStackFromId(inputItemId);
            Minecraft.getInstance().setScreen(new RecipeDetailScreen(
                    this, wheelPos, inputItemId, inputStack, recipes
            ));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Empty - we draw our own background
    }
}