package org.neo.shadesclient.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.neo.shadesclient.events.EventHandler;
import org.neo.shadesclient.modules.InventoryLockModule;
import org.neo.shadesclient.modules.ItemRenameModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neo.shadesclient.qolitems.ShadesClientScreen;
import org.neo.shadesclient.qolitems.ModuleManager;
import org.neo.shadesclient.qolitems.ModuleGUIManager;
import net.fabricmc.api.ClientModInitializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShadesClient implements ClientModInitializer {
    public static final String MOD_ID = "shadesclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openGuiKey;
    private static final List<KeyBinding> moduleKeybindings = new ArrayList<>();

    // Flags to handle one-time execution for commands
    private static final AtomicBoolean mainGuiPending = new AtomicBoolean(false);
    private static final AtomicBoolean renameGuiPending = new AtomicBoolean(false);

    @Override
    public void onInitializeClient() {
        LOGGER.info("ShadesClient is initializing...");

        EventHandler.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EventHandler.onTick();

            // Handle pending GUI openings from commands
            if (mainGuiPending.get() && client.currentScreen == null) {
                LOGGER.info("Opening main GUI from command (deferred)");
                client.setScreen(new ShadesClientScreen());
                client.inGameHud.getChatHud().addMessage(
                        Text.literal("§a[ShadesClient] §7Main menu opened via command"));
                mainGuiPending.set(false);
            }

            if (renameGuiPending.get() && client.currentScreen == null) {
                LOGGER.info("Opening rename GUI from command (deferred)");
                try {
                    ItemRenameModule module = ModuleManager.getModule(ItemRenameModule.class);
                    if (module != null) {
                        if (!module.isEnabled()) {
                            module.setEnabled(true);
                            client.inGameHud.getChatHud().addMessage(
                                    Text.literal("§a[ShadesClient] §7Enabled Item Rename module"));
                        }
                        module.openConfigScreen();
                        client.inGameHud.getChatHud().addMessage(
                                Text.literal("§a[ShadesClient] §7Rename config opened successfully"));
                    } else {
                        LOGGER.error("Failed to find Item Rename module!");
                        client.inGameHud.getChatHud().addMessage(
                                Text.literal("§c[ShadesClient] §7Error: Item Rename module not found!"));
                    }
                } catch (Exception e) {
                    LOGGER.error("Error opening rename config: " + e.getMessage(), e);
                    client.inGameHud.getChatHud().addMessage(
                            Text.literal("§c[ShadesClient] §7Error: " + e.getMessage()));
                }
                renameGuiPending.set(false);
            }
        });

        WorldRenderEvents.END.register(context -> {
            LOGGER.info("Context class: " + context.getClass().getName());
            float tempTickDelta = 0.0f;
            EventHandler.onRenderWorld(context.matrixStack(), tempTickDelta);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            EventHandler.onRenderHUD(drawContext);
        });

        // Register main GUI keybinding
        openGuiKey = new KeyBinding(
                "key.shadesclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.shadesclient.general"
        );
        KeyBindingHelper.registerKeyBinding(openGuiKey);
        LOGGER.info("Main keybinding registered");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.wasPressed() && client.currentScreen == null) {
                LOGGER.info("Keybinding pressed, opening screen");
                client.setScreen(new ShadesClientScreen());
            }
        });
        LOGGER.info("Keybinding event registered");

        ModuleManager.initializeModules();
        LOGGER.info("Modules initialized");

        registerCommands();
        LOGGER.info("Commands registered");

        // Register module keybindings
        registerModuleKeybindings();
        LOGGER.info("Module keybindings registered");

        LOGGER.info("ShadesClient initialization complete");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LOGGER.info("Registering /shades commands");

            dispatcher.register(ClientCommandManager.literal("shades")
                    .executes(context -> {
                        LOGGER.info("Command /shades executed");

                        // Set flag to open the GUI in the next tick
                        mainGuiPending.set(true);
                        return 1;
                    })
                    // Add rename subcommand
                    .then(ClientCommandManager.literal("rename")
                            .executes(context -> {
                                LOGGER.info("Command /shades rename executed");

                                // Set flag to open the rename GUI in the next tick
                                renameGuiPending.set(true);
                                return 1;
                            })
                    )
            );

            LOGGER.info("/shades commands registered");
        });
    }


    private void registerModuleKeybindings() {
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
    }
}