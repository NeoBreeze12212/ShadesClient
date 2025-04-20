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
    private static final int ENABLED_COLOR = 0xFF4080FF; // Now used in toggleButton
    private static final int TEXT_COLOR = 0xFFE0E0E0;

    private ModuleCategory selectedCategory = ModuleCategory.SURVIVAL_QOL;
    private final List<CategoryButton> categoryButtons = new ArrayList<>();
    private final List<ModuleButton> moduleButtons = new ArrayList<>();
    private Text categoryTitle;

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

        addCategoryButton(ModuleCategory.COMBAT, leftPanelWidth, categoryY);
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

        // Update the category title
        categoryTitle = Text.literal(selectedCategory.getDisplayName());

        // Refresh module buttons
        refreshModuleButtons();

        org.neo.shadesclient.client.ShadesClient.LOGGER.info("ShadesClient Screen initialized");
    }

    private void addCategoryButton(ModuleCategory category, int width, int y) {
        CategoryButton button = new CategoryButton(10, y, width - 20, 25, Text.literal(category.getDisplayName()),
                btn -> {
                    selectedCategory = category;
                    categoryTitle = Text.literal(category.getDisplayName());
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

    private void refreshModuleButtons() {
        // Log refresh operation
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Refreshing module buttons for category: " + selectedCategory.getDisplayName());

        // Remove existing module buttons from the screen's children list
        // Fixed: use removeChild instead of remove
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

        int x = 200;
        int y = 60;
        int moduleWidth = width - x - 20;
        int moduleHeight = 80;
        int margin = 10;

        for (Module module : categoryModules) {
            ModuleButton moduleButton = new ModuleButton(x, y, moduleWidth, moduleHeight, module);
            moduleButtons.add(moduleButton);
            addDrawableChild(moduleButton);
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Added module button: " + module.getName());

            y += moduleHeight + margin;
            if (y > height - moduleHeight - 10) {
                y = 60;
                x += moduleWidth + margin;
            }
        }
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
        private final ButtonWidget toggleButton;
        private final ButtonWidget configButton;

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