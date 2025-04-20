package org.neo.shadesclient.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.modules.configs.WaypointConfigScreen;

import net.minecraft.client.render.*;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class WaypointsModule extends Module {
    // Map of worldId -> waypoints list
    private final Map<String, Map<UUID, Waypoint>> worldWaypoints = new HashMap<>();

    // Configuration options
    private boolean showDistance = true;
    private boolean showCoordinates = true;
    private int maxRenderDistance = 256;
    private boolean renderBeacons = true;
    private boolean renderLabels = true;
    private float beaconHeight = 100.0f;
    private float labelScale = 1.0f;

    // File to save waypoints
    private final File waypointsFile;
    private final Gson gson;

    public WaypointsModule(String name, String description, ModuleCategory category) {
        super(name, description, category);

        // Set up waypoints saving
        File configDir = new File(MinecraftClient.getInstance().runDirectory, "config/shadesclient");
        if (!configDir.exists() && !configDir.mkdirs()) {
            ShadesClient.LOGGER.error("Failed to create config directory");
        }

        waypointsFile = new File(configDir, "waypoints.json");
        gson = new GsonBuilder().setPrettyPrinting().create();

        // Load existing waypoints
        loadWaypoints();
    }

    @Override
    protected void onEnable() {
        ShadesClient.LOGGER.info(getName() + " module enabled");
        loadWaypoints();
    }

    @Override
    protected void onDisable() {
        ShadesClient.LOGGER.info(getName() + " module disabled");
        saveWaypoints();
    }

    // Called when rendering the world
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers,
                       double cameraX, double cameraY, double cameraZ) {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Get current world identifier
        String worldId = getCurrentWorldIdentifier();
        if (!worldWaypoints.containsKey(worldId)) return;

        // Get current dimension
        String currentDimension = client.world.getRegistryKey().getValue().toString();

        // Draw each waypoint in this world that's in the current dimension
        for (Waypoint waypoint : worldWaypoints.get(worldId).values()) {
            if (!waypoint.isVisible() || !waypoint.getDimension().equals(currentDimension)) continue;

            BlockPos pos = waypoint.getPosition();

            // Calculate distance to player
            double dx = pos.getX() + 0.5 - client.player.getX();
            double dy = pos.getY() + 0.5 - client.player.getY();
            double dz = pos.getZ() + 0.5 - client.player.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Skip if too far away
            if (distance > maxRenderDistance) continue;

            // Draw the waypoint
            matrixStack.push();
            matrixStack.translate(pos.getX() - cameraX + 0.5, pos.getY() - cameraY + 0.5, pos.getZ() - cameraZ + 0.5);

            // Draw beacon
            if (renderBeacons) {
                renderBeam(matrixStack, vertexConsumers, waypoint.getColor(), beaconHeight);
            }

            // Draw label
            if (renderLabels) {
                matrixStack.push();

                // Make label face the camera
                float yaw = (float) Math.atan2(dz, dx);
                matrixStack.multiply(new org.joml.Quaternionf().rotationY(-yaw));

                // Scale the label
                float scale = labelScale * 0.025f; // Base scale
                matrixStack.scale(-scale, -scale, scale);

                Matrix4f matrix = matrixStack.peek().getPositionMatrix();
                int labelColor = waypoint.getColor();

                // Render label with shadow
                client.textRenderer.draw(waypoint.getName(), -client.textRenderer.getWidth(waypoint.getName()) / 2f,
                        -client.textRenderer.fontHeight, labelColor, true, matrix, vertexConsumers,
                        net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 15728880);

                // Add distance if enabled
                if (showDistance) {
                    String distText = String.format("%.1fm", distance);
                    client.textRenderer.draw(distText, -client.textRenderer.getWidth(distText) / 2f,
                            client.textRenderer.fontHeight, 0xFFFFFFFF, true, matrix, vertexConsumers,
                            net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 15728880);
                }

                // Add coordinates if enabled
                if (showCoordinates) {
                    String coordText = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                    client.textRenderer.draw(coordText, -client.textRenderer.getWidth(coordText) / 2f,
                            client.textRenderer.fontHeight * 2, 0xFFFFFFFF, true, matrix, vertexConsumers,
                            net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 15728880);
                }

                matrixStack.pop();
            }

            matrixStack.pop();
        }
    }

    private void renderBeam(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int color, float height) {
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        float alpha = 0.5f;

        float size = 0.15f;

        // Use VertexConsumer directly from the provider
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Draw vertical lines
        vertexConsumer.vertex(matrix, -size, 0, -size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, -size, height, -size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, size, 0, -size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, size, height, -size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, size, 0, size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, size, height, size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, -size, 0, size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, -size, height, size).color(red, green, blue, alpha).normal(0, 1, 0);

        // Top horizontal lines
        vertexConsumer.vertex(matrix, -size, height, -size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, size, height, -size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, size, height, -size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, size, height, size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, size, height, size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, -size, height, size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, -size, height, size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, -size, height, -size).color(red, green, blue, alpha).normal(0, 1, 0);

        // Bottom horizontal lines
        vertexConsumer.vertex(matrix, -size, 0, -size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, size, 0, -size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, size, 0, -size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, size, 0, size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, size, 0, size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, -size, 0, size).color(red, green, blue, alpha).normal(0, 1, 0);

        vertexConsumer.vertex(matrix, -size, 0, size).color(red, green, blue, alpha).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, -size, 0, -size).color(red, green, blue, alpha).normal(0, 1, 0);

        // No need to call draw() - the VertexConsumerProvider handles this
    }

    public UUID addWaypoint(String name, BlockPos pos, String dimension, int color, String worldId) {
        UUID id = UUID.randomUUID();
        Waypoint waypoint = new Waypoint(id, name, pos, dimension, color);

        // Ensure this world has a waypoints map
        worldWaypoints.computeIfAbsent(worldId, k -> new HashMap<>());

        // Add the waypoint
        worldWaypoints.get(worldId).put(id, waypoint);

        ShadesClient.LOGGER.info("Added waypoint: " + name + " at " + pos + " in " + dimension + " for world " + worldId);

        // Save to file
        saveWaypoints();

        return id;
    }

    public void removeWaypoint(UUID id) {
        for (Map<UUID, Waypoint> waypointsInWorld : worldWaypoints.values()) {
            Waypoint removed = waypointsInWorld.remove(id);
            if (removed != null) {
                ShadesClient.LOGGER.info("Removed waypoint: " + removed.name);
                saveWaypoints();
                return;
            }
        }
    }

    public List<Waypoint> getWaypoints() {
        List<Waypoint> allWaypoints = new ArrayList<>();
        for (Map<UUID, Waypoint> waypointsInWorld : worldWaypoints.values()) {
            allWaypoints.addAll(waypointsInWorld.values());
        }
        return allWaypoints;
    }

    public List<Waypoint> getWaypointsInCurrentDimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return new ArrayList<>();
        }

        String currentDimension = client.world.getRegistryKey().getValue().toString();
        String worldId = getCurrentWorldIdentifier();

        if (!worldWaypoints.containsKey(worldId)) {
            return new ArrayList<>();
        }

        List<Waypoint> result = new ArrayList<>();
        for (Waypoint waypoint : worldWaypoints.get(worldId).values()) {
            if (waypoint.dimension.equals(currentDimension)) {
                result.add(waypoint);
            }
        }

        return result;
    }

    public List<Waypoint> getWaypointsInWorld(String worldId) {
        if (!worldWaypoints.containsKey(worldId)) {
            return new ArrayList<>();
        }

        return new ArrayList<>(worldWaypoints.get(worldId).values());
    }

    public Waypoint getWaypointByName(String name, String worldId) {
        if (!worldWaypoints.containsKey(worldId)) {
            return null;
        }

        for (Waypoint waypoint : worldWaypoints.get(worldId).values()) {
            if (waypoint.name.equalsIgnoreCase(name)) {
                return waypoint;
            }
        }

        return null;
    }

    public int clearWaypointsInWorld(String worldId) {
        if (!worldWaypoints.containsKey(worldId)) {
            return 0;
        }

        int count = worldWaypoints.get(worldId).size();
        worldWaypoints.get(worldId).clear();
        saveWaypoints();

        return count;
    }

    private String getCurrentWorldIdentifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            // Single player world
            try {
                // Try to get the world name directly
                return "singleplayer:" + client.getServer().getSaveProperties().getLevelName();
            } catch (Exception e) {
                // Fallback method using file path
                return "singleplayer:" + new File(String.valueOf(client.getServer().getRunDirectory()), "saves").listFiles()[0].getName();
            }
        } else if (client.getCurrentServerEntry() != null) {
            // Multiplayer server
            return "server:" + client.getCurrentServerEntry().address;
        }
        return "unknown";
    }

    // Save waypoints to file
    private void saveWaypoints() {
        try {
            // Convert to a serializable format
            Map<String, List<WaypointData>> serializedData = new HashMap<>();

            for (Map.Entry<String, Map<UUID, Waypoint>> entry : worldWaypoints.entrySet()) {
                String worldId = entry.getKey();
                List<WaypointData> waypointDataList = entry.getValue().values().stream()
                        .map(waypoint -> new WaypointData(
                                waypoint.id.toString(),
                                waypoint.name,
                                waypoint.position.getX(),
                                waypoint.position.getY(),
                                waypoint.position.getZ(),
                                waypoint.dimension,
                                waypoint.color,
                                waypoint.visible
                        ))
                        .collect(Collectors.toList());

                serializedData.put(worldId, waypointDataList);
            }

            try (FileWriter writer = new FileWriter(waypointsFile)) {
                gson.toJson(serializedData, writer);
                ShadesClient.LOGGER.info("Saved waypoints to file");
            }
        } catch (IOException e) {
            ShadesClient.LOGGER.error("Failed to save waypoints: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load waypoints from file
    @SuppressWarnings("unchecked")
    private void loadWaypoints() {
        if (!waypointsFile.exists()) {
            ShadesClient.LOGGER.info("No waypoints file found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(waypointsFile)) {
            Type type = new TypeToken<Map<String, List<WaypointData>>>(){}.getType();
            Map<String, List<WaypointData>> serializedData = gson.fromJson(reader, type);

            // Clear current waypoints
            worldWaypoints.clear();

            // Populate from file
            if (serializedData != null) {
                for (Map.Entry<String, List<WaypointData>> entry : serializedData.entrySet()) {
                    String worldId = entry.getKey();
                    Map<UUID, Waypoint> worldWaypointMap = new HashMap<>();

                    for (WaypointData data : entry.getValue()) {
                        UUID id = UUID.fromString(data.id);
                        BlockPos pos = new BlockPos(data.x, data.y, data.z);
                        Waypoint waypoint = new Waypoint(id, data.name, pos, data.dimension, data.color);
                        waypoint.setVisible(data.visible);

                        worldWaypointMap.put(id, waypoint);
                    }

                    worldWaypoints.put(worldId, worldWaypointMap);
                }
            }

            ShadesClient.LOGGER.info("Loaded waypoints from file: " + getTotalWaypointCount() + " waypoints across " + worldWaypoints.size() + " worlds");
        } catch (IOException e) {
            ShadesClient.LOGGER.error("Failed to load waypoints: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getTotalWaypointCount() {
        int count = 0;
        for (Map<UUID, Waypoint> waypointsInWorld : worldWaypoints.values()) {
            count += waypointsInWorld.size();
        }
        return count;
    }

    public static class Waypoint {
        private final UUID id;
        private String name;
        private BlockPos position;
        private final String dimension;
        private int color;
        private boolean visible = true;

        public Waypoint(UUID id, String name, BlockPos position, String dimension, int color) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.dimension = dimension;
            this.color = color;
        }

        // Getters and setters
        public UUID getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BlockPos getPosition() { return position; }
        public void setPosition(BlockPos position) { this.position = position; }
        public String getDimension() { return dimension; }
        public int getColor() { return color; }
        public void setColor(int color) { this.color = color; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
    }

    // Serializable waypoint data for saving/loading
    private static class WaypointData {
        private final String id;
        private final String name;
        private final int x;
        private final int y;
        private final int z;
        private final String dimension;
        private final int color;
        private final boolean visible;

        public WaypointData(String id, String name, int x, int y, int z, String dimension, int color, boolean visible) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.color = color;
            this.visible = visible;
        }
    }

    // Configuration getters/setters
    public boolean isShowDistance() { return showDistance; }
    public void setShowDistance(boolean showDistance) { this.showDistance = showDistance; }

    public boolean isShowCoordinates() { return showCoordinates; }
    public void setShowCoordinates(boolean showCoordinates) { this.showCoordinates = showCoordinates; }

    public int getMaxRenderDistance() { return maxRenderDistance; }
    public void setMaxRenderDistance(int maxRenderDistance) { this.maxRenderDistance = maxRenderDistance; }

    public boolean isRenderBeacons() { return renderBeacons; }
    public void setRenderBeacons(boolean renderBeacons) { this.renderBeacons = renderBeacons; }

    public boolean isRenderLabels() { return renderLabels; }
    public void setRenderLabels(boolean renderLabels) { this.renderLabels = renderLabels; }

    public float getBeaconHeight() { return beaconHeight; }
    public void setBeaconHeight(float beaconHeight) { this.beaconHeight = beaconHeight; }

    public float getLabelScale() { return labelScale; }
    public void setLabelScale(float labelScale) { this.labelScale = labelScale; }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        MinecraftClient.getInstance().setScreen(new WaypointConfigScreen(this));
        ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}