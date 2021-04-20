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

import net.TheElm.project.utilities.nbt.NbtUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(CauldronBlock.class)
public abstract class CauldronCleaning extends Block {
    
    @Shadow
    public native void setLevel(World world_1, BlockPos blockPos_1, BlockState blockState_1, int int_1);
    
    public CauldronCleaning(Settings block$Settings_1) {
        super(block$Settings_1);
    }
    
    @Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
    public void onEntityCollided(BlockState blockState, World world, BlockPos blockPos, Entity entity, CallbackInfo callback) {
        if (world.isClient() || (!(entity instanceof ItemEntity)) || (blockState.get(CauldronBlock.LEVEL) < 3))
            return;
        ItemEntity spawnerEntity = (ItemEntity) entity;
        ItemStack spawnerStack = spawnerEntity.getStack();
        Item spawnerItem = spawnerStack.getItem();
        
        // If not an emerald block, return
        if (!(spawnerItem == Items.SPAWNER))
            return;
        
        // Get person throwing the ingredients
        UUID owner = spawnerEntity.getThrower();
        
        // Get conduit
        ItemStack binderStack = null;
        
        // Search for the spawner
        List<ItemEntity> list = world.getEntitiesByType(EntityType.ITEM, new Box(blockPos), entity1 -> true);
        for (ItemEntity item : list) {
            if ((item.getThrower() == null) || (!item.getThrower().equals(owner)))
                continue;
            
            binderStack = item.getStack();
            if (binderStack.getItem() == Items.EMERALD_BLOCK)
                break;
            
            binderStack = null;
        }
        
        PlayerEntity ownerEntity = world.getPlayerByUuid( owner );
        
        // If no binder was found
        if ((binderStack == null) || (ownerEntity == null))
            return;
        
        // Get the entity IDs on the spawner
        CompoundTag spawnerTag = spawnerStack.getOrCreateTag();
        ListTag entityIds;
        if ((!spawnerTag.contains("EntityIds", NbtType.LIST)) || ((entityIds = spawnerTag.getList("EntityIds", NbtType.STRING)).size() < 2))
            return;
        
        // Remove the first spawn type
        entityIds.remove(0);
        
        // Update the display
        spawnerTag.put("display", NbtUtils.getSpawnerDisplay( entityIds ));
        
        // Take the emerald
        binderStack.decrement( 1 );
        
        // Play a level effect
        ((ServerWorld) world).spawnParticles(ParticleTypes.SPLASH, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, 25, 0.0, 2.0, 0.0, 0.2);
        world.playSound(null, blockPos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1.0f, 1.0f);
        this.setLevel(world, blockPos, blockState, 0);
        
        // Cause the player to pickup the spawner
        ownerEntity.sendPickup( spawnerEntity, spawnerStack.getCount() );
        spawnerEntity.remove();
        ownerEntity.inventory.offerOrDrop( world, spawnerStack );
        
        // Cancel the initial cauldron event
        callback.cancel();
    }
    
}
