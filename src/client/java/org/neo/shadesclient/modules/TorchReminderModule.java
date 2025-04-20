package org.neo.shadesclient.modules;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

public class TorchReminderModule extends Module {
    private static final int LIGHT_LEVEL_THRESHOLD = 7;
    private static final long NOTIFICATION_COOLDOWN = 5000; // 5 seconds in ms
    private long lastNotificationTime = 0;

    public TorchReminderModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module enabled");
    }

    @Override
    protected void onDisable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module disabled");
    }

    public void checkLightLevel() {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime < NOTIFICATION_COOLDOWN) return;

        BlockPos playerPos = client.player.getBlockPos();
        int blockLight = client.world.getLightLevel(LightType.BLOCK, playerPos);
        int skyLight = client.world.getLightLevel(LightType.SKY, playerPos);
        int effectiveLight = Math.max(blockLight, skyLight - client.world.getAmbientDarkness());

        // Check if light level is below threshold and player is not in water/lava
        if (effectiveLight < LIGHT_LEVEL_THRESHOLD && !client.player.isSubmergedInWater() && !client.player.isInLava()) {
            client.player.sendMessage(Text.literal("§c[Torch Reminder] §fLight level is " + effectiveLight + ". Consider placing a torch!"), true);
            lastNotificationTime = currentTime;
        }
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        // Would implement configuration screen for light level threshold and notification cooldown
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}