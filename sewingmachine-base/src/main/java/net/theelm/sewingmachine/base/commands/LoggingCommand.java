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

package net.theelm.sewingmachine.base.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.MySQL.MySQLStatement;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.interfaces.SQLFunction;
import net.theelm.sewingmachine.protections.logging.EventLogger.BlockAction;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class LoggingCommand implements SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess access) {
        if (( SewConfig.any(SewBaseConfig.LOG_CHUNKS_CLAIMED, SewBaseConfig.LOG_CHUNKS_UNCLAIMED) ) && ( SewConfig.any(SewBaseConfig.LOG_BLOCKS_BREAKING, SewBaseConfig.LOG_BLOCKS_PLACING) )) {
            CommandUtils.register(dispatcher, "blocklog", builder -> builder
                .requires(CommandPredicate.opLevel(SewBaseConfig.LOG_VIEW_OP_LEVEL))
                .then(CommandManager.literal("pos")
                    .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                .executes(this::getBlockHistoryWithCount)
                            )
                            .executes(this::getBlockHistory)
                        )
                    )
                )
                .then(CommandManager.literal("search")
                    .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(access))
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                    .executes(this::getFromRangeWithCount)
                                )
                                .executes(this::getFromRange)
                            )
                        )
                    )
                )
                .then(CommandManager.literal("by")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                            .executes(this::getByPlayerWithCount)
                        )
                        .executes(this::getByPlayer)
                    )
                )
            );
        }
    }
    
    private int getBlockHistory(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.getBlockHistorySize(
            context,
            5
        );
    }
    private int getBlockHistoryWithCount(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.getBlockHistorySize(
            context,
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private int getBlockHistorySize(@NotNull CommandContext<ServerCommandSource> context, int limit) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = DimensionArgumentType.getDimensionArgument(context, "dimension");
        BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "pos");
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `block`, `updatedEvent`, `updatedBy`, `updatedAt` FROM `logging_Blocks` WHERE `blockWorld` = ? AND `blockX` = ? AND `blockY` = ? AND `blockZ` = ? ORDER BY `updatedAt` DESC" + ( limit > 0 ? " LIMIT ?" : "" ) + ";")
            .addPrepared(NbtUtils.worldToTag(world))
            .addPrepared(blockPos.getX())
            .addPrepared(blockPos.getY())
            .addPrepared(blockPos.getZ())) {
            
            // If limit is set
            if (limit > 0) stmt.addPrepared( limit );
            
            // Create the main text object
            MutableText heading = Text.literal("Block History for ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.xyzToText( blockPos ));
            
            // Append our results
            Text text = this.executeSQLStatement(heading, stmt, (results -> {
                // Get the row statement information
                String blockTranslation = results.getString("block");
                boolean add = (BlockAction.valueOf(results.getString("updatedEvent")) == BlockAction.PLACE);
                UUID updatedBy = UUID.fromString(results.getString("updatedBy"));
                
                // Add the row text to the main text
                return Text.literal("\n" + results.getRow() + ". ")
                    .append(Text.literal( add ? "+ " : "- " ).append(Text.translatable(blockTranslation)).formatted( add ? Formatting.GREEN: Formatting.RED ))
                    .append(" by ")
                    .append(TextUtils.mutable(PlayerNameCallback.getName(source.getServer(), updatedBy)).formatted(Formatting.AQUA)
                        .styled(MessageUtils.simpleHoverText(updatedBy.toString())))
                    .append("\n     at ")
                    .append(Text.literal(results.getTimestamp("updatedAt").toString()).formatted(Formatting.GRAY));
            }));
            
            // Send the text to the player
            source.sendFeedback(
                () -> text,
                false
            );
            
        } catch (SQLException e) {
            // SQL statement
            CoreMod.logError( e );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getFromRange(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.searchForUsedItems(
            context,
            5
        );
    }
    private int getFromRangeWithCount(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.searchForUsedItems(
            context,
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private int searchForUsedItems(@NotNull CommandContext<ServerCommandSource> context, int limit) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = DimensionArgumentType.getDimensionArgument(context, "dimension");
        BlockPos centerPos = BlockPosArgumentType.getBlockPos(context, "pos");
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        String blockTranslation = item.getTranslationKey();
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `blockX`, `blockY`, `blockZ`, `updatedEvent`, `updatedBy`, `updatedAt` FROM `logging_Blocks` WHERE `blockWorld` = ? AND `block` = ? AND `blockX` >= ? AND `blockX` <= ? AND `blockZ` >= ? AND `blockZ` <= ? ORDER BY `updatedAt` DESC" + ( limit > 0 ? " LIMIT ?" : "" ) + ";")
            .addPrepared(NbtUtils.worldToTag(world))
            .addPrepared(blockTranslation)
            .addPrepared(centerPos.getX() - 8)
            .addPrepared(centerPos.getX() + 8)
            .addPrepared(centerPos.getZ() - 8)
            .addPrepared(centerPos.getZ() + 8)) {
            
            // If limit is set
            if (limit > 0) stmt.addPrepared( limit );
            
            // Create the output heading
            MutableText heading = Text.literal("Block History of ")
                .formatted(Formatting.YELLOW)
                .append(Text.translatable( blockTranslation ));
            
            // Append our results
            Text text = this.executeSQLStatement(heading, stmt, (results -> {
                // Get the row statement information
                boolean add = (BlockAction.valueOf(results.getString("updatedEvent")) == BlockAction.PLACE);
                UUID updatedBy = UUID.fromString(results.getString("updatedBy"));
                UnaryOperator<Style> hoverEvent = (styler) -> styler.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal(updatedBy.toString())));
                
                // Add the row text to the main text
                return Text.literal("\n" + results.getRow() + ". ")
                    .append(Text.literal( add ? "+ " : "- " ).append(Text.translatable(blockTranslation)).formatted( add ? Formatting.GREEN: Formatting.RED ))
                    .append(" by ")
                    .append(PlayerNameCallback.getPlainName(source.getServer(), updatedBy).formatted(Formatting.AQUA).styled(hoverEvent))
                    .append("\n     at ")
                    .append(MessageUtils.xyzToText(new BlockPos(results.getInt("blockX"), results.getInt("blockY"), results.getInt("blockZ"))).formatted(Formatting.GRAY))
                    .append("\n     at ")
                    .append(Text.literal(results.getTimestamp("updatedAt").toString()).formatted(Formatting.GRAY));
            }));
            
            // Send the text to the player
            source.sendFeedback(() -> text, false);
            
        } catch (SQLException e) {
            // SQL statement
            CoreMod.logError( e );
            
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getByPlayer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.searchForPlayer(
            context,
            5
        );
    }
    private int getByPlayerWithCount(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.searchForPlayer(
            context,
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private int searchForPlayer(@NotNull CommandContext<ServerCommandSource> context, int limit) throws CommandSyntaxException {
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile player = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `block`, `blockX`, `blockY`, `blockZ`, `blockWorld`, `updatedEvent`, `updatedAt` FROM `logging_Blocks` WHERE `updatedBy` = ? ORDER BY `updatedAt` DESC" + ( limit > 0 ? " LIMIT ?" : "" ) + ";")
            .addPrepared(player.getId().toString())) {
            
            // If limit is set
            if (limit > 0) stmt.addPrepared( limit );
            
            // Create the output heading
            MutableText heading = Text.literal("Block History for " + player.getName())
                .formatted(Formatting.YELLOW);
            
            // Append our results
            Text text = this.executeSQLStatement(heading, stmt, (results) -> {
                // Get the row statement information
                String blockTranslation = results.getString("block");
                boolean add = (BlockAction.valueOf(results.getString("updatedEvent")) == BlockAction.PLACE);
                UnaryOperator<Style> hoverEvent = (style) -> style.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal(player.getId().toString())));
                
                // Add the row text to the main text
                return Text.literal("\n" + results.getRow() + ". ")
                    .append(Text.literal( add ? "+ " : "- " ).append(Text.translatable(blockTranslation)).formatted( add ? Formatting.GREEN: Formatting.RED ))
                    .append(" by ")
                    .append(Text.literal(player.getName()).formatted(Formatting.AQUA).styled(hoverEvent))
                    .append("\n     at ")
                    .append(MessageUtils.xyzToText(new BlockPos(results.getInt("blockX"), results.getInt("blockY"), results.getInt("blockZ")), new Identifier(results.getString("blockWorld"))).formatted(Formatting.GRAY))
                    .append("\n     at ")
                    .append(Text.literal(results.getTimestamp("updatedAt").toString()).formatted(Formatting.GRAY));
            });
            
            // Send the text to the player
            context.getSource().sendFeedback(() -> text, false);
            
        } catch (SQLException e) {
            // SQL statement
            CoreMod.logError( e );
        
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private Text executeSQLStatement(@NotNull MutableText text, @NotNull MySQLStatement stmt, @NotNull SQLFunction<ResultSet, Text> function) throws SQLException {
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
