package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

import java.util.HashSet;
import java.util.Set;

public class ToolDurabilityModule extends Module {
    private static final int DEFAULT_WARNING_THRESHOLD = 10; // Percentage of durability remaining
    private int warningThreshold = DEFAULT_WARNING_THRESHOLD;
    private boolean showDurabilityOverlay = true;
    private boolean playSound = true;

    private final Set<Integer> warnedItems = new HashSet<>();

    public ToolDurabilityModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module enabled");
        warnedItems.clear();
    }

    @Override
    protected void onDisable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module disabled");
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

        client.player.sendMessage(
                Text.literal("§c[Tool Durability] §f" + itemName + " is at §c" + durabilityPercent + "%§f durability!"),
                true
        );

        if (playSound) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        }

        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Sent durability warning for " + itemName + " at " + durabilityPercent + "%");
    }

    // This method would be called during HUD rendering
    public void renderDurabilityOverlay(DrawContext context) {
        if (!isEnabled() || !showDurabilityOverlay) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack mainHandItem = client.player.getMainHandStack();

        if (!mainHandItem.isEmpty() && mainHandItem.isDamageable()) {
            int maxDamage = mainHandItem.getMaxDamage();
            int damage = mainHandItem.getDamage();

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

                // Render durability text
                String text = durabilityPercent + "%";
                int x = context.getScaledWindowWidth() / 2;
                int y = context.getScaledWindowHeight() - 40;

                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, x, y, color);
            }
        }
    }

    public void setWarningThreshold(int threshold) {
        this.warningThreshold = Math.max(1, Math.min(100, threshold));
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Set durability warning threshold to " + warningThreshold + "%");
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        // Would implement configuration screen for threshold and display settings
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}