package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.neo.shadesclient.modules.ItemRenameModule;

import java.util.ArrayList;
import java.util.List;

public class ItemRenameConfigGUI extends Screen {
    private final ItemRenameModule module;
    private final Screen parent;
    private TextFieldWidget nameField;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final List<ColorButton> colorButtons = new ArrayList<>();

    // Colors - using the same color scheme as ModuleConfigGUI
    private static final int BACKGROUND_COLOR = 0x90000000; // Semi-transparent black
    private static final int HEADER_COLOR = 0xFF1A1A1A; // Dark header
    private static final int TEXT_COLOR = 0xFFE0E0E0; // Light gray for text

    // Currently selected item preview
    private ItemStack currentItem = null;

    // All available colors
    private static final Formatting[] AVAILABLE_COLORS = new Formatting[] {
            Formatting.WHITE, Formatting.RED, Formatting.GREEN, Formatting.BLUE,
            Formatting.YELLOW, Formatting.AQUA, Formatting.LIGHT_PURPLE, Formatting.GOLD,
            Formatting.DARK_RED, Formatting.DARK_GREEN, Formatting.DARK_BLUE,
            Formatting.DARK_AQUA, Formatting.DARK_PURPLE, Formatting.GRAY
    };

    public ItemRenameConfigGUI(Screen parent, ItemRenameModule module) {
        super(Text.of(module.getName() + " Configuration"));
        this.parent = parent;
        this.module = module;

        // Get current item from player's hand
        if (MinecraftClient.getInstance().player != null) {
            currentItem = MinecraftClient.getInstance().player.getMainHandStack();
            this.module.setCurrentItem(currentItem);
        }
    }

    @Override
    protected void init() {
        super.init();

        // Add text field for renaming
        nameField = new TextFieldWidget(textRenderer, width / 2 - 100, 100, 200, 20, Text.of("Item Name"));
        nameField.setMaxLength(50);

        // Get original name if item exists
        if (currentItem != null && !currentItem.isEmpty()) {
            String originalName = currentItem.getName().getString();
            nameField.setText(originalName);
        }

        this.addDrawableChild(nameField);

        // Add color selection buttons
        addColorButtons();

        // Add rename button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Rename Item"), button -> {
            if (currentItem != null && !currentItem.isEmpty()) {
                module.renameCurrentItem(nameField.getText());
            }
        }).dimensions(width / 2 - 100, height - 80, 200, 20).build());

        // Add reset button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Reset Name"), button -> {
            if (currentItem != null && !currentItem.isEmpty()) {
                module.resetCurrentItemName();
                // Update text field to show original name
                if (currentItem.getName().getString().equals(Text.translatable(currentItem.getItem().getTranslationKey()).getString())) {
                    // If the current name matches the translation key, it's the default name
                    nameField.setText(currentItem.getName().getString());
                } else {
                    // Otherwise it's a custom name
                    nameField.setText(currentItem.getName().getString());
                }
            }
        }).dimensions(width / 2 - 100, height - 50, 200, 20).build());

        // Add back button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            client.setScreen(parent);
        }).dimensions(10, height - 30, 80, 20).build());
    }

    private void addColorButtons() {
        int buttonsPerRow = 7;
        int buttonSize = 20;
        int spacing = 5;
        int startX = width / 2 - ((buttonSize + spacing) * buttonsPerRow) / 2;
        int startY = 140;

        for (int i = 0; i < AVAILABLE_COLORS.length; i++) {
            int row = i / buttonsPerRow;
            int col = i % buttonsPerRow;

            int x = startX + col * (buttonSize + spacing);
            int y = startY + row * (buttonSize + spacing);

            ColorButton colorButton = new ColorButton(
                    x, y, buttonSize, buttonSize,
                    Text.literal(""),
                    button -> {
                        // When clicked, update the current color
                        module.setCurrentColor(((ColorButton)button).getColor());
                        updateColorButtonStates();
                    },
                    AVAILABLE_COLORS[i]
            );

            this.addDrawableChild(colorButton);
            colorButtons.add(colorButton);
        }

        // Set initial button states
        updateColorButtonStates();
    }

    private void updateColorButtonStates() {
        for (ColorButton button : colorButtons) {
            button.setSelected(button.getColor() == module.getCurrentColor());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background
        context.fill(0, 0, width, height, BACKGROUND_COLOR);

        // Draw header
        context.fill(0, 0, width, 40, HEADER_COLOR);

        // Draw header text
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.of(module.getName() + " Configuration"),
                width / 2,
                15,
                TEXT_COLOR
        );

        // Draw item preview
        if (currentItem != null && !currentItem.isEmpty()) {
            // Draw item
            context.drawItem(currentItem, width / 2 - 8, 60);

            // Draw item name with selected color
            String previewName = nameField.getText().isEmpty() ?
                    currentItem.getName().getString() : nameField.getText();

            Text coloredName = Text.literal(previewName).formatted(module.getCurrentColor());
            context.drawCenteredTextWithShadow(textRenderer, coloredName, width / 2, 85, 0xFFFFFF);
        } else {
            // No item selected message
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("Hold an item in your main hand!").formatted(Formatting.RED),
                    width / 2,
                    70,
                    0xFFFFFF
            );
        }

        // Draw instructions
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Select a color:"),
                width / 2,
                130,
                0xFFFFFF
        );

        // Draw all widgets
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();
        // TextFieldWidget no longer has tick() or tickField() methods in newer versions
        // The widget is updated automatically when rendered

        // Check if player switched items
        if (client.player != null) {
            ItemStack handItem = client.player.getMainHandStack();
            if (!ItemStack.areEqual(handItem, currentItem)) {
                currentItem = handItem;
                module.setCurrentItem(currentItem);

                // Update name field with new item's name
                if (currentItem != null && !currentItem.isEmpty()) {
                    String originalName = currentItem.getName().getString();
                    nameField.setText(originalName);
                } else {
                    nameField.setText("");
                }
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * Custom button for color selection
     */
    private class ColorButton extends ButtonWidget {
        private final Formatting color;
        private boolean selected = false;

        public ColorButton(int x, int y, int width, int height, Text message, PressAction onPress, Formatting color) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.color = color;
        }

        public Formatting getColor() {
            return color;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int backgroundColor = selected ? 0xFF000000 : 0xFF333333;
            int colorValue = getColorValue(color);
            int borderColor = selected ? 0xFFFFFFFF : (isHovered() ? 0xFFCCCCCC : 0xFF666666);

            // Draw button background
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), backgroundColor);

            // Draw color square
            context.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, colorValue);

            // Draw border
            context.drawBorder(getX(), getY(), getWidth(), getHeight(), borderColor);
        }

        private int getColorValue(Formatting format) {
            switch (format) {
                case BLACK: return 0xFF000000;
                case DARK_BLUE: return 0xFF0000AA;
                case DARK_GREEN: return 0xFF00AA00;
                case DARK_AQUA: return 0xFF00AAAA;
                case DARK_RED: return 0xFFAA0000;
                case DARK_PURPLE: return 0xFFAA00AA;
                case GOLD: return 0xFFFFAA00;
                case GRAY: return 0xFFAAAAAA;
                case DARK_GRAY: return 0xFF555555;
                case BLUE: return 0xFF5555FF;
                case GREEN: return 0xFF55FF55;
                case AQUA: return 0xFF55FFFF;
                case RED: return 0xFFFF5555;
                case LIGHT_PURPLE: return 0xFFFF55FF;
                case YELLOW: return 0xFFFFFF55;
                case WHITE:
                default: return 0xFFFFFFFF;
            }
        }
    }
}