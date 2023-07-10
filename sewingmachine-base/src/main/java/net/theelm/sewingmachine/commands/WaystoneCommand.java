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

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.blocks.entities.LecternWarpsBlockEntity;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import net.theelm.sewingmachine.utilities.WarpUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WaystoneCommand extends SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "waystones", builder -> builder
            .then(CommandManager.literal("set")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("location", StringArgumentType.string())
                        .suggests(this::getPlayerEntityLocations)
                        .executes(this::updatePlayerWaystone)
                    )
                )
            )
            .then(CommandManager.literal("remove")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("location", StringArgumentType.string())
                        .suggests(this::getPlayerEntityLocations)
                        .executes(this::deletePlayerWaystone)
                    )
                )
            )
            .then(CommandManager.literal("send")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                    .then(CommandManager.argument("to", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .then(CommandManager.argument("location", StringArgumentType.string())
                            .suggests(this::getPlayerSendableLocations)
                            .executes(this::sendEntitiesToLocation)
                        )
                        .executes(this::sendEntitiesTo)
                    )
                    .executes(this::sendHome)
                )
            )
            .then(CommandManager.literal("lectern")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.literal("set")
                    .executes(this::updateLecternToWarpBook)
                )
            )
            /*.then(CommandManager.literal("generation")
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.literal("cancel")
                    .executes(WaystoneCommand::cancelGenerators)
                )
                .executes(WaystoneCommand::listGenerators)
            )*/
        );
    }
    
    private int updatePlayerWaystone(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntity();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        String name = StringArgumentType.getString(context, "location");
        
        // Get the position to set the waystone to
        BlockPos blockPos = (entity instanceof PlayerEntity ? entity.getBlockPos() : BlockPos.ofFloored(source.getPosition()).up());
        
        // Update the positioning
        ((PlayerData) target).setWarp(new WarpUtils.Warp(
            name,
            source.getWorld(),
            blockPos,
            false
        ));
        
        source.sendFeedback(
            () -> Text.literal("Set ")
                .append(TextUtils.literal(target.getDisplayName()))
                .append("'s waystone ")
                .append(Text.literal(name).formatted(Formatting.AQUA))
                .append(" to ")
                .append(MessageUtils.xyzToText(blockPos))
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int deletePlayerWaystone(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get source of the command
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        String name = StringArgumentType.getString(context, "location");
        
        if (!WarpUtils.hasWarp(target, name))
            throw TeleportsCommand.TARGET_NO_WARP.create(source);
        
        // Send feedback to the source
        ((PlayerData) target).delWarp(name);
        source.sendFeedback(
            () -> Text.literal("Deleted the waystone ")
                .append(Text.literal(name).formatted(Formatting.AQUA))
                .append(" of ")
                .append(TextUtils.literal(target.getDisplayName()))
                .append("."),
            true
        );
        
        // Reset the waystone
        return Command.SINGLE_SUCCESS;
    }
    private int sendHome(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Set<ServerPlayerEntity> players = EntityArgumentType.getEntities(context, "targets")
            .stream()
            .map(entity -> {
                if (entity instanceof ServerPlayerEntity player1)
                    return player1;
                else if (entity.getControllingPassenger() instanceof ServerPlayerEntity player2)
                    return player2;
                return null;
            })
            .collect(Collectors.toSet());
        
        int sent = 0;
        
        // Iterate players
        for (ServerPlayerEntity player : players) {
            if (!WarpUtils.hasWarp(player)) {
                if (source.getEntity() == null)
                    player.sendMessage(Text.literal("You do not have a waystone.").formatted(Formatting.RED));
                else if (players.size() == 1)
                    source.sendError(Text.literal("That player does not have a waystone.").formatted(Formatting.RED));
                continue;
            }
            
            // Teleport the player to their warp
            WarpUtils.Warp warp = WarpUtils.teleportEntityAndAttached(player, (String) null);
            
            sent++;
            
            // Provide feedback about the teleport
            TeleportsCommand.feedback(player, warp);
        }
        
        return sent;
    }
    private int sendEntitiesTo(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.sendEntitiesToLocation(context, null);
    }
    private int sendEntitiesToLocation(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.sendEntitiesToLocation(context, StringArgumentType.getString(context, "location"));
    }
    private int sendEntitiesToLocation(@NotNull CommandContext<ServerCommandSource> context, @Nullable String location) throws CommandSyntaxException {
        // Get information about the request
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        PlayerManager manager = server.getPlayerManager();
        
        // Get information about the teleporting players
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "targets");
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "to");
        
        // Get information about the target
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        ServerPlayerEntity targetPlayer = manager.getPlayer(target.getId());
        
        WarpUtils.Warp warp;
        // If the player to teleport to does not have a warp
        if ((warp = WarpUtils.getWarp(target.getId(), location)) == null)
            throw TeleportsCommand.TARGET_NO_WARP.create(source);
        
        // Teleport all of the players
        for (Entity porter : entities) {
            if (porter instanceof ServerPlayerEntity portingPlayer) {
                WarpUtils.teleportEntityAndAttached(portingPlayer, warp);
                
                TeleportsCommand.feedback(portingPlayer, target, warp);
            } else WarpUtils.teleportEntityAndAttached(porter, warp);
            
            // Notify the player
            if (!porter.isSpectator()) {
                if ((targetPlayer != null) && (!target.getId().equals(porter.getUuid()))) {
                    TitleUtils.showPlayerAlert(
                        targetPlayer,
                        Formatting.YELLOW,
                        ServerText.text(targetPlayer, "warp.notice.player", porter.getDisplayName())
                    );
                    targetPlayer.playSound(SoundEvents.UI_TOAST_IN, SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }
        }
        
        return entities.size();
    }
    
    private int updateLecternToWarpBook(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Either<LecternWarpsBlockEntity, String> either = BlockUtils.getLecternBlockEntity(
            source.getWorld(),
            source.getEntityOrThrow(),
            LecternWarpsBlockEntity.class,
            LecternWarpsBlockEntity::new
        );
        Optional<LecternWarpsBlockEntity> optionalLectern = either.left();
        Optional<String> error = either.right();
        
        if (error.isPresent())
            source.sendError(Text.literal(error.get()));
        else if (optionalLectern.isPresent()) {
            LecternWarpsBlockEntity warps = optionalLectern.get();
            
            // Run the created state
            warps.onCreated();
            
            source.sendFeedback(
                () -> Text.literal("Updated lectern to show player warps.")
                    .formatted(Formatting.YELLOW),
                true
            );
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
    
    private int listGenerators(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        for (ServerWorld world : server.getWorlds()) {
            
        }
        
        return 0;
    }
    private int cancelGenerators(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return 0;
    }
    
    private @NotNull CompletableFuture<Suggestions> getPlayerEntityLocations(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntity();
        UUID untrusted = entity instanceof ServerPlayerEntity ? entity.getUuid() : null;
        
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        return WarpUtils.buildSuggestions(source.getServer(), untrusted, player, builder);
    }
    private @NotNull CompletableFuture<Suggestions> getPlayerSendableLocations(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntity();
        UUID untrusted = entity instanceof ServerPlayerEntity ? entity.getUuid() : null;
        
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "to");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        return WarpUtils.buildSuggestions(source.getServer(), untrusted, target.getId(), builder);
    }
}
