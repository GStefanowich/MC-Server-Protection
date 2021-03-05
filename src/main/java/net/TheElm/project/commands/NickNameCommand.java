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
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.commands.ArgumentTypes.ArgumentSuggestions;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.ColorUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NickNameCommand {
    
    private NickNameCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("nick")
            .requires((source) -> SewConfig.get(SewConfig.DO_PLAYER_NICKS))
            .then(CommandManager.literal("reset")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                    .requires((source) -> source.hasPermissionLevel(OpLevels.KICK_BAN_OP))
                    .executes((context) -> {
                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "target");
                        return setNickForPlayer(player, null);
                    })
                )
                .executes((context) -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    return setNickForPlayer(player, null);
                })
            )
            .then(CommandManager.argument("nick", StringArgumentType.string())
                .requires((source) -> (!SewConfig.get(SewConfig.HANDLE_PERMISSIONS)) || RankUtils.hasPermission(source, Permissions.PLAYER_NICKNAME))
                .then(CommandManager.argument("color", StringArgumentType.greedyString())
                    .suggests(ArgumentSuggestions::suggestColors)
                    .executes(NickNameCommand::commandNickSetNamedColor)
                )
                .executes(NickNameCommand::commandNickSet)
            )
        );
        CoreMod.logDebug("- Registered Nick command");
    }
    
    private static int commandNickSet(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        if (SewConfig.get(SewConfig.NICKNAME_COST) > 0) {
            try {
                if (!MoneyUtils.takePlayerMoney(player, SewConfig.get(SewConfig.NICKNAME_COST)))
                    return Command.SINGLE_SUCCESS;
            } catch (NotEnoughMoneyException e) {
                return Command.SINGLE_SUCCESS;
            }
        }
        
        return NickNameCommand.setNickForPlayer(player, StringArgumentType.getString(context, "nick"));
    }
    
    private static int commandNickSetNamedColor(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        String nickname = StringArgumentType.getString(context, "nick");
        
        TextColor color = ColorUtils.getRawColor(StringArgumentType.getString(context, "color"));
        if (color == null)
            return 0;
        return NickNameCommand.setNickForPlayer(player, nickname, color);
    }
    
    private static int commandNickSetOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer( context, "player" );
        String nickname = StringArgumentType.getString( context, "nick" );
        return NickNameCommand.setNickForPlayer(player, nickname);
    }
    
    private static int setNickForPlayer(@NotNull ServerPlayerEntity player, @Nullable String nickname) {
        return NickNameCommand.setNickForPlayer(player, nickname, null);
    }
    private static int setNickForPlayer(@NotNull ServerPlayerEntity player, @Nullable String nickname, @Nullable TextColor color) {
        if (nickname == null)
            ((Nicknamable)player).setPlayerNickname(null);
        else {
            // Format the text with a color
            MutableText text = new LiteralText(nickname);
            if (color != null)
                text.styled(style -> style.withColor(color));
            
            // Update the players display name
            ((Nicknamable)player).setPlayerNickname(text);
            
            player.sendMessage(new TranslatableText(World.OVERWORLD.getValue().toString()), false);
        }
        
        // Update the name in the claim cache
        ((PlayerData)player).getClaim().updateName();
        ClaimCommand.notifyChangedClaimed(player.getUuid());
        
        // Send update to the player list
        ServerCore.get().getPlayerManager().sendToAll(
            (new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, player))
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
}
