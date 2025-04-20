package org.neo.shadesclient.modules.configs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.FishingNotifierModule;

public class FishingNotifierConfig extends Screen {
    private final FishingNotifierModule module;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 150;
    private static final int SLIDER_WIDTH = 150;

    public FishingNotifierConfig(FishingNotifierModule module) {
        super(Text.literal("Fishing Notifier Settings"));
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int startY = height / 4 - 30; // Start higher to fit more buttons
        int spacing = 25;
        int currentY = startY;

        // Enable/Disable module button
        ButtonWidget toggleButton = ButtonWidget.builder(
                        Text.literal(module.isEnabled() ? "§aEnabled" : "§cDisabled"),
                        button -> {
                            module.toggle();
                            button.setMessage(Text.literal(module.isEnabled() ? "§aEnabled" : "§cDisabled"));
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(toggleButton);
        currentY += spacing;

        // Notification Type - Simple Button approach instead of CyclingButtonWidget
        ButtonWidget notificationTypeButton = ButtonWidget.builder(
                        Text.literal("Notification: " + module.getNotificationType()),
                        button -> {
                            // Cycle through notification types
                            FishingNotifierModule.NotificationType[] types = FishingNotifierModule.NotificationType.values();
                            int currentIndex = 0;
                            for (int i = 0; i < types.length; i++) {
                                if (types[i] == module.getNotificationType()) {
                                    currentIndex = i;
                                    break;
                                }
                            }
                            int nextIndex = (currentIndex + 1) % types.length;
                            module.setNotificationType(types[nextIndex]);
                            button.setMessage(Text.literal("Notification: " + types[nextIndex]));
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(notificationTypeButton);
        currentY += spacing;

        // Sound toggle
        ButtonWidget soundToggleButton = ButtonWidget.builder(
                        Text.literal("Sound: " + (module.isSoundEnabled() ? "§aON" : "§cOFF")),
                        button -> {
                            module.setPlaySound(!module.isSoundEnabled());
                            button.setMessage(Text.literal("Sound: " + (module.isSoundEnabled() ? "§aON" : "§cOFF")));
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(soundToggleButton);
        currentY += spacing;

        // Sound volume slider
        SliderWidget volumeSlider = new VolumeSliderWidget(
                centerX - SLIDER_WIDTH / 2,
                currentY,
                SLIDER_WIDTH,
                BUTTON_HEIGHT,
                Text.literal("Volume: " + Math.round(module.getSoundVolume() * 100) + "%"),
                module.getSoundVolume());
        addDrawableChild(volumeSlider);
        currentY += spacing;

        // Always show GUI when holding rod toggle
        ButtonWidget alwaysShowGuiButton = ButtonWidget.builder(
                        Text.literal("Show GUI with Rod: " + (module.isAlwaysShowGuiWithRod() ? "§aON" : "§cOFF")),
                        button -> {
                            module.setAlwaysShowGuiWithRod(!module.isAlwaysShowGuiWithRod());
                            button.setMessage(Text.literal("Show GUI with Rod: " + (module.isAlwaysShowGuiWithRod() ? "§aON" : "§cOFF")));
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(alwaysShowGuiButton);
        currentY += spacing;

        // Reset statistics button
        ButtonWidget resetStatsButton = ButtonWidget.builder(
                        Text.literal("§cReset Fish Count"),
                        button -> {
                            module.resetFishCaught();
                            ShadesClient.LOGGER.info("Fish count reset");
                        })
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(resetStatsButton);
        currentY += spacing;

        // Custom GUI settings header
        Text guiSettingsText = Text.literal("§e§lGUI Position Settings");
        int textWidth = textRenderer.getWidth(guiSettingsText);
        currentY += 5; // small spacing before the header

        // GUI Position X slider
        SliderWidget guiXSlider = new GuiPositionSliderWidget(
                centerX - SLIDER_WIDTH / 2,
                currentY,
                SLIDER_WIDTH,
                BUTTON_HEIGHT,
                Text.literal("GUI X: " + module.getGuiX()),
                module.getGuiX() / (float)MinecraftClient.getInstance().getWindow().getScaledWidth(),
                true);
        addDrawableChild(guiXSlider);
        currentY += spacing;

        // GUI Position Y slider
        SliderWidget guiYSlider = new GuiPositionSliderWidget(
                centerX - SLIDER_WIDTH / 2,
                currentY,
                SLIDER_WIDTH,
                BUTTON_HEIGHT,
                Text.literal("GUI Y: " + module.getGuiY()),
                module.getGuiY() / (float)MinecraftClient.getInstance().getWindow().getScaledHeight(),
                false);
        addDrawableChild(guiYSlider);
        currentY += spacing;

        // GUI display duration slider (seconds)
        SliderWidget durationSlider = new DurationSliderWidget(
                centerX - SLIDER_WIDTH / 2,
                currentY,
                SLIDER_WIDTH,
                BUTTON_HEIGHT,
                Text.literal("Display Duration: " + module.getGuiDisplayDuration() / 1000 + "s"),
                (module.getGuiDisplayDuration() - 1000) / 9000.0);  // Range 1-10 seconds
        addDrawableChild(durationSlider);
        currentY += spacing;

        // Done button
        ButtonWidget doneButton = ButtonWidget.builder(
                        Text.literal("Done"),
                        button -> this.close())
                .dimensions(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addDrawableChild(doneButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fixed renderBackground call with appropriate parameters
        this.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // Draw statistics
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Total Fish Caught: " + module.getFishCaught()),
                width / 2, 30, 0x00FF00);

        // Draw GUI Settings header
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§e§lGUI Position Settings"),
                width / 2, height / 4 + 155, 0xFFFFFF);

        // Preview of custom GUI notification if that type is selected
        if (module.getNotificationType() == FishingNotifierModule.NotificationType.CUSTOM_GUI) {
            module.renderGuiPreview(context, module.getGuiX(), module.getGuiY());

            // Show GUI position info
            String posInfo = "Current position: X=" + module.getGuiX() + ", Y=" + module.getGuiY();
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(posInfo),
                    width / 2, height - 30, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // Volume slider widget
    private class VolumeSliderWidget extends SliderWidget {
        public VolumeSliderWidget(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Volume: " + Math.round(value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            module.setSoundVolume((float) value);
        }
    }

    // GUI position slider
    private class GuiPositionSliderWidget extends SliderWidget {
        private final boolean isXAxis;

        public GuiPositionSliderWidget(int x, int y, int width, int height, Text text, double value, boolean isXAxis) {
            super(x, y, width, height, text, value);
            this.isXAxis = isXAxis;
        }

        @Override
        protected void updateMessage() {
            MinecraftClient client = MinecraftClient.getInstance();
            int pos;
            if (isXAxis) {
                pos = (int)(value * client.getWindow().getScaledWidth());
                setMessage(Text.literal("GUI X: " + pos));
            } else {
                pos = (int)(value * client.getWindow().getScaledHeight());
                setMessage(Text.literal("GUI Y: " + pos));
            }
        }

        @Override
        protected void applyValue() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (isXAxis) {
                module.setGuiX((int)(value * client.getWindow().getScaledWidth()));
            } else {
                module.setGuiY((int)(value * client.getWindow().getScaledHeight()));
            }
        }
    }

    // Display duration slider
    private class DurationSliderWidget extends SliderWidget {
        public DurationSliderWidget(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
        }

        @Override
        protected void updateMessage() {
            int durationMs = 1000 + (int)(value * 9000);  // 1-10 seconds
            setMessage(Text.literal("Display Duration: " + (durationMs / 1000) + "s"));
        }

        @Override
        protected void applyValue() {
            int durationMs = 1000 + (int)(value * 9000);  // 1-10 seconds
            module.setGuiDisplayDuration(durationMs);
        }
    }
}