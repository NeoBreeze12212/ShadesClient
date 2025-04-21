package org.neo.shadesclient.events;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.*;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.qolitems.ModuleGUIManager;
import org.neo.shadesclient.qolitems.ModuleManager;
/**
 * Central event handler that dispatches Minecraft lifecycle events to the appropriate modules
 */
public class EventHandler {
    private static MinecraftClient client;

    // Initialize the event handler
    public static void init() {
        client = MinecraftClient.getInstance();
        ShadesClient.LOGGER.info("Initializing ShadesClient Event Handler");
    }

    /**
     * Called on every game tick
     * This is where we dispatch tick events to various modules
     */
    public static void onTick() {
        try {
            if (client == null || client.player == null || client.world == null) return;

            // Dispatch tick events to modules
            for (Module module : ModuleManager.getModules()) {
                if (!module.isEnabled()) continue;

                if (module instanceof HungerWarningModule) {
                    ((HungerWarningModule) module).checkHunger();
                }
                else if (module instanceof HealthWarningModule) {
                    ((HealthWarningModule) module).checkHealth();
                }
                else if (module instanceof ToolDurabilityModule) {
                    ((ToolDurabilityModule) module).checkInventory();
                }
                else if (module instanceof TorchReminderModule) {
                    ((TorchReminderModule) module).checkLightLevel();
                }
                else if (module instanceof DeathWaypointModule) {
                    ((DeathWaypointModule) module).checkPlayerStatus();
                }
                else if (module instanceof FishingNotifierModule) {
                    ((FishingNotifierModule) module).checkFishingBobber();
                }
                else if (module instanceof CropHelperModule) {
                    ((CropHelperModule) module).tick();
                }
                else if (module instanceof PlaytimeTrackerModule) {
                    // BOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO
                }
            }
        } catch (Exception e) {
            // Log the error but don't crash the game
            ShadesClient.LOGGER.error("Error in ShadesClient tick event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called during world rendering
     */
    public static void onRenderWorld(MatrixStack matrixStack, float tickDelta) {
        try {
            if (client == null || client.player == null || client.world == null) return;

            // Get camera position for rendering offset calculations
            double cameraX = client.gameRenderer.getCamera().getPos().x;
            double cameraY = client.gameRenderer.getCamera().getPos().y;
            double cameraZ = client.gameRenderer.getCamera().getPos().z;

            VertexConsumerProvider.Immediate immediate =
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            // Dispatch render events to modules
            for (Module module : ModuleManager.getModules()) {
                if (!module.isEnabled()) continue;

                if (module instanceof CropHelperModule) {
                    ((CropHelperModule) module).render(matrixStack, immediate, cameraX, cameraY, cameraZ);
                }
            }
        } catch (Exception e) {
            // Log the error but don't crash the game
            ShadesClient.LOGGER.error("Error in ShadesClient render event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called during HUD rendering
     */
    public static void onRenderHUD(DrawContext context) {
        try {
            if (client == null || client.player == null) return;

            // Dispatch HUD render events to modules
            for (Module module : ModuleManager.getModules()) {
                if (!module.isEnabled()) continue;

                if (module instanceof ToolDurabilityModule) {
                    ((ToolDurabilityModule) module).renderDurabilityOverlay(context);
                }
                else if (module instanceof InventoryLockModule) {
                    ((InventoryLockModule) module).renderHUD(context);
                }
                else if (module instanceof PlaytimeTrackerModule) {
                    int offsetY = 0;  // Define an appropriate Y offset
                    int displayWidth = context.getScaledWindowWidth();  // Get the display width from context

                    ((PlaytimeTrackerModule) module).renderPlaytimeInfo(context,
                            ModuleGUIManager.getInstance().getX(),
                            ModuleGUIManager.getInstance().getY() + offsetY,
                            displayWidth);
                }
            }
        } catch (Exception e) {
            // Log the error but don't crash the game
            ShadesClient.LOGGER.error("Error in ShadesClient HUD render event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}