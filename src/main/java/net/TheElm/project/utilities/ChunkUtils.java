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

package net.TheElm.project.utilities;

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.Claim;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public final class ChunkUtils {

    /**
     * Check the database if a user can perform an action within the specified chunk
     */
    public static boolean canPlayerDoInChunk(@Nullable ClaimPermissions perm, @NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( perm, player, player.getEntityWorld().getWorldChunk( blockPos ), blockPos);
    }
    public static boolean canPlayerDoInChunk(@Nullable ClaimPermissions perm, @Nullable PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        // If claims are disabled
        if ((!SewingMachineConfig.INSTANCE.DO_CLAIMS.get()) || (player != null && player.isCreative())) return true;
        
        // Check if player can do action in chunk
        return ChunkUtils.canPlayerDoInChunk( perm, EntityUtils.getUUID(player), chunk, blockPos );
    }
    public static boolean canPlayerDoInChunk(@Nullable ClaimPermissions perm, @Nullable UUID playerId, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        // Return false (Chunks should never BE null, but this is our catch)
        if ( chunk == null ) return false;
        
        // Check if player can do action in chunk
        return ((IClaimedChunk) chunk).canPlayerDo(blockPos, playerId, perm);
    }
    
    /**
     * Check the database if a user can ride entities within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can ride entities
     */
    public static boolean canPlayerRideInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.RIDING, player, blockPos );
    }
    
    /**
     * Check the database if a user can place/break blocks within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can break blocks
     */
    public static boolean canPlayerBreakInChunk(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.BLOCKS, player, blockPos );
    }
    public static boolean canPlayerBreakInChunk(@Nullable UUID playerId, @NotNull World world, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.BLOCKS, playerId, world.getWorldChunk( blockPos ), blockPos );
    }
    
    /**
     * Check the database if a user can loot chests within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can loot storages
     */
    public static boolean canPlayerLootChestsInChunk(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.STORAGE, player, blockPos );
    }
    public static boolean canPlayerLootChestsInChunk(@NotNull PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.STORAGE, player, chunk, blockPos );
    }
    
    /**
     * Check the database if a user can  within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can pick up dropped items
     */
    public static boolean canPlayerLootDropsInChunk(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.PICKUP, player, blockPos );
    }
    
    /**
     * Check the database if a user can interact with doors within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can interact with doors
     */
    public static boolean canPlayerToggleDoor(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.DOORS, player, blockPos );
    }
    public static boolean canPlayerToggleDoor(@NotNull PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.DOORS, player, chunk, blockPos );
    }
    
    /**
     * Check the database if a user can interact with mobs within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can harm or loot friendly entities
     */
    public static boolean canPlayerInteractFriendlies(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.CREATURES, player, blockPos );
    }
    
    /**
     * Check the database if a user can trade with villagers within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can trade with villagers
     */
    public static boolean canPlayerTradeAt(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.TRADING, player, blockPos );
    }
    
    /**
     * Check the database if a user can harvest crops within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player is allowed to harvest crops
     */
    public static boolean canPlayerHarvestCrop(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.HARVEST, player, blockPos );
    }
    
    /**
     * @param player The player that wants to teleport
     * @param target The destination to teleport to
     * @return If the player is a high enough rank to teleport to the target
     */
    public static boolean canPlayerWarpTo(PlayerEntity player, UUID target) {
        if ((!SewingMachineConfig.INSTANCE.DO_CLAIMS.get()) || (SewingMachineConfig.INSTANCE.CLAIM_CREATIVE_BYPASS.get() && (player.isCreative() || player.isSpectator())))
            return SewingMachineConfig.INSTANCE.COMMAND_WARP_TPA.get();
        
        // Check our chunk permissions
        ClaimantPlayer permissions = ClaimantPlayer.get(target);
        
        // Get the ranks of the user and the rank required for performing
        ClaimRanks userRank = permissions.getFriendRank(player.getUuid());
        ClaimRanks permReq = permissions.getPermissionRankRequirement(ClaimPermissions.WARP);
        
        // Return the test if the user can perform the action
        return permReq.canPerform( userRank );
    }
    
    public static boolean isSetting(@NotNull ClaimSettings setting, @NotNull World world, @NotNull BlockPos blockPos) {
        WorldChunk chunk = world.getWorldChunk( blockPos );
        if (chunk != null) {
            return ((IClaimedChunk) chunk)
                .isSetting(blockPos, setting);
        }
        return setting.getDefault( null );
    }
    
    /*
     * Claim slices between two areas
     */
    public static void claimSlices(ServerWorld world, UUID player, BlockPos firstPos, BlockPos secondPos) {
        // Get range of values
        BlockPos min = ChunkUtils.getMinimumPosition(firstPos, secondPos);
        BlockPos max = ChunkUtils.getMaximumPosition(firstPos, secondPos);
        
        // Log the blocks being claimed
        CoreMod.logDebug("Claiming " + MessageUtils.blockPosToString(min) + " to " + MessageUtils.blockPosToString(max));
        
        // Iterate through the blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                BlockPos sliceLoc = new BlockPos( x, 0, z );
                int slicePos = ChunkUtils.getPositionWithinChunk( sliceLoc );
                
                // Get the chunk
                WorldChunk chunk = world.getWorldChunk(sliceLoc);
                
                // Update the owner of the chunk
                ((IClaimedChunk) chunk).updateSliceOwner(player, slicePos, min.getY(), max.getY());
            }
        }
    }
    public static boolean canPlayerClaimSlices(ServerWorld world, BlockPos firstPos, BlockPos secondPos) {
        // Get range of values
        BlockPos min = getMinimumPosition(firstPos, secondPos);
        BlockPos max = getMaximumPosition(firstPos, secondPos);
        
        // Iterate through the blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                BlockPos sliceLoc = new BlockPos( x, 0, z );
                int slicePos = getPositionWithinChunk( sliceLoc );
                
                WorldChunk chunk = world.getWorldChunk( sliceLoc );
                if (((IClaimedChunk) chunk).getSliceOwner(slicePos, min.getY(), max.getY()).length > 0)
                    return false;
            }
        }
        
        return true;
    }
    public static BlockPos getMinimumPosition(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );
    }
    public static BlockPos getMaximumPosition(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );
    }
    
    /*
     * Get data about where the player is
     */
    @Nullable
    public static UUID getPlayerLocation(@NotNull final ServerPlayerEntity player) {
        return CoreMod.PLAYER_LOCATIONS.get( player );
    }
    public static boolean isPlayerWithinSpawn(@NotNull final ServerPlayerEntity player) {
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return true;
        // If player is in creative/spectator, or is within Spawn
        return (SewingMachineConfig.INSTANCE.CLAIM_CREATIVE_BYPASS.get() && (player.isCreative() || player.isSpectator()))
            || CoreMod.spawnID.equals(ChunkUtils.getPlayerLocation( player ));
    }
    public static int getPositionWithinChunk(BlockPos blockPos) {
        int chunkIndex = blockPos.getX() & 0xF;
        return (chunkIndex |= (blockPos.getZ() & 0xF) << 4);
    }
    
    public static Text getPlayerWorldWilderness(@NotNull final PlayerEntity player) {
        if (player.getEntityWorld().dimension.getType() == DimensionType.THE_END)
            return TranslatableServerSide.text(player, "claim.wilderness.end").formatted(Formatting.BLACK);
        if (player.getEntityWorld().dimension.getType() == DimensionType.THE_NETHER)
            return TranslatableServerSide.text(player, "claim.wilderness.nether").formatted(Formatting.LIGHT_PURPLE);
        return TranslatableServerSide.text( player, "claim.wilderness.general" ).formatted(Formatting.GREEN);
    }
    
    public static Optional<UUID> getPosOwner(World world, BlockPos pos) {
        return Optional.ofNullable( ((IClaimedChunk)world.getChunk( pos )).getOwner() );
    }
    
    public static boolean lightChunk(WorldChunk chunk) {
        if (!(chunk.getLightingProvider() instanceof ServerLightingProvider)) return false;
        ServerLightingProvider lighting = (ServerLightingProvider) chunk.getLightingProvider();
        
        ChunkPos pos = chunk.getPos();
        
        // For X
        for (int x = 0; x < 16; x++) {
            int xPos = x + pos.getStartX();
            
            // For Z
            for (int z = 0; z < 16; z++) {
                int zPos = z + pos.getStartZ();
                
                // For Y
                for (int y = chunk.getHeight(); y > 0; y--) {
                    BlockPos lightPos = new BlockPos(xPos, y, zPos);
                    BlockState state = chunk.getBlockState(lightPos);
                    Block block = state.getBlock();
                    
                    // Only if is an AIR Block
                    if (!(block instanceof AirBlock))
                        continue;
                    
                    // TODO: Update the light level
                    /*int lvl = lighting.get(LightType.BLOCK).getLightLevel(lightPos);
                    if (lvl > 0)
                        System.out.println(MessageUtils.blockPosToString(lightPos) + ": " + lvl);*/
                }
            }
        }
        
        return true;
    }
    
    /*
     * Chunk claim classes
     */
    public static final class ClaimSlice {
        private final NavigableMap<Integer, InnerClaim> innerChunks = Collections.synchronizedNavigableMap(new TreeMap<>());
        
        public ClaimSlice() {
            this.innerChunks.put( -1, new InnerClaim( null ));
        }
        
        public void set(InnerClaim claim) {
            this.innerChunks.put( claim.lower(), claim );
        }
        @NotNull
        public InnerClaim get(int y) {
            return this.innerChunks.floorEntry( y ).getValue();
        }
        @NotNull
        public InnerClaim get(BlockPos blockPos) {
            return this.get(blockPos.getY());
        }
        public void reset() {
            Iterator<Map.Entry<Integer, InnerClaim>> it = this.innerChunks.entrySet().iterator();
            while ( it.hasNext() ) {
                Map.Entry<Integer, InnerClaim> entry = it.next();
                InnerClaim claim = entry.getValue();
                
                // Remove all that are not SPAWN
                if (!CoreMod.spawnID.equals( claim.getOwner() ))
                    it.remove();
            }
        }
        
        public Iterator<InnerClaim> getClaims() {
            return this.innerChunks.values().iterator();
        }
    }
    public static final class InnerClaim implements Claim {
        
        @Nullable
        private final ClaimantPlayer owner;
        private final int yUpper;
        private final int yLower;
        
        public InnerClaim(@Nullable UUID owner) {
            this( owner, -1, -1 );
        }
        public InnerClaim(@Nullable UUID owner, int upper, int lower) {
            this.owner = (owner == null ? null : ClaimantPlayer.get( owner ));
            this.yUpper = ( upper > 256 ? 256 : Collections.max(Arrays.asList( upper, lower )));
            this.yLower = Math.max( lower, -1 );
        }
        
        @Nullable
        public UUID getOwner() {
            if (this.owner == null)
                return null;
            return this.owner.getId();
        }
        public int upper() {
            return this.yUpper;
        }
        public int lower() {
            return this.yLower;
        }
        
        public boolean isWithin(int y) {
            return y >= this.lower() && y <= this.upper();
        }
        
        @Override
        public boolean canPlayerDo(@Nullable UUID player, @Nullable ClaimPermissions perm) {
            if (player != null && player.equals(this.getOwner()))
                return true;
            assert this.owner != null;
            
            // Get the ranks of the user and the rank required for performing
            ClaimRanks userRank = this.owner.getFriendRank(player);
            ClaimRanks permReq = this.owner.getPermissionRankRequirement(perm);
            
            // Return the test if the user can perform the action (If friend of chunk owner OR if friend of town and chunk owned by town owner)
            return permReq.canPerform( userRank );
        }
        
        @Override
        public boolean isSetting(@NotNull ClaimSettings setting) {
            if (this.owner == null)
                return setting.getDefault( null );
            return this.owner.getProtectedChunkSetting( setting );
        }
    }
    
}
