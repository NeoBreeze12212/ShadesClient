package org.neo.shadesclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.WaypointsModule;
import org.neo.shadesclient.qolitems.ModuleManager;

import java.awt.Color;
import java.io.File;
import java.util.UUID;

public class WaypointCommandHandler {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> waypointCommand =
                net.minecraft.server.command.CommandManager.literal("shades")
                        .then(net.minecraft.server.command.CommandManager.literal("waypoint")
                                .then(net.minecraft.server.command.CommandManager.literal("add")
                                        .then(net.minecraft.server.command.CommandManager.argument("name", StringArgumentType.string())
                                                .executes(ctx -> addWaypoint(ctx, StringArgumentType.getString(ctx, "name"), 0xFF0000)) // Default red color
                                                .then(net.minecraft.server.command.CommandManager.argument("color", IntegerArgumentType.integer(0, 0xFFFFFF))
                                                        .executes(ctx -> addWaypoint(ctx, StringArgumentType.getString(ctx, "name"),
                                                                IntegerArgumentType.getInteger(ctx, "color"))))))
                                .then(net.minecraft.server.command.CommandManager.literal("delete")
                                        .then(net.minecraft.server.command.CommandManager.argument("name", StringArgumentType.string())
                                                .executes(ctx -> deleteWaypoint(ctx, StringArgumentType.getString(ctx, "name")))))
                                .then(net.minecraft.server.command.CommandManager.literal("list")
                                        .executes(WaypointCommandHandler::listWaypoints))
                                .then(net.minecraft.server.command.CommandManager.literal("clear")
                                        .executes(WaypointCommandHandler::clearWaypoints))
                                .then(net.minecraft.server.command.CommandManager.literal("help")
                                        .executes(WaypointCommandHandler::showHelp)));

        dispatcher.register(waypointCommand);
        ShadesClient.LOGGER.info("Registered Waypoint commands");
    }

    private static int addWaypoint(CommandContext<ServerCommandSource> context, String name, int color) throws CommandSyntaxException {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;

        BlockPos playerPos = client.player.getBlockPos();
        String worldIdentifier = getCurrentWorldIdentifier();

        WaypointsModule waypointsModule = getWaypointsModule();
        if (waypointsModule == null) {
            sendMessage("§c[Waypoints] §fModule not found or not enabled");
            return 0;
        }

        // Check if a waypoint with this name already exists
        if (waypointsModule.getWaypointByName(name, worldIdentifier) != null) {
            sendMessage("§c[Waypoints] §fA waypoint with name '" + name + "' already exists");
            return 0;
        }

        String dimension = client.world.getRegistryKey().getValue().toString();
        UUID id = waypointsModule.addWaypoint(name, playerPos, dimension, color, worldIdentifier);

        if (id != null) {
            String colorHex = String.format("#%06X", color);
            sendMessage("§a[Waypoints] §fAdded waypoint '" + name + "' at " +
                    playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ() +
                    " with color " + colorHex);
        }

        return 1;
    }

    private static int deleteWaypoint(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        WaypointsModule waypointsModule = getWaypointsModule();
        if (waypointsModule == null) {
            sendMessage("§c[Waypoints] §fModule not found or not enabled");
            return 0;
        }

        String worldIdentifier = getCurrentWorldIdentifier();
        WaypointsModule.Waypoint waypoint = waypointsModule.getWaypointByName(name, worldIdentifier);

        if (waypoint == null) {
            sendMessage("§c[Waypoints] §fNo waypoint with name '" + name + "' found");
            return 0;
        }

        waypointsModule.removeWaypoint(waypoint.getId());
        sendMessage("§a[Waypoints] §fRemoved waypoint '" + name + "'");

        return 1;
    }

    private static int listWaypoints(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        WaypointsModule waypointsModule = getWaypointsModule();
        if (waypointsModule == null) {
            sendMessage("§c[Waypoints] §fModule not found or not enabled");
            return 0;
        }

        String worldIdentifier = getCurrentWorldIdentifier();
        java.util.List<WaypointsModule.Waypoint> waypoints = waypointsModule.getWaypointsInWorld(worldIdentifier);

        if (waypoints.isEmpty()) {
            sendMessage("§a[Waypoints] §fNo waypoints set in this world");
            return 1;
        }

        sendMessage("§a[Waypoints] §fWaypoints in this world:");
        for (WaypointsModule.Waypoint waypoint : waypoints) {
            BlockPos pos = waypoint.getPosition();
            String colorHex = String.format("#%06X", waypoint.getColor());
            sendMessage("§f- §e" + waypoint.getName() + "§f: " +
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                    " [" + waypoint.getDimension() + "] §7(Color: " + colorHex + ")");
        }

        return 1;
    }

    private static int clearWaypoints(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        WaypointsModule waypointsModule = getWaypointsModule();
        if (waypointsModule == null) {
            sendMessage("§c[Waypoints] §fModule not found or not enabled");
            return 0;
        }

        String worldIdentifier = getCurrentWorldIdentifier();
        int count = waypointsModule.clearWaypointsInWorld(worldIdentifier);

        if (count > 0) {
            sendMessage("§a[Waypoints] §fCleared " + count + " waypoints");
        } else {
            sendMessage("§a[Waypoints] §fNo waypoints to clear");
        }

        return 1;
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) {
        sendMessage("§a[Waypoints] §fAvailable commands:");
        sendMessage("§f- §e/shades waypoint add <name> [color]§f: Add a waypoint at your position");
        sendMessage("§f- §e/shades waypoint delete <name>§f: Delete a waypoint by name");
        sendMessage("§f- §e/shades waypoint list§f: List all waypoints in current world");
        sendMessage("§f- §e/shades waypoint clear§f: Clear all waypoints in current world");
        return 1;
    }

    private static WaypointsModule getWaypointsModule() {
        for (org.neo.shadesclient.qolitems.Module module : ModuleManager.getModules()) {
            if (module instanceof WaypointsModule && module.isEnabled()) {
                return (WaypointsModule) module;
            }
        }
        return null;
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

    private static void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
}