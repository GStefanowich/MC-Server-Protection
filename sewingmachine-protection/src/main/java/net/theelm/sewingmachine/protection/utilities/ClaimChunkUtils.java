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

package net.theelm.sewingmachine.protection.utilities;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.enums.ClaimRanks;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.protection.interfaces.Claim;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.interfaces.PlayerTravel;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.theelm.sewingmachine.protection.objects.PlayerVisitor;
import net.theelm.sewingmachine.protections.BlockRange;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.utilities.ChunkUtils;
import net.theelm.sewingmachine.utilities.DimensionUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public final class ClaimChunkUtils {
    /**
     * Check the database if a user can perform an action within the specified chunk
     */
    public static boolean canPlayerDoInChunk(@Nullable ClaimPermissions perm, @NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(perm, player, player.getEntityWorld().getWorldChunk(blockPos), blockPos);
    }
    public static boolean canPlayerDoInChunk(@Nullable ClaimPermissions perm, @Nullable PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        // If claims are disabled
        if (player != null && player.isCreative() && SewConfig.get(SewProtectionConfig.CLAIM_CREATIVE_BYPASS))
            return true;
        
        // Check if player can do action in chunk
        return ClaimChunkUtils.canPlayerDoInChunk(perm, EntityUtils.getUUID(player), chunk, blockPos);
    }
    public static boolean canPlayerDoInChunk(@Nullable ClaimPermissions perm, @Nullable UUID playerId, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        // Return false (Chunks should never BE null, but this is our catch)
        if ( chunk == null )
            return false;
        
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
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.RIDING, player, blockPos);
    }
    
    /**
     * Check the database if a user can interact with doors within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can interact with doors
     */
    public static boolean canPlayerSleep(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.BEDS, player, blockPos);
    }
    public static boolean canPlayerSleep(@NotNull PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.BEDS, player, chunk, blockPos);
    }
    
    /**
     * Check the database if a user can place/break blocks within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can break blocks
     */
    public static boolean canPlayerBreakInChunk(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.BLOCKS, player, blockPos);
    }
    public static boolean canPlayerBreakInChunk(@Nullable UUID playerId, @NotNull World world, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.BLOCKS, playerId, world.getWorldChunk(blockPos), blockPos);
    }
    
    /**
     * Check the database if a user can loot chests within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player can loot storages
     */
    public static boolean canPlayerLootChestsInChunk(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.STORAGE, player, blockPos);
    }
    public static boolean canPlayerLootChestsInChunk(@NotNull PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.STORAGE, player, chunk, blockPos);
    }
    
    /**
     * Check the database if a user can  within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can pick up dropped items
     */
    public static boolean canPlayerLootDropsInChunk(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk( ClaimPermissions.PICKUP, player, blockPos );
    }
    
    /**
     * Check the database if a user can interact with doors within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can interact with doors
     */
    public static boolean canPlayerToggleDoor(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.DOORS, player, blockPos);
    }
    public static boolean canPlayerToggleDoor(@NotNull PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.DOORS, player, chunk, blockPos);
    }
    
    /**
     * Check the database if a user can interact with redstone mechanisms within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can interact with mechanisms
     */
    public static boolean canPlayerToggleMechanisms(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.REDSTONE, player, blockPos);
    }
    public static boolean canPlayerToggleMechanisms(@NotNull PlayerEntity player, @Nullable WorldChunk chunk, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.REDSTONE, player, chunk, blockPos);
    }
    
    /**
     * Check the database if a user can interact with mobs within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can harm or loot friendly entities
     */
    public static boolean canPlayerInteractFriendlies(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.CREATURES, player, blockPos);
    }
    
    /**
     * Check the database if a user can trade with villagers within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If player can trade with villagers
     */
    public static boolean canPlayerTradeAt(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.TRADING, player, blockPos);
    }
    
    /**
     * Check the database if a user can harvest crops within the specified chunk
     * @param player The player to check
     * @param blockPos The block position of the interaction
     * @return If the player is allowed to harvest crops
     */
    public static boolean canPlayerHarvestCrop(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ClaimChunkUtils.canPlayerDoInChunk(ClaimPermissions.HARVEST, player, blockPos);
    }
    
    /**
     * @param player The player that wants to teleport
     * @param target The destination to teleport to
     * @return If the player is a high enough rank to teleport to the target
     */
    public static boolean canPlayerWarpTo(@NotNull PlayerEntity player, @NotNull UUID target) {
        if (SewConfig.get(SewProtectionConfig.CLAIM_CREATIVE_BYPASS) && (player.isCreative() || player.isSpectator()))
            return SewConfig.get(SewBaseConfig.COMMAND_WARP_TPA);
        return ClaimChunkUtils.canPlayerWarpTo(Objects.requireNonNull(player.getServer()), player.getUuid(), target);
    }
    
    /**
     * @param player The player that wants to teleport
     * @param target The destination to teleport to
     * @return If the player is a high enough rank to teleport to the target
     */
    public static boolean canPlayerWarpTo(@NotNull MinecraftServer server, @NotNull UUID player, @NotNull UUID target) {
        // Check our chunk permissions
        ClaimantPlayer permissions = ((ClaimsAccessor) server).getClaimManager()
            .getPlayerClaim(target);
        
        // Get the ranks of the user and the rank required for performing
        ClaimRanks userRank = permissions.getFriendRank(player);
        ClaimRanks permReq = permissions.getPermissionRankRequirement(ClaimPermissions.WARP);
        
        // Return the test if the user can perform the action
        return permReq.canPerform(userRank);
    }
    
    /**
     * Simple check of permissions based on the owners of two positions
     * @param world The world to test the permissions in
     * @param protectedPos The position that is being interacted with ()
     * @param sourcePos The position doing the interacting (A piston, player, etc)
     * @param permission The permission to test
     * @return Whether sourcePos is allowed to do something to protectedPos
     */
    public static boolean canBlockModifyBlock(@NotNull World world, @NotNull BlockPos protectedPos, @NotNull BlockPos sourcePos, @Nullable ClaimPermissions permission) {
        // Get chunks
        WorldChunk protectedChunk = world.getWorldChunk(protectedPos);
        WorldChunk sourceChunk = world.getWorldChunk(sourcePos);
        
        // Check that first chunk owner can modify the next chunk
        return ((IClaimedChunk) protectedChunk).canPlayerDo(protectedPos, ((IClaimedChunk) sourceChunk).getOwnerId(sourcePos), permission);
    }
    
    public static boolean isSetting(@NotNull ClaimSettings setting, @NotNull WorldView world, @NotNull BlockPos blockPos) {
        Chunk chunk = world.getChunk(blockPos);
        return chunk instanceof IClaimedChunk claimedChunk ? claimedChunk.isSetting(blockPos, setting) : setting.getDefault(null);
    }
    
    /*
     * Claim slices between two areas
     */
    public static void claimSlices(@NotNull ServerWorld world, @Nullable UUID player, @NotNull BlockRange region) {
        // Get range of values
        BlockPos min = region.getLower();
        BlockPos max = region.getUpper();
        
        // Log the blocks being claimed
        CoreMod.logDebug("Claiming " + MessageUtils.xyzToString(min) + " to " + MessageUtils.xyzToString(max) + " in '" + DimensionUtils.dimensionIdentifier(world) + "'.");
        
        // Iterate through the blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                BlockPos sliceLoc = new BlockPos(x, 0, z);
                int slicePos = ChunkUtils.getPositionWithinChunk(sliceLoc);
                
                // Get the chunk
                WorldChunk chunk = world.getWorldChunk(sliceLoc);
                
                // Update the owner of the chunk
                ((IClaimedChunk) chunk).updateSliceOwner(player, slicePos, min.getY(), max.getY());
            }
        }
    }
    public static void unclaimSlices(@NotNull ServerWorld world, @NotNull BlockRange region) {
        // Get range of values
        BlockPos min = region.getLower();
        BlockPos max = region.getUpper();
        
        // Log the blocks being claimed
        CoreMod.logDebug("Unclaiming " + MessageUtils.xyzToString(min) + " to " + MessageUtils.xyzToString(max));
        
        // Iterate through the blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                BlockPos sliceLoc = new BlockPos(x, 0, z);
                int slicePos = ChunkUtils.getPositionWithinChunk(sliceLoc);
                
                // Get the chunk
                WorldChunk chunk = world.getWorldChunk(sliceLoc);
                
                ((IClaimedChunk) chunk).updateSliceOwner(null, slicePos, min.getY(), max.getY());
            }
        }
    }
    
    public static boolean canPlayerClaimSlices(@NotNull ServerWorld world, @NotNull BlockRange region) {
        // Get range of values
        BlockPos min = region.getLower();
        BlockPos max = region.getUpper();
        
        // Iterate through the blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                BlockPos sliceLoc = new BlockPos(x, 0, z);
                int slicePos = ChunkUtils.getPositionWithinChunk(sliceLoc);
                
                WorldChunk chunk = world.getWorldChunk( sliceLoc );
                if (((IClaimedChunk) chunk).getSliceOwner(slicePos, min.getY(), max.getY()).length > 0)
                    return false;
            }
        }
        
        return true;
    }
    
    /*
     * Get data about where the player is
     */
    
    public static @Nullable UUID getPlayerLocation(@NotNull final ServerPlayerEntity player) {
        PlayerVisitor visitor = ((PlayerTravel) player).getLocation();
        return visitor == null ? null : visitor.get();
    }
    public static boolean isPlayerWithinSpawn(@NotNull final ServerPlayerEntity player) {
        // If player is in creative/spectator, or is within Spawn
        return (SewConfig.get(SewProtectionConfig.CLAIM_CREATIVE_BYPASS) && (player.isCreative() || player.isSpectator()))
            || CoreMod.SPAWN_ID.equals(ClaimChunkUtils.getPlayerLocation(player));
    }
    
    public static MutableText getPlayerWorldWilderness(@NotNull final PlayerEntity player) {
        if (World.END.equals(player.getEntityWorld().getRegistryKey()))
            return ServerText.translatable(player, "claim.wilderness.end").formatted(Formatting.BLACK);
        if (World.NETHER.equals(player.getEntityWorld().getRegistryKey()))
            return ServerText.translatable(player, "claim.wilderness.nether").formatted(Formatting.LIGHT_PURPLE);
        return ServerText.translatable(player, "claim.wilderness.general").formatted(Formatting.GREEN);
    }
    
    public static Optional<UUID> getPosOwner(World world, BlockPos pos) {
        return Optional.ofNullable(((IClaimedChunk)world.getChunk( pos )).getOwnerId(pos));
    }
    
    /*
     * Chunk claim classes
     */
    public static final class ClaimSlice {
        private final NavigableMap<Integer, InnerClaim> innerChunks = Collections.synchronizedNavigableMap(new TreeMap<>());
        private final ClaimCache claims;
        private final HeightLimitView view;
        private final int chunkPos;

        public ClaimSlice(@NotNull ClaimCache claims, @NotNull HeightLimitView view, int chunkPos) {
            this.claims = claims;
            this.view = view;
            this.chunkPos = chunkPos;
        }

        public boolean has(int y) {
            return this.get(y) != null;
        }
        public boolean hasUpperNeighbor(@NotNull InnerClaim claim) {
            return this.has(claim.upper() + 1);
        }
        public boolean hasLowerNeighbor(@NotNull InnerClaim claim) {
            return this.has(claim.lower() - 1);
        }

        public void set(@NotNull InnerClaim claim) {
            this.innerChunks.put(claim.lower(), claim);
        }
        public void setAll(@NotNull Collection<InnerClaim> claims) {
            for (InnerClaim claim : claims)
                this.set(claim);
        }

        public InnerClaim remove(@NotNull InnerClaim claim) {
            return this.remove(claim.lower());
        }
        public InnerClaim remove(int y) {
            return this.innerChunks.remove(y);
        }

        public @Nullable InnerClaim get(int y) {
            Map.Entry<Integer, InnerClaim> pair = this.innerChunks.floorEntry(y);
            if (pair == null)
                return null;
            InnerClaim claim = pair.getValue();
            return claim.isWithin(y) ? claim : null;
        }
        public @Nullable InnerClaim get(@NotNull BlockPos blockPos) {
            return this.get(blockPos.getY());
        }

        public void insert(@Nullable UUID owner, int upper, int lower) {
            this.displace(new InnerClaim(owner, upper, lower));
        }
        public void displace(@NotNull InnerClaim newClaim) {
            List<InnerClaim> updates = new ArrayList<>();
            Iterator<InnerClaim> intersects = this.innerChunks.subMap(newClaim.lower(), true, newClaim.upper(), true)
                .values()
                .iterator();
            while (intersects.hasNext()) {
                InnerClaim claim = intersects.next();
                if (claim.upper() <= newClaim.upper() && claim.lower() >= newClaim.lower()) {
                    intersects.remove();
                    continue;
                }

                if (claim.upper() > newClaim.upper())
                    updates.add(new InnerClaim(claim.getOwnerId(), claim.upper(), newClaim.upper()));
                if (claim.lower() < newClaim.lower())
                    updates.add(new InnerClaim(claim.getOwnerId(), newClaim.lower(), claim.lower()));
            }

            // Add updated regions into the heightmap
            this.setAll(updates);

            // Don't save unclaimed regions into the heightmap
            if (newClaim.getOwnerId() != null)
                this.set(newClaim);
        }

        public void reset() {
            Iterator<Map.Entry<Integer, InnerClaim>> it = this.innerChunks.entrySet().iterator();
            while ( it.hasNext() ) {
                Map.Entry<Integer, InnerClaim> entry = it.next();
                InnerClaim claim = entry.getValue();

                // Remove all that are not SPAWN
                if (!CoreMod.SPAWN_ID.equals( claim.getOwnerId() ))
                    it.remove();
            }
        }

        public Iterator<InnerClaim> getClaims() {
            return this.innerChunks.values().iterator();
        }

        public final class InnerClaim implements Claim {
            private final @Nullable ClaimantPlayer owner;
            private final int yUpper;
            private final int yLower;
            
            public InnerClaim(@Nullable UUID owner, int upper, int lower) {
                this.owner = (owner == null ? null : ClaimSlice.this.claims.getPlayerClaim(owner));
                this.yUpper = Integer.min(ClaimSlice.this.view.getTopY(), Integer.max(upper, lower));
                this.yLower = Math.max(lower, ClaimSlice.this.view.getBottomY() - 1);
            }
            
            public boolean hasOwner() {
                return this.getOwnerId() != null;
            }
            @Override
            public @Nullable ClaimantPlayer getOwner() {
                return this.owner;
            }
            @Override
            public @Nullable UUID getOwnerId() {
                return this.owner == null ? null : this.owner.getId();
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
            public boolean isWithin(@NotNull BlockPos pos) {
                return this.isWithin(pos.getY());
            }

            @Override
            public boolean canPlayerDo(@Nullable UUID player, @Nullable ClaimPermissions perm) {
                if (player != null && player.equals(this.getOwnerId()))
                    return true;
                assert this.owner != null;

                // Get the ranks of the user and the rank required for performing
                ClaimRanks userRank = this.owner.getFriendRank(player);
                ClaimRanks permReq = this.owner.getPermissionRankRequirement(perm);

                // Return the test if the user can perform the action (If friend of chunk owner OR if friend of town and chunk owned by town owner)
                return permReq.canPerform(userRank);
            }

            @Override
            public boolean isSetting(@NotNull ClaimSettings setting) {
                if (this.owner == null)
                    return setting.getDefault(null);
                return setting.hasSettingSet(this);
            }
        }
    }
    /*public static final class InnerClaim implements Claim {
        private final @Nullable ClaimantPlayer owner;
        private final int yUpper;
        private final int yLower;
        
        public InnerClaim(@NotNull HeightLimitView view, @Nullable UUID owner, int upper, int lower) {
            this.owner = (owner == null ? null : ClaimantPlayer.getPlayer(owner));
            this.yUpper = Integer.min(view.getTopY(), Integer.max(upper, lower));
            this.yLower = Math.max(lower, view.getBottomY() - 1);
        }
        
        public boolean hasOwner() {
            return this.getOwner() != null;
        }
        public @Nullable UUID getOwner() {
            return this.owner == null ? null : this.owner.getId();
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
        public boolean isWithin(@NotNull BlockPos pos) {
            return this.isWithin(pos.getY());
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
            return permReq.canPerform(userRank);
        }
        
        @Override
        public boolean isSetting(@NotNull ClaimSettings setting) {
            if (this.owner == null)
                return setting.getDefault( null );
            return this.owner.getProtectedChunkSetting( setting );
        }
    }*/
}