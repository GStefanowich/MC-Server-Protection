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

package net.TheElm.project.objects.ticking;

import net.TheElm.project.CoreMod;
import net.TheElm.project.interfaces.TickableContext;
import net.TheElm.project.interfaces.TickingAction;
import net.TheElm.project.mixins.Interfaces.InteractionManagerAccessor;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created on Aug 29 2021 at 12:46 PM.
 * By greg in SewingMachineMod
 */
@Deprecated
public class PlayerNoBreak implements TickingAction {
    private static final @NotNull StatusEffectInstance FATIGUE = new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 2, 3);
    
    private final @NotNull ServerPlayerEntity player;
    private final @NotNull BlockPos blockPos;
    
    public PlayerNoBreak(@NotNull ServerPlayerEntity player, @NotNull BlockPos blockPos) {
        this.player = player;
        this.blockPos = blockPos;
    }
    
    @Override
    public boolean isCompleted(@NotNull TickableContext tickable) {
        if (tickable.isRemoved() || this.isPlayerGone())
            return true;
        if (tickable.getTicks() % 20 != 0)
            return false;
        
        // Send a no-breaking packet
        return !this.sendPacket(this.isPlayerBreaking());
    }
    
    public boolean isPlayerGone() {
        return this.player.isDisconnected()
            || this.player.isSpectator()
            || this.player.isCreative();
    }
    public boolean isPlayerBreaking() {
        InteractionManagerAccessor accessor = (InteractionManagerAccessor) player.interactionManager;
        return this.player.handSwinging
            && Objects.equals(accessor.miningPos(), this.blockPos);
    }
    
    private boolean sendPacket(final boolean isMining) {
        CoreMod.logInfo("Sending a packet");
        if (!this.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            if (isMining)
                this.sendPacket(new EntityStatusEffectS2CPacket(this.player.getId(), PlayerNoBreak.FATIGUE));
            else this.sendPacket(new RemoveEntityStatusEffectS2CPacket(this.player.getId(), StatusEffects.MINING_FATIGUE));
        }
        return isMining;
    }
    private void sendPacket(@NotNull Packet<?> packet) {
        this.player.networkHandler.sendPacket(packet);
    }
}
