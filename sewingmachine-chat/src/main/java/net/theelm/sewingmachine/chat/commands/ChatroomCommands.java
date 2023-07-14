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
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.chat.config.SewChatConfig;
import net.theelm.sewingmachine.chat.interfaces.PlayerChat;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.objects.MessageRegion;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public final class ChatroomCommands extends SewCommand {
    private static final SimpleCommandExceptionType MUTE_EXEMPT = new SimpleCommandExceptionType(Text.literal("That player is exempt from being muted."));
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "Chat", builder -> builder
            .then(CommandManager.argument("room", StringArgumentType.word())
                .suggests(this::getRoomSuggestions)
                .then(CommandManager.argument("text", MessageArgumentType.message())
                    .executes(this::sendToChatRoom)
                )
                .executes(this::switchToChatRoom)
            )
        );
        
        // TODO: Add Mute permission node
        // TODO: Add shadow mute
        CommandUtils.register(dispatcher, "Mute", builder -> builder
            .requires(CommandPredicate.isEnabled(SewChatConfig.CHAT_MODIFY)
                .and(
                    CommandPredicate.isEnabled(SewChatConfig.CHAT_MUTE_SELF)
                        .or(CommandPredicate.isEnabled(SewChatConfig.CHAT_MUTE_OP).and(OpLevels.KICK_BAN_OP))
                )
            )
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .suggests(CommandUtils::getOnlineNames)
                .then(CommandManager.literal("global")
                    .requires(CommandPredicate.isEnabled(SewChatConfig.CHAT_MUTE_OP)
                        .and(
                            CommandPredicate.opLevel(OpLevels.KICK_BAN_OP)
                                .or(PermissionNodes.CHAT_COMMAND_MUTE)
                        )
                    )
                    .executes(this::opMute)
                )
                .executes(this::playerMute)
            )
        );
    }
    
    private int sendToChatRoom(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource()
            .getPlayerOrThrow();
        MessageRegion room = this.getChatRoom(context);
        if (room != null) {
            if (((PlayerChat)player).isMuted()) {
                player.sendMessage(ServerText.text(
                    player,
                    "chat.muted",
                    room
                ).formatted(Formatting.RED));
            } else {
                MessageArgumentType.getSignedMessage(context, "text", message -> {
                    room.broadcast(player, null, Collections.emptyList(), message);
                });
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int switchToChatRoom(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource()
            .getPlayerOrThrow();
        MessageRegion room = this.getChatRoom(context);
        
        if (room != null) {
            // Update the chat room
            ((PlayerChat) player).setChatRoom(room);
            
            // Tell the player
            player.sendMessage(ServerText.text(player, "chat.change." + room.name().toLowerCase()));
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private @Nullable MessageRegion getChatRoom(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String room = StringArgumentType.getString(context, "room");
        MessageRegion region = MessageRegion.get(room);
        return region.enabled(context.getSource()) ? region : null;
    }
    
    private @NotNull CompletableFuture<Suggestions> getRoomSuggestions(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) {
        ServerCommandSource source = context.getSource();
        return CommandSource.suggestMatching(
            MessageRegion.get()
                .stream()
                .filter(region -> region.enabled(source))
                .map(MessageRegion::name),
            builder
        );
    }
    
    private int playerMute(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (!SewConfig.get(SewChatConfig.CHAT_MUTE_SELF))
            return this.opMute(context);
        
        ServerCommandSource source = context.getSource();
        PlayerChat self = (PlayerChat)(source.getPlayer());
        
        ServerPlayerEntity target = EntityArgumentType.getPlayer( context, "player" );
        
        source.sendFeedback(
            () -> ServerText.text(
                source,
                (self.toggleMute( target.getGameProfile() ) ? "chat.mute.player" : "chat.unmute.player"),
                target.getDisplayName()
            ).formatted(Formatting.GREEN),
            false
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int opMute(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        PlayerChat chatter = (PlayerChat) target;
        
        if (Permissions.check(target, PermissionNodes.CHAT_COMMAND_MUTE_EXEMPT.getNode()) || target.hasPermissionLevel(1))
            throw MUTE_EXEMPT.create();
        else {
            source.sendFeedback(
                () -> ServerText.text(
                    source,
                    (chatter.toggleMute() ? "chat.mute.global" : "chat.unmute.global"),
                    target.getDisplayName()
                ).formatted(Formatting.GREEN),
                false
            );
            
            return Command.SINGLE_SUCCESS;
        }

    }
}