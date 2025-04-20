package org.neo.shadesclient.qolitems;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.FishingNotifierModule;
import org.neo.shadesclient.qolitems.ModuleManager;
import org.neo.shadesclient.qolitems.Module;

public class HudRenderer {
    private final MinecraftClient client;

    public HudRenderer() {
        this.client = MinecraftClient.getInstance();
    }

    public void render(DrawContext context) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        // Render module HUD elements
        renderModuleHudElements(context);
    }

    private void renderModuleHudElements(DrawContext context) {
        // Get the fishing module by iterating through modules
        FishingNotifierModule fishingModule = null;
        for (Module module : ModuleManager.getModules()) {
            if (module instanceof FishingNotifierModule) {
                fishingModule = (FishingNotifierModule) module;
                break;
            }
        }

        if (fishingModule != null && fishingModule.isEnabled()) {
            fishingModule.onRenderHud(context);
        }

        // Other modules' HUD rendering can be added here
    }
}