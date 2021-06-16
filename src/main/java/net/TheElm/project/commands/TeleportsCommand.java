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

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.*;
import net.TheElm.project.utilities.WarpUtils.Warp;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeleportsCommand {
    public static final ExceptionTranslatableServerSide INVALID_HOME_CHARACTERS = TranslatableServerSide.exception("warp.notice.name.invalid");
    public static final ExceptionTranslatableServerSide INVALID_HOME_LENGTH = TranslatableServerSide.exception("warp.notice.name.too_long");
    public static final ExceptionTranslatableServerSide PLAYER_NOT_IN_SPAWN = TranslatableServerSide.exception("warp.notice.player.outside_spawn");
    public static final ExceptionTranslatableServerSide TARGET_NOT_IN_SPAWN = TranslatableServerSide.exception("warp.notice.target.outside_spawn");
    public static final ExceptionTranslatableServerSide TARGET_NOT_REQUESTING = TranslatableServerSide.exception("warp.notice.no_request");
    public static final ExceptionTranslatableServerSide TARGET_NO_WARP = TranslatableServerSide.exception("warp.notice.no_warp");
    public static final ExceptionTranslatableServerSide TARGET_NOT_ONLINE = TranslatableServerSide.exception("warp.notice.offline");
    
    private TeleportsCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "spawn", builder -> builder
            .requires(CommandUtils.requires(OpLevels.CHEATING))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .executes((context) -> {
                    // Get location information
                    ServerCommandSource source = context.getSource();
                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "player");
                    ServerWorld world = source.getMinecraftServer().getWorld(ServerCore.defaultWorldKey());
                    
                    for (ServerPlayerEntity player : players) {
                        WarpUtils.teleportPlayer(world, player, WarpUtils.getWorldSpawn(world));
                    }
                    
                    Text spawnText = new LiteralText("Spawn").formatted(Formatting.GOLD);
                    if (players.size() == 1) {
                        source.sendFeedback(new TranslatableText("commands.teleport.success.entity.single", ((Entity) players.iterator().next()).getDisplayName(), spawnText), true);
                    } else {
                        source.sendFeedback(new TranslatableText("commands.teleport.success.entity.multiple", players.size(), spawnText), true);
                    }
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
            .executes((context) -> {
                // Get location information
                ServerCommandSource source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                ServerWorld world = source.getMinecraftServer().getWorld(ServerCore.defaultWorldKey());
                
                WarpUtils.teleportPlayer(world, player, WarpUtils.getWorldSpawn(world));
                
                source.sendFeedback(new TranslatableText("commands.teleport.success.entity.single", player.getDisplayName(), new LiteralText("Spawn").formatted(Formatting.GOLD)), true);
                
                return Command.SINGLE_SUCCESS;
            })
        );
        
        ServerCore.register(dispatcher, "theend", "end teleport", builder -> builder
            .requires(CommandUtils.requires(OpLevels.CHEATING))
            .then(CommandManager.argument("players", EntityArgumentType.entities())
                .executes((context) -> TeleportsCommand.sendEntitiesToEnd(EntityArgumentType.getEntities(context, "players")))
            )
            .executes((context) -> {
                // Get location information
                ServerCommandSource source = context.getSource();
                return TeleportsCommand.sendEntitiesToEnd(Collections.singleton(source.getPlayer()));
            })
        );
        
        ServerCore.register(dispatcher, "tpa", builder -> builder
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .suggests(CommandUtils::getAllPlayerNames)
                .then(CommandManager.argument("location", StringArgumentType.string())
                    .suggests(TeleportsCommand::playerHomeNamesOfPlayer)
                    .executes(TeleportsCommand::tpaCommandNamed)
                )
                .executes(TeleportsCommand::tpaCommand)
            )
            .executes(TeleportsCommand::toPrimaryHomeCommand)
        );
        
        ServerCore.register(dispatcher, "tpaccept", builder -> builder
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(WarpUtils::hasWarp)
                .executes(TeleportsCommand::tpAcceptCommand)
            )
        );
        
        ServerCore.register(dispatcher, "tpdeny", builder -> builder
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(WarpUtils::hasWarp)
                .executes(TeleportsCommand::tpDenyCommand)
            )
        );
        
        ServerCore.register(dispatcher, "home", builder -> builder
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("location", StringArgumentType.string())
                .suggests(TeleportsCommand::playerHomeNames)
                .then(CommandManager.literal("rename")
                    .then(CommandManager.argument("new", StringArgumentType.string())
                        .executes(TeleportsCommand::renameHomeCommand)
                    )
                )
                .then(CommandManager.literal("set")
                    .then(CommandManager.literal("primary")
                        .executes(TeleportsCommand::setHomePrimaryCommand)
                    )
                )
                .executes(TeleportsCommand::toSpecificHomeCommand)
            )
            .executes(TeleportsCommand::toPrimaryHomeCommand)
        );
    }
    
    private static int toPrimaryHomeCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return TeleportsCommand.toSpecificHomeCommand(context, null);
    }
    private static int toSpecificHomeCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return TeleportsCommand.toSpecificHomeCommand(context, StringArgumentType.getString(context, "location"));
    }
    private static int toSpecificHomeCommand(@NotNull CommandContext<ServerCommandSource> context, @Nullable String location) throws CommandSyntaxException {
        // Get players info
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity porter = source.getPlayer();
        
        return TeleportsCommand.tpaToPlayer(
            source.getMinecraftServer(),
            porter,
            porter.getGameProfile(),
            location
        );
    }
    
    private static int renameHomeCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        final String oldName = StringArgumentType.getString(context, "location");
        final String newName = StringArgumentType.getString(context, "new");
        
        if (!IntUtils.between(1, newName.length(), 15))
            throw INVALID_HOME_LENGTH.create(player, 15);
        
        if (!WarpUtils.validateName(newName))
            throw INVALID_HOME_CHARACTERS.create(player);
        
        final Warp oldWarp = WarpUtils.getWarp(player, oldName);
        final Warp newWarp;
        if (oldWarp == null)
            throw TARGET_NO_WARP.create(player);
        
        // Check if the new warp exists
        boolean nameIsTaken = (newWarp = WarpUtils.getWarp(player, newName)) != null;
        
        PlayerData dat = ((PlayerData) player);
        
        if (!nameIsTaken) {
            dat.delWarp(oldWarp);
        } else {
            TeleportsCommand.renameNotify(player, newName, oldName);
            dat.setWarp(newWarp.copy(oldName));
        }
        
        TeleportsCommand.renameNotify(player, oldName, newName);
        dat.setWarp(oldWarp.copy(newName));
        
        return Command.SINGLE_SUCCESS;
    }
    private static void renameNotify(@NotNull ServerPlayerEntity player, @NotNull String oldName, @NotNull String newName) {
        player.sendMessage(new LiteralText("Renamed waystone \"").formatted(Formatting.YELLOW)
            .append(new LiteralText(oldName).formatted(Formatting.AQUA))
            .append("\" to \"")
            .append(new LiteralText(newName).formatted(Formatting.AQUA))
            .append("\""), false);
    }
    
    private static int setHomePrimaryCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        
        final String favorite = StringArgumentType.getString(context, "location");
        
        // Get the warp to set as the new favorite
        final Warp newFavorite = WarpUtils.getWarp(player, favorite);
        if (newFavorite == null)
            throw TARGET_NO_WARP.create(player);
        if (newFavorite.favorite) {
            source.sendFeedback(new LiteralText("That waystone is already your primary.").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Set the current favorite to not the favorite any more
        final Warp oldFavorite = WarpUtils.getWarp(player, null);
        if (oldFavorite != null)
            oldFavorite.favorite = false;
        
        // Set the new warp to the favorite
        newFavorite.favorite = true;
        
        player.sendMessage(new LiteralText("Set waystone \"").formatted(Formatting.YELLOW)
            .append(new LiteralText(favorite).formatted(Formatting.AQUA))
            .append("\"")
            .append(" to primary."), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static @NotNull CompletableFuture<Suggestions> playerHomeNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return WarpUtils.buildSuggestions(source.getPlayer(), builder);
    }
    private static @NotNull CompletableFuture<Suggestions> playerHomeNamesOfPlayer(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        return WarpUtils.buildSuggestions(target.getId(), builder);
    }
    
    private static int tpaCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return TeleportsCommand.tpaCommand(context, null);
    }
    private static int tpaCommand(@NotNull CommandContext<ServerCommandSource> context, @Nullable String location) throws CommandSyntaxException {
        // Get players info
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity porter = source.getPlayer();
        
        // Get the reference of the player to request a teleport to
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        return TeleportsCommand.tpaToPlayer(
            source.getMinecraftServer(),
            porter,
            target,
            location
        );
    }
    private static int tpaCommandNamed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String location = StringArgumentType.getString(context, "location");
        return TeleportsCommand.tpaCommand(context, location);
    }
    private static int tpaToPlayer(@NotNull MinecraftServer server, @NotNull ServerPlayerEntity porter, @NotNull GameProfile target, @Nullable String location) throws CommandSyntaxException {
        final PlayerManager manager = server.getPlayerManager();
        
        // Check if player is within spawn
        if (!ChunkUtils.isPlayerWithinSpawn(porter))
            throw PLAYER_NOT_IN_SPAWN.create(porter);
        
        Warp warp;
        // If the player to teleport to does not have a warp
        if ((warp = WarpUtils.getWarp(target.getId(), location)) == null)
            throw TARGET_NO_WARP.create(porter);
        
        ServerPlayerEntity targetPlayer = manager.getPlayer(target.getId());
        
        // Accept the teleport automatically
        if ( ChunkUtils.canPlayerWarpTo(porter, target.getId()) ) {
            WarpUtils.teleportPlayer(warp, porter);
            
            TeleportsCommand.feedback(porter, target, warp.name);
            
            // Notify the player
            if (!porter.isSpectator()) {
                if ((targetPlayer != null) && (!target.getId().equals(porter.getUuid()))) {
                    TitleUtils.showPlayerAlert(
                        targetPlayer,
                        Formatting.YELLOW,
                        TranslatableServerSide.text(targetPlayer, "warp.notice.player", porter.getDisplayName())
                    );
                    targetPlayer.playSound(SoundEvents.UI_TOAST_IN, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }
        } else {
            // If player not online
            if (targetPlayer == null)
                throw TeleportsCommand.TARGET_NOT_ONLINE.create( porter );
            
            // Add the player to the list of invitations
            CoreMod.PLAYER_WARP_INVITES.put(porter, new Pair<>(target.getId(), warp.name));
            
            porter.sendSystemMessage(new LiteralText("Waiting for ")
                .formatted(Formatting.YELLOW)
                .append(targetPlayer.getDisplayName())
                .append(" to accept your teleport."), Util.NIL_UUID);
            CoreMod.logInfo(porter.getName().asString() + " has requested to teleport to " + (porter.getUuid().equals(target.getId()) ? "their" : target.getName() + "'s") + " warp");
            
            // Notify the target
            targetPlayer.sendMessage(ColorUtils.format(porter.getName(), Formatting.AQUA)
                .append(new LiteralText(" sent you a teleport request, Click ").formatted(Formatting.YELLOW)
                    .append(new LiteralText("here to accept it").formatted(Formatting.GREEN).styled(
                        (consumer) -> consumer.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + porter.getName().asString()))))
                    .append(", or ")
                    .append(new LiteralText("here to deny it").formatted(Formatting.RED).styled(
                        (consumer) -> consumer.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + porter.getName().asString()))))
                    .append(".")
                ), MessageType.CHAT, porter.getUuid());
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private static int tpAcceptCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer(context, "player");
        
        Pair<UUID, String> warpTo;
        if ((( warpTo = CoreMod.PLAYER_WARP_INVITES.get(porter) ) == null) || (!target.getUuid().equals(warpTo.getLeft())) )
            throw TARGET_NOT_REQUESTING.create(target);
        
        if (!ChunkUtils.isPlayerWithinSpawn(porter)) {
            porter.sendSystemMessage(new LiteralText("Your warp could not be completed, you must be within spawn to warp.").formatted(Formatting.RED), Util.NIL_UUID);
            throw TARGET_NOT_IN_SPAWN.create(target);
        }
        
        Warp warp = WarpUtils.getWarp(target.getUuid(), warpTo.getRight());
        WarpUtils.teleportPlayer(warp, porter);
        
        source.sendFeedback(new LiteralText("Teleport request accepted").formatted(Formatting.GREEN), false);
        
        TeleportsCommand.feedback(porter, target, warp.name);
        
        CoreMod.PLAYER_WARP_INVITES.remove(porter);
        return Command.SINGLE_SUCCESS;
    }
    private static int tpDenyCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        Pair<UUID, String> warpTo;
        if ((( warpTo = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!target.getUuid().equals(warpTo.getLeft())) )
            throw TARGET_NOT_REQUESTING.create(target);
        
        source.sendFeedback(new LiteralText("Teleport request rejected").formatted(Formatting.RED), false);
        CoreMod.logInfo(porter.getName().asString() + "'s teleport was rejected by " + target.getName().asString());
        
        CoreMod.PLAYER_WARP_INVITES.remove(porter);
        return Command.SINGLE_SUCCESS;
    }
    
    private static int sendEntitiesToEnd(@NotNull Collection<? extends Entity> entities) throws CommandSyntaxException {
        // Move the player to the end
        for (Entity entity : entities)
            WarpUtils.teleportEntity(World.END, entity);
        return Command.SINGLE_SUCCESS;
    }
    
    public static void feedback(@NotNull PlayerEntity porter, @NotNull PlayerEntity target, @Nullable String location) {
        TeleportsCommand.feedback(porter, target.getGameProfile(), location);
    }
    public static void feedback(@NotNull PlayerEntity porter, @NotNull GameProfile target, @Nullable String location) {
        MutableText feedback = new LiteralText("")
            .append(porter.getDisplayName().shallowCopy())
            .append(" was teleported to ");
        if (porter.getUuid().equals(target.getId())) feedback.append("their");
        else feedback.append(PlayerNameUtils.fetchPlayerNick(target.getId())).append("'s");
        feedback.append(" '")
            .append(location == null ? WarpUtils.PRIMARY_DEFAULT_HOME : location)
            .append("' warp");
        
        MessageUtils.consoleToOps(feedback);
    }
}
