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
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.CommandUtils;
import net.TheElm.project.utilities.RankUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

public final class ChatroomCommands {
    
    private static final SimpleCommandExceptionType MUTE_EXEMPT = new SimpleCommandExceptionType(new LiteralText("That player is exempt from being muted."));
    
    private ChatroomCommands() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "t", "Town Chat", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MODIFY).and(ClaimCommand::sourceInTown))
            .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.TOWN)))
            .executes((context -> switchToChatRoom(context, ChatRooms.TOWN)))
        );
        
        ServerCore.register(dispatcher, "g", "Global Chat", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MODIFY))
            .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.GLOBAL)))
            .executes((context -> switchToChatRoom(context, ChatRooms.GLOBAL)))
        );
        
        ServerCore.register(dispatcher, "l", "Local Chat", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MODIFY))
            .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.LOCAL)))
            .executes((context -> switchToChatRoom(context, ChatRooms.LOCAL)))
        );
        
        ServerCore.register(dispatcher, "Chat", builder -> builder
            .then(CommandManager.literal("town")
                .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MODIFY).and(ClaimCommand::sourceInTown))
                .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.TOWN)))
                .executes((context -> switchToChatRoom(context, ChatRooms.TOWN)))
            )
            .then(CommandManager.literal("global")
                .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MODIFY))
                .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.GLOBAL)))
                .executes((context -> switchToChatRoom(context, ChatRooms.GLOBAL)))
            )
            .then(CommandManager.literal("local")
                .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MODIFY))
                .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.LOCAL)))
                .executes((context -> switchToChatRoom(context, ChatRooms.LOCAL)))
            )
        );
        
        // TODO: Add Mute permission node
        // TODO: Add shadow mute
        ServerCore.register(dispatcher, "Mute", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MODIFY)
                .and(
                    CommandPredicate.isEnabled(SewConfig.CHAT_MUTE_SELF)
                    .or(CommandPredicate.isEnabled(SewConfig.CHAT_MUTE_OP).and(OpLevels.KICK_BAN_OP))
                )
            )
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .suggests(CommandUtils::getOnlineNames)
                .then(CommandManager.literal("global")
                    .requires(CommandPredicate.isEnabled(SewConfig.CHAT_MUTE_OP)
                        .and(
                            CommandPredicate.opLevel(OpLevels.KICK_BAN_OP)
                            .or(Permissions.CHAT_COMMAND_MUTE)
                        )
                    )
                    .executes(ChatroomCommands::opMute)
                )
                .executes(ChatroomCommands::playerMute)
            )
        );
    }
    
    private static int sendToChatRoom(@NotNull final CommandContext<ServerCommandSource> context, final ChatRooms room) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        if ((room != ChatRooms.TOWN) && ((PlayerChat)player).isMuted()) {
            player.sendSystemMessage(TranslatableServerSide.text(
                player,
                "chat.muted",
                room
            ).formatted(Formatting.RED), Util.NIL_UUID);
            return Command.SINGLE_SUCCESS;
        }
        
        // Format the text
        Text chatText = MessageUtils.formatPlayerMessage(
            player,
            room,
            MessageArgumentType.getMessage(context, "text")
        );
        
        // Send the new chat message to the currently selected chat room
        MessageUtils.sendTo(room, player, chatText);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int switchToChatRoom(@NotNull final CommandContext<ServerCommandSource> context, final ChatRooms chatRoom) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Update the chat room
        ((PlayerChat) player).setChatRoom(chatRoom);
        
        // Tell the player
        player.sendSystemMessage(TranslatableServerSide.text(player, "chat.change." + chatRoom.name().toLowerCase()), Util.NIL_UUID);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int playerMute(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (!SewConfig.get(SewConfig.CHAT_MUTE_SELF))
            return ChatroomCommands.opMute( context );
        
        ServerCommandSource source = context.getSource();
        PlayerChat self = (PlayerChat)(source.getPlayer());
        
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        
        source.sendFeedback(TranslatableServerSide.text(
            source,
            (self.toggleMute( target.getGameProfile() ) ? "chat.mute.player" : "chat.unmute.player"),
            target.getDisplayName()
        ).formatted(Formatting.GREEN), false);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int opMute(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        PlayerChat chatter = (PlayerChat) target;
        
        if (RankUtils.hasPermission(target, Permissions.CHAT_COMMAND_MUTE_EXEMPT) || target.hasPermissionLevel(1))
            throw MUTE_EXEMPT.create();
        else {
            source.sendFeedback(TranslatableServerSide.text(
                source,
                (chatter.toggleMute() ? "chat.mute.global" : "chat.unmute.global"),
                target.getDisplayName()
            ).formatted(Formatting.GREEN), false);
            
            return Command.SINGLE_SUCCESS;
        }
        
    }
}
