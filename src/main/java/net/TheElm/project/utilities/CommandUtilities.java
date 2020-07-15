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

package net.TheElm.project.utilities;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.TheElm.project.CoreMod;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class CommandUtilities {
    
    public static @NotNull CompletableFuture<Suggestions> getOnlinePlayerNames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        PlayerManager manager = context.getSource().getMinecraftServer().getPlayerManager();
        return CommandSource.suggestMatching(manager.getPlayerList().stream()
            .map(( player ) -> player.getGameProfile().getName()), builder);
    }
    public static @NotNull CompletableFuture<Suggestions> getAllPlayerNames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        // Get the server that the command came from
        MinecraftServer server = context.getSource()
            .getMinecraftServer();
        
        Set<String> userNames = new HashSet<>(CommandUtilities.getOnlinePlayerNames(server));
        
        // Add all users
        if (!builder.getRemaining().isEmpty())
            userNames.addAll(CommandUtilities.getWhitelistedNames(server));
        
        // Return the suggestion handler
        return CommandSource.suggestMatching(
            userNames,
            builder
        );
    }
    
    public static @NotNull CompletableFuture<Suggestions> getAllTowns(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Set<String> townNames = new HashSet<>();
    
        Stream<ClaimantTown> towns = CoreMod.getCacheStream(ClaimantTown.class);
        towns.forEach(town -> townNames.add(town.getName().getString()));
        
        return CommandSource.suggestMatching(
            townNames,
            builder
        );
    }
    
    public static boolean playerIsInTown(ServerCommandSource serverCommandSource) {
        Entity source = serverCommandSource.getEntity();
        if (!( source instanceof ServerPlayerEntity ))
            return false;
        
        ServerPlayerEntity player = (ServerPlayerEntity) source;
        ClaimantPlayer claim;
        return (((claim = ((PlayerData)player).getClaim()) != null) && (claim.getTown() != null));
    }
    
    public static @NotNull List<String> getOnlinePlayerNames(@NotNull final MinecraftServer server) {
        PlayerManager playerManager = server.getPlayerManager();
        return Arrays.asList(playerManager.getPlayerNames());
    }
    public static @NotNull List<String> getWhitelistedNames(@NotNull final MinecraftServer server) {
        PlayerManager playerManager = server.getPlayerManager();
        return Arrays.asList(playerManager.getWhitelistedNames());
    }
    
}
