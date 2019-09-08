/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.utilities.*;
import net.TheElm.project.utilities.WarpUtils.Warp;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.dimension.DimensionType;

import java.util.Collection;
import java.util.UUID;

public final class TeleportsCommand {
    private static final DynamicCommandExceptionType PLAYER_NOT_IN_SPAWN = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text((ServerPlayerEntity) player, "warp.notice.player.outside_spawn").formatted(Formatting.RED)
    );
    private static final DynamicCommandExceptionType TARGET_NOT_IN_SPAWN = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text((ServerPlayerEntity) player, "warp.notice.target.outside_spawn").formatted(Formatting.RED)
    );
    private static final DynamicCommandExceptionType TARGET_NOT_REQUESTING = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text((ServerPlayerEntity) player, "warp.notice.no_request").formatted(Formatting.RED)
    );
    private static final DynamicCommandExceptionType TARGET_NO_WARP = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text((ServerPlayerEntity) player, "warp.notice.no_warp").formatted(Formatting.RED)
    );
    private static final DynamicCommandExceptionType TARGET_NOT_ONLINE = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text((ServerPlayerEntity) player, "warp.notice.offline").formatted(Formatting.RED)
    );
    
    private TeleportsCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register( CommandManager.literal("spawn")
            .requires((source) -> source.hasPermissionLevel(SewingMachineConfig.INSTANCE.CLAIM_OP_LEVEL_SPAWN.get()))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .executes((context) -> {
                    // Get location information
                    ServerCommandSource source = context.getSource();
                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers( context, "player" );
                    ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
                    
                    for ( ServerPlayerEntity player : players ) {
                        WarpUtils.teleportPlayer(world, player, WarpUtils.getWorldSpawn(world));
                    }
                    
                    Text spawnText = new LiteralText("Spawn").formatted(Formatting.GOLD);
                    if (players.size() == 1) {
                        source.sendFeedback(new TranslatableText("commands.teleport.success.entity.single", ((Entity)players.iterator().next()).getDisplayName(), spawnText), true);
                    } else {
                        source.sendFeedback(new TranslatableText("commands.teleport.success.entity.multiple", players.size(), spawnText), true);
                    }
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
            .executes((context) -> {
                // Get location information
                ServerCommandSource source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
                
                WarpUtils.teleportPlayer(world, player, WarpUtils.getWorldSpawn( world ));
                
                source.sendFeedback(new TranslatableText("commands.teleport.success.entity.single", player.getDisplayName(), new LiteralText("Spawn").formatted(Formatting.GOLD)), true);
                
                return Command.SINGLE_SUCCESS;
            })
        );
        CoreMod.logDebug( "- Registered Spawn command" );
        
        if (SewingMachineConfig.INSTANCE.COMMAND_WARP_TPA.get()) {
            dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtilities::getAllPlayerNames)
                    .executes(TeleportsCommand::tpaCommand)
                )
            );
            CoreMod.logDebug("- Registered TPA command");
            
            dispatcher.register(CommandManager.literal("tpaccept")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .requires(TeleportsCommand::sourceHasWarp)
                    .executes(TeleportsCommand::tpAcceptCommand)
                )
            );
            CoreMod.logDebug("- Registered TPAccept command");
            
            dispatcher.register(CommandManager.literal("tpdeny")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .requires(TeleportsCommand::sourceHasWarp)
                    .executes(TeleportsCommand::tpDenyCommand)
                )
            );
            CoreMod.logDebug("- Registered TPDeny command");
        }
    }
    
    private static boolean sourceHasWarp(final ServerCommandSource source) {
        try {
            return sourceHasWarp( source.getPlayer() );
        } catch (CommandSyntaxException e) {
            CoreMod.logError( e );
        }
        return false;
    }
    private static boolean sourceHasWarp(final ServerPlayerEntity player) {
        return WarpUtils.getPlayerWarp( player.getUuid() ) != null;
    }
    private static int tpaCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity porter = source.getPlayer();
        
        // Get the reference of the player to request a teleport to
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Check if player is within spawn
        if (!ChunkUtils.isPlayerWithinSpawn( porter ))
            throw PLAYER_NOT_IN_SPAWN.create( porter );
        
        Warp warp;
        // If the player to teleport to does not have a warp
        if ((warp = WarpUtils.getPlayerWarp( target.getId() )) == null)
            throw TARGET_NO_WARP.create( porter );
        
        final MinecraftServer server = source.getMinecraftServer();
        final PlayerManager manager = server.getPlayerManager();
        
        ServerPlayerEntity targetPlayer = manager.getPlayer( target.getId() );
        
        // Accept the teleport automatically
        if ( ChunkUtils.canPlayerWarpTo( porter, target.getId() ) ) {
            WarpUtils.teleportPlayer( warp, porter );
            
            CoreMod.logInfo(porter.getName().asString() + " was teleported to " + (porter.getUuid().equals(target.getId()) ? "their" : target.getName() + "'s") + " warp");
            
            // Notify the player
            if ((targetPlayer != null) && (!target.getId().equals(porter.getUuid()))) {
                TitleUtils.showPlayerAlert(
                    targetPlayer,
                    Formatting.YELLOW,
                    TranslatableServerSide.text( targetPlayer, "warp.notice.player", porter.getDisplayName() )
                );
                targetPlayer.playSound(SoundEvents.UI_TOAST_IN, SoundCategory.MASTER, 1.0f, 1.0f);
            }
            
        } else {
            // If player not online
            if (targetPlayer == null)
                throw TeleportsCommand.TARGET_NOT_ONLINE.create( porter );
            
            // Add the player to the list of invitations
            CoreMod.PLAYER_WARP_INVITES.put(porter, target.getId());
            
            CoreMod.logInfo(porter.getName().asString() + " has requested to teleport to " + (porter.getUuid().equals(target.getId()) ? "their" : target.getName() + "'s") + " warp");
            
            // Notify the target
            targetPlayer.sendMessage(porter.getName().formatted(Formatting.AQUA)
                .append(new LiteralText(" sent you a TP request, Click ").formatted(Formatting.YELLOW)
                    .append(new LiteralText("here to accept it").formatted(Formatting.GREEN).styled(
                        (consumer) -> consumer.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + porter.getName().asString()))))
                    .append(", or ")
                    .append(new LiteralText("here to deny it").formatted(Formatting.RED).styled(
                        (consumer) -> consumer.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + porter.getName().asString()))))
                    .append(".")
                ));
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int tpAcceptCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        UUID warpToUUID;
        if ((( warpToUUID = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!target.getUuid().equals( warpToUUID )) )
            throw TARGET_NOT_REQUESTING.create( target );
        
        if (!ChunkUtils.isPlayerWithinSpawn( porter )) {
            porter.sendMessage(new LiteralText( "Your warp could not be completed, you must be within spawn to warp." ).formatted(Formatting.RED));
            throw TARGET_NOT_IN_SPAWN.create( target );
        }
        
        Warp warp = WarpUtils.getPlayerWarp( target.getUuid() );
        WarpUtils.teleportPlayer( warp, porter );
        
        CoreMod.logInfo(porter.getName().asString() + " was teleported to " + (porter.getUuid().equals(target.getUuid()) ? "their" : target.getName().asString() + "'s") + " warp");
        
        CoreMod.PLAYER_WARP_INVITES.remove( porter );
        return Command.SINGLE_SUCCESS;
    }
    private static int tpDenyCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        UUID warpToUUID;
        if ((( warpToUUID = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!target.getUuid().equals( warpToUUID )) )
            throw TARGET_NOT_REQUESTING.create( target );
        
        CoreMod.logInfo( porter.getName().asString() + "'s teleport was rejected by " + target.getName().asString() );
        
        CoreMod.PLAYER_WARP_INVITES.remove( porter );
        return Command.SINGLE_SUCCESS;
    }
    
}
