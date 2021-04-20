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

package net.TheElm.project.mixins.Player.Interaction;

import net.TheElm.project.interfaces.DamageEntityCallback;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Collections;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrames extends AbstractDecorationEntity {
    
    protected ItemFrames(EntityType<? extends AbstractDecorationEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    public boolean handleAttack(Entity entity) {
        if (entity instanceof PlayerEntity) {
            ActionResult result = DamageEntityCallback.EVENT.invoker().interact(this, this.getEntityWorld(), DamageSource.player((PlayerEntity) entity), 0.0f);
            if (result != ActionResult.PASS)
                return result == ActionResult.FAIL;
        }
        return super.handleAttack(entity);
    }
    
    @Inject(at = @At("HEAD"), method = "interact", cancellable = true)
    public void onPlayerInteract(final PlayerEntity player, final Hand hand, final @NotNull CallbackInfoReturnable<ActionResult> callback) {
        ItemFrameEntity entity = (ItemFrameEntity)(AbstractDecorationEntity) this;
        
        // Do special item frame interaction if NOT CROUCHING and HOLDING A TOOL
        if (( entity instanceof ItemFrameEntity) && (!( player.isSneaking() && player.getStackInHand( hand ).isDamageable() ))) {
            ItemFrameEntity itemFrame = (ItemFrameEntity) entity;
            Direction direction = itemFrame.getHorizontalFacing().getOpposite();
            
            // Get the item in the item frame
            ItemStack itemStack = itemFrame.getHeldItemStack();
            
            // Get blocks
            BlockPos containerPos = itemFrame.getBlockPos().offset(direction, 1);
            Block containerBlock = this.world.getBlockState(containerPos).getBlock();
            
            // If the block behind the item frame is a storage
            if ( containerBlock instanceof ChestBlock || containerBlock instanceof BarrelBlock ) {
                // Check chunk permissions
                if (!ChunkUtils.canPlayerLootChestsInChunk(player, containerPos)) {
                    callback.setReturnValue(ActionResult.SUCCESS);
                    return;
                }
                
                Inventory chestInventory = InventoryUtils.getInventoryOf(this.world, containerPos);
                if ((chestInventory != null) && (!itemStack.getItem().equals(Items.AIR))) {
                    // The amount the player wants to take
                    int putStackSize = (player.isSneaking() ? Collections.min(Arrays.asList(64, itemStack.getMaxCount())) : 1);
                    
                    InventoryUtils.playerToChest((ServerPlayerEntity) player, itemFrame.getBlockPos(), player.inventory, chestInventory, itemStack.getItem(), putStackSize);
                    callback.setReturnValue(ActionResult.SUCCESS);
                }
            }
        }
        
        // If player should be able to interact with the item frame
        if (!ChunkUtils.canPlayerBreakInChunk(player, this.getBlockPos()))
            callback.setReturnValue(ActionResult.SUCCESS);
    }
    
    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    public void onDamage(@NotNull DamageSource damageSource, float damage, final @NotNull CallbackInfoReturnable<Boolean> callback) {
        Entity attacker = damageSource.getAttacker();
        if (!(attacker instanceof PlayerEntity))
            return;
        PlayerEntity player = (PlayerEntity) attacker;
        
        // If player should be able to interact with the item frame
        if (!ChunkUtils.canPlayerBreakInChunk(player, this.getBlockPos()))
            callback.setReturnValue(false);
    }
    
}
