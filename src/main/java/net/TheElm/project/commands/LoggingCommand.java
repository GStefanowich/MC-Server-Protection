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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.protections.logging.EventLogger.BlockAction;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                            .executes(LoggingCommand::getBlockHistoryCount)
                        )
                        .executes(LoggingCommand::getBlockHistory)
                    )
                )
                .then(CommandManager.literal("block")
                    .then(CommandManager.argument("block", IdentifierArgumentType.identifier())
                        
                    )
                )
            );
            
            CoreMod.logDebug( "- Registered BlockLog command" );
        }
    }
    
    private static int getBlockHistory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "pos");
        
        return LoggingCommand.getBlockHistorySize(player, blockPos, 5);
    }
    private static int getBlockHistoryCount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "pos");
        int integer = IntegerArgumentType.getInteger(context, "count");
        
        return LoggingCommand.getBlockHistorySize(player, blockPos, integer);
    }
    private static int getBlockHistorySize(ServerPlayerEntity player, BlockPos blockPos, int limit) throws CommandSyntaxException {
        // Create the main text object
        Text text = new LiteralText("Block History for ")
            .formatted(Formatting.YELLOW)
            .append(MessageUtils.blockPosToTextComponent( blockPos ));
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `block`, `updatedEvent`, `updatedBy`, `updatedAt` FROM `logging_Blocks` WHERE `blockWorld` = ? AND `blockX` = ? AND `blockY` = ? AND `blockZ` = ? ORDER BY `updatedAt` DESC" + ( limit > 0 ? " LIMIT ?" : "" ) + ";")
            .addPrepared(player.dimension.getRawId())
            .addPrepared(blockPos.getX())
            .addPrepared(blockPos.getY())
            .addPrepared(blockPos.getZ())) {

            ArrayList<Text> list = new ArrayList<>();
            
            // If limit is set
            if (limit > 0) stmt.addPrepared( limit );
            
            // Execute the statement
            ResultSet results = stmt.executeStatement();
            
            // For all of the rows
            int i = 0;
            while (results.next()) {
                // Get the row statement information
                String blockTranslation = results.getString("block");
                boolean add = (BlockAction.valueOf(results.getString("updatedEvent")) == BlockAction.PLACE);
                UUID updatedBy = UUID.fromString(results.getString("updatedBy"));
                Consumer<Style> hoverEvent = (styler) -> styler.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new LiteralText(updatedBy.toString())));
                
                // Add the row text to the main text
                list.add(new LiteralText("\n" + ( ++i ) + ". ")
                    .append(new LiteralText( add ? "+ " : "- " ).append(new TranslatableText(blockTranslation)).formatted( add ? Formatting.GREEN: Formatting.RED ))
                    .append(" by ")
                    .append(PlayerNameUtils.fetchPlayerName(updatedBy).formatted(Formatting.AQUA).styled(hoverEvent))
                    .append("\n     at ")
                    .append(new LiteralText(results.getTimestamp("updatedAt").toString()).formatted(Formatting.GRAY)));
            }
            
            // Add the rows
            for ( i = list.size(); i-- > 0; ) {
                text.append(list.get(i));
            }
            
        } catch (SQLException e) {
            // SQL statement
            CoreMod.logError( e );
        }
        
        // Send the text to the player
        player.sendMessage(text.append("\nDone."));
        
        return Command.SINGLE_SUCCESS;
    }

}
