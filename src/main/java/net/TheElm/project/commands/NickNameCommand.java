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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.Nicknamable;
import net.minecraft.command.arguments.ColorArgumentType;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NickNameCommand {
    
    private NickNameCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (!SewingMachineConfig.INSTANCE.DO_PLAYER_NICKS.get())
            return;
        
        LiteralCommandNode<ServerCommandSource> pay = dispatcher.register( CommandManager.literal( "nick" )
            .then( CommandManager.literal( "reset" )
                .executes((context) -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    return setNickForPlayer( player, null );
                })
            )
            .then( CommandManager.argument( "nick", StringArgumentType.string())
                .then( CommandManager.argument( "color", ColorArgumentType.color())
                    .executes( NickNameCommand::commandNickSetColored )
                )
                .executes( NickNameCommand::commandNickSet )
            )
        );
        CoreMod.logDebug( "- Registered Nick command" );
    }
    
    private static int commandNickSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String nickname = StringArgumentType.getString( context, "nick" );
        return NickNameCommand.setNickForPlayer( player, nickname, Formatting.WHITE );
    }
    
    private static int commandNickSetColored(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String nickname = StringArgumentType.getString( context, "nick" );
        Formatting color = ColorArgumentType.getColor( context, "color" );
        return NickNameCommand.setNickForPlayer( player, nickname, color );
    }
    
    private static int commandNickSetOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer( context, "player" );
        String nickname = StringArgumentType.getString( context, "nick" );
        return NickNameCommand.setNickForPlayer( player, nickname );
    }
    
    private static int setNickForPlayer(@NotNull ServerPlayerEntity player, @Nullable String nickname, Formatting... formatting) {
        ((Nicknamable)player).setPlayerNickname( nickname == null ? null : new LiteralText( nickname ).formatted( formatting ) );
        return Command.SINGLE_SUCCESS;
    }
    
}
