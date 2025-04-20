package org.neo.shadesclient.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.neo.shadesclient.events.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neo.shadesclient.qolitems.ShadesClientScreen;
import org.neo.shadesclient.qolitems.ModuleManager;
import net.fabricmc.api.ClientModInitializer;

public class ShadesClient implements ClientModInitializer {
    public static final String MOD_ID = "shadesclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ShadesClient is initializing...");

        EventHandler.init();

        // In ShadesClient.java, add these registrations after EventHandler.init():

// Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EventHandler.onTick();
        });

        WorldRenderEvents.END.register(context -> {
            LOGGER.info("Context class: " + context.getClass().getName());
            // Use a temporary value for now
            float tempTickDelta = 0.0f;
            EventHandler.onRenderWorld(context.matrixStack(), tempTickDelta);
        });
// HUD rendering event
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            EventHandler.onRenderHUD(drawContext);
        });

        // Register keybinding
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.shadesclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.shadesclient.general"
        ));
        LOGGER.info("Keybinding registered");

        // Register tick event for the keybinding - Fixed to use lambda
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.wasPressed() && client.currentScreen == null) {
                LOGGER.info("Keybinding pressed, opening screen");
                client.setScreen(new ShadesClientScreen());
            }
        });
        LOGGER.info("Keybinding event registered");

        // Register the /shades command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LOGGER.info("Registering /shades command");
            dispatcher.register(ClientCommandManager.literal("shades")
                    .executes(context -> {
                        LOGGER.info("Command /shades executed");
                        context.getSource().getClient().execute(() -> {
                            LOGGER.info("Opening screen from command");
                            context.getSource().getClient().setScreen(new ShadesClientScreen());
                        });
                        return 1;
                    })
            );
            LOGGER.info("/shades command registered");
        });

        // Initialize modules
        ModuleManager.initializeModules();
        LOGGER.info("Modules initialized");

        LOGGER.info("ShadesClient initialization complete");
    }
}