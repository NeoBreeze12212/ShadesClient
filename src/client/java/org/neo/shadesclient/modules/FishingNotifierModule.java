package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.configs.FishingNotifierConfig;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

public class FishingNotifierModule extends Module {
    // Notification types
    private NotificationType notificationType = NotificationType.ACTION_BAR;

    // Statistics tracking
    private int fishCaught = 0;
    private long fishingStartTime = 0;
    private long lastFishCaughtTime = 0;
    private boolean isFishingSessionActive = false;

    // Notification types enum
    public enum NotificationType {
        ACTION_BAR("Action Bar"),
        TITLE_SCREEN("Title Screen"),
        CUSTOM_GUI("Custom GUI"),
        CHAT_MESSAGE("Chat Message");

        private final String name;

        NotificationType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

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
    private int guiX = 10;
    private int guiY = 10;
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
            if (holdingRod && alwaysShowGuiWithRod && notificationType == NotificationType.CUSTOM_GUI) {
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
                    client.player.sendMessage(Text.literal("§aFish caught! Total: " + fishCaught), false);
                } catch (Exception e) {
                    ShadesClient.LOGGER.error("Could not send action bar notification: " + e.getMessage());
                }
                break;

            case TITLE_SCREEN:
                try {
                    client.inGameHud.setTitle(Text.literal("§aFish caught!"));
                    client.inGameHud.setSubtitle(Text.literal("§eTotal: " + fishCaught));
                    client.inGameHud.setTitleTicks(10, 30, 10); // fade in, stay, fade out
                } catch (Exception e) {
                    ShadesClient.LOGGER.error("Could not send title notification: " + e.getMessage());
                }
                break;

            case CUSTOM_GUI:
                showGui = true;
                guiDisplayStartTime = System.currentTimeMillis();
                break;

            case CHAT_MESSAGE:
                try {
                    client.player.sendMessage(
                            Text.literal("§a[Fishing Notifier] §fA fish has been caught! Total: " + fishCaught),
                            true
                    );
                } catch (Exception e) {
                    ShadesClient.LOGGER.error("Could not send chat notification: " + e.getMessage());
                }
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

    // Render the custom GUI notification
    public void renderNotification(DrawContext context) {
        if (!isEnabled() || !showGui || notificationType != NotificationType.CUSTOM_GUI) {
            return;
        }

        renderGuiPreview(context, guiX, guiY);
    }

    // Method to render the GUI (both for preview and actual notification)
    public void renderGuiPreview(DrawContext context, int x, int y) {
        // Background panel
        int panelWidth = 160;
        int panelHeight = 75; // Increased height for more info

        // Draw background
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xDD000000); // Semi-transparent black

        // Draw border (green)
        context.drawBorder(x, y, panelWidth, panelHeight, 0xFF00FF00); // Bright green border

        // Draw title
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                "§a[Fishing Notifier]", x + 5, y + 5, 0xFFFFFF);

        // Draw fishing stats
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                "§eTotal fish caught: §f" + fishCaught, x + 5, y + 20, 0xFFFFFF);

        // Draw bobber status
        String statusText = "§eBobber status: §f" + currentBobberStatus.getDisplay();
        if (currentBobberStatus == BobberStatus.FISH_BITING) {
            statusText = "§c§lFISH BITING! REEL IN NOW!";
        }
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                statusText, x + 5, y + 35, 0xFFFFFF);

        // Draw session time if active
        if (isFishingSessionActive) {
            long sessionDuration = (System.currentTimeMillis() - fishingStartTime) / 1000; // seconds
            long minutes = sessionDuration / 60;
            long seconds = sessionDuration % 60;
            String timeStr = String.format("§eSession time: §f%d:%02d", minutes, seconds);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    timeStr, x + 5, y + 50, 0xFFFFFF);
        }

        // If fish just caught, show notification message
        if (System.currentTimeMillis() - lastFishCaughtTime < 3000) {
            int alpha = (int)(255 * (1 - (System.currentTimeMillis() - lastFishCaughtTime) / 3000.0));

            // Pulsing text effect
            double pulse = Math.sin((System.currentTimeMillis() % 1000) / 500.0 * Math.PI) * 0.2 + 0.8;
            int pulseScale = (int)(pulse * 255);

            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    "§" + Integer.toHexString(pulseScale).charAt(0) + "§lFish caught!",
                    x + panelWidth/2 - MinecraftClient.getInstance().textRenderer.getWidth("Fish caught!")/2,
                    y + 65,
                    0xFFFFFF | (alpha << 24));
        }
    }

    // Create a HUD renderer method that can be called from the main render event
    public void onRenderHud(DrawContext context) {
        if (notificationType == NotificationType.CUSTOM_GUI &&
                (showGui || (alwaysShowGuiWithRod && isHoldingFishingRod()))) {
            renderNotification(context);
        }
    }

    // Getter and Setter methods for config
    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType type) {
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
        MinecraftClient.getInstance().setScreen(new FishingNotifierConfig(this));
    }
}