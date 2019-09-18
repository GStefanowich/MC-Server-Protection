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
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.ChunkUtils.ClaimSlice;
import net.TheElm.project.utilities.ChunkUtils.InnerClaim;
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
import java.util.Iterator;
import java.util.UUID;

@Mixin(WorldChunk.class)
public abstract class ClaimedChunk implements IClaimedChunk, Chunk {
    
    @Shadow public abstract void markDirty();
    
    private final ClaimSlice[] claimSlices = new ClaimSlice[256];
    
    private WeakReference<ClaimantTown> chunkTown = null;
    private ClaimantPlayer chunkPlayer = null;
    
    public ClaimantTown updateTownOwner(@Nullable UUID owner) {
        ClaimantTown town = null;
        if (owner != null) {
            try {
                town = ClaimantTown.get( owner );
            } catch (NbtNotFoundException ignored) {}
        }
        // Make sure we have the towns permissions cached
        this.chunkTown = (town == null ? null : new WeakReference<>( town ));
        this.markDirty();
        return this.getTown();
    }
    private ClaimantTown updateTownOwner(@NotNull ClaimantTown town) {
        this.markDirty();
        return (this.chunkTown = new WeakReference<>(town)).get();
    }
    public ClaimantPlayer updatePlayerOwner(@Nullable UUID owner) {
        this.chunkPlayer = ( owner == null ? null : ClaimantPlayer.get( owner ));
        this.markDirty();
        
        // If there is no player owner, there is no town
        if (owner == null)
            this.updateTownOwner( (UUID)null );
        return this.chunkPlayer;
    }
    
    public void setSliceOwner(UUID owner, int slicePos) {
        this.setSliceOwner(owner, slicePos, 0, 256);
    }
    public void setSliceOwner(UUID owner, int slicePos, int yFrom, int yTo) {
        // If heights are invalid
        if (World.isHeightInvalid( yFrom ) || World.isHeightInvalid( yTo ))
            return;
        
        ClaimSlice slice;
        if ((slice = this.claimSlices[slicePos]) == null) {
            slice = (this.claimSlices[slicePos] = new ClaimSlice());
        }
        
        // Get upper and lower positioning
        int upper = Math.max( yFrom, yTo );
        int lower = Math.min( yFrom, yTo );
        
        slice.set(new InnerClaim(owner, upper, lower));
    }
    
    public void canPlayerClaim(@NotNull UUID owner) throws TranslationKeyException {
        if (this.chunkPlayer != null)
            throw new TranslationKeyException( "claim.chunk.error.claimed" );
        if (owner.equals(CoreMod.spawnID))
            return;
        // Check claims limit
        ClaimantPlayer player = ClaimantPlayer.get( owner );
        if (player != null) { 
            if ((SewingMachineConfig.INSTANCE.PLAYER_CLAIMS_LIMIT.get() == 0) || (((player.getCount() + 1) > player.getMaxChunkLimit()) && (SewingMachineConfig.INSTANCE.PLAYER_CLAIMS_LIMIT.get() > 0)))
                throw new TranslationKeyException("claim.chunk.error.max");
        }
    }
    
    @Nullable
    public UUID getOwner() {
        if (this.chunkPlayer == null)
            return null;
        return this.chunkPlayer.getId();
    }
    @Nullable
    public UUID getOwner(BlockPos pos) {
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
    @Nullable
    public UUID getTownId() {
        ClaimantTown town;
        if ((town = this.getTown()) == null)
            return null;
        return town.getId();
    }
    @Nullable
    public ClaimantTown getTown() {
        if (( this.chunkPlayer == null ))
            return null;
        if (this.chunkTown == null) {
            ClaimantTown playerTown;
            if ((playerTown = this.chunkPlayer.getTown()) != null)
                return this.updateTownOwner(playerTown);
        }
        return (this.chunkTown == null ? null : this.chunkTown.get());
    }
    
    public Text getOwnerName(@NotNull PlayerEntity zonePlayer) {
        if ( this.chunkPlayer == null )
            return new LiteralText(SewingMachineConfig.INSTANCE.NAME_WILDERNESS.get())
                .formatted(Formatting.GREEN);
        
        // Get the owners name
        return this.chunkPlayer.getName( zonePlayer.getUuid() );
    }
    
    public boolean canUserDo(UUID player, ClaimPermissions perm) {
        if (this.chunkPlayer == null || (player != null && player.equals(this.chunkPlayer.getId())))
            return true;
        if ( ( this.getTown() != null ) && (player != null) && player.equals( this.getTown().getOwner() ) )
            return true;
        
        // Get the ranks of the user and the rank required for performing
        ClaimRanks userRank = this.chunkPlayer.getFriendRank( player );
        ClaimRanks permReq = this.chunkPlayer.getPermissionRankRequirement( perm );
        
        // Return the test if the user can perform the action
        return permReq.canPerform( userRank );
    }
    public boolean isSetting(@NotNull ClaimSettings setting) {
        boolean permission;
        if ( this.chunkPlayer == null )
            permission = setting.getPlayerDefault();
        else
            permission = this.chunkPlayer.getProtectedChunkSetting( setting );
        return permission;
    }
    
    @Override @NotNull
    public ListTag serializeSlices() {
        ListTag serialized = new ListTag();
        for (int i = 0; i < this.claimSlices.length; i++) {
            ClaimSlice slice = this.claimSlices[i];
            
            // Slice must be defined
            if (slice == null) continue;
            
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
            
            ListTag claimsTag = sliceTag.getList("claims", 10);
            int i = sliceTag.getInt("i");
            
            for (Tag claimTag : claimsTag) {
                UUID owner = ((CompoundTag) claimTag).getUuid("owner");
                int upper = ((CompoundTag) claimTag).getInt("upper");
                int lower = ((CompoundTag) claimTag).getInt("lower");
                
                this.setSliceOwner( owner, i, lower, upper );
            }
        }
    }
    
}
