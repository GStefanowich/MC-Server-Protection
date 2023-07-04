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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.events.RegionNameCallback;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public final class WhereCommand extends SewCommand {
    public static final @NotNull String NAME = "Where";
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, WhereCommand.NAME, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.LOCATE_PLAYERS))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(this::locatePlayer)
            )
        );
    }
    
    private int locatePlayer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        BlockPos pos = player.getBlockPos();
        
        MutableText feedback = Text.literal("")
            .formatted(Formatting.YELLOW)
            .append(player.getDisplayName())
            .append(" is currently at ")
            .append(MessageUtils.xyzToText(pos))
            .append(" in ")
            .append(Text.literal(player.getWorld().getRegistryKey().getValue().toString()).formatted(Formatting.AQUA));
        
        Text location = RegionNameCallback.EVENT.invoker()
            .getName(
                source.getWorld(),
                pos,
                source.getEntity(),
                false,
                false
            );
        
        if (location != null) {
            feedback.append("\n")
                .append("They are currently in ")
                .append(location)
                .append(".");
        }
        
        source.sendFeedback(
            () -> feedback,
            false
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
}