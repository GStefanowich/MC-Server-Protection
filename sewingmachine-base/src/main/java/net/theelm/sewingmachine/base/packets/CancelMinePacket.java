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

package net.theelm.sewingmachine.base.packets;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.utilities.mod.Sew;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Jul 01 2023 at 2:25 PM.
 * By greg in sewingmachine
 */
public record CancelMinePacket(@NotNull BlockPos pos) implements FabricPacket {
    public static final PacketType<CancelMinePacket> TYPE = PacketType.create(Sew.modIdentifier("cancel_mining"), CancelMinePacket::new);
    
    public CancelMinePacket(PacketByteBuf buf) {
        this(buf.readBlockPos());
    }
    
    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }
    
    @Override
    public PacketType<?> getType() {
        return CancelMinePacket.TYPE;
    }
}
