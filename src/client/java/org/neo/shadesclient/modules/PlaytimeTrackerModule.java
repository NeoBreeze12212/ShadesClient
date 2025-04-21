package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.ModuleGUIManager;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PlaytimeTrackerModule extends Module {
    private long sessionStartTime;
    private long lastBreakNotification;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    // Configuration options
    private boolean showCurrentTime = true;
    private boolean showPlaytime = true;
    private boolean enableBreakReminders = true;
    private int breakReminderInterval = 20; // in minutes

    // Colors
    private static final int HEADER_COLOR = 0xFF00FF00;     // Green text for headers
    private static final int VALUE_COLOR = 0xFFFFFFFF;      // White for values
    private static final int WARNING_COLOR = 0xFFFF5555;    // Red for warnings

    public PlaytimeTrackerModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        sessionStartTime = System.currentTimeMillis();
        lastBreakNotification = sessionStartTime;
        ShadesClient.LOGGER.info("Playtime Tracker module enabled. Session started at: " + timeFormat.format(new Date()));
    }

    @Override
    protected void onDisable() {
        ShadesClient.LOGGER.info("Playtime Tracker module disabled. Session duration: " + formatDuration(getSessionDuration()));
    }

    /**
     * Get the current system time as a formatted string
     * @return Current time formatted as HH:mm:ss
     */
    public String getCurrentTime() {
        return timeFormat.format(new Date());
    }

    /**
     * Get the session duration in milliseconds
     * @return Duration of the current session
     */
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    /**
     * Format a duration from milliseconds to a readable string
     * @param duration Duration in milliseconds
     * @return Formatted string (HH:mm:ss)
     */
    public String formatDuration(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format("%02d:%02d:%02d",
                hours,
                minutes % 60,
                seconds % 60);
    }

    /**
     * Check if it's time to show a break reminder
     * @return true if a break reminder should be shown
     */
    public boolean shouldShowBreakReminder() {
        if (!enableBreakReminders) return false;

        long timeSinceLastBreak = System.currentTimeMillis() - lastBreakNotification;
        return timeSinceLastBreak >= breakReminderInterval * 60 * 1000;
    }

    /**
     * Show a break reminder to the player
     */
    public void showBreakReminder() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of("§c[Playtime Tracker] §fYou have been playing for " +
                    formatDuration(getSessionDuration()) + ". Time to take a break!"), true);

            // Play a sound to get the player's attention
            client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            // Update the last notification time
            lastBreakNotification = System.currentTimeMillis();
        }
    }

    /**
     * Render playtime information in the ModuleGUIManager's interface
     * @param context DrawContext for rendering
     * @param x X position
     * @param y Y position
     * @param width Width of the display
     * @return new Y position after rendering
     */
    public int renderPlaytimeInfo(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Module title centered and green
        String titleText = "— Playtime Tracker —";
        int titleWidth = client.textRenderer.getWidth(titleText);
        context.drawText(client.textRenderer, titleText, x + (width - titleWidth) / 2, y, HEADER_COLOR, false);
        y += 10;

        // Show current time if enabled
        if (showCurrentTime) {
            String timeText = "Current Time: " + getCurrentTime();
            context.drawText(client.textRenderer, timeText, x + 5, y, VALUE_COLOR, false);
            y += 10;
        }

        // Show playtime if enabled
        if (showPlaytime) {
            String playtimeText = "Session: " + formatDuration(getSessionDuration());
            context.drawText(client.textRenderer, playtimeText, x + 5, y, VALUE_COLOR, false);
            y += 10;
        }

        // Check if we should show a break reminder
        if (shouldShowBreakReminder()) {
            showBreakReminder();
        }

        return y;
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        MinecraftClient.getInstance().setScreen(new PlaytimeConfigScreen(null, this));
    }

    // Getters and setters for configuration
    public boolean isShowCurrentTime() {
        return showCurrentTime;
    }

    public void setShowCurrentTime(boolean showCurrentTime) {
        this.showCurrentTime = showCurrentTime;
    }

    public boolean isShowPlaytime() {
        return showPlaytime;
    }

    public void setShowPlaytime(boolean showPlaytime) {
        this.showPlaytime = showPlaytime;
    }

    public boolean isEnableBreakReminders() {
        return enableBreakReminders;
    }

    public void setEnableBreakReminders(boolean enableBreakReminders) {
        this.enableBreakReminders = enableBreakReminders;
    }

    public int getBreakReminderInterval() {
        return breakReminderInterval;
    }

    public void setBreakReminderInterval(int breakReminderInterval) {
        this.breakReminderInterval = breakReminderInterval;
    }

    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        lastBreakNotification = sessionStartTime;
        ShadesClient.LOGGER.info("Playtime Tracker session reset");
    }

    /**
     * Custom configuration screen for the Playtime Tracker module
     */
    public class PlaytimeConfigScreen extends Screen {
        private final Screen parent;
        private final PlaytimeTrackerModule module;

        // Layout properties
        private static final int BACKGROUND_COLOR = 0x90000000;
        private static final int HEADER_COLOR = 0xFF1A1A1A;
        private static final int TEXT_COLOR = 0xFFE0E0E0;

        public PlaytimeConfigScreen(Screen parent, PlaytimeTrackerModule module) {
            super(Text.of("Playtime Tracker Configuration"));
            this.parent = parent;
            this.module = module;
        }

        @Override
        protected void init() {
            super.init();

            int centerX = width / 2;
            int y = 80;
            int buttonWidth = 200;
            int buttonHeight = 20;
            int spacing = 30;

            // Show Current Time toggle
            this.addDrawableChild(ButtonWidget.builder(
                            Text.of("Show Current Time: " + (module.isShowCurrentTime() ? "ON" : "OFF")),
                            button -> {
                                module.setShowCurrentTime(!module.isShowCurrentTime());
                                button.setMessage(Text.of("Show Current Time: " + (module.isShowCurrentTime() ? "ON" : "OFF")));
                            })
                    .dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                    .build());
            y += spacing;

            // Show Playtime toggle
            this.addDrawableChild(ButtonWidget.builder(
                            Text.of("Show Playtime: " + (module.isShowPlaytime() ? "ON" : "OFF")),
                            button -> {
                                module.setShowPlaytime(!module.isShowPlaytime());
                                button.setMessage(Text.of("Show Playtime: " + (module.isShowPlaytime() ? "ON" : "OFF")));
                            })
                    .dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                    .build());
            y += spacing;

            // Break Reminders toggle
            this.addDrawableChild(ButtonWidget.builder(
                            Text.of("Break Reminders: " + (module.isEnableBreakReminders() ? "ON" : "OFF")),
                            button -> {
                                module.setEnableBreakReminders(!module.isEnableBreakReminders());
                                button.setMessage(Text.of("Break Reminders: " + (module.isEnableBreakReminders() ? "ON" : "OFF")));
                            })
                    .dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                    .build());
            y += spacing;

            // Break Reminder Interval slider
            final int currentInterval = module.getBreakReminderInterval();
            this.addDrawableChild(new SliderWidget(
                    centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                    Text.of("Reminder Interval: " + currentInterval + " min"),
                    currentInterval / 60.0f
            ) {
                @Override
                protected void updateMessage() {
                    int value = (int) (this.value * 60);
                    // Ensure minimum value of 1 minute
                    value = Math.max(1, value);
                    this.setMessage(Text.of("Reminder Interval: " + value + " min"));
                }

                @Override
                protected void applyValue() {
                    int value = (int) (this.value * 60);
                    // Ensure minimum value of 1 minute
                    value = Math.max(1, value);
                    module.setBreakReminderInterval(value);
                }
            });
            y += spacing;

            // Reset Session button
            this.addDrawableChild(ButtonWidget.builder(
                            Text.of("Reset Session Timer"),
                            button -> module.resetSession())
                    .dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                    .build());
            y += spacing;

            // Back button
            this.addDrawableChild(ButtonWidget.builder(
                            Text.of("Back"),
                            button -> MinecraftClient.getInstance().setScreen(parent))
                    .dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                    .build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context);

            // Draw title
            String title = "Playtime Tracker Configuration";
            int titleWidth = textRenderer.getWidth(title);
            context.drawText(textRenderer, title, (width - titleWidth) / 2, 40, TEXT_COLOR, false);

            super.render(context, mouseX, mouseY, delta);
        }

        public void renderBackground(DrawContext context) {
            context.fill(0, 0, width, height, BACKGROUND_COLOR);
            context.fill(0, 0, width, 60, HEADER_COLOR);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}