package org.neo.shadesclient.modules;

import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.qolitems.ModuleCategory;

public class WaypointsModule extends Module {

    public WaypointsModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        // Coming Soon - No functionality implemented yet
    }

    @Override
    protected void onDisable() {
        // Coming Soon - No functionality implemented yet
    }

    @Override
    public boolean hasConfigScreen() {
        return false;
    }

    @Override
    public void openConfigScreen() {
        // No config screen implemented yet
    }

    // For rendering "Coming Soon" text when the module is active
    public void render() {
        // This method would be called from your rendering system
        // You would implement the actual rendering of "Coming Soon" text here
        // Example (pseudo-code):
        // FontRenderer.drawString("Coming Soon", x, y, color);
    }
}