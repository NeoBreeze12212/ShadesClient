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

        // Register tick event for keybindings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (lockCurrentSlotKey.wasPressed()) {
                toggleCurrentSlotLock();
            }
        });
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
                client.player.sendMessage(
                        Text.literal("§aUnlocked slot " + (currentSlot + 1) + ": " + itemName),
                        true
                );
            } else {
                lockedSlots.add(currentSlot);
                client.player.sendMessage(
                        Text.literal("§cLocked slot " + (currentSlot + 1) + ": " + itemName),
                        true
                );
            }

            ShadesClient.LOGGER.info("Toggled lock for slot {}: {} ({})",
                    currentSlot, lockedSlots.contains(currentSlot) ? "locked" : "unlocked", itemName);
        }
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
        if (!showLockIndicator) return;

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

                // Draw a red overlay on locked slots
                context.fill(x, y, x + slotWidth, y + 18, indicatorColor);
            }
        }
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