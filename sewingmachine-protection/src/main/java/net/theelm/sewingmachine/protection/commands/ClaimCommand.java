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

package net.theelm.sewingmachine.protection.commands;

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
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.commands.TeleportsCommand;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.commands.arguments.EnumArgumentType;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.events.RegionUpdateCallback;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.exceptions.NotEnoughMoneyException;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.interfaces.VillagerTownie;
import net.theelm.sewingmachine.interfaces.WhitelistedPlayer;
import net.theelm.sewingmachine.objects.Value;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.theelm.sewingmachine.protection.objects.ticking.ChunkOwnerUpdate;
import net.theelm.sewingmachine.protection.claims.Claimant;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.protection.enums.ClaimRanks;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.objects.ServerClaimCache;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.ClaimPropertyUtils;
import net.theelm.sewingmachine.protection.utilities.CommandClaimUtils;
import net.theelm.sewingmachine.protections.BlockRange;
import net.theelm.sewingmachine.utilities.CasingUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.EffectUtils;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.theelm.sewingmachine.utilities.WarpUtils;
import net.theelm.sewingmachine.utilities.mod.SewServer;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

public final class ClaimCommand extends SewCommand {
    private static final ExceptionTranslatableServerSide NOT_ENOUGH_MONEY = TranslatableServerSide.exception("town.found.poor", 1);
    private static final ExceptionTranslatableServerSide SELF_RANK_CHANGE = TranslatableServerSide.exception("friends.rank.self");
    public static final ExceptionTranslatableServerSide CHUNK_NOT_OWNED_BY_PLAYER = TranslatableServerSide.exception("claim.chunk.error.not_players");
    public static final ExceptionTranslatableServerSide CHUNK_ALREADY_OWNED = TranslatableServerSide.exception("claim.chunk.error.claimed");
    public static final ExceptionTranslatableServerSide CHUNK_NOT_OWNED = TranslatableServerSide.exception("claim.chunk.error.not_claimed");
    private static final ExceptionTranslatableServerSide CHUNK_RADIUS_OWNED = TranslatableServerSide.exception("claim.chunk.error.radius_owned", 1 );
    private static final SimpleCommandExceptionType WHITELIST_FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.whitelist.add.failed"));
    private static final ExceptionTranslatableServerSide TOWN_INVITE_RANK = TranslatableServerSide.exception("town.invite.rank");
    private static final ExceptionTranslatableServerSide TOWN_INVITE_FAIL = TranslatableServerSide.exception("town.invite.fail");
    private static final ExceptionTranslatableServerSide TOWN_INVITE_MISSING = TranslatableServerSide.exception("town.invite.missing");
    private static final SimpleCommandExceptionType TOWN_NOT_EXISTS = new SimpleCommandExceptionType(Text.literal("That town does not exist."));
    private static final ExceptionTranslatableServerSide PLAYER_NOT_FRIENDS = TranslatableServerSide.exception("friends.not_friends");
    private static final ExceptionTranslatableServerSide PLAYER_DIFFERENT_WORLD = TranslatableServerSide.exception("player.target.different_world");
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        /*
         * Admin Force commands
         */
        CommandUtils.register(dispatcher, "Chunk", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.STOP).or(PermissionNodes.ADMIN_CLAIMS).or(PermissionNodes.ADMIN_CLAIM_TOWNS))
            .then(CommandManager.literal("set")
                .then(CommandManager.literal("player")
                    .requires(CommandPredicate.opLevel(OpLevels.STOP).or(PermissionNodes.ADMIN_CLAIMS))
                    .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(this::rawSetChunkPlayer)
                    )
                )
                .then(CommandManager.literal("town")
                    .requires(CommandPredicate.opLevel(OpLevels.STOP).or(PermissionNodes.ADMIN_CLAIM_TOWNS))
                    .executes(this::rawSetChunkTown)
                )
            )
        );
        
        /*
         * Claim Command
         */
        LiteralCommandNode<ServerCommandSource> claim = CommandUtils.register(dispatcher, "claim", builder -> builder
            // Claim a chunk radius
            .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 4))
                .executes(this::claimChunkSelfRadius)
            )
            // Claim a region
            .then(CommandManager.literal("region")
                .requires(CommandPredicate.opLevel(SewProtectionConfig.CLAIM_OP_LEVEL_SPAWN).or(SewProtectionConfig.CLAIM_OP_LEVEL_OTHER))
                .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                            .requires(CommandPredicate.opLevel(SewProtectionConfig.CLAIM_OP_LEVEL_OTHER))
                            .executes(this::claimRegionFor)
                        )
                        .then(CommandManager.literal("spawn")
                            .requires(CommandPredicate.opLevel(SewProtectionConfig.CLAIM_OP_LEVEL_SPAWN))
                            .executes(this::claimSpawnRegionAt)
                        )
                    )
                )
            )
            // Claim chunk for another player
            .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                .suggests(CommandUtils::getAllPlayerNames)
                .requires(CommandPredicate.opLevel(SewProtectionConfig.CLAIM_OP_LEVEL_OTHER))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 4))
                    .executes(this::claimChunkOtherRadius)
                )
                .executes(this::claimChunkOther)
            )
            // Claim chunk for the spawn
            .then(CommandManager.literal("spawn")
                .requires(CommandPredicate.opLevel(SewProtectionConfig.CLAIM_OP_LEVEL_SPAWN))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 4))
                    .executes(this::claimChunkSpawnRadius)
                )
                .executes(this::claimChunkSpawn)
            )
            // Claim chunk for players town
            .then(CommandManager.literal("town")
                .requires(CommandClaimUtils::playerIsInTown)
                .executes(this::claimChunkTown)
            )
            // Claim a chunk for yourself
            .executes(this::claimChunkSelf)
        );
        
        /*
         * Unclaim Command
         */
        LiteralCommandNode<ServerCommandSource> unclaim = CommandUtils.register(dispatcher, "unclaim", builder -> builder
            // Unclaim all chunks
            .then(CommandManager.literal("all")
                .executes(this::unclaimAll)
            )
            // Unclaim a region
            .then(CommandManager.literal("region")
                .requires(CommandPredicate.opLevel(SewProtectionConfig.CLAIM_OP_LEVEL_OTHER))
                .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                        .executes(this::unclaimRegionAt)
                    )
                )
            )
            // Force remove a claim
            .then(CommandManager.literal("force")
                .requires(CommandPredicate.opLevel(SewProtectionConfig.CLAIM_OP_LEVEL_OTHER))
                .executes(this::unclaimChunkOther)
            )
            // Unclaim current chunk
            .executes(this::unclaimChunkSelf)
        );
        
        /*
         * Friends Command
         */
        LiteralCommandNode<ServerCommandSource> friends = CommandUtils.register(dispatcher, "friends", builder -> builder
            // Whitelist a friend
            .then(CommandManager.literal("whitelist")
                .requires(CommandPredicate.isEnabled(SewCoreConfig.FRIEND_WHITELIST))
                .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .suggests((context, builder2) -> {
                        PlayerManager manager = context.getSource().getServer().getPlayerManager();
                        return CommandSource.suggestMatching(manager.getPlayerList().stream()
                            .filter(( player ) -> !manager.getWhitelist().isAllowed(player.getGameProfile()))
                            .map(( player ) -> player.getGameProfile().getName()), builder2);
                    })
                    .executes(this::inviteFriend)
                )
                .executes(this::invitedListSelf)
            )
            // Locate friends using pathing
            .then(CommandManager.literal("locate")
                .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .suggests(CommandClaimUtils::getFriendPlayerNames)
                    .then(CommandManager.argument("location", StringArgumentType.string())
                        .suggests(this::playerHomeNamesOfFriend)
                        .executes(this::findFriendWarp)
                    )
                    .executes(this::findFriend)
                )
                .executes(this::stopFindingPos)
            )
            // Get Whitelisted friends
            .then(CommandManager.literal("get")
                .requires(CommandPredicate.opLevel(OpLevels.KICK_BAN_OP))
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .executes(this::invitedListOther)
                )
            )
            // Set a friends rank
            .then(CommandManager.literal("set")
                .then(CommandManager.argument("rank", StringArgumentType.word())
                    .suggests(EnumArgumentType.enumerate(ClaimRanks.class, ClaimRanks::rankSetterDescription))
                    .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(this::addRank)
                    )
                )
            )
            // Remove a friends rank
            .then(CommandManager.literal("remove")
                .then(CommandManager.argument("friend", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .executes(this::remRank)
                )
            )
            // List all friends
            .executes(this::listFriends)
        );
        
        /*
         * Register the town command
         */
        LiteralCommandNode<ServerCommandSource> towns = CommandUtils.register(dispatcher, "town", builder -> builder
            .then(CommandManager.literal("new")
                .requires(ClaimCommand::sourceNotMayor)
                .then( CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(this::townFound)
                )
            )
            .then(CommandManager.literal("disband")
                .requires(ClaimCommand::sourceIsMayor)
                .executes(this::townDisband)
            )
            .then(CommandManager.literal("claim")
                .requires(ClaimCommand::sourceIsMayor)
                .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .executes(this::townGiveChunk)
                )
                .executes(this::claimChunkTown)
            )
            .then(CommandManager.literal("unclaim")
                .requires(ClaimCommand::sourceInTown)
                .executes(this::unclaimChunkTown)
            )
            .then(CommandManager.literal("invite")
                .requires(ClaimCommand::sourceIsMayor)
                .then( CommandManager.argument("target", EntityArgumentType.player())
                    .executes(this::townInvite)
                )
            )
            .then(CommandManager.literal("join")
                .requires(ClaimCommand::sourceNotInTown)
                .then( CommandManager.argument("town", StringArgumentType.greedyString())
                    .suggests(this::listTownInvites)
                    .executes(this::playerJoinsTown)
                )
            )
            .then(CommandManager.literal("leave")
                .requires(CommandPredicate.cast(ClaimCommand::sourceInTown).and(ClaimCommand::sourceNotMayor))
                .executes(this::playerPartsTown)
            )
            .then(CommandManager.literal("set")
                .requires(CommandPredicate.opLevel(OpLevels.STOP).or(PermissionNodes.ADMIN_CLAIM_TOWNS))
                .then(CommandManager.argument("target", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .then(CommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(CommandClaimUtils::getAllTowns)
                        .executes(this::adminSetPlayerTown)
                    )
                )
            )
            .then(CommandManager.literal("villagers")
                .requires(CommandPredicate.opLevel(OpLevels.STOP).or(PermissionNodes.ADMIN_CLAIM_TOWNS))
                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                    .then(CommandManager.argument("town", StringArgumentType.greedyString())
                        .suggests(CommandClaimUtils::getAllTowns)
                        .executes(this::adminSetEntityTown)
                    )
                )
            )
        );
        
        /*
         * Register the main command object
         */
        
        // The main command
        LiteralCommandNode<ServerCommandSource> protection = CommandUtils.register(dispatcher, "protection", builder -> builder
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
                        .executes(this::updateSetting)
                    )
                )
                .then(CommandManager.literal("*")
                    .then(CommandManager.argument( "rank", StringArgumentType.word())
                        .suggests(EnumArgumentType.enumerate(ClaimRanks.class, ClaimRanks::rankNormalDescription))
                        .executes(this::updateSettings)
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
                        .executes(this::updateBoolean)
                    )
                )
            )
        );
    }
    
    /*
     * Set data for a chunk
     */
    
    private int rawSetChunkPlayer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<GameProfile> targets = GameProfileArgumentType.getProfileArgument(context, "target");
        for (GameProfile target : targets)
            return this.claimChunkAt(source, source.getWorld(), target.getId(), false, BlockPos.ofFloored(source.getPosition()));
        
        return 0;
    }
    private int rawSetChunkTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Claim a chunk
     */
    
    private int claimChunkSelf(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the source of the command
        ServerCommandSource source = context.getSource();
        
        // Get the player running the command
        ServerPlayerEntity player = source.getPlayer();
        
        // Claim the chunk for own player
        return this.claimChunk(
            source,
            player.getUuid()
        );
    }
    private int claimChunkSelfRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.claimChunkRadius(
            context, null
        );
    }
    private int claimChunkTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        
        // Attempt to claim the chunk
        WorldChunk chunk = world.getWorldChunk(BlockPos.ofFloored(source.getPosition()));
        ServerPlayerEntity player = source.getPlayer();
        if (!player.getUuid().equals(((IClaimedChunk) chunk).getOwnerId()))
            this.claimChunkSelf(context);
        
        // Update owner of the town
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        ClaimantTown town = claim.getTown();
        if (town != null) {
            if ((((IClaimedChunk) chunk).getTownId() != null))
                throw ClaimCommand.CHUNK_ALREADY_OWNED.create(player);
            ((IClaimedChunk) chunk).updateTownOwner(town.getId());
        }
        
        // Notify the players in claimed chunks
        RegionUpdateCallback.EVENT.invoker()
            .update(player);
        
        return Command.SINGLE_SUCCESS;
    }
    private int claimChunkTownRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return 0;
    }
    private int claimChunkOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the target player
        GameProfile targetPlayer = GameProfileArgumentType.getProfileArgument(context, "target").stream()
            .findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Claim the chunk for other player
        return this.claimChunk(
            context.getSource(),
            targetPlayer.getId()
        );
    }
    private int claimChunkOtherRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, "target");
        GameProfile targetPlayer = gameProfiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        return this.claimChunkRadius(
            context, targetPlayer.getId()
        );
    }
    private int claimChunkSpawn(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Claim the chunk for spawn
        return this.claimChunk(
            context.getSource(),
            CoreMod.SPAWN_ID
        );
    }
    private int claimChunkSpawnRadius(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.claimChunkRadius(
            context, CoreMod.SPAWN_ID
        );
    }
    private int claimChunk(@NotNull final ServerCommandSource source) throws CommandSyntaxException {
        return this.claimChunk(source, null);
    }
    private int claimChunk(@NotNull final ServerCommandSource source, @Nullable UUID chunkFor) throws CommandSyntaxException {
        // Claiming chunks for self player
        if (chunkFor == null) {
            ServerPlayerEntity player = source.getPlayer();
            chunkFor = player.getUuid();
        }
        
        return this.claimChunkAt(
            source,
            source.getWorld(),
            chunkFor,
            true,
            BlockPos.ofFloored(source.getPosition())
        );
    }
    private int claimChunkRadius(@NotNull final CommandContext<ServerCommandSource> context, @Nullable UUID chunkFor) throws CommandSyntaxException {
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
        BlockPos blockPos = BlockPos.ofFloored(source.getPosition());
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
        return this.claimChunkAt(
            source,
            world,
            chunkFor,
            true,
            chunksToClaim
        );
    }
    
    public int claimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull BlockPos... positions) {
        return this.claimChunkAt(source, world, chunkFor, verify, Arrays.asList(positions));
    }
    public int claimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull Collection<? extends BlockPos> positions) {
        MinecraftServer server = source.getServer();
        if (((ClaimsAccessor) server).getClaimManager() instanceof ServerClaimCache claimCache) {
            ((LogicalWorld)world).addTickableEvent(ChunkOwnerUpdate.forPlayer(
                claimCache,
                source,
                chunkFor,
                ChunkOwnerUpdate.Mode.CLAIM,
                positions
            ).setVerify(verify));
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private int unclaimChunkSelf(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        return this.unclaimChunkAt(
            source,
            source.getWorld(),
            player.getUuid(),
            true,
            BlockPos.ofFloored(source.getPosition())
        );
    }
    private int unclaimChunkTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        
        Chunk chunk = world.getChunk(BlockPos.ofFloored(source.getPosition()));
        if (((IClaimedChunk) chunk).getTownId() == null)
            throw CHUNK_NOT_OWNED.create( player );
        
        // Town SHOULDN'T be null here
        if (claim.getTownId() == null) return -1;
        
        if (!claim.getTownId().equals(((IClaimedChunk)chunk).getTownId()))
            throw CHUNK_NOT_OWNED_BY_PLAYER.create( player );
        
        // Change the town owner
        ((IClaimedChunk) chunk).updateTownOwner( null );
        
        // Notify the players in claimed chunks
        RegionUpdateCallback.EVENT.invoker()
            .update(player);
        
        return Command.SINGLE_SUCCESS;
    }
    private int unclaimChunkOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        return this.unclaimChunkAt(
            source,
            source.getWorld(),
            player.getUuid(),
            false,
            BlockPos.ofFloored(source.getPosition())
        );
    }
    private int unclaimAll(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        final ServerWorld world = source.getWorld();
        
        // Update total count
        Claimant claimed;
        if ((claimed = ((PlayerClaimData) player).getClaim()) != null) {
            this.unclaimChunkAt(
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
    
    public int unclaimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull BlockPos... positions) {
        return this.unclaimChunkAt(source, world, chunkFor, verify, Arrays.asList(positions));
    }
    public int unclaimChunkAt(@NotNull ServerCommandSource source, @NotNull World world, @NotNull final UUID chunkFor, final boolean verify, @NotNull Collection<? extends BlockPos> positions) {
        MinecraftServer server = source.getServer();
        if (((ClaimsAccessor) server).getClaimManager() instanceof ServerClaimCache claimCache) {
            ((LogicalWorld)world).addTickableEvent(ChunkOwnerUpdate.forPlayer(
                claimCache,
                source,
                chunkFor,
                ChunkOwnerUpdate.Mode.UNCLAIM,
                positions
            ).setVerify(verify));
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private int claimSpawnRegionAt(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.claimRegionAt(context, CoreMod.SPAWN_ID);
    }
    private int claimRegionFor(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get information about the target
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "target");
        GameProfile target = profiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        return this.claimRegionAt(context, target.getId());
    }
    private int claimRegionAt(@NotNull CommandContext<ServerCommandSource> context, @NotNull UUID target) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        BlockRange region = BlockRange.between(
            BlockPosArgumentType.getBlockPos(context, "from"),
            BlockPosArgumentType.getBlockPos(context, "to")
        );
        
        // Claim the defined slices
        ClaimChunkUtils.claimSlices(source.getWorld(), target, region);
        source.sendFeedback(
            () -> Text.literal("Claimed ")
                .append(region.formattedVolume())
                .append(" blocks."),
            false
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int unclaimRegionAt(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        BlockRange region = BlockRange.between(
            BlockPosArgumentType.getBlockPos(context, "from"),
            BlockPosArgumentType.getBlockPos(context, "to")
        );
        
        // Unclaim the defined slices
        ClaimChunkUtils.unclaimSlices(source.getWorld(), region);
        source.sendFeedback(
            () -> Text.literal("Unclaimed ")
                .append(region.formattedVolume())
                .append(" blocks."), false
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Town Options
     */
    
    @Deprecated
    private static boolean sourceIsMayor(@NotNull final ServerCommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity player && ClaimCommand.sourceIsMayor(player);
    }
    @Deprecated
    public static boolean sourceIsMayor(@NotNull final ServerPlayerEntity player) {
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        ClaimantTown town;
        
        // Check if owner
        if ((claim != null) && ((town = claim.getTown()) != null))
            return player.getUuid().equals(town.getOwnerId());
        
        return false;
    }
    @Deprecated
    private static boolean sourceNotMayor(@NotNull final ServerCommandSource source) {
        return !ClaimCommand.sourceIsMayor(source);
    }
    @Deprecated
    public static boolean sourceInTown(@NotNull final ServerCommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity player && ClaimCommand.sourceInTown(player);
    }
    @Deprecated
    private static boolean sourceInTown(@NotNull final ServerPlayerEntity player) {
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        return ((claim != null) && claim.getTown() != null);
    }
    @Deprecated
    public static boolean sourceNotInTown(@NotNull final ServerCommandSource source) {
        return !ClaimCommand.sourceInTown( source );
    }
    
    private int townFound(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        if (!(((ClaimsAccessor) server).getClaimManager() instanceof ServerClaimCache claimCache))
            return 0;
        
        ServerPlayerEntity founder = source.getPlayer();
        
        // Charge the player money
        try {
            if ((SewConfig.get(SewProtectionConfig.TOWN_FOUND_COST) > 0) && (!MoneyUtils.takePlayerMoney(founder, SewConfig.get(SewProtectionConfig.TOWN_FOUND_COST))))
                throw NOT_ENOUGH_MONEY.create(founder, "$" + FormattingUtils.format(SewConfig.get(SewProtectionConfig.TOWN_FOUND_COST)));
        } catch (NotEnoughMoneyException e) {
            throw NOT_ENOUGH_MONEY.create(founder, "$" + FormattingUtils.format(SewConfig.get(SewProtectionConfig.TOWN_FOUND_COST)));
        }
        try {
            // Get town information
            MutableText townName = Text.literal(StringArgumentType.getString(context, "name"));
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
    private int townDisband(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity founder = source.getPlayer();
        
        ClaimantPlayer claimantPlayer = ((PlayerClaimData) founder).getClaim();
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
    private int townInvite(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerClaimData) player).getClaim();
        ClaimantTown town = claimant.getTown();
        if (town == null) return -1; // Towns SHOULD always be set when reaching here
        
        if (town.getFriendRank(player.getUuid()) != ClaimRanks.OWNER)
            throw ClaimCommand.TOWN_INVITE_RANK.create(player);
        
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        ClaimantPlayer targetClaimant = ((PlayerClaimData) target).getClaim();
        
        if ( !targetClaimant.inviteTown(town) )
            throw ClaimCommand.TOWN_INVITE_FAIL.create(player);
        
        // Send the notice to the inviter
        TranslatableServerSide.send(player, "town.invite.sent", target.getDisplayName());
        
        // Send the notice to the invitee
        TranslatableServerSide.send(target, "town.invite.receive", town.getName(), player.getDisplayName());
        
        return Command.SINGLE_SUCCESS;
    }
    private int townGiveChunk(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument( context, "target" );
        GameProfile targetPlayer = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        ServerWorld world = source.getWorld();
        IClaimedChunk claimedChunk = (IClaimedChunk) world.getChunk(BlockPos.ofFloored(source.getPosition()));
        
        ServerPlayerEntity player = source.getPlayer();
        ClaimantTown town;
        if ((claimedChunk.getOwnerId() == null) || ((town = claimedChunk.getTown()) == null) || (!player.getUuid().equals(claimedChunk.getOwnerId())) || (!player.getUuid().equals( town.getOwnerId() )))
            throw ClaimCommand.CHUNK_NOT_OWNED_BY_PLAYER.create(player);
        
        claimedChunk.updatePlayerOwner( targetPlayer.getId() );
        
        return Command.SINGLE_SUCCESS;
    }
    private int playerJoinsTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerClaimData) player).getClaim();
        
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
    private int playerPartsTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimaint = ((PlayerClaimData) player).getClaim();
        
        /*
         * Remove town from player
         */
        ClaimantTown town = claimaint.getTown();
        if (town != null)
            town.updateFriend(player.getUuid(), null);
        //claimaint.setTown( null );
        
        // Refresh the command tree
        server.getPlayerManager().sendCommandTree( player );
        
        return Command.SINGLE_SUCCESS;
    }
    private int adminSetPlayerTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context,"target");
        GameProfile profile = gameProfiles.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the town
        String townName = StringArgumentType.getString(context, "town");
        if (!(((ClaimsAccessor) server).getClaimManager() instanceof ServerClaimCache claimCache))
            return 0;
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
    private int adminSetEntityTown(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "entities");
        String townName = StringArgumentType.getString(context, "town");
        if (!(((ClaimsAccessor) server).getClaimManager() instanceof ServerClaimCache claimCache))
            return 0;
        
        ClaimantTown town = claimCache.getTownClaim(townName);
        
        if (town == null)
            throw TOWN_NOT_EXISTS.create();
        
        int added = 0;
        for (Entity entity : entities) {
            if (!(entity instanceof VillagerEntity))
                continue;
            if (((VillagerTownie)entity).setTown(town))
                added++;
        }
        
        Text amount = Text.literal(FormattingUtils.format(added)).formatted(Formatting.AQUA);
        source.sendFeedback(() -> Text.literal("Added ")
            .append(amount)
            .append(" villagers to ")
            .append(town.getName())
            .append("."), false);
        if (added > 0) {
            town.send(server, TextUtils.literal()
                .append(amount)
                .append(" villagers have been added to your town.")
            );
        }
        
        return added;
    }
    private CompletableFuture<Suggestions> listTownInvites(@NotNull CommandContext<ServerCommandSource> context, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ClaimantPlayer claimant = ((PlayerClaimData) player).getClaim();
        
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
    
    private int updateSetting(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimPermissions permission = EnumArgumentType.getEnum(ClaimPermissions.class, StringArgumentType.getString(context, "permission"));
        ClaimRanks rank = EnumArgumentType.getEnum(ClaimRanks.class, StringArgumentType.getString(context, "rank"));
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Update the runtime
        this.updateSetting(player, permission, rank);
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private int updateSettings(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimRanks rank = EnumArgumentType.getEnum(ClaimRanks.class, StringArgumentType.getString(context, "rank"));
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        for ( ClaimPermissions permission : ClaimPermissions.values() )
            this.updateSetting(player, permission, rank);
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private void updateSetting(@NotNull ServerPlayerEntity player, ClaimPermissions permission, ClaimRanks rank) throws CommandSyntaxException {
        // Update the runtime
        ClaimPropertyUtils.updatePermission(player, permission, rank);
        
        // Notify the player
        player.sendMessage(Text.literal("Interacting with ").formatted(Formatting.WHITE)
            .append(Text.literal(CasingUtils.sentence(permission.name())).formatted(Formatting.AQUA))
            .append(Text.literal(" is now limited to ").formatted(Formatting.WHITE))
            .append(Text.literal(CasingUtils.sentence(rank.name())).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.WHITE))
        );
    }
    private int updateBoolean(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get enums
        ClaimSettings setting = EnumArgumentType.getEnum(ClaimSettings.class, StringArgumentType.getString(context, "setting"));
        boolean enabled = BoolArgumentType.getBool(context, "bool");
        
        // Get the player
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Update the runtime
        ClaimPropertyUtils.updateSetting(player, setting, enabled);
        
        // Notify the player
        player.sendMessage(Text.literal(CasingUtils.words(setting.name().replace("_", " "))).formatted(Formatting.AQUA)
            .append(Text.literal(" is now ").formatted(Formatting.WHITE))
            .append(Text.literal( enabled ? "Enabled" : "Disabled" ).formatted(setting.getAttributeColor( enabled )))
            .append(Text.literal(" in your claimed area.").formatted(Formatting.WHITE))
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Adding/Removing users from friends
     */
    
    private int addRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
        ClaimPropertyUtils.updateRank(player, friend.getId(), rank);
        
        // Attempting to update the friend
        player.sendMessage(Text.literal("Player ").formatted(Formatting.WHITE)
            .append(Text.literal(friend.getName()).formatted(Formatting.DARK_PURPLE))
            .append(Text.literal(" is now an "))
            .append(Text.literal(CasingUtils.sentence(rank.name())).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.WHITE))
        );
        
        // Play a sound to the player
        player.playSound(SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.MASTER, 0.5f, 1f );
        
        // Find the entity of the friend
        ServerPlayerEntity friendEntity = SewServer.getPlayer(source.getServer(), friend.getId());
        if ( friendEntity != null ) {
            // Notify the friend
            friendEntity.sendMessage(Text.literal("Player ").formatted(Formatting.WHITE)
                .append(((MutableText)player.getName()).formatted(Formatting.DARK_PURPLE))
                .append(Text.literal(" has added you as an ").formatted(Formatting.WHITE))
                .append(Text.literal(rank.name()).formatted(Formatting.AQUA))
                .append(Text.literal(".").formatted(Formatting.WHITE)), false
            );
            
            friendEntity.playSound(SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.MASTER, 0.5f, 1f);
        }
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private int remRank(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the player
        ServerPlayerEntity player = source.getPlayer();
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, "friend");
        GameProfile friend = gameProfiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Player tries changing their own rank
        if (player.getUuid().equals(friend.getId()))
            throw SELF_RANK_CHANGE.create(player);
        
        // Update our runtime
        ClaimPropertyUtils.updateRank(player, friend.getId(), null);
        
        // Attempting to remove the friend
        player.sendMessage( Text.literal("Player ").formatted(Formatting.WHITE)
            .append( Text.literal(friend.getName()).formatted(Formatting.DARK_PURPLE) )
            .append( Text.literal(" removed."))
        );
        
        // Play sound to player
        player.playSound(SoundEvents.ENTITY_VILLAGER_DEATH, SoundCategory.MASTER, 0.5f, 1f );
        
        // Find the entity of the friend
        ServerPlayerEntity friendEntity = source.getServer()
            .getPlayerManager()
            .getPlayer(friend.getId());
        
        // If the friend is online
        if ( friendEntity != null ) {
            // Notify the friend
            friendEntity.sendMessage(Text.literal("Player ").formatted(Formatting.WHITE)
                .append(((MutableText)player.getName()).formatted(Formatting.DARK_PURPLE))
                .append(Text.literal(" has removed you.")), false
            );
            
            friendEntity.playSound(SoundEvents.ENTITY_VILLAGER_DEATH, SoundCategory.MASTER, 0.5f, 1f);
        }
        
        // Return command success
        return Command.SINGLE_SUCCESS;
    }
    private int listFriends(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Command.SINGLE_SUCCESS;
    }
    private int findFriend(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
        if (player.getWorld() != target.getWorld() || target.isInvisible())
            throw ClaimCommand.PLAYER_DIFFERENT_WORLD.create(player);
        
        return this.pathPlayerToTarget(player, target);
    }
    private int findFriendWarp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
        return this.pathPlayerToTarget(player, warp.warpPos);
    }
    private int stopFindingPos(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        return this.pathPlayerToTarget(player, player);
    }
    private int pathPlayerToTarget(@NotNull ServerPlayerEntity player, @NotNull Entity target) {
        Path navigator = ((PlayerData)player).findPathTo(target, 3);
        if (navigator != null)
            EffectUtils.summonBreadcrumbs(ParticleTypes.FALLING_OBSIDIAN_TEAR, player, navigator);
        else CoreMod.logInfo("Could not find path.");
        return navigator == null ? 0 : 1;
    }
    private int pathPlayerToTarget(@NotNull ServerPlayerEntity player, @NotNull BlockPos position) {
        Path navigator = ((PlayerData)player).findPathTo(position, 3);
        if (navigator != null)
            EffectUtils.summonBreadcrumbs(ParticleTypes.FALLING_OBSIDIAN_TEAR, player, navigator);
        else CoreMod.logInfo("Could not find path.");
        return navigator == null ? 0 : 1;
    }
    
    private @NotNull CompletableFuture<Suggestions> playerHomeNamesOfFriend(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) throws CommandSyntaxException {
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
    
    private int inviteFriend(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
                context.getSource().sendFeedback(
                    () -> Text.translatable("commands.whitelist.add.success", Texts.toText( profile )),
                    true
                );
                
                ++count;
            }
        }
        
        if (count == 0) {
            throw WHITELIST_FAILED_EXCEPTION.create();
        } else {
            return count;
        }
    }
    private int invitedListSelf(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get command context information
        ServerCommandSource source = context.getSource();
        
        return this.invitedList(
            source,
            source.getPlayer().getGameProfile()
        );
    }
    private int invitedListOther(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get command context information
        ServerCommandSource source = context.getSource();
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        
        for (GameProfile profile : profiles) {
            return this.invitedList(
                source,
                profile
            );
        }
        
        return 0;
    }
    private int invitedList(@NotNull ServerCommandSource source, @NotNull GameProfile player) {
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
            out = Text.literal("").append(Text.literal(isPlayer ? "You" : player.getName()).formatted(Formatting.GRAY))
                .append(" " + ( isPlayer ? "were" : "was" ) + " invited to the server by ").formatted(Formatting.WHITE)
                .append(this.inviteeFormattedName(source, claim, invitedBy.getName(), invitedBy.getId()));
        
        MutableText inv = Text.literal("").formatted(Formatting.WHITE)
            .append(Text.literal(isPlayer ? "You" : player.getName()).formatted(Formatting.GRAY))
            .append(" invited the following players [")
            .append(MessageUtils.formatNumber(invited.size()))
            .append("]: ")
            .append(MessageUtils.listToTextComponent(invited, (entry) -> this.inviteeFormattedName(source, claim, ((WhitelistedPlayer)entry).getName(), ((WhitelistedPlayer)entry).getUUID())));
        
        if (out == null) out = inv;
        else out.append("\n").append(inv);
        
        source.sendFeedback(
            Value.of(out),
            false
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private Text inviteeFormattedName(@NotNull ServerCommandSource source, @NotNull ClaimantPlayer claim, @NotNull String name, @NotNull UUID uuid) {
        return Text.literal(name).styled((style) -> {
            if (source.hasPermissionLevel(OpLevels.CHEATING)) {
                ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friends get " + name);
                style = style.withClickEvent(click);
            }
            return style.withFormatting(claim.getFriendRank(uuid).getColor());
        });
    }
}
