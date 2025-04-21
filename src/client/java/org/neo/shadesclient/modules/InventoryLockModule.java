package org.neo.shadesclient.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;
import org.neo.shadesclient.qolitems.ModuleGUIManager;

import java.util.HashSet;
import java.util.Set;

public class InventoryLockModule extends Module {
    private static final int HOTBAR_SLOTS = 9;

    private final Set<Integer> lockedSlots = new HashSet<>();
    private KeyBinding lockCurrentSlotKey;

    // Configuration options
    private boolean showLockIndicator = true;
    private boolean preventDropping = true;
    private int indicatorColor = 0xAAFF0000; // Semi-transparent red

    // Status display timers
    private int lockStatusMessageTimer = 0;
    private int dropPreventedMessageTimer = 0;
    private String lockStatusMessage = "";
    private static final int MESSAGE_DISPLAY_TIME = 60; // 3 seconds at 20 ticks per second

    public InventoryLockModule(String name, String description, ModuleCategory category) {
        super(name, description, category);

        // Module is enabled by default
        setEnabled(true);

        // Register keybinding for locking/unlocking slots
        lockCurrentSlotKey = new KeyBinding(
                "key.shadesclient.lock_current_slot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.shadesclient.inventory_lock"
        );

        // Register tick event for keybindings and message timers
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (lockCurrentSlotKey.wasPressed()) {
                toggleCurrentSlotLock();
            }

            // Update timers
            if (lockStatusMessageTimer > 0) {
                lockStatusMessageTimer--;
            }

            if (dropPreventedMessageTimer > 0) {
                dropPreventedMessageTimer--;
            }

            // Update status message based on current selected slot
            if (client.player != null && isEnabled()) {
                int currentSlot = client.player.getInventory().selectedSlot;
                updateSlotStatusMessage(currentSlot);
            }
        });

        // Register this module with the GUI manager
        ModuleGUIManager.getInstance().registerModule(this);
    }

    /**
     * Initialize keybindings - called from ShadesClient main class
     */
    public KeyBinding[] getKeybindings() {
        return new KeyBinding[] { lockCurrentSlotKey };
    }

    /**
     * Toggle the lock state of the current selected slot
     */
    private void toggleCurrentSlotLock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            int currentSlot = client.player.getInventory().selectedSlot;
            ItemStack currentItem = client.player.getInventory().getStack(currentSlot);
            String itemName = currentItem.isEmpty() ? "Empty Slot" : currentItem.getName().getString();

            if (lockedSlots.contains(currentSlot)) {
                lockedSlots.remove(currentSlot);
                showActionBarMessage("§aUnlocked slot " + (currentSlot + 1) + ": " + itemName, 0xFF55FF55);
                lockStatusMessage = "Slot unlocked!";
            } else {
                lockedSlots.add(currentSlot);
                showActionBarMessage("§cLocked slot " + (currentSlot + 1) + ": " + itemName, 0xFFFF5555);
                lockStatusMessage = "Slot locked!";
            }

            // Reset the timer for the status message
            lockStatusMessageTimer = MESSAGE_DISPLAY_TIME;

            ShadesClient.LOGGER.info("Toggled lock for slot {}: {} ({})",
                    currentSlot, lockedSlots.contains(currentSlot) ? "locked" : "unlocked", itemName);
        }
    }

    /**
     * Show a message in the action bar (above hotbar)
     */
    public void showActionBarMessage(String message, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message).styled(style -> style.withColor(color)), true);
        }
    }

    /**
     * Update the status message based on the current slot
     */
    private void updateSlotStatusMessage(int slot) {
        if (isSlotLocked(slot)) {
            lockStatusMessage = "This slot is locked!";
        } else {
            lockStatusMessage = "This slot is unlocked!";
        }
    }

    /**
     * Called when a player attempts to drop an item
     * @return true if dropping should be prevented
     */
    public boolean onItemDropAttempt(int slot) {
        if (preventDropping && isSlotLocked(slot)) {
            // Show message and prevent dropping
            dropPreventedMessageTimer = MESSAGE_DISPLAY_TIME;
            showActionBarMessage("§cCannot drop locked item!", 0xFFFF5555);
            return true; // Prevent dropping
        }
        return false; // Allow dropping
    }

    /**
     * Check if a specific slot is locked
     */
    public boolean isSlotLocked(int slot) {
        return lockedSlots.contains(slot);
    }

    /**
     * Render locked slot indicators on the HUD
     */
    public void renderHUD(DrawContext context) {
        if (!showLockIndicator || !isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Get hotbar dimensions
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Hotbar starts at center - 91 and is 182 pixels wide
        int hotbarLeft = screenWidth / 2 - 91;
        int slotWidth = 20; // Each slot is roughly 20 pixels

        for (int slot = 0; slot < HOTBAR_SLOTS; slot++) {
            if (lockedSlots.contains(slot)) {
                int x = hotbarLeft + slot * slotWidth;
                int y = screenHeight - 19; // Just above the hotbar

                // Draw a red outline on locked slots instead of a full overlay
                context.fill(x, y, x + slotWidth, y + 1, indicatorColor); // Top
                context.fill(x, y + 17, x + slotWidth, y + 18, indicatorColor); // Bottom
                context.fill(x, y, x + 1, y + 18, indicatorColor); // Left
                context.fill(x + slotWidth - 1, y, x + slotWidth, y + 18, indicatorColor); // Right
            }
        }

        // Display status messages via action bar instead of on-screen text
        // (Done through showActionBarMessage method)
    }

    /**
     * Render module information in the ModuleGUIManager interface
     */
    public int renderInventoryLockInfo(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Module title with lock icon - centered and green
        String titleText = "— Inventory Lock —";
        int titleWidth = client.textRenderer.getWidth(titleText);
        context.drawText(client.textRenderer, titleText, x + (width - titleWidth) / 2, y, 0xFF00FF00, false);
        y += 10;

        // Current slot status with color coding
        int currentSlot = client.player.getInventory().selectedSlot;
        ItemStack currentItem = client.player.getInventory().getStack(currentSlot);
        String itemName = currentItem.isEmpty() ? "-" : currentItem.getName().getString();

        // Status line with color based on lock state (similar to the fishing rod in image)
        int textColor = isSlotLocked(currentSlot) ? 0xFFFF7700 : 0xFFAAAAAA; // Orange for locked, gray for unlocked
        String slotStatus = "Current Slot: " + itemName + (isSlotLocked(currentSlot) ? " (Locked)" : "");

        context.drawText(client.textRenderer, slotStatus, x + 5, y, textColor, false);
        y += 10;

        // Second line showing control info
        String keyBindInfo = "Press [" + lockCurrentSlotKey.getBoundKeyLocalizedText().getString() + "] to toggle lock";
        context.drawText(client.textRenderer, keyBindInfo, x + 5, y, 0xFFFFFFFF, false);
        y += 10;

        // Count of locked slots
        context.drawText(client.textRenderer, "Locked Slots: " + lockedSlots.size(), x + 5, y, 0xFFFFFFFF, false);

        // Return new Y position (height of this module's display)
        return y + 5;
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        MinecraftClient.getInstance().setScreen(
                new ModuleConfigGUI(null, "Inventory Lock", this)
        );
    }

    // Required abstract method implementation (likely from Module class)
    @Override
    public void onEnable() {
        ShadesClient.LOGGER.info("InventoryLockModule enabled");
    }

    @Override
    public void onDisable() {
        ShadesClient.LOGGER.info("InventoryLockModule disabled");
    }

    // Getters and setters for config options
    public boolean isShowLockIndicator() {
        return showLockIndicator;
    }

    public void setShowLockIndicator(boolean showLockIndicator) {
        this.showLockIndicator = showLockIndicator;
    }

    public boolean isPreventDropping() {
        return preventDropping;
    }

    public void setPreventDropping(boolean preventDropping) {
        this.preventDropping = preventDropping;
    }

    public int getIndicatorColor() {
        return indicatorColor;
    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
    }

    public void clearLockedSlots() {
        lockedSlots.clear();
        ShadesClient.LOGGER.info("Cleared all locked slots");
    }

    public Set<Integer> getLockedSlots() {
        return new HashSet<>(lockedSlots); // Return a copy to prevent direct modification
    }
}