package org.neo.shadesclient.modules;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;
import net.minecraft.client.sound.PositionedSoundInstance;
import org.neo.shadesclient.qolitems.ModulePlacementScreen;

public class TorchReminderModule extends Module {
    // Module settings
    private int lightLevelThreshold = 7;
    private long notificationCooldown = 5000; // 5 seconds in ms
    private long lastNotificationTime = 0;
    private ModuleConfigGUI.NotificationType notificationType = ModuleConfigGUI.NotificationType.GUI;
    private boolean playSound = true;

    // GUI positioning fields
    private int guiX = 5;
    private int guiY = 60; // Default position below tool durability
    private boolean hasCustomPosition = false;

    // Colors for GUI
    private static final int BACKGROUND_COLOR = 0xAA000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF404040;     // Dark gray border
    private static final int HEADER_COLOR = 0xFF00FF00;     // Green text for headers
    private static final int TEXT_COLOR = 0xFFFFFFFF;       // White for values
    private static final int WARNING_COLOR = 0xFFFF0000;    // Red for warnings
    private static final int CAUTION_COLOR = 0xFFFF7F00;    // Orange for caution

    public TorchReminderModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        ShadesClient.LOGGER.info(getName() + " module enabled");
    }

    @Override
    protected void onDisable() {
        ShadesClient.LOGGER.info(getName() + " module disabled");
    }

    public void checkLightLevel() {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime < notificationCooldown) return;

        BlockPos playerPos = client.player.getBlockPos();
        int blockLight = client.world.getLightLevel(LightType.BLOCK, playerPos);
        int skyLight = client.world.getLightLevel(LightType.SKY, playerPos);
        int effectiveLight = Math.max(blockLight, skyLight - client.world.getAmbientDarkness());

        // Check if light level is below threshold and player is not in water/lava
        if (effectiveLight < lightLevelThreshold && !client.player.isSubmergedInWater() && !client.player.isInLava()) {
            sendWarning(client, effectiveLight);
            lastNotificationTime = currentTime;
        }
    }

    private void sendWarning(MinecraftClient client, int lightLevel) {
        // Create formatted message
        Text message = Text.literal("[Torch Reminder] ")
                .formatted(Formatting.RED)
                .append(Text.literal("Light level is ")
                        .formatted(Formatting.WHITE))
                .append(Text.literal(String.valueOf(lightLevel))
                        .formatted(Formatting.RED))
                .append(Text.literal(". Consider placing a torch!")
                        .formatted(Formatting.WHITE));

        // Send notification based on selected type
        switch (notificationType) {
            case ACTION_BAR:
                client.player.sendMessage(message, true);
                break;
            case TITLE:
                client.inGameHud.setTitle(Text.literal("Low Light Level!").formatted(Formatting.RED));
                client.inGameHud.setSubtitle(message);
                client.inGameHud.setTitleTicks(10, 40, 10);
                break;
            case GUI:
                // Don't send chat message for GUI mode - the GUI display is handled by renderTorchReminderInfo
                // Just log it for debugging
                ShadesClient.LOGGER.info("GUI notification: Light level warning. Current level: " + lightLevel);
                break;
        }

        if (playSound) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        }

        ShadesClient.LOGGER.info("Sent light level warning. Current level: " + lightLevel);
    }

    // Getters and setters
    public int getLightLevelThreshold() {
        return lightLevelThreshold;
    }

    public void setLightLevelThreshold(int threshold) {
        this.lightLevelThreshold = Math.max(0, Math.min(15, threshold));
    }

    public long getNotificationCooldown() {
        return notificationCooldown;
    }

    public void setNotificationCooldown(long cooldown) {
        this.notificationCooldown = Math.max(1000, cooldown); // Minimum 1 second
    }

    public ModuleConfigGUI.NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(ModuleConfigGUI.NotificationType type) {
        this.notificationType = type;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    // GUI position getters and setters
    public int getGuiX() {
        return guiX;
    }

    public int getGuiY() {
        return guiY;
    }

    public void setGuiPosition(int x, int y) {
        this.guiX = x;
        this.guiY = y;
        this.hasCustomPosition = true;
        ShadesClient.LOGGER.info("Set GUI position for " + getName() + " to X:" + x + ", Y:" + y);
    }

    public boolean hasCustomPosition() {
        return hasCustomPosition;
    }

    public void setCustomPosition(boolean hasCustomPosition) {
        this.hasCustomPosition = hasCustomPosition;
    }

    // Method for ModulePlacementScreen integration
    public void openPlacementScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ModulePlacementScreen(client.currentScreen, getName(), this));
        ShadesClient.LOGGER.info("Opening placement screen for " + getName());
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ModuleConfigGUI(client.currentScreen, getName(), this));
        ShadesClient.LOGGER.info("Opening config for " + getName());
    }

    // Add this method to render the torch reminder info
    public int renderTorchReminderInfo(DrawContext context, int x, int y, int width) {
        if (!isEnabled()) return y;
    
        // Use custom position if set
        if (hasCustomPosition) {
            x = guiX;
            y = guiY;
        }
    
        MinecraftClient client = MinecraftClient.getInstance();
        int startY = y;
        int padding = 5;
        
        // Draw background box
        int boxHeight = 25; // Height for title + light level info
        context.fill(x, y, x + width, y + boxHeight, BACKGROUND_COLOR);
        
        // Draw border
        drawBorder(context, x, y, width, boxHeight);
    
        // Module title centered and green
        String titleText = "— Torch Reminder —";
        int titleWidth = client.textRenderer.getWidth(titleText);
        context.drawText(client.textRenderer, titleText, x + (width - titleWidth) / 2, y + 2, HEADER_COLOR, false);
        y += 12;
    
        // Get light levels
        if (client.world != null && client.player != null) {
            int blockLight = client.world.getLightLevel(LightType.BLOCK, client.player.getBlockPos());
            int skyLight = client.world.getLightLevel(LightType.SKY, client.player.getBlockPos());
            int effectiveLight = Math.max(blockLight, skyLight - client.world.getAmbientDarkness());
    
            // Light level info with color coding
            int lightColor;
            if (effectiveLight < lightLevelThreshold) {
                lightColor = CAUTION_COLOR; // Orange for warning (matching the screenshot)
            } else {
                lightColor = TEXT_COLOR; // White for normal
            }
    
            String lightText = "Light Level: " + effectiveLight + " (Threshold: " + lightLevelThreshold + ")";
            context.drawText(client.textRenderer, lightText, x + padding, y, lightColor, false);
        }
    
        // If using custom position, return the original y
        if (hasCustomPosition) {
            return startY;
        }
        
        return startY + boxHeight; // Return position after box
    }
    
    /**
     * Draw a border around the module GUI
     */
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
}