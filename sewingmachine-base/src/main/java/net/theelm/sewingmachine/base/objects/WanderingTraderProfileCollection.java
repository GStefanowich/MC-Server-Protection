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
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Created on Dec 02 2021 at 10:20 PM.
 * By greg in SewingMachineMod
 */
public final class WanderingTraderProfileCollection {
    private final @NotNull GameProfile profile;
    private final @NotNull Text name;
    private final boolean show;
    
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
        this.show = show;
    }
    
    public @NotNull PlayerListS2CPacket getPacket() {
        return this.getPacket(new Action[0]);
    }
    public @NotNull PlayerListS2CPacket getPacket(Action... actions) {
        EnumSet<Action> set;
        if (!this.show)
            set = EnumSet.of(Action.UPDATE_LISTED);
        else if (actions.length == 0)
            set = EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_DISPLAY_NAME, Action.UPDATE_LISTED);
        else {
            set = EnumSet.noneOf(Action.class);
            for (Action action : actions)
                set.add(action);
        }
        return this.getPacket(set);
    }
    public @NotNull PlayerListS2CPacket getPacket(@NotNull EnumSet<Action> actions) {
        PacketByteBuf mimic = new PacketByteBuf(Unpooled.buffer());
        mimic.writeEnumSet(actions, Action.class);
        
        mimic.writeCollection(Collections.singleton(this.profile), (buf, profile) -> {
            buf.writeUuid(profile.getId());
            for (Action action : actions) {
                switch (action) {
                    case ADD_PLAYER: {
                        buf.writeString(profile.getName(), 16);
                        buf.writePropertyMap(profile.getProperties());
                        break;
                    }
                    case UPDATE_LISTED: {
                        buf.writeBoolean(this.show);
                        break;
                    }
                    case UPDATE_DISPLAY_NAME: {
                        buf.writeNullable(this.name, PacketByteBuf::writeText);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException("No handler for mimicking Action packet type " + action);
                }
            }
        });
        
        return new PlayerListS2CPacket(mimic);
    }
}
