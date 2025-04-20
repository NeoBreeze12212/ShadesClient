package org.neo.shadesclient.qolitems;

public class TestModule extends Module {

    public TestModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        // Log that the module was enabled
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module enabled");
    }

    @Override
    protected void onDisable() {
        // Log that the module was disabled
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module disabled");
    }
}