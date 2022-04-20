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
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.interfaces.Claim;
import net.TheElm.project.interfaces.ClaimsAccessor;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.objects.ticking.ClaimCache;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.ChunkUtils.ClaimSlice;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Mixin(Chunk.class)
public abstract class ClaimedChunk implements BlockView, IClaimedChunk, Claim {
    
    private final ClaimSlice[] claimSlices = new ClaimSlice[256];
    
    private WeakReference<ClaimantTown> chunkTown = null;
    private ClaimantPlayer chunkPlayer = null;
    
    @Shadow @Final
    protected HeightLimitView heightLimitView;
    
    @Shadow
    public native void setNeedsSaving(boolean shouldSave);
    
    @Override
    public ClaimantTown updateTownOwner(@Nullable UUID owner, boolean fresh) {
        ClaimantTown town = null;
        if (owner != null)
            town = this.getClaimCache().getTownClaim(owner);
        
        // Make sure we have the towns permissions cached
        this.chunkTown = (town == null ? null : new WeakReference<>(town));
        
        if ( fresh )
            this.setNeedsSaving(true);
        
        return this.getTown();
    }
    private ClaimantTown loadTownReference(@NotNull ClaimantTown town) {
        return (this.chunkTown = new WeakReference<>(town)).get();
    }
    @Override
    public ClaimantPlayer updatePlayerOwner(@Nullable UUID owner, boolean fresh) {
        this.chunkPlayer = ( owner == null ? null : this.getClaimCache().getPlayerClaim(owner));
        
        if (fresh)
            this.setNeedsSaving(true);
        
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
        this.setNeedsSaving(true);
    }
    @Override
    public void updateSliceOwner(UUID owner, int slicePos, int yFrom, int yTo, boolean fresh) {
        // If heights are invalid
        if (this.heightLimitView.isOutOfHeightLimit(yFrom) || this.heightLimitView.isOutOfHeightLimit(yTo))
            return;
        
        ClaimSlice slice;
        if ((slice = this.claimSlices[slicePos]) == null)
            slice = (this.claimSlices[slicePos] = new ClaimSlice(this.getClaimCache(), this.heightLimitView, slicePos));
        
        // Get upper and lower positioning
        int yMax = Math.max(yFrom, yTo);
        int yMin = Math.min(yFrom, yTo);
        
        slice.insert(owner, yMax, yMin);
        
        // Make sure the chunk gets saved
        if ( fresh )
            this.setNeedsSaving(true);
    }
    public UUID[] getSliceOwner(int slicePos, int yFrom, int yTo) {
        ClaimSlice slice;
        if ((slice = this.claimSlices[slicePos]) == null)
            return new UUID[0];
        
        // Get upper and lower positioning
        int yMax = Math.max(yFrom, yTo);
        int yMin = Math.min(yFrom, yTo);
        
        // Get all owners
        Set<UUID> owners = new HashSet<>();
        for (int y = yMin; y <= yMax; y++) {
            ClaimSlice.InnerClaim claim = slice.get(y);
            if (claim != null)
                owners.add(claim.getOwner());
        }
        
        return owners.toArray(new UUID[0]);
    }
    @Override
    public @NotNull ClaimSlice[] getSlices() {
        return this.claimSlices;
    }
    @Override
    public void setSlices(@NotNull ClaimSlice[] slices) {
        int c = slices.length;
        for (int i = 0; i < c; i++)
            this.claimSlices[i] = slices[i];
    }
    
    public @NotNull Claim getClaim(BlockPos blockPos) {
        int slicePos = ChunkUtils.getPositionWithinChunk( blockPos );
        
        ClaimSlice slice;
        if ((slice = this.claimSlices[slicePos]) != null) {
            // Get inside claim
            ClaimSlice.InnerClaim inner = slice.get(blockPos.getY());
            
            // If claim inner is not nobody
            if (inner != null && inner.getOwner() != null && inner.isWithin(blockPos.getY()))
                return inner;
        }
        
        return this;
    }
    
    public boolean canPlayerClaim(@NotNull ClaimantPlayer player, boolean stopIfClaimed) throws TranslationKeyException {
        // If the chunk is owned, return false
        if (this.chunkPlayer != null) {
            if (stopIfClaimed)
                throw new TranslationKeyException("claim.chunk.error.claimed");
            return false;
        }
        // Check claims limit (If the player is spawn, always allow)
        if (!player.getId().equals(CoreMod.SPAWN_ID) && !player.canClaim((Chunk)(Object) this))
            throw new TranslationKeyException("claim.chunk.error.max");
        return true;
    }
    
    @Override
    public @Nullable ClaimCache getClaimCache() {
        Chunk chunk = (Chunk)(Object)this;
        
        // TODO: Alternative way of getting the World from the Chunk?
        if (chunk instanceof WorldChunk worldChunk) {
            MinecraftServer server = worldChunk.getWorld().getServer();
            return ((ClaimsAccessor)server).getClaimManager();
        } else if (this.heightLimitView instanceof World world) {
            MinecraftServer server = world.getServer();
            return ((ClaimsAccessor)server).getClaimManager();
        }
        
        return null;
    }
    public @Nullable UUID getOwner() {
        if (this.chunkPlayer == null)
            return null;
        return this.chunkPlayer.getId();
    }
    public @Nullable UUID getOwner(@Nullable BlockPos pos) {
        if (pos != null) {
            int slicePos = ChunkUtils.getPositionWithinChunk(pos);
            if (this.claimSlices[slicePos] != null) {
                ClaimSlice slice = claimSlices[slicePos];
                
                // Get the players Y position
                ClaimSlice.InnerClaim claim = slice.get(pos);
                
                // Check that the player is within the Y
                if (claim != null && claim.isWithin(pos))
                    return claim.getOwner();
            }
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
        return permReq.canPerform(userRank) || ((town != null) && (this.chunkPlayer.getId().equals(town.getOwner())) && permReq.canPerform(town.getFriendRank(player)));
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
    public @NotNull NbtList serializeSlices() {
        NbtList serialized = new NbtList();
        ClaimSlice slice;
        for (int i = 0; i < this.claimSlices.length; i++) {
            // Slice must be defined
            if ((slice = this.claimSlices[i]) == null)
                continue;
            
            // Create a new tag to save the slice
            NbtCompound sliceTag = new NbtCompound();
            NbtList claimsTag = new NbtList();
            
            // For all slice claims
            Iterator<ClaimSlice.InnerClaim> claims = slice.getClaims();
            while ( claims.hasNext() ) {
                ClaimSlice.InnerClaim claim = claims.next();
                
                // If bottom of world, or no owner
                if ((claim.lower() == -1) || (claim.getOwner() == null))
                    continue;
                
                // Save data to the tag
                NbtCompound claimTag = new NbtCompound();
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
    public void deserializeSlices(@NotNull NbtList serialized) {
        for (NbtElement tag : serialized) {
            // Must be compound tags
            if (!(tag instanceof NbtCompound sliceTag))
                continue;
            
            NbtList claimsTag = sliceTag.getList("claims", NbtElement.COMPOUND_TYPE);
            int i = sliceTag.getInt("i");
            
            for (NbtElement claimTag : claimsTag) {
                UUID owner = NbtUtils.getUUID((NbtCompound) claimTag,"owner");
                int upper = ((NbtCompound) claimTag).getInt("upper");
                int lower = ((NbtCompound) claimTag).getInt("lower");
                
                this.updateSliceOwner(owner, i, lower, upper, false);
            }
        }
    }
    
}
