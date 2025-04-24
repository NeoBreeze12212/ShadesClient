package org.neo.shadesclient.qolitems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.FishingNotifierModule;
import org.neo.shadesclient.modules.InventoryLockModule;
import org.neo.shadesclient.modules.PlaytimeTrackerModule;
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
    private static final int TITLE_BAR_HEIGHT = 10;

    private boolean isDragging = false;
    private String moduleInDragMode = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

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

        // Calculate total height based on active modules
        int moduleHeight = calculateModuleHeight();
        if (moduleHeight == 0) return; // No modules to display

        int width = 130;
        int currentY = guiY;

        // Background with title bar
        context.fill(guiX, currentY, guiX + width, currentY + TITLE_BAR_HEIGHT, 0xFF000000); // Black title bar
        context.fill(guiX, currentY + TITLE_BAR_HEIGHT, guiX + width, currentY + moduleHeight, BACKGROUND_COLOR);

        // Regular border
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
                    TorchReminderModule torchModule = (TorchReminderModule) module;
                    
                    // Check if module has custom position
                    if (torchModule.hasCustomPosition() && !moduleInDragMode.equals("Torch Reminder")) {
                        // Render at custom position
                        torchModule.renderTorchReminderInfo(context, 0, 0, width);
                    } else {
                        // Render in the normal flow
                        currentY = torchModule.renderTorchReminderInfo(context, guiX, currentY, width);
                    }
                } else if (module instanceof FishingNotifierModule) {
                    FishingNotifierModule fishingModule = (FishingNotifierModule) module;
                    currentY = fishingModule.renderFishingInfo(context, guiX, currentY, width);
                } else if (module instanceof InventoryLockModule) {
                    currentY = ((InventoryLockModule) module).renderInventoryLockInfo(context, guiX, currentY, width);
                } else if (module instanceof PlaytimeTrackerModule) {
                    PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;
                    
                    // Check if module has custom position
                    if (playtimeModule.hasCustomPosition() && !moduleInDragMode.equals("Playtime Tracker")) {
                        // Render at custom position
                        playtimeModule.renderPlaytimeInfo(context, 0, 0, width);
                    } else {
                        // Render in the normal flow
                        currentY = playtimeModule.renderPlaytimeInfo(context, guiX, currentY, width);
                    }
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
                    totalHeight += 60; // Adjusted for FishingNotifierModule height
                } else if (module instanceof InventoryLockModule) {
                    totalHeight += 35; // More compact height
                } else if (module instanceof PlaytimeTrackerModule) {
                    totalHeight += ((PlaytimeTrackerModule) module).calculateHeight();
                }
            }
        }

        return totalHeight;
    }

    public void startDragging(int mouseX, int mouseY) {
        if (moduleInDragMode != null) {
            isDragging = true;
            dragOffsetX = mouseX - guiX;
            dragOffsetY = mouseY - guiY;
        }
    }

    // Add this method to handle the actual dragging
    public void handleDrag(int mouseX, int mouseY) {
        if (isDragging) {
            guiX = mouseX - dragOffsetX;
            guiY = mouseY - dragOffsetY;

            // Make sure the GUI doesn't go off-screen
            MinecraftClient client = MinecraftClient.getInstance();
            if (guiX < 0) guiX = 0;
            if (guiY < 0) guiY = 0;
            if (guiX > client.getWindow().getScaledWidth() - 130) guiX = client.getWindow().getScaledWidth() - 130;
            if (guiY > client.getWindow().getScaledHeight() - 50) guiY = client.getWindow().getScaledHeight() - 50;
        }
    }

    // Add this method to end the drag operation
    public void stopDragging() {
        isDragging = false;
    }

    // Add this method to set drag mode for a specific module
    public void setDragModeForModule(String moduleName, boolean dragMode) {
        if (dragMode) {
            moduleInDragMode = moduleName;
        } else {
            moduleInDragMode = null;
        }
        isDragging = false;
    }

    // Add this method to check if a module is in drag mode
    public boolean isInDragMode() {
        return moduleInDragMode != null;
    }

    // Add this method to check if a point is within the GUI area
    public boolean isPointInGUI(int mouseX, int mouseY) {
        int height = calculateModuleHeight();
        return mouseX >= guiX && mouseX <= guiX + 130 &&
                mouseY >= guiY && mouseY <= guiY + height;
    }

    // Add this method to save module positions
    // Update saveModulePositions method
    public void saveModulePositions() {
        // Save the positions to the modules
        for (Module module : modules) {
            if (module instanceof PlaytimeTrackerModule && moduleInDragMode != null && 
                moduleInDragMode.equals("Playtime Tracker")) {
                PlaytimeTrackerModule playtimeModule = (PlaytimeTrackerModule) module;
                playtimeModule.setPosition(guiX, guiY);
                playtimeModule.setCustomPosition(true);
            } else if (module instanceof TorchReminderModule && moduleInDragMode != null && 
                      moduleInDragMode.equals("Torch Reminder")) {
                TorchReminderModule torchModule = (TorchReminderModule) module;
                torchModule.setGuiPosition(guiX, guiY);
                torchModule.setCustomPosition(true);
            }
        }
        
        ShadesClient.LOGGER.info("Saved module position: X=" + guiX + ", Y=" + guiY);
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
        // Always render the GUI
        render(context);

        // Also call renderHUD for modules that need to render directly on the game HUD
        for (Module module : modules) {
            if (module.isEnabled() && module instanceof InventoryLockModule) {
                ((InventoryLockModule) module).renderHUD(context);
            }
        }
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