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

import net.TheElm.project.interfaces.IClaimedChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ChunkSerializer.class)
public class ChunkSaving {
    
    private static final String sewingMachineSerializationPlayer = "sewingMachineOwnerUUID";
    private static final String sewingMachineSerializationTown = "sewingMachineTownUUID";
    
    @Inject(at = @At("TAIL"), method = "serialize")
    private static void saveSewingOwner(ServerWorld world, Chunk chunk, CallbackInfoReturnable<CompoundTag> callback) {
        CompoundTag mainTag = callback.getReturnValue();
        CompoundTag levelTag = mainTag.getCompound( "Level" );
        
        // Only add the chunks if they're an ownable chunk
        if ( chunk instanceof WorldChunk ) {
            // If the chunk is Claimed, save that players ID
            UUID player, town;
            
            // Save the chunks owned-player
            if ((player = ((IClaimedChunk) chunk).getOwner()) != null)
                levelTag.putUuid(sewingMachineSerializationPlayer, player);
            
            // Save the chunks town
            if ((town = ((IClaimedChunk) chunk).getTownId()) != null)
                levelTag.putUuid(sewingMachineSerializationTown, town);
        }
    }
    
    @Inject(at = @At("RETURN"), method = "writeEntities")
    private static void loadSewingOwner(CompoundTag levelTag, WorldChunk chunk, CallbackInfo callback) {
        // Update the chunks player-owner
        if ( levelTag.hasUuid(sewingMachineSerializationPlayer) )
            ((IClaimedChunk) chunk).updatePlayerOwner(levelTag.getUuid(sewingMachineSerializationPlayer));
        // Update the chunks town
        if ( levelTag.hasUuid(sewingMachineSerializationTown) )
            ((IClaimedChunk) chunk).updateTownOwner(levelTag.getUuid(sewingMachineSerializationTown));
    }
    
}
