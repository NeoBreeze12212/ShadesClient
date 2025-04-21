package org.neo.shadesclient.qolitems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.neo.shadesclient.modules.ToolDurabilityModule;
import org.neo.shadesclient.modules.TorchReminderModule;
import org.neo.shadesclient.qolitems.ModuleConfigGUI; // Correct import path

import java.util.ArrayList;
import java.util.List;

public class ModuleGUIManager {
    private static final ModuleGUIManager INSTANCE = new ModuleGUIManager();

    private final List<Module> modules = new ArrayList<>();
    private boolean isEnabled = true;

    // GUI positioning and styling
    private int guiX = 5;
    private int guiY = 5;
    private int moduleSpacing = 10;
    private static final int BACKGROUND_COLOR = 0xAA000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFFFDE92F;     // Yellow border like in image
    private static final int HEADER_COLOR = 0xFFFFFF55;     // Yellow text for headers
    private static final int VALUE_COLOR = 0xFFFFFFFF;      // White for values
    private static final int WARNING_COLOR = 0xFFFF5555;    // Red for warnings
    private static final int SUCCESS_COLOR = 0xFF55FF55;    // Green for good values
    private static final int ICON_COLOR = 0xFFAAAAAA;       // Gray for icons

    private ModuleGUIManager() {
        // Private constructor for singleton
    }

    public static ModuleGUIManager getInstance() {
        return INSTANCE;
    }

    public void registerModule(Module module) {
        if (!modules.contains(module)) {
            modules.add(module);
        }
    }

    public void unregisterModule(Module module) {
        modules.remove(module);
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void render(DrawContext context) {
        if (!isEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Calculate total height needed for all modules
        int totalHeight = 25; // Header height

        // Add space for each enabled module
        for (Module module : modules) {
            if (module.isEnabled()) {
                if (module instanceof ToolDurabilityModule) {
                    totalHeight += 55; // Height for tool durability section
                } else if (module instanceof TorchReminderModule) {
                    totalHeight += 45; // Height for torch reminder section
                }
            }
        }

        if (totalHeight <= 25) return; // No modules to display

        int width = 130;
        int currentY = guiY;

        // Background
        context.fill(guiX, currentY, guiX + width, currentY + totalHeight, BACKGROUND_COLOR);

        // Border (with yellow color like in the example)
        drawBorder(context, guiX, currentY, width, totalHeight);

        // Header
        String headerText = "— ShadesClient —";
        int headerWidth = client.textRenderer.getWidth(headerText);
        context.drawText(client.textRenderer, headerText, guiX + (width - headerWidth) / 2, currentY + 5, HEADER_COLOR, true);

        // Header separator
        context.fill(guiX + 5, currentY + 15, guiX + width - 5, currentY + 16, BORDER_COLOR);

        currentY += 20; // Move down past header

        // Render each enabled module
        for (Module module : modules) {
            if (module.isEnabled()) {
                if (module instanceof ToolDurabilityModule) {
                    currentY = renderToolDurabilityInfo(context, (ToolDurabilityModule) module, guiX, currentY, width);
                } else if (module instanceof TorchReminderModule) {
                    currentY = renderTorchReminderInfo(context, (TorchReminderModule) module, guiX, currentY, width);
                }
            }
        }
    }

    private int renderToolDurabilityInfo(DrawContext context, ToolDurabilityModule module, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Module title with pickaxe icon
        String titleText = "⛏ Tool Durability";
        context.drawText(client.textRenderer, titleText, x + 8, y, HEADER_COLOR, true);

        // Draw main hand info
        String mainHandText = "";
        int mainHandColor = VALUE_COLOR;

        if (!client.player.getMainHandStack().isEmpty() && client.player.getMainHandStack().isDamageable()) {
            int maxDamage = client.player.getMainHandStack().getMaxDamage();
            int damage = client.player.getMainHandStack().getDamage();
            int durabilityPercent = (int) (100 * (maxDamage - damage) / (float) maxDamage);

            String itemName = client.player.getMainHandStack().getName().getString();
            mainHandText = "Main: " + itemName + ": " + durabilityPercent + "%";

            // Color based on durability
            if (durabilityPercent <= module.getWarningThreshold()) {
                mainHandColor = WARNING_COLOR;
            } else if (durabilityPercent <= 25) {
                mainHandColor = 0xFFFF7F00; // Orange
            } else {
                mainHandColor = SUCCESS_COLOR;
            }
        } else {
            mainHandText = "Main: No tool";
        }

        context.drawText(client.textRenderer, mainHandText, x + 10, y + 15, mainHandColor, true);

        // Draw off hand info
        String offHandText = "";
        int offHandColor = VALUE_COLOR;

        if (!client.player.getOffHandStack().isEmpty() && client.player.getOffHandStack().isDamageable()) {
            int maxDamage = client.player.getOffHandStack().getMaxDamage();
            int damage = client.player.getOffHandStack().getDamage();
            int durabilityPercent = (int) (100 * (maxDamage - damage) / (float) maxDamage);

            String itemName = client.player.getOffHandStack().getName().getString();
            offHandText = "Off: " + itemName + ": " + durabilityPercent + "%";

            // Color based on durability
            if (durabilityPercent <= module.getWarningThreshold()) {
                offHandColor = WARNING_COLOR;
            } else if (durabilityPercent <= 25) {
                offHandColor = 0xFFFF7F00; // Orange
            } else {
                offHandColor = SUCCESS_COLOR;
            }
        } else {
            offHandText = "Off: No tool";
        }

        context.drawText(client.textRenderer, offHandText, x + 10, y + 30, offHandColor, true);

        // Add threshold info
        context.drawText(client.textRenderer, "Threshold: " + module.getWarningThreshold() + "%", x + 10, y + 45, VALUE_COLOR, true);

        return y + 55; // Return new Y position for next module
    }

    private int renderTorchReminderInfo(DrawContext context, TorchReminderModule module, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Module title with torch icon
        String titleText = "🔥 Torch Reminder";
        context.drawText(client.textRenderer, titleText, x + 8, y, HEADER_COLOR, true);

        // Get light levels
        int blockLight = client.world.getLightLevel(net.minecraft.world.LightType.BLOCK, client.player.getBlockPos());
        int skyLight = client.world.getLightLevel(net.minecraft.world.LightType.SKY, client.player.getBlockPos());
        int effectiveLight = Math.max(blockLight, skyLight - client.world.getAmbientDarkness());

        // Light level info with color coding
        int lightColor;
        if (effectiveLight < module.getLightLevelThreshold()) {
            lightColor = WARNING_COLOR;
        } else if (effectiveLight < 10) {
            lightColor = 0xFFFF7F00; // Orange
        } else {
            lightColor = SUCCESS_COLOR;
        }

        context.drawText(client.textRenderer, "Light: " + effectiveLight, x + 10, y + 15, lightColor, true);
        context.drawText(client.textRenderer, "Threshold: " + module.getLightLevelThreshold(), x + 10, y + 30, VALUE_COLOR, true);

        return y + 45; // Return new Y position for next module
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height) {
        // Top border
        context.fill(x, y, x + width, y + 1, BORDER_COLOR);
        // Bottom border
        context.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        // Left border
        context.fill(x, y, x + 1, y + height, BORDER_COLOR);
        // Right border
        context.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
    }

    // Method to be called from game's render loop
    public void onRenderGameHud(DrawContext context) {
        render(context);

        // Also call module-specific rendering if they use GUI notification type
        for (Module module : modules) {
            if (module.isEnabled()) {
                if (module instanceof ToolDurabilityModule) {
                    ToolDurabilityModule toolModule = (ToolDurabilityModule) module;
                    if (toolModule.isShowDurabilityOverlay() &&
                            toolModule.getNotificationType().equals(ModuleConfigGUI.NotificationType.GUI)) {
                        // Using equals() instead of == for enum comparison
                        toolModule.renderDurabilityOverlay(context);
                    }
                } else if (module instanceof TorchReminderModule) {
                    TorchReminderModule torchModule = (TorchReminderModule) module;
                    if (torchModule.getNotificationType().equals(ModuleConfigGUI.NotificationType.GUI)) {
                        // Using equals() instead of == for enum comparison
                        torchModule.renderLightLevelOverlay(context);
                    }
                }
            }
        }
    }

    // Methods for setting GUI position
    public void setPosition(int x, int y) {
        this.guiX = x;
        this.guiY = y;
    }

    public int getX() {
        return guiX;
    }

    public int getY() {
        return guiY;
    }
}