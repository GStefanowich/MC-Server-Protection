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

package net.theelm.sewingmachine.protection.utilities;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.interfaces.WhitelistedPlayer;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.objects.ServerClaimCache;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class CommandClaimUtils {
    private CommandClaimUtils() {}
    
    public static @NotNull CompletableFuture<Suggestions> getAllTowns(@NotNull CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerClaimCache claimCache = ((ClaimsAccessor)source.getServer())
            .getClaimManager();
        
        Set<String> townNames = new HashSet<>();
        
        claimCache.getTownCaches()
            .forEach(town -> townNames.add(town.getName().getString()));
        
        return CommandSource.suggestMatching(
            townNames,
            builder
        );
    }
    
    public static boolean playerIsInTown(@NotNull ServerCommandSource serverCommandSource) {
        Entity source = serverCommandSource.getEntity();
        if (!( source instanceof ServerPlayerEntity ))
            return false;
        
        ServerPlayerEntity player = (ServerPlayerEntity) source;
        ClaimantPlayer claim;
        return (((claim = ((PlayerClaimData) player).getClaim()) != null) && (claim.getTown() != null));
    }
    
    public static @NotNull CompletableFuture<Suggestions> getFriendPlayerNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get only a list of friends
        Set<String> userNames = new HashSet<>(CommandClaimUtils.getFriendNames(server, player));
        
        // Add all users if using Search
        if (!builder.getRemaining().isEmpty())
            userNames.addAll(CommandClaimUtils.getFriendWhitelistedNames(server, player));
        
        return CommandSource.suggestMatching(
            userNames,
            builder
        );
    }
    
    public static @NotNull List<String> getFriendWhitelistedNames(@NotNull final MinecraftServer server, @NotNull final ServerPlayerEntity player) {
        ClaimantPlayer claimant = ((ClaimsAccessor)server).getClaimManager()
            .getPlayerClaim(player);
        PlayerManager playerManager = server.getPlayerManager();
        
        return playerManager.getWhitelist().values().stream()
            .map(entry -> (WhitelistedPlayer)entry)
            .filter(claimant::isFriend)
            .map(WhitelistedPlayer::getName)
            .toList();
    }
    public static @NotNull List<String> getFriendNames(@NotNull final MinecraftServer server, @NotNull final ServerPlayerEntity player) {
        ClaimantPlayer claimant = ((ClaimsAccessor)server).getClaimManager()
            .getPlayerClaim(player);
        PlayerManager playerManager = server.getPlayerManager();

        return playerManager.getPlayerList().stream()
            .filter(claimant::isFriend)
            .map(PlayerEntity::getEntityName)
            .toList();
    }
}
