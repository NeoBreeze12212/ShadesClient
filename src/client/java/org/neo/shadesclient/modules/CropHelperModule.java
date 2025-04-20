package org.neo.shadesclient.modules;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.Module;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class CropHelperModule extends Module {
    private static final int SCAN_RANGE = 16;
    private static final int SCAN_HEIGHT = 5;
    private static final int MAX_BLOCKS_PER_TICK = 100;
    private boolean showOutlines = true;
    private boolean showParticles = false;

    // Queue for block scanning to spread over multiple ticks
    private final Queue<BlockPos> scanQueue = new ArrayDeque<>();
    // Set to store mature crop positions for rendering
    private final Set<BlockPos> matureCrops = new HashSet<>();
    private boolean isScanning = false;
    private long lastScanTime = 0;
    private static final long SCAN_COOLDOWN = 5000; // 5 seconds

    public CropHelperModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module enabled");
        matureCrops.clear();
    }

    @Override
    protected void onDisable() {
        org.neo.shadesclient.client.ShadesClient.LOGGER.info(getName() + " module disabled");
        scanQueue.clear();
        matureCrops.clear();
        isScanning = false;
    }

    // Called each tick by the EventHandler
    public void tick() {
        if (!isEnabled()) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            long currentTime = System.currentTimeMillis();

            // Start scanning if not already scanning and cooldown has passed
            if (!isScanning && scanQueue.isEmpty() && (currentTime - lastScanTime > SCAN_COOLDOWN)) {
                startScan(client);
                lastScanTime = currentTime;
            }

            // Process some blocks from queue
            if (isScanning) {
                processBlocksFromQueue(client.world);
            }
        } catch (Exception e) {
            org.neo.shadesclient.client.ShadesClient.LOGGER.error("Error in CropHelperModule tick: " + e.getMessage());
        }
    }

    // Called during world render by the EventHandler
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
        if (!isEnabled() || !showOutlines || matureCrops.isEmpty()) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            // Begin rendering mature crops
            for (BlockPos pos : matureCrops) {
                // Skip blocks that are too far away (optimization)
                if (pos.getSquaredDistance(client.player.getBlockPos()) > 256) {
                    continue;
                }

                // Draw outline around mature crop
                drawCropOutline(matrixStack, vertexConsumers, pos, cameraX, cameraY, cameraZ);
            }
        } catch (Exception e) {
            org.neo.shadesclient.client.ShadesClient.LOGGER.error("Error in CropHelperModule render: " + e.getMessage());
        }
    }

    private void drawCropOutline(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers,
                                 BlockPos pos, double cameraX, double cameraY, double cameraZ) {
        // Push matrix state
        matrixStack.push();

        // Translate to block position relative to camera
        matrixStack.translate(
                pos.getX() - cameraX,
                pos.getY() - cameraY,
                pos.getZ() - cameraZ
        );

        // Get matrix for rendering
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        // Create crop box - slightly smaller than a full block
        Box box = new Box(0.1, 0.0, 0.1, 0.9, 1.0, 0.9);

        // Render box outline
        renderOutline(matrix, vertexConsumers, box, 0.0f, 1.0f, 0.0f, 1.0f);

        // Pop matrix state
        matrixStack.pop();
    }

    private void renderOutline(Matrix4f matrix, VertexConsumerProvider vertexConsumers,
                               Box box, float red, float green, float blue, float alpha) {
        // For actual implementation, this would use WorldRenderer.drawBox
        // or a custom vertex consumer to draw the box
        org.neo.shadesclient.client.ShadesClient.LOGGER.debug("Drawing crop outline");
    }

    private void startScan(MinecraftClient client) {
        BlockPos playerPos = client.player.getBlockPos();
        scanQueue.clear();
        matureCrops.clear();

        // Add blocks in range to scan queue
        for (int x = -SCAN_RANGE; x <= SCAN_RANGE; x++) {
            for (int z = -SCAN_RANGE; z <= SCAN_RANGE; z++) {
                for (int y = -SCAN_HEIGHT; y <= SCAN_HEIGHT; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    scanQueue.add(pos);
                }
            }
        }

        isScanning = true;
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Started crop scan. Queued " + scanQueue.size() + " blocks");
    }

    private void processBlocksFromQueue(World world) {
        int processed = 0;
        while (!scanQueue.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            BlockPos pos = scanQueue.poll();
            processed++;

            try {
                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                // Check if this is a crop block and is mature
                if (isMatureCrop(state, block)) {
                    // Store position for rendering
                    matureCrops.add(pos.toImmutable());
                    org.neo.shadesclient.client.ShadesClient.LOGGER.debug("Found mature crop at " + pos);
                }
            } catch (Exception e) {
                // Skip this block on error
                continue;
            }
        }

        if (scanQueue.isEmpty()) {
            isScanning = false;
            org.neo.shadesclient.client.ShadesClient.LOGGER.info("Crop scan completed. Found " + matureCrops.size() + " mature crops");
        }
    }

    private boolean isMatureCrop(BlockState state, Block block) {
        try {
            // Check different crop types
            if (block instanceof CropBlock) {
                CropBlock cropBlock = (CropBlock) block;
                return cropBlock.isMature(state);
            } else if (block instanceof StemBlock) {
                // Pumpkin and melon stems
                int age = state.get(StemBlock.AGE);
                return age >= 7;
            } else if (block instanceof SweetBerryBushBlock) {
                int age = state.get(SweetBerryBushBlock.AGE);
                return age >= 3;
            } else if (block instanceof NetherWartBlock) {
                int age = state.get(NetherWartBlock.AGE);
                return age >= 3;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        // Would implement configuration screen for display settings
        org.neo.shadesclient.client.ShadesClient.LOGGER.info("Opening config for " + getName());
    }
}