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

package net.TheElm.project.objects;

import net.TheElm.project.utilities.LegacyConverter;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClaimTag extends CompoundTag {
    
    public ClaimTag() {
        super();
    }
    public ClaimTag(@NotNull World world, @NotNull ChunkPos chunk) {
        super();
        
        RegistryKey dimType = world.getRegistryKey();
        if (dimType != null)
            this.putString("dimension", dimType.toString());
        this.putInt("x", chunk.x);
        this.putInt("z", chunk.z);
    }
    public ClaimTag(WorldChunk chunk) {
        this(chunk.getWorld(), chunk.getPos());
    }
    
    public int getX() {
        return this.getInt("x");
    }
    public int getZ() {
        return this.getInt("z");
    }
    
    public @Nullable RegistryKey<World> getDimension() {
        String dimType = this.getString("dimension");
        if (dimType == null) return null;
        return RegistryKey.of(Registry.DIMENSION, new Identifier(dimType));
    }
    
    public static @Nullable ClaimTag fromCompound(CompoundTag compoundTag) {
        if (!(compoundTag.contains("dimension", NbtType.STRING) && compoundTag.contains("x", NbtType.INT) && compoundTag.contains("z", NbtType.INT)))
            return null;
        else {
            ClaimTag tag = new ClaimTag();
            
            tag.putString("dimension", compoundTag.getString("dimension"));
            tag.putInt("x", compoundTag.getInt("x"));
            tag.putInt("z", compoundTag.getInt("z"));
            
            return tag;
        }
    }
    public static @Nullable ClaimTag fromArray(IntArrayTag arrayTag) {
        int[] array = arrayTag.getIntArray();
        ClaimTag tag = new ClaimTag();
        
        RegistryKey<World> world = LegacyConverter.getWorldFromId((byte) array[0]);
        if (world == null) return null;
        
        tag.putString("dimension", world.toString());
        tag.putInt("x", array[1]);
        tag.putInt("z", array[2]);
        
        return tag;
    }
}
