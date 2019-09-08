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
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class MiscCommands {
    
    private static String FLIP = "(╯°□°)╯︵ ┻━┻";
    private static String SHRUG = "¯\\_(ツ)_/¯";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (SewingMachineConfig.INSTANCE.COMMAND_SHRUG.get()) {
            dispatcher.register(CommandManager.literal("shrug")
                    .then(CommandManager.argument("message", StringArgumentType.greedyString())
                            .executes(MiscCommands::shrugMessage)
                    )
                    .executes(MiscCommands::shrug)
            );
            CoreMod.logDebug("- Registered Nick command");
        }
        if (SewingMachineConfig.INSTANCE.COMMAND_TABLEFLIP.get()) {
            dispatcher.register(CommandManager.literal("tableflip")
                    .then(CommandManager.argument("message", StringArgumentType.greedyString())
                            .executes(MiscCommands::flipMessage)
                    )
                    .executes(MiscCommands::flip)
            );
            CoreMod.logDebug("- Registered Nick command");
        }
    }
    
    private static int shrug(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        return MiscCommands.playerSendsMessageAndData(player,"", MiscCommands.SHRUG);
    }
    private static int shrugMessage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String text = StringArgumentType.getString( context, "message" );
        return MiscCommands.playerSendsMessageAndData(player, text, MiscCommands.SHRUG);
    }
    private static int flip(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        return MiscCommands.playerSendsMessageAndData(player,"", MiscCommands.FLIP);
    }
    private static int flipMessage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String text = StringArgumentType.getString( context, "message" );
        return MiscCommands.playerSendsMessageAndData(player, text, MiscCommands.FLIP);
    }
    
    public static int playerSendsMessageAndData(ServerPlayerEntity player, String message, String main) {
        return MiscCommands.playerSendsMessageAndData(player, message, new LiteralText( main ));
    }
    public static int playerSendsMessageAndData(ServerPlayerEntity player, String message, Text main ) {
        // Get Server
        MinecraftServer server = player.getServer();
        
        // Create the player display for chat
        Text text = PlayerNameUtils.getPlayerChatDisplay( player, ((PlayerChat) player).getChatRoom() )
            .append(new LiteralText( ": " ).formatted(Formatting.GRAY));
        
        // Append the users message
        if ( !"".equals( message ) )
            text.append( message )
                .append( " " );
        
        // Append the main information
        text.append( main );
        
        // Send to all players
        if (server != null) server.getPlayerManager().sendToAll(text);
        
        return Command.SINGLE_SUCCESS;
    }
    
}
