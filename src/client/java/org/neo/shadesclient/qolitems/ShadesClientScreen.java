package org.neo.shadesclient.qolitems;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;

import java.util.ArrayList;
import java.util.List;

public class ShadesClientScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int HEADER_COLOR = 0xFF1A1A1A;
    private static final int SELECTED_CATEGORY_COLOR = 0xFF3050CF;
    private static final int CATEGORY_HOVER_COLOR = 0xFF404040;
    private static final int CATEGORY_COLOR = 0xFF303030;
    private static final int MODULE_COLOR = 0xFF262626;
    private static final int MODULE_HOVER_COLOR = 0xFF383838;
    private static final int ENABLED_COLOR = 0xFF4080FF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int SCROLL_BUTTON_COLOR = 0xFF404040;
    private static final int SCROLL_BUTTON_HOVER_COLOR = 0xFF606060;

    private ModuleCategory selectedCategory = ModuleCategory.SURVIVAL_QOL;
    private final List<CategoryButton> categoryButtons = new ArrayList<>();
    private final List<ModuleButton> moduleButtons = new ArrayList<>();
    private Text categoryTitle;

    // Scroll variables
    private int scrollX = 0;
    private int maxScrollX = 0;
    private ButtonWidget leftScrollButton;
    private ButtonWidget rightScrollButton;

    // Grid layout settings
    private static final int MODULES_PER_COLUMN = 3;
    private static final int MODULE_WIDTH = 220;
    private static final int MODULE_HEIGHT = 80;
    private static final int MODULE_MARGIN = 10;

    public ShadesClientScreen() {
        super(Text.literal("ShadesClient"));
    }

    @Override
    protected void init() {
        // Log that the screen is initializing
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Initializing ShadesClient Screen");

        int leftPanelWidth = 180;
        int yStart = 40;
        int buttonHeight = 30;

        // Initialize category buttons
        categoryButtons.clear();
        int categoryY = yStart;

        // FEATURES Header (no need for a button here)
        categoryY += 25;

        // Category buttons
        addCategoryButton(ModuleCategory.SURVIVAL_QOL, leftPanelWidth, categoryY);
        categoryY += buttonHeight;

        addCategoryButton(ModuleCategory.EXTERNAL_TOOLS, leftPanelWidth, categoryY);
        categoryY += buttonHeight;

        addCategoryButton(ModuleCategory.VISUALS, leftPanelWidth, categoryY);
        categoryY += buttonHeight;

        addCategoryButton(ModuleCategory.GAMEPLAY, leftPanelWidth, categoryY);
        categoryY += buttonHeight + 20;

        // SETTINGS Header (no need for a button here)
        categoryY += 25;

        // Settings buttons
        addButton("Configuration", leftPanelWidth, categoryY, button -> {
            // Open configuration screen
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Configuration button clicked");
        });
        categoryY += buttonHeight;

        addButton("Statistics", leftPanelWidth, categoryY, button -> {
            // Open statistics screen
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Statistics button clicked");
        });

        // Add scroll buttons
        leftScrollButton = ButtonWidget.builder(Text.literal("<"), button -> {
            scrollLeft();
        }).dimensions(190, height / 2, 20, 40).build();

        rightScrollButton = ButtonWidget.builder(Text.literal(">"), button -> {
            scrollRight();
        }).dimensions(width - 30, height / 2, 20, 40).build();

        addDrawableChild(leftScrollButton);
        addDrawableChild(rightScrollButton);

        // Update the category title
        categoryTitle = Text.literal(selectedCategory.getDisplayName());

        // Reset scroll position when initializing
        scrollX = 0;

        // Refresh module buttons
        refreshModuleButtons();

        org.neo.shadesclient.client.ShadesClient.LOGGER.info("ShadesClient Screen initialized");
    }

    private void addCategoryButton(ModuleCategory category, int width, int y) {
        CategoryButton button = new CategoryButton(10, y, width - 20, 25, Text.literal(category.getDisplayName()),
                btn -> {
                    selectedCategory = category;
                    categoryTitle = Text.literal(category.getDisplayName());
                    scrollX = 0; // Reset scroll position when changing categories
                    refreshModuleButtons();
                    org.neo.shadesclient.client.ShadesClient.LOGGER.info("Selected category: " + category.getDisplayName());
                }, category);
        categoryButtons.add(button);
        addDrawableChild(button);
    }

    private void addButton(String text, int width, int y, ButtonWidget.PressAction action) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(text), action)
                .dimensions(10, y, width - 20, 25)
                .build();
        addDrawableChild(button);
    }

    private void scrollLeft() {
        scrollX = Math.max(0, scrollX - MODULE_WIDTH - MODULE_MARGIN);
        updateModuleButtonPositions();
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Scrolled left, new scrollX: " + scrollX);
    }

    private void scrollRight() {
        if (scrollX < maxScrollX) {
            scrollX = Math.min(maxScrollX, scrollX + MODULE_WIDTH + MODULE_MARGIN);
            updateModuleButtonPositions();
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Scrolled right, new scrollX: " + scrollX);
        }
    }

    private void updateModuleButtonPositions() {
        List<Module> categoryModules = ModuleManager.getModulesByCategory(selectedCategory);
        int columnCount = (int) Math.ceil((double) categoryModules.size() / MODULES_PER_COLUMN);

        int contentWidth = 200; // Starting X position for modules
        int visibleWidth = width - contentWidth;
        int totalWidth = columnCount * (MODULE_WIDTH + MODULE_MARGIN);

        // Only show scroll buttons if needed
        leftScrollButton.visible = scrollX > 0;
        rightScrollButton.visible = totalWidth > visibleWidth && scrollX < maxScrollX;

        // Update position of all module buttons
        for (int i = 0; i < moduleButtons.size(); i++) {
            Module module = categoryModules.get(i);
            ModuleButton button = moduleButtons.get(i);

            int column = i / MODULES_PER_COLUMN;
            int row = i % MODULES_PER_COLUMN;

            int x = contentWidth + column * (MODULE_WIDTH + MODULE_MARGIN) - scrollX;
            int y = 60 + row * (MODULE_HEIGHT + MODULE_MARGIN);

            button.setPosition(x, y);

            // Update toggle and config buttons positions
            if (button.toggleButton != null) {
                button.toggleButton.setX(x + MODULE_WIDTH - 60);
                button.toggleButton.setY(y + 10);
            }

            if (button.configButton != null) {
                button.configButton.setX(x + MODULE_WIDTH - 140);
                button.configButton.setY(y + MODULE_HEIGHT - 30);
            }
        }
    }

    private void refreshModuleButtons() {
        // Log refresh operation
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Refreshing module buttons for category: " + selectedCategory.getDisplayName());

        // Remove existing module buttons from the screen's children list
        for (ModuleButton button : new ArrayList<>(moduleButtons)) {
            remove(button);
            // Also remove the toggle and config buttons
            if (button.toggleButton != null) {
                remove(button.toggleButton);
            }
            if (button.configButton != null) {
                remove(button.configButton);
            }
        }
        moduleButtons.clear();

        // Populate with new module buttons for the current category
        List<Module> categoryModules = ModuleManager.getModulesByCategory(selectedCategory);
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Found " + categoryModules.size() + " modules for category " + selectedCategory.getDisplayName());

        int contentWidth = 200; // Starting X position for modules
        int columnCount = (int) Math.ceil((double) categoryModules.size() / MODULES_PER_COLUMN);

        // Calculate the max scroll value
        int visibleWidth = width - contentWidth;
        int totalWidth = columnCount * (MODULE_WIDTH + MODULE_MARGIN);
        maxScrollX = Math.max(0, totalWidth - visibleWidth + MODULE_MARGIN);

        // Create module buttons
        for (int i = 0; i < categoryModules.size(); i++) {
            Module module = categoryModules.get(i);

            int column = i / MODULES_PER_COLUMN;
            int row = i % MODULES_PER_COLUMN;

            int x = contentWidth + column * (MODULE_WIDTH + MODULE_MARGIN) - scrollX;
            int y = 60 + row * (MODULE_HEIGHT + MODULE_MARGIN);

            ModuleButton moduleButton = new ModuleButton(x, y, MODULE_WIDTH, MODULE_HEIGHT, module);
            moduleButtons.add(moduleButton);
            addDrawableChild(moduleButton);
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Added module button: " + module.getName());
        }

        // Update scroll button visibility
        leftScrollButton.visible = scrollX > 0;
        rightScrollButton.visible = totalWidth > visibleWidth && scrollX < maxScrollX;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Log rendering start with a constant string
        org.neo.shadesclient.client.ShadesClient.LOGGER.debug("Rendering ShadesClient Screen");

        // Fill the background
        renderBackground(context);

        // Draw panel backgrounds
        context.fill(0, 0, 180, height, BACKGROUND_COLOR);  // Left panel
        context.fill(180, 0, width, height, 0x80000000);   // Right panel

        // Draw left panel headers
        drawCenteredText(context, Text.literal("FEATURES"), 90, 20, TEXT_COLOR);
        drawCenteredText(context, Text.literal("SETTINGS"), 90, 200, TEXT_COLOR);

        // Draw right panel header
        context.fill(180, 0, width, 40, HEADER_COLOR);
        drawCenteredText(context, categoryTitle, (width - 180) / 2 + 180, 15, TEXT_COLOR);

        // Draw version info
        drawTextWithShadow(context, Text.literal("ShadesClient v1.0.0"), 10, height - 15, 0xAAAAAA);

        // Draw child elements (buttons, etc.)
        super.render(context, mouseX, mouseY, delta);
    }

    // Helper method for DrawContext compatibility
    private void renderBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xC0101010); // Semi-transparent dark background
    }

    private void drawCenteredText(DrawContext context, Text text, int x, int y, int color) {
        int textWidth = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, x - textWidth / 2, y, color, true);
    }

    private void drawTextWithShadow(DrawContext context, Text text, int x, int y, int color) {
        context.drawText(textRenderer, text, x, y, color, true);
    }

    // Updated button implementations with correct method names
    class CategoryButton extends ButtonWidget {
        private final ModuleCategory category;

        public CategoryButton(int x, int y, int width, int height, Text message, PressAction onPress, ModuleCategory category) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.category = category;
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int color = category == selectedCategory ? SELECTED_CATEGORY_COLOR :
                    isHovered() ? CATEGORY_HOVER_COLOR : CATEGORY_COLOR;

            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);

            int textX = getX() + getWidth() / 2 - textRenderer.getWidth(getMessage()) / 2;
            int textY = getY() + (getHeight() - 8) / 2;

            context.drawText(textRenderer, getMessage(), textX, textY, TEXT_COLOR, false);
        }
    }

    class ModuleButton extends ButtonWidget {
        private final Module module;
        private ButtonWidget toggleButton;
        private ButtonWidget configButton;

        public ModuleButton(int x, int y, int width, int height, Module module) {
            super(x, y, width, height, Text.literal(module.getName()), button -> {
                // Basic click action - could be empty or do something when clicking the module area
            }, DEFAULT_NARRATION_SUPPLIER);
            this.module = module;

            // Create toggle button
            this.toggleButton = ButtonWidget.builder(
                            Text.literal(module.isEnabled() ? "ON" : "OFF"),
                            button -> {
                                module.toggle();
                                button.setMessage(Text.literal(module.isEnabled() ? "ON" : "OFF"));
                                // Use ENABLED_COLOR for the button text when enabled
                                org.neo.shadesclient.client.ShadesClient.LOGGER.info("Toggled module: " + module.getName() + " - " +
                                        (module.isEnabled() ? "enabled" : "disabled"));
                            })
                    .dimensions(x + width - 60, y + 10, 50, 20)
                    .build();
            addDrawableChild(toggleButton);

            // Create config button if module has config
            if (module.hasConfigScreen()) {
                this.configButton = ButtonWidget.builder(
                                Text.literal("Configure"),
                                button -> {
                                    module.openConfigScreen();
                                    org.neo.shadesclient.client.ShadesClient.LOGGER.info("Opening config for: " + module.getName());
                                })
                        .dimensions(x + width - 140, y + height - 30, 120, 20)
                        .build();
                addDrawableChild(configButton);
            } else {
                this.configButton = null;
            }
        }

        public void setPosition(int x, int y) {
            setX(x);
            setY(y);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int color = isHovered() ? MODULE_HOVER_COLOR : MODULE_COLOR;
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);

            // Draw module name
            context.drawText(textRenderer, getMessage(), getX() + 10, getY() + 10, TEXT_COLOR, false);

            // Draw module description
            List<OrderedText> descriptionLines = textRenderer.wrapLines(Text.literal(module.getDescription()), getWidth() - 70);
            int lineY = getY() + 30;
            for (OrderedText line : descriptionLines) {
                context.drawText(textRenderer, line, getX() + 10, lineY, 0xAAAAAA, false);
                lineY += 10;
            }

            // Toggle and config buttons are drawn by the parent class as they're registered as drawable children
        }
    }
}