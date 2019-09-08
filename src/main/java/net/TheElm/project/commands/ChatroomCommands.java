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
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.interfaces.PlayerChat;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ChatroomCommands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (!SewingMachineConfig.INSTANCE.CHAT_MODIFY.get())
            return;
        
        final LiteralCommandNode<ServerCommandSource> townChat = dispatcher.register(CommandManager.literal("t")
            .requires(ClaimCommand::sourceInTown)
            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                .executes((context -> 1))
            )
            .executes((context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                ((PlayerChat) player).setChatRoom(ChatRooms.TOWN);
                return Command.SINGLE_SUCCESS;
            }))
        );
        CoreMod.logDebug( "- Registered Town chat command" );
        
        final LiteralCommandNode<ServerCommandSource> globalChat = dispatcher.register(CommandManager.literal("g")
            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                .executes((context -> 1))
            )
            .executes((context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                ((PlayerChat) player).setChatRoom(ChatRooms.GLOBAL);
                return Command.SINGLE_SUCCESS;
            }))
        );
        CoreMod.logDebug( "- Registered Global chat command" );
        
        final LiteralCommandNode<ServerCommandSource> localChat = dispatcher.register(CommandManager.literal("l")
            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                .executes((context -> 1))
            )
            .executes((context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                ((PlayerChat) player).setChatRoom(ChatRooms.LOCAL);
                return Command.SINGLE_SUCCESS;
            }))
        );
        CoreMod.logDebug( "- Registered Local chat command" );
        
        dispatcher.register(CommandManager.literal("chat")
            .then(CommandManager.literal("town")
                .requires(ClaimCommand::sourceInTown)
                .redirect( townChat )
            )
            .then(CommandManager.literal("global")
                .redirect( globalChat )
            )
            .then(CommandManager.literal("local")
                .redirect( localChat )
            )
        );
        
    }
    
}
