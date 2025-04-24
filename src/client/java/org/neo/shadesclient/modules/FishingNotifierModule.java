package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;
import org.neo.shadesclient.qolitems.ModulePlacementScreen;

public class FishingNotifierModule extends Module {
    // Notification types - using the shared enum from ModuleConfigGUI
    private ModuleConfigGUI.NotificationType notificationType = ModuleConfigGUI.NotificationType.GUI;

    // Statistics tracking
    private int fishCaught = 0;
    private long fishingStartTime = 0;
    private long lastFishCaughtTime = 0;
    private boolean isFishingSessionActive = false;

    // Bobber status tracking
    public enum BobberStatus {
        NO_BOBBER("No bobber"),
        CASTING("Casting..."),
        IN_AIR("In air"),
        IN_WATER("In water"),
        FISH_BITING("Fish biting!"),
        FISH_CAUGHT("Fish caught!");

        private final String display;

        BobberStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    private BobberStatus currentBobberStatus = BobberStatus.NO_BOBBER;

    // Sound settings
    private boolean playSound = true;
    private float soundVolume = 1.0f;

    // Custom GUI settings
    // Update the existing guiX and guiY fields and add hasCustomPosition
    private int guiX = 10;
    private int guiY = 10;
    private boolean hasCustomPosition = false;
    
    // Add these methods if they don't already exist
    public void setGuiPosition(int x, int y) {
        this.guiX = x;
        this.guiY = y;
    }
    
    public boolean hasCustomPosition() {
        return hasCustomPosition;
    }
    
    public void setCustomPosition(boolean hasCustomPosition) {
        this.hasCustomPosition = hasCustomPosition;
    }
    
    public void openPlacementScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ModulePlacementScreen(client.currentScreen, getName(), this));
        ShadesClient.LOGGER.info("Opening placement screen for " + getName());
    }
    private long guiDisplayDuration = 3000; // Display time in milliseconds
    private long guiDisplayStartTime = 0;
    private boolean showGui = false;
    private boolean alwaysShowGuiWithRod = true; // New setting, on by default

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
        showGui = false;
        currentBobberStatus = BobberStatus.NO_BOBBER;
    }

    // New method to check if player is holding a fishing rod
    private boolean isHoldingFishingRod() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;

        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();

        return (mainHand != null && mainHand.getItem() instanceof FishingRodItem) ||
                (offHand != null && offHand.getItem() instanceof FishingRodItem);
    }

    // This method is called by the EventHandler on each tick
    public void checkFishingBobber() {
        if (!isEnabled()) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) return;

            // Check if player is holding a fishing rod - if so, always show GUI when that option is enabled
            boolean holdingRod = isHoldingFishingRod();
            if (holdingRod && alwaysShowGuiWithRod && notificationType == ModuleConfigGUI.NotificationType.GUI) {
                showGui = true;

                // Start fishing session if not already active
                if (!isFishingSessionActive) {
                    isFishingSessionActive = true;
                    fishingStartTime = System.currentTimeMillis();
                    ShadesClient.LOGGER.info("Fishing session started");
                }
            } else if (!holdingRod && isFishingSessionActive) {
                // End fishing session if no longer holding a rod
                isFishingSessionActive = false;
                ShadesClient.LOGGER.info("Fishing session ended. Fish caught: " + fishCaught);
            }

            FishingBobberEntity bobber = client.player.fishHook;

            // Update bobber status
            updateBobberStatus(bobber);

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

                    // Increment fish caught count and update last caught time
                    fishCaught++;
                    lastFishCaughtTime = System.currentTimeMillis();

                    notifyFishCaught(client);
                    lastNotificationTime = System.currentTimeMillis();
                }

                lastTickHooked = isHooked;
            }

            // Handle custom GUI visibility timer (only if not holding a rod with alwaysShowGuiWithRod)
            if (!holdingRod || !alwaysShowGuiWithRod) {
                if (showGui && System.currentTimeMillis() > guiDisplayStartTime + guiDisplayDuration) {
                    showGui = false;
                }
            }

        } catch (Exception e) {
            ShadesClient.LOGGER.error("Error in FishingNotifierModule: " + e.getMessage());
        }
    }

    private void updateBobberStatus(FishingBobberEntity bobber) {
        if (bobber == null) {
            currentBobberStatus = BobberStatus.NO_BOBBER;
            return;
        }

        // Just cast
        if (System.currentTimeMillis() - lastCastTime < 500) {
            currentBobberStatus = BobberStatus.CASTING;
            return;
        }

        // Determine if bobber is in water or air
        if (!bobber.isSubmergedInWater()) {
            currentBobberStatus = BobberStatus.IN_AIR;
            return;
        }

        // In water - check for bites
        if (lastTickHooked) {
            currentBobberStatus = BobberStatus.FISH_BITING;
        } else {
            currentBobberStatus = BobberStatus.IN_WATER;
        }

        // If we just caught a fish
        if (System.currentTimeMillis() - lastFishCaughtTime < 1500) {
            currentBobberStatus = BobberStatus.FISH_CAUGHT;
        }
    }

    private boolean detectBobberBite(FishingBobberEntity bobber) {
        // Focus only on vertical movement for bite detection
        double currentVelocityY = bobber.getVelocity().y;

        // Check for the characteristic bobber splash (sudden downward movement)
        if (currentVelocityY < SPLASH_VELOCITY_THRESHOLD) {
            consecutiveDownwardTicks++;
            ShadesClient.LOGGER.debug("Potential bite detected: vY=" + currentVelocityY +
                    ", consecutive=" + consecutiveDownwardTicks);
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
        switch (notificationType) {
            case ACTION_BAR:
                try {
                    client.player.sendMessage(Text.of("§aFish caught! Total: " + fishCaught), true);
                } catch (Exception e) {
                    ShadesClient.LOGGER.error("Could not send action bar notification: " + e.getMessage());
                }
                break;

            case TITLE:
                try {
                    client.inGameHud.setTitle(Text.of("§aFish caught!"));
                    client.inGameHud.setSubtitle(Text.of("§eTotal: " + fishCaught));
                    client.inGameHud.setTitleTicks(10, 30, 10); // fade in, stay, fade out
                } catch (Exception e) {
                    ShadesClient.LOGGER.error("Could not send title notification: " + e.getMessage());
                }
                break;

            case GUI:
            default:
                showGui = true;
                guiDisplayStartTime = System.currentTimeMillis();
                break;
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

    // Method to render the fishing GUI information
    public int renderFishingInfo(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Module title with fishing rod icon
        String titleText = "🎣 Fishing Notifier";
        context.drawText(client.textRenderer, titleText, x + 8, y, 0xFFFFFF55, true);

        // Fish count display
        String fishText = "Fish caught: " + fishCaught;
        context.drawText(client.textRenderer, fishText, x + 10, y + 15, 0xFFFFFFFF, true);

        // Bobber status with color coding
        int statusColor;
        if (currentBobberStatus == BobberStatus.FISH_BITING) {
            statusColor = 0xFFFF5555; // Red for bite alert
            // Add a flashing effect for fish biting
            if ((System.currentTimeMillis() / 250) % 2 == 0) {
                statusColor = 0xFFFFFF55; // Flash between red and yellow
            }
        } else if (currentBobberStatus == BobberStatus.FISH_CAUGHT) {
            statusColor = 0xFF55FF55; // Green for caught
        } else if (currentBobberStatus == BobberStatus.IN_WATER) {
            statusColor = 0xFF55FFFF; // Cyan for in water
        } else if (currentBobberStatus == BobberStatus.IN_AIR || currentBobberStatus == BobberStatus.CASTING) {
            statusColor = 0xFFAAAAAA; // Gray for in air/casting
        } else {
            statusColor = 0xFFFFFFFF; // White default
        }

        context.drawText(client.textRenderer, "Status: " + currentBobberStatus.getDisplay(), x + 10, y + 30, statusColor, true);

        // Session time if active
        if (isFishingSessionActive) {
            long sessionDuration = (System.currentTimeMillis() - fishingStartTime) / 1000; // seconds
            long minutes = sessionDuration / 60;
            long seconds = sessionDuration % 60;
            String timeStr = String.format("Session: %d:%02d", minutes, seconds);
            context.drawText(client.textRenderer, timeStr, x + 10, y + 45, 0xFFFFFFFF, true);
        }

        return y + 60; // Return the new Y position (height of this module section)
    }

    // Getter and Setter methods for config
    public ModuleConfigGUI.NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(ModuleConfigGUI.NotificationType type) {
        this.notificationType = type;
    }

    public boolean isSoundEnabled() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public int getGuiX() {
        return guiX;
    }

    public void setGuiX(int x) {
        this.guiX = x;
    }

    public int getGuiY() {
        return guiY;
    }

    public void setGuiY(int y) {
        this.guiY = y;
    }

    public long getGuiDisplayDuration() {
        return guiDisplayDuration;
    }

    public void setGuiDisplayDuration(long duration) {
        this.guiDisplayDuration = duration;
    }

    public boolean isAlwaysShowGuiWithRod() {
        return alwaysShowGuiWithRod;
    }

    public void setAlwaysShowGuiWithRod(boolean alwaysShow) {
        this.alwaysShowGuiWithRod = alwaysShow;
    }

    public int getFishCaught() {
        return fishCaught;
    }

    public void resetFishCaught() {
        fishCaught = 0;
        fishingStartTime = System.currentTimeMillis();
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