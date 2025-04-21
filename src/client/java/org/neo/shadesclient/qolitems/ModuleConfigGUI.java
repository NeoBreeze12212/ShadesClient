package org.neo.shadesclient.qolitems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import org.neo.shadesclient.modules.ToolDurabilityModule;
import org.neo.shadesclient.modules.TorchReminderModule;
import org.neo.shadesclient.modules.FishingNotifierModule;
import org.neo.shadesclient.modules.PlaytimeTrackerModule;

import java.util.ArrayList;
import java.util.List;

public class ModuleConfigGUI extends Screen {
    // Colors - using the same color scheme as ShadesClientScreen
    private static final int BACKGROUND_COLOR = 0x90000000; // Semi-transparent black
    private static final int HEADER_COLOR = 0xFF1A1A1A; // Dark header
    private static final int SELECTED_CATEGORY_COLOR = 0xFF3050CF; // Blue for selected category
    private static final int CATEGORY_HOVER_COLOR = 0xFF404040; // Hover color
    private static final int CATEGORY_COLOR = 0xFF303030; // Normal category color
    private static final int MODULE_COLOR = 0xFF262626; // Module background
    private static final int MODULE_HOVER_COLOR = 0xFF383838; // Module hover
    private static final int ENABLED_COLOR = 0xFF4080FF; // Blue for enabled items
    private static final int TEXT_COLOR = 0xFFE0E0E0; // Light gray for text
    private static final int BORDER_COLOR = 0xFF404040; // Border color

    private final Screen parent;
    private final String moduleName;
    private final Object module; // Type of module (PlaytimeTrackerModule, etc.)

    // Categories for settings
    private enum SettingCategory {
        MODULE_SETTINGS("Module Settings"),
        NOTIFICATION_SETTINGS("Notification Settings");

        private final String displayName;

        SettingCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private SettingCategory selectedCategory = SettingCategory.MODULE_SETTINGS;
    private final List<CategoryButton> categoryButtons = new ArrayList<>();
    private final List<SettingOption> settingOptions = new ArrayList<>();

    // Notification types
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

    // Layout properties
    private int leftPanelWidth = 180;
    private int contentStartX;
    private int contentWidth;

    // Animation variables
    private long openTime;
    private static final int ANIMATION_DURATION = 300; // ms

    // Setting option class
    private class SettingOption {
        private final String name;
        private final String description;
        private Object associatedWidget;
        private final SettingCategory category;

        public SettingOption(String name, String description, SettingCategory category) {
            this.name = name;
            this.description = description;
            this.category = category;
        }

        public void setAssociatedButton(Object widget) {
            this.associatedWidget = widget;
        }

        public Object getAssociatedWidget() {
            return associatedWidget;
        }

        public ButtonWidget getButtonWidget() {
            if (associatedWidget instanceof ButtonWidget) {
                return (ButtonWidget) associatedWidget;
            }
            return null;
        }
    }

    public ModuleConfigGUI(Screen parent, String moduleName, Object module) {
        super(Text.of(moduleName + " Configuration"));
        this.parent = parent;
        this.moduleName = moduleName;
        this.module = module;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        this.leftPanelWidth = 180;
        this.contentStartX = leftPanelWidth + 10;
        this.contentWidth = width - leftPanelWidth - 20;

        // Clear existing buttons
        categoryButtons.clear();
        settingOptions.clear();

        // Add category buttons on the left side
        int categoryY = 50;
        int buttonHeight = 30;

        for (SettingCategory category : SettingCategory.values()) {
            addCategoryButton(category, categoryY);
            categoryY += buttonHeight;
        }

        // Add "Back" button at bottom of left panel
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(10, height - 40, leftPanelWidth - 20, 30).build());

        // Add "Save" button at bottom right
        this.addDrawableChild(ButtonWidget.builder(Text.of("Save"), button -> {
            saveSettings();
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(width - 110, height - 40, 100, 30).build());

        // Initialize module settings
        initializeSettings();

        // Add module-specific settings based on the selected category
        refreshSettingsForCategory();
    }

    private void addCategoryButton(SettingCategory category, int y) {
        CategoryButton button = new CategoryButton(10, y, leftPanelWidth - 20, 30,
                Text.of(category.getDisplayName()),
                btn -> {
                    selectedCategory = category;
                    clearAndRebuildUI();
                }, category);
        categoryButtons.add(button);
        addDrawableChild(button);
    }

    // New method to completely clear and rebuild the UI
    private void clearAndRebuildUI() {
        // Save current state before clearing
        SettingCategory currentCategory = selectedCategory;

        // Clear everything
        clearChildren();

        // Restore state
        selectedCategory = currentCategory;

        // Re-initialize all UI elements
        init();
    }

    private void initializeSettings() {
        // Add module-specific settings
        if (module instanceof PlaytimeTrackerModule) {
            PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;

            // Add module settings for PlaytimeTrackerModule
            settingOptions.add(new SettingOption("Show Current Time",
                    "Display the current system time on screen",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Show Playtime",
                    "Display the current session playtime on screen",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Enable Break Reminders",
                    "Remind you to take breaks after extended playtime",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Break Reminder Interval",
                    "Time between break reminders (in minutes)",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Reset Session",
                    "Reset the current playtime session timer",
                    SettingCategory.MODULE_SETTINGS));
        } else if (module instanceof ToolDurabilityModule) {
            ToolDurabilityModule toolModule = (ToolDurabilityModule) module;

            // Add module settings
            settingOptions.add(new SettingOption("Warning Threshold",
                    "Sets the durability percentage at which warnings will appear",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Play Sound",
                    "Play a sound when tool durability is low",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Show Overlay",
                    "Show durability information on screen",
                    SettingCategory.MODULE_SETTINGS));
        } else if (module instanceof TorchReminderModule) {
            TorchReminderModule torchModule = (TorchReminderModule) module;

            // Add module settings
            settingOptions.add(new SettingOption("Light Level Threshold",
                    "Minimum light level before warning appears",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Cooldown",
                    "Time between notifications",
                    SettingCategory.MODULE_SETTINGS));
        } else if (module instanceof FishingNotifierModule) {
            FishingNotifierModule fishingModule = (FishingNotifierModule) module;

            // Add fishing module settings
            settingOptions.add(new SettingOption("Play Sound",
                    "Play a sound when fish is detected",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Sound Volume",
                    "Adjust the volume of notification sounds",
                    SettingCategory.MODULE_SETTINGS));

            settingOptions.add(new SettingOption("Always Show GUI With Rod",
                    "Show fishing GUI whenever holding a fishing rod",
                    SettingCategory.MODULE_SETTINGS));
        }

        // Add notification settings for all module types
        for (NotificationType type : NotificationType.values()) {
            settingOptions.add(new SettingOption(type.getName(),
                    "Use " + type.getName() + " for notifications",
                    SettingCategory.NOTIFICATION_SETTINGS));
        }
    }

    private void refreshSettingsForCategory() {
        // Remove all existing setting widgets
        for (SettingOption option : settingOptions) {
            if (option.getAssociatedWidget() != null) {
                if (option.getAssociatedWidget() instanceof ButtonWidget) {
                    remove((ButtonWidget) option.getAssociatedWidget());
                }
                option.setAssociatedButton(null);
            }
        }

        // Add widgets for current category
        int y = 80; // Starting position below header
        int settingHeight = 30;
        int settingGap = 40; // Provide space for descriptions

        for (SettingOption option : settingOptions) {
            if (option.category != selectedCategory) continue;

            if (module instanceof PlaytimeTrackerModule) {
                PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;

                if (selectedCategory == SettingCategory.MODULE_SETTINGS) {
                    if (option.name.equals("Show Current Time")) {
                        boolean isEnabled = playtimeModule.isShowCurrentTime();
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Show Current Time: " + (isEnabled ? "ON" : "OFF")),
                                        btn -> {
                                            playtimeModule.setShowCurrentTime(!playtimeModule.isShowCurrentTime());
                                            btn.setMessage(Text.of("Show Current Time: " + (playtimeModule.isShowCurrentTime() ? "ON" : "OFF")));
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    } else if (option.name.equals("Show Playtime")) {
                        boolean isEnabled = playtimeModule.isShowPlaytime();
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Show Playtime: " + (isEnabled ? "ON" : "OFF")),
                                        btn -> {
                                            playtimeModule.setShowPlaytime(!playtimeModule.isShowPlaytime());
                                            btn.setMessage(Text.of("Show Playtime: " + (playtimeModule.isShowPlaytime() ? "ON" : "OFF")));
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    } else if (option.name.equals("Enable Break Reminders")) {
                        boolean isEnabled = playtimeModule.isEnableBreakReminders();
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Enable Break Reminders: " + (isEnabled ? "ON" : "OFF")),
                                        btn -> {
                                            playtimeModule.setEnableBreakReminders(!playtimeModule.isEnableBreakReminders());
                                            btn.setMessage(Text.of("Enable Break Reminders: " + (playtimeModule.isEnableBreakReminders() ? "ON" : "OFF")));
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    } else if (option.name.equals("Break Reminder Interval")) {
                        final int currentValue = playtimeModule.getBreakReminderInterval();
                        SliderWidget slider = new SliderWidget(
                                contentStartX, y, contentWidth, settingHeight,
                                Text.of("Break Reminder Interval: " + currentValue + " min"),
                                currentValue / 60.0f
                        ) {
                            @Override
                            protected void updateMessage() {
                                int value = (int) (this.value * 60);
                                // Ensure minimum value is 5 minutes
                                value = Math.max(5, value);
                                this.setMessage(Text.of("Break Reminder Interval: " + value + " min"));
                            }

                            @Override
                            protected void applyValue() {
                                int value = (int) (this.value * 60);
                                // Ensure minimum value is 5 minutes
                                value = Math.max(5, value);
                                playtimeModule.setBreakReminderInterval(value);
                            }
                        };
                        addDrawableChild(slider);
                        option.setAssociatedButton(slider);
                    } else if (option.name.equals("Reset Session")) {
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Reset Session"),
                                        btn -> {
                                            playtimeModule.resetSession();
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    }
                }
            } else if (module instanceof ToolDurabilityModule) {
                ToolDurabilityModule toolModule = (ToolDurabilityModule) module;

                if (selectedCategory == SettingCategory.MODULE_SETTINGS) {
                    if (option.name.equals("Warning Threshold")) {
                        // Create custom slider with fixed text
                        final int currentValue = toolModule.getWarningThreshold();
                        SliderWidget slider = new SliderWidget(
                                contentStartX, y, contentWidth, settingHeight,
                                Text.of("Warning Threshold: " + currentValue + "%"),
                                currentValue / 100.0f
                        ) {
                            @Override
                            protected void updateMessage() {
                                int value = (int) (this.value * 100);
                                this.setMessage(Text.of("Warning Threshold: " + value + "%"));
                            }

                            @Override
                            protected void applyValue() {
                                int value = (int) (this.value * 100);
                                toolModule.setWarningThreshold(value);
                            }
                        };
                        addDrawableChild(slider);
                        option.setAssociatedButton(slider);
                    } else if (option.name.equals("Play Sound")) {
                        boolean isEnabled = toolModule.isPlaySound();
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Play Sound: " + (isEnabled ? "ON" : "OFF")),
                                        btn -> {
                                            toolModule.setPlaySound(!toolModule.isPlaySound());
                                            btn.setMessage(Text.of("Play Sound: " + (toolModule.isPlaySound() ? "ON" : "OFF")));
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    } else if (option.name.equals("Show Overlay")) {
                        boolean isEnabled = toolModule.isShowDurabilityOverlay();
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Show Overlay: " + (isEnabled ? "ON" : "OFF")),
                                        btn -> {
                                            toolModule.setShowDurabilityOverlay(!toolModule.isShowDurabilityOverlay());
                                            btn.setMessage(Text.of("Show Overlay: " + (toolModule.isShowDurabilityOverlay() ? "ON" : "OFF")));
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    }
                }
            } else if (module instanceof TorchReminderModule) {
                TorchReminderModule torchModule = (TorchReminderModule) module;

                if (selectedCategory == SettingCategory.MODULE_SETTINGS) {
                    if (option.name.equals("Light Level Threshold")) {
                        final int currentValue = torchModule.getLightLevelThreshold();
                        SliderWidget slider = new SliderWidget(
                                contentStartX, y, contentWidth, settingHeight,
                                Text.of("Light Level Threshold: " + currentValue),
                                currentValue / 15.0f
                        ) {
                            @Override
                            protected void updateMessage() {
                                int value = (int) (this.value * 15);
                                this.setMessage(Text.of("Light Level Threshold: " + value));
                            }

                            @Override
                            protected void applyValue() {
                                int value = (int) (this.value * 15);
                                torchModule.setLightLevelThreshold(value);
                            }
                        };
                        addDrawableChild(slider);
                        option.setAssociatedButton(slider);
                    } else if (option.name.equals("Cooldown")) {
                        final int currentValue = (int) torchModule.getNotificationCooldown();
                        SliderWidget slider = new SliderWidget(
                                contentStartX, y, contentWidth, settingHeight,
                                Text.of("Cooldown: " + (currentValue / 1000) + "s"),
                                currentValue / 10000.0f
                        ) {
                            @Override
                            protected void updateMessage() {
                                int value = (int) (this.value * 10000);
                                this.setMessage(Text.of("Cooldown: " + (value / 1000) + "s"));
                            }

                            @Override
                            protected void applyValue() {
                                int value = (int) (this.value * 10000);
                                torchModule.setNotificationCooldown(value);
                            }
                        };
                        addDrawableChild(slider);
                        option.setAssociatedButton(slider);
                    }
                }
            } else if (module instanceof FishingNotifierModule) {
                FishingNotifierModule fishingModule = (FishingNotifierModule) module;

                if (selectedCategory == SettingCategory.MODULE_SETTINGS) {
                    if (option.name.equals("Play Sound")) {
                        boolean isEnabled = fishingModule.isSoundEnabled();
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Play Sound: " + (isEnabled ? "ON" : "OFF")),
                                        btn -> {
                                            fishingModule.setPlaySound(!fishingModule.isSoundEnabled());
                                            btn.setMessage(Text.of("Play Sound: " + (fishingModule.isSoundEnabled() ? "ON" : "OFF")));
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    } else if (option.name.equals("Sound Volume")) {
                        final float currentValue = fishingModule.getSoundVolume();
                        SliderWidget slider = new SliderWidget(
                                contentStartX, y, contentWidth, settingHeight,
                                Text.of("Sound Volume: " + (int)(currentValue * 100) + "%"),
                                currentValue
                        ) {
                            @Override
                            protected void updateMessage() {
                                float value = (float) this.value;
                                this.setMessage(Text.of("Sound Volume: " + (int)(value * 100) + "%"));
                            }

                            @Override
                            protected void applyValue() {
                                fishingModule.setSoundVolume((float) this.value);
                            }
                        };
                        addDrawableChild(slider);
                        option.setAssociatedButton(slider);
                    } else if (option.name.equals("Always Show GUI With Rod")) {
                        boolean isEnabled = fishingModule.isAlwaysShowGuiWithRod();
                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of("Always Show GUI With Rod: " + (isEnabled ? "ON" : "OFF")),
                                        btn -> {
                                            fishingModule.setAlwaysShowGuiWithRod(!fishingModule.isAlwaysShowGuiWithRod());
                                            btn.setMessage(Text.of("Always Show GUI With Rod: " + (fishingModule.isAlwaysShowGuiWithRod() ? "ON" : "OFF")));
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();
                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    }
                }
            }

            // Create notification type buttons
            if (selectedCategory == SettingCategory.NOTIFICATION_SETTINGS) {
                for (NotificationType type : NotificationType.values()) {
                    if (option.name.equals(type.getName())) {
                        final NotificationType thisType = type;
                        NotificationType currentType = getCurrentNotificationType();
                        boolean isSelected = currentType == thisType;

                        ButtonWidget button = ButtonWidget.builder(
                                        Text.of((isSelected ? "● " : "○ ") + type.getName()),
                                        btn -> {
                                            // Update the module's notification type directly
                                            if (module instanceof ToolDurabilityModule) {
                                                ((ToolDurabilityModule) module).setNotificationType(thisType);
                                            } else if (module instanceof TorchReminderModule) {
                                                ((TorchReminderModule) module).setNotificationType(thisType);
                                            } else if (module instanceof FishingNotifierModule) {
                                                ((FishingNotifierModule) module).setNotificationType(thisType);
                                            } else if (module instanceof PlaytimeTrackerModule) {
                                                setNotificationTypeForPlaytimeTracker((PlaytimeTrackerModule) module, thisType);
                                            }

                                            // Rebuild UI to reflect the changes
                                            clearAndRebuildUI();
                                        })
                                .dimensions(contentStartX, y, contentWidth, settingHeight)
                                .build();

                        addDrawableChild(button);
                        option.setAssociatedButton(button);
                    }
                }
            }

            y += settingHeight + settingGap;
        }
    }

    // Helper method to get the current notification type from any module
    private NotificationType getCurrentNotificationType() {
        if (module instanceof ToolDurabilityModule) {
            return ((ToolDurabilityModule) module).getNotificationType();
        } else if (module instanceof TorchReminderModule) {
            return ((TorchReminderModule) module).getNotificationType();
        } else if (module instanceof FishingNotifierModule) {
            return ((FishingNotifierModule) module).getNotificationType();
        } else if (module instanceof PlaytimeTrackerModule) {
            // Assuming PlaytimeTrackerModule has a getNotificationType method
            // If not implemented yet, we'll add it separately
            return NotificationType.GUI; // Default if not yet implemented
        }
        return NotificationType.GUI; // Default
    }

    // Helper method to set notification type for PlaytimeTrackerModule
    // This would need to be implemented in the PlaytimeTrackerModule class
    private void setNotificationTypeForPlaytimeTracker(PlaytimeTrackerModule module, NotificationType type) {
        // This would call module.setNotificationType(type) once implemented
        // For now, we'll handle it as a placeholder
    }

    private void saveSettings() {
        // Since we're now applying settings directly when buttons are clicked,
        // this method is mostly for handling any final cleanup or validation

        // Nothing needed here currently, as all settings are applied immediately
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the background
        renderBackgroundTexture(context);

        long timeSinceOpen = System.currentTimeMillis() - openTime;
        float animationProgress = Math.min(1.0f, (float) timeSinceOpen / ANIMATION_DURATION);

        if (animationProgress < 1.0f) {
            // Animated display
            int expandedWidth = (int) (width * animationProgress);
            int expandedHeight = (int) (height * animationProgress);
            int animatedLeft = (width - expandedWidth) / 2;
            int animatedTop = (height - expandedHeight) / 2;

            context.fill(animatedLeft, animatedTop, animatedLeft + expandedWidth, animatedTop + expandedHeight, BACKGROUND_COLOR);
        } else {
            // Full animation completed, draw the complete UI

            // Left panel - darker background
            context.fill(0, 0, leftPanelWidth, height, BACKGROUND_COLOR);

            // Right panel - semi-transparent
            context.fill(leftPanelWidth, 0, width, height, 0x80000000);

            // Top header
            context.fill(0, 0, width, 40, HEADER_COLOR);

            // Draw single title clearly centered
            String titleText = moduleName + " Configuration";
            int titleWidth = textRenderer.getWidth(titleText);
            context.drawText(textRenderer, titleText, (width - titleWidth) / 2, 15, TEXT_COLOR, false);

            // Draw option descriptions
            drawOptionDescriptions(context);

            // Draw version info
            context.drawText(textRenderer, "ShadesClient v1.0.0", 10, height - 15, 0xAAAAAA, true);
        }

        // Draw buttons last
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawOptionDescriptions(DrawContext context) {
        for (SettingOption option : settingOptions) {
            if (option.category == selectedCategory && option.getAssociatedWidget() != null) {
                if (option.getAssociatedWidget() instanceof ButtonWidget) {
                    ButtonWidget button = (ButtonWidget) option.getAssociatedWidget();
                    int descY = button.getY() + button.getHeight() + 5;

                    // Draw description with proper wrapping
                    List<OrderedText> lines = textRenderer.wrapLines(
                            Text.of(option.description), contentWidth);

                    for (int i = 0; i < lines.size(); i++) {
                        context.drawText(textRenderer, lines.get(i),
                                contentStartX, descY + (i * 10), 0xAAAAAA, false);
                    }
                }
            }
        }
    }

    private void renderBackgroundTexture(DrawContext context) {
        context.fill(0, 0, width, height, 0xC0101010); // Semi-transparent dark background
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    class CategoryButton extends ButtonWidget {
        private final SettingCategory category;

        public CategoryButton(int x, int y, int width, int height, Text message, PressAction onPress, SettingCategory category) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.category = category;
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int color = category == selectedCategory ? SELECTED_CATEGORY_COLOR :
                    isHovered() ? CATEGORY_HOVER_COLOR : CATEGORY_COLOR;

            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);

            // Text properly centered in button
            int textX = getX() + getWidth() / 2 - textRenderer.getWidth(getMessage()) / 2;
            int textY = getY() + (getHeight() - 8) / 2;

            context.drawText(textRenderer, getMessage(), textX, textY, TEXT_COLOR, false);
        }
    }
}