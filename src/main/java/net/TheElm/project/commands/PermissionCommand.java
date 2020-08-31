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
import net.TheElm.project.CoreMod;
import net.TheElm.project.commands.ArgumentTypes.PermissionArgumentType;
import net.TheElm.project.config.SewConfig;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class PermissionCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("permissions")
            .requires((source) -> SewConfig.get(SewConfig.HANDLE_PERMISSIONS) && source.hasPermissionLevel( 2 ))
            .then(CommandManager.literal("help")
                .then(CommandManager.argument("permission", StringArgumentType.word())
                    .suggests( PermissionArgumentType::suggestsNodes )
                    .executes( PermissionCommand::nodeInfo )
                )
            )
            .then(CommandManager.literal("add")
                .then(CommandManager.argument("rank", StringArgumentType.word())
                    .suggests( PermissionArgumentType::suggestsRanks )
                    .then(CommandManager.argument("permission", StringArgumentType.word())
                        .suggests( PermissionArgumentType::suggestsNodes )
                        .executes( PermissionCommand::addNodeToRank )
                    )
                    .executes( PermissionCommand::addRank )
                )
            )
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("rank", StringArgumentType.word())
                    .suggests( PermissionArgumentType::suggestsRanks )
                    .then(CommandManager.argument("permission", StringArgumentType.word())
                        .suggests( PermissionArgumentType::suggestsNodes )
                        .executes( PermissionCommand::delNodeFromRank )
                    )
                    .executes( PermissionCommand::delRank )
                )
            )
        );
        CoreMod.logDebug("- Registered Permission command");
    }
    
    public static int nodeInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    
    public static int addRank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    public static int delRank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    
    public static int addNodeToRank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    public static int delNodeFromRank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    
}
