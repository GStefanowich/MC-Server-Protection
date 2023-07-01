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

package net.theelm.sewingmachine.deathchests.mixins.Entities;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.deathchests.interfaces.PlayerCorpse;
import net.theelm.sewingmachine.base.objects.PlayerBackpack;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.UUID;

/**
 * Created on Jun 25 2023 at 7:58 PM.
 * By greg in sewingmachine
 */
@Mixin(value = ArmorStandEntity.class, priority = 10000)
public abstract class ArmorStandEntityMixin extends LivingEntity implements PlayerCorpse {
    private UUID corpsePlayerUUID = null;
    private NbtList corpsePlayerItems = null;
    private NbtList corpsePlayerBackpack = null;
    
    protected ArmorStandEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Inject(at = @At("HEAD"), method = "interactAt", cancellable = true)
    public void onPlayerInteract(PlayerEntity player, Vec3d vec3d, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        // Armor Stand is a corpse
        if (this.corpsePlayerUUID != null) {
            // Return the items back to their owner
            if (EntityUtils.canEntityTakeDeathChest(player, this.corpsePlayerUUID))
                this.returnItemsToPlayer(player);
            else {
                // Deny if the corpse does not belong to this player
                callback.setReturnValue(ActionResult.FAIL);
            }
        }
    }
    
    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // If the corpse belongs to the player
        if ( (this.corpsePlayerUUID != null) && player.getUuid().equals(this.corpsePlayerUUID)) {
            this.returnItemsToPlayer(player);
            return;
        }
        // Regular collision
        super.onPlayerCollision(player);
    }
    
    @Override
    public void setCorpseData(UUID owner, NbtList inventory, NbtList backpack) {
        this.corpsePlayerUUID = owner;
        this.corpsePlayerItems = inventory;
        this.corpsePlayerBackpack = backpack;
    }
    private void giveCorpseItems(@NotNull final PlayerEntity player) {
        Iterator<NbtElement> items;
        
        // Get all of the items to give back
        if (this.corpsePlayerItems != null) {
            items = this.corpsePlayerItems.iterator();
            while (items.hasNext()) {
                // Create the item from the tag
                ItemStack itemStack = ItemStack.fromNbt((NbtCompound) items.next());
                
                // Try equipping the item if the slot is available
                EquipmentSlot slot = null;
                
                if (itemStack.getItem() instanceof ArmorItem && !EnchantmentHelper.hasBindingCurse(itemStack)) {
                    // If armor
                    slot = ((ArmorItem) itemStack.getItem()).getSlotType();
                } else if (itemStack.getItem().equals(Items.SHIELD)) {
                    // If shield
                    slot = EquipmentSlot.OFFHAND;
                }
                
                // If slot is set, equip it there (If allowed)
                if ((slot != null) && player.getEquippedStack(slot).getItem() == Items.AIR)
                    player.equipStack(slot, itemStack);
                else // Add to the inventory (If not equipped)
                    player.getInventory().offerOrDrop(itemStack);
                
                // Remove from the iterator (Tag list)
                items.remove();
            }
        }
        
        // Get all of the backpack items back
        if (this.corpsePlayerBackpack != null) {
            items = this.corpsePlayerBackpack.iterator();
            while (items.hasNext()) {
                // Create the item from the tag
                ItemStack itemStack = ItemStack.fromNbt((NbtCompound) items.next());
                PlayerBackpack backpack = ((BackpackCarrier) player).getBackpack();
                
                // Attempt to put items into the backpack
                if ((backpack == null) || (!backpack.insertStack(itemStack)))
                    player.getInventory().offerOrDrop(itemStack);
                
                // Remove from the iterator (Tag list)
                items.remove();
            }
        }
    }
    private void returnItemsToPlayer(@NotNull final PlayerEntity player) {
        // Give the items back to the player
        this.giveCorpseItems(player);
        
        if (((this.corpsePlayerItems == null) || this.corpsePlayerItems.isEmpty()) && ((this.corpsePlayerBackpack == null) || this.corpsePlayerBackpack.isEmpty())) {
            BlockPos blockPos = this.getBlockPos().up();
            
            // Play sound
            player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
            
            // Spawn particles
            ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                blockPos.getX() + 0.5D,
                blockPos.getY() + 1.0D,
                blockPos.getZ() + 0.5D,
                150,
                1.0D,
                0.5D,
                1.0D,
                0.3D
            );
            
            // Remove the armor stand
            this.discard();
        }
    }
    
    @Inject(at=@At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(NbtCompound tag, CallbackInfo callback) {
        // Save the player warp location for restarts
        if ( this.corpsePlayerUUID != null ) {
            tag.putUuid("corpsePlayerUUID", this.corpsePlayerUUID);
            if ((this.corpsePlayerItems != null) && (!this.corpsePlayerItems.isEmpty()))
                tag.put("corpsePlayerItems", this.corpsePlayerItems);
            if ((this.corpsePlayerBackpack != null) && (!this.corpsePlayerBackpack.isEmpty()))
                tag.put("corpsePlayerBackpack", this.corpsePlayerBackpack);
        }
    }
    @Inject(at=@At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(NbtCompound tag, CallbackInfo callback) {
        if ( NbtUtils.hasUUID(tag, "corpsePlayerUUID") ) {
            this.corpsePlayerUUID = NbtUtils.getUUID(tag, "corpsePlayerUUID");
            if (tag.contains("corpsePlayerItems", NbtElement.LIST_TYPE))
                this.corpsePlayerItems = tag.getList("corpsePlayerItems", NbtElement.COMPOUND_TYPE);
            if (tag.contains("corpsePlayerBackpack", NbtElement.LIST_TYPE))
                this.corpsePlayerBackpack = tag.getList("corpsePlayerBackpack", NbtElement.COMPOUND_TYPE);
        }
    }
}
