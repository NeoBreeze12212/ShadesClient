package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

public class HealthWarningModule extends Module {
    private static final float DEFAULT_LOW_HEALTH_THRESHOLD = 6.0f; // 3 hearts
    private float lowHealthThreshold = DEFAULT_LOW_HEALTH_THRESHOLD;
    private boolean hasWarned = false;
    private long lastWarningTime = 0;
    private static final long WARNING_COOLDOWN = 5000; // 5 seconds

    public HealthWarningModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module enabled");
        hasWarned = false;
    }

    @Override
    protected void onDisable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module disabled");
    }

    // This method should be called from a tick event handler
    public void checkHealth() {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        float health = client.player.getHealth();
        long currentTime = System.currentTimeMillis();

        // Check if health is below threshold and we haven't warned recently
        if (health <= lowHealthThreshold && (currentTime - lastWarningTime > WARNING_COOLDOWN)) {
            // Send warning message
            client.player.sendMessage(Text.literal("§c[Health Warning] §fYour health is low! (" + health + "/" + client.player.getMaxHealth() + ")"), true);

            // Play warning sound
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));

            lastWarningTime = currentTime;
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Sent low health warning. Health: " + health);
        }
    }

    public void setLowHealthThreshold(float threshold) {
        this.lowHealthThreshold = Math.max(0.5f, threshold);
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Set low health threshold to " + lowHealthThreshold);
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        // Would implement configuration screen for health threshold
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}