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
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class MiscCommands {
    
    private static String FLIP = "(╯°□°)╯︵ ┻━┻";
    private static String SHRUG = "¯\\_(ツ)_/¯";
    
    private MiscCommands() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "shrug", builder -> builder
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_SHRUG))
            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                .executes(MiscCommands::shrugMessage)
            )
            .executes(MiscCommands::shrug)
        );
        
        ServerCore.register(dispatcher, "tableflip", builder -> builder
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_TABLEFLIP))
            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                .executes(MiscCommands::flipMessage)
            )
            .executes(MiscCommands::flip)
        );
    }
    
    private static int shrug(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        return MiscCommands.playerSendsMessageAndData(player,"", MiscCommands.SHRUG);
    }
    private static int shrugMessage(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String text = StringArgumentType.getString( context, "message" );
        return MiscCommands.playerSendsMessageAndData(player, text, MiscCommands.SHRUG);
    }
    private static int flip(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        return MiscCommands.playerSendsMessageAndData(player,"", MiscCommands.FLIP);
    }
    private static int flipMessage(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String text = StringArgumentType.getString( context, "message" );
        return MiscCommands.playerSendsMessageAndData(player, text, MiscCommands.FLIP);
    }
    
    public static int playerSendsMessageAndData(@NotNull ServerPlayerEntity player, @NotNull String message, @NotNull String main) {
        return MiscCommands.playerSendsMessageAndData(player, message, new LiteralText( main ));
    }
    public static int playerSendsMessageAndData(@NotNull ServerPlayerEntity player, @NotNull String message, @NotNull Text main) {
        MutableText text;
        if (SewConfig.get(SewConfig.CHAT_MODIFY)) {
            // Create the player display for chat
            text = PlayerNameUtils.getPlayerChatDisplay(player, ((PlayerChat) player).getChatRoom())
                .append(new LiteralText(": ").formatted(Formatting.GRAY));
            
            // Append the users message
            if (!"".equals(message))
                text.append(message)
                    .append(" ");
            
            // Append the main information
            text.append(main);
        } else {
            text = new TranslatableText("chat.type.text", player.getDisplayName(), main);
        }
        
        // Send to all players
        MessageUtils.sendTo(
        ((PlayerChat) player).getChatRoom(),
        player,
        text
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
}
