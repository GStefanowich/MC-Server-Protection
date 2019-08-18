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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.TheElm.project.CoreMod;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.WarpUtils;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.Collection;
import java.util.UUID;

public final class TeleportsCommand {
    private static final SimpleCommandExceptionType PLAYER_NOT_IN_SPAWN = new SimpleCommandExceptionType(
        new LiteralText("Warping is only allowed within spawn.").formatted(Formatting.RED)
    );
    private static final SimpleCommandExceptionType TARGET_NOT_IN_SPAWN = new SimpleCommandExceptionType(
        new LiteralText("That player isn't in spawn.").formatted(Formatting.RED)
    );
    private static final SimpleCommandExceptionType TARGET_NOT_REQUESTING = new SimpleCommandExceptionType(
        new LiteralText("That player isn't trying to teleport to you.").formatted(Formatting.RED)
    );
    private static final SimpleCommandExceptionType TARGET_NO_WARP = new SimpleCommandExceptionType(
        new LiteralText("That player doesn't have a warp.").formatted(Formatting.RED)
    );
    
    private TeleportsCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register( CommandManager.literal("spawn")
            .requires((source) -> source.hasPermissionLevel( 1 ))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .executes((context) -> {
                    // Get location information
                    ServerCommandSource source = context.getSource();
                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers( context, "player" );
                    ServerWorld world = source.getMinecraftServer().getWorld(DimensionType.OVERWORLD);
                    
                    for ( ServerPlayerEntity player : players ) {
                        WarpUtils.teleportPlayer(world, player, WarpUtils.getWorldSpawn(world));
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
                return Command.SINGLE_SUCCESS;
            })
        );
        CoreMod.logDebug( "- Registered Spawn command" );
        
        dispatcher.register( CommandManager.literal("tpa")
            .then( CommandManager.argument( "player", EntityArgumentType.player() )
                .executes(TeleportsCommand::tpaCommand)
            )
        );
        CoreMod.logDebug( "- Registered TPA command" );
        
        dispatcher.register( CommandManager.literal("tpaccept")
            .then( CommandManager.argument( "player", EntityArgumentType.player() )
                .requires(TeleportsCommand::sourceHasWarp)
                .executes(TeleportsCommand::tpAcceptCommand)
            )
        );
        CoreMod.logDebug( "- Registered TPAccept command" );
        
        dispatcher.register( CommandManager.literal("tpdeny")
            .then( CommandManager.argument( "player", EntityArgumentType.player() )
                .requires(TeleportsCommand::sourceHasWarp)
                .executes(TeleportsCommand::tpDenyCommand)
            )
        );
        CoreMod.logDebug( "- Registered TPDeny command" );
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
        return ((PlayerData) player).getWarpPos() != null;
    }
    private static int tpaCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity porter = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        
        // Check if player is within spawn
        if (!ChunkUtils.isPlayerWithinSpawn( porter ))
            throw PLAYER_NOT_IN_SPAWN.create();
        
        if (((PlayerData) target).getWarpPos() == null)
            throw TARGET_NO_WARP.create();
        
        // Add the player to the list of invitations
        CoreMod.PLAYER_WARP_INVITES.put( porter, target.getUuid() );
        
        // Notify the target
        target.sendMessage(porter.getName().formatted( Formatting.AQUA )
            .append( new LiteralText( " sent you a TP request, type " ).formatted(Formatting.YELLOW)
            .append( new LiteralText( "/tpaccept " ).formatted(Formatting.GREEN).append( porter.getName() ) )
            .append( " to accept it, or " )
            .append( new LiteralText( "/tpdeny " ).formatted(Formatting.RED).append( porter.getName() ) )
            .append( " to deny it." )
        ));
        
        return Command.SINGLE_SUCCESS;
    }
    private static int tpAcceptCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        UUID warpToUUID;
        if ((( warpToUUID = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!player.getUuid().equals( warpToUUID )) )
            throw TARGET_NOT_REQUESTING.create();
        
        if (!ChunkUtils.isPlayerWithinSpawn( porter )) {
            porter.sendMessage(new LiteralText( "Your warp could not be completed, you must be within spawn to warp." ).formatted(Formatting.RED));
            throw TARGET_NOT_IN_SPAWN.create();
        }
        
        BlockPos warpPos = ((PlayerData) player).getWarpPos();
        WarpUtils.teleportPlayer( porter.getServerWorld(), porter, warpPos );
        
        CoreMod.PLAYER_WARP_INVITES.remove( porter );
        return Command.SINGLE_SUCCESS;
    }
    private static int tpDenyCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        UUID warpToUUID;
        if ((( warpToUUID = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!player.getUuid().equals( warpToUUID )) )
            throw TARGET_NOT_REQUESTING.create();
        
        CoreMod.PLAYER_WARP_INVITES.remove( porter );
        return Command.SINGLE_SUCCESS;
    }
    
}
