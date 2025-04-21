package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

import java.util.HashSet;
import java.util.Set;

public class ToolDurabilityModule extends Module {
    private static final int DEFAULT_WARNING_THRESHOLD = 10; // Percentage of durability remaining
    private int warningThreshold = DEFAULT_WARNING_THRESHOLD;
    private boolean showDurabilityOverlay = true;
    private boolean playSound = true;
    private ModuleConfigGUI.NotificationType notificationType = ModuleConfigGUI.NotificationType.GUI;

    private final Set<Integer> warnedItems = new HashSet<>();

    public ToolDurabilityModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        ShadesClient.LOGGER.info(getName() + " module enabled");
        warnedItems.clear();
    }

    @Override
    protected void onDisable() {
        ShadesClient.LOGGER.info(getName() + " module disabled");
        warnedItems.clear();
    }

    // This method should be called each tick
    public void checkInventory() {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        PlayerInventory inventory = client.player.getInventory();

        // Check each item in the inventory
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (!stack.isEmpty() && stack.isDamageable()) {
                int maxDamage = stack.getMaxDamage();
                int damage = stack.getDamage();

                if (maxDamage > 0) {
                    int durabilityPercent = (int) (100 * (maxDamage - damage) / (float) maxDamage);

                    // Check if durability is below threshold and we haven't warned about this item yet
                    if (durabilityPercent <= warningThreshold && !warnedItems.contains(i)) {
                        sendWarning(client, stack, durabilityPercent);
                        warnedItems.add(i);
                    }

                    // If item was repaired, remove it from warned list
                    if (durabilityPercent > warningThreshold && warnedItems.contains(i)) {
                        warnedItems.remove(i);
                    }
                }
            }
        }
    }

    private void sendWarning(MinecraftClient client, ItemStack stack, int durabilityPercent) {
        String itemName = stack.getName().getString();

        // Create formatted message
        Text message = Text.literal("[Tool Durability] ")
                .formatted(Formatting.RED)
                .append(Text.literal(itemName + " is at ")
                        .formatted(Formatting.WHITE))
                .append(Text.literal(durabilityPercent + "%")
                        .formatted(Formatting.RED))
                .append(Text.literal(" durability!")
                        .formatted(Formatting.WHITE));

        // Send notification based on selected type
        switch (notificationType) {
            case ACTION_BAR:
                client.player.sendMessage(message, true);
                break;
            case TITLE:
                client.inGameHud.setTitle(Text.literal("Low Durability!").formatted(Formatting.RED));
                client.inGameHud.setSubtitle(message);
                client.inGameHud.setTitleTicks(10, 40, 10);
                break;
            case GUI:
                // GUI display is handled by renderDurabilityOverlay method
                client.player.sendMessage(message, false);
                break;
        }

        if (playSound) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        }

        ShadesClient.LOGGER.info("Sent durability warning for " + itemName + " at " + durabilityPercent + "%");
    }

    // This method would be called during HUD rendering
    public void renderDurabilityOverlay(DrawContext context) {
        if (!isEnabled() || !showDurabilityOverlay || notificationType != ModuleConfigGUI.NotificationType.GUI) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Draw the module GUI background
        int width = 120;
        int height = 50;
        int x = 5;
        int y = 5;

        // Background
        context.fill(x, y, x + width, y + height, 0xAA000000);

        // Border
        context.fill(x, y, x + width, y + 1, 0xFF404040);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF404040);
        context.fill(x, y, x + 1, y + height, 0xFF404040);
        context.fill(x + width - 1, y, x + width, y + height, 0xFF404040);

        // Header
        String headerText = "— Tool Durability —";
        int headerWidth = client.textRenderer.getWidth(headerText);
        context.drawText(client.textRenderer, headerText, x + (width - headerWidth) / 2, y + 5, 0xFF00FF00, true);

        // Separator
        context.fill(x + 5, y + 15, x + width - 5, y + 16, 0xFF404040);

        // Main hand item durability
        ItemStack mainHandItem = client.player.getMainHandStack();
        renderItemDurability(context, mainHandItem, x + 10, y + 20, "Main Hand");

        // Offhand item durability
        ItemStack offHandItem = client.player.getOffHandStack();
        renderItemDurability(context, offHandItem, x + 10, y + 35, "Off Hand");
    }

    private void renderItemDurability(DrawContext context, ItemStack stack, int x, int y, String slotName) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!stack.isEmpty() && stack.isDamageable()) {
            int maxDamage = stack.getMaxDamage();
            int damage = stack.getDamage();

            if (maxDamage > 0) {
                int durabilityPercent = (int) (100 * (maxDamage - damage) / (float) maxDamage);

                // Determine color based on durability
                int color;
                if (durabilityPercent <= warningThreshold) {
                    color = 0xFFFF0000; // Red
                } else if (durabilityPercent <= 25) {
                    color = 0xFFFF7F00; // Orange
                } else if (durabilityPercent <= 50) {
                    color = 0xFFFFFF00; // Yellow
                } else {
                    color = 0xFF00FF00; // Green
                }

                // Draw item name and durability
                String itemName = stack.getName().getString();
                String text = slotName + ": " + itemName + " (" + durabilityPercent + "%)";
                context.drawTextWithShadow(client.textRenderer, text, x, y, color);
            }
        } else {
            // No item or not damageable
            context.drawTextWithShadow(client.textRenderer, slotName + ": -", x, y, 0xFFAAAAAA);
        }
    }

    // Getters and setters
    public int getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(int threshold) {
        this.warningThreshold = Math.max(1, Math.min(100, threshold));
        ShadesClient.LOGGER.info("Set durability warning threshold to " + warningThreshold + "%");
    }

    public boolean isShowDurabilityOverlay() {
        return showDurabilityOverlay;
    }

    public void setShowDurabilityOverlay(boolean showOverlay) {
        this.showDurabilityOverlay = showOverlay;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    public ModuleConfigGUI.NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(ModuleConfigGUI.NotificationType type) {
        this.notificationType = type;
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