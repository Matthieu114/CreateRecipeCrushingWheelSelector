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

import java.util.List;

/**
 * Recipe detail screen with proper responsive layout
 */
public class RecipeDetailScreen extends Screen {

    private static final int RECIPE_BOX_HEIGHT = 74;
    private static final int RECIPE_SPACING = 10;

    private final Screen parent;
    private final BlockPos wheelPosition;
    private final String inputItemId;
    private final ItemStack inputItemStack;
    private final List<RecipeHandler.RecipeConflict> recipes;

    private ResourceLocation selectedRecipeId;

    // scrolling state
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // scrollbar dragging state
    private boolean draggingThumb = false;
    private int dragStartY = 0;
    private int initialScroll = 0;

    public RecipeDetailScreen(Screen parent, BlockPos wheelPosition, String inputItemId,
                              ItemStack inputItemStack, List<RecipeHandler.RecipeConflict> recipes) {
        super(Component.literal("Select Recipe"));
        this.parent = parent;
        this.wheelPosition = wheelPosition;
        this.inputItemId = inputItemId;
        this.inputItemStack = inputItemStack;
        this.recipes = recipes;
        
        // Load saved selection from client cache
        this.selectedRecipeId = ClientSelectionCache.getSelection(wheelPosition, inputItemId);
        
        if (this.selectedRecipeId != null) {
            CrushingWheelRecipeSelector.LOGGER.info("Loaded saved selection: {} for item {} at wheel {}", 
                this.selectedRecipeId, inputItemId, wheelPosition);
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int centerX = this.width / 2;

        // Back button
        this.addRenderableWidget(
                Button.builder(Component.literal("← Back"), b -> this.onClose())
                        .bounds(centerX - buttonWidth - 5, this.height - 30, buttonWidth, buttonHeight)
                        .build()
        );

        // Clear selection button
        this.addRenderableWidget(
                Button.builder(Component.literal("Clear Selection"), b -> clearSelection())
                        .bounds(centerX + 5, this.height - 30, buttonWidth, buttonHeight)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Responsive panel sizing
        int panelWidth = Math.min(this.width - 40, 600);
        int panelHeight = this.height - 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;

        // panel background & border
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0101010);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF8B8B8B);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF8B8B8B);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF8B8B8B);
        graphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF8B8B8B);

        // title
        graphics.drawCenteredString(this.font, "Select Recipe", this.width / 2, panelY + 10, 0xFFFFFF);

        // input item info (centered)
        int inputY = panelY + 30;
        String inputText = "Configure recipes for:";
        int inputTextWidth = this.font.width(inputText);
        int itemNameWidth = this.font.width(inputItemStack.getHoverName().getString());
        int totalWidth = inputTextWidth + 26 + itemNameWidth; // text + icon + name
        int startX = (this.width - totalWidth) / 2;
        
        graphics.drawString(this.font, inputText, startX, inputY, 0xCCCCCC);
        graphics.renderItem(inputItemStack, startX + inputTextWidth + 4, inputY - 3);
        graphics.drawString(this.font, inputItemStack.getHoverName().getString(), 
            startX + inputTextWidth + 26, inputY, 0xFFFFFF);

        // recipe list area (responsive)
        renderRecipeList(graphics, mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRecipeList(GuiGraphics graphics, int mouseX, int mouseY, int panelX, int panelY, int panelWidth, int panelHeight) {
        // Define margins and spacing
        int headerHeight = 60; // space for title and input item
        int footerHeight = 40; // space for buttons
        int leftMargin = 12;
        
        // Calculate available space for recipe list
        int availableWidth = panelWidth - (leftMargin * 2);
        int availableHeight = panelHeight - headerHeight - footerHeight;
        
        // Recipe box should be responsive but have min/max sizes
        int recipeBoxWidth = Math.min(Math.max(280, availableWidth - 20), 500); // min 280, max 500, -20 for scrollbar
        
        // Position recipe list area (centered horizontally)
        int listX = panelX + (panelWidth - recipeBoxWidth) / 2;
        int listY = panelY + headerHeight;
        int listHeight = availableHeight;

        // compute content height and max scroll
        int contentHeight = recipes.size() * (RECIPE_BOX_HEIGHT + RECIPE_SPACING);
        maxScroll = Math.max(0, contentHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Only draw scrollbar if content is scrollable
        if (maxScroll > 0) {
            // draw scrollbar track and thumb
            int scrollbarX = listX + recipeBoxWidth + 6;
            int scrollbarY = listY;
            int scrollbarHeight = listHeight;
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 8, scrollbarY + scrollbarHeight, 0x50333333);

            // thumb sizing
            int thumbMin = 24;
            int thumbHeight = contentHeight <= 0 ? scrollbarHeight : Math.max(thumbMin, (int)((float)listHeight / (float)contentHeight * scrollbarHeight));
            int thumbMaxPosRange = Math.max(1, scrollbarHeight - thumbHeight);
            int thumbY = scrollbarY + (int)((float)scrollOffset / Math.max(1, maxScroll) * thumbMaxPosRange);

            graphics.fill(scrollbarX + 1, thumbY, scrollbarX + 7, thumbY + thumbHeight, 0xFF888888);
        }

        // Enable scissor (clipping) to prevent items from rendering outside the list area
        graphics.enableScissor(listX - 5, listY, listX + recipeBoxWidth + 15, listY + listHeight);

        // render recipes
        for (int i = 0; i < recipes.size(); i++) {
            int boxY = listY + i * (RECIPE_BOX_HEIGHT + RECIPE_SPACING) - scrollOffset;
            int boxX = listX;

            // cull boxes outside visible region
            if (boxY + RECIPE_BOX_HEIGHT < listY || boxY > listY + listHeight) continue;

            RecipeHandler.RecipeConflict recipe = recipes.get(i);

            boolean isSelected = recipe.recipeId().equals(selectedRecipeId);
            boolean isHovered = mouseX >= boxX && mouseX < boxX + recipeBoxWidth && 
                               mouseY >= boxY && mouseY < boxY + RECIPE_BOX_HEIGHT &&
                               mouseY >= listY && mouseY <= listY + listHeight; // only hover in visible area

            int backgroundColor = isSelected ? 0x8000FF00 : (isHovered ? 0x80666666 : 0x80333333);
            graphics.fill(boxX, boxY, boxX + recipeBoxWidth, boxY + RECIPE_BOX_HEIGHT, backgroundColor);

            int borderColor = isSelected ? 0xFF00FF00 : 0xFF888888;
            graphics.fill(boxX, boxY, boxX + recipeBoxWidth, boxY + 1, borderColor);
            graphics.fill(boxX, boxY + RECIPE_BOX_HEIGHT - 1, boxX + recipeBoxWidth, boxY + RECIPE_BOX_HEIGHT, borderColor);
            graphics.fill(boxX, boxY, boxX + 1, boxY + RECIPE_BOX_HEIGHT, borderColor);
            graphics.fill(boxX + recipeBoxWidth - 1, boxY, boxX + recipeBoxWidth, boxY + RECIPE_BOX_HEIGHT, borderColor);

            // header text
            graphics.drawString(this.font, "From: " + getModName(recipe.recipeId()), boxX + 6, boxY + 6, 0xFFFFFF);

            // outputs with dynamic wrapping
            // Skip index 0 as it's the primary output without chance/percentage
            List<String> outputs = recipe.outputs();
            int itemX = boxX + 6;
            int itemY = boxY + 22;
            int rowSpacing = 20;
            int maxInnerWidth = recipeBoxWidth - 12;

            // Start from index 1 to skip the primary output without chance
            for (int j = 1; j < outputs.size(); j++) {
                String output = outputs.get(j);
                ItemStack outStack = getItemStackFromOutput(output);
                if (outStack.isEmpty()) continue;

                String countText = getOutputText(output);
                int textWidth = this.font.width(countText);
                int slotWidth = Math.max(24, 16 + 4 + textWidth + 6);

                // wrap to next row if needed
                if (itemX + slotWidth > boxX + maxInnerWidth) {
                    itemX = boxX + 6;
                    itemY += rowSpacing;
                }

                graphics.renderItem(outStack, itemX, itemY);
                graphics.drawString(this.font, countText, itemX + 20, itemY + 4, 0xFFFFFF);

                itemX += slotWidth + 6;
            }
            
            // If no secondary outputs, show a message
            if (outputs.size() <= 1) {
                graphics.drawString(this.font, "§7No secondary outputs", boxX + 6, itemY, 0x888888);
            }

            // selection checkmark
            if (isSelected) {
                graphics.drawString(this.font, "✓", boxX + recipeBoxWidth - 14, boxY + 6, 0x00FF00);
            }

            // tooltip
            if (isHovered && !isSelected) {
                // Disable scissor temporarily for tooltip rendering
                graphics.disableScissor();
                graphics.renderTooltip(this.font, List.of(Component.literal("Click to select this recipe").getVisualOrderText()), mouseX, mouseY);
                graphics.enableScissor(listX - 5, listY, listX + recipeBoxWidth + 15, listY + listHeight);
            }
        }

        // Disable scissor after rendering
        graphics.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int scrollAmount = (int) Math.signum(scrollY) * 20;
        scrollOffset -= scrollAmount;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Calculate panel dimensions (same as in render)
        int panelWidth = Math.min(this.width - 40, 600);
        int panelHeight = this.height - 80;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;
        
        int headerHeight = 60;
        int footerHeight = 40;
        int availableWidth = panelWidth - 24;
        int availableHeight = panelHeight - headerHeight - footerHeight;
        int recipeBoxWidth = Math.min(Math.max(280, availableWidth - 20), 500);
        
        int listX = panelX + (panelWidth - recipeBoxWidth) / 2;
        int listY = panelY + headerHeight;
        int listHeight = availableHeight;

        // scrollbar geometry
        int scrollbarX = listX + recipeBoxWidth + 6;
        int scrollbarY = listY;
        int scrollbarHeight = listHeight;
        int contentHeight = recipes.size() * (RECIPE_BOX_HEIGHT + RECIPE_SPACING);
        int thumbMin = 24;
        int thumbHeight = contentHeight <= 0 ? scrollbarHeight : Math.max(thumbMin, (int)((float)listHeight / (float)contentHeight * scrollbarHeight));
        int thumbMaxPosRange = Math.max(1, scrollbarHeight - thumbHeight);
        int thumbY = scrollbarY + (int)((float)scrollOffset / Math.max(1, maxScroll) * thumbMaxPosRange);
        if (maxScroll == 0) thumbY = scrollbarY;

        // check if clicking scrollbar thumb
        if (mouseX >= scrollbarX + 1 && mouseX <= scrollbarX + 7 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
            draggingThumb = true;
            dragStartY = (int) mouseY;
            initialScroll = scrollOffset;
            return true;
        }

        // check recipe box clicks
        for (int i = 0; i < recipes.size(); i++) {
            int boxY = listY + i * (RECIPE_BOX_HEIGHT + RECIPE_SPACING) - scrollOffset;
            int boxX = listX;
            
            // only click if in visible area
            if (boxY + RECIPE_BOX_HEIGHT < listY || boxY > listY + listHeight) continue;
            
            if (mouseX >= boxX && mouseX < boxX + recipeBoxWidth && mouseY >= boxY && mouseY < boxY + RECIPE_BOX_HEIGHT) {
                selectRecipe(recipes.get(i));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingThumb) {
            int panelWidth = Math.min(this.width - 40, 600);
            int panelHeight = this.height - 80;
            int panelY = 40;
            int headerHeight = 60;
            int footerHeight = 40;
            int listHeight = panelHeight - headerHeight - footerHeight;

            int contentHeight = recipes.size() * (RECIPE_BOX_HEIGHT + RECIPE_SPACING);
            int thumbMin = 24;
            int thumbHeight = contentHeight <= 0 ? listHeight : Math.max(thumbMin, (int)((float)listHeight / (float)contentHeight * listHeight));
            int thumbMaxPosRange = Math.max(1, listHeight - thumbHeight);

            int deltaY = (int) mouseY - dragStartY;
            int scrollDelta = (int) ((float) deltaY / (float) Math.max(1, thumbMaxPosRange) * (float) maxScroll);
            scrollOffset = initialScroll + scrollDelta;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingThumb) {
            draggingThumb = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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

    private String getOutputText(String output) {
        try {
            String[] parts = output.split(" ", 2);
            if (parts.length > 1) return parts[1];
        } catch (Exception ignored) {}
        return "";
    }

    private void selectRecipe(RecipeHandler.RecipeConflict recipe) {
        this.selectedRecipeId = recipe.recipeId();

        CrushingWheelRecipeSelector.LOGGER.info("Selected recipe {} for item {} at wheel {}", recipe.recipeId(), inputItemId, wheelPosition);

        // Send packet to server to save the selection
        PacketDistributor.sendToServer(new SelectRecipePacket(wheelPosition, inputItemId, recipe.recipeId()));

        // Play click sound
        Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);

        // Show feedback message
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§aSelected: §f" + recipe.primaryOutput()), true);
        }
    }

    private void clearSelection() {
        this.selectedRecipeId = null;
        CrushingWheelRecipeSelector.LOGGER.info("Cleared selection for item {} at wheel {}", inputItemId, wheelPosition);

        // Send packet to server to clear the selection
        PacketDistributor.sendToServer(new ClearRecipePacket(wheelPosition, inputItemId));

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("§7Selection cleared"), true);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0x60000000, 0x60000000);
    }
}
