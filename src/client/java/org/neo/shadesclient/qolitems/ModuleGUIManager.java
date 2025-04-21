package org.neo.shadesclient.qolitems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.neo.shadesclient.modules.FishingNotifierModule;
import org.neo.shadesclient.modules.InventoryLockModule;
import org.neo.shadesclient.modules.ToolDurabilityModule;
import org.neo.shadesclient.modules.TorchReminderModule;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;

import java.util.ArrayList;
import java.util.List;

public class ModuleGUIManager {
    private static final ModuleGUIManager INSTANCE = new ModuleGUIManager();

    private final List<Module> modules = new ArrayList<>();
    private boolean isEnabled = true;

    // GUI positioning and styling
    private int guiX = 5;
    private int guiY = 5;
    private int moduleSpacing = 5;
    private static final int BACKGROUND_COLOR = 0xAA000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFFFDE92F;     // Yellow border
    private static final int HEADER_COLOR = 0xFF00FF00;     // Green text for headers (like in image)
    private static final int VALUE_COLOR = 0xFFFFFFFF;      // White for values
    private static final int WARNING_COLOR = 0xFFFF5555;    // Red for warnings
    private static final int SUCCESS_COLOR = 0xFF55FF55;    // Green for good values
    private static final int ICON_COLOR = 0xFFAAAAAA;       // Gray for icons

    // Dragging functionality
    private boolean isDragging = false;
    private int dragOffsetX, dragOffsetY;
    private static final int TITLE_BAR_HEIGHT = 10;

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

    public void handleMouseInput(double mouseX, double mouseY, boolean mouseDown) {
        if (!isEnabled) return;

        if (mouseDown) {
            // Check if click is in title area of the GUI
            if (mouseX >= guiX && mouseX <= guiX + 130 &&
                    mouseY >= guiY && mouseY <= guiY + TITLE_BAR_HEIGHT) {
                isDragging = true;
                dragOffsetX = (int) (mouseX - guiX);
                dragOffsetY = (int) (mouseY - guiY);
            }
        } else {
            isDragging = false;
        }

        // Update position if dragging
        if (isDragging) {
            guiX = (int) (mouseX - dragOffsetX);
            guiY = (int) (mouseY - dragOffsetY);

            // Ensure GUI stays on screen
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            guiX = Math.max(0, Math.min(guiX, screenWidth - 130));
            guiY = Math.max(0, Math.min(guiY, screenHeight - 50));
        }
    }

    public void render(DrawContext context) {
        if (!isEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Calculate total height based on active modules
        int moduleHeight = calculateModuleHeight();
        if (moduleHeight == 0) return; // No modules to display

        int width = 130;
        int currentY = guiY;

        // Background with title bar
        context.fill(guiX, currentY, guiX + width, currentY + TITLE_BAR_HEIGHT, 0xFF000000); // Black title bar
        context.fill(guiX, currentY + TITLE_BAR_HEIGHT, guiX + width, currentY + moduleHeight, BACKGROUND_COLOR);

        // Border
        drawBorder(context, guiX, currentY, width, moduleHeight);

        // Title "ShadesClient" - smaller and in title bar
        String headerText = "ShadesClient";
        int headerWidth = client.textRenderer.getWidth(headerText);
        context.drawText(client.textRenderer, headerText, guiX + (width - headerWidth) / 2, currentY + 2, HEADER_COLOR, false);

        currentY += TITLE_BAR_HEIGHT; // Move down past title bar

        // Render each enabled module
        for (Module module : modules) {
            if (module.isEnabled()) {
                // Add spacing between modules
                if (currentY > guiY + TITLE_BAR_HEIGHT) {
                    currentY += moduleSpacing;
                }

                // Render the appropriate module
                if (module instanceof ToolDurabilityModule) {
                    currentY = renderToolDurabilityInfo(context, (ToolDurabilityModule) module, guiX, currentY, width);
                } else if (module instanceof TorchReminderModule) {
                    currentY = renderTorchReminderInfo(context, (TorchReminderModule) module, guiX, currentY, width);
                } else if (module instanceof FishingNotifierModule) {
                    currentY = ((FishingNotifierModule) module).renderFishingInfo(context, guiX, currentY, width);
                } else if (module instanceof InventoryLockModule) {
                    currentY = ((InventoryLockModule) module).renderInventoryLockInfo(context, guiX, currentY, width);
                }
            }
        }
    }

    private int calculateModuleHeight() {
        int totalHeight = TITLE_BAR_HEIGHT; // Start with title bar height
        boolean firstModule = true;

        for (Module module : modules) {
            if (module.isEnabled()) {
                // Add spacing except for first module
                if (!firstModule) {
                    totalHeight += moduleSpacing;
                } else {
                    firstModule = false;
                }

                if (module instanceof ToolDurabilityModule) {
                    totalHeight += 30; // More compact height
                } else if (module instanceof TorchReminderModule) {
                    totalHeight += 25; // More compact height
                } else if (module instanceof FishingNotifierModule) {
                    totalHeight += 30; // More compact height
                } else if (module instanceof InventoryLockModule) {
                    totalHeight += 35; // More compact height
                }
            }
        }

        return totalHeight;
    }

    private int renderToolDurabilityInfo(DrawContext context, ToolDurabilityModule module, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Module title with pickaxe icon - centered and green like in image
        String titleText = "— Tool Durability —";
        int titleWidth = client.textRenderer.getWidth(titleText);
        context.drawText(client.textRenderer, titleText, x + (width - titleWidth) / 2, y, HEADER_COLOR, false);
        y += 10;

        // Draw main hand info - more compact
        if (!client.player.getMainHandStack().isEmpty() && client.player.getMainHandStack().isDamageable()) {
            int maxDamage = client.player.getMainHandStack().getMaxDamage();
            int damage = client.player.getMainHandStack().getDamage();
            int durabilityPercent = (int) (100 * (maxDamage - damage) / (float) maxDamage);

            String itemName = client.player.getMainHandStack().getName().getString();
            String mainHandText = "Main Hand: " + itemName + " (" + durabilityPercent + "%)";

            // Color based on durability - orange for warnings like in image
            int mainHandColor;
            if (durabilityPercent <= module.getWarningThreshold()) {
                mainHandColor = 0xFFFF7700; // Orange like in image
            } else if (durabilityPercent <= 25) {
                mainHandColor = 0xFFFF7700; // Orange
            } else {
                mainHandColor = VALUE_COLOR; // White for normal
            }

            context.drawText(client.textRenderer, mainHandText, x + 5, y, mainHandColor, false);
        } else {
            context.drawText(client.textRenderer, "Main Hand: -", x + 5, y, VALUE_COLOR, false);
        }
        y += 10;

        // Draw off hand info - more compact
        if (!client.player.getOffHandStack().isEmpty() && client.player.getOffHandStack().isDamageable()) {
            int maxDamage = client.player.getOffHandStack().getMaxDamage();
            int damage = client.player.getOffHandStack().getDamage();
            int durabilityPercent = (int) (100 * (maxDamage - damage) / (float) maxDamage);

            String itemName = client.player.getOffHandStack().getName().getString();
            String offHandText = "Off Hand: " + itemName + " (" + durabilityPercent + "%)";

            // Color based on durability
            int offHandColor;
            if (durabilityPercent <= module.getWarningThreshold()) {
                offHandColor = 0xFFFF7700; // Orange like in image
            } else if (durabilityPercent <= 25) {
                offHandColor = 0xFFFF7700; // Orange
            } else {
                offHandColor = VALUE_COLOR; // White for normal
            }

            context.drawText(client.textRenderer, offHandText, x + 5, y, offHandColor, false);
        } else {
            context.drawText(client.textRenderer, "Off Hand: -", x + 5, y, VALUE_COLOR, false);
        }

        return y + 10; // Return new Y position for next module
    }

    private int renderTorchReminderInfo(DrawContext context, TorchReminderModule module, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Module title centered and green
        String titleText = "— Torch Reminder —";
        int titleWidth = client.textRenderer.getWidth(titleText);
        context.drawText(client.textRenderer, titleText, x + (width - titleWidth) / 2, y, HEADER_COLOR, false);
        y += 10;

        // Get light levels
        int blockLight = client.world.getLightLevel(net.minecraft.world.LightType.BLOCK, client.player.getBlockPos());
        int skyLight = client.world.getLightLevel(net.minecraft.world.LightType.SKY, client.player.getBlockPos());
        int effectiveLight = Math.max(blockLight, skyLight - client.world.getAmbientDarkness());

        // Light level info with color coding
        int lightColor;
        if (effectiveLight < module.getLightLevelThreshold()) {
            lightColor = 0xFFFF7700; // Orange for warning like in image
        } else if (effectiveLight < 10) {
            lightColor = 0xFFFF7700; // Orange
        } else {
            lightColor = VALUE_COLOR; // White for normal
        }

        context.drawText(client.textRenderer, "Light Level: " + effectiveLight + " (Threshold: " + module.getLightLevelThreshold() + ")",
                x + 5, y, lightColor, false);

        return y + 15; // Return new Y position for next module
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

    public void onRenderGameHud(DrawContext context) {
        render(context);

        // Also call renderHUD for modules that need to render directly on the game HUD
        for (Module module : modules) {
            if (module.isEnabled() && module instanceof InventoryLockModule) {
                ((InventoryLockModule) module).renderHUD(context);
            }
        }
    }

    public void onMouseInput(double mouseX, double mouseY, boolean mouseDown) {
        handleMouseInput(mouseX, mouseY, mouseDown);
    }

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