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
import net.theelm.sewingmachine.utilities.Sew;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Jul 03 2023 at 5:15 PM.
 * By greg in sewingmachine
 */
public record SewHelloPacket(@NotNull Map<String, String> modules) implements FabricPacket {
    public static final PacketType<SewHelloPacket> TYPE = PacketType.create(Sew.modIdentifier("hello"), SewHelloPacket::new);
    
    public SewHelloPacket(@NotNull PacketByteBuf buf) {
        this(mapFromPacket(buf));
    }
    
    @Override
    public void write(@NotNull PacketByteBuf buf) {
        buf.writeInt(this.modules.size());
        for (Map.Entry<String, String> entry : this.modules.entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeString(entry.getValue());
        }
    }
    
    @Override
    public PacketType<?> getType() {
        return SewHelloPacket.TYPE;
    }
    
    private static @NotNull Map<String, String> mapFromPacket(@NotNull PacketByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> modules = new HashMap<>(size);
        for (int i = 0; i < size; i++)
            modules.put(buf.readString(), buf.readString());
        
        return modules;
    }
}
