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

package net.theelm.sewingmachine.base.mixins.Player.Interaction;

import net.minecraft.text.Text;
import net.theelm.sewingmachine.enums.ArmorStandPose;
import net.theelm.sewingmachine.utilities.CasingUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin extends LivingEntity {
    @Shadow public native boolean shouldShowArms();
    @Shadow private native void setShowArms( boolean show );
    
    @Shadow public native boolean shouldHideBasePlate();
    @Shadow private native void setHideBasePlate(boolean bl);
    
    protected ArmorStandEntityMixin(EntityType<? extends LivingEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    /*
     * Armor Stand Modifications
     */
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/block/Block.dropStack(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V"), method = "breakAndDropItem")
    private void onDropSelf(World world, BlockPos blockPos, ItemStack itemStack) {
        boolean showArms = this.shouldShowArms();
        boolean hidePlate = this.shouldHideBasePlate();
        
        if (showArms || hidePlate) {
            NbtCompound name = itemStack.getOrCreateSubNbt("display");
            NbtCompound enti = itemStack.getOrCreateSubNbt("EntityTag");
            enti.putBoolean("ShowArms", showArms);
            enti.putBoolean("NoBasePlate", hidePlate);
            
            NbtList lore = new NbtList();
            lore.add(NbtString.of("{\"text\":\""
                + String.join(", ", Arrays.asList(
                    ( hidePlate ? "No base" : "Base" ),
                    ( showArms ? "Arms" : "No Arms" )
                ))
                + "\",\"color\":\"gray\"}"));
            
            name.put("Lore", lore);
        }
        
        Block.dropStack(world, blockPos, itemStack);
    }
    
    /*
     * Armor Stand Protections
     */
    
    @Inject(at = @At("HEAD"), method = "interactAt", cancellable = true)
    public void onPlayerInteract(PlayerEntity player, Vec3d vec3d, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        ItemStack handStack = player.getStackInHand(hand);
        
        if (!this.shouldHideBasePlate()) {
            if (player.isSneaking() && handStack.isEmpty() && (vec3d.y < 0.5D)) {
                this.setHideBasePlate(true);
                
                this.dropStack(new ItemStack(Items.SMOOTH_STONE_SLAB, 1));
                
                callback.setReturnValue(ActionResult.SUCCESS);
                return;
            }
        } else if (handStack.getItem().equals(Items.SMOOTH_STONE_SLAB)) {
            handStack.decrement(1);
            this.setHideBasePlate(false);
            
            callback.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        
        // Take away the arms
        if (this.shouldShowArms()) {
            if (!player.isSneaking() && handStack.getItem().equals(Items.STICK)) {
                // Get the next pose after the current
                ArmorStandPose pose = ArmorStandPose.getCurrent(this)
                    .next();
                
                // Apply that pose
                ArmorStandPose.apply(pose, this);
                
                // Tell the player
                player.sendMessage(Text.literal(CasingUtils.words(pose.name()
                    .replace('_', ' '))).formatted(Formatting.YELLOW), true);
                
                // Eat the interaction
                callback.setReturnValue(ActionResult.SUCCESS);
            } else if (player.isSneaking() && handStack.isEmpty()) {
                this.setShowArms(false);
                
                ItemStack mainHand = this.getStackInHand(Hand.MAIN_HAND);
                ItemStack offHand = this.getStackInHand(Hand.OFF_HAND);
                
                // Remove items from hands
                if (!mainHand.isEmpty()) {
                    this.dropStack(mainHand);
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                }
                if (!offHand.isEmpty()) {
                    this.dropStack(offHand);
                    this.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                }
                
                // Drop the arms on the ground
                this.dropStack(new ItemStack(Items.STICK, 2));
                
                callback.setReturnValue(ActionResult.SUCCESS);
            }
        }
        
        // Add arms
        else if (handStack.getItem().equals(Items.STICK) && (handStack.getCount() >= 2)) {
            handStack.decrement(2);
            this.setShowArms(true);
            
            callback.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
