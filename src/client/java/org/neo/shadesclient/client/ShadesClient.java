package org.neo.shadesclient.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.neo.shadesclient.events.EventHandler;
import org.neo.shadesclient.modules.InventoryLockModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neo.shadesclient.qolitems.ShadesClientScreen;
import org.neo.shadesclient.qolitems.ModuleManager;
import org.neo.shadesclient.qolitems.ModuleGUIManager;
import net.fabricmc.api.ClientModInitializer;
import org.neo.shadesclient.modules.InventoryLockModule;

import java.util.ArrayList;
import java.util.List;

public class ShadesClient implements ClientModInitializer {
    public static final String MOD_ID = "shadesclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openGuiKey;
    private static final List<KeyBinding> moduleKeybindings = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        LOGGER.info("ShadesClient is initializing...");

        EventHandler.init();

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

        // Register mouse input handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check if we're in-game and not in a GUI
            if (client.world != null && client.currentScreen == null) {
                double mouseX = client.mouse.getX();
                double mouseY = client.mouse.getY();
                boolean leftMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

                handleMouse(mouseX, mouseY, leftMouseDown);
            }
        });
        LOGGER.info("Mouse input handler registered");

        // Register main GUI keybinding
        openGuiKey = new KeyBinding(
                "key.shadesclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.shadesclient.general"
        );
        KeyBindingHelper.registerKeyBinding(openGuiKey);
        LOGGER.info("Main keybinding registered");

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

        // Register module keybindings
        registerModuleKeybindings();
        LOGGER.info("Module keybindings registered");

        LOGGER.info("ShadesClient initialization complete");
    }

    /**
     * Handle mouse input and delegate to the ModuleGUIManager
     * @param mouseX X position of the mouse
     * @param mouseY Y position of the mouse
     * @param mouseDown Whether the left mouse button is pressed
     */
    public void handleMouse(double mouseX, double mouseY, boolean mouseDown) {
        ModuleGUIManager.getInstance().onMouseInput(mouseX, mouseY, mouseDown);
    }

    /**
     * Register keybindings for all modules that provide them
     */
    private void registerModuleKeybindings() {
        // Register InventoryLockModule keybindings
        InventoryLockModule inventoryLockModule = ModuleManager.getModule(InventoryLockModule.class);

        if (inventoryLockModule != null) {
            KeyBinding[] moduleBindings = inventoryLockModule.getKeybindings();
            if (moduleBindings != null) {
                for (KeyBinding binding : moduleBindings) {
                    KeyBindingHelper.registerKeyBinding(binding);
                    moduleKeybindings.add(binding);
                    LOGGER.info("Registered keybinding: " + binding.getTranslationKey());
                }
            }
        }

        // Add other modules with keybindings here as needed
    }
}