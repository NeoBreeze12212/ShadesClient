package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

public class HungerWarningModule extends Module {
    private static final int DEFAULT_LOW_HUNGER_THRESHOLD = 6; // 3 hunger shanks
    private int lowHungerThreshold = DEFAULT_LOW_HUNGER_THRESHOLD;
    private long lastWarningTime = 0;
    private static final long WARNING_COOLDOWN = 30000; // 30 seconds

    public HungerWarningModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module enabled");
    }

    @Override
    protected void onDisable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module disabled");
    }

    // This method should be called from a tick event handler
    public void checkHunger() {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int foodLevel = client.player.getHungerManager().getFoodLevel();
        long currentTime = System.currentTimeMillis();

        // Check if hunger is below threshold and we haven't warned recently
        if (foodLevel <= lowHungerThreshold && (currentTime - lastWarningTime > WARNING_COOLDOWN)) {
            // Send warning message
            client.player.sendMessage(Text.literal("§e[Hunger Warning] §fYour hunger is low! (" + foodLevel + "/20)"), true);

            // Play warning sound
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f));

            lastWarningTime = currentTime;
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Sent low hunger warning. Hunger: " + foodLevel);
        }
    }

    public void setLowHungerThreshold(int threshold) {
        this.lowHungerThreshold = Math.max(0, Math.min(20, threshold));
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Set low hunger threshold to " + lowHungerThreshold);
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        // Would implement configuration screen for hunger threshold
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}