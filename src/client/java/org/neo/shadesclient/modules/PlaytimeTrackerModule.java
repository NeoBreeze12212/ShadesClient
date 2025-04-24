package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;
import org.neo.shadesclient.qolitems.ModulePlacementScreen;

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
    private ModuleConfigGUI.NotificationType notificationType = ModuleConfigGUI.NotificationType.GUI;

    // Colors - matching Tool Durability module style
    private static final int HEADER_COLOR = 0xFF00FF00;     // Green text for headers
    private static final int VALUE_COLOR = 0xFFFFFFFF;      // White for values
    private static final int WARNING_COLOR = 0xFFFF7700;    // Orange for warnings
    private static final int BACKGROUND_COLOR = 0xA0000000; // Semi-transparent black background

    // Position variables - these will be used for custom positioning
    private int posX = 5;
    private int posY = 5;
    private boolean customPosition = false;
    // Add these variables for GUI positioning
    private int guiX = 5;
    private int guiY = 5;

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

    public String getCurrentTime() {
        return timeFormat.format(new Date());
    }

    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    public String formatDuration(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format("%02d:%02d:%02d",
                hours,
                minutes % 60,
                seconds % 60);
    }

    public boolean shouldShowBreakReminder() {
        if (!enableBreakReminders) return false;

        long timeSinceLastBreak = System.currentTimeMillis() - lastBreakNotification;
        return timeSinceLastBreak >= (long) breakReminderInterval * 60 * 1000;
    }

    public void showBreakReminder() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String message = "You have been playing for " + formatDuration(getSessionDuration()) + ". Time to take a break!";

            // Display based on notification type
            switch (notificationType) {
                case ACTION_BAR:
                    client.player.sendMessage(Text.of(message), true);
                    break;
                case TITLE:
                    client.inGameHud.setTitle(Text.of("Break Time!"));
                    client.inGameHud.setSubtitle(Text.of(message));
                    break;
                case GUI:
                default:
                    client.player.sendMessage(Text.of("§c[Playtime Tracker] §f" + message), true);
                    break;
            }

            // Play a sound to get the player's attention
            client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            // Update the last notification time
            lastBreakNotification = System.currentTimeMillis();
        }
    }

    public int renderPlaytimeInfo(DrawContext context, int x, int y, int width) {
        if (!isEnabled()) return y;

        // Use custom position if set
        if (customPosition) {
            x = posX;
            y = posY;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int startY = y;
        int padding = 5;
        int contentWidth = 150; // Fixed width for the box
        int contentHeight = calculateHeight(); // Dynamic height based on content

        // Draw background box
        context.fill(x, startY, x + contentWidth, startY + contentHeight, BACKGROUND_COLOR);

        // Draw module title centered and green - matching Tool Durability style
        String titleText = "— Playtime Tracker —";
        int titleWidth = client.textRenderer.getWidth(titleText);
        context.drawText(client.textRenderer, titleText, x + (contentWidth - titleWidth) / 2, startY + padding, HEADER_COLOR, false);
        y = startY + padding + 10;

        // Show current time if enabled - white text
        if (showCurrentTime) {
            String timeText = "Current Time: " + getCurrentTime();
            context.drawText(client.textRenderer, timeText, x + padding, y, VALUE_COLOR, false);
            y += 10;
        }

        // Show playtime if enabled - white text or warning color if session is long
        if (showPlaytime) {
            String playtimeText = "Session: " + formatDuration(getSessionDuration());

            int textColor = VALUE_COLOR;
            if (getSessionDuration() > (long) breakReminderInterval * 60 * 1000) {
                textColor = WARNING_COLOR; // Use orange warning color
            }

            context.drawText(client.textRenderer, playtimeText, x + padding, y, textColor, false);
            y += 10;
        }

        // Check if we should show a break reminder
        if (shouldShowBreakReminder()) {
            showBreakReminder();
        }

        // If using custom position, return the original y
        if (customPosition) {
            return startY;
        }
        
        return startY + contentHeight + 5; // Return position after box with a small gap
    }

    // Add methods for position management
    public void setPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
        this.guiX = x;  // Keep both position systems in sync
        this.guiY = y;  // Keep both position systems in sync
        this.customPosition = true;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setCustomPosition(boolean customPosition) {
        this.customPosition = customPosition;
    }

    public boolean hasCustomPosition() {
        return customPosition;
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        MinecraftClient.getInstance().setScreen(new ModuleConfigGUI(null, "Playtime Tracker", this));
    }

    // Add method to open placement screen
    public void openPlacementScreen() {
        MinecraftClient.getInstance().setScreen(new ModulePlacementScreen(null, "Playtime Tracker", this));
    }

    /**
     * Calculate the height needed for the box based on enabled content
     * @return height in pixels
     */
    public int calculateHeight() {
        int height = 20; // Base height with title
        if (showCurrentTime) height += 10;
        if (showPlaytime) height += 10;
        return height;
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

    public ModuleConfigGUI.NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(ModuleConfigGUI.NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        lastBreakNotification = sessionStartTime;
        ShadesClient.LOGGER.info("Playtime Tracker session reset");
    }

    public int getGuiX() {
        return guiX;
    }

    public void setGuiX(int guiX) {
        this.guiX = guiX;
    }

    public int getGuiY() {
        return guiY;
    }

    public void setGuiY(int guiY) {
        this.guiY = guiY;
    }
}