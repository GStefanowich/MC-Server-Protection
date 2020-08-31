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

package net.TheElm.project.mixins.World;

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.interfaces.Claim;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.ChunkUtils.ClaimSlice;
import net.TheElm.project.utilities.ChunkUtils.InnerClaim;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Mixin(WorldChunk.class)
public abstract class ClaimedChunk implements IClaimedChunk, Chunk, Claim {
    
    @Shadow public abstract void markDirty();
    
    private final ClaimSlice[] claimSlices = new ClaimSlice[256];
    
    private WeakReference<ClaimantTown> chunkTown = null;
    private ClaimantPlayer chunkPlayer = null;
    
    @Override
    public ClaimantTown updateTownOwner(@Nullable UUID owner, boolean fresh) {
        ClaimantTown town = null;
        if (owner != null) {
            try {
                town = ClaimantTown.get( owner );
            } catch (NbtNotFoundException ignored) {}
        }
        
        // Make sure we have the towns permissions cached
        this.chunkTown = (town == null ? null : new WeakReference<>( town ));
        
        if ( fresh )
            this.markDirty();
        
        return this.getTown();
    }
    private ClaimantTown loadTownReference(@NotNull ClaimantTown town) {
        return (this.chunkTown = new WeakReference<>(town)).get();
    }
    @Override
    public ClaimantPlayer updatePlayerOwner(@Nullable UUID owner, boolean fresh) {
        this.chunkPlayer = ( owner == null ? null : ClaimantPlayer.get( owner ));
        
        if (fresh)
            this.markDirty();
        
        // If there is no player owner, there is no town
        if (owner == null) {
            // Reset the inner slices (SHOULD NOT RESET SPAWN)
            this.resetSlices();
            this.updateTownOwner(null, fresh);
        }
        return this.chunkPlayer;
    }
    
    public void resetSlices() {
        ClaimSlice slice;
        for (int i = 0; i < this.claimSlices.length; i++) {
            if ((slice = this.claimSlices[i]) == null)
                continue;
            slice.reset();
        }
        this.markDirty();
    }
    @Override
    public void updateSliceOwner(UUID owner, int slicePos, int yFrom, int yTo, boolean fresh) {
        // If heights are invalid
        if (World.isHeightInvalid( yFrom ) || World.isHeightInvalid( yTo ))
            return;
        
        ClaimSlice slice;
        if ((slice = this.claimSlices[slicePos]) == null)
            slice = (this.claimSlices[slicePos] = new ClaimSlice());
        
        // Get upper and lower positioning
        int yMax = Math.max( yFrom, yTo );
        int yMin = Math.min( yFrom, yTo );
        
        slice.set(new InnerClaim(owner, yMax, yMin));
        
        // Make sure the chunk gets saved
        if ( fresh )
            this.markDirty();
    }
    public UUID[] getSliceOwner(int slicePos, int yFrom, int yTo) {
        ClaimSlice slice;
        if ((slice = this.claimSlices[slicePos]) == null)
            return new UUID[0];
        
        // Get upper and lower positioning
        int yMax = Math.max( yFrom, yTo );
        int yMin = Math.min( yFrom, yTo );
        
        // Get all owners
        Set<UUID> owners = new HashSet<>();
        for (int y = yMin; y <= yMax; y++)
            owners.add(slice.get(y).getOwner());
        
        return owners.toArray(new UUID[0]);
    }
    
    public @NotNull Claim getClaim(BlockPos blockPos) {
        int slicePos = ChunkUtils.getPositionWithinChunk( blockPos );
        
        ClaimSlice slice;
        if ((slice = this.claimSlices[slicePos]) != null) {
            // Get inside claim
            InnerClaim inner = slice.get( blockPos.getY() );
            
            // If claim inner is not nobody
            if (inner.getOwner() != null && inner.isWithin(blockPos.getY()))
                return inner;
        }
        
        return this;
    }
    
    public void canPlayerClaim(@NotNull ClaimantPlayer player) throws TranslationKeyException {
        if (this.chunkPlayer != null)
            throw new TranslationKeyException( "claim.chunk.error.claimed" );
        if (player.getId().equals(CoreMod.spawnID))
            return;
        // Check claims limit
        if (!player.canClaim((WorldChunk)(Chunk) this))
            throw new TranslationKeyException("claim.chunk.error.max");
    }
    
    public @Nullable UUID getOwner() {
        if (this.chunkPlayer == null)
            return null;
        return this.chunkPlayer.getId();
    }
    public @Nullable UUID getOwner(BlockPos pos) {
        int slicePos = ChunkUtils.getPositionWithinChunk( pos );
        if ( this.claimSlices[slicePos] != null ) {
            ClaimSlice slice = claimSlices[slicePos];
            
            // Get the players Y position
            InnerClaim claim = slice.get( pos );
            
            // Check that the player is within the Y
            if (claim.lower() <= pos.getY() && claim.upper() >= pos.getY())
                return claim.getOwner();
        }
        return this.getOwner();
    }
    public @Nullable UUID getTownId() {
        ClaimantTown town;
        if ((town = this.getTown()) == null)
            return null;
        return town.getId();
    }
    public @Nullable ClaimantTown getTown() {
        if (( this.chunkPlayer == null ))
            return null;
        if (this.chunkTown == null) {
            ClaimantTown playerTown;
            if ((playerTown = this.chunkPlayer.getTown()) != null)
                return this.loadTownReference(playerTown);
        }
        return (this.chunkTown == null ? null : this.chunkTown.get());
    }
    
    @Override
    public Text getOwnerName(@NotNull PlayerEntity zonePlayer, @NotNull BlockPos pos) {
        UUID owner = this.getOwner(pos);
        if ( owner == null )
            return new LiteralText(SewConfig.get(SewConfig.NAME_WILDERNESS))
                .formatted(Formatting.GREEN);
        
        // Get the owner of the chunk
        ClaimantPlayer chunkPlayer = ClaimantPlayer.get(owner);
        
        // Get the owners name (Colored using the relation to the zonePlayer)
        return chunkPlayer.getName( zonePlayer.getUuid() );
    }
    
    @Override
    public boolean canPlayerDo(@Nullable UUID player, @Nullable ClaimPermissions perm) {
        if (this.chunkPlayer == null || (player != null && player.equals(this.chunkPlayer.getId())))
            return true;
        ClaimantTown town;
        if ( ((town = this.getTown()) != null ) && (player != null) && player.equals( town.getOwner() ) )
            return true;
        
        // Get the ranks of the user and the rank required for performing
        ClaimRanks userRank = this.chunkPlayer.getFriendRank(player);
        ClaimRanks permReq = this.chunkPlayer.getPermissionRankRequirement(perm);
        
        // Return the test if the user can perform the action (If friend of chunk owner OR if friend of town and chunk owned by town owner)
        return permReq.canPerform( userRank ) || ((town != null) && (this.chunkPlayer.getId().equals( town.getOwner() )) && permReq.canPerform(town.getFriendRank( player )));
    }
    @Override
    public boolean canPlayerDo(@NotNull BlockPos pos, @Nullable UUID player, @Nullable ClaimPermissions perm) {
        return this.getClaim(pos)
            .canPlayerDo(player, perm);
    }
    @Override
    public boolean isSetting(@NotNull ClaimSettings setting) {
        if (this.chunkPlayer == null)
            return setting.getDefault( this.getOwner() );
        return this.chunkPlayer.getProtectedChunkSetting( setting );
    }
    @Override
    public boolean isSetting(@NotNull BlockPos pos, @NotNull ClaimSettings setting) {
        if ( !setting.isEnabled() )
            return setting.getDefault( this.getOwner() );
        else
            return this.getClaim( pos )
                .isSetting( setting );
    }
    
    @Override
    public @NotNull ListTag serializeSlices() {
        ListTag serialized = new ListTag();
        ClaimSlice slice;
        for (int i = 0; i < this.claimSlices.length; i++) {
            // Slice must be defined
            if ((slice = this.claimSlices[i]) == null)
                continue;
            
            // Create a new tag to save the slice
            CompoundTag sliceTag = new CompoundTag();
            ListTag claimsTag = new ListTag();
            
            // For all slice claims
            Iterator<InnerClaim> claims = slice.getClaims();
            while ( claims.hasNext() ) {
                InnerClaim claim = claims.next();
                
                // If bottom of world, or no owner
                if ((claim.lower() == -1) || (claim.getOwner() == null))
                    continue;
                
                // Save data to the tag
                CompoundTag claimTag = new CompoundTag();
                claimTag.putUuid("owner", claim.getOwner());
                claimTag.putInt("upper", claim.upper());
                claimTag.putInt("lower", claim.lower());
                
                // Add tag to array
                claimsTag.add(claimTag);
            }
            
            // Save data for slice
            sliceTag.putInt("i", i);
            sliceTag.put("claims", claimsTag);
            
            // Save the tag
            serialized.add(sliceTag);
        }
        
        return serialized;
    }
    @Override
    public void deserializeSlices(@NotNull ListTag serialized) {
        for (Tag tag : serialized) {
            // Must be compound tags
            if (!(tag instanceof CompoundTag)) continue;
            CompoundTag sliceTag = (CompoundTag) tag;
            
            ListTag claimsTag = sliceTag.getList("claims", NbtType.COMPOUND);
            int i = sliceTag.getInt("i");
            
            for (Tag claimTag : claimsTag) {
                UUID owner = ((CompoundTag) claimTag).getUuid("owner");
                int upper = ((CompoundTag) claimTag).getInt("upper");
                int lower = ((CompoundTag) claimTag).getInt("lower");
                
                this.updateSliceOwner( owner, i, lower, upper, false );
            }
        }
    }
    
}
