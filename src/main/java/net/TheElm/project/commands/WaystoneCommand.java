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
import com.mojang.datafixers.util.Either;
import net.TheElm.project.ServerCore;
import net.TheElm.project.blocks.entities.LecternWarpsBlockEntity;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.BlockUtils;
import net.TheElm.project.utilities.CommandUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.WarpUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.TheElm.project.commands.TeleportsCommand.TARGET_NO_WARP;

public class WaystoneCommand {
    
    private WaystoneCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "waystones", builder -> builder
            .then(CommandManager.literal("set")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("location", StringArgumentType.string())
                        .suggests(WaystoneCommand::getPlayerEntityLocations)
                        .executes(WaystoneCommand::updatePlayerWaystone)
                    )
                )
            )
            .then(CommandManager.literal("remove")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("location", StringArgumentType.string())
                        .suggests(WaystoneCommand::getPlayerEntityLocations)
                        .executes(WaystoneCommand::deletePlayerWaystone)
                    )
                )
            )
            .then(CommandManager.literal("send")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("players", EntityArgumentType.players())
                    .then(CommandManager.argument("to", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .then(CommandManager.argument("location", StringArgumentType.string())
                            .suggests(WaystoneCommand::getPlayersToLocations)
                            .executes(WaystoneCommand::sendPlayersToLocation)
                        )
                        .executes(WaystoneCommand::sendPlayersTo)
                    )
                    .executes(WaystoneCommand::sendHome)
                )
            )
            .then(CommandManager.literal("lectern")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.literal("set")
                    .executes(WaystoneCommand::updateLecternToWarpBook)
                )
            )
        );
    }
    
    private static int updatePlayerWaystone(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntity();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        String name = StringArgumentType.getString(context, "location");
        
        // Get the position to set the waystone to
        BlockPos blockPos = (entity instanceof PlayerEntity ? entity.getBlockPos() : new BlockPos(source.getPosition()).up());
        
        // Update the positioning
        ((PlayerData) target).setWarp(new WarpUtils.Warp(
            name,
            source.getWorld(),
            blockPos,
            false
        ));
        
        source.sendFeedback(new LiteralText("Set ")
            .append(target.getDisplayName().shallowCopy())
            .append("'s waystone ")
            .append(new LiteralText(name).formatted(Formatting.AQUA))
            .append(" to ")
            .append(MessageUtils.xyzToText(blockPos))
            .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int deletePlayerWaystone(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get source of the command
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        String name = StringArgumentType.getString(context, "location");
        
        if (!WarpUtils.hasWarp(target, name))
            throw TeleportsCommand.TARGET_NO_WARP.create(source);
        
        // Send feedback to the source
        ((PlayerData) target).delWarp(name);
        source.sendFeedback(new LiteralText("Deleted the waystone ")
            .append(new LiteralText(name).formatted(Formatting.AQUA))
            .append(" of ")
            .append(target.getDisplayName().shallowCopy())
            .append("."), true);
        
        // Reset the waystone
        return Command.SINGLE_SUCCESS;
    }
    private static int sendHome(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
        
        // Iterate players
        for (ServerPlayerEntity player : players) {
            if (!WarpUtils.hasWarp(player)) {
                if (source.getEntity() == null)
                    player.sendSystemMessage(new LiteralText("You do not have a waystone.").formatted(Formatting.RED), Util.NIL_UUID);
                else if (players.size() == 1)
                    source.sendError(new LiteralText("That player does not have a waystone.").formatted(Formatting.RED));
                continue;
            }
            
            // Teleport the player to their warp
            WarpUtils.Warp warp = WarpUtils.teleportPlayerAndAttached(player, null);
            
            // Provide feedback about the teleport
            TeleportsCommand.feedback(player, warp);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    private static int sendPlayersTo(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return WaystoneCommand.sendPlayersToLocation(context, null);
    }
    private static int sendPlayersToLocation(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return WaystoneCommand.sendPlayersToLocation(context, StringArgumentType.getString(context, "location"));
    }
    private static int sendPlayersToLocation(@NotNull CommandContext<ServerCommandSource> context, @Nullable String location) throws CommandSyntaxException {
        // Get information about the request
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getMinecraftServer();
        PlayerManager manager = server.getPlayerManager();
        
        // Get information about the teleporting players
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "to");
        
        // Get information about the target
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        ServerPlayerEntity targetPlayer = manager.getPlayer(target.getId());
        
        WarpUtils.Warp warp;
        // If the player to teleport to does not have a warp
        if ((warp = WarpUtils.getWarp(target.getId(), location)) == null)
            throw TARGET_NO_WARP.create(source);
        
        // Teleport all of the players
        for (ServerPlayerEntity porter : players) {
            WarpUtils.teleportPlayerAndAttached(warp, porter);
            
            TeleportsCommand.feedback(porter, target, warp);
            
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
        }
        
        return players.size();
    }
    
    private static int updateLecternToWarpBook(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Either<LecternWarpsBlockEntity, String> either = BlockUtils.getLecternBlockEntity(
            source.getWorld(),
            source.getEntityOrThrow(),
            LecternWarpsBlockEntity.class, LecternWarpsBlockEntity::new
        );
        Optional<LecternWarpsBlockEntity> optionalLectern = either.left();
        Optional<String> error = either.right();
        
        if (error.isPresent())
            source.sendError(new LiteralText(error.get()));
        else if (optionalLectern.isPresent()) {
            LecternWarpsBlockEntity warps = optionalLectern.get();
            
            // Run the created state
            warps.onCreated();
            
            source.sendFeedback(new LiteralText("Updated lectern to show player warps.")
                .formatted(Formatting.YELLOW), true);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
    
    private static @NotNull CompletableFuture<Suggestions> getPlayerEntityLocations(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        Entity entity = context.getSource().getEntity();
        UUID untrusted = entity instanceof ServerPlayerEntity ? entity.getUuid() : null;
        
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        return WarpUtils.buildSuggestions(untrusted, player, builder);
    }
    private static @NotNull CompletableFuture<Suggestions> getPlayersToLocations(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        Entity entity = context.getSource().getEntity();
        UUID untrusted = entity instanceof ServerPlayerEntity ? entity.getUuid() : null;
        
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "to");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        return WarpUtils.buildSuggestions(untrusted, target.getId(), builder);
    }
}
