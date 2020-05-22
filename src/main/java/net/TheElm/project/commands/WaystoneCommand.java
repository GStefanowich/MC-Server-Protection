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
import net.TheElm.project.CoreMod;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.WarpUtils;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;

public class WaystoneCommand {
    
    private WaystoneCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("waystones")
            .requires((source) -> source.hasPermissionLevel( 2 ))
            .then( CommandManager.literal("set")
                .then( CommandManager.argument( "player", EntityArgumentType.player())
                    .executes(WaystoneCommand::setToCurrentLocation)
                )
            )
            .then(CommandManager.literal("reset")
                .then( CommandManager.argument( "player", EntityArgumentType.player())
                    .executes(WaystoneCommand::resetPlayer)
                )
                .executes(WaystoneCommand::resetSelf)
            )
            .then(CommandManager.literal("send")
                .then(CommandManager.argument("players", EntityArgumentType.players())
                    .requires((source) -> source.hasPermissionLevel(1))
                    .executes(WaystoneCommand::sendHome)
                )
            )
        );
        CoreMod.logDebug( "- Registered Waystone command" );
    }
    
    private static int setToCurrentLocation(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntity();
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        
        // Get the position to set the waystone to
        BlockPos blockPos = (entity instanceof PlayerEntity ? entity.getBlockPos() : new BlockPos(source.getPosition()).up());
        
        // Update the positioning
        ((PlayerData) target).setWarpPos( blockPos );
        ((PlayerData) target).setWarpDimension( source.getWorld() );
        
        source.sendFeedback(new LiteralText("Set ")
            .append(target.getDisplayName().deepCopy())
            .append("'s waystone to ")
            .append(MessageUtils.blockPosToTextComponent(blockPos))
            .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int resetSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get source of the command
        ServerCommandSource source = context.getSource();
        
        // Send feedback to the source
        source.sendFeedback(new LiteralText("Waystone reset."), true);
        
        // Reset the waystone
        return WaystoneCommand.reset(source.getPlayer());
    }
    private static int resetPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get source of the command
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        
        // Send feedback to the source
        source.sendFeedback(new LiteralText("Reset the waystone of ")
            .append(target.getDisplayName().deepCopy())
            .append("."), true);
        
        // Reset the waystone
        return WaystoneCommand.reset(target);
    }
    private static int reset(ServerPlayerEntity player) {
        // Reset the position
        ((PlayerData) player).setWarpPos( null );
        return Command.SINGLE_SUCCESS;
    }
    private static int sendHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
        
        // Iterate players
        for (ServerPlayerEntity player : players) {
            if (!WarpUtils.hasWarp(player)) {
                if (source.getEntity() == null) {
                    player.sendMessage(new LiteralText("You do not have a waystone.").formatted(Formatting.RED));
                } else if (players.size() == 1)
                    source.sendError(new LiteralText("That player does not have a waystone.").formatted(Formatting.RED));
                continue;
            }
            
            // Teleport the player to their warp
            WarpUtils.teleportPlayer( player );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
}
