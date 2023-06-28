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

package net.theelm.sewingmachine.utilities;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.interfaces.WhitelistedPlayer;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class CommandUtils {
    private CommandUtils() {}
    
    public static @NotNull CompletableFuture<Suggestions> getOnlineNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) {
        PlayerManager manager = context.getSource().getServer().getPlayerManager();
        return CommandSource.suggestMatching(manager.getPlayerList().stream()
            .map(( player ) -> player.getGameProfile().getName()), builder);
    }
    public static @NotNull CompletableFuture<Suggestions> getAllPlayerNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        // Get the server that the command came from
        MinecraftServer server = context.getSource()
            .getServer();
        
        // Get a list of all online player names
        Set<String> userNames = new HashSet<>(CommandUtils.getOnlineNames(server));
        
        // Add all users if using Search
        if (!builder.getRemaining().isEmpty())
            userNames.addAll(CommandUtils.getWhitelistedNames(server));
        
        // Return the suggestion handler
        return CommandSource.suggestMatching(
            userNames,
            builder
        );
    }
    
    public static @NotNull List<String> getOnlineNames(@NotNull final MinecraftServer server) {
        PlayerManager playerManager = server.getPlayerManager();
        return Arrays.asList(playerManager.getPlayerNames());
    }
    public static @NotNull List<String> getWhitelistedNames(@NotNull final MinecraftServer server) {
        PlayerManager playerManager = server.getPlayerManager();
        return Arrays.asList(playerManager.getWhitelistedNames());
    }
    
    public static @NotNull <S> Command<S> command(@NotNull Command<S> command) {
        if (DevUtils.isDebugging())
            return new CommandExceptionable<>(command, CoreMod::logError);
        return command;
    }
    
    public static boolean isSourcePlayer(@NotNull ServerCommandSource source) {
        return source.getEntity() instanceof PlayerEntity;
    }
    public static boolean isDedicatedServer(@NotNull ServerCommandSource source) {
        return source.getServer() instanceof DedicatedServer;
    }
    
    private static class CommandExceptionable<S> implements Command<S> {
        private final @NotNull Command<S> command;
        private final @NotNull Consumer<Exception> handler;
        
        private CommandExceptionable(@NotNull Command<S> command, @NotNull Consumer<Exception> handler) {
            this.command = command;
            this.handler = handler;
        }
        
        @Override
        public int run(CommandContext<S> context) throws CommandSyntaxException {
            try {
                return this.command.run(context);
            } catch (Exception e) {
                if (!(e instanceof CommandSyntaxException))
                    this.handler.accept(e);
                throw e;
            }
        }
    }
}
