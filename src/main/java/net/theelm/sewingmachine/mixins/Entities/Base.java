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

package net.theelm.sewingmachine.mixins.Entities;

import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.entity.EntityLike;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created on Jun 13 2021 at 11:26 PM.
 * By greg in SewingMachineMod
 */
@Mixin(Entity.class)
public abstract class Base implements EntityLike {
    /**
     * When an entity is hit by a lightning bolt, pass the lightning bolt entity as the damage source
     * Vanilla does not pass the LightningEntity and therefore it is impossible to know the source of the lightning
     * Lightning can be cast by players with the trident enchantment and is not always natural
     * @param self This entity
     * @param source The initial damage source (An anonymous "lightningBolt" DamageSource)
     * @param damage The damage amount caused by the lightning bolt
     * @param world The world the lightning is in
     * @param lightning The lightning entity that hit this entity
     * @return If the damage should be applied
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/entity/Entity.damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"), method = "onStruckByLightning")
    public boolean onLightningHit(@NotNull Entity self, DamageSource source, float damage, ServerWorld world, LightningEntity lightning) {
        return self.damage(new EntityDamageSource("lightningBolt", lightning), damage);
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
            .interact((Entity)(Object) this, world, new EntityDamageSource("lightningBolt", lightning));
        if (result != ActionResult.PASS && result != ActionResult.SUCCESS)
            callback.cancel();
    }
    
    /**
     * When an entity mounts a player, notify the "mount" player of the mounting
     * @param entity The mounting entity
     * @param callback The mixin callback information
     */
    @Inject(at = @At("TAIL"), method = "addPassenger")
    private void onAddPassenger(@NotNull Entity entity, @NotNull CallbackInfo callback) {
        if (this.isPlayer()) {
            ServerPlayerEntity self = ((ServerPlayerEntity)(EntityLike)this);
            self.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(self));
        }
    }
    
    /**
     * When an entity dismounts off of a player, notify the "mount" player of the dismounting
     * @param entity The dismounting entity
     * @param callback The mixin callback information
     */
    @Inject(at = @At("TAIL"), method = "removePassenger")
    private void onRemovePassenger(@NotNull Entity entity, @NotNull CallbackInfo callback) {
        if (this.isPlayer()) {
            ServerPlayerEntity self = ((ServerPlayerEntity)(EntityLike)this);
            self.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(self));
        }
    }
}
