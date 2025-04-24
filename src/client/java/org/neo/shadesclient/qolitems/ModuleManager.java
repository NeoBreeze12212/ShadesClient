package org.neo.shadesclient.qolitems;

import org.neo.shadesclient.modules.*;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();

    public static void initializeModules() {
        // Clear modules list to avoid duplicates on reinitialization
        modules.clear();

        // Log initialization with constant string
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Initializing modules");

        // Register modules for Survival QoL category
        registerModule(new TorchReminderModule("Torch Reminder", "Automatically reminds you to place torches at places below light level 7.", ModuleCategory.SURVIVAL_QOL));
        registerModule(new DeathWaypointModule("Death Waypoint", "Creates a waypoint when you die", ModuleCategory.SURVIVAL_QOL));
        registerModule(new HealthWarningModule("Health Warning", "Warns when health is low", ModuleCategory.SURVIVAL_QOL));
        registerModule(new HungerWarningModule("Hunger Warning", "Warns when your hunger is low", ModuleCategory.SURVIVAL_QOL));

        // Register modules for External Tools
        registerModule(new PlaytimeTrackerModule("Playtime Tracker", "Shows you how long you have been playing", ModuleCategory.EXTERNAL_TOOLS));

        // Register modules for Visuals category
        registerModule(new CropHelperModule("Crop Helper", "Shows when crops are ready for harvest", ModuleCategory.VISUALS));
        registerModule(new WaypointsModule("Waypoints", "Coming Soon", ModuleCategory.VISUALS));
        registerModule(new ToolDurabilityModule("Tool Durability", "Notifies you if your tools are about to break", ModuleCategory.VISUALS));
        registerModule(new ItemRenameModule("Item Rename", "Rename items client-side with custom colors", ModuleCategory.VISUALS));


        // Register modules for Gameplay category
        registerModule(new FishingNotifierModule("Fishing Notifier", "Notifies you if your bobber catched a fish", ModuleCategory.GAMEPLAY));
        registerModule(new InventoryLockModule("Hotbar Lock", "Locks your hotbar slot to prevent dropping items accidentally.", ModuleCategory.GAMEPLAY));

        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Initialized " + modules.size() + " modules");
    }

    public static void registerModule(Module module) {
        modules.add(module);
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Registered module: " + module.getName() + " in category " + module.getCategory().getDisplayName());
    }

    public static List<Module> getModules() {
        return modules;
    }

    public static List<Module> getModulesByCategory(ModuleCategory category) {
        List<Module> categoryModules = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                categoryModules.add(module);
            }
        }
        return categoryModules;
    }

    // Add this new method to get a specific module by class
    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> moduleClass) {
        for (Module module : modules) {
            if (moduleClass.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }
}