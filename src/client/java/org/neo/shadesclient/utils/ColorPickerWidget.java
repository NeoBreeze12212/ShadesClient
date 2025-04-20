package org.neo.shadesclient.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ColorPickerWidget extends ButtonWidget {
    private static final int[] PRESET_COLORS = new int[] {
            0xFF0000, // Red
            0xFF8000, // Orange
            0xFFFF00, // Yellow
            0x00FF00, // Green
            0x00FFFF, // Cyan
            0x0080FF, // Light Blue
            0x0000FF, // Blue
            0x8000FF, // Purple
            0xFF00FF, // Magenta
            0xFF0080, // Pink
            0xFFFFFF, // White
            0x808080  // Gray
    };

    private int selectedColor;
    private boolean showColorPalette = false;

    public ColorPickerWidget(int x, int y, int width, int height, Text message, int initialColor) {
        super(x, y, width, height, message, button -> {
            ColorPickerWidget picker = (ColorPickerWidget) button;
            picker.showColorPalette = !picker.showColorPalette;
        }, DEFAULT_NARRATION_SUPPLIER);

        this.selectedColor = initialColor;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw button background
        int bgColor = isHovered() ? 0xFF404040 : 0xFF303030;
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

        // Draw color preview
        context.fill(getX() + 5, getY() + 5, getX() + 15, getY() + getHeight() - 5, selectedColor | 0xFF000000);

        // Draw color hex code
        String hexColor = String.format("#%06X", selectedColor);
        context.drawTextWithShadow(textRenderer, Text.literal(hexColor), getX() + 25, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);

        // Draw color palette if open
        if (showColorPalette) {
            int paletteX = getX();
            int paletteY = getY() + getHeight() + 5;
            int colorSize = 20;
            int colorMargin = 2;
            int colorsPerRow = 6;

            // Draw palette background
            int paletteWidth = (colorSize + colorMargin) * colorsPerRow + colorMargin;
            int paletteHeight = (colorSize + colorMargin) * ((PRESET_COLORS.length + colorsPerRow - 1) / colorsPerRow) + colorMargin;
            context.fill(paletteX, paletteY, paletteX + paletteWidth, paletteY + paletteHeight, 0xE0000000);

            // Draw color squares
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int row = i / colorsPerRow;
                int col = i % colorsPerRow;
                int x = paletteX + colorMargin + col * (colorSize + colorMargin);
                int y = paletteY + colorMargin + row * (colorSize + colorMargin);

                // Draw color
                context.fill(x, y, x + colorSize, y + colorSize, PRESET_COLORS[i] | 0xFF000000);

                // Highlight selected color
                if (PRESET_COLORS[i] == selectedColor) {
                    context.drawBorder(x - 1, y - 1, colorSize + 2, colorSize + 2, 0xFFFFFFFF);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Handle color palette clicks
        if (showColorPalette) {
            int paletteX = getX();
            int paletteY = getY() + getHeight() + 5;
            int colorSize = 20;
            int colorMargin = 2;
            int colorsPerRow = 6;

            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int row = i / colorsPerRow;
                int col = i % colorsPerRow;
                int x = paletteX + colorMargin + col * (colorSize + colorMargin);
                int y = paletteY + colorMargin + row * (colorSize + colorMargin);

                if (mouseX >= x && mouseX <= x + colorSize && mouseY >= y && mouseY <= y + colorSize) {
                    selectedColor = PRESET_COLORS[i];
                    showColorPalette = false;
                    return true;
                }
            }
        }

        return false;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(int color) {
        this.selectedColor = color;
    }
}