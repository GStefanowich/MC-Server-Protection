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

package net.theelm.sewingmachine.protection.mixins.Client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.protection.interfaces.ClientMiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(MinecraftClient.class)
public abstract class ClientPlayerInteractionManagerMixin implements ClientMiner {
    private BlockPos lastFailPos = null;
    
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    
    @Override
    public void stopMining(@NotNull BlockPos pos) {
        this.lastFailPos = pos;
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;"), method = "doAttack")
    private HitResult.Type onDoAttack(HitResult result) {
        if (this.lastFailPos != null) {
            if (
                result instanceof BlockHitResult blockHit
                && Objects.equals(blockHit.getBlockPos(), this.lastFailPos)
            ) return HitResult.Type.MISS;
            else this.lastFailPos = null;
        }
        
        return result.getType();
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;"), method = "handleBlockBreaking")
    private HitResult.Type onhandleBlockBreaking(HitResult result) {
        if (this.lastFailPos != null) {
            if (
                result instanceof BlockHitResult blockHit
                    && Objects.equals(blockHit.getBlockPos(), this.lastFailPos)
            ) return HitResult.Type.MISS;
            else this.lastFailPos = null;
        }
        
        return result.getType();
    }
}
