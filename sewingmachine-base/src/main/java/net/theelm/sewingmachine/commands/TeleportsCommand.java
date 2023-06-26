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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.IntUtils;
import net.theelm.sewingmachine.utilities.PlayerNameUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.theelm.sewingmachine.utilities.WarpUtils;
import net.theelm.sewingmachine.utilities.WarpUtils.Warp;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeleportsCommand extends SewCommand {
    public static final ExceptionTranslatableServerSide INVALID_HOME_CHARACTERS = TranslatableServerSide.exception("warp.notice.name.invalid");
    public static final ExceptionTranslatableServerSide INVALID_HOME_LENGTH = TranslatableServerSide.exception("warp.notice.name.too_long");
    public static final ExceptionTranslatableServerSide PLAYER_NOT_IN_SPAWN = TranslatableServerSide.exception("warp.notice.player.outside_spawn");
    public static final ExceptionTranslatableServerSide TARGET_NOT_IN_SPAWN = TranslatableServerSide.exception("warp.notice.target.outside_spawn");
    public static final ExceptionTranslatableServerSide TARGET_NOT_REQUESTING = TranslatableServerSide.exception("warp.notice.no_request");
    public static final ExceptionTranslatableServerSide TARGET_NO_WARP = TranslatableServerSide.exception("warp.notice.no_warp");
    public static final ExceptionTranslatableServerSide TARGET_NOT_ONLINE = TranslatableServerSide.exception("warp.notice.offline");
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        ServerCore.register(dispatcher, "spawn", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("targets", EntityArgumentType.entities())
                .executes((context) -> this.sendEntitiesToServerSpawn(context.getSource(), EntityArgumentType.getEntities(context, "targets")))
            )
            .executes((context) -> {
                // Get location information
                ServerCommandSource source = context.getSource();
                return this.sendEntitiesToServerSpawn(source, Collections.singleton(source.getPlayer()));
            })
        );
        
        ServerCore.register(dispatcher, "tphere", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("target", EntityArgumentType.entities())
                .executes(this::tpHere)
            )
        );
        
        ServerCore.register(dispatcher, "theend", "end teleport", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("targets", EntityArgumentType.entities())
                .executes((context) -> this.sendEntitiesToEnd(context.getSource(), EntityArgumentType.getEntities(context, "targets")))
            )
            .executes((context) -> {
                // Get location information
                ServerCommandSource source = context.getSource();
                return this.sendEntitiesToEnd(source, Collections.singleton(source.getPlayer()));
            })
        );
        
        ServerCore.register(dispatcher, "tpa", builder -> builder
            .requires(CommandPredicate.isEnabled(SewCoreConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .suggests(CommandUtils::getAllPlayerNames)
                .then(CommandManager.argument("location", StringArgumentType.string())
                    .suggests(this::playerHomeNamesOfPlayer)
                    .executes(this::tpaCommandNamed)
                )
                .executes(this::tpaCommand)
            )
            .executes(this::toPrimaryHomeCommand)
        );
        
        ServerCore.register(dispatcher, "tpaccept", builder -> builder
            .requires(CommandPredicate.isEnabled(SewCoreConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(WarpUtils::hasWarp)
                .executes(this::tpAcceptCommand)
            )
        );
        
        ServerCore.register(dispatcher, "tpdeny", builder -> builder
            .requires(CommandPredicate.isEnabled(SewCoreConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(WarpUtils::hasWarp)
                .executes(this::tpDenyCommand)
            )
        );
        
        ServerCore.register(dispatcher, "home", builder -> builder
            .requires(CommandPredicate.isEnabled(SewCoreConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("location", StringArgumentType.string())
                .suggests(this::playerHomeNames)
                .then(CommandManager.literal("rename")
                    .then(CommandManager.argument("new", StringArgumentType.string())
                        .executes(this::renameHomeCommand)
                    )
                )
                .then(CommandManager.literal("set")
                    .then(CommandManager.literal("primary")
                        .executes(this::setHomePrimaryCommand)
                    )
                )
                .executes(this::toSpecificHomeCommand)
            )
            .executes(this::toPrimaryHomeCommand)
        );
    }
    
    private int toPrimaryHomeCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.toSpecificHomeCommand(context, null);
    }
    private int toSpecificHomeCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.toSpecificHomeCommand(context, StringArgumentType.getString(context, "location"));
    }
    private int toSpecificHomeCommand(@NotNull CommandContext<ServerCommandSource> context, @Nullable String location) throws CommandSyntaxException {
        // Get players info
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity porter = source.getPlayer();
        
        return this.tpaToPlayer(
            source.getServer(),
            porter,
            porter.getGameProfile(),
            location
        );
    }
    
    private int renameHomeCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
            this.renameNotify(player, newName, oldName);
            dat.setWarp(newWarp.copy(oldName));
        }
        
        this.renameNotify(player, oldName, newName);
        dat.setWarp(oldWarp.copy(newName));
        
        return Command.SINGLE_SUCCESS;
    }
    private void renameNotify(@NotNull ServerPlayerEntity player, @NotNull String oldName, @NotNull String newName) {
        player.sendMessage(Text.literal("Renamed waystone \"").formatted(Formatting.YELLOW)
            .append(Text.literal(oldName).formatted(Formatting.AQUA))
            .append("\" to \"")
            .append(Text.literal(newName).formatted(Formatting.AQUA))
            .append("\""), false);
    }
    
    private int setHomePrimaryCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        
        final String favorite = StringArgumentType.getString(context, "location");
        
        // Get the warp to set as the new favorite
        final Warp newFavorite = WarpUtils.getWarp(player, favorite);
        if (newFavorite == null)
            throw TARGET_NO_WARP.create(player);
        if (newFavorite.favorite) {
            source.sendFeedback(() -> Text.literal("That waystone is already your primary.").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Set the current favorite to not the favorite any more
        final Warp oldFavorite = WarpUtils.getWarp(player, null);
        if (oldFavorite != null)
            oldFavorite.favorite = false;
        
        // Set the new warp to the favorite
        newFavorite.favorite = true;
        
        player.sendMessage(Text.literal("Set waystone \"").formatted(Formatting.YELLOW)
            .append(Text.literal(favorite).formatted(Formatting.AQUA))
            .append("\"")
            .append(" to primary."), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private @NotNull CompletableFuture<Suggestions> playerHomeNames(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return WarpUtils.buildSuggestions(source.getServer(), source.getPlayer().getUuid(), source.getPlayer(), builder);
    }
    private @NotNull CompletableFuture<Suggestions> playerHomeNamesOfPlayer(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the uuid of the executor
        Entity entity = source.getEntity();
        UUID untrusted = entity instanceof ServerPlayerEntity ? entity.getUuid() : null;
        
        // Get the matching player being looked up
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Build the suggestions
        return WarpUtils.buildSuggestions(source.getServer(), untrusted, target.getId(), builder);
    }
    
    private int tpaCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.tpaCommand(context, null);
    }
    private int tpaCommand(@NotNull CommandContext<ServerCommandSource> context, @Nullable String location) throws CommandSyntaxException {
        // Get players info
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity porter = source.getPlayer();
        
        // Get the reference of the player to request a teleport to
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        return this.tpaToPlayer(
            source.getServer(),
            porter,
            target,
            location
        );
    }
    private int tpaCommandNamed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String location = StringArgumentType.getString(context, "location");
        return this.tpaCommand(context, location);
    }
    private int tpaToPlayer(@NotNull MinecraftServer server, @NotNull ServerPlayerEntity porter, @NotNull GameProfile target, @Nullable String location) throws CommandSyntaxException {
        final PlayerManager manager = server.getPlayerManager();
        
        // Check if player is within spawn
        if (!ChunkUtils.isPlayerWithinSpawn(porter))
            throw TeleportsCommand.PLAYER_NOT_IN_SPAWN.create(porter);
        
        // If the player to teleport to does not have a warp
        Warp warp = WarpUtils.getWarp(target.getId(), location);
        if (warp == null)
            throw TeleportsCommand.TARGET_NO_WARP.create(porter);
        
        ServerPlayerEntity targetPlayer = manager.getPlayer(target.getId());
        
        // Accept the teleport automatically
        if ( ChunkUtils.canPlayerWarpTo(server, porter, target.getId()) ) {
            WarpUtils.teleportEntityAndAttached(porter, warp);
            
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
        } else {
            // If player not online
            if (targetPlayer == null)
                throw TeleportsCommand.TARGET_NOT_ONLINE.create( porter );
            
            // Add the player to the list of invitations
            CoreMod.PLAYER_WARP_INVITES.put(porter, new Pair<>(target.getId(), warp.name));
            
            porter.sendMessage(Text.literal("Waiting for ")
                .formatted(Formatting.YELLOW)
                .append(targetPlayer.getDisplayName())
                .append(" to accept your teleport."));
            CoreMod.logInfo(porter.getName().getString() + " has requested to teleport to " + (porter.getUuid().equals(target.getId()) ? "their" : target.getName() + "'s") + " warp");
            
            // Notify the target
            targetPlayer.sendMessage(ColorUtils.format(porter.getName(), Formatting.AQUA)
                .append(Text.literal(" sent you a teleport request, Click ").formatted(Formatting.YELLOW)
                    .append(Text.literal("here to accept it").formatted(Formatting.GREEN).styled(
                        (consumer) -> consumer.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + porter.getEntityName()))))
                    .append(", or ")
                    .append(Text.literal("here to deny it").formatted(Formatting.RED).styled(
                        (consumer) -> consumer.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + porter.getEntityName()))))
                    .append(".")
                ));
        }
        return Command.SINGLE_SUCCESS;
    }
    private int tpHere(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get location information
        ServerCommandSource source = context.getSource();
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(context, "target");
        
        for (Entity entity : targets)
            if (entity != source.getEntity())
                WarpUtils.teleportEntity(source.getWorld(), entity, BlockPos.ofFloored(source.getPosition()));
        
        Text self = source.getDisplayName();
        if (targets.size() == 1) {
            source.sendFeedback(
                () -> Text.translatable("commands.teleport.success.entity.single", targets.iterator().next().getDisplayName(), self),
                true
            );
        } else {
            source.sendFeedback(
                () -> Text.translatable("commands.teleport.success.entity.multiple", targets.size(), self),
                true
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int tpAcceptCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer(context, "player");
        
        Pair<UUID, String> warpTo;
        if ((( warpTo = CoreMod.PLAYER_WARP_INVITES.get(porter) ) == null) || (!target.getUuid().equals(warpTo.getLeft())) )
            throw TARGET_NOT_REQUESTING.create(target);
        
        if (!ChunkUtils.isPlayerWithinSpawn(porter)) {
            porter.sendMessage(Text.literal("Your warp could not be completed, you must be within spawn to warp.").formatted(Formatting.RED));
            throw TARGET_NOT_IN_SPAWN.create(target);
        }
        
        Warp warp = WarpUtils.getWarp(target.getUuid(), warpTo.getRight());
        WarpUtils.teleportEntityAndAttached(porter, warp);
        
        source.sendFeedback(() -> Text.literal("Teleport request accepted").formatted(Formatting.GREEN), false);
        
        TeleportsCommand.feedback(porter, target, warp);
        
        CoreMod.PLAYER_WARP_INVITES.remove(porter);
        return Command.SINGLE_SUCCESS;
    }
    private int tpDenyCommand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        Pair<UUID, String> warpTo;
        if ((( warpTo = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!target.getUuid().equals(warpTo.getLeft())) )
            throw TARGET_NOT_REQUESTING.create(target);
        
        source.sendFeedback(() -> Text.literal("Teleport request rejected").formatted(Formatting.RED), false);
        CoreMod.logInfo(porter.getName().getString() + "'s teleport was rejected by " + target.getName().getString());
        
        CoreMod.PLAYER_WARP_INVITES.remove(porter);
        return Command.SINGLE_SUCCESS;
    }
    
    private int sendEntitiesToServerSpawn(@NotNull ServerCommandSource source, @NotNull Collection<? extends Entity> players) {
        RegistryKey<World> worldDimensionKey = ServerCore.defaultWorldKey();
        
        // Teleport players to location
        for (Entity entity : players)
            WarpUtils.teleportEntityAndAttached(worldDimensionKey, entity);
        
        Text spawnText = Text.literal("Spawn").formatted(Formatting.GOLD);
        if (players.size() == 1)
            source.sendFeedback(() -> Text.translatable("commands.teleport.success.entity.single", players.iterator().next().getDisplayName(), spawnText), true);
        else
            source.sendFeedback(() -> Text.translatable("commands.teleport.success.entity.multiple", players.size(), spawnText), true);
        
        return players.size();
    }
    private int sendEntitiesToEnd(@NotNull ServerCommandSource source, @NotNull Collection<? extends Entity> entities) {
        // Move the player to the end
        for (Entity entity : entities)
            WarpUtils.teleportEntityAndAttached(World.END, entity);
        
        Text endText = Text.literal("The End").formatted(Formatting.GOLD);
        if (entities.size() == 1)
            source.sendFeedback(
                () -> Text.translatable("commands.teleport.success.entity.single", entities.iterator().next().getDisplayName(), endText),
                true
            );
        else
            source.sendFeedback(
                () -> Text.translatable("commands.teleport.success.entity.multiple", entities.size(), endText),
                true
            );
            
        return entities.size();
    }
    
    public static void feedback(@NotNull PlayerEntity porter, @Nullable Warp location) {
        TeleportsCommand.feedback(porter, porter, location);
    }
    public static void feedback(@NotNull PlayerEntity porter, @NotNull PlayerEntity target, @Nullable Warp location) {
        TeleportsCommand.feedback(porter, target.getGameProfile(), location);
    }
    public static void feedback(@NotNull PlayerEntity porter, @NotNull GameProfile target, @Nullable Warp location) {
        MutableText feedback = Text.literal("")
            .append(porter.getDisplayName().copyContentOnly())
            .append(" was teleported to ");
        
        if (porter.getUuid().equals(target.getId())) feedback.append("their");
        else feedback.append(PlayerNameUtils.fetchPlayerNick(porter.getServer(), target.getId())).append("'s");
        
        feedback.append(" '")
            .append(location == null ? WarpUtils.PRIMARY_DEFAULT_HOME : location.name)
            .append("' warp");
        
        MessageUtils.consoleToOps(feedback);
    }
}
