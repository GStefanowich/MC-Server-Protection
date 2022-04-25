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
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.interfaces.ClaimsAccessor;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.LogicalWorld;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.PlayerMovement;
import net.TheElm.project.interfaces.VillagerTownie;
import net.TheElm.project.interfaces.WhitelistedPlayer;
import net.TheElm.project.objects.ticking.ChunkOwnerUpdate;
import net.TheElm.project.objects.ticking.ClaimCache;
import net.TheElm.project.protections.BlockRange;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.CommandUtils;
import net.TheElm.project.utilities.EffectUtils;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.WarpUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.TextUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.particle.ParticleTypes;
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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class ClaimCommand {
    
    private static final ExceptionTranslatableServerSide NOT_ENOUGH_MONEY = TranslatableServerSide.exception("town.found.poor", 1);
    private static final ExceptionTranslatableServerSide SELF_RANK_CHANGE = TranslatableServerSide.exception("friends.rank.self");
    public static final ExceptionTranslatableServerSide CHUNK_NOT_OWNED_BY_PLAYER = TranslatableServerSide.exception("claim.chunk.error.not_players");
    public static final ExceptionTranslatableServerSide CHUNK_ALREADY_OWNED = TranslatableServerSide.exception("claim.chunk.error.claimed");
    public static final ExceptionTranslatableServerSide CHUNK_NOT_OWNED = TranslatableServerSide.exception("claim.chunk.error.not_claimed");
    private static final ExceptionTranslatableServerSide CHUNK_RADIUS_OWNED = TranslatableServerSide.exception("claim.chunk.error.radius_owned", 1 );
    private static final SimpleCommandExceptionType WHITELIST_FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.whitelist.add.failed"));
    private static final ExceptionTranslatableServerSide TOWN_INVITE_RANK = TranslatableServerSide.exception("town.invite.rank");
    private static final ExceptionTranslatableServerSide TOWN_INVITE_FAIL = TranslatableServerSide.exception("town.invite.fail");
    private static final ExceptionTranslatableServerSide TOWN_INVITE_MISSING = TranslatableServerSide.exception("town.invite.missing");
    private static final SimpleCommandExceptionType TOWN_NOT_EXISTS = new SimpleCommandExceptionType(new LiteralText("That town does not exist."));
    private static final ExceptionTranslatableServerSide PLAYER_NOT_FRIENDS = TranslatableServerSide.exception("friends.not_friends");
    private static final ExceptionTranslatableServerSide PLAYER_DIFFERENT_WORLD = TranslatableServerSide.exception("player.target.different_world");
    
    private ClaimCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        if (!SewConfig.get(SewConfig.DO_CLAIMS))
            return;
        
        /*
         * Admin Force commands
         */
        ServerCore.register(dispatcher, "Chunk", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.STOP).or(Permissions.ADMIN_CLAIMS).or(Permissions.ADMIN_CLAIM_TOWNS))
            .then(CommandManager.literal("set")
                .then(CommandManager.literal("player")
                    .requires(CommandPredicate.opLevel(OpLevels.STOP).or(Permissions.ADMIN_CLAIMS))
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(ClaimCommand::rawSetChunkPlayer)
                    )
                )
                .then(CommandManager.literal("town")
                    .requires(CommandPredicate.opLevel(OpLevels.STOP).or(Permissions.ADMIN_CLAIM_TOWNS))
                    .executes(ClaimCommand::rawSetChunkTown)
                )
            )
        );
        
        /*
         * Claim Command
         */
        LiteralCommandNode<ServerCommandSource> claim = ServerCore.register(dispatcher, "claim", builder -> builder
            // Claim a chunk radius
            .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 4))
                .executes(ClaimCommand::claimChunkSelfRadius)
            )
            // Claim a region
            .then(CommandManager.literal("region")
                .requires(CommandPredicate.opLevel(SewConfig.CLAIM_OP_LEVEL_SPAWN).or(SewConfig.CLAIM_OP_LEVEL_OTHER))
                .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                            .requires(CommandPredicate.opLevel(SewConfig.CLAIM_OP_LEVEL_OTHER))
                            .executes(ClaimCommand::claimRegionFor)
                        )
                        .then(CommandManager.literal("spawn")
                            .requires(CommandPredicate.opLevel(SewConfig.CLAIM_OP_LEVEL_SPAWN))
                            .executes(ClaimCommand::claimSpawnRegionAt)
                        )
                    )
                )
            )
            // Claim chunk for another player
            .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                .suggests(CommandUtils::getAllPlayerNames)
                .requires(CommandPredicate.opLevel(SewConfig.CLAIM_OP_LEVEL_OTHER))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 4))
                    .executes(ClaimCommand::claimChunkOtherRadius)
                )
                .executes(ClaimCommand::claimChunkOther)
            )
            // Claim chunk for the spawn
            .then(CommandManager.literal("spawn")
                .requires(CommandPredicate.opLevel(SewConfig.CLAIM_OP_LEVEL_SPAWN))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 4))
                    .executes(ClaimCommand::claimChunkSpawnRadius)
                )
                .executes(ClaimCommand::claimChunkSpawn)
            )
            // Claim chunk for players town
            .then(CommandManager.literal("town")
                .requires(CommandUtils::playerIsInTown)
                .executes(ClaimCommand::claimChunkTown)
            )
            // Claim a chunk for yourself
            .executes(ClaimCommand::claimChunkSelf)
        );
        
        /*
         * Unclaim Command
         */
        LiteralCommandNode<ServerCommandSource> unclaim = ServerCore.register(dispatcher, "unclaim", builder -> builder
            // Unclaim all chunks
            .then(CommandManager.literal("all")
                .executes(ClaimCommand::unclaimAll)
            )
            // Unclaim a region
            .then(CommandManager.literal("region")
                .requires(CommandPredicate.opLevel(SewConfig.CLAIM_OP_LEVEL_OTHER))
                .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                        .executes(ClaimCommand::unclaimRegionAt)
                    )
                )
            )
            // Force remove a claim
            .then(CommandManager.literal("force")
                .requires(CommandPredicate.opLevel(SewConfig.CLAIM_OP_LEVEL_OTHER))
                .executes(ClaimCommand::unclaimChunkOther)
            )
            // Unclaim current chunk
            .executes(ClaimCommand::unclaimChunkSelf)
        );
        
        /*
         * Friends Command
         */
        LiteralCommandNode<ServerCommandSource> friends = ServerCore.register(dispatcher, "friends", builder -> builder
            // Whitelist a friend
            .then(CommandManager.literal("whitelist")
                .requires(CommandPredicate.isEnabled(SewConfig.FRIEND_WHITELIST))
                .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .suggests((context, builder2) -> {
                        PlayerManager manager = context.getSource().getServer().getPlayerManager();
                        return CommandSource.suggestMatching(manager.getPlayerList().stream()
                            .filter(( player ) -> !manager.getWhitelist().isAllowed(player.getGameProfile()))
                            .map(( player ) -> player.getGameProfile().getName()), builder2);
                    })
                    .executes(ClaimCommand::inviteFriend)
                )
                .executes(ClaimCommand::invitedListSelf)
            )
            // Locate friends using pathing
            .then(CommandManager.literal("locate")
                .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getFriendPlayerNames)
                    .then(CommandManager.argument("location", StringArgumentType.string())
                        .suggests(ClaimCommand::playerHomeNamesOfFriend)
                        .executes(ClaimCommand::findFriendWarp)
                    )
                    .executes(ClaimCommand::findFriend)
                )
                .executes(ClaimCommand::stopFindingPos)
            )
            // Get Whitelisted friends
            .then(CommandManager.literal("get")
                .requires(CommandPredicate.opLevel(OpLevels.KICK_BAN_OP))
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .executes(ClaimCommand::invitedListOther)
                )
            )
            // Set a friends rank
            .then(CommandManager.literal("set")
                .then(CommandManager.argument("rank", StringArgumentType.word())
                    .suggests(EnumArgumentType.enumerate(ClaimRanks.class, ClaimRanks::rankSetterDescription))
                    .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
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
            // List all friends
            .executes(ClaimCommand::listFriends)
        );
        
        /*
         * Register the town command
         */
        LiteralCommandNode<ServerCommandSource> towns = ServerCore.register(dispatcher, "town", builder -> builder
            .then(CommandManager.literal("new")
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
                    .suggests(CommandUtils::getAllPlayerNames)
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
                .requires(CommandPredicate.cast(ClaimCommand::sourceInTown).and(ClaimCommand::sourceNotMayor))
                .executes(ClaimCommand::playerPartsTown)
            )
            .then(CommandManager.literal("set")
                .requires(CommandPredicate.opLevel(OpLevels.STOP).or(Permissions.ADMIN_CLAIM_TOWNS))
                .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .then(CommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(CommandUtils::getAllTowns)
                        .executes(ClaimCommand::adminSetPlayerTown)
                    )
                )
            )
            .then(CommandManager.literal("villagers")
                .requires(CommandPredicate.opLevel(OpLevels.STOP).or(Permissions.ADMIN_CLAIM_TOWNS))
                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                    .then(CommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(CommandUtils::getAllTowns)
                        .executes(ClaimCommand::adminSetEntityTown)
                    )
                )
            )
        );
        
        /*
         * Register the main command object
         */
        
        // The main command
        LiteralCommandNode<ServerCommandSource> protection = ServerCore.register(dispatcher, "protection", builder -> builder
            // Claim a chunk
            .then(claim)
            
            // Unclaim a chunk
            .then(unclaim)
            
            // Towns
            .then(towns)
            
            // Update claim permissions
            .then(CommandManager.literal("permissions")
                .then(CommandManager.argument("permission", StringArgumentType.word())
                    .suggests(EnumArgumentType.enumerate(ClaimPermissions.class, ClaimPermissions::getDescription))
                    .then(CommandManager.argument("rank", StringArgumentType.word())
                        .suggests(EnumArgumentType.enumerate(ClaimRanks.class, ClaimRanks::rankNormalDescription))
                        .executes(ClaimCommand::updateSetting)
                    )
                )
                .then(CommandManager.literal("*")
                    .then(CommandManager.argument( "rank", StringArgumentType.word())
                        .suggests(EnumArgumentType.enumerate(ClaimRanks.class, ClaimRanks::rankNormalDescription))
                        .executes(ClaimCommand::updateSettings)
                    )
                )
            )
            
            // Update friends
            .then(friends)
            
            // Chunk settings
            .then(CommandManager.literal("settings")
                .then(CommandManager.argument("setting", StringArgumentType.word())
                    .suggests(EnumArgumentType.enumerate(ClaimSettings.class, ClaimSettings::getDescription))
                    .then(CommandManager.argument("bool", BoolArgumentType.bool())
                        .executes(ClaimCommand::updateBoolean)
                    )
                )
            )
        );
    }
    
    /*
     * Set data for a chunk
     */
    
    private static int rawSetChunkPlayer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "target");
        for (GameProfile target : targets)
            return ClaimCommand.claimChunkAt(source, source.getWorld(), target.getId(), false, new BlockPos(source.getPosition()));
        
        return 0;
    }
    private static int rawSetChunkTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Claim a chunk
     */
    
    private static int claimChunkSelf(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the source of the command
        ServerCommandSource source = context.getSource();
        
        // Get the player running the command
        ServerPlayerEntity player = source.getPlayer();
        
        // Claim the chunk for own player
        return ClaimCommand.claimChunk(
            source,
            player.getUuid()
        );
    }
    private static int claimChunkSelfRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return ClaimCommand.claimChunkRadius(
            context, null
        );
    }
    private static int claimChunkTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        
        // Attempt to claim the chunk
        WorldChunk chunk = world.getWorldChunk(new BlockPos(source.getPosition()));
        ServerPlayerEntity player = source.getPlayer();
        if (!player.getUuid().equals(((IClaimedChunk) chunk).getOwnerId()))
            ClaimCommand.claimChunkSelf(context);
        
        // Update owner of the town
        ClaimantTown town = ((PlayerData) player).getClaim().getTown();
        if (town != null) {
            if ((((IClaimedChunk) chunk).getTownId() != null))
                throw ClaimCommand.CHUNK_ALREADY_OWNED.create(player);
            ((IClaimedChunk) chunk).updateTownOwner(town.getId());
        }
        
        // Notify the players in claimed chunks
        ClaimCommand.notifyChangedClaimed(player.getUuid());
        
        return Command.SINGLE_SUCCESS;
    }
    private static int claimChunkTownRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return 0;
    }
    private static int claimChunkOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the target player
        GameProfile targetPlayer = GameProfileArgumentType.getProfileArgument(context, "target").stream()
            .findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Claim the chunk for other player
        return ClaimCommand.claimChunk(
            context.getSource(),
            targetPlayer.getId()
        );
    }
    private static int claimChunkOtherRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, "target");
        GameProfile targetPlayer = gameProfiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        return ClaimCommand.claimChunkRadius(
            context, targetPlayer.getId()
        );
    }
    private static int claimChunkSpawn(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Claim the chunk for spawn
        return ClaimCommand.claimChunk(
            context.getSource(),
            CoreMod.SPAWN_ID
        );
    }
    private static int claimChunkSpawnRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return ClaimCommand.claimChunkRadius(
            context, CoreMod.SPAWN_ID
        );
    }
    private static int claimChunk(@NotNull final ServerCommandSource source) throws CommandSyntaxException {
        return ClaimCommand.claimChunk(source, null);
    }
    private static int claimChunk(@NotNull final ServerCommandSource source, @Nullable UUID chunkFor) throws CommandSyntaxException {
        // Claiming chunks for self player
        if (chunkFor == null) {
            ServerPlayerEntity player = source.getPlayer();
            chunkFor = player.getUuid();
        }
        
        return ClaimCommand.claimChunkAt(
            source,
            source.getWorld(),
            chunkFor,
            true,
            new BlockPos(source.getPosition())
        );
    }
    private static int claimChunkRadius(@NotNull final CommandContext<ServerCommandSource> context, @Nullable UUID chunkFor) throws CommandSyntaxException {
        // Get the player running the command
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = null;
        World world = source.getWorld();
        
        // Claiming chunks for self player
        if (chunkFor == null) {
            player = source.getPlayer();
            chunkFor = player.getUuid();
        }
        
        // Get the players positioning
        BlockPos blockPos = new BlockPos(source.getPosition());
        List<BlockPos> chunksToClaim = new ArrayList<>();
        
        // Check the radius that the player wants to claim
        final int radius = IntegerArgumentType.getInteger(context, "radius");
        IClaimedChunk[] claimedChunks = IClaimedChunk.getOwnedAround(source.getWorld(), blockPos, radius);
        for (IClaimedChunk claimedChunk : claimedChunks) {
            if (!chunkFor.equals(claimedChunk.getOwnerId(blockPos)))
                throw CHUNK_RADIUS_OWNED.create(source, claimedChunk.getOwnerName(player, blockPos));
        }
        
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        
        // For the X axis
        for (int x = chunkX - radius; x <= chunkX + radius; x++) {
            // For the Z axis
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                // Create the chunk position
                chunksToClaim.add(new BlockPos(x << 4, 0, z << 4));
            }
        }
        
        // Claim all chunks
        return ClaimCommand.claimChunkAt(
            source,
            world,
            chunkFor,
            true,
            chunksToClaim
        );
    }
    
    public static int claimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull BlockPos... positions) {
        return ClaimCommand.claimChunkAt(source, world, chunkFor, verify, Arrays.asList(positions));
    }
    public static int claimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull Collection<? extends BlockPos> positions) {
        ClaimCache claimCache = ((ClaimsAccessor)source.getServer())
            .getClaimManager();
        ((LogicalWorld)world).addTickableEvent(ChunkOwnerUpdate.forPlayer(
            claimCache,
            source,
            chunkFor,
            ChunkOwnerUpdate.Mode.CLAIM,
            positions
        ).setVerify(verify));
        return Command.SINGLE_SUCCESS;
    }
    
    private static int unclaimChunkSelf(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        return ClaimCommand.unclaimChunkAt(
            source,
            source.getWorld(),
            player.getUuid(),
            true,
            new BlockPos(source.getPosition())
        );
    }
    private static int unclaimChunkTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        
        Chunk chunk = world.getChunk(new BlockPos(source.getPosition()));
        if (((IClaimedChunk) chunk).getTownId() == null)
            throw CHUNK_NOT_OWNED.create( player );
        
        // Town SHOULDN'T be null here
        if (claimant.getTownId() == null) return -1;
        
        if (!claimant.getTownId().equals(((IClaimedChunk)chunk).getTownId()))
            throw CHUNK_NOT_OWNED_BY_PLAYER.create( player );
        
        // Change the town owner
        ((IClaimedChunk) chunk).updateTownOwner( null );
        
        // Notify the players in claimed chunks
        ClaimCommand.notifyChangedClaimed(player.getUuid());
        
        return Command.SINGLE_SUCCESS;
    }
    private static int unclaimChunkOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        return ClaimCommand.unclaimChunkAt(
            source,
            source.getWorld(),
            player.getUuid(),
            false,
            new BlockPos(source.getPosition())
        );
    }
    private static int unclaimAll(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        final ServerWorld world = source.getWorld();
        
        // Update total count
        Claimant claimed;
        if ((claimed = ((PlayerData) player).getClaim()) != null) {
            ClaimCommand.unclaimChunkAt(
                source,
                world,
                player.getUuid(),
                true,
                // Unclaim EVERY chunk
                claimed.getChunks().stream()
                    .filter(claimTag -> Objects.equals(claimTag.getDimension(), world.getRegistryKey()))
                    .map(claimTag -> new BlockPos(claimTag.getLowerX(), 0, claimTag.getLowerZ()))
                    .collect(Collectors.toList())
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    public static int unclaimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull BlockPos... positions) {
        return ClaimCommand.unclaimChunkAt(source, world, chunkFor, verify, Arrays.asList(positions));
    }
    public static int unclaimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull Collection<? extends BlockPos> positions) {
        ClaimCache claimCache = ((ClaimsAccessor)source.getServer())
            .getClaimManager();
        ((LogicalWorld)world).addTickableEvent(ChunkOwnerUpdate.forPlayer(
            claimCache,
            source,
            chunkFor,
            ChunkOwnerUpdate.Mode.UNCLAIM,
            positions
        ).setVerify(verify));
        return Command.SINGLE_SUCCESS;
    }
    
    private static int claimSpawnRegionAt(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return ClaimCommand.claimRegionAt(context, CoreMod.SPAWN_ID);
    }
    private static int claimRegionFor(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get information about the target
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "target");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        return ClaimCommand.claimRegionAt(context, target.getId());
    }
    private static int claimRegionAt(@NotNull CommandContext<ServerCommandSource> context, @NotNull UUID target) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        BlockRange region = BlockRange.between(
            BlockPosArgumentType.getBlockPos(context, "from"),
            BlockPosArgumentType.getBlockPos(context, "to")
        );
        
        // Claim the defined slices
        ChunkUtils.claimSlices(source.getWorld(), target, region);
        source.sendFeedback(new LiteralText("Claimed ")
            .append(region.formattedVolume())
            .append(" blocks."), false);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int unclaimRegionAt(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        BlockRange region = BlockRange.between(
            BlockPosArgumentType.getBlockPos(context, "from"),
            BlockPosArgumentType.getBlockPos(context, "to")
        );
        
        // Unclaim the defined slices
        ChunkUtils.unclaimSlices(source.getWorld(), region);
        source.sendFeedback(new LiteralText("Unclaimed ")
            .append(region.formattedVolume())
            .append(" blocks."), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Town Options
     */
    
    private static boolean sourceIsMayor(@NotNull final ServerCommandSource source) {
        try {
            return ClaimCommand.sourceIsMayor( source.getPlayer() );
        } catch (CommandSyntaxException e) {
            CoreMod.logError( e );
        }
        return false;
    }
    public static boolean sourceIsMayor(@NotNull final ServerPlayerEntity player) {
        ClaimantPlayer claim = ((PlayerData) player).getClaim();
        ClaimantTown town;
        
        // Check if owner
        if ((claim != null) && ((town = claim.getTown()) != null))
            return player.getUuid().equals(town.getOwnerId());
        
        return false;
    }
    private static boolean sourceNotMayor(@NotNull final ServerCommandSource source) {
        return !ClaimCommand.sourceIsMayor( source );
    }
    public static boolean sourceInTown(@NotNull final ServerCommandSource source) {
        try {
            return ClaimCommand.sourceInTown( source.getPlayer() );
        } catch (CommandSyntaxException e) {
            CoreMod.logError( e );
        }
        return false;
    }
    private static boolean sourceInTown(@NotNull final ServerPlayerEntity player) {
        ClaimantPlayer claim = ((PlayerData) player).getClaim();
        return ((claim != null) && claim.getTown() != null);
    }
    public static boolean sourceNotInTown(@NotNull final ServerCommandSource source) {
        return !ClaimCommand.sourceInTown( source );
    }
    
    private static int townFound(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ClaimCache claimCache = ((ClaimsAccessor)server).getClaimManager();
        ServerPlayerEntity founder = source.getPlayer();
        
        // Charge the player money
        try {
            if ((SewConfig.get(SewConfig.TOWN_FOUND_COST) > 0) && (!MoneyUtils.takePlayerMoney(founder, SewConfig.get(SewConfig.TOWN_FOUND_COST))))
                throw NOT_ENOUGH_MONEY.create(founder, "$" + FormattingUtils.format(SewConfig.get(SewConfig.TOWN_FOUND_COST)));
        } catch (NotEnoughMoneyException e) {
            throw NOT_ENOUGH_MONEY.create(founder, "$" + FormattingUtils.format(SewConfig.get(SewConfig.TOWN_FOUND_COST)));
        }
        try {
            // Get town information
            MutableText townName = new LiteralText(StringArgumentType.getString(context, "name"));
            ClaimantTown town = claimCache.makeTownClaim(founder, townName);
            
            // Tell all players of the founding
            MessageUtils.sendToAll("town.found",
                ((MutableText)founder.getName()).formatted(Formatting.AQUA),
                town.getName().formatted(Formatting.AQUA)
            );
            
            // Resend the command tree
            server.getPlayerManager().sendCommandTree( founder );
        } catch (Throwable t) {
            CoreMod.logError( t );
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int townDisband(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity founder = source.getPlayer();
        
        ClaimantPlayer claimantPlayer = ((PlayerData) founder).getClaim();
        ClaimantTown claimantTown;
        
        // Check that player is in a town (Should ALWAYS be TRUE here)
        if ((claimantPlayer == null) || ((claimantTown = claimantPlayer.getTown() ) == null))
            return -1;
        
        // Delete the town
        claimantTown.delete(server.getPlayerManager());
        
        // Tell all other players
        MessageUtils.sendToAll( "town.disband",
            ((MutableText)founder.getName()).formatted(Formatting.AQUA),
            claimantTown.getName().formatted(Formatting.AQUA)
        );
        
        // Resend the command tree
        server.getPlayerManager().sendCommandTree( founder );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int townInvite(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        ClaimantTown town = claimant.getTown();
        if (town == null) return -1; // Towns SHOULD always be set when reaching here
        
        if (town.getFriendRank(player.getUuid()) != ClaimRanks.OWNER)
            throw ClaimCommand.TOWN_INVITE_RANK.create(player);
        
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        ClaimantPlayer targetClaimant = ((PlayerData) target).getClaim();
        
        if ( !targetClaimant.inviteTown(town) )
            throw ClaimCommand.TOWN_INVITE_FAIL.create(player);
        
        // Send the notice to the inviter
        TranslatableServerSide.send(player, "town.invite.sent", target.getDisplayName());
        
        // Send the notice to the invitee
        TranslatableServerSide.send(target, "town.invite.receive", town.getName(), player.getDisplayName());
        
        return Command.SINGLE_SUCCESS;
    }
    private static int townGiveChunk(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "target" );
        GameProfile targetPlayer = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        ServerWorld world = source.getWorld();
        IClaimedChunk claimedChunk = (IClaimedChunk) world.getChunk(new BlockPos(source.getPosition()));
        
        ServerPlayerEntity player = source.getPlayer();
        ClaimantTown town;
        if ((claimedChunk.getOwnerId() == null) || ((town = claimedChunk.getTown()) == null) || (!player.getUuid().equals(claimedChunk.getOwnerId())) || (!player.getUuid().equals( town.getOwnerId() )))
            throw ClaimCommand.CHUNK_NOT_OWNED_BY_PLAYER.create(player);
        
        claimedChunk.updatePlayerOwner( targetPlayer.getId() );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int playerJoinsTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        
        String townName = StringArgumentType.getString(context, "town");
        ClaimantTown town = claimant.getTownInvite(townName);
        if (town == null)
            throw ClaimCommand.TOWN_INVITE_MISSING.create(player);
        
        /* 
         * Update players town
         */
        town.updateFriend(player, ClaimRanks.ALLY);
        //claimant.setTown( town );
        
        // Tell the player
        TranslatableServerSide.send(player, "town.invite.join", town.getName());
        
        // Refresh the command tree
        server.getPlayerManager().sendCommandTree( player );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int playerPartsTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
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
    private static int adminSetPlayerTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context,"target");
        GameProfile profile = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the town
        String townName = StringArgumentType.getString(context, "town");
        
        ClaimCache claimCache = ((ClaimsAccessor) server).getClaimManager();
        ClaimantTown town = claimCache.getTownClaim(townName);
        
        if (town == null)
            throw TOWN_INVITE_MISSING.create(source.getPlayer());
        
        // Update the rank of the player for the town
        town.updateFriend(profile.getId(), ClaimRanks.ALLY);
        claimCache.getPlayerClaim(profile)
            .setTown(town);
        
        // Refresh the command tree
        PlayerManager players = server.getPlayerManager();
        ServerPlayerEntity player = players
            .getPlayer(profile.getId());
        if (player != null)
            players.sendCommandTree(player);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int adminSetEntityTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "entities");
        String townName = StringArgumentType.getString(context, "town");
        ClaimantTown town = ((ClaimsAccessor)source.getServer()).getClaimManager()
            .getTownClaim(townName);
        
        if (town == null)
            throw TOWN_NOT_EXISTS.create();
        
        int added = 0;
        for (Entity entity : entities) {
            if (!(entity instanceof VillagerEntity))
                continue;
            if (((VillagerTownie)entity).setTown(town))
                added++;
        }
        
        Text amount = new LiteralText(FormattingUtils.format(added)).formatted(Formatting.AQUA);
        source.sendFeedback(new LiteralText("Added ")
            .append(amount)
            .append(" villagers to ")
            .append(town.getName())
            .append("."), false);
        if (added > 0) {
            town.send(source.getServer(), TextUtils.literal()
                .append(amount)
                .append(" villagers have been added to your town."),
                MessageType.SYSTEM,
                CoreMod.SPAWN_ID
            );
        }
        
        return added;
    }
    private static CompletableFuture<Suggestions> listTownInvites(@NotNull CommandContext<ServerCommandSource> context, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerData) player).getClaim();
        
        // Suggestion set
        Set<String> set = new HashSet<>();
        for (ClaimantTown town : claimant.getTownInvites())
            set.add(town.getName().getString());
        
        // Return the output
        return CommandSource.suggestMatching(set.stream(), suggestionsBuilder);
    }
    
    /*
     * Update Chunk settings
     */
    
    private static int updateSetting(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimPermissions permission = EnumArgumentType.getEnum(ClaimPermissions.class, StringArgumentType.getString(context, "permission"));
        ClaimRanks rank = EnumArgumentType.getEnum(ClaimRanks.class, StringArgumentType.getString(context, "rank"));
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Update the runtime
        ClaimCommand.updateSetting(player, permission, rank);
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private static int updateSettings(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimRanks rank = EnumArgumentType.getEnum(ClaimRanks.class, StringArgumentType.getString(context, "rank"));
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        for ( ClaimPermissions permission : ClaimPermissions.values() )
            ClaimCommand.updateSetting(player, permission, rank);
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private static void updateSetting(@NotNull ServerPlayerEntity player, ClaimPermissions permission, ClaimRanks rank) throws CommandSyntaxException {
        // Update the runtime
        ((PlayerData) player).getClaim()
            .updatePermission(permission, rank);
        
        // Notify the player
        player.sendMessage(new LiteralText("Interacting with ").formatted(Formatting.WHITE)
                .append(new LiteralText(CasingUtils.sentence(permission.name())).formatted(Formatting.AQUA))
                .append(new LiteralText(" is now limited to ").formatted(Formatting.WHITE))
                .append(new LiteralText(CasingUtils.sentence(rank.name())).formatted(Formatting.AQUA))
                .append(new LiteralText(".").formatted(Formatting.WHITE)),
            MessageType.SYSTEM,
            CoreMod.SPAWN_ID
        );
    }
    private static int updateBoolean(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimSettings setting = EnumArgumentType.getEnum(ClaimSettings.class, StringArgumentType.getString(context, "setting"));
        boolean enabled = BoolArgumentType.getBool(context, "bool");
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Update the runtime
        ((PlayerData) player).getClaim()
            .updateSetting(setting, enabled);
        
        // Notify other players
        if (ClaimSettings.PLAYER_COMBAT.equals(setting)) {
            ClaimCommand.notifyChangedClaimed( player.getUuid() );
        }
        
        // Notify the player
        player.sendMessage(new LiteralText(CasingUtils.words(setting.name().replace("_", " "))).formatted(Formatting.AQUA)
            .append(new LiteralText(" is now ").formatted(Formatting.WHITE))
            .append(new LiteralText( enabled ? "Enabled" : "Disabled" ).formatted(setting.getAttributeColor( enabled )))
            .append(new LiteralText(" in your claimed area.").formatted(Formatting.WHITE)),
            MessageType.SYSTEM,
            CoreMod.SPAWN_ID
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Adding/Removing users from friends
     */
    
    private static int addRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the targeted rank
        ClaimRanks rank = EnumArgumentType.getEnum( ClaimRanks.class, StringArgumentType.getString( context,"rank" ) );
        
        ServerCommandSource source = context.getSource();
        
        // Get the player
        ServerPlayerEntity player = source.getPlayer();
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "friend" );
        GameProfile friend = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Player tries changing their own rank
        if ( player.getUuid().equals( friend.getId() ) )
            throw SELF_RANK_CHANGE.create(player);
        
        // Update our runtime
        ((PlayerData) player).getClaim()
            .updateFriend(friend.getId(), rank);
        
        // Attempting to update the friend
        player.sendMessage(new LiteralText("Player ").formatted(Formatting.WHITE)
            .append(new LiteralText(friend.getName()).formatted(Formatting.DARK_PURPLE))
            .append(new LiteralText(" is now an "))
            .append(new LiteralText(CasingUtils.sentence(rank.name())).formatted(Formatting.AQUA))
            .append(new LiteralText(".").formatted(Formatting.WHITE)),
            MessageType.SYSTEM,
            CoreMod.SPAWN_ID
        );
        
        // Play a sound to the player
        player.playSound(SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.MASTER, 0.5f, 1f );
        
        // Find the entity of the friend
        ServerPlayerEntity friendEntity = ServerCore.getPlayer(source.getServer(), friend.getId());
        if ( friendEntity != null ) {
            // Notify the friend
            friendEntity.sendMessage(new LiteralText("Player ").formatted(Formatting.WHITE)
                .append(((MutableText)player.getName()).formatted(Formatting.DARK_PURPLE))
                .append(new LiteralText(" has added you as an ").formatted(Formatting.WHITE))
                .append(new LiteralText(rank.name()).formatted(Formatting.AQUA))
                .append(new LiteralText(".").formatted(Formatting.WHITE)), false
            );
            
            friendEntity.playSound(SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.MASTER, 0.5f, 1f);
        }
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private static int remRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, "friend");
        GameProfile friend = gameProfiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Player tries changing their own rank
        if (player.getUuid().equals(friend.getId()))
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
                .append( new LiteralText(friend.getName()).formatted(Formatting.DARK_PURPLE) )
                .append( new LiteralText(" removed.")),
                MessageType.SYSTEM,
                CoreMod.SPAWN_ID
            );
            
            // Play sound to player
            player.playSound(SoundEvents.ENTITY_VILLAGER_DEATH, SoundCategory.MASTER, 0.5f, 1f );
            
            // Find the entity of the friend
            ServerPlayerEntity friendEntity = context.getSource().getServer().getPlayerManager().getPlayer( friend.getId() );
            
            // If the friend is online
            if ( friendEntity != null ) {
                // Notify the friend
                friendEntity.sendMessage(new LiteralText("Player ").formatted(Formatting.WHITE)
                    .append(((MutableText)player.getName()).formatted(Formatting.DARK_PURPLE))
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
    private static int listFriends(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    private static int findFriend(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        MinecraftServer server = source.getServer();
        
        // Get the GameProfile of the Target
        GameProfile friend = GameProfileArgumentType.getProfileArgument(context, "friend").stream()
            .findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the Claims handler
        ClaimCache claimCache = ((ClaimsAccessor)source.getServer())
            .getClaimManager();
        
        // Check if the command player and the target are friends
        ClaimantPlayer friendData = claimCache.getPlayerClaim(friend);
        if (!friendData.isFriend(player.getUuid()))
            throw ClaimCommand.PLAYER_NOT_FRIENDS.create(player);
        
        // Try to get the entity to target
        Entity target = Optional.ofNullable(server.getPlayerManager()
            .getPlayer(friend.getId())).orElseThrow(EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION::create);
        if (player.world != target.world || target.isInvisible())
            throw ClaimCommand.PLAYER_DIFFERENT_WORLD.create(player);
        
        return ClaimCommand.pathPlayerToTarget(player, target);
    }
    private static int findFriendWarp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get the GameProfile of the Target
        GameProfile friend = GameProfileArgumentType.getProfileArgument(context, "friend").stream()
            .findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the name of the warp to target to
        String name = StringArgumentType.getString(context, "location");
        
        // Get the Claims handler
        ClaimCache claimCache = ((ClaimsAccessor)source.getServer())
            .getClaimManager();
        
        // Check if the command player and the target are friends
        ClaimantPlayer friendData = claimCache.getPlayerClaim(friend);
        if (!friendData.isFriend(player.getUuid()))
            throw ClaimCommand.PLAYER_NOT_FRIENDS.create(player);
        
        // Try to get the warp
        WarpUtils.Warp warp = WarpUtils.getWarp(friend.getId(), name);
        if (warp == null)
            throw TeleportsCommand.TARGET_NO_WARP.create(player);
        
        // Check that the player is in the world for the warp
        if (!world.getRegistryKey().equals(warp.world))
            throw ClaimCommand.PLAYER_DIFFERENT_WORLD.create(player);
        
        // Locate to the Targets Waystone
        return ClaimCommand.pathPlayerToTarget(player, warp.warpPos);
    }
    private static int stopFindingPos(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        return ClaimCommand.pathPlayerToTarget(player, player);
    }
    private static int pathPlayerToTarget(@NotNull ServerPlayerEntity player, @NotNull Entity target) {
        Path navigator = ((PlayerData)player).findPathTo(target, 3);
        if (navigator != null)
            EffectUtils.summonBreadcrumbs(ParticleTypes.FALLING_OBSIDIAN_TEAR, player, navigator);
        else CoreMod.logInfo("Could not find path.");
        return navigator == null ? 0 : 1;
    }
    private static int pathPlayerToTarget(@NotNull ServerPlayerEntity player, @NotNull BlockPos position) {
        Path navigator = ((PlayerData)player).findPathTo(position, 3);
        if (navigator != null)
            EffectUtils.summonBreadcrumbs(ParticleTypes.FALLING_OBSIDIAN_TEAR, player, navigator);
        else CoreMod.logInfo("Could not find path.");
        return navigator == null ? 0 : 1;
    }
    
    private static @NotNull CompletableFuture<Suggestions> playerHomeNamesOfFriend(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the uuid of the executor
        Entity entity = source.getEntity();
        UUID untrusted = entity instanceof ServerPlayerEntity ? entity.getUuid() : null;
        
        // Get the matching player being looked up
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "friend");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Build the suggestions
        return WarpUtils.buildSuggestions(source.getServer(), untrusted, target.getId(), builder);
    }
    
    /*
     * Let players add friends to the whitelist
     */
    
    private static int inviteFriend(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Whitelist whitelist = source.getServer().getPlayerManager().getWhitelist();
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
    private static int invitedListSelf(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get command context information
        ServerCommandSource source = context.getSource();
        
        return ClaimCommand.invitedList(
            source,
            source.getPlayer().getGameProfile()
        );
    }
    private static int invitedListOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
    private static int invitedList(@NotNull ServerCommandSource source, @NotNull GameProfile player) throws CommandSyntaxException {
        // Get information about the server
        MinecraftServer server = source.getServer();
        ClaimCache claimCache = ((ClaimsAccessor)server).getClaimManager();
        UserCache cache = server.getUserCache();
        Whitelist whitelist = server.getPlayerManager().getWhitelist();
        
        // Store information about invites
        GameProfile invitedBy = null;
        List<WhitelistEntry> invited = new ArrayList<>();
        
        ClaimantPlayer claim = claimCache.getPlayerClaim(player.getId());
        
        // Loop the whitelist
        for (WhitelistEntry entry : whitelist.values()) {
            if (entry == null)
                continue;
            UUID invitedById = ((WhitelistedPlayer)entry).getInvitedBy();
            if (invitedById != null) {
                if (player.getId().equals(((WhitelistedPlayer)entry).getUUID()))
                    invitedBy = cache.getByUuid(invitedById).orElse(null);
                else if (player.getId().equals(invitedById))
                    invited.add(entry);
            }
        }
        
        Entity entity = source.getEntity();
        boolean isPlayer = (entity instanceof PlayerEntity && player.getId().equals(entity.getUuid()));
        
        // Output as text
        MutableText out = null;
        if (invitedBy != null)
            out = new LiteralText("").append(new LiteralText(isPlayer ? "You" : player.getName()).formatted(Formatting.GRAY))
                .append(" " + ( isPlayer ? "were" : "was" ) + " invited to the server by ").formatted(Formatting.WHITE)
                .append(ClaimCommand.inviteeFormattedName(source, claim, invitedBy.getName(), invitedBy.getId()));
        
        MutableText inv = new LiteralText("").formatted(Formatting.WHITE)
            .append(new LiteralText(isPlayer ? "You" : player.getName()).formatted(Formatting.GRAY))
            .append(" invited the following players [")
            .append(MessageUtils.formatNumber(invited.size()))
            .append("]: ")
            .append(MessageUtils.listToTextComponent(invited, (entry) -> ClaimCommand.inviteeFormattedName(source, claim, ((WhitelistedPlayer)entry).getName(), ((WhitelistedPlayer)entry).getUUID())));
        
        if (out == null) out = inv;
        else out.append("\n").append(inv);
        
        source.sendFeedback(out, false);
        
        return Command.SINGLE_SUCCESS;
    }
    private static Text inviteeFormattedName(@NotNull ServerCommandSource source, @NotNull ClaimantPlayer claim, @NotNull String name, @NotNull UUID uuid) {
        return new LiteralText(name).styled((style) -> {
            if (source.hasPermissionLevel(OpLevels.CHEATING)) {
                ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friends get " + name);
                style = style.withClickEvent(click);
            }
            return style.withFormatting(claim.getFriendRank(uuid).getColor());
        });
    }
    
    /*
     * Notify players of chunk claim changes
     */
    
    public static void notifyChangedClaimed(final UUID chunkOwner) {
        CoreMod.PLAYER_LOCATIONS.entrySet().stream().filter((entry) -> chunkOwner.equals(entry.getValue())).forEach((entry) -> {
            ServerPlayerEntity notifyPlayer = entry.getKey();
            PlayerMovement movement = ((PlayerMovement) notifyPlayer.networkHandler);
            movement.showPlayerNewLocation(notifyPlayer, notifyPlayer.getWorld().getWorldChunk(notifyPlayer.getBlockPos()));
        });
    }
}
