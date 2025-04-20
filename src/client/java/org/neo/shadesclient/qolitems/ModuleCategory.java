package org.neo.shadesclient.qolitems;

public enum ModuleCategory {
    SURVIVAL_QOL("Survival QOL"),
    COMBAT("Utilities"),
    VISUALS("Visuals"),
    GAMEPLAY("Gameplay");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}