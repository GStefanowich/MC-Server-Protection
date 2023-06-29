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

package net.theelm.sewingmachine.base.objects;

import com.mojang.authlib.GameProfile;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.UUID;

/**
 * Created on Dec 02 2021 at 10:20 PM.
 * By greg in SewingMachineMod
 */
public class WanderingTraderProfileCollection implements Collection<ServerPlayerEntity> {
    private final @NotNull EmptyWanderingTraderIterator iterator = new EmptyWanderingTraderIterator();
    private final @NotNull PlayerListS2CPacket.Entry entry;
    private final @NotNull GameProfile profile;
    private final @NotNull Text name;
    
    public WanderingTraderProfileCollection() {
        this(null);
    }
    public WanderingTraderProfileCollection(@Nullable Entity entity) {
        this(entity instanceof WanderingTraderEntity trader ? EntityUtils.wanderingTraderDepartureTime(trader) : Text.literal("Mob").formatted(Formatting.RED), entity != null);
    }
    private WanderingTraderProfileCollection(@NotNull Text text, boolean show) {
        this.profile = new GameProfile(UUID.fromString("bd482739-767c-45dc-a1f8-c33c40530952"), "MHF_VILLAGER");
        this.name = Text.literal("").formatted(Formatting.WHITE)
            .append(Text.translatable(EntityType.WANDERING_TRADER.getTranslationKey()).formatted(Formatting.BLUE))
            .append(" - ")
            .append(text);
        
        this.entry = new PlayerListS2CPacket.Entry(this.profile.getId(), this.profile, show, 0, GameMode.DEFAULT, this.name, null);
    }
    
    public @NotNull PlayerListS2CPacket getPacket(PlayerListS2CPacket.Action action) {
        EnumSet<PlayerListS2CPacket.Action> set = EnumSet.of(action);
        PlayerListS2CPacket packet = new PlayerListS2CPacket(set, this);
        packet.getEntries()
            .add(this.entry);
        return packet;
    }
    
    @Override
    public int size() { return 1; }
    
    @Override
    public boolean isEmpty() { return false; }
    
    @Override
    public boolean contains(Object o) { return false; }
    
    @Override
    public @NotNull Iterator<ServerPlayerEntity> iterator() { return this.iterator; }
    
    @Override
    public @NotNull Object[] toArray() { return new Object[0]; }
    
    @Override
    public @NotNull <T> T[] toArray(@NotNull T[] ts) { return (T[])this.toArray(); }
    
    @Override
    public boolean add(ServerPlayerEntity serverPlayerEntity) { return false; }
    
    @Override
    public boolean remove(Object o) { return false; }
    
    @Override
    public boolean containsAll(@NotNull Collection<?> collection) { return false; }
    
    @Override
    public boolean addAll(@NotNull Collection<? extends ServerPlayerEntity> collection) { return false; }
    
    @Override
    public boolean removeAll(@NotNull Collection<?> collection) { return false; }
    
    @Override
    public boolean retainAll(@NotNull Collection<?> collection) { return false; }
    
    @Override
    public void clear() {}
    
    private static class EmptyWanderingTraderIterator implements Iterator<ServerPlayerEntity> {
        @Override
        public boolean hasNext() { return false; }
        
        @Override
        public ServerPlayerEntity next() { return null; }
    }
}
