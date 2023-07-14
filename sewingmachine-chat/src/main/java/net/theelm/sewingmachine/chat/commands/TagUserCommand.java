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

package net.theelm.sewingmachine.chat.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.chat.enums.ChatRooms;
import net.theelm.sewingmachine.chat.interfaces.PlayerChat;
import net.theelm.sewingmachine.chat.utilities.ChatRoomUtilities;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.objects.MessageRegion;
import net.theelm.sewingmachine.utilities.CommandUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * Created on Mar 18 2021 at 1:07 PM.
 * By greg in SewingMachineMod
 */
public final class TagUserCommand extends SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "@", "tag user", builder -> builder
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                    .executes(this::tagPlayerMessage)
                )
                .executes(this::tagPlayer)
            )
        );
    }

    private int tagPlayer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.sendTaggedMessage(
            context,
            ""
        );
    }
    private int tagPlayerMessage(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.sendTaggedMessage(
            context,
            StringArgumentType.getString(context, "message")
        );
    }
    private int sendTaggedMessage(@NotNull CommandContext<ServerCommandSource> context, @NotNull String text) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity from = source.getPlayerOrThrow();
        ServerPlayerEntity to = EntityArgumentType.getPlayer(context, "player");
        
        // The chatroom to send the message in
        MessageRegion room = ((PlayerChat)from).getChatRoom();
        return Command.SINGLE_SUCCESS;
    }
}
