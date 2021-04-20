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
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.permissions.PermissionNode;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class CommandUtils {
    
    public static @NotNull CompletableFuture<Suggestions> getOnlinePlayerNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) {
        PlayerManager manager = context.getSource().getMinecraftServer().getPlayerManager();
        return CommandSource.suggestMatching(manager.getPlayerList().stream()
            .map(( player ) -> player.getGameProfile().getName()), builder);
    }
    public static @NotNull CompletableFuture<Suggestions> getAllPlayerNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        // Get the server that the command came from
        MinecraftServer server = context.getSource()
            .getMinecraftServer();
        
        Set<String> userNames = new HashSet<>(CommandUtils.getOnlinePlayerNames(server));
        
        // Add all users
        if (!builder.getRemaining().isEmpty())
            userNames.addAll(CommandUtils.getWhitelistedNames(server));
        
        // Return the suggestion handler
        return CommandSource.suggestMatching(
            userNames,
            builder
        );
    }
    public static @NotNull CompletableFuture<Suggestions> getFriendPlayerNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return CommandSource.suggestMatching(
            CommandUtils.getFriendPlayerNames(source.getMinecraftServer(), source.getPlayer()),
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
    
    public static boolean playerIsInTown(@NotNull ServerCommandSource serverCommandSource) {
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
    public static @NotNull Stream<String> getFriendPlayerNames(@NotNull final MinecraftServer server, @NotNull final ServerPlayerEntity player) {
        ClaimantPlayer claimant = ClaimantPlayer.get(player);
        PlayerManager playerManager = server.getPlayerManager();
        
        return playerManager.getPlayerList().stream()
            .filter(entity -> claimant.isFriend(entity.getUuid()))
            .map(entity -> entity.getName()
                .asString());
    }
    
    private static @NotNull Predicate<ServerCommandSource> OP_LEVEL_1 = source -> source.hasPermissionLevel(OpLevels.SPAWN_PROTECTION);
    private static @NotNull Predicate<ServerCommandSource> OP_LEVEL_2 = source -> source.hasPermissionLevel(OpLevels.CHEATING);
    private static @NotNull Predicate<ServerCommandSource> OP_LEVEL_3 = source -> source.hasPermissionLevel(OpLevels.KICK_BAN_OP);
    private static @NotNull Predicate<ServerCommandSource> OP_LEVEL_4 = source -> source.hasPermissionLevel(OpLevels.STOP);
    
    public static @NotNull Predicate<ServerCommandSource> isEnabled(ConfigOption<Boolean> configLevel) {
        return source -> SewConfig.get(configLevel);
    }
    public static @NotNull Predicate<ServerCommandSource> requires(final int level) {
        switch (level) {
            case 1: return CommandUtils.OP_LEVEL_1;
            case 2: return CommandUtils.OP_LEVEL_2;
            case 3: return CommandUtils.OP_LEVEL_3;
            default: return CommandUtils.OP_LEVEL_4;
        }
    }
    public static @NotNull Predicate<ServerCommandSource> requires(ConfigOption<Integer> configLevel) {
        return source -> source.hasPermissionLevel(SewConfig.get(configLevel));
    }
    public static @NotNull Predicate<ServerCommandSource> requires(@NotNull final PermissionNode permission) {
        return source -> RankUtils.hasPermission(source, permission);
    }
    public static @NotNull Predicate<ServerCommandSource> either(final int level, @NotNull final PermissionNode permission) {
        Predicate<ServerCommandSource> predicate = CommandUtils.requires(level);
        return source -> predicate.test(source) || RankUtils.hasPermission(source, permission);
    }
    public static @NotNull Predicate<ServerCommandSource> either(@NotNull final ConfigOption<Integer> level, @NotNull final PermissionNode permission) {
        Predicate<ServerCommandSource> predicate = CommandUtils.requires(level);
        return source -> predicate.test(source) || RankUtils.hasPermission(source, permission);
    }
    public static @NotNull Predicate<ServerCommandSource> isEnabledAnd(@NotNull final ConfigOption<Boolean> enabled, @NotNull final ConfigOption<Integer> permission) {
        return source -> SewConfig.get(enabled) && source.hasPermissionLevel(SewConfig.get(permission));
    }
    public static @NotNull Predicate<ServerCommandSource> isEnabledAnd(@NotNull final ConfigOption<Boolean> enabled, @NotNull int permission) {
        return source -> SewConfig.get(enabled) && source.hasPermissionLevel(permission);
    }
    public static @NotNull Predicate<ServerCommandSource> isEnabledOr(@NotNull final ConfigOption<Boolean> enabled, @NotNull final PermissionNode permission) {
        return source -> SewConfig.get(enabled) || RankUtils.hasPermission(source, permission);
    }
    public static @NotNull Predicate<ServerCommandSource> isEnabledOr(@NotNull final ConfigOption<Boolean> enabled, @NotNull final ConfigOption<Integer> permission) {
        return source -> SewConfig.get(enabled) || source.hasPermissionLevel(SewConfig.get(permission));
    }
    public static @NotNull Predicate<ServerCommandSource> isEnabledOr(@NotNull final ConfigOption<Boolean> enabled, @NotNull final int permission) {
        return source -> SewConfig.get(enabled) || source.hasPermissionLevel(permission);
    }
    public static @NotNull Predicate<ServerCommandSource> isEnabledAndEither(@NotNull final ConfigOption<Boolean> enabled, @NotNull final ConfigOption<Integer> level, @NotNull final PermissionNode permission) {
        Predicate<ServerCommandSource> predicate = CommandUtils.either(level, permission);
        return source -> SewConfig.get(enabled) && predicate.test(source);
    }
}
