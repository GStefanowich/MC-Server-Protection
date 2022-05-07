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

package net.theelm.sewingmachine.mixins.World;

import net.theelm.sewingmachine.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ChunkSerializer.class)
public class ChunkSaving {
    private static final String sewingMachineSerializationPlayer = "sewingMachineOwnerUUID";
    private static final String sewingMachineSerializationTown = "sewingMachineTownUUID";
    private static final String sewingMachineSerializationSlices = "sewingMachineOwnerSlices";
    
    @Inject(at = @At("RETURN"), method = "serialize")
    private static void saveSewingOwner(@NotNull ServerWorld world, @NotNull Chunk chunk, @NotNull CallbackInfoReturnable<NbtCompound> callback) {
        NbtCompound levelTag = callback.getReturnValue();
        
        // Save the chunks owned-player
        UUID player = ((IClaimedChunk)chunk).getOwnerId();
        if (player != null)
            levelTag.putUuid(sewingMachineSerializationPlayer, player);
        
        NbtList slices = ((IClaimedChunk) chunk).serializeSlices();
        
        // Save the inner claims
        levelTag.put(sewingMachineSerializationSlices, slices);
        
        // Save the chunks town
        UUID town = ((IClaimedChunk)chunk).getTownId();
        if (town != null)
            levelTag.putUuid(sewingMachineSerializationTown, town);
    }
    
    @Inject(at = @At("RETURN"), method = "deserialize")
    private static void loadSewingOwner(@NotNull ServerWorld world, @NotNull PointOfInterestStorage poiStorage, @NotNull ChunkPos chunkPos, @NotNull NbtCompound levelTag, @NotNull CallbackInfoReturnable<ProtoChunk> callback) {
        Chunk chunk = callback.getReturnValue();
        if (chunk instanceof ReadOnlyChunk readOnlyChunk)
            chunk = readOnlyChunk.getWrappedChunk();
        
        // Update the chunks player-owner
        if ( NbtUtils.hasUUID(levelTag, sewingMachineSerializationPlayer) )
            ((IClaimedChunk) chunk).updatePlayerOwner(NbtUtils.getUUID(levelTag, sewingMachineSerializationPlayer), false);
        
        // Load the inner claims
        if (levelTag.contains(sewingMachineSerializationSlices, NbtElement.LIST_TYPE))
            ((IClaimedChunk) chunk).deserializeSlices(levelTag.getList(sewingMachineSerializationSlices, NbtElement.COMPOUND_TYPE));
        
        // Update the chunks town
        if ( NbtUtils.hasUUID(levelTag, sewingMachineSerializationTown) )
            ((IClaimedChunk) chunk).updateTownOwner(NbtUtils.getUUID(levelTag, sewingMachineSerializationTown), false);
    }
    
}
