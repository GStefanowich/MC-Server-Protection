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
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.utilities.CommandUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;

public final class GameModesCommand {
    
    private GameModesCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "gms", builder -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_SURVIVAL))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_SURVIVAL.onOther()))
                .executes((context) -> setPlayerGameMode(context, GameMode.SURVIVAL))
            )
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.SURVIVAL ))
        );
        
        ServerCore.register(dispatcher, "gmc", builder -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_CREATIVE))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                    .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_CREATIVE.onOther()))
                    .executes((context) -> setPlayerGameMode(context, GameMode.CREATIVE))
            )
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.CREATIVE ))
        );
        
        ServerCore.register(dispatcher, "gma", builder -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_ADVENTURE))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_ADVENTURE.onOther()))
                .executes((context) -> setPlayerGameMode(context, GameMode.ADVENTURE))
            )
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.ADVENTURE ))
        );
        
        ServerCore.register(dispatcher, "gmsp", builder -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_SPECTATOR))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GAMEMODE_SPECTATOR.onOther()))
                .executes((context) -> setPlayerGameMode(context, GameMode.SPECTATOR))
            )
            .executes((context) -> setPlayerGameMode(context.getSource(), GameMode.SPECTATOR ))
        );
    }
    
    private static int setPlayerGameMode(ServerCommandSource source, GameMode gameMode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        Text gmText = new TranslatableText("gameMode." + gameMode.getName());
        
        source.sendFeedback(new TranslatableText("commands.gamemode.success.self", gmText), true);
        player.setGameMode( gameMode );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int setPlayerGameMode(CommandContext<ServerCommandSource> context, GameMode gameMode) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Text gmText = new TranslatableText("gameMode." + gameMode.getName());
        
        source.sendFeedback(new TranslatableText("commands.gamemode.success.other", player.getDisplayName(), gmText), true);
        player.setGameMode(gameMode);
        player.sendSystemMessage(new TranslatableText("gameMode.changed", gmText), Util.NIL_UUID);
        
        return Command.SINGLE_SUCCESS;
    }
    
}
