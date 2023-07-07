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
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.utilities.Sew;
import org.jetbrains.annotations.NotNull;

public record ClaimCountPacket(int current, int maximum) implements FabricPacket {
    public static PacketType<ClaimCountPacket> TYPE = PacketType.create(Sew.modIdentifier("claim_count"), ClaimCountPacket::new);
    
    public ClaimCountPacket(@NotNull PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }
    public ClaimCountPacket(@NotNull ClaimantPlayer claimant) {
        this(claimant.getCount(), claimant.getMaxChunkLimit());
    }
    
    @Override
    public void write(@NotNull PacketByteBuf buf) {
        buf.writeInt(this.current);
        buf.writeInt(this.maximum);
    }
    
    @Override
    public PacketType<?> getType() {
        return ClaimCountPacket.TYPE;
    }
}
