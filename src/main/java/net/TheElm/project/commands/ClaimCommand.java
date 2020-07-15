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
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.ServerCore;
import net.TheElm.project.commands.ArgumentTypes.EnumArgumentType;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.PlayerMovement;
import net.TheElm.project.interfaces.VillagerTownie;
import net.TheElm.project.interfaces.WhitelistedPlayer;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.CommandUtilities;
import net.TheElm.project.utilities.LegacyConverter;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandSource;
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
import net.minecraft.util.UserCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ClaimCommand {
    
    private static final ExceptionTranslatableServerSide NOT_ENOUGH_MONEY = new ExceptionTranslatableServerSide("town.found.poor", 1);
    private static final ExceptionTranslatableServerSide SELF_RANK_CHANGE = new ExceptionTranslatableServerSide("friends.rank.self");
    private static final ExceptionTranslatableServerSide CHUNK_NOT_OWNED_BY_PLAYER = new ExceptionTranslatableServerSide("claim.chunk.error.not_players");
    private static final ExceptionTranslatableServerSide CHUNK_ALREADY_OWNED = new ExceptionTranslatableServerSide("claim.chunk.error.claimed");
    private static final ExceptionTranslatableServerSide CHUNK_NOT_OWNED = new ExceptionTranslatableServerSide("claim.chunk.error.not_claimed");
    private static final ExceptionTranslatableServerSide CHUNK_RADIUS_OWNED = new ExceptionTranslatableServerSide("claim.chunk.error.radius_owned", 1 );
    private static final SimpleCommandExceptionType WHITELIST_FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.whitelist.add.failed"));
    private static final ExceptionTranslatableServerSide TOWN_INVITE_RANK = new ExceptionTranslatableServerSide("town.invite.rank");
    private static final ExceptionTranslatableServerSide TOWN_INVITE_FAIL = new ExceptionTranslatableServerSide("town.invite.fail");
    private static final ExceptionTranslatableServerSide TOWN_INVITE_MISSING = new ExceptionTranslatableServerSide("town.invite.missing");
    private static final SimpleCommandExceptionType TOWN_NOT_EXISTS = new SimpleCommandExceptionType(new LiteralText("That town does not exist."));
    
    private ClaimCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return;
        
        /*
         * Admin Force commands
         */
        dispatcher.register(CommandManager.literal("chunk")
            .requires((source) -> source.hasPermissionLevel(OpLevels.STOP))
            .then(CommandManager.literal("set")
                .then(CommandManager.literal("player")
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtilities::getAllPlayerNames)
                        .executes(ClaimCommand::rawSetChunkPlayer)
                    )
                )
                .then(CommandManager.literal("town")
                    .executes(ClaimCommand::rawSetChunkTown)
                )
            )
        );
        
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
                .requires((source) -> source.hasPermissionLevel(SewingMachineConfig.INSTANCE.CLAIM_OP_LEVEL_OTHER.get()))
                .executes(ClaimCommand::claimChunkOther)
            )
            // Claim chunk for the spawn
            .then(CommandManager.literal( "spawn" )
                .requires((source) -> source.hasPermissionLevel(SewingMachineConfig.INSTANCE.CLAIM_OP_LEVEL_SPAWN.get()))
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
                .executes(ClaimCommand::unclaimChunkOther)
            )
            // Unclaim current chunk
            .executes(ClaimCommand::unclaimChunk)
        );
        CoreMod.logDebug( "- Registered Unclaim command" );
        
        /*
         * Friends Command
         */
        LiteralCommandNode<ServerCommandSource> friends = dispatcher.register(CommandManager.literal("friends")
            // Whitelist a friend
            .then(CommandManager.literal( "whitelist" )
                .requires((context) -> SewingMachineConfig.INSTANCE.FRIEND_WHITELIST.get())
                .then( CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .suggests((context, builder) -> {
                        PlayerManager manager = context.getSource().getMinecraftServer().getPlayerManager();
                        return CommandSource.suggestMatching(manager.getPlayerList().stream()
                            .filter(( player ) -> !manager.getWhitelist().isAllowed( player.getGameProfile() ))
                            .map(( player ) -> player.getGameProfile().getName()), builder);
                    })
                    .executes(ClaimCommand::inviteFriend)
                )
                .executes(ClaimCommand::invitedListSelf)
            )
            .then(CommandManager.literal("get")
                .requires((source) -> source.hasPermissionLevel(OpLevels.KICK_BAN_OP))
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtilities::getAllPlayerNames)
                    .executes(ClaimCommand::invitedListOther)
                )
            )
            // Set a friends rank
            .then(CommandManager.literal("set")
                .then(CommandManager.argument( "rank", StringArgumentType.word())
                    .suggests(EnumArgumentType.enumerate( ClaimRanks.class ))
                    .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtilities::getAllPlayerNames)
                        .executes(ClaimCommand::addRank)
                    )
                )
            )
            // Remove a friends rank
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    //.suggests(CommandUtilities::getAllPlayerNames)
                    .executes(ClaimCommand::remRank)
                )
            )
            .executes(ClaimCommand::listFriends)
        );
        CoreMod.logDebug( "- Registered Friends command" );
        
        /*
         * Register the town command
         */
        LiteralCommandNode<ServerCommandSource> towns = dispatcher.register( CommandManager.literal( "town" )
            .then(CommandManager.literal("new" )
                .requires(ClaimCommand::sourceNotMayor)
                .then( CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(ClaimCommand::townFound)
                )
            )
            .then(CommandManager.literal("disband")
                .requires(ClaimCommand::sourceIsMayor)
                .executes(ClaimCommand::townDisband)
            )
            .then(CommandManager.literal("claim")
                .requires(ClaimCommand::sourceIsMayor)
                .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtilities::getAllPlayerNames)
                    .executes(ClaimCommand::townGiveChunk)
                )
                .executes(ClaimCommand::claimChunkTown)
            )
            .then(CommandManager.literal("unclaim")
                .requires(ClaimCommand::sourceInTown)
                .executes(ClaimCommand::unclaimChunkTown)
            )
            .then(CommandManager.literal("invite")
                .requires(ClaimCommand::sourceIsMayor)
                .then( CommandManager.argument("target", EntityArgumentType.player())
                    .executes(ClaimCommand::townInvite)
                )
            )
            .then(CommandManager.literal("join")
                .requires(ClaimCommand::sourceNotInTown)
                .then( CommandManager.argument("town", StringArgumentType.greedyString())
                    .suggests(ClaimCommand::listTownInvites)
                    .executes(ClaimCommand::playerJoinsTown)
                )
            )
            .then(CommandManager.literal("leave")
                .requires((source) -> ClaimCommand.sourceInTown( source ) && ClaimCommand.sourceNotMayor( source ))
                .executes(ClaimCommand::playerPartsTown)
            )
            .then(CommandManager.literal("set")
                .requires((source) -> source.hasPermissionLevel(OpLevels.STOP))
                .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtilities::getAllPlayerNames)
                    .then(CommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(CommandUtilities::getAllTowns)
                        .executes(ClaimCommand::adminSetPlayerTown)
                    )
                )
            )
            .then(CommandManager.literal("villagers")
                .requires((source) -> source.hasPermissionLevel(OpLevels.STOP))
                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                    .then(CommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(CommandUtilities::getAllTowns)
                        .executes(ClaimCommand::adminSetEntityTown)
                    )
                )
            )
        );
        CoreMod.logDebug( "- Registered Town command" );
        
        /*
         * Register the main command object
         */
        
        // The main command
        LiteralCommandNode<ServerCommandSource> protection = dispatcher.register( CommandManager.literal("protection" )
            // Claim a chunk
            .then( claim )
            
            // Unclaim a chunk
            .then( unclaim )
            
            // Towns
            .then( towns )
            
            // Update claim permissions
            .then( CommandManager.literal("permissions")
                .then( CommandManager.argument( "permission", StringArgumentType.word())
                    .suggests( EnumArgumentType.enumerate( ClaimPermissions.class ) )
                    .then( CommandManager.argument( "rank", StringArgumentType.word())
                        .suggests( EnumArgumentType.enumerate( ClaimRanks.class ) )
                        .executes( ClaimCommand::updateSetting )
                    )
                )
                .then( CommandManager.literal("*")
                    .then( CommandManager.argument( "rank", StringArgumentType.word())
                        .suggests(EnumArgumentType.enumerate(ClaimRanks.class ))
                        .executes(ClaimCommand::updateSettings)
                    )
                )
            )
            
            // Update friends
            .then( friends )
            
            // Chunk settings
            .then(CommandManager.literal("settings")
                .then(CommandManager.argument( "setting", StringArgumentType.word())
                    .suggests(EnumArgumentType.enumerate( ClaimSettings.class ))
                    .then(CommandManager.argument( "bool", BoolArgumentType.bool())
                        .executes(ClaimCommand::updateBoolean)
                    )
                )
            )
            
            // Import from older version
            .then(CommandManager.literal("legacy-import")
                .requires((source -> source.hasPermissionLevel( 4 ) && LegacyConverter.isLegacy()))
                .executes(ClaimCommand::convertFromLegacy )
            )
        );
        
        CoreMod.logDebug( "- Registered Protection command" );
    }
    
    /*
     * Set data for a chunk
     */
    private static int rawSetChunkPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "target");
        
        ServerPlayerEntity player = source.getPlayer();
        for (GameProfile target : targets)
            return ClaimCommand.claimChunkAt(target.getId(), false, player, player.world.getWorldChunk(player.getBlockPos()));
        return 0;
    }
    private static int rawSetChunkTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Claim a chunk
     */
    private static int claimChunkSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player running the command
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Claim the chunk for own player
        return ClaimCommand.claimChunk(player.getUuid(), player);
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
        return ClaimCommand.claimChunkAt(player.getUuid(), true, player, chunksToClaim.toArray(new WorldChunk[0]));
    }
    private static int claimChunkTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getServerWorld();
        
        // Attempt to claim the chunk
        WorldChunk chunk = (WorldChunk) world.getChunk(player.getBlockPos());
        if (!player.getUuid().equals(((IClaimedChunk) chunk).getOwner()))
            ClaimCommand.claimChunkSelf( context );
        
        // Update owner of the town
        ClaimantTown town = ((PlayerData) player).getClaim().getTown();
        if (town != null) {
            if ((((IClaimedChunk) chunk).getTownId() != null))
                throw CHUNK_ALREADY_OWNED.create( player );
            ((IClaimedChunk) chunk).updateTownOwner(town.getId());
        }
        
        // Notify the players in claimed chunks
        ClaimCommand.notifyChangedClaimed( player.getUuid() );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int claimChunkOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player running the command
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "target" );
        GameProfile targetPlayer = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Claim the chunk for other player
        return ClaimCommand.claimChunk( targetPlayer.getId(), player );
    }
    private static int claimChunkSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player running the command
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        if (SewingMachineConfig.INSTANCE.LIGHT_UP_SPAWN.get())
            ChunkUtils.lightChunk(source.getWorld().getWorldChunk(player.getBlockPos()));
        
        // Claim the chunk for spawn
        return ClaimCommand.claimChunk(CoreMod.spawnID, player);
    }
    private static int claimChunk(@NotNull final UUID chunkFor, @NotNull final ServerPlayerEntity claimant) {
        // Get run from positioning
        BlockPos blockPos = claimant.getBlockPos();
        return ClaimCommand.claimChunkAt(chunkFor, true, claimant, claimant.world.getWorldChunk(blockPos));
    }
    public static int claimChunkAt(@NotNull final UUID chunkFor, final boolean verify, @Nullable final ServerPlayerEntity claimant, final WorldChunk... worldChunks) {
        new Thread(ClaimCommand.claimChunkThread( chunkFor, verify, claimant, worldChunks)).start();
        return Command.SINGLE_SUCCESS;
    }
    private static Runnable claimChunkThread(@NotNull final UUID chunkFor, final boolean verify, @Nullable final ServerPlayerEntity claimant, final WorldChunk... worldChunks) {
        // Handle the claim in a new thread (Because it could have to load chunks)
        return (() -> {
            try {
                if (!ClaimCommand.tryClaimChunkAt(chunkFor, verify, claimant, worldChunks))
                    throw CHUNK_ALREADY_OWNED.create( claimant );
            } catch (TranslationKeyException e) {
                if (claimant != null) claimant.sendMessage(TranslatableServerSide.text(claimant, e.getKey()));
            } catch (CommandSyntaxException e) {
                if (claimant != null) claimant.sendMessage(new LiteralText( e.getMessage() ).formatted(Formatting.RED));
            }
        });
    }
    public static boolean tryClaimChunkAt(@NotNull final UUID chunkFor, final boolean verify, @Nullable final ServerPlayerEntity claimant, final WorldChunk... worldChunks) throws TranslationKeyException {
        boolean success = false;
        int claimed = 0; // Amount of chunks claimed
        
        try {
            for (WorldChunk worldChunk : worldChunks) {
                Claimant claim = ClaimantPlayer.get(chunkFor);
                IClaimedChunk chunk = (IClaimedChunk) worldChunk;
                
                // Check if it's available
                if (verify) chunk.canPlayerClaim((ClaimantPlayer)claim);
                
                // Update the chunk owner
                chunk.updatePlayerOwner(claim.getId());
                worldChunk.setShouldSave(true);
                
                // Log that the chunk was claimed
                ChunkPos chunkPos = worldChunk.getPos();
                if (claimant != null)
                    CoreMod.logInfo(claimant.getName().asString() + " has claimed chunk " + chunkPos.x + ", " + chunkPos.z);
                
                // Save the chunk for the player
                claim.addToCount(worldChunk);
                
                // Save the chunk for the town
                if ((claim = ((ClaimantPlayer) claim).getTown()) != null)
                    claim.addToCount(worldChunk);
                
                // Increase our counter
                ++claimed;
            }
            
            return (success = true);
        } finally {
            if ((claimant != null) && (success || (claimed > 0)))
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
    private static int unclaimChunkTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = player.getServerWorld();
        
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        
        WorldChunk chunk = (WorldChunk) world.getChunk(player.getBlockPos());
        if (((IClaimedChunk) chunk).getTownId() == null)
            throw CHUNK_NOT_OWNED.create( player );
        
        // Town SHOULDN'T be null here
        if (claimant.getTownId() == null) return -1;
        
        if (!claimant.getTownId().equals(((IClaimedChunk)chunk).getTownId()))
            throw CHUNK_NOT_OWNED_BY_PLAYER.create( player );
        
        // Change the town owner
        ((IClaimedChunk) chunk).updateTownOwner( null );
        
        // Notify the players in claimed chunks
        ClaimCommand.notifyChangedClaimed( player.getUuid() );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int unclaimChunkOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
        Claimant claimed = ClaimantPlayer.get( currentOwner );
        claimed.removeFromCount( worldChunk );
        
        if ((claimed = ((ClaimantPlayer) claimed).getTown()) != null) claimed.removeFromCount( worldChunk );
        
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
        Claimant claimed = ClaimantPlayer.get( chunkFor );
        
        // Remove the players count
        claimed.removeFromCount( chunk );
        
        // Remove the towns count
        if ((claimed = ((IClaimedChunk)chunk).getTown()) != null) claimed.removeFromCount( chunk );
        
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
                DimensionType dimension = set.getDimension();
                if (dimension != null) {
                    ServerWorld world = server.getWorld(dimension);
                    
                    // Unclaim the chunk
                    ClaimCommand.tryUnclaimChunkAt(player.getUuid(), (WorldChunk) world.getChunk(set.getX(), set.getZ(), ChunkStatus.FULL));
                }
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
    public static boolean sourceNotInTown(final ServerCommandSource source) {
        return !ClaimCommand.sourceInTown( source );
    }
    
    private static int townFound(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getMinecraftServer();
        ServerPlayerEntity founder = source.getPlayer();
        
        // Charge the player money
        try {
            if ((SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get() > 0) && (!MoneyUtils.takePlayerMoney(founder, SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get())))
                throw NOT_ENOUGH_MONEY.create(founder, "$" + NumberFormat.getInstance().format(SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get()));
        } catch (NotEnoughMoneyException e) {
            throw NOT_ENOUGH_MONEY.create(founder, "$" + NumberFormat.getInstance().format(SewingMachineConfig.INSTANCE.TOWN_FOUND_COST.get()));
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
            
            // Resend the command tree
            server.getPlayerManager().sendCommandTree( founder );
        } catch (Throwable t) {
            CoreMod.logError( t );
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int townDisband(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getMinecraftServer();
        ServerPlayerEntity founder = source.getPlayer();
        
        ClaimantPlayer claimantPlayer = ((PlayerData) founder).getClaim();
        ClaimantTown claimantTown;
        
        // Check that player is in a town (Should ALWAYS be TRUE here)
        if ((claimantPlayer == null) || ((claimantTown = claimantPlayer.getTown() ) == null))
            return -1;
        
        // Delete the town
        claimantTown.delete();
        
        // Tell all other players
        MessageUtils.sendToAll( "town.disband",
            founder.getName().formatted(Formatting.AQUA),
            claimantTown.getName().formatted(Formatting.AQUA)
        );
        
        // Resend the command tree
        server.getPlayerManager().sendCommandTree( founder );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int townInvite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        ClaimantTown town = claimant.getTown();
        if (town == null) return -1; // Towns SHOULD always be set when reaching here
        
        if (town.getFriendRank( player.getUuid() ) != ClaimRanks.OWNER)
            throw TOWN_INVITE_RANK.create( player );
        
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        ClaimantPlayer targetClaimant = ((PlayerData) target).getClaim();
        
        if ( !targetClaimant.inviteTown( town ) )
            throw TOWN_INVITE_FAIL.create( player );
        
        // Send the notice to the inviter
        TranslatableServerSide.send( player, "town.invite.sent", target.getDisplayName());
        
        // Send the notice to the invitee
        TranslatableServerSide.send( target, "town.invite.receive", town.getName(), player.getDisplayName());
        
        return Command.SINGLE_SUCCESS;
    }
    private static int townGiveChunk(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "target" );
        GameProfile targetPlayer = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        ServerWorld world = player.getServerWorld();
        IClaimedChunk claimedChunk = (IClaimedChunk) world.getChunk( player.getBlockPos() );
        
        ClaimantTown town;
        if ((claimedChunk.getOwner() == null) || ((town = claimedChunk.getTown()) == null) || (!player.getUuid().equals(claimedChunk.getOwner())) || (!player.getUuid().equals( town.getOwner() )))
            throw CHUNK_NOT_OWNED_BY_PLAYER.create( player );
        
        claimedChunk.updatePlayerOwner( targetPlayer.getId() );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int playerJoinsTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getMinecraftServer();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        
        String townName = StringArgumentType.getString(context, "town");
        ClaimantTown town = claimant.getTownInvite(townName);
        if (town == null)
            throw TOWN_INVITE_MISSING.create( player );
        
        /* 
         * Update players town
         */
        town.updateFriend( player, ClaimRanks.ALLY );
        //claimant.setTown( town );
        
        // Tell the player
        TranslatableServerSide.send(player, "town.invite.join", town.getName());
        
        // Refresh the command tree
        server.getPlayerManager().sendCommandTree( player );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int playerPartsTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getMinecraftServer();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimaint = ((PlayerData) player).getClaim();
        
        /*
         * Remove town from player
         */
        ClaimantTown town = claimaint.getTown();
        if (town != null) town.updateFriend( player.getUuid(), null );
        //claimaint.setTown( null );
        
        // Refresh the command tree
        server.getPlayerManager().sendCommandTree( player );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int adminSetPlayerTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context,"target");
        GameProfile player = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the town
        String townName = StringArgumentType.getString(context, "town");
        ClaimantTown town = CoreMod.getFromCache(ClaimantTown.class, townName);
        if (town == null)
            throw TOWN_INVITE_MISSING.create(source.getPlayer());
        
        // Update the rank of the player for the town
        town.updateFriend( player.getId(), ClaimRanks.ALLY );
        ClaimantPlayer.get( player ).setTown( town );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int adminSetEntityTown(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "entities");
        String townName = StringArgumentType.getString(context, "town");
        ClaimantTown town = CoreMod.getFromCache(ClaimantTown.class, townName);
        if (town == null)
            throw TOWN_NOT_EXISTS.create();
        
        int added = 0;
        for (Entity entity : entities) {
            if (!(entity instanceof VillagerEntity))
                continue;
            if (((VillagerTownie)entity).setTown(town))
                added++;
        }
        
        Text amount = new LiteralText(NumberFormat.getInstance().format(added)).formatted(Formatting.AQUA);
        source.sendFeedback(new LiteralText("Added ")
            .append(amount)
            .append(" villagers to ")
            .append(town.getName())
            .append("."), false);
        if (added > 0) {
            town.send(new LiteralText("")
                .append(amount)
                .append(" villagers have been added to your town."));
        }
        
        return added;
    }
    private static CompletableFuture<Suggestions> listTownInvites(CommandContext<ServerCommandSource> context, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        
        // Suggestion set
        Set<String> set = new HashSet<>();
        for (ClaimantTown town : claimant.getTownInvites())
            set.add(town.getName().asString());
        
        // Return the output
        return CommandSource.suggestMatching( set.stream(), suggestionsBuilder );
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
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private static int updateSettings(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimRanks rank = EnumArgumentType.getEnum(ClaimRanks.class, StringArgumentType.getString(context, "rank"));
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        for ( ClaimPermissions permissions : ClaimPermissions.values() ) {
            
            // Update the runtime
            ((PlayerData) player).getClaim()
                .updatePermission(permissions, rank);
            
            // Notify the player
            player.sendMessage(new LiteralText("Interacting with ").formatted(Formatting.WHITE)
                .append(new LiteralText(CasingUtils.Sentence(permissions.name())).formatted(Formatting.AQUA))
                .append(new LiteralText(" is now limited to ").formatted(Formatting.WHITE))
                .append(new LiteralText(CasingUtils.Sentence(rank.name())).formatted(Formatting.AQUA))
                .append(new LiteralText(".").formatted(Formatting.WHITE))
            );
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
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Adding/Removing users from friends
     */
    private static int addRank(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the targeted rank
        ClaimRanks rank = EnumArgumentType.getEnum( ClaimRanks.class, StringArgumentType.getString( context,"rank" ) );
        
        ServerCommandSource source = context.getSource();
        
        // Get the player
        ServerPlayerEntity player = source.getPlayer();
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "friend" );
        GameProfile friend = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Player tries changing their own rank
        if ( player.getUuid().equals( friend.getId() ) )
            throw SELF_RANK_CHANGE.create( player );
        
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
        ServerPlayerEntity friendEntity = ServerCore.getPlayer( friend.getId() );
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
        ServerCommandSource source = context.getSource();
        Whitelist whitelist = source.getMinecraftServer().getPlayerManager().getWhitelist();
        int count = 0;
        
        // Get the reference of the player to whitelist
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "friend" );
        
        Iterator<GameProfile> gameProfiles = argumentType.iterator();
        while ( gameProfiles.hasNext() ) {
            GameProfile profile = gameProfiles.next();
            if (!whitelist.isAllowed( profile )) {
                // Create the entry and set invited by
                WhitelistEntry entry = new WhitelistEntry( profile );
                
                Entity player = source.getEntity();
                if (player instanceof PlayerEntity)
                    ((WhitelistedPlayer)entry).setInvitedBy(player.getUuid());
                
                // Add profile to the whitelist
                whitelist.add( entry );
                
                // For each profile added to the whitelist
                context.getSource().sendFeedback(new TranslatableText("commands.whitelist.add.success", Texts.toText( profile )), true);
                ++count;
            }
        }
        
        if (count == 0) {
            throw WHITELIST_FAILED_EXCEPTION.create();
        } else {
            return count;
        }
    }
    private static int invitedListSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get command context information
        ServerCommandSource source = context.getSource();
        
        return ClaimCommand.invitedList(
            source,
            source.getPlayer().getGameProfile()
        );
    }
    private static int invitedListOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get command context information
        ServerCommandSource source = context.getSource();
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        
        for (GameProfile profile : profiles) {
            return ClaimCommand.invitedList(
                source,
                profile
            );
        }
        
        return 0;
    }
    private static int invitedList(ServerCommandSource source, GameProfile player) throws CommandSyntaxException {
        // Get information about the server
        MinecraftServer server = source.getMinecraftServer();
        UserCache cache = server.getUserCache();
        Whitelist whitelist = server.getPlayerManager().getWhitelist();
        
        // Store information about invites
        GameProfile invitedBy = null;
        List<WhitelistEntry> invited = new ArrayList<>();
        
        ClaimantPlayer claim = ClaimantPlayer.get(player.getId());
        
        // Loop the whitelist
        for (WhitelistEntry entry : whitelist.values()) {
            if (player.getId().equals(((WhitelistedPlayer)entry).getUUID()))
                invitedBy = cache.getByUuid(((WhitelistedPlayer)entry).getInvitedBy());
            else if (player.getId().equals(((WhitelistedPlayer) entry).getInvitedBy()))
                invited.add(entry);
        }
        
        Entity entity = source.getEntity();
        boolean isPlayer = (entity instanceof PlayerEntity && player.getId().equals(entity.getUuid()));
        
        // Output as text
        Text out = null;
        if (invitedBy != null)
            out = new LiteralText("").append(new LiteralText(isPlayer ? "You" : player.getName()).formatted(Formatting.GRAY))
                .append(" " + ( isPlayer ? "were" : "was" ) + " invited to the server by ").formatted(Formatting.WHITE)
                .append(new LiteralText(invitedBy.getName()).formatted(claim.getFriendRank(invitedBy.getId()).getColor()));
        
        Text inv = new LiteralText("").append(new LiteralText(isPlayer ? "You" : player.getName()).formatted(Formatting.GRAY))
            .append(" invited the following players: ").formatted(Formatting.WHITE)
            .append(MessageUtils.listToTextComponent(invited, (entry) -> {
                ClaimRanks rank = claim.getFriendRank(((WhitelistedPlayer)entry).getUUID());
                return new LiteralText(((WhitelistedPlayer)entry).getName()).formatted(rank.getColor());
            }));
        
        if (out == null) out = inv;
        else out.append("\n").append(inv);
        
        source.sendFeedback(out, false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Convert from legacy database version
     */
    private static int convertFromLegacy(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (LegacyConverter.exists())
            return -1;
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return -1;
        
        Thread threadConverter = new Thread(() -> {
            try (LegacyConverter converter = LegacyConverter.create()) {
                
            }
        });
        threadConverter.setName("Sewing Converter");
        threadConverter.start();
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Notify players of chunk claim changes
     */
    public static void notifyChangedClaimed(final UUID chunkOwner) {
        CoreMod.PLAYER_LOCATIONS.entrySet().stream().filter((entry) -> chunkOwner.equals(entry.getValue())).forEach((entry) -> {
            ServerPlayerEntity notifyPlayer = entry.getKey();
            PlayerMovement movement = ((PlayerMovement) notifyPlayer.networkHandler);
            movement.showPlayerNewLocation(notifyPlayer, notifyPlayer.getServerWorld().getWorldChunk(notifyPlayer.getBlockPos()));
        });
    }
}
