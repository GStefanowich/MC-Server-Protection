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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.ColorUtils;
import net.TheElm.project.utilities.CommandUtilities;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.WarpUtils;
import net.TheElm.project.utilities.WarpUtils.Warp;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.GameProfileArgumentType;
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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.UUID;

public final class TeleportsCommand {
    public static final ExceptionTranslatableServerSide PLAYER_NOT_IN_SPAWN = TranslatableServerSide.exception("warp.notice.player.outside_spawn");
    public static final ExceptionTranslatableServerSide TARGET_NOT_IN_SPAWN = TranslatableServerSide.exception("warp.notice.target.outside_spawn");
    public static final ExceptionTranslatableServerSide TARGET_NOT_REQUESTING = TranslatableServerSide.exception("warp.notice.no_request");
    public static final ExceptionTranslatableServerSide TARGET_NO_WARP = TranslatableServerSide.exception("warp.notice.no_warp");
    public static final ExceptionTranslatableServerSide TARGET_NOT_ONLINE = TranslatableServerSide.exception("warp.notice.offline");
    
    private TeleportsCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawn")
            .requires((source) -> source.hasPermissionLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .executes((context) -> {
                    // Get location information
                    ServerCommandSource source = context.getSource();
                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "player");
                    ServerWorld world = source.getMinecraftServer().getWorld(World.OVERWORLD);
                    
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
                ServerWorld world = source.getMinecraftServer().getWorld(World.OVERWORLD);
                
                WarpUtils.teleportPlayer(world, player, WarpUtils.getWorldSpawn(world));
                
                source.sendFeedback(new TranslatableText("commands.teleport.success.entity.single", player.getDisplayName(), new LiteralText("Spawn").formatted(Formatting.GOLD)), true);
                
                return Command.SINGLE_SUCCESS;
            })
        );
        CoreMod.logDebug("- Registered Spawn command");
        
        dispatcher.register(CommandManager.literal("theend")
            .requires((source -> source.hasPermissionLevel(OpLevels.CHEATING)))
            .executes((context) -> {
                // Get location information
                ServerCommandSource source = context.getSource();
                ServerWorld world = source.getWorld();
                ServerPlayerEntity player = source.getPlayer();
                
                // Move the player to the end
                if (World.END.equals(world.getRegistryKey()))
                    player.changeDimension(world);
                else WarpUtils.teleportEntity(World.END, player);
                
                return Command.SINGLE_SUCCESS;
            })
        );
        
        dispatcher.register(CommandManager.literal("tpa")
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .suggests(CommandUtilities::getAllPlayerNames)
                .executes(TeleportsCommand::tpaCommand)
            )
            .executes(TeleportsCommand::homeCommand)
        );
        CoreMod.logDebug("- Registered TPA command");
        
        dispatcher.register(CommandManager.literal("tpaccept")
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(WarpUtils::hasWarp)
                .executes(TeleportsCommand::tpAcceptCommand)
            )
        );
        CoreMod.logDebug("- Registered TPAccept command");
        
        dispatcher.register(CommandManager.literal("tpdeny")
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(WarpUtils::hasWarp)
                .executes(TeleportsCommand::tpDenyCommand)
            )
        );
        CoreMod.logDebug("- Registered TPDeny command");
        
        dispatcher.register(CommandManager.literal("home")
            .requires((source) -> SewConfig.get(SewConfig.COMMAND_WARP_TPA))
            .executes(TeleportsCommand::homeCommand)
        );
        CoreMod.logDebug("- Registered Home command");
    }
    
    private static int homeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity porter = source.getPlayer();
        
        return TeleportsCommand.tpaToPlayer(
            source.getMinecraftServer(),
            porter,
            porter.getGameProfile()
        );
    }
    private static int tpaCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity porter = source.getPlayer();
        
        // Get the reference of the player to request a teleport to
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = profiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        return TeleportsCommand.tpaToPlayer(
            source.getMinecraftServer(),
            porter,
            target
        );
    }
    private static int tpaToPlayer(MinecraftServer server, ServerPlayerEntity porter, GameProfile target) throws CommandSyntaxException {
        final PlayerManager manager = server.getPlayerManager();
        
        // Check if player is within spawn
        if (!ChunkUtils.isPlayerWithinSpawn( porter ))
            throw PLAYER_NOT_IN_SPAWN.create( porter );
        
        Warp warp;
        // If the player to teleport to does not have a warp
        if ((warp = WarpUtils.getWarp(target.getId())) == null)
            throw TARGET_NO_WARP.create( porter );
        
        ServerPlayerEntity targetPlayer = manager.getPlayer(target.getId());
        
        // Accept the teleport automatically
        if ( ChunkUtils.canPlayerWarpTo(porter, target.getId()) ) {
            WarpUtils.teleportPlayer(warp, porter);
            
            TeleportsCommand.feedback(porter, target);
            
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
            CoreMod.PLAYER_WARP_INVITES.put(porter, target.getId());
            
            porter.sendSystemMessage(new LiteralText("Waiting for ")
                .formatted(Formatting.YELLOW)
                .append(targetPlayer.getDisplayName())
                .append(" to accept your teleport."), Util.NIL_UUID);
            CoreMod.logInfo(porter.getName().asString() + " has requested to teleport to " + (porter.getUuid().equals(target.getId()) ? "their" : target.getName() + "'s") + " warp");
            
            // Notify the target
            targetPlayer.sendMessage(ColorUtils.format(porter.getName(), Formatting.AQUA)
                .append(new LiteralText(" sent you a TP request, Click ").formatted(Formatting.YELLOW)
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
    private static int tpAcceptCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        UUID warpToUUID;
        if ((( warpToUUID = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!target.getUuid().equals( warpToUUID )) )
            throw TARGET_NOT_REQUESTING.create( target );
        
        if (!ChunkUtils.isPlayerWithinSpawn( porter )) {
            porter.sendSystemMessage(new LiteralText( "Your warp could not be completed, you must be within spawn to warp." ).formatted(Formatting.RED), Util.NIL_UUID);
            throw TARGET_NOT_IN_SPAWN.create( target );
        }
        
        Warp warp = WarpUtils.getWarp( target.getUuid() );
        WarpUtils.teleportPlayer( warp, porter );
        
        source.sendFeedback(new LiteralText("Teleport request accepted").formatted(Formatting.GREEN), false);
        
        TeleportsCommand.feedback(porter, target);
        
        CoreMod.PLAYER_WARP_INVITES.remove( porter );
        return Command.SINGLE_SUCCESS;
    }
    private static int tpDenyCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get players info
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity target = source.getPlayer();
        
        // Get targets info
        ServerPlayerEntity porter = EntityArgumentType.getPlayer( context, "player" );
        
        UUID warpToUUID;
        if ((( warpToUUID = CoreMod.PLAYER_WARP_INVITES.get( porter ) ) == null) || (!target.getUuid().equals( warpToUUID )) )
            throw TARGET_NOT_REQUESTING.create(target);
        
        source.sendFeedback(new LiteralText("Teleport request rejected").formatted(Formatting.RED), false);
        CoreMod.logInfo( porter.getName().asString() + "'s teleport was rejected by " + target.getName().asString() );
        
        CoreMod.PLAYER_WARP_INVITES.remove(porter);
        return Command.SINGLE_SUCCESS;
    }
    
    private static void feedback(PlayerEntity porter, PlayerEntity target) {
        TeleportsCommand.feedback(porter, target.getGameProfile());
    }
    public static void feedback(PlayerEntity porter, GameProfile target) {
        MutableText feedback = new LiteralText("")
            .append(porter.getDisplayName())
            .append(" was teleported to ");
        if (porter.getUuid().equals(target.getId())) feedback.append("their");
        else feedback.append(PlayerNameUtils.fetchPlayerNick(target.getId())).append("'s");
        feedback.append(" warp.");
        
        MessageUtils.consoleToOps(feedback);
    }
    
}
