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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.SQLFunction;
import net.TheElm.project.protections.logging.EventLogger.BlockAction;
import net.TheElm.project.utilities.CommandUtilities;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.DimensionArgumentType;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.command.arguments.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public final class LoggingCommand {
    
    private LoggingCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        SewingMachineConfig CONFIG = SewingMachineConfig.INSTANCE;
        
        if (( CONFIG.LOG_CHUNKS_CLAIMED.get() || CONFIG.LOG_CHUNKS_UNCLAIMED.get() ) && ( CONFIG.LOG_BLOCKS_BREAKING.get() || CONFIG.LOG_BLOCKS_PLACING.get() )) {
            dispatcher.register(CommandManager.literal("blocklog")
                .requires((source -> source.hasPermissionLevel(CONFIG.LOG_VIEW_OP_LEVEL.get())))
                .then(CommandManager.literal("pos")
                    .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                .executes(LoggingCommand::getBlockHistoryWithCount)
                            )
                            .executes(LoggingCommand::getBlockHistory)
                        )
                    )
                )
                .then(CommandManager.literal("search")
                    .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                    .executes(LoggingCommand::getFromRangeWithCount)
                                )
                                .executes(LoggingCommand::getFromRange)
                            )
                        )
                    )
                )
                .then(CommandManager.literal("by")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtilities::getAllPlayerNames)
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                            .executes(LoggingCommand::getByPlayerWithCount)
                        )
                        .executes(LoggingCommand::getByPlayer)
                    )
                )
            );
            
            CoreMod.logDebug( "- Registered BlockLog command" );
        }
    }
    
    private static int getBlockHistory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return LoggingCommand.getBlockHistorySize(
            context,
            5
        );
    }
    private static int getBlockHistoryWithCount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return LoggingCommand.getBlockHistorySize(
            context,
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private static int getBlockHistorySize(CommandContext<ServerCommandSource> context, int limit) throws CommandSyntaxException {
        DimensionType dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "pos");
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `block`, `updatedEvent`, `updatedBy`, `updatedAt` FROM `logging_Blocks` WHERE `blockWorld` = ? AND `blockX` = ? AND `blockY` = ? AND `blockZ` = ? ORDER BY `updatedAt` DESC" + ( limit > 0 ? " LIMIT ?" : "" ) + ";")
            .addPrepared(dimension.getRawId())
            .addPrepared(blockPos.getX())
            .addPrepared(blockPos.getY())
            .addPrepared(blockPos.getZ())) {
            
            // If limit is set
            if (limit > 0) stmt.addPrepared( limit );
            
            // Create the main text object
            Text heading = new LiteralText("Block History for ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.blockPosToTextComponent( blockPos ));
            
            // Append our results
            Text text = executeSQLStatement( heading, stmt, (results -> {
                // Get the row statement information
                String blockTranslation = results.getString("block");
                boolean add = (BlockAction.valueOf(results.getString("updatedEvent")) == BlockAction.PLACE);
                UUID updatedBy = UUID.fromString(results.getString("updatedBy"));
                
                // Add the row text to the main text
                return new LiteralText("\n" + results.getRow() + ". ")
                    .append(new LiteralText( add ? "+ " : "- " ).append(new TranslatableText(blockTranslation)).formatted( add ? Formatting.GREEN: Formatting.RED ))
                    .append(" by ")
                    .append(PlayerNameUtils.fetchPlayerName(updatedBy).formatted(Formatting.AQUA)
                        .styled(MessageUtils.simpleHoverText(updatedBy.toString())))
                    .append("\n     at ")
                    .append(new LiteralText(results.getTimestamp("updatedAt").toString()).formatted(Formatting.GRAY));
            }));
            
            // Send the text to the player
            context.getSource().sendFeedback(text, false);
            
        } catch (SQLException e) {
            // SQL statement
            CoreMod.logError( e );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getFromRange(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return LoggingCommand.searchForUsedItems(
            context,
            5
        );
    }
    private static int getFromRangeWithCount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return LoggingCommand.searchForUsedItems(
            context,
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private static int searchForUsedItems(CommandContext<ServerCommandSource> context, int limit) throws CommandSyntaxException {
        DimensionType dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        BlockPos centerPos = BlockPosArgumentType.getBlockPos(context, "pos");
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        String blockTranslation = item.getTranslationKey();
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `blockX`, `blockY`, `blockZ`, `updatedEvent`, `updatedBy`, `updatedAt` FROM `logging_Blocks` WHERE `blockWorld` = ? AND `block` = ? AND `blockX` >= ? AND `blockX` <= ? AND `blockZ` >= ? AND `blockZ` <= ? ORDER BY `updatedAt` DESC" + ( limit > 0 ? " LIMIT ?" : "" ) + ";")
            .addPrepared(dimension.getRawId())
            .addPrepared(blockTranslation)
            .addPrepared(centerPos.getX() - 8)
            .addPrepared(centerPos.getX() + 8)
            .addPrepared(centerPos.getZ() - 8)
            .addPrepared(centerPos.getZ() + 8)) {
            
            // If limit is set
            if (limit > 0) stmt.addPrepared( limit );
            
            // Create the output heading
            Text heading = new LiteralText("Block History of ")
                .formatted(Formatting.YELLOW)
                .append(new TranslatableText( blockTranslation ));
            
            // Append our results
            Text text = executeSQLStatement( heading, stmt, (results -> {
                // Get the row statement information
                boolean add = (BlockAction.valueOf(results.getString("updatedEvent")) == BlockAction.PLACE);
                UUID updatedBy = UUID.fromString(results.getString("updatedBy"));
                Consumer<Style> hoverEvent = (styler) -> styler.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new LiteralText(updatedBy.toString())));
                
                // Add the row text to the main text
                return new LiteralText("\n" + results.getRow() + ". ")
                    .append(new LiteralText( add ? "+ " : "- " ).append(new TranslatableText(blockTranslation)).formatted( add ? Formatting.GREEN: Formatting.RED ))
                    .append(" by ")
                    .append(PlayerNameUtils.fetchPlayerName(updatedBy).formatted(Formatting.AQUA).styled(hoverEvent))
                    .append("\n     at ")
                    .append(MessageUtils.blockPosToTextComponent(new BlockPos(results.getInt("blockX"), results.getInt("blockY"), results.getInt("blockZ"))).formatted(Formatting.GRAY))
                    .append("\n     at ")
                    .append(new LiteralText(results.getTimestamp("updatedAt").toString()).formatted(Formatting.GRAY));
            }));
            
            // Send the text to the player
            context.getSource().sendFeedback(text, false);
            
        } catch (SQLException e) {
            // SQL statement
            CoreMod.logError( e );
            
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getByPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return LoggingCommand.searchForPlayer(
            context,
            5
        );
    }
    private static int getByPlayerWithCount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return LoggingCommand.searchForPlayer(
            context,
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private static int searchForPlayer(CommandContext<ServerCommandSource> context, int limit) throws CommandSyntaxException {
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile player = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `block`, `blockX`, `blockY`, `blockZ`, `blockWorld`, `updatedEvent`, `updatedAt` FROM `logging_Blocks` WHERE `updatedBy` = ? ORDER BY `updatedAt` DESC" + ( limit > 0 ? " LIMIT ?" : "" ) + ";")
            .addPrepared(player.getId().toString())) {
            
            // If limit is set
            if (limit > 0) stmt.addPrepared( limit );
            
            // Create the output heading
            Text heading = new LiteralText("Block History for " + player.getName())
                .formatted(Formatting.YELLOW);
            
            // Append our results
            Text text = executeSQLStatement( heading, stmt, (results -> {
                // Get the row statement information
                String blockTranslation = results.getString("block");
                boolean add = (BlockAction.valueOf(results.getString("updatedEvent")) == BlockAction.PLACE);
                Consumer<Style> hoverEvent = (styler) -> styler.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new LiteralText(player.getId().toString())));
                
                // Add the row text to the main text
                return new LiteralText("\n" + results.getRow() + ". ")
                    .append(new LiteralText( add ? "+ " : "- " ).append(new TranslatableText(blockTranslation)).formatted( add ? Formatting.GREEN: Formatting.RED ))
                    .append(" by ")
                    .append(new LiteralText(player.getName()).formatted(Formatting.AQUA).styled(hoverEvent))
                    .append("\n     at ")
                    .append(MessageUtils.blockPosToTextComponent(new BlockPos(results.getInt("blockX"), results.getInt("blockY"), results.getInt("blockZ")), results.getInt("blockWorld")).formatted(Formatting.GRAY))
                    .append("\n     at ")
                    .append(new LiteralText(results.getTimestamp("updatedAt").toString()).formatted(Formatting.GRAY));
            }));
            
            // Send the text to the player
            context.getSource().sendFeedback(text, false);
            
        } catch (SQLException e) {
            // SQL statement
            CoreMod.logError( e );
        
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static Text executeSQLStatement(@NotNull Text text, @NotNull MySQLStatement stmt, @NotNull SQLFunction<ResultSet, Text> function) throws SQLException {
        ArrayList<Text> list = new ArrayList<>();
        
        // Execute the statement
        ResultSet results = stmt.executeStatement();
        
        // For all of the rows
        int i = 0;
        while (results.next()) {
            // Add the row text to the main text
            list.add(function.apply( results ));
        }
        
        // Add the rows
        for ( i = list.size(); i-- > 0; ) {
            text.append(list.get(i));
        }
        
        return text.append("\nDone.");
    }
    
}
