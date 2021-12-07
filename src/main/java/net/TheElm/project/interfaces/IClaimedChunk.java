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

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface IClaimedChunk {
    
    ClaimantTown updateTownOwner(@Nullable UUID owner, boolean fresh);
    default ClaimantTown updateTownOwner(@Nullable UUID owner) {
        return this.updateTownOwner(owner, true);
    }
    ClaimantPlayer updatePlayerOwner(@Nullable UUID owner, boolean fresh);
    default ClaimantPlayer updatePlayerOwner(@Nullable UUID owner) {
        return this.updatePlayerOwner(owner, true);
    }
    boolean canPlayerClaim(@NotNull ClaimantPlayer player, boolean stopIfClaimed) throws TranslationKeyException;
    
    @Nullable
    UUID getOwner();
    @Nullable
    UUID getOwner(BlockPos pos);
    @Nullable
    UUID getTownId();
    @Nullable
    ClaimantTown getTown();
    
    default MutableText getOwnerName() {
        return this.getOwnerName((UUID) null);
    }
    default MutableText getOwnerName(@Nullable UUID zonePlayer) {
        UUID owner = this.getOwner();
        if ( owner == null )
            return new LiteralText(SewConfig.get(SewConfig.NAME_WILDERNESS))
                .formatted(Formatting.GREEN);
    
        // Get the owner of the chunk
        ClaimantPlayer chunkPlayer = ClaimantPlayer.get(owner);
    
        // Get the owners name (Colored using the relation to the zonePlayer)
        return chunkPlayer.getName(zonePlayer);
    }
    default MutableText getOwnerName(@NotNull PlayerEntity zonePlayer) {
        return this.getOwnerName(zonePlayer, zonePlayer.getBlockPos());
    }
    default MutableText getOwnerName(@NotNull PlayerEntity zonePlayer, @NotNull BlockPos pos) {
        return this.getOwnerName(zonePlayer.getUuid(), pos);
    }
    default MutableText getOwnerName(@NotNull UUID zonePlayer, @NotNull BlockPos pos) {
        UUID owner = this.getOwner(pos);
        if ( owner == null )
            return new LiteralText(SewConfig.get(SewConfig.NAME_WILDERNESS))
                .formatted(Formatting.GREEN);
        
        // Get the owner of the chunk
        ClaimantPlayer chunkPlayer = ClaimantPlayer.get(owner);
        
        // Get the owners name (Colored using the relation to the zonePlayer)
        return chunkPlayer.getName(zonePlayer);
    }
    
    boolean canPlayerDo(@NotNull BlockPos blockPos, @Nullable UUID player, @Nullable ClaimPermissions perm);
    boolean isSetting(@NotNull BlockPos pos, @NotNull ClaimSettings setting);
    
    /*
     * Claim Slices
     */
    @NotNull
    NbtList serializeSlices();
    void deserializeSlices(@NotNull NbtList serialized);
    
    default void updateSliceOwner(@Nullable UUID owner, int slicePos) {
        this.updateSliceOwner(owner, slicePos, 0, 256);
    }
    default void updateSliceOwner(@Nullable UUID owner, int slicePos, int yFrom, int yTo) {
        this.updateSliceOwner(owner, slicePos, yFrom, yTo, true);
    }
    void updateSliceOwner(UUID owner, int slicePos, int yFrom, int yTo, boolean fresh);
    UUID[] getSliceOwner(int slicePos, int yFrom, int yTo);
    @NotNull ChunkUtils.ClaimSlice[] getSlices();
    void setSlices(@NotNull ChunkUtils.ClaimSlice[] slices);
    
    /*
     * Statics
     */
    static boolean isOwnedAround(@NotNull final World world, @NotNull final BlockPos blockPos, int leniency) {
        return IClaimedChunk.getOwnedAround( world, blockPos, leniency).length > 0;
    }
    static @NotNull IClaimedChunk[] getOwnedAround(@NotNull final World world, @NotNull final BlockPos blockPos, final int radius) {
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
