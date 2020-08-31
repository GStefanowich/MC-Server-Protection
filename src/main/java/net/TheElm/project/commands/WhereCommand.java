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
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class WhereCommand {
    private WhereCommand() {
    }
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("where")
            .requires((source) -> source.hasPermissionLevel(OpLevels.CHEATING) || RankUtils.hasPermission(source, Permissions.LOCATE_PLAYERS))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(WhereCommand::locatePlayer)
            )
        );
    }
    
    private static int locatePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        BlockPos pos = player.getBlockPos();
        
        MutableText feedback = new LiteralText("")
            .formatted(Formatting.YELLOW)
            .append(player.getDisplayName())
            .append(" is currently at ")
            .append(MessageUtils.blockPosToTextComponent(pos));
        
        if (SewConfig.get(SewConfig.DO_CLAIMS)) {
            Entity commandSource = source.getEntity();
            
            ServerWorld world = source.getWorld();
            IClaimedChunk chunk = (IClaimedChunk) world.getWorldChunk(pos);
            
            // Append where they are located
            if (chunk.getOwner(pos) != null) {
                feedback.append("\n")
                    .append("They are currently in ")
                    .append(chunk.getOwnerName(commandSource instanceof PlayerEntity ? (PlayerEntity) commandSource : player, pos))
                    .append("'s claimed area.");
            }
        }
        
        source.sendFeedback(feedback, false);
        
        return Command.SINGLE_SUCCESS;
    }
    
}