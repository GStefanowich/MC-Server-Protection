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

package net.TheElm.project.interfaces;

import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface IClaimedChunk extends Chunk {
    
    ClaimantTown updateTownOwner(@Nullable UUID owner, boolean fresh);
    default ClaimantTown updateTownOwner(@Nullable UUID owner) {
        return this.updateTownOwner(owner, true);
    }
    ClaimantPlayer updatePlayerOwner(@Nullable UUID owner, boolean fresh);
    default ClaimantPlayer updatePlayerOwner(@Nullable UUID owner) {
        return this.updatePlayerOwner(owner, true);
    }
    void canPlayerClaim(@NotNull ClaimantPlayer player) throws TranslationKeyException;
    
    @Nullable
    UUID getOwner();
    @Nullable
    UUID getOwner(BlockPos pos);
    @Nullable
    UUID getTownId();
    @Nullable
    ClaimantTown getTown();
    
    default Text getOwnerName(@NotNull PlayerEntity zonePlayer) {
        return this.getOwnerName(zonePlayer, zonePlayer.getBlockPos());
    }
    Text getOwnerName(@NotNull PlayerEntity zonePlayer, @NotNull BlockPos pos);
    
    boolean canPlayerDo(@NotNull BlockPos blockPos, @Nullable UUID player, @Nullable ClaimPermissions perm);
    boolean isSetting(@NotNull BlockPos pos, @NotNull ClaimSettings setting);
    
    /*
     * Claim Slices
     */
    @NotNull
    ListTag serializeSlices();
    void deserializeSlices(@NotNull ListTag serialized);
    
    default void updateSliceOwner(UUID owner, int slicePos) {
        this.updateSliceOwner(owner, slicePos, 0, 256);
    }
    default void updateSliceOwner(UUID owner, int slicePos, int yFrom, int yTo) {
        this.updateSliceOwner(owner, slicePos, yFrom, yTo, true);
    }
    void updateSliceOwner(UUID owner, int slicePos, int yFrom, int yTo, boolean fresh);
    UUID[] getSliceOwner(int slicePos, int yFrom, int yTo);
    
    /*
     * Statics
     */
    static boolean isOwnedAround(World world, BlockPos blockPos, int leniency ) {
        return IClaimedChunk.getOwnedAround( world, blockPos, leniency).length > 0;
    }
    static IClaimedChunk[] getOwnedAround(final World world, final BlockPos blockPos, final int radius) {
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        
        List<IClaimedChunk> claimedChunks = new ArrayList<>();
        
        // For the X axis
        for ( int x = chunkX - radius; x <= chunkX + radius; x++ ) {
            // For the Z axis
            for ( int z = chunkZ - radius; z <= chunkZ + radius; z++ ) {
                // Create the chunk position
                WorldChunk worldChunk = world.getWorldChunk(new BlockPos( x << 4, 0, z << 4 ));
                
                // If the chunk is claimed
                if (((IClaimedChunk) worldChunk).getOwner() != null)
                    claimedChunks.add( (IClaimedChunk)worldChunk );
            }
        }
        
        return claimedChunks.toArray(new IClaimedChunk[0]);
    }
    
}
