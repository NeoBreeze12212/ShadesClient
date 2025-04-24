package org.neo.shadesclient.qolitems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.neo.shadesclient.modules.FishingNotifierModule;
import org.neo.shadesclient.modules.PlaytimeTrackerModule;
import org.neo.shadesclient.modules.ToolDurabilityModule;
import org.neo.shadesclient.modules.TorchReminderModule;

public class ModulePlacementScreen extends Screen {
    // Colors - using the same color scheme as ShadesClientScreen
    private static final int HEADER_COLOR = 0xFF1A1A1A; // Dark header
    private static final int TEXT_COLOR = 0xFFE0E0E0; // Light gray for text
    private static final int PLACEMENT_OVERLAY_COLOR = 0x40FF8000; // Semi-transparent orange for placement mode
    private static final int SLIDER_BACKGROUND = 0xFF303030; // Background for sliders

    private final Screen parent;
    private final String moduleName;
    private final Object module; // Type of module (PlaytimeTrackerModule, etc.)

    // Position values
    private int posX = 5;
    private int posY = 5;

    // Animation variables
    private long openTime;
    private static final int ANIMATION_DURATION = 300; // ms

    // Sliders for X and Y position
    private PositionSlider xSlider;
    private PositionSlider ySlider;

    public ModulePlacementScreen(Screen parent, String moduleName, Object module) {
        super(Text.of(moduleName + " Placement"));
        this.parent = parent;
        this.moduleName = moduleName;
        this.module = module;
        this.openTime = System.currentTimeMillis();
        
        // Initialize position values from the module
        if (module instanceof PlaytimeTrackerModule) {
            PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;
            this.posX = playtimeModule.getPosX();
            this.posY = playtimeModule.getPosY();
        } else if (module instanceof TorchReminderModule) {
            TorchReminderModule torchModule = (TorchReminderModule) module;
            this.posX = torchModule.getGuiX();
            this.posY = torchModule.getGuiY();
        } else if (module instanceof FishingNotifierModule) {
            FishingNotifierModule fishingModule = (FishingNotifierModule) module;
            this.posX = fishingModule.getGuiX();
            this.posY = fishingModule.getGuiY();
        }
    }

    // Custom slider for position control
    private class PositionSlider extends SliderWidget {
        private final boolean isXAxis;
        private final int maxValue;

        public PositionSlider(int x, int y, int width, int height, String prefix, boolean isXAxis) {
            super(x, y, width, height, Text.of(prefix + ": " + (isXAxis ? posX : posY)), 
                  (isXAxis ? posX : posY) / (double)(isXAxis ? 
                      MinecraftClient.getInstance().getWindow().getScaledWidth() : 
                      MinecraftClient.getInstance().getWindow().getScaledHeight()));
            
            this.isXAxis = isXAxis;
            this.maxValue = isXAxis ? 
                MinecraftClient.getInstance().getWindow().getScaledWidth() - 150 : // Subtract module width
                MinecraftClient.getInstance().getWindow().getScaledHeight() - 50;  // Subtract module height
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.of((isXAxis ? "X: " : "Y: ") + (isXAxis ? posX : posY)));
        }

        @Override
        protected void applyValue() {
            int newValue = (int)(value * maxValue);
            if (isXAxis) {
                posX = newValue;
                if (module instanceof PlaytimeTrackerModule) {
                    ((PlaytimeTrackerModule) module).setPosition(posX, posY);
                }
            } else {
                posY = newValue;
                if (module instanceof PlaytimeTrackerModule) {
                    ((PlaytimeTrackerModule) module).setPosition(posX, posY);
                }
            }
            updatePreview();
        }
    }

    @Override
    protected void init() {
        super.init();

        // Initialize position values from the module
        if (module instanceof PlaytimeTrackerModule) {
            PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;
            this.posX = playtimeModule.getPosX();
        } else if (module instanceof TorchReminderModule) {
            TorchReminderModule torchModule = (TorchReminderModule) module;
            this.posX = torchModule.getGuiX();
            this.posY = torchModule.getGuiY();
        } else if (module instanceof FishingNotifierModule) {
            FishingNotifierModule fishingModule = (FishingNotifierModule) module;
            this.posX = fishingModule.getGuiX();
            this.posY = fishingModule.getGuiY();
        }

        // Add X position slider
        xSlider = new PositionSlider(
            width / 2 - 150, height / 2 - 30, 
            300, 20, "X", true
        );
        this.addDrawableChild(xSlider);

        // Add Y position slider
        ySlider = new PositionSlider(
            width / 2 - 150, height / 2, 
            300, 20, "Y", false
        );
        this.addDrawableChild(ySlider);

        // Add Save and Cancel buttons at the bottom
        this.addDrawableChild(ButtonWidget.builder(Text.of("Save Position"), button -> {
            savePlacementPosition();
            exitPlacementMode(true);
        }).dimensions(width / 2 - 105, height - 40, 100, 30).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            exitPlacementMode(false);
        }).dimensions(width / 2 + 5, height - 40, 100, 30).build());
    }

    private void updatePreview() {
        // Update the module position for preview
        if (module instanceof PlaytimeTrackerModule) {
            PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;
            playtimeModule.setPosition(posX, posY);
        } else if (module instanceof TorchReminderModule) {
            TorchReminderModule torchModule = (TorchReminderModule) module;
            torchModule.setGuiPosition(posX, posY);
        } else if (module instanceof FishingNotifierModule) {
            FishingNotifierModule fishingModule = (FishingNotifierModule) module;
            fishingModule.setGuiPosition(posX, posY);
        }
    }

    private void savePlacementPosition() {
        // Save the position to the module
        if (module instanceof PlaytimeTrackerModule) {
            PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;
            playtimeModule.setPosition(posX, posY);
            playtimeModule.setCustomPosition(true);
        } else if (module instanceof TorchReminderModule) {
            TorchReminderModule torchModule = (TorchReminderModule) module;
            torchModule.setGuiPosition(posX, posY);
            torchModule.setCustomPosition(true);
        } else if (module instanceof FishingNotifierModule) {
            FishingNotifierModule fishingModule = (FishingNotifierModule) module;
            fishingModule.setGuiPosition(posX, posY);
            fishingModule.setCustomPosition(true);
        }
    }

    private void exitPlacementMode(boolean save) {
        // Show confirmation message
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (save) {
                client.player.sendMessage(Text.of("§aPosition saved for " + moduleName), true);
            } else {
                // Reset position if canceled
                if (!save) {
                    if (module instanceof PlaytimeTrackerModule) {
                        PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;
                        // Only reset if we're canceling
                        if (playtimeModule.hasCustomPosition()) {
                            // Keep the original position
                            playtimeModule.setPosition(playtimeModule.getPosX(), playtimeModule.getPosY());
                        } else {
                            // Reset to default
                            playtimeModule.setCustomPosition(false);
                        }
                    } else if (module instanceof TorchReminderModule) {
                        TorchReminderModule torchModule = (TorchReminderModule) module;
                        if (torchModule.hasCustomPosition()) {
                            torchModule.setGuiPosition(torchModule.getGuiX(), torchModule.getGuiY());
                        } else {
                            torchModule.setCustomPosition(false);
                        }
                    } else if (module instanceof FishingNotifierModule) {
                        FishingNotifierModule fishingModule = (FishingNotifierModule) module;
                        if (fishingModule.hasCustomPosition()) {
                            fishingModule.setGuiPosition(fishingModule.getGuiX(), fishingModule.getGuiY());
                        } else {
                            fishingModule.setCustomPosition(false);
                        }
                    }
                }
                client.player.sendMessage(Text.of("§cPlacement canceled for " + moduleName), true);
            }
        }

        // Return to the parent screen
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the background texture
        renderBackgroundTexture(context);

        long timeSinceOpen = System.currentTimeMillis() - openTime;
        float animationProgress = Math.min(1.0f, (float) timeSinceOpen / ANIMATION_DURATION);

        if (animationProgress < 1.0f) {
            // Animated display
            int expandedWidth = (int) (width * animationProgress);
            int expandedHeight = (int) (height * animationProgress);
            int animatedLeft = (width - expandedWidth) / 2;
            int animatedTop = (height - expandedHeight) / 2;

            context.fill(animatedLeft, animatedTop, animatedLeft + expandedWidth, animatedTop + expandedHeight, PLACEMENT_OVERLAY_COLOR);
        } else {
            // Full animation completed, draw the complete UI
            renderPlacementModeUI(context);
        }

        // Draw buttons last
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPlacementModeUI(DrawContext context) {
        // Remove the full-screen overlay fill
        // context.fill(0, 0, width, height, PLACEMENT_OVERLAY_COLOR); <- Removed this line

        // Remove the dark header background
        // context.fill(0, 0, width, 40, HEADER_COLOR); <- Removed this line
        
        String titleText = "Placement Mode - " + moduleName;
        int titleWidth = textRenderer.getWidth(titleText);
        context.drawText(textRenderer, titleText, (width - titleWidth) / 2, 15, TEXT_COLOR, false);

        // Draw instructions
        String instructions = "Use the sliders to position the module";
        int instructionsWidth = textRenderer.getWidth(instructions);
        context.drawText(textRenderer, instructions, (width - instructionsWidth) / 2, 50, TEXT_COLOR, false);
        
        // Draw current position values
        String positionText = "Current Position: X=" + posX + ", Y=" + posY;
        int positionWidth = textRenderer.getWidth(positionText);
        context.drawText(textRenderer, positionText, (width - positionWidth) / 2, height / 2 + 30, TEXT_COLOR, false);
    }

    private void renderBackgroundTexture(DrawContext context) {
        context.fill(0, 0, width, height, 0xC0101010); // Semi-transparent dark background
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // If ESC is pressed in placement mode, treat it as Cancel
        if (keyCode == 256) { // 256 is ESC key
            exitPlacementMode(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}