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

public class TorchReminderModule extends Module {
    private int lightLevelThreshold = 7;
    private long notificationCooldown = 5000; // 5 seconds in ms
    private long lastNotificationTime = 0;
    private ModuleConfigGUI.NotificationType notificationType = ModuleConfigGUI.NotificationType.GUI;
    private boolean playSound = true;

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
                // GUI display is handled by renderLightLevelOverlay method
                client.player.sendMessage(message, false);
                break;
        }

        if (playSound) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        }

        ShadesClient.LOGGER.info("Sent light level warning. Current level: " + lightLevel);
    }

    // This method would be called during HUD rendering
    public void renderLightLevelOverlay(DrawContext context) {
        if (!isEnabled() || notificationType != ModuleConfigGUI.NotificationType.GUI) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        BlockPos playerPos = client.player.getBlockPos();
        int blockLight = client.world.getLightLevel(LightType.BLOCK, playerPos);
        int skyLight = client.world.getLightLevel(LightType.SKY, playerPos);
        int effectiveLight = Math.max(blockLight, skyLight - client.world.getAmbientDarkness());

        // Draw the module GUI background
        int width = 120;
        int height = 50;
        int x = 5;
        int y = 60; // Position below the tool durability module

        // Background
        context.fill(x, y, x + width, y + height, 0xAA000000);

        // Border
        context.fill(x, y, x + width, y + 1, 0xFF404040);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF404040);
        context.fill(x, y, x + 1, y + height, 0xFF404040);
        context.fill(x + width - 1, y, x + width, y + height, 0xFF404040);

        // Header
        String headerText = "— Torch Reminder —";
        int headerWidth = client.textRenderer.getWidth(headerText);
        context.drawText(client.textRenderer, headerText, x + (width - headerWidth) / 2, y + 5, 0xFF00FF00, true);

        // Separator
        context.fill(x + 5, y + 15, x + width - 5, y + 16, 0xFF404040);

        // Light level info
        int color;
        if (effectiveLight < lightLevelThreshold) {
            color = 0xFFFF0000; // Red
        } else if (effectiveLight < 10) {
            color = 0xFFFF7F00; // Orange
        } else {
            color = 0xFF00FF00; // Green
        }

        context.drawTextWithShadow(client.textRenderer, "Current Light: " + effectiveLight, x + 10, y + 20, color);
        context.drawTextWithShadow(client.textRenderer, "Block Light: " + blockLight, x + 10, y + 30, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, "Sky Light: " + skyLight, x + 10, y + 40, 0xFFFFFFFF);
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
}