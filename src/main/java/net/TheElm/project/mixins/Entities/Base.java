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

package net.TheElm.project.mixins.Entities;

import net.TheElm.project.interfaces.DamageEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created on Jun 13 2021 at 11:26 PM.
 * By greg in SewingMachineMod
 */
@Mixin(Entity.class)
public class Base {
    @Shadow
    public native World getEntityWorld();
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/entity/Entity.damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), method = "onStruckByLightning")
    public boolean onLightningHit(@NotNull Entity self, DamageSource source, float damage, ServerWorld world, LightningEntity lightning) {
        return self.damage(new EntityDamageSource("lightningBolt", lightning), damage);
    }
    
    @Inject(at = @At("HEAD"), method = "onStruckByLightning", cancellable = true)
    public void onLightningHit(ServerWorld world, LightningEntity lightning, CallbackInfo callback) {
        ActionResult result = DamageEntityCallback.EVENT.invoker()
            .interact((Entity)(Object) this, this.getEntityWorld(), new EntityDamageSource("lightningBolt", lightning), 0F);
        if (result != ActionResult.PASS && result != ActionResult.SUCCESS)
            callback.cancel();
    }
}
