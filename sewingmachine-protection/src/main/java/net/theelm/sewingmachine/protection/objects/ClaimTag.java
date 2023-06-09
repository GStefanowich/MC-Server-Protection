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

package net.theelm.sewingmachine.protection.objects;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.theelm.sewingmachine.utilities.LegacyConverter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClaimTag extends NbtCompound {
    private ClaimTag() {
        super();
    }
    public ClaimTag(@NotNull Chunk chunk, @NotNull ChunkPos pos) {
        super();
        
        if (chunk instanceof WorldChunk worldChunk) {
            World world = worldChunk.getWorld();
            RegistryKey<World> dimType = world.getRegistryKey();
            if (dimType != null)
                this.putString("dimension", dimType.getValue().toString());
        }
        this.putInt("x", pos.x);
        this.putInt("z", pos.z);
    }
    public ClaimTag(@NotNull World world, @NotNull ChunkPos pos) {
        super();
        
        RegistryKey<World> dimType = world.getRegistryKey();
        if (dimType != null)
            this.putString("dimension", dimType.getValue().toString());
        
        this.putInt("x", pos.x);
        this.putInt("z", pos.z);
    }
    public ClaimTag(@NotNull Chunk chunk) {
        this(chunk, chunk.getPos());
    }
    
    public int getX() {
        return this.getInt("x");
    }
    public int getZ() {
        return this.getInt("z");
    }
    
    public int getLowerX() {
        return this.getX() << 4;
    }
    public int getLowerZ() {
        return this.getZ() << 4;
    }
    public int getUpperX() {
        return this.getLowerX() + 15;
    }
    public int getUpperZ() {
        return this.getLowerZ() + 15;
    }
    
    public @Nullable RegistryKey<World> getDimension() {
        Identifier identifier;
        String dimType = this.getString("dimension");
        if (dimType == null)
            return null;
        if ((identifier = Identifier.tryParse(dimType)) == null)
            return null;
        return RegistryKey.of(RegistryKeys.WORLD, identifier);
    }
    
    public static @Nullable ClaimTag fromCompound(@NotNull NbtCompound compoundTag) {
        if (!(compoundTag.contains("dimension", NbtElement.STRING_TYPE) && compoundTag.contains("x", NbtElement.INT_TYPE) && compoundTag.contains("z", NbtElement.INT_TYPE)))
            return null;
        else {
            ClaimTag tag = new ClaimTag();
            
            tag.putString("dimension", compoundTag.getString("dimension"));
            tag.putInt("x", compoundTag.getInt("x"));
            tag.putInt("z", compoundTag.getInt("z"));
            
            return tag;
        }
    }
    public static @Nullable ClaimTag fromArray(@NotNull NbtIntArray arrayTag) {
        int[] array = arrayTag.getIntArray();
        ClaimTag tag = new ClaimTag();
        
        RegistryKey<World> world = LegacyConverter.getWorldFromId((byte) array[0]);
        if (world == null)
            return null;
        
        tag.putString("dimension", world.getValue().toString());
        tag.putInt("x", array[1]);
        tag.putInt("z", array[2]);
        
        return tag;
    }
    
    public static @NotNull ClaimTag of(@NotNull Chunk chunk) {
        return new ClaimTag(chunk);
    }
    public static @NotNull ClaimTag of(@NotNull World world, @NotNull ChunkPos chunkPos) {
        return new ClaimTag(world, chunkPos);
    }
}
