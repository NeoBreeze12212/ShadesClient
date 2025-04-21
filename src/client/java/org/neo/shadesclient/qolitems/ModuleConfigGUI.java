package org.neo.shadesclient.qolitems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import org.neo.shadesclient.modules.ToolDurabilityModule;
import org.neo.shadesclient.modules.TorchReminderModule;

import java.util.ArrayList;
import java.util.List;

public class ModuleConfigGUI extends Screen {
    // Updated color scheme to match ShadesClientScreen
    private static final int BACKGROUND_COLOR = 0x90000000; // Semi-transparent black
    private static final int HEADER_COLOR = 0xFF1A1A1A; // Dark header
    private static final int SECTION_BACKGROUND = 0xFF262626; // Module background color
    private static final int SECTION_HOVER_COLOR = 0xFF383838; // Module hover color
    private static final int BORDER_COLOR = 0xFF404040; // Dark gray for borders
    private static final int ACTIVE_COLOR = 0xFF4080FF; // Blue for enabled/active items
    private static final int TEXT_COLOR = 0xFFE0E0E0; // Light gray for text
    private static final int TITLE_COLOR = 0xFFFFFFFF; // White for titles
    private static final int SCROLL_BUTTON_COLOR = 0xFF404040; // Dark gray for scroll buttons
    private static final int SCROLL_BUTTON_HOVER_COLOR = 0xFF606060; // Lighter gray for hover

    private final Screen parent;
    private final String moduleName;
    private final Object module; // Either ToolDurabilityModule or TorchReminderModule

    private final List<NotificationOption> notificationOptions = new ArrayList<>();
    private NotificationType selectedNotificationType = NotificationType.GUI;

    // Updated GUI dimensions
    private int guiLeft;
    private int guiTop;
    private int guiWidth = 380; // Wider for better layout
    private int guiHeight = 240; // Taller for better spacing

    // Scroll variables
    private int scrollY = 0;
    private int maxScrollY = 0;
    private ButtonWidget upScrollButton;
    private ButtonWidget downScrollButton;

    // Animation variables
    private long openTime;
    private static final int ANIMATION_DURATION = 300; // ms

    public enum NotificationType {
        ACTION_BAR("Action Bar"),
        TITLE("Title Screen"),
        GUI("GUI");

        private final String name;

        NotificationType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private class NotificationOption {
        private final NotificationType type;
        private boolean enabled;

        public NotificationOption(NotificationType type, boolean enabled) {
            this.type = type;
            this.enabled = enabled;
        }
    }

    public ModuleConfigGUI(Screen parent, String moduleName, Object module) {
        super(Text.literal(moduleName + " Configuration"));
        this.parent = parent;
        this.moduleName = moduleName;
        this.module = module;
        this.openTime = System.currentTimeMillis();

        // Initialize default notification options
        for (NotificationType type : NotificationType.values()) {
            notificationOptions.add(new NotificationOption(type, type == NotificationType.GUI));
        }
    }

    @Override
    protected void init() {
        super.init();

        // Center the GUI
        this.guiLeft = (this.width - this.guiWidth) / 2;
        this.guiTop = (this.height - this.guiHeight) / 2;

        // Add "Done" button at bottom
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            saveSettings();
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(this.guiLeft + this.guiWidth - 120, this.guiTop + this.guiHeight - 35, 110, 25).build());

        // Add "Cancel" button at bottom
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(this.guiLeft + 10, this.guiTop + this.guiHeight - 35, 110, 25).build());

        // Add scroll buttons if needed
        upScrollButton = ButtonWidget.builder(Text.literal("▲"), button -> {
            scrollUp();
        }).dimensions(this.guiLeft + this.guiWidth - 30, this.guiTop + 50, 20, 20).build();

        downScrollButton = ButtonWidget.builder(Text.literal("▼"), button -> {
            scrollDown();
        }).dimensions(this.guiLeft + this.guiWidth - 30, this.guiTop + this.guiHeight - 60, 20, 20).build();

        addDrawableChild(upScrollButton);
        addDrawableChild(downScrollButton);

        // Add module-specific settings
        addModuleSettings();

        // Reset scroll position
        scrollY = 0;
        calculateMaxScroll();
        updateScrollButtonVisibility();
    }

    private void addModuleSettings() {
        int contentWidth = this.guiWidth - 50;
        int startY = this.guiTop + 50 - scrollY;
        int y = startY;

        // Left side - Module specific settings
        if (module instanceof ToolDurabilityModule) {
            ToolDurabilityModule toolModule = (ToolDurabilityModule) module;

            // Module settings section
            drawSettingsSection(y, "Module Settings");
            y += 30;

            // Add slider for warning threshold
            this.addDrawableChild(new SliderWidget(
                    this.guiLeft + 20, y, contentWidth - 40, 20,
                    Text.literal("Warning Threshold: " + toolModule.getWarningThreshold() + "%"),
                    toolModule.getWarningThreshold() / 100.0f
            ) {
                @Override
                protected void updateMessage() {
                    int value = (int) (this.value * 100);
                    this.setMessage(Text.literal("Warning Threshold: " + value + "%"));
                }

                @Override
                protected void applyValue() {
                    int value = (int) (this.value * 100);
                    toolModule.setWarningThreshold(value);
                }
            });

            y += 30;

            // Add toggle for sound
            this.addDrawableChild(
                    CyclingButtonWidget.<Boolean>builder(value -> Text.literal("Play Sound: " + (value ? "ON" : "OFF")))
                            .values(true, false)
                            .initially(toolModule.isPlaySound())
                            .build(this.guiLeft + 20, y, contentWidth - 40, 20, Text.literal("Play Sound"),
                                    (button, value) -> toolModule.setPlaySound(value))
            );

            y += 30;

            // Add toggle for overlay
            this.addDrawableChild(
                    CyclingButtonWidget.<Boolean>builder(value -> Text.literal("Show Overlay: " + (value ? "ON" : "OFF")))
                            .values(true, false)
                            .initially(toolModule.isShowDurabilityOverlay())
                            .build(this.guiLeft + 20, y, contentWidth - 40, 20, Text.literal("Show Overlay"),
                                    (button, value) -> toolModule.setShowDurabilityOverlay(value))
            );

        } else if (module instanceof TorchReminderModule) {
            TorchReminderModule torchModule = (TorchReminderModule) module;

            // Module settings section
            drawSettingsSection(y, "Module Settings");
            y += 30;

            // Add slider for light level threshold
            this.addDrawableChild(new SliderWidget(
                    this.guiLeft + 20, y, contentWidth - 40, 20,
                    Text.literal("Light Level Threshold: " + torchModule.getLightLevelThreshold()),
                    torchModule.getLightLevelThreshold() / 15.0f
            ) {
                @Override
                protected void updateMessage() {
                    int value = (int) (this.value * 15);
                    this.setMessage(Text.literal("Light Level Threshold: " + value));
                }

                @Override
                protected void applyValue() {
                    int value = (int) (this.value * 15);
                    torchModule.setLightLevelThreshold(value);
                }
            });

            y += 30;

            // Add slider for notification cooldown
            this.addDrawableChild(new SliderWidget(
                    this.guiLeft + 20, y, contentWidth - 40, 20,
                    Text.literal("Cooldown: " + (torchModule.getNotificationCooldown() / 1000) + "s"),
                    torchModule.getNotificationCooldown() / 10000.0f
            ) {
                @Override
                protected void updateMessage() {
                    int value = (int) (this.value * 10000);
                    this.setMessage(Text.literal("Cooldown: " + (value / 1000) + "s"));
                }

                @Override
                protected void applyValue() {
                    int value = (int) (this.value * 10000);
                    torchModule.setNotificationCooldown(value);
                }
            });
        }

        // All modules get notification settings
        y += 50;
        drawSettingsSection(y, "Notification Settings");
        y += 30;

        // Add notification type toggles
        for (int i = 0; i < notificationOptions.size(); i++) {
            NotificationOption option = notificationOptions.get(i);
            final int index = i;

            this.addDrawableChild(
                    CyclingButtonWidget.<Boolean>builder(value -> Text.literal(option.type.getName() + ": " + (value ? "ON" : "OFF")))
                            .values(true, false)
                            .initially(option.enabled)
                            .build(this.guiLeft + 20, y, contentWidth - 40, 20,
                                    Text.literal(option.type.getName()),
                                    (button, value) -> toggleNotificationOption(index, value))
            );

            y += 30;
        }

        // Update max scroll based on the content height
        maxScrollY = Math.max(0, y - (this.guiTop + this.guiHeight - 40));
    }

    private void drawSettingsSection(int y, String title) {
        // This method doesn't actually draw anything but helps with organizing the sections
        // The actual drawing happens in the render method
    }

    private void toggleNotificationOption(int index, boolean value) {
        // When enabling one option, disable others
        if (value) {
            for (int i = 0; i < notificationOptions.size(); i++) {
                notificationOptions.get(i).enabled = (i == index);
            }
            selectedNotificationType = notificationOptions.get(index).type;
        } else {
            // Don't allow all options to be disabled
            boolean anyEnabled = false;
            for (NotificationOption option : notificationOptions) {
                if (option.enabled && notificationOptions.indexOf(option) != index) {
                    anyEnabled = true;
                    break;
                }
            }

            if (!anyEnabled) {
                notificationOptions.get(index).enabled = true;
            }
        }

        // Refresh buttons
        this.clearAndInit();
    }

    private void scrollUp() {
        scrollY = Math.max(0, scrollY - 30);
        updateScrollButtonVisibility();
        clearAndInit();
    }

    private void scrollDown() {
        scrollY = Math.min(maxScrollY, scrollY + 30);
        updateScrollButtonVisibility();
        clearAndInit();
    }

    private void calculateMaxScroll() {
        // This will be calculated when adding module settings
    }

    private void updateScrollButtonVisibility() {
        upScrollButton.visible = scrollY > 0;
        downScrollButton.visible = scrollY < maxScrollY;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long timeSinceOpen = System.currentTimeMillis() - openTime;
        float animationProgress = Math.min(1.0f, (float) timeSinceOpen / ANIMATION_DURATION);

        // Render background with animation
        this.renderBackground(context, mouseX, mouseY, delta);

        // Draw module GUI background with animation
        int expandedWidth = (int) (this.guiWidth * animationProgress);
        int expandedHeight = (int) (this.guiHeight * animationProgress);
        int animatedLeft = this.guiLeft + (this.guiWidth - expandedWidth) / 2;
        int animatedTop = this.guiTop + (this.guiHeight - expandedHeight) / 2;

        if (animationProgress < 1.0f) {
            context.fill(animatedLeft, animatedTop, animatedLeft + expandedWidth, animatedTop + expandedHeight, BACKGROUND_COLOR);
        } else {
            // Full animation completed, draw the complete UI

            // Main background
            context.fill(this.guiLeft, this.guiTop, this.guiLeft + this.guiWidth, this.guiTop + this.guiHeight, BACKGROUND_COLOR);

            // Header background
            context.fill(this.guiLeft, this.guiTop, this.guiLeft + this.guiWidth, this.guiTop + 35, HEADER_COLOR);

            // Draw border around the entire GUI
            drawBorder(context, this.guiLeft, this.guiTop, this.guiWidth, this.guiHeight);

            // Draw header with gradient
            String headerText = this.moduleName + " Configuration";
            int headerWidth = this.textRenderer.getWidth(headerText);
            context.drawText(this.textRenderer, headerText, this.guiLeft + (this.guiWidth - headerWidth) / 2, this.guiTop + 13, TITLE_COLOR, true);

            // Draw header separator line
            context.fill(this.guiLeft, this.guiTop + 35, this.guiLeft + this.guiWidth, this.guiTop + 36, BORDER_COLOR);

            // Draw module settings sections
            renderSettingsSections(context);

            // Draw footer separator line
            context.fill(this.guiLeft, this.guiTop + this.guiHeight - 45, this.guiLeft + this.guiWidth, this.guiTop + this.guiHeight - 44, BORDER_COLOR);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSettingsSections(DrawContext context) {
        int contentWidth = this.guiWidth - 50;
        int startY = this.guiTop + 50;
        int y = startY;

        // Module Settings section
        context.fill(this.guiLeft + 10, startY - 10, this.guiLeft + this.guiWidth - 10, startY + 10, SECTION_BACKGROUND);
        String moduleSettingsText = "Module Settings";
        int moduleSettingsWidth = this.textRenderer.getWidth(moduleSettingsText);
        context.drawText(this.textRenderer, moduleSettingsText,
                this.guiLeft + (this.guiWidth - moduleSettingsWidth) / 2,
                startY - 5, TEXT_COLOR, true);

        // Skip past the module settings (this varies by module type)
        if (module instanceof ToolDurabilityModule) {
            y += 90; // Approx height for 3 settings
        } else if (module instanceof TorchReminderModule) {
            y += 60; // Approx height for 2 settings
        }

        // Notification Settings section
        context.fill(this.guiLeft + 10, y - 10, this.guiLeft + this.guiWidth - 10, y + 10, SECTION_BACKGROUND);
        String notificationSettingsText = "Notification Settings";
        int notificationSettingsWidth = this.textRenderer.getWidth(notificationSettingsText);
        context.drawText(this.textRenderer, notificationSettingsText,
                this.guiLeft + (this.guiWidth - notificationSettingsWidth) / 2,
                y - 5, TEXT_COLOR, true);
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height) {
        // Top border
        context.fill(x, y, x + width, y + 1, BORDER_COLOR);
        // Bottom border
        context.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        // Left border
        context.fill(x, y, x + 1, y + height, BORDER_COLOR);
        // Right border
        context.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
    }

    private void saveSettings() {
        if (module instanceof ToolDurabilityModule) {
            ToolDurabilityModule toolModule = (ToolDurabilityModule) module;
            toolModule.setNotificationType(selectedNotificationType);
            // Any additional settings are saved in real-time through the widgets
        } else if (module instanceof TorchReminderModule) {
            TorchReminderModule torchModule = (TorchReminderModule) module;
            torchModule.setNotificationType(selectedNotificationType);
            // Any additional settings are saved in real-time through the widgets
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // Helper method to draw multiline descriptions with proper wrapping
    private void drawMultilineText(DrawContext context, String text, int x, int y, int maxWidth, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(text), maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            context.drawText(textRenderer, lines.get(i), x, y + (i * 10), color, false);
        }
    }
}