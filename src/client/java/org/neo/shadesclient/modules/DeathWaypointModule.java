package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

import java.util.ArrayList;
import java.util.List;

public class DeathWaypointModule extends Module {
    private final List<DeathPoint> deathPoints = new ArrayList<>();
    private boolean lastTickAlive = true;

    public DeathWaypointModule(String name, String description, ModuleCategory category) {
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

    public void checkPlayerStatus() {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Check if player is dead and was alive last tick
        boolean isAlive = client.player.getHealth() > 0;
        if (lastTickAlive && !isAlive) {
            // Player just died, create a waypoint
            createDeathWaypoint(client);
        }
        lastTickAlive = isAlive;
    }

    private void createDeathWaypoint(MinecraftClient client) {
        BlockPos deathPos = client.player.getBlockPos();
        String dimensionId = client.world.getRegistryKey().getValue().toString();
        long timestamp = System.currentTimeMillis();

        DeathPoint deathPoint = new DeathPoint(deathPos, dimensionId, timestamp);
        deathPoints.add(deathPoint);

        // Display death coordinates to player
        String message = String.format("§c[Death Waypoint] §fYou died at §6X: %d, Y: %d, Z: %d§f in §6%s",
                deathPos.getX(), deathPos.getY(), deathPos.getZ(), getDimensionName(dimensionId));

        // Schedule a delayed task to show message after respawn
        scheduleMessageAfterRespawn(message);

        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Created death waypoint at " + deathPos + " in " + dimensionId);
    }

    private void scheduleMessageAfterRespawn(String message) {
        // I'll try to implement better sys later
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Will display after respawn: " + message);
    }

    private String getDimensionName(String dimensionId) {
        if (dimensionId.contains("overworld")) return "Overworld";
        if (dimensionId.contains("the_nether")) return "Nether";
        if (dimensionId.contains("the_end")) return "End";
        return dimensionId;
    }

    private static class DeathPoint {
        private final BlockPos position;
        private final String dimension;
        private final long timestamp;

        public DeathPoint(BlockPos position, String dimension, long timestamp) {
            this.position = position;
            this.dimension = dimension;
            this.timestamp = timestamp;
        }
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        // I hate this bro
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}