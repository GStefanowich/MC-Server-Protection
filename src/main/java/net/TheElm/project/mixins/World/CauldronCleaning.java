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

package net.TheElm.project.mixins.World;

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.mixins.Interfaces.PowderBlockAccessor;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(LeveledCauldronBlock.class)
public abstract class CauldronCleaning extends Block {
    
    @Shadow
    public native void setLevel(World world_1, BlockPos blockPos_1, BlockState blockState_1, int int_1);
    
    public CauldronCleaning(Settings block$Settings_1) {
        super(block$Settings_1);
    }
    
    @Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
    public void onEntityCollided(@NotNull BlockState blockState, @NotNull World world, @NotNull BlockPos blockPos, Entity entity, CallbackInfo callback) {
        if (world.isClient() || (!(entity instanceof ItemEntity)))
            return;
        int waterLevel = blockState.get(LeveledCauldronBlock.LEVEL);
        
        ItemEntity colliderEntity = (ItemEntity) entity;
        ItemStack colliderStack = colliderEntity.getStack();
        
        UUID owner = colliderEntity.getThrower();
        if (owner == null)
            return;
        PlayerEntity thrower = world.getPlayerByUuid(owner);
        if (!ChunkUtils.canPlayerLootChestsInChunk(thrower, blockPos))
            return;
        
        // If not a spawner, return
        if (colliderStack.getItem() == Items.SPAWNER && waterLevel >= 3) {// Get person throwing the ingredients
            
            // Get conduit
            ItemStack binderStack = null;
            
            // Search for the emerald block
            List<ItemEntity> list = world.getEntitiesByType(EntityType.ITEM, new Box(blockPos), e -> true);
            for (ItemEntity item : list) {
                if ((item.getThrower() == null) || (!item.getThrower().equals(owner)))
                    continue;
                
                binderStack = item.getStack();
                if (binderStack.getItem() == Items.EMERALD_BLOCK)
                    break;
                
                binderStack = null;
            }
            
            // If no binder was found
            if ((binderStack == null) || (thrower == null))
                return;
            
            // Get the entity IDs on the spawner
            NbtCompound spawnerTag = colliderStack.getOrCreateNbt();
            NbtList entityIds;
            if ((!spawnerTag.contains("EntityIds", NbtType.LIST)) || ((entityIds = spawnerTag.getList("EntityIds", NbtType.STRING)).size() < 2))
                return;
            
            // Remove the first spawn type
            entityIds.remove(0);
            
            // Update the display
            spawnerTag.put("display", NbtUtils.getSpawnerDisplay(entityIds));
            
            // Take the emerald
            binderStack.decrement(1);
            
            // Play a level effect
            ((ServerWorld) world).spawnParticles(ParticleTypes.SPLASH, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, 25, 0.0, 2.0, 0.0, 0.2);
            world.playSound(null, blockPos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1.0f, 1.0f);
            this.setLevel(world, blockPos, blockState, 0);
            
            // Cause the player to pickup the spawner
            thrower.sendPickup(colliderEntity, colliderStack.getCount());
            colliderEntity.remove(Entity.RemovalReason.DISCARDED);
            thrower.getInventory()
                .offerOrDrop(colliderStack);
            
            // Cancel the initial cauldron event
            callback.cancel();
        } else if (SewConfig.get(SewConfig.CAULDRON_HARDEN) && colliderStack.getItem() instanceof BlockItem && waterLevel >= 1) {
            BlockItem blockItem = ((BlockItem)colliderStack.getItem());
            
            // If the block item is one of the Concrete Powder Blocks
            if (blockItem.getBlock() instanceof ConcretePowderBlock) {
                BlockState hardened = ((PowderBlockAccessor)blockItem.getBlock()).getHardenedState();
                Block solid = hardened.getBlock();
                
                Vec3d scatter = colliderEntity.getPos();
                
                // If there are still items in the stack
                if (!colliderStack.isEmpty()) {
                    // Get an amount of items to decrement by
                    final int decrement = MathHelper.clamp(colliderStack.getCount(), 1, 8);
                    colliderStack.decrement(decrement);
                    
                    // Spawn a stack of solidified concrete
                    ItemScatterer.spawn(world, scatter.x, scatter.y + 0.85, scatter.z, new ItemStack(solid, decrement));
                    
                    // Randomly lower the water level
                    if (world.random.nextInt((8 / decrement) * 4) == 0)
                        this.setLevel(world, blockPos, blockState, waterLevel - 1);
                    
                    // Cancel the initial cauldron event
                    callback.cancel();
                }
            }
        }
    }
}
