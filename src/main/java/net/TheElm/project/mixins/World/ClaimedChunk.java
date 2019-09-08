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

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.*;

@Mixin(WorldChunk.class)
public abstract class ClaimedChunk implements IClaimedChunk, Chunk {
    
    private final ClaimSlice[] claimSlices = new ClaimSlice[256];
    
    private ClaimantTown   chunkTown   = null;
    private ClaimantPlayer chunkPlayer = null;
    
    public void updateTownOwner(@Nullable UUID owner) {
        // Make sure we have the towns permissions cached
        this.chunkTown = ( owner == null ? null : ClaimantTown.get( owner ));
    }
    public void updatePlayerOwner(@Nullable UUID owner) {
        this.chunkPlayer = ( owner == null ? null : ClaimantPlayer.get( owner ));
        
        // If there is no player owner, there is no town
        if (owner == null)
            this.updateTownOwner( null );
    }
    
    public void canPlayerClaim(@NotNull UUID owner) throws TranslationKeyException {
        if (this.chunkPlayer != null)
            throw new TranslationKeyException( "claim.chunk.error.claimed" );
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
            int y = pos.getY();
            InnerClaim claim = slice.get(y);
            
            // Check that the player is within the Y
            if (claim.yLower <= y && claim.yUpper >= y)
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
        if (( this.chunkPlayer == null ) || ( this.chunkTown == null ))
            return null;
        return this.chunkTown;
    }
    
    public Text getOwnerName(@NotNull PlayerEntity zonePlayer) {
        if ( this.chunkPlayer == null )
            return new LiteralText(SewingMachineConfig.INSTANCE.NAME_WILDERNESS.get())
                    .formatted(Formatting.GREEN);
        
        // Get the owners name
        return this.chunkPlayer.getName( zonePlayer.getUuid() );
    }
    
    public boolean canUserDo(@NotNull UUID player, ClaimPermissions perm) {
        if (this.chunkPlayer == null || player.equals(this.chunkPlayer.getId()))
            return true;
        if ( ( this.getTown() != null ) && player.equals( this.getTown().getOwner() ) )
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
    
    public static final class ClaimSlice {
        private final NavigableMap<Integer, InnerClaim> innerChunks = Collections.synchronizedNavigableMap(new TreeMap<>());
        
        public ClaimSlice() {
            this.innerChunks.put( -1, new InnerClaim( null ));
        }
        
        public InnerClaim get(int y) {
            return this.innerChunks.floorEntry( y ).getValue();
        }
    }
    public static final class InnerClaim {
        
        private final UUID owner;
        private final int yUpper;
        private final int yLower;
        
        public InnerClaim(@Nullable UUID owner) {
            this( owner, -1, -1 );
        }
        public InnerClaim(@Nullable UUID owner, int upper, int lower) {
            this.owner = owner;
            this.yUpper = ( upper > 256 ? 256 : Collections.max(Arrays.asList( upper, lower )));
            this.yLower = ( lower < -1 ? -1 : lower);
        }
        
        @Nullable
        public UUID getOwner() {
            return this.owner;
        }
        public int upper() {
            return this.yUpper;
        }
        public int lower() {
            return this.yLower;
        }
        
    }
    
}
