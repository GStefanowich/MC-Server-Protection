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

package net.theelm.sewingmachine.protection.mixins.Entities;

import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.world.ServerWorld;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityLike;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = 1)
public abstract class EntityMixin implements EntityLike {
    @Shadow public World world;
    @Shadow public abstract DamageSources getDamageSources();
    
    @Inject(at = @At("HEAD"), method = "isInvulnerableTo", cancellable = true)
    public void onDamage(@NotNull DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        ActionResult result = DamageEntityCallback.EVENT.invoker()
            .interact((Entity)(EntityLike) this, this.world, source);
        if (result != ActionResult.PASS)
            callback.setReturnValue(result != ActionResult.SUCCESS);
    }
    
    /**
     * Run a permission check if the specific lightning entity is allowed to cause damage to the entity
     * @param world The world the lightning is in
     * @param lightning The lightning entity that hit the this entity
     * @param callback The mixin callback information
     */
    @Inject(at = @At("HEAD"), method = "onStruckByLightning", cancellable = true)
    public void onLightningHit(ServerWorld world, LightningEntity lightning, CallbackInfo callback) {
        ActionResult result = DamageEntityCallback.EVENT.invoker()
            .interact((Entity)(Object) this, world, this.getDamageSources().create(DamageTypes.LIGHTNING_BOLT, lightning, lightning.getChanneler()));
        if (result != ActionResult.PASS && result != ActionResult.SUCCESS)
            callback.cancel();
    }
}
