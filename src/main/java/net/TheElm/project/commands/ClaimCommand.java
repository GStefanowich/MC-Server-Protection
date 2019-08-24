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
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.TheElm.project.interfaces.PlayerMovement;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.commands.ArgumentTypes.EnumArgumentType;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.CoreMod;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.utilities.CommandUtilities;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;

public final class ClaimCommand {
    
    private static final Dynamic2CommandExceptionType NOT_ENOUGH_MONEY = new Dynamic2CommandExceptionType((player, amount) -> 
        TranslatableServerSide.text( (ServerPlayerEntity) player, "town.found.poor", new LiteralText("$" + NumberFormat.getInstance().format( amount )).formatted(Formatting.AQUA) )
    );
    private static final DynamicCommandExceptionType SELF_RANK_CHANGE = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text( (ServerPlayerEntity) player, "friends.rank.self" )
    );
    private static final DynamicCommandExceptionType CHUNK_NOT_OWNED_BY_PLAYER = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text( (ServerPlayerEntity) player, "claim.chunk.error.not_players" )
    );
    private static final Dynamic2CommandExceptionType CHUNK_RADIUS_OWNED = new Dynamic2CommandExceptionType((player, ownerName) ->
        TranslatableServerSide.text( (ServerPlayerEntity)player, "claim.chunk.error.radius_owned", ownerName )
    );
    private static final SimpleCommandExceptionType WHITELIST_FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.whitelist.add.failed", new Object[0]));
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return;
        
        /*
         * Claim Command
         */
        LiteralCommandNode<ServerCommandSource> claim = dispatcher.register( CommandManager.literal("claim" )
            // Claim a chunk radius
            .then(CommandManager.argument( "radius", IntegerArgumentType.integer( 1, 4 ))
                .executes(ClaimCommand::claimChunkSelfRadius)
            )
            // Claim chunk for another player
            .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                .suggests(CommandUtilities::getAllPlayerNames)
                .requires((source) -> source.hasPermissionLevel( SewingMachineConfig.INSTANCE.CLAIM_OP_LEVEL_OTHER.get() ))
                .executes(ClaimCommand::claimChunkOther)
            )
            // Claim chunk for the spawn
            .then(CommandManager.literal( "spawn" )
                .requires((source) -> source.hasPermissionLevel( SewingMachineConfig.INSTANCE.CLAIM_OP_LEVEL_SPAWN.get() ))
                .executes(ClaimCommand::claimChunkSpawn)
            )
            // Claim chunk for players town
            .then(CommandManager.literal("town" )
                .requires(CommandUtilities::playerIsInTown)
                .executes(ClaimCommand::claimChunkTown)
            )
            // Claim a chunk for yourself
            .executes(ClaimCommand::claimChunkSelf)
        );
        CoreMod.logDebug( "- Registered Claim command" );
        
        /*
         * Unclaim Command
         */
        LiteralCommandNode<ServerCommandSource> unclaim = dispatcher.register( CommandManager.literal("unclaim")
            // Unclaim all chunks
            .then(CommandManager.literal("all")
                .executes(ClaimCommand::unclaimAll)
            )
            // Unclaim current chunk
            .executes(ClaimCommand::unclaimChunk)
        );
        CoreMod.logDebug( "- Registered Unclaim command" );
        
        /*
         * Friends Command
         */
        LiteralCommandNode<ServerCommandSource> friends = dispatcher.register( CommandManager.literal("friends")
            // Whiteliat a friend
            .then( CommandManager.literal( "whitelist" )
                .then( CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .executes(ClaimCommand::inviteFriend)
                )
            )
            // Set a friends rank
            .then( CommandManager.literal("set")
                .then( CommandManager.argument( "rank", StringArgumentType.word())
                    .suggests( EnumArgumentType.create( ClaimRanks.class )::listSuggestions )
                    .then( CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtilities::getAllPlayerNames)
                        .executes( ClaimCommand::addRank )
                    )
                )
            )
            // Remove a friends rank
            .then( CommandManager.literal("remove")
                .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtilities::getAllPlayerNames)
                    .executes( ClaimCommand::remRank )
                )
            )
            .executes( ClaimCommand::listFriends )
        );
        CoreMod.logDebug( "- Registered Friends command" );
        
        /*
         * Register the town command
         */
        LiteralCommandNode<ServerCommandSource> towns = dispatcher.register( CommandManager.literal( "town" )
            .then( CommandManager.literal( "new" )
                .requires(source -> !sourceIsMayor( source ))
                .then( CommandManager.argument( "name", StringArgumentType.greedyString() )
                    .executes(ClaimCommand::townFound)
                )
            )
            .then( CommandManager.literal( "disband" )
                .requires(ClaimCommand::sourceIsMayor)
                .executes(ClaimCommand::townDisband)
            )
            .then( CommandManager.literal( "claim" )
                .requires(ClaimCommand::sourceIsMayor)
                .executes(ClaimCommand::claimChunkTown)
            )
        );
        CoreMod.logMessage( "- Registered Town command" );
        
        /*
         * Register the main command object
         */
        
        // The main command
        LiteralCommandNode<ServerCommandSource> protection = dispatcher.register( CommandManager.literal("protection" )
            .then(CommandManager.literal("claim" )
                .redirect( claim )
            )
            
            // Unclaim a chunk
            .then(CommandManager.literal("unclaim")
                .redirect( unclaim )
            )
            
            // Towns
            .then(CommandManager.literal("town")
                .redirect( towns )
            )
            
            // Update claim permissions
            .then( CommandManager.literal("permissions")
                .then( CommandManager.argument( "permission", StringArgumentType.word())
                    .suggests( EnumArgumentType.create( ClaimPermissions.class )::listSuggestions )
                    .then( CommandManager.argument( "rank", StringArgumentType.word())
                        .suggests( EnumArgumentType.create( ClaimRanks.class )::listSuggestions )
                        .executes( ClaimCommand::updateSetting )
                    )
                )
            )
            
            // Update friends
            .then( CommandManager.literal("friends")
                .redirect( friends )
            )
            .then( CommandManager.literal("settings")
                .then( CommandManager.argument( "setting", StringArgumentType.word())
                    .suggests( EnumArgumentType.create( ClaimSettings.class )::listSuggestions )
                    .then( CommandManager.argument( "bool", BoolArgumentType.bool() )
                        .executes( ClaimCommand::updateBoolean )
                    )
                )
            )
        );
        
        CoreMod.logDebug( "- Registered Protection command" );
    }
    
    /*
     * Claim a chunk
     */
    private static int claimChunkSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player running the command
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Claim the chunk for own player
        return ClaimCommand.claimChunk( player.getUuid(), player );
    }
    private static int claimChunkSelfRadius(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player running the command
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        World world = source.getWorld();
        
        // Get the players positioning
        BlockPos blockPos = player.getBlockPos();
        
        List<WorldChunk> ignoreChunks = new ArrayList<>();
        
        // Check the radius that the player wants to claim
        final int radius = IntegerArgumentType.getInteger( context, "radius" );
        ClaimedChunk[] claimedChunks = ClaimedChunk.getOwnedAround( player.getServerWorld(), player.getBlockPos(), radius );
        for ( ClaimedChunk claimedChunk : claimedChunks ) {
            if (!player.getUuid().equals( claimedChunk.getOwner() ))
                throw CHUNK_RADIUS_OWNED.create( player, claimedChunk.getOwnerName(player.getUuid()));
            ignoreChunks.add(world.getWorldChunk(claimedChunk.getBlockPos()));
        }
        
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        
        List<WorldChunk> chunksToClaim = new ArrayList<>();
        // For the X axis
        for ( int x = chunkX - radius; x <= chunkX + radius; x++ ) {
            // For the Z axis
            for ( int z = chunkZ - radius; z <= chunkZ + radius; z++ ) {
                // Create the chunk position
                WorldChunk chunkPos = world.getWorldChunk(new BlockPos( x << 4, 0, z << 4 ));
                // Add if not already claimed
                if (!ignoreChunks.contains(chunkPos))
                    chunksToClaim.add(chunkPos);
            }
        }
        
        // Claim all chunks
        return ClaimCommand.claimChunkAt( player.getUuid(), player, chunksToClaim.toArray(new WorldChunk[0]));
    }
    private static int claimChunkTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    private static int claimChunkOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player running the command
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "target" );
        GameProfile targetPlayer = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Claim the chunk for other player
        return ClaimCommand.claimChunk( targetPlayer.getId(), player );
    }
    private static int claimChunkSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player running the command
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Claim the chunk for spawn
        return ClaimCommand.claimChunk( CoreMod.spawnID, player );
    }
    private static int claimChunk(final UUID chunkFor, final PlayerEntity claimant) throws CommandSyntaxException {
        // Get run from positioning
        BlockPos blockPos = claimant.getBlockPos();
        return ClaimCommand.claimChunkAt( chunkFor, claimant, claimant.world.getWorldChunk(blockPos) );
    }
    public static int claimChunkAt(final UUID chunkFor, final PlayerEntity claimant, final WorldChunk... worldChunks) {
        new Thread(ClaimCommand.claimChunkThread( chunkFor, claimant, worldChunks)).start();
        return Command.SINGLE_SUCCESS;
    }
    private static Runnable claimChunkThread(final UUID chunkFor, final PlayerEntity claimant, final WorldChunk... worldChunks) {
        return (() -> {
            if (!ClaimCommand.tryClaimChunkAt(chunkFor, claimant, worldChunks))
                claimant.sendMessage(TranslatableServerSide.text(claimant, "claim.chunk.error.claimed"));
            
            Claimant claimed;
            if ((claimed = ClaimantPlayer.get(chunkFor)) != null) {
                claimed.adjustCount(worldChunks.length);
                
                if ((claimed = ClaimantTown.get(((ClaimantPlayer) claimed).getTown())) != null) claimed.adjustCount(worldChunks.length);
            }
        });
    }
    public static boolean tryClaimChunkAt(final UUID chunkFor, final PlayerEntity claimant, final WorldChunk... worldChunks) {
        World world = claimant.getEntityWorld();
        
        int claimed = 0;
        try ( MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT INTO `chunk_Claimed` ( `chunkX`, `chunkZ`, `chunkOwner`, `chunkWorld` ) VALUES ( ?, ?, ?, ? );", true) ) {
            for ( WorldChunk pos : worldChunks ) {
                ChunkPos chunk = pos.getPos();
                
                if (stmt
                    .addPrepared(chunk.x)
                    .addPrepared(chunk.z)
                    .addPrepared(chunkFor)
                    .addPrepared(world.dimension.getType().getRawId())
                    .executeUpdate() <= 0) continue;
                
                // Log that the chunk was claimed
                CoreMod.logMessage(claimant.getName().asString() + " has claimed chunk " + chunk.x + ", " + chunk.z);
                
                ++claimed;
                
                // Update the owners of existing chunks
                ClaimedChunk claimedChunk;
                if ((claimedChunk = ClaimedChunk.convertFromCache(pos)) != null) claimedChunk.updatePlayerOwner( chunkFor );
            }
            
        } catch ( SQLException e ) {
            // If the chunk is already claimed
            if ( e.getErrorCode() == 1062 )
                return false;
            
            CoreMod.logError( e );
        }
        
        if ( worldChunks.length > 1 )
            TranslatableServerSide.send( claimant, "claim.chunk.claimed", claimed );
        
        return true;
    }
    
    private static int unclaimChunk(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Get run from positioning
        BlockPos blockPos = player.getBlockPos();
        World world = player.getEntityWorld();
        
        if (!ClaimCommand.tryUnclaimChunkAt( player.getUuid(), player, player.getBlockPos() ))
            throw CHUNK_NOT_OWNED_BY_PLAYER.create( player );
        
        // Update runtime
        ClaimedChunk chunk = ClaimedChunk.convert(world, blockPos);
        if (chunk != null)
            chunk.updatePlayerOwner( null );
        
        // Update total count
        Claimant claimed;
        if ((claimed = ClaimantPlayer.get( player )) != null) {
            claimed.adjustCount( -1 );
            
            if ((claimed = ClaimantTown.get(((ClaimantPlayer) claimed).getTown())) != null) claimed.adjustCount( -1 );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    public static boolean tryUnclaimChunkAt(final UUID chunkFor, final PlayerEntity claimant, final BlockPos blockPos) {
        World world = claimant.getEntityWorld();
        
        // Try unclaiming the chunk
        try ( MySQLStatement statement = CoreMod.getSQL().prepare("DELETE FROM `chunk_Claimed` WHERE `chunkX` = ? AND `chunkZ` = ? AND `chunkOwner` = ? AND `chunkWorld` = ?;" ) ) {
            
            // Update database
            int i = statement
                .addPrepared( blockPos.getX() >> 4 )
                .addPrepared( blockPos.getZ() >> 4 )
                .addPrepared( chunkFor )
                .addPrepared( world.dimension.getType().getRawId() )
                .executeUpdate();
            
            // If the chunk was NOT modified
            return ( i > 0 );
            
        } catch ( SQLException e ) {
            CoreMod.logError( e );
            return false;
        }
    }
    private static int unclaimAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Try unclaiming all of the users chunks
        try ( MySQLStatement stmt = CoreMod.getSQL().prepare( "DELETE FROM `chunk_Claimed` WHERE `chunkOwner` = ?;" ) ) {
            
            stmt.addPrepared( player.getUuid() ).executeUpdate();

            // Update total count
            Claimant claimed;
            if ((claimed = ClaimantPlayer.get( player )) != null) {
                TranslatableServerSide.send( player, "claim.chunk.unclaimed", claimed.getCount() );
                
                claimed.resetCount();
                
                CoreMod.CHUNK_CACHE.values().stream().filter((chunk) -> player.getUuid().equals(chunk.getOwner())).forEach((chunk) -> {
                    chunk.updatePlayerOwner( null );
                });
                if ((claimed = ClaimantTown.get(((ClaimantPlayer) claimed).getTown())) != null) claimed.resetCount();
            }
            
        } catch ( SQLException e ) {
            CoreMod.logError( e );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Town Options
     */
    private static boolean sourceIsMayor(final ServerCommandSource source) {
        try {
            return sourceIsMayor( source.getPlayer() );
        } catch (CommandSyntaxException e) {
            CoreMod.logError( e );
        }
        return false;
    }
    public static boolean sourceIsMayor(final ServerPlayerEntity player) {
        ClaimantPlayer claimantPlayer = ClaimantPlayer.get( player );
        return claimantPlayer.getTown() != null;
    }
    private static int townFound(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity founder = source.getPlayer();
        
        // Charge the player money
        try {
            if ((SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get() > 0) && (!MoneyCommand.databaseTakePlayerMoney(founder.getUuid(), SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get())))
                throw NOT_ENOUGH_MONEY.create(founder, SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get());
        } catch (SQLException e) {
            CoreMod.logError( e );
            return -1;
        }
        
        // Get town information
        UUID townUUID = UUID.randomUUID();
        String townName = StringArgumentType.getString( context, "name" );
        
        /*
         * Create our town
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT INTO `chunk_Towns` (`townId`, `townOwner`, `townName`) VALUES ( ?, ?, ? );")) {
            
            stmt.addPrepared( townUUID )
                .addPrepared( founder.getUuid() )
                .addPrepared( townName )
                .executeUpdate();
            
        } catch (SQLException e) {
            CoreMod.logError( e );
            return -1;
        }
        
        /*
         * Add player to the town
         */
        try {
            ClaimCommand.playerJoinsTown(
                source.getMinecraftServer().getPlayerManager(),
                founder,
                townUUID
            );
        } catch (SQLException e) {
            CoreMod.logError( e );
            return -1;
        }
        
        // Tell all players of the founding
        MessageUtils.sendToAll( source.getMinecraftServer().getPlayerManager(), "town.found",
            founder.getName().formatted(Formatting.AQUA),
            new LiteralText( townName ).formatted(Formatting.AQUA)
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int townDisband(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity founder = source.getPlayer();
        
        ClaimantPlayer claimantPlayer = ClaimantPlayer.get( founder );
        ClaimantTown claimantTown = ClaimantTown.get( claimantPlayer.getTown() );
        
        // Get town information
        UUID townUUID = claimantTown.getId();
        
        /*
         * Delete our town
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("DELETE FROM `chunk_Towns` WHERE `townId` = ? AND `townOwner` = ?;")) {
            
            stmt.addPrepared( townUUID )
                .addPrepared( founder.getUuid() )
                .executeUpdate();
            
            // Delete the town
            CoreMod.TOWNS_CACHE.remove( townUUID );
            
        } catch (SQLException e) {
            CoreMod.logError( e );
            return -1;
        }
        
        /*
         * Remove player from town
         */
        try {
            ClaimCommand.playerPartsTown(
                source.getMinecraftServer().getPlayerManager(),
                founder,
                claimantTown
            );
        } catch (SQLException e) {
            CoreMod.logError( e );
            return -1;
        }
        
        // Tell all other players
        MessageUtils.sendToAll( source.getMinecraftServer().getPlayerManager(), "town.disband",
            founder.getName().formatted(Formatting.AQUA),
            claimantTown.getName().formatted(Formatting.AQUA)
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private static void playerJoinsTown(final PlayerManager manager, final ServerPlayerEntity player, final UUID townUUID) throws SQLException {
        ClaimantPlayer claimaint = ClaimantPlayer.get( player );
        MySQLStatement stmt;
        
        /*
         * Set player as the towns owner
         */
        stmt = CoreMod.getSQL().prepare("INSERT INTO `player_Towns` ( `townId`, `townPlayer` ) VALUES ( ?, ? );", false );
        stmt.addPrepared( townUUID )
            .addPrepared( player.getUuid() )
            .executeUpdate();
        
        // Update players town
        claimaint.updateTown( townUUID );
        
        // Refresh the command tree
        manager.sendCommandTree( player );
        
        /*
         * Convert players chunks to their town
         */
        stmt = CoreMod.getSQL().prepare("UPDATE `chunk_Claimed` SET `chunkTown` = ? WHERE `chunkOwner` = ? AND `chunkTown` IS NULL;", false );
        stmt.addPrepared( townUUID )
            .addPrepared( player.getUuid() )
            .executeUpdate();
        
        ClaimCommand.notifyChangedClaimed( player.getUuid() );
    }
    private static void playerPartsTown(final PlayerManager manager, final ServerPlayerEntity player, final ClaimantTown town) throws SQLException {
        ClaimantPlayer claimaint = ClaimantPlayer.get( player );
        MySQLStatement stmt;
        
        boolean playerIsMayor = player.getUuid().equals( town.getOwner() );
        
        /*
         * Remove town owner
         */
        stmt = CoreMod.getSQL().prepare("DELETE FROM `player_Towns` WHERE `townId` = ?" + ( playerIsMayor ? "" : " AND `townPlayer` = ?" ) + ";")
            .addPrepared( town.getId() );
        if (!playerIsMayor) // If player is not mayor, only remove that players chunks
            stmt.addPrepared( player.getUuid() );
        stmt.executeUpdate();
        
        // Update the players town
        claimaint.updateTown( null );

        // Refresh the command tree
        manager.sendCommandTree( player );
        
        /*
         * Convert town chunks to none
         */
        stmt = CoreMod.getSQL().prepare("UPDATE `chunk_Claimed` SET `chunkTown` = NULL WHERE `chunkTown` = ?" + ( playerIsMayor ? "" : " AND `chunkOwner` = ?" ) + ";")
            .addPrepared( town.getId() );
        if (!playerIsMayor) // If player is not mayor, only remove that players chunks
            stmt.addPrepared( player.getUuid() );
        stmt.executeUpdate();

        ClaimCommand.notifyChangedClaimed( player.getUuid() );
    }
    
    /*
     * Update Chunk settings
     */
    private static int updateSetting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimPermissions permissions = EnumArgumentType.getEnum( ClaimPermissions.class, StringArgumentType.getString( context, "permission" ) );
        ClaimRanks rank = EnumArgumentType.getEnum( ClaimRanks.class, StringArgumentType.getString( context, "rank" ) );
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        try ( MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT INTO `chunk_Settings` ( `settingOwner`, `settingOption`, `settingRank` )  VALUES ( ?, ?, ? ) ON DUPLICATE KEY UPDATE `settingRank` = VALUES( `settingRank` );") ) {
            
            // Update the database
            stmt.addPrepared( player.getUuid() )
                .addPrepared( permissions )
                .addPrepared( rank )
                .executeUpdate();
            
            // ???
            // player.getServer().getSessionService().fillProfileProperties(new GameProfile( player.getUuid(), "" ), true);
            
            // Update the runtime
            ClaimantPlayer.get( player )
                .updatePermission( permissions, rank );
            
            // Notify the player
            player.sendMessage(new LiteralText("Interacting with ").formatted(Formatting.WHITE)
                .append(new LiteralText(CasingUtils.Sentence(permissions.name())).formatted(Formatting.AQUA))
                .append(new LiteralText(" is now limited to ").formatted(Formatting.WHITE))
                .append(new LiteralText(CasingUtils.Sentence(rank.name())).formatted(Formatting.AQUA))
                .append(new LiteralText(".").formatted(Formatting.WHITE))
            );
            
        } catch ( SQLException e ) {
            CoreMod.logError( e );
            
        }
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private static int updateBoolean(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimSettings setting = EnumArgumentType.getEnum( ClaimSettings.class, StringArgumentType.getString( context, "setting" ) );
        boolean enabled = BoolArgumentType.getBool( context, "bool" );
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        try ( MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT INTO `chunk_Options` ( `optionOwner`, `optionName`, `optionValue` ) VALUES ( ?, ?, ? ) ON DUPLICATE KEY UPDATE `optionValue` = VALUES( `optionValue` );") ) {
            
            // Update the database
            stmt
                .addPrepared( player.getUuid() )
                .addPrepared( setting.name() )
                .addPrepared( enabled ? "TRUE" : "FALSE" )
                .executeUpdate();
            
            // Update the runtime
            ClaimantPlayer.get( player )
                .updateSetting( setting, enabled );
            
            // Notify other players
            if (ClaimSettings.PLAYER_COMBAT.equals(setting)) {
                ClaimCommand.notifyChangedClaimed( player.getUuid() );
            }
            
            // Notify the player
            player.sendMessage(new LiteralText(CasingUtils.Words(setting.name().replace("_", " "))).formatted(Formatting.AQUA)
                .append(new LiteralText(" is now ").formatted(Formatting.WHITE))
                .append(new LiteralText( enabled ? "Enabled" : "Disabled" ).formatted(setting.getAttributeColor( enabled )))
                .append(new LiteralText(" in your claimed area.").formatted(Formatting.WHITE))
            );
            
        } catch ( SQLException e ) {
            CoreMod.logError( e );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Adding/Removing users from friends
     */
    private static int addRank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the targeted rank
        ClaimRanks rank = EnumArgumentType.getEnum( ClaimRanks.class, StringArgumentType.getString( context,"rank" ) );
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "friend" );
        GameProfile friend = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Player tries changing their own rank
        if ( player.getUuid().equals( friend.getId() ) )
            throw SELF_RANK_CHANGE.create( player );
        
        try ( MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT INTO `chunk_Friends` ( `chunkOwner`, `chunkFriend`, `chunkRank` ) VAlUES ( ?, ?, ? ) ON DUPLICATE KEY UPDATE `chunkRank` = VALUES( `chunkRank` );") ) {
            
            // Update database
            stmt
                .addPrepared( player.getUuid() )
                .addPrepared( friend.getId() )
                .addPrepared( rank )
                .executeUpdate();
            
            // Update our runtime
            ClaimantPlayer.get( player )
                .updateFriend( friend.getId(), rank );
            
            // Attempting to update the friend
            player.sendMessage(new LiteralText("Player ").formatted(Formatting.WHITE)
                .append( new LiteralText( friend.getName() ).formatted(Formatting.DARK_PURPLE) )
                .append( new LiteralText(" is now an ") )
                .append( new LiteralText( CasingUtils.Sentence(rank.name()) ).formatted(Formatting.AQUA) )
                .append( new LiteralText("." ).formatted(Formatting.WHITE) )
            );
            
            // Play a sound to the player
            player.playSound(SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.MASTER, 0.5f, 1f );
            
            // Find the entity of the friend
            ServerPlayerEntity friendEntity = context.getSource().getMinecraftServer().getPlayerManager().getPlayer( friend.getId() );
            
            if ( friendEntity != null ) {
                // Notify the friend
                friendEntity.addChatMessage(new LiteralText("Player ").formatted(Formatting.WHITE)
                    .append(player.getName().formatted(Formatting.DARK_PURPLE))
                    .append(new LiteralText(" has added you as an ").formatted(Formatting.WHITE))
                    .append(new LiteralText(rank.name()).formatted(Formatting.AQUA))
                    .append(new LiteralText(".").formatted(Formatting.WHITE)), false
                );
                
                friendEntity.playSound(SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.MASTER, 0.5f, 1f);
            }
            
        } catch ( SQLException e ) {
            CoreMod.logError( e );
            
        }
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private static int remRank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "friend" );
        GameProfile friend = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Player tries changing their own rank
        if ( player.getUuid().equals( friend.getId() ) )
            throw SELF_RANK_CHANGE.create( player );
        
        try ( MySQLStatement stmt = CoreMod.getSQL().prepare("DELETE FROM `chunk_Friends` WHERE `chunkOwner` = ? AND `chunkFriend` = ?;") ) {
            
            // Update database
            stmt
                .addPrepared( player.getUuid() )
                .addPrepared( friend.getId() )
                .executeUpdate();
            
            // Update our runtime
            ClaimantPlayer.get( player )
                .removeFriend( player.getUuid() );
            
            // Attempting to remove the friend
            player.sendMessage( new LiteralText("Player ").formatted(Formatting.WHITE)
                .append( new LiteralText( friend.getName() ).formatted(Formatting.DARK_PURPLE) )
                .append( new LiteralText(" removed."))
            );
            
            // Play sound to player
            player.playSound(SoundEvents.ENTITY_VILLAGER_DEATH, SoundCategory.MASTER, 0.5f, 1f );
            
            // Find the entity of the friend
            ServerPlayerEntity friendEntity = context.getSource().getMinecraftServer().getPlayerManager().getPlayer( friend.getId() );
            
            // If the friend is online
            if ( friendEntity != null ) {
                // Notify the friend
                friendEntity.addChatMessage(new LiteralText("Player ").formatted(Formatting.WHITE)
                    .append(player.getName().formatted(Formatting.DARK_PURPLE))
                    .append(new LiteralText(" has removed you.")), false
                );
                
                friendEntity.playSound(SoundEvents.ENTITY_VILLAGER_DEATH, SoundCategory.MASTER, 0.5f, 1f);
            }
        } catch ( SQLException e ){
            CoreMod.logError( e );
            
        }
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private static int listFriends(CommandContext<ServerCommandSource> context) {
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Let players add friends to the whitelist
     */
    private static int inviteFriend(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Whitelist whitelist = context.getSource().getMinecraftServer().getPlayerManager().getWhitelist();
        int count = 0;
        
        // Get the reference of the player to whitelist
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "friend" );
        
        Iterator<GameProfile> gameProfiles = argumentType.iterator();
        while ( gameProfiles.hasNext() ) {
            GameProfile profile = gameProfiles.next();
            if (!whitelist.isAllowed( profile )) {
                // Add profile to the whitelist
                whitelist.add( new WhitelistEntry( profile ) );
                context.getSource().sendFeedback(new TranslatableText("commands.whitelist.add.success", new Object[]{Texts.toText(profile)}), true);
                ++count;
            }
        }
        
        if (count == 0) {
            throw WHITELIST_FAILED_EXCEPTION.create();
        } else {
            return count;
        }
    }
    
    /*
     * Notify players of chunk claim changes
     */
    private static void notifyChangedClaimed(final UUID chunkOwner) {
        CoreMod.PLAYER_LOCATIONS.entrySet().stream().filter((entry) -> chunkOwner.equals(entry.getValue())).forEach((entry) -> {
            ServerPlayerEntity notifyPlayer = entry.getKey();
            PlayerMovement movement = ((PlayerMovement) notifyPlayer.networkHandler);
            movement.showPlayerNewLocation(notifyPlayer, ClaimedChunk.convert(notifyPlayer.getServerWorld(), notifyPlayer.getBlockPos()));
        });
    }
}
