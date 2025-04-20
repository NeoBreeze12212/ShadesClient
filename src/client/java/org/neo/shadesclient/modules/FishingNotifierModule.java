package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

public class FishingNotifierModule extends Module {
    private static final int NOTIFICATION_STYLE_CHAT = 0;
    private static final int NOTIFICATION_STYLE_TITLE = 1;
    private static final int NOTIFICATION_STYLE_BOTH = 2;

    private int notificationStyle = NOTIFICATION_STYLE_BOTH;
    private boolean playSound = true;
    private float soundVolume = 1.0f;

    // Bobber state tracking
    private boolean lastTickHooked = false;
    private double lastVelocityY = 0;
    private int consecutiveDownwardTicks = 0;
    private long lastCastTime = 0;
    private long lastNotificationTime = 0;
    private boolean bobberExistedLastTick = false;
    private int fishingState = 0; // 0 = waiting, 1 = cast, 2 = bobber in water steady
    private static final long CAST_IGNORE_DURATION = 1500; // Ignore 1.5 seconds after casting
    private static final long NOTIFICATION_COOLDOWN = 2000; // Prevent multiple notifications within 2 seconds

    // Movement thresholds for detection
    private static final double SPLASH_VELOCITY_THRESHOLD = -0.04; // More sensitive threshold for initial splash
    private static final double STEADY_VELOCITY_THRESHOLD = 0.01; // Threshold for considering bobber "steady"
    private static final int REQUIRED_SPLASH_TICKS = 2; // Number of ticks with movement to confirm bite

    public FishingNotifierModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        ShadesClient.LOGGER.info(getName() + " module enabled");
        resetTracking();
    }

    @Override
    protected void onDisable() {
        ShadesClient.LOGGER.info(getName() + " module disabled");
    }

    private void resetTracking() {
        lastTickHooked = false;
        consecutiveDownwardTicks = 0;
        lastVelocityY = 0;
        lastCastTime = 0;
        lastNotificationTime = 0;
        bobberExistedLastTick = false;
        fishingState = 0;
    }

    // This method is called by the EventHandler on each tick
    public void checkFishingBobber() {
        if (!isEnabled()) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) return;

            FishingBobberEntity bobber = client.player.fishHook;

            // Detect new cast or disappeared bobber
            if (bobber != null && !bobberExistedLastTick) {
                // New cast detected
                lastCastTime = System.currentTimeMillis();
                consecutiveDownwardTicks = 0;
                lastVelocityY = 0;
                fishingState = 1; // Set to cast state
                ShadesClient.LOGGER.debug("New fishing cast detected");
            } else if (bobber == null && bobberExistedLastTick) {
                // Bobber disappeared (retrieved or broke)
                resetTracking();
                ShadesClient.LOGGER.debug("Bobber disappeared");
            }

            // Track bobber existence
            bobberExistedLastTick = (bobber != null);

            if (bobber == null) {
                lastTickHooked = false;
                return;
            }

            // Skip detection if we're in the ignore period after casting
            if (fishingState == 1 && System.currentTimeMillis() - lastCastTime < CAST_IGNORE_DURATION) {
                return;
            } else if (fishingState == 1) {
                // Transition from cast to steady state after ignore duration
                fishingState = 2;
                ShadesClient.LOGGER.debug("Bobber settled in water");
            }

            // Only check for bites when in steady state
            if (fishingState == 2) {
                boolean isHooked = detectBobberBite(bobber);

                // Detect the moment a fish is caught
                if (isHooked && !lastTickHooked &&
                        System.currentTimeMillis() - lastNotificationTime > NOTIFICATION_COOLDOWN) {
                    ShadesClient.LOGGER.info("Fish detected! Sending notification.");
                    notifyFishCaught(client);
                    lastNotificationTime = System.currentTimeMillis();
                }

                lastTickHooked = isHooked;
            }
        } catch (Exception e) {
            ShadesClient.LOGGER.error("Error in FishingNotifierModule: " + e.getMessage());
        }
    }

    private boolean detectBobberBite(FishingBobberEntity bobber) {
        // Focus only on vertical movement for bite detection
        double currentVelocityY = bobber.getVelocity().y;
        boolean significantMovement = false;

        // Check for the characteristic bobber splash (sudden downward movement)
        if (currentVelocityY < SPLASH_VELOCITY_THRESHOLD) {
            consecutiveDownwardTicks++;
            ShadesClient.LOGGER.debug("Potential bite detected: vY=" + currentVelocityY +
                    ", consecutive=" + consecutiveDownwardTicks);
            significantMovement = true;
        } else if (Math.abs(currentVelocityY) < STEADY_VELOCITY_THRESHOLD) {
            // Bobber is relatively steady (small movement in either direction)
            // Don't reset counter immediately for small movements
        } else {
            // Reset for any other significant movement
            consecutiveDownwardTicks = 0;
        }

        // Store current velocity for next tick comparison
        lastVelocityY = currentVelocityY;

        // Require multiple consecutive ticks with significant downward movement to confirm bite
        return consecutiveDownwardTicks >= REQUIRED_SPLASH_TICKS;
    }

    private void notifyFishCaught(MinecraftClient client) {
        if (client.player == null) return;

        // Send notification based on style setting
        if (notificationStyle == NOTIFICATION_STYLE_CHAT || notificationStyle == NOTIFICATION_STYLE_BOTH) {
            try {
                client.player.sendMessage(Text.literal("§a[Fishing Notifier] §fA fish has been caught!"), true);
            } catch (Exception e) {
                ShadesClient.LOGGER.error("Could not send chat notification: " + e.getMessage());
            }
        }

        if (notificationStyle == NOTIFICATION_STYLE_TITLE || notificationStyle == NOTIFICATION_STYLE_BOTH) {
            try {
                // Using sendMessage with a different boolean flag to display as action bar
                client.player.sendMessage(Text.literal("§aFish caught!"), false);
            } catch (Exception e) {
                ShadesClient.LOGGER.error("Could not send title notification: " + e.getMessage());
            }
        }

        // Play sound if enabled
        if (playSound) {
            try {
                client.getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, soundVolume)
                );
            } catch (Exception e) {
                ShadesClient.LOGGER.error("Could not play notification sound: " + e.getMessage());
            }
        }

        ShadesClient.LOGGER.info("Fish caught notification sent");
    }

    public void setNotificationStyle(int style) {
        if (style >= NOTIFICATION_STYLE_CHAT && style <= NOTIFICATION_STYLE_BOTH) {
            this.notificationStyle = style;
        }
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        // Would implement configuration screen for notification settings
        ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}