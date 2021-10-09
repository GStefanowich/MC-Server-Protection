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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.ServerCore;
import net.TheElm.project.commands.ArgumentTypes.ArgumentSuggestions;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.ColorUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.text.TextUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public final class NickNameCommand {
    
    private static final ExceptionTranslatableServerSide NAME_TOO_LONG = TranslatableServerSide.exception("player.nick.too_long");
    private static final int NICK_MAX_LENGTH = 20;
    
    private NickNameCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "Nick", (builder) -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.DO_PLAYER_NICKS))
            .then(CommandManager.literal("reset")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                    .requires(CommandPredicate.opLevel(OpLevels.KICK_BAN_OP))
                    .executes((context) -> {
                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "target");
                        return NickNameCommand.setNickForPlayer(player, null);
                    })
                )
                .executes((context) -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    return NickNameCommand.setNickForPlayer(player, null);
                })
            )
            .then(CommandManager.argument("nick", StringArgumentType.string())
                .requires(CommandPredicate.node(Permissions.PLAYER_NICKNAME))
                .then(CommandManager.argument("color", StringArgumentType.string())
                    .requires(CommandPredicate.node(Permissions.PLAYER_NICKNAME_COLOR))
                    .suggests(ArgumentSuggestions::suggestColors)
                    .then(CommandManager.argument("ends", StringArgumentType.string())
                        .requires(CommandPredicate.node(Permissions.PLAYER_NICKNAME_COLOR_GRADIENT))
                        .suggests(ArgumentSuggestions::suggestColors)
                        .executes(NickNameCommand::commandNickSetNamedColorRange)
                    )
                    .executes(NickNameCommand::commandNickSetNamedColor)
                )
                .executes(NickNameCommand::commandNickSet)
            )
        );
    }
    
    private static @Nullable String commandVerifyNick(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        String nick = StringArgumentType.getString(context, "nick");
        if (nick.isEmpty())
            return null;
        else if (nick.length() > NickNameCommand.NICK_MAX_LENGTH)
            throw NickNameCommand.NAME_TOO_LONG.create(context.getSource(), NickNameCommand.NICK_MAX_LENGTH);
        
        if (SewConfig.get(SewConfig.NICKNAME_COST) > 0) {
            try {
                if (!MoneyUtils.takePlayerMoney(player, SewConfig.get(SewConfig.NICKNAME_COST)))
                    return null;
            } catch (NotEnoughMoneyException e) {
                return null;
            }
        }
        
        return nick;
    }
    private static int commandNickSet(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get command runtime information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get the nickname
        String nickname = NickNameCommand.commandVerifyNick(context);
        if (nickname == null)
            return 0;
        
        return NickNameCommand.setNickForPlayer(player, nickname);
    }
    private static int commandNickSetNamedColor(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get the nickname
        String nickname = NickNameCommand.commandVerifyNick(context);
        if (nickname == null)
            return 0;
        
        // Get the color
        Color color = ColorUtils.getRawColor(StringArgumentType.getString(context, "color"));
        if (color == null)
            return 0;
        
        return NickNameCommand.setNickForPlayer(player, nickname, color);
    }
    private static int commandNickSetNamedColorRange(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get the nickname
        String nickname = NickNameCommand.commandVerifyNick(context);
        if (nickname == null)
            return 0;
        
        // Get the color
        Color starts = ColorUtils.getRawColor(StringArgumentType.getString(context, "color"));
        Color ends = ColorUtils.getRawColor(StringArgumentType.getString(context, "ends"));
        if (starts == null || ends == null)
            return 0;
        return NickNameCommand.setNickForPlayer(player, nickname, starts, ends);
    }
    
    private static int commandNickSetOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String nickname = StringArgumentType.getString(context, "nick");
        return NickNameCommand.setNickForPlayer(player, nickname);
    }
    
    private static int setNickForPlayer(@NotNull final ServerPlayerEntity player, @Nullable final String nickname) {
        return NickNameCommand.setNickForPlayer(player, nickname, null);
    }
    private static int setNickForPlayer(@NotNull final ServerPlayerEntity player, @Nullable final String nickname, @Nullable final Color color) {
        return NickNameCommand.setNickForPlayer(player, nickname, color, color);
    }
    private static int setNickForPlayer(@NotNull final ServerPlayerEntity player, @Nullable final String nickname, @Nullable final Color starts, @Nullable final Color ends) {
        MutableText notifyMessage;
        Text newName = null;
        if (nickname == null)
            notifyMessage = TranslatableServerSide.text(player, "player.nick.reset");
        else {
            // Format the text with a color
            newName = TextUtils.literal(nickname, starts, ends);
            notifyMessage = TranslatableServerSide.text(player, "player.nick.updated", newName);
        }
        
        // Update the players display name
        ((Nicknamable) player).setPlayerNickname(newName);
        player.sendMessage(notifyMessage.formatted(Formatting.YELLOW), false);
        
        // Update the name in the claim cache
        ((PlayerData)player).getClaim().updateName();
        ClaimCommand.notifyChangedClaimed(player.getUuid());
        
        // Send update to the player list
        ServerCore.get(player).getPlayerManager().sendToAll(
            (new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, player))
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
}
