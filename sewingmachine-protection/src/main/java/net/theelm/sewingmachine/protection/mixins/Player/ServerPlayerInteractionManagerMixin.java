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

package net.theelm.sewingmachine.protection.mixins.Player;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.theelm.sewingmachine.events.BlockBreakCallback;
import net.theelm.sewingmachine.utilities.BlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayerInteractionManager.class, priority = 10000)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    private BlockPos miningPos;
    
    @Shadow public ServerWorld world;
    @Shadow public ServerPlayerEntity player;
    
    @Shadow protected abstract void method_41250(BlockPos pos, boolean success, int sequence, String reason);
    
    @Inject(at = @At("HEAD"), method = "processBlockBreakingAction", cancellable = true)
    private void onBlockBreakChange(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight, int sequence, CallbackInfo callback) {
        if ( !BlockBreakCallback.canDestroy(this.player, this.world, this.player.preferredHand, pos, direction, action) ) {
            // Send the player a failed notice
            this.world.setBlockBreakingInfo(this.player.getId(), pos, -1);
            this.method_41250(pos, true, sequence, "may not interact");
            
            // Update the neighboring blocks on the client
            BlockUtils.updateNeighboringBlockStates(this.player, this.world, pos);
            
            // Update the players mining position
            // Must to set to prevent (Block Mismatch) messages
            this.miningPos = pos;
            
            // Create a slow mining event
            //((LogicalWorld)this.world).addTickableEvent(new PlayerNoBreak(this.player, blockPos));
            
            // Cancel the rest of the event
            callback.cancel();
        }
    }
}
