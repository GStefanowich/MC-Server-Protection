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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.base.objects.PlayerBackpack;
import net.theelm.sewingmachine.utilities.Sew;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created on Jul 03 2023 at 7:36 PM.
 * By greg in sewingmachine
 */
public record PlayerBackpackDataPacket(int rows, Collection<Identifier> items) implements FabricPacket {
    public static final PacketType<PlayerBackpackDataPacket> TYPE = PacketType.create(Sew.modIdentifier("backpack_data"), PlayerBackpackDataPacket::new);
    
    public PlayerBackpackDataPacket(@Nullable PlayerBackpack backpack) {
        this(backpack == null ? 0 : backpack.getRows(), backpack == null ? new ArrayList<>() : backpack.getPickupIdentifiers());
    }
    
    public PlayerBackpackDataPacket(@NotNull PacketByteBuf buf) {
        this(buf.readInt(), PlayerBackpackDataPacket.read(buf));
    }
    
    @Override
    public void write(@NotNull PacketByteBuf buf) {
        buf.writeInt(this.rows);
        buf.writeInt(this.items.size());
        Iterator<Identifier> iterator = this.items.iterator();
        while (iterator.hasNext())
            buf.writeIdentifier(iterator.next());
    }
    
    private static @NotNull Collection<Identifier> read(@NotNull PacketByteBuf buf) {
        List<Identifier> identifiers = new ArrayList<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++)
            identifiers.add(buf.readIdentifier());
        return identifiers;
    }
    
    public @Nullable PlayerBackpack getBackpack(@NotNull PlayerEntity player) {
        if (this.rows == 0)
            return null;
        PlayerBackpack backpack = new PlayerBackpack(player, this.rows);
        
        // Read the tags
        backpack.readPickupTags(this.items);
        
        return backpack;
    }
    
    @Override
    public PacketType<?> getType() {
        return PlayerBackpackDataPacket.TYPE;
    }
}
