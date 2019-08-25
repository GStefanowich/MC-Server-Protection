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
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class WaystoneCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register( CommandManager.literal("waystones")
            .requires((source) -> source.hasPermissionLevel( 2 ))
            .then( CommandManager.literal("set")
                .then( CommandManager.argument( "player", EntityArgumentType.player())
                    .executes(WaystoneCommand::setToCurrentLocation)
                )
            )
            .then( CommandManager.literal("reset")
                .then( CommandManager.argument( "player", EntityArgumentType.player())
                    .executes(WaystoneCommand::resetPlayer)
                )
            )
        );
        CoreMod.logDebug( "- Registered Waystone command" );
    }
    
    private static int setToCurrentLocation(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        
        BlockPos blockPos = player.getBlockPos();
        
        // Update the positioning
        ((PlayerData) target).setWarpPos( blockPos );
        ((PlayerData) target).setWarpDimension( player.getServerWorld() );
        
        player.sendMessage(new LiteralText("").formatted(Formatting.YELLOW)
            .append(target.getName().formatted(Formatting.AQUA))
            .append("'s waystone is now set to ")
            .append(new LiteralText(""+blockPos.getX()).formatted(Formatting.AQUA))
            .append(", ")
            .append(new LiteralText(""+blockPos.getY()).formatted(Formatting.AQUA))
            .append(",")
            .append(new LiteralText(""+blockPos.getZ()).formatted(Formatting.AQUA))
            .append(".")
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int resetPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        
        // Reset the position
        ((PlayerData) target).setWarpPos( null );
        
        return Command.SINGLE_SUCCESS;
    }
    
}
