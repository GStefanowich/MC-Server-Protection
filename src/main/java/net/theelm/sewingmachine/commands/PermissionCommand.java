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

package net.theelm.sewingmachine.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.commands.ArgumentTypes.ArgumentSuggestions;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.protections.ranks.PlayerRank;
import net.theelm.sewingmachine.utilities.RankUtils;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PermissionCommand {
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "permissions", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.HANDLE_PERMISSIONS).and(OpLevels.CHEATING))
            // Create a new Rank
            .then(CommandManager.literal("add")
                .then(CommandManager.argument("rank", StringArgumentType.string())
                    .executes(PermissionCommand::addRank)
                )
            )
            // Delete a Rank
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("rank", StringArgumentType.string())
                    .suggests(ArgumentSuggestions::suggestRanks)
                    .executes(PermissionCommand::delRank)
                )
            )
            // Modify a Ranks Permissions
            .then(CommandManager.literal("modify")
                .then(CommandManager.argument("rank", StringArgumentType.string())
                    .suggests(ArgumentSuggestions::suggestRanks)
                    .then(CommandManager.literal("nodes")
                        .then(CommandManager.argument("permission", StringArgumentType.string())
                            .suggests(ArgumentSuggestions::suggestNodes)
                            .then(CommandManager.literal("grant")
                                .executes(PermissionCommand::grantNodeToRank)
                            )
                            .then(CommandManager.literal("deny")
                                .executes(PermissionCommand::denyNodeToRank)
                            )
                            .then(CommandManager.literal("reset")
                                .executes(PermissionCommand::delNodeFromRank)
                            )
                        )
                    )
                    .then(CommandManager.literal("inherits")
                        .then(CommandManager.argument("from", StringArgumentType.string())
                            .suggests(ArgumentSuggestions::suggestRanks)
                            //.executes()
                        )
                    )
                )
            )
        );
    }
    
    /**
     * Create a new permission rank
     * @param context The command runtime context
     * @return 1 if created, 0 if failed
     * @throws CommandSyntaxException If creating the rank fails
     */
    public static int addRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String rank = StringArgumentType.getString(context, "rank");
        
        // Check that the rank doesn't already exist
        if (RankUtils.getRank(rank) != null)
            throw new CommandException(new LiteralText("Rank by that name already exists"));
        
        
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Delete an existing permission rank
     * @param context The command runtime context
     * @return 1 if removed, 0 if failed
     * @throws CommandSyntaxException If deleting the rank fails
     */
    public static int delRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerRank rank = PermissionCommand.getRankByName(StringArgumentType.getString(context, "rank"));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Add a positive "+" node to a rank
     * @param context The command runtime context
     * @return 1 if added, 0 if failed
     * @throws CommandSyntaxException If an error occurs adding the node
     */
    public static int grantNodeToRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerRank rank = PermissionCommand.getRankByName(StringArgumentType.getString(context, "rank"));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Add a negative "-" node to a rank
     * @param context The command runtime context
     * @return 1 if added, 0 if failed
     * @throws CommandSyntaxException If an error occurs adding the node
     */
    public static int denyNodeToRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerRank rank = PermissionCommand.getRankByName(StringArgumentType.getString(context, "rank"));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Clear permission nodes for a rank
     * @param context The command runtime context
     * @return 1 if cleared, 0 if failed
     * @throws CommandSyntaxException If an error occurs clearing the node
     */
    public static int delNodeFromRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerRank rank = PermissionCommand.getRankByName(StringArgumentType.getString(context, "rank"));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Get a non-null Rank by name, throw an exception if is null
     * @param name The name of the PlayerRank to find
     * @return The requested PlayerRank
     * @throws CommandSyntaxException If the rank is not found by name
     */
    private static @NotNull PlayerRank getRankByName(@Nullable String name) throws CommandSyntaxException {
        PlayerRank rank = RankUtils.getRank(name);
        if (rank == null)
            throw new CommandException(new LiteralText("Could not find a rank by that name."));
        return rank;
    }
    
}
