package org.neo.shadesclient.qolitems;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private boolean enabled;

    public Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    protected abstract void onEnable();
    protected abstract void onDisable();

    // Optional method for modules that need configuration
    public boolean hasConfigScreen() {
        return false;
    }

    // Optional method for modules that need configuration
    public void openConfigScreen() {
        // Override in subclasses if needed
    }
}
