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

package net.theelm.sewingmachine.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.theelm.sewingmachine.utilities.CommandUtils;
import org.jetbrains.annotations.NotNull;

public final class GameModesCommand extends SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "gms", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_SURVIVAL))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_SURVIVAL.onOther()))
                .executes((context) -> this.setPlayerGameMode(context, GameMode.SURVIVAL))
            )
            .executes((context) -> this.setPlayerGameMode(context.getSource(), GameMode.SURVIVAL))
        );
        
        CommandUtils.register(dispatcher, "gmc", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_CREATIVE))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_CREATIVE.onOther()))
                .executes((context) -> this.setPlayerGameMode(context, GameMode.CREATIVE))
            )
            .executes((context) -> this.setPlayerGameMode(context.getSource(), GameMode.CREATIVE))
        );
        
        CommandUtils.register(dispatcher, "gma", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_ADVENTURE))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_ADVENTURE.onOther()))
                .executes((context) -> this.setPlayerGameMode(context, GameMode.ADVENTURE))
            )
            .executes((context) -> this.setPlayerGameMode(context.getSource(), GameMode.ADVENTURE))
        );
        
        CommandUtils.register(dispatcher, "gmsp", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_SPECTATOR))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GAMEMODE_SPECTATOR.onOther()))
                .executes((context) -> this.setPlayerGameMode(context, GameMode.SPECTATOR))
            )
            .executes((context) -> this.setPlayerGameMode(context.getSource(), GameMode.SPECTATOR))
        );
    }
    
    private int setPlayerGameMode(@NotNull ServerCommandSource source, @NotNull GameMode gameMode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Text gmText = Text.translatable("gameMode." + gameMode.getName());
        
        source.sendFeedback(() -> Text.translatable("commands.gamemode.success.self", gmText), true);
        player.changeGameMode(gameMode);
        
        return Command.SINGLE_SUCCESS;
    }
    private int setPlayerGameMode(@NotNull CommandContext<ServerCommandSource> context, @NotNull GameMode gameMode) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Text gmText = Text.translatable("gameMode." + gameMode.getName());
        
        source.sendFeedback(() -> Text.translatable("commands.gamemode.success.other", player.getDisplayName(), gmText), true);
        player.changeGameMode(gameMode);
        player.sendMessage(Text.translatable("gameMode.changed", gmText));
        
        return Command.SINGLE_SUCCESS;
    }
    
}
