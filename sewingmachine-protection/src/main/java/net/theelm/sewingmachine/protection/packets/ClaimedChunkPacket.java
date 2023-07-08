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

package net.theelm.sewingmachine.protection.packets;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.theelm.sewingmachine.utilities.mod.Sew;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Created on Jul 07 2023 at 1:58 AM.
 * By greg in sewingmachine
 */
public record ClaimedChunkPacket(int x, int z, @Nullable UUID owner, @Nullable Text name) implements FabricPacket {
    public static final PacketType<ClaimedChunkPacket> TYPE = PacketType.create(Sew.modIdentifier("claimed"), ClaimedChunkPacket::new);
    
    public ClaimedChunkPacket(@NotNull ChunkPos chunkPos, @Nullable UUID owner, @Nullable Text name) {
        this(chunkPos.x, chunkPos.z, owner, name);
    }
    public ClaimedChunkPacket(@NotNull PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readNullable(PacketByteBuf::readUuid), buf.readNullable(PacketByteBuf::readText));
    }
    
    public @NotNull ChunkPos chunkPos() {
        return new ChunkPos(this.x, this.z);
    }
    
    @Override
    public void write(@NotNull PacketByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.z);
        buf.writeNullable(this.owner, PacketByteBuf::writeUuid);
        buf.writeNullable(this.name, PacketByteBuf::writeText);
    }
    
    @Override
    public PacketType<?> getType() {
        return ClaimedChunkPacket.TYPE;
    }
}
