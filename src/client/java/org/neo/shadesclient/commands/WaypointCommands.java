package org.neo.shadesclient.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.WaypointsModule;
import org.neo.shadesclient.qolitems.ModuleManager;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.modules.WaypointsModule.Waypoint;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WaypointCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        ShadesClient.LOGGER.info("Registering waypoint commands");

        // Main command: /wp
        dispatcher.register(ClientCommandManager.literal("wp")
                .executes(WaypointCommands::showHelp)

                // /wp add <name> [color]
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                .executes(ctx -> addWaypoint(ctx, 0xFF0000)) // Default red
                                .then(ClientCommandManager.argument("color", IntegerArgumentType.integer(0, 0xFFFFFF))
                                        .executes(ctx -> addWaypoint(ctx, IntegerArgumentType.getInteger(ctx, "color")))
                                )
                        )
                )

                // /wp remove <name>
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                .suggests(WaypointCommands::suggestWaypoints)
                                .executes(WaypointCommands::removeWaypoint)
                        )
                )

                // /wp list
                .then(ClientCommandManager.literal("list")
                        .executes(WaypointCommands::listWaypoints)
                )

                // /wp tp <name>
                .then(ClientCommandManager.literal("tp")
                        .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                .suggests(WaypointCommands::suggestWaypoints)
                                .executes(WaypointCommands::teleportToWaypoint)
                        )
                )

                // /wp clear
                .then(ClientCommandManager.literal("clear")
                        .executes(WaypointCommands::clearWaypoints)
                )

                // /wp toggle
                .then(ClientCommandManager.literal("toggle")
                        .executes(WaypointCommands::toggleWaypointModule)
                )

                // /wp help
                .then(ClientCommandManager.literal("help")
                        .executes(WaypointCommands::showHelp)
                )
        );

        ShadesClient.LOGGER.info("Waypoint commands registered");
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        source.sendFeedback(Text.literal("§6----- Waypoint Commands -----"));
        source.sendFeedback(Text.literal("§e/wp add <name> [color] §7- Add waypoint at current position"));
        source.sendFeedback(Text.literal("§e/wp remove <name> §7- Remove a waypoint"));
        source.sendFeedback(Text.literal("§e/wp list §7- List all waypoints"));
        source.sendFeedback(Text.literal("§e/wp tp <name> §7- Teleport to a waypoint (creative/op only)"));
        source.sendFeedback(Text.literal("§e/wp clear §7- Remove all waypoints"));
        source.sendFeedback(Text.literal("§e/wp toggle §7- Toggle waypoints module"));
        source.sendFeedback(Text.literal("§e/wp help §7- Show this help message"));
        return Command.SINGLE_SUCCESS;
    }

    private static int addWaypoint(CommandContext<FabricClientCommandSource> ctx, int color) {
        String name = StringArgumentType.getString(ctx, "name");
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
            ctx.getSource().sendError(Text.literal("Cannot add waypoint: Player not in world"));
            return 0;
        }

        WaypointsModule waypointsMod = ModuleManager.getModule(WaypointsModule.class);
        if (waypointsMod == null) {
            ctx.getSource().sendError(Text.literal("Waypoints module not found"));
            return 0;
        }

        BlockPos pos = client.player.getBlockPos();
        String dimension = client.world.getRegistryKey().getValue().toString();
        String worldId = getCurrentWorldIdentifier();

        // Check if waypoint with this name already exists
        if (waypointsMod.getWaypointByName(name, worldId) != null) {
            ctx.getSource().sendError(Text.literal("A waypoint with this name already exists"));
            return 0;
        }

        // Add the waypoint
        UUID id = waypointsMod.addWaypoint(name, pos, dimension, color, worldId);
        ctx.getSource().sendFeedback(Text.literal("§aAdded waypoint §f'" + name + "'§a at §f" +
                pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));

        return Command.SINGLE_SUCCESS;
    }

    private static int removeWaypoint(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        String worldId = getCurrentWorldIdentifier();

        WaypointsModule waypointsMod = ModuleManager.getModule(WaypointsModule.class);
        if (waypointsMod == null) {
            ctx.getSource().sendError(Text.literal("Waypoints module not found"));
            return 0;
        }

        Waypoint waypoint = waypointsMod.getWaypointByName(name, worldId);
        if (waypoint == null) {
            ctx.getSource().sendError(Text.literal("Waypoint '" + name + "' not found"));
            return 0;
        }

        waypointsMod.removeWaypoint(waypoint.getId());
        ctx.getSource().sendFeedback(Text.literal("§aRemoved waypoint §f'" + name + "'"));

        return Command.SINGLE_SUCCESS;
    }

    private static int listWaypoints(CommandContext<FabricClientCommandSource> ctx) {
        WaypointsModule waypointsMod = ModuleManager.getModule(WaypointsModule.class);
        if (waypointsMod == null) {
            ctx.getSource().sendError(Text.literal("Waypoints module not found"));
            return 0;
        }

        List<Waypoint> waypoints = waypointsMod.getWaypointsInCurrentDimension();
        if (waypoints.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§eNo waypoints in current dimension"));
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendFeedback(Text.literal("§6Waypoints in current dimension:"));
        for (Waypoint waypoint : waypoints) {
            BlockPos pos = waypoint.getPosition();
            String colorHex = String.format("#%06X", waypoint.getColor());

            ctx.getSource().sendFeedback(Text.literal("§f" + waypoint.getName() + "§7: " +
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                    " §8[" + colorHex + "]"));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int teleportToWaypoint(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
            ctx.getSource().sendError(Text.literal("Cannot teleport: Player not in world"));
            return 0;
        }

        // Check if player is in creative mode or is op (server operator)
        boolean canTeleport = client.player.isCreative() || client.player.hasPermissionLevel(2);
        if (!canTeleport) {
            ctx.getSource().sendError(Text.literal("Teleport requires creative mode or operator status"));
            return 0;
        }

        WaypointsModule waypointsMod = ModuleManager.getModule(WaypointsModule.class);
        if (waypointsMod == null) {
            ctx.getSource().sendError(Text.literal("Waypoints module not found"));
            return 0;
        }

        String worldId = getCurrentWorldIdentifier();
        Waypoint waypoint = waypointsMod.getWaypointByName(name, worldId);
        if (waypoint == null) {
            ctx.getSource().sendError(Text.literal("Waypoint '" + name + "' not found"));
            return 0;
        }

        // Check if the waypoint is in the current dimension
        String currentDimension = client.world.getRegistryKey().getValue().toString();
        if (!waypoint.getDimension().equals(currentDimension)) {
            ctx.getSource().sendError(Text.literal("Cannot teleport: Waypoint is in a different dimension"));
            return 0;
        }

        // Teleport the player
        BlockPos pos = waypoint.getPosition();
        client.player.updatePosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ctx.getSource().sendFeedback(Text.literal("§aTeleported to §f'" + name + "'"));

        return Command.SINGLE_SUCCESS;
    }

    private static int clearWaypoints(CommandContext<FabricClientCommandSource> ctx) {
        WaypointsModule waypointsMod = ModuleManager.getModule(WaypointsModule.class);
        if (waypointsMod == null) {
            ctx.getSource().sendError(Text.literal("Waypoints module not found"));
            return 0;
        }

        String worldId = getCurrentWorldIdentifier();
        int count = waypointsMod.clearWaypointsInWorld(worldId);

        if (count > 0) {
            ctx.getSource().sendFeedback(Text.literal("§aCleared §f" + count + "§a waypoints"));
        } else {
            ctx.getSource().sendFeedback(Text.literal("§eNo waypoints to clear"));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int toggleWaypointModule(CommandContext<FabricClientCommandSource> ctx) {
        WaypointsModule waypointsMod = ModuleManager.getModule(WaypointsModule.class);
        if (waypointsMod == null) {
            ctx.getSource().sendError(Text.literal("Waypoints module not found"));
            return 0;
        }

        waypointsMod.toggle();
        boolean enabled = waypointsMod.isEnabled();

        ctx.getSource().sendFeedback(Text.literal("Waypoints module " +
                (enabled ? "§aenabled" : "§cdisabled")));

        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestWaypoints(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        WaypointsModule waypointsMod = ModuleManager.getModule(WaypointsModule.class);
        if (waypointsMod == null) {
            return builder.buildFuture();
        }

        String worldId = getCurrentWorldIdentifier();
        List<Waypoint> waypoints = waypointsMod.getWaypointsInWorld(worldId);

        String remaining = builder.getRemaining().toLowerCase();
        for (Waypoint waypoint : waypoints) {
            String name = waypoint.getName();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
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
}