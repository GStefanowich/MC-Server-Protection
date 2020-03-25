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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.interfaces.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.GameMode;

public final class GameModesCommand {
    
    private GameModesCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register( CommandManager.literal("gms" )
            .requires((source) -> source.hasPermissionLevel( 2 ) || ((CommandSource)source).hasPermission("player.gamemode.survival"))
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.SURVIVAL ))
        );
        CoreMod.logDebug( "- Registered GMS command" );
        
        dispatcher.register( CommandManager.literal("gmc" )
            .requires((source) -> source.hasPermissionLevel( 2 ) || ((CommandSource)source).hasPermission("player.gamemode.creative"))
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.CREATIVE ))
        );
        CoreMod.logDebug( "- Registered GMC command" );
        
        dispatcher.register( CommandManager.literal("gma" )
            .requires((source) -> source.hasPermissionLevel( 2 ) || ((CommandSource)source).hasPermission("player.gamemode.adventure"))
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.ADVENTURE ))
        );
        CoreMod.logDebug( "- Registered GMA command" );
        
        dispatcher.register( CommandManager.literal("gmsp" )
            .requires((source) -> source.hasPermissionLevel( 2 ) || ((CommandSource)source).hasPermission("player.gamemode.spectator"))
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.SPECTATOR ))
        );
        CoreMod.logDebug( "- Registered GMSP command" );
        
    }
    
    private static int setPlayerGameMode(ServerCommandSource source, GameMode gameMode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        Text gmText = new TranslatableText("gameMode." + gameMode.getName());
        
        source.sendFeedback(new TranslatableText("commands.gamemode.success.self", gmText), true);
        player.setGameMode( gameMode );
        
        return Command.SINGLE_SUCCESS;
    }
    
}
