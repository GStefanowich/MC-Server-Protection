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

package net.TheElm.project.mixins.Player;

import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.objects.PlayerBackpack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Nameable;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class FullInventory implements Inventory, Nameable {
    
    @Shadow public PlayerEntity player;
    
    @Shadow public abstract boolean insertStack(ItemStack itemStack);
    
    @Inject(at = @At("HEAD"), method = "insertStack", cancellable = true)
    public void onInsertStack(ItemStack itemStack, CallbackInfoReturnable<Boolean> callback) {
        PlayerBackpack backpack = ((BackpackCarrier)this.player).getBackpack();
        if ((backpack != null) && backpack.shouldAutoPickup(itemStack) && backpack.insertStack(itemStack) && itemStack.isEmpty())
            callback.setReturnValue( true );
    }
    
    @Inject(at = @At(value = "INVOKE", target = "net/minecraft/entity/player/PlayerEntity.dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;"), method = "offerOrDrop", cancellable = true)
    private void setDropOwner(World world, ItemStack itemStack, CallbackInfo callback) {
        // Drop the item from the player
        ItemEntity drop = this.player.dropItem( itemStack, true );
        
        // Set the dropped items owner
        if ( drop != null ) drop.setOwner(this.player.getUuid());
        
        // Cancel a second drop
        callback.cancel();
    }
    
}
