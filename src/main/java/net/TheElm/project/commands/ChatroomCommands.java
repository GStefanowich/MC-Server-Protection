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
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.CommandUtilities;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.RankUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.MessageArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public final class ChatroomCommands {
    
    private static final SimpleCommandExceptionType MUTE_EXEMPT = new SimpleCommandExceptionType(new LiteralText("That player is exempt from being muted."));
    
    private ChatroomCommands() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("t")
            .requires((source) -> SewConfig.get(SewConfig.CHAT_MODIFY) && ClaimCommand.sourceInTown( source ))
            .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.TOWN)))
            .executes((context -> switchToChatRoom(context, ChatRooms.TOWN)))
        );
        CoreMod.logDebug( "- Registered Town chat command" );
        
        dispatcher.register(CommandManager.literal("g")
            .requires((source) -> SewConfig.get(SewConfig.CHAT_MODIFY))
            .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.GLOBAL)))
            .executes((context -> switchToChatRoom(context, ChatRooms.GLOBAL)))
        );
        CoreMod.logDebug( "- Registered Global chat command" );
        
        dispatcher.register(CommandManager.literal("l")
            .requires((source) -> SewConfig.get(SewConfig.CHAT_MODIFY))
            .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.LOCAL)))
            .executes((context -> switchToChatRoom(context, ChatRooms.LOCAL)))
        );
        CoreMod.logDebug( "- Registered Local chat command" );
        
        dispatcher.register(CommandManager.literal("chat")
            .then(CommandManager.literal("town")
                .requires(( source ) -> SewConfig.get(SewConfig.CHAT_MODIFY) && ClaimCommand.sourceInTown( source ))
                .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.TOWN)))
                .executes((context -> switchToChatRoom(context, ChatRooms.TOWN)))
            )
            .then(CommandManager.literal("global")
                .requires(( source ) -> SewConfig.get(SewConfig.CHAT_MODIFY))
                .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.GLOBAL)))
                .executes((context -> switchToChatRoom(context, ChatRooms.GLOBAL)))
            )
            .then(CommandManager.literal("local")
                .requires(( source ) -> SewConfig.get(SewConfig.CHAT_MODIFY))
                .then(CommandManager.argument("text", MessageArgumentType.message()).executes((context) -> sendToChatRoom(context, ChatRooms.LOCAL)))
                .executes((context -> switchToChatRoom(context, ChatRooms.LOCAL)))
            )
        );
        
        // TODO: Add Mute permission node
        // TODO: Add shadow mute
        dispatcher.register(CommandManager.literal("mute")
            .requires(( source ) -> (SewConfig.get(SewConfig.CHAT_MODIFY)) && (SewConfig.get(SewConfig.CHAT_MUTE_SELF) || SewConfig.get(SewConfig.CHAT_MUTE_OP) && source.hasPermissionLevel( 3 )))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .suggests( CommandUtilities::getOnlinePlayerNames )
                .then(CommandManager.literal("global")
                    .requires(( source ) -> SewConfig.get(SewConfig.CHAT_MUTE_OP) && (source.hasPermissionLevel(OpLevels.KICK_BAN_OP) || RankUtils.hasPermission(source, Permissions.CHAT_COMMAND_MUTE)))
                    .executes(ChatroomCommands::opMute)
                )
                .executes(ChatroomCommands::playerMute)
            )
        );
    }
    
    private static int sendToChatRoom(final CommandContext<ServerCommandSource> context, final ChatRooms room) throws CommandSyntaxException {
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
    private static int switchToChatRoom(final CommandContext<ServerCommandSource> context, final ChatRooms chatRoom) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Update the chat room
        ((PlayerChat) player).setChatRoom(chatRoom);
        
        // Tell the player
        player.sendSystemMessage(TranslatableServerSide.text(player, "chat.change." + chatRoom.name().toLowerCase()), Util.NIL_UUID);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int playerMute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
    private static int opMute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
