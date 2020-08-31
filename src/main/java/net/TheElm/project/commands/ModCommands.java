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
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.PlayerPermissions;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.io.IOException;

public final class ModCommands {
    private ModCommands() {
    }
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sewingmachine")
            .requires((source) -> source.hasPermissionLevel(OpLevels.STOP))
            .then(CommandManager.literal("reload")
                .then(CommandManager.literal("config")
                    .executes(ModCommands::ReloadConfig)
                )
                .then(CommandManager.literal("permissions")
                    .requires((source) -> SewConfig.get(SewConfig.HANDLE_PERMISSIONS))
                    .executes(ModCommands::ReloadPermissions)
                )
            )
        );
        
        CoreMod.logDebug("- Registered SewingMachine command");
    }
    
    private static int ReloadConfig(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            // Reload the config
            SewConfig.reload();
            source.sendFeedback(new LiteralText("Config has been reloaded.").formatted(Formatting.GREEN), true);
            
            // Re-send the command-tree to all players
            ModCommands.ReloadCommandTree(source.getMinecraftServer(), false);
            
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendFeedback(new LiteralText("Failed to reload config, see console for errors.").formatted(Formatting.RED), true);
            CoreMod.logError( e );
            return -1;
        }
    }
    
    private static int ReloadPermissions(CommandContext<ServerCommandSource> context) {
        boolean success = RankUtils.reload();
        ServerCommandSource source = context.getSource();
        
        if (!success)
            source.sendFeedback(new LiteralText("Failed to reload permissions, see console for errors").formatted(Formatting.RED), true);
        else{
            ModCommands.ReloadCommandTree(source.getMinecraftServer(), true);
            source.sendFeedback(new LiteralText("Permissions file has been reloaded").formatted(Formatting.GREEN), true);
        }
        
        return success ? Command.SINGLE_SUCCESS : -1;
    }
    
    private static void ReloadCommandTree(MinecraftServer server, boolean reloadPermissions) {
        PlayerManager playerManager = server.getPlayerManager();
        
        // For all players
        for (ServerPlayerEntity player : playerManager.getPlayerList()) {
            // Clear permissions
            if (reloadPermissions)
                ((PlayerPermissions) player).resetRanks();
            
            // Resend the player the command tree
            playerManager.sendCommandTree(player);
        }
    }
    
}