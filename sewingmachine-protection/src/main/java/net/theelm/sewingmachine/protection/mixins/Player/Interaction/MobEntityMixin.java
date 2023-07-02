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

package net.theelm.sewingmachine.protection.mixins.Player.Interaction;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.EntityLockUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {
    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Nullable
    private UUID getOwnerUuid() {
        if ( ((LivingEntity) this) instanceof TameableEntity tameableEntity )
            return tameableEntity.getOwnerUuid();
        if ( ((LivingEntity) this) instanceof AbstractHorseEntity horseBaseEntity )
            return horseBaseEntity.getOwnerUuid();
        return null;
    }
    
    /**
     * Control attaching of leashes to this mob
     * @param player The entity to attach to
     * @param hand The hand the player interacted with
     * @param callback The callback
     */
    @Inject(at = @At("HEAD"), method = "interactWithItem", cancellable = true)
    private void onAttachLeash(@NotNull final PlayerEntity player, @NotNull final Hand hand, @NotNull final CallbackInfoReturnable<ActionResult> callback) {
        // If player is in creative mode, bypass permissions
        if ((player.isCreative() && SewConfig.get(SewCoreConfig.CLAIM_CREATIVE_BYPASS)) || player.isSpectator())
            return;
        
        // If player is the owner of the entity
        if ((this.getOwnerUuid() != null) && (player.getUuid().equals(this.getOwnerUuid())))
            return;
        
        // If player can interact with tameable
        if (ClaimChunkUtils.canPlayerInteractFriendlies(player, this.getBlockPos()))
            return;
        
        ActionResult result;
        ItemStack stack = player.getStackInHand(hand);
        
        if (!(stack.getItem() == Items.LEAD || stack.getItem() == Items.NAME_TAG))
            result = ActionResult.PASS;
        else {
            result = ActionResult.CONSUME;
            
            EntityLockUtils.playLockSoundFromSource(this, player);
            
            // Make sure the client knows that they are not leashing
            if (player instanceof ServerPlayerEntity && stack.getItem() == Items.LEAD) {
                EntityUtils.resendInventory(player);
                
                ((ServerPlayerEntity) player).networkHandler.sendPacket(
                    new EntityAttachS2CPacket(this, null)
                );
            }
        }
        
        // Decline allowing leading
        callback.setReturnValue(result);
    }
}
