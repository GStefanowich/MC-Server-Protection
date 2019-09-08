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
import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.commands.ArgumentTypes.EnumArgumentType;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.PlayerMovement;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.*;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;

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
    private static final DynamicCommandExceptionType CHUNK_NOT_OWNED = new DynamicCommandExceptionType((player) ->
        TranslatableServerSide.text( (ServerPlayerEntity) player, "claim.chunk.error.not_claimed" )
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
            .then(CommandManager.literal("force")
                .requires((source -> source.hasPermissionLevel( SewingMachineConfig.INSTANCE.CLAIM_OP_LEVEL_OTHER.get() )))
                .executes(ClaimCommand::unclaimOther)
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
                .requires(ClaimCommand::sourceNotMayor)
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
        CoreMod.logDebug( "- Registered Town command" );
        
        /*
         * Register the main command object
         */
        
        // The main command
        LiteralCommandNode<ServerCommandSource> protection = dispatcher.register( CommandManager.literal("protection" )
            // Claim a chunk
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
            
            // Chunk settings
            .then( CommandManager.literal("settings")
                .then( CommandManager.argument( "setting", StringArgumentType.word())
                    .suggests( EnumArgumentType.create( ClaimSettings.class )::listSuggestions )
                    .then( CommandManager.argument( "bool", BoolArgumentType.bool() )
                        .executes( ClaimCommand::updateBoolean )
                    )
                )
            )
            
            // Import from older version
            .then( CommandManager.literal("legacy-import")
                .requires((source -> source.hasPermissionLevel( 4 )))
                .executes( ClaimCommand::convertFromLegacy )
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
        
        List<WorldChunk> chunksToClaim = new ArrayList<>();
        // Check the radius that the player wants to claim
        final int radius = IntegerArgumentType.getInteger(context, "radius");
        IClaimedChunk[] claimedChunks = IClaimedChunk.getOwnedAround(player.getServerWorld(), player.getBlockPos(), radius);
        for (IClaimedChunk claimedChunk : claimedChunks) {
            if (!player.getUuid().equals(claimedChunk.getOwner( blockPos )))
                throw CHUNK_RADIUS_OWNED.create(player, claimedChunk.getOwnerName(player));
        }
        
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        
        // For the X axis
        for (int x = chunkX - radius; x <= chunkX + radius; x++) {
            // For the Z axis
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                // Create the chunk position
                WorldChunk worldChunk = world.getWorldChunk(new BlockPos(x << 4, 0, z << 4));
                // Add if not already claimed
                if (!player.getUuid().equals(((IClaimedChunk) worldChunk).getOwner( blockPos )))
                    chunksToClaim.add(worldChunk);
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
            try {
                if (!ClaimCommand.tryClaimChunkAt(chunkFor, claimant, worldChunks))
                    claimant.sendMessage(TranslatableServerSide.text(claimant, "claim.chunk.error.claimed"));
            } catch (TranslationKeyException e) {
                claimant.sendMessage(TranslatableServerSide.text(claimant, e.getKey()));
            }
        });
    }
    public static boolean tryClaimChunkAt(final UUID chunkFor, final PlayerEntity claimant, final WorldChunk... worldChunks) throws TranslationKeyException {
        boolean success = false;
        int claimed = 0; // Amount of chunks claimed
        
        try {
            for (WorldChunk worldChunk : worldChunks) {
                IClaimedChunk chunk = (IClaimedChunk) worldChunk;
                
                // Check if it's available
                chunk.canPlayerClaim(chunkFor);
                
                // Update the chunk owner
                chunk.updatePlayerOwner(chunkFor);
                worldChunk.setShouldSave(true);
                
                // Log that the chunk was claimed
                ChunkPos chunkPos = worldChunk.getPos();
                CoreMod.logInfo(claimant.getName().asString() + " has claimed chunk " + chunkPos.x + ", " + chunkPos.z);
                
                // Save the chunk for the player
                Claimant claim;
                if ((claim = ClaimantPlayer.get(chunkFor)) != null) {
                    claim.addToCount(worldChunk);
                    
                    if ((claim = ((ClaimantPlayer) claim).getTown()) != null)
                        claim.addToCount(worldChunk);
                }
                
                // Increase our counter
                ++claimed;
            }
            
            return (success = true);
        } finally {
            if (success || (claimed > 0))
                TranslatableServerSide.send( claimant, "claim.chunk.claimed", claimed );
        }
    }
    
    private static int unclaimChunk(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Get run from positioning
        BlockPos blockPos = player.getBlockPos();
        World world = player.getEntityWorld();
        
        if (!ClaimCommand.tryUnclaimChunkAt( player.getUuid(), player, blockPos ))
            throw ClaimCommand.CHUNK_NOT_OWNED_BY_PLAYER.create( player );
        
        // Get chunk
        WorldChunk worldChunk = world.getWorldChunk( blockPos );
        
        // Update total count
        Claimant claimed;
        if ((claimed = ((PlayerData) player).getClaim()) != null) {
            claimed.removeFromCount( worldChunk );
            
            if ((claimed = ((ClaimantPlayer) claimed).getTown()) != null) claimed.removeFromCount( worldChunk );
        }
        
        // Update runtime
        ((IClaimedChunk) worldChunk).updatePlayerOwner( null );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int unclaimOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        // Get run from positioning
        BlockPos blockPos = player.getBlockPos();
        World world = player.getEntityWorld();
        
        // Convert from position
        WorldChunk worldChunk = world.getWorldChunk( blockPos );
        final UUID currentOwner = ((IClaimedChunk) worldChunk).getOwner( blockPos );
        if (currentOwner == null )
            throw ClaimCommand.CHUNK_NOT_OWNED.create(player);
        
        if (!ClaimCommand.tryUnclaimChunkAt( currentOwner, player, blockPos ))
            throw ClaimCommand.CHUNK_NOT_OWNED_BY_PLAYER.create( player );
        
        // Update total count
        Claimant claimed;
        if ((claimed = ClaimantPlayer.get( currentOwner )) != null) {
            claimed.removeFromCount( worldChunk );
            
            if ((claimed = ((ClaimantPlayer) claimed).getTown()) != null) claimed.removeFromCount( worldChunk );
        }
        
        // Update the owner to NONE
        ((IClaimedChunk) worldChunk).updatePlayerOwner( null );
        
        return Command.SINGLE_SUCCESS;
    }
    public static boolean tryUnclaimChunkAt(final UUID chunkFor, final PlayerEntity claimant, final BlockPos blockPos) {
        World world = claimant.getEntityWorld();
        return ClaimCommand.tryUnclaimChunkAt( chunkFor, world.getWorldChunk( blockPos ));
    }
    private static boolean tryUnclaimChunkAt(final UUID chunkFor, final WorldChunk chunk) {
        if ( !chunkFor.equals( ((IClaimedChunk) chunk).getOwner() ) )
            return false;
        
        // Update total count
        Claimant claimed;
        if ((claimed = ClaimantPlayer.get( chunkFor )) != null) {
            // Remove the players count
            claimed.removeFromCount( chunk );
            
            // Remove the towns count
            if ((claimed = ((IClaimedChunk)chunk).getTown()) != null) claimed.removeFromCount( chunk );
        }
        
        // Set the chunks owner
        ((IClaimedChunk) chunk).updatePlayerOwner( null );
        
        return true;
    }
    private static int unclaimAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        final MinecraftServer server = source.getMinecraftServer();
        
        // Update total count
        Claimant claimed;
        if ((claimed = ((PlayerData) player).getClaim()) != null) {
            // Send the message to the claimant
            TranslatableServerSide.send( player, "claim.chunk.unclaimed", claimed.getCount() );
            
            // Unclaim EVERY chunk
            claimed.forEachChunk((set) -> {
                // Get the dimension
                DimensionType dimension = DimensionType.byRawId(set[0]);
                ServerWorld world = server.getWorld(dimension);
                
                // Unclaim the chunk
                ClaimCommand.tryUnclaimChunkAt(player.getUuid(), (WorldChunk)world.getChunk( set[1], set[2], ChunkStatus.FULL ));
            });
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Town Options
     */
    private static boolean sourceIsMayor(final ServerCommandSource source) {
        try {
            return ClaimCommand.sourceIsMayor( source.getPlayer() );
        } catch (CommandSyntaxException e) {
            CoreMod.logError( e );
        }
        return false;
    }
    public static boolean sourceIsMayor(final ServerPlayerEntity player) {
        ClaimantPlayer claim = ((PlayerData) player).getClaim();
        ClaimantTown town;
        
        // Check if owner
        if ((claim != null) && ((town = claim.getTown()) != null))
            return player.getUuid().equals(town.getOwner());
        
        return false;
    }
    private static boolean sourceNotMayor(final ServerCommandSource source) {
        return !ClaimCommand.sourceIsMayor( source );
    }
    public static boolean sourceInTown(final ServerCommandSource source) {
        try {
            return ClaimCommand.sourceInTown( source.getPlayer() );
        } catch (CommandSyntaxException e) {
            CoreMod.logError( e );
        }
        return false;
    }
    private static boolean sourceInTown(final ServerPlayerEntity player) {
        ClaimantPlayer claim = ((PlayerData) player).getClaim();
        return ((claim != null) && claim.getTown() != null);
    }
    
    private static int townFound(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity founder = source.getPlayer();
        
        // Charge the player money
        try {
            if ((SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get() > 0) && (!MoneyUtils.takePlayerMoney(founder, SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get())))
                throw NOT_ENOUGH_MONEY.create(founder, SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get());
        } catch (NotEnoughMoneyException e) {
            throw NOT_ENOUGH_MONEY.create(founder, SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get());
        }
        try {
            // Get town information
            Text townName = new LiteralText(StringArgumentType.getString(context, "name"));
            ClaimantTown town = ClaimantTown.makeTown(founder, townName);
            
            // Tell all players of the founding
            MessageUtils.sendToAll("town.found",
                founder.getName().formatted(Formatting.AQUA),
                town.getName().formatted(Formatting.AQUA)
            );
        } catch (Throwable t) {
            CoreMod.logError( t );
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int townDisband(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity founder = source.getPlayer();
        
        ClaimantPlayer claimantPlayer = ((PlayerData) founder).getClaim();
        ClaimantTown claimantTown;
        
        if ((claimantPlayer == null) || ((claimantTown = claimantPlayer.getTown() ) == null))
            return -1;
        
        // Delete the town
        claimantTown.delete();
        
        // Tell all other players
        MessageUtils.sendToAll( "town.disband",
            founder.getName().formatted(Formatting.AQUA),
            claimantTown.getName().formatted(Formatting.AQUA)
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int townInvite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    private static void playerJoinsTown(final PlayerManager manager, final ServerPlayerEntity player, final UUID townUUID) throws SQLException {
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        //MySQLStatement stmt;
        
        /*
         * Set player as the towns owner
         */
        /*stmt = CoreMod.getSQL().prepare("INSERT INTO `player_Towns` ( `townId`, `townPlayer` ) VALUES ( ?, ? );", false );
        stmt.addPrepared( townUUID )
            .addPrepared( player.getUuid() )
            .executeUpdate();*/
        
        /*
         * Convert players chunks to their town
         */
        /*stmt = CoreMod.getSQL().prepare("UPDATE `chunk_Claimed` SET `chunkTown` = ? WHERE `chunkOwner` = ? AND `chunkTown` IS NULL;", false );
        stmt.addPrepared( townUUID )
            .addPrepared( player.getUuid() )
            .executeUpdate();*/
        
        // Update players town
        //claimant.updateTown( townUUID );
        
        // Refresh the command tree
        manager.sendCommandTree( player );
        
        // Notify the players in claimed chunks
        ClaimCommand.notifyChangedClaimed( player.getUuid() );
    }
    private static void playerPartsTown(final PlayerManager manager, final ServerPlayerEntity player, final ClaimantTown town) throws SQLException {
        ClaimantPlayer claimaint = ((PlayerData) player).getClaim();
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
        claimaint.setTown( null );

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
            ((PlayerData) player).getClaim()
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
            ((PlayerData) player).getClaim()
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
            ((PlayerData) player).getClaim()
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
            ((PlayerData) player).getClaim()
                .updateFriend( player.getUuid(), null );
            
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
     * Convert from legacy database version
     */
    private static int convertFromLegacy(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (LegacyConverter.exists())
            return -1;
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return -1;
        
        new Thread(() -> {
            try (LegacyConverter converter = LegacyConverter.create()) {
                
            }
        }).start();
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Notify players of chunk claim changes
     */
    private static void notifyChangedClaimed(final UUID chunkOwner) {
        CoreMod.PLAYER_LOCATIONS.entrySet().stream().filter((entry) -> chunkOwner.equals(entry.getValue())).forEach((entry) -> {
            ServerPlayerEntity notifyPlayer = entry.getKey();
            PlayerMovement movement = ((PlayerMovement) notifyPlayer.networkHandler);
            movement.showPlayerNewLocation(notifyPlayer, notifyPlayer.getServerWorld().getWorldChunk(notifyPlayer.getBlockPos()));
        });
    }
}
