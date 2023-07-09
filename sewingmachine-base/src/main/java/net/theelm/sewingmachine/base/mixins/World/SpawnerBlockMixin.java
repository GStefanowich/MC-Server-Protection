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

package net.theelm.sewingmachine.base.mixins.World;

import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin extends BlockWithEntity {
    protected SpawnerBlockMixin(Settings block$Settings_1) {
        super(block$Settings_1);
    }
    
    @Override
    public void afterBreak(World world, @NotNull PlayerEntity player, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity, ItemStack itemStack) {
        if (!SewConfig.get(SewBaseConfig.SILK_TOUCH_SPAWNERS)) {
            super.afterBreak(world, player, blockPos, blockState, blockEntity, itemStack);
        } else {
            player.incrementStat(Stats.MINED.getOrCreateStat(this));
            player.addExhaustion(0.005F);
            
            // Get hand item (Pickaxe)
            ItemStack handItem = player.getMainHandStack();
            
            if (blockEntity instanceof MobSpawnerBlockEntity spawnerBlock) {
                // Spawner Display
                NbtList spawnEntities = new NbtList();
                
                // Load spawnEntities from the spawnerblock
                NbtUtils.withSpawnerEntities(blockEntity.createNbtWithIdentifyingData(), spawnEntities::add);
                
                boolean doDrop = false;
                
                int toolDamage = SewConfig.get(SewBaseConfig.SPAWNER_PICKUP_DAMAGE);
                if (handItem.getItem().isDamageable() && (doDrop = (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, handItem) > 0))) {
                    if (!world.isClient()) {
                        // Damage the pickaxe
                        if (handItem.damage(toolDamage, world.random, (ServerPlayerEntity) player)) {
                            // Remove the tool
                            handItem.decrement(handItem.getCount());
                            player.sendToolBreakStatus(Hand.MAIN_HAND);
                            
                            // Fail to break spawner
                            doDrop = false;
                        }
                        player.playSound(SoundEvents.ENTITY_ARMOR_STAND_BREAK, SoundCategory.MASTER, 1.0f, 0.5f);
                    }
                }
                
                if (doDrop) {
                    // Create the mob spawner drop
                    ItemStack dropStack = new ItemStack(Items.SPAWNER);
                    dropStack.setCount(1);
                    
                    // Create the item tag
                    NbtCompound dropTag = dropStack.getOrCreateNbt();
                    
                    //dropTag.put("EntityIds", itemNbt);
                    dropTag.put("display", NbtUtils.getSpawnerDisplay(spawnEntities));
                    dropTag.put("EntityIds", spawnEntities);
                    
                    // Drop the spawner
                    ItemScatterer.spawn(world, blockPos.getX(), blockPos.getY(), blockPos.getZ(), dropStack);
                } else {
                    // Play a glass breaking sound
                    world.playSound(null, blockPos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    
                    // Calculate XP to give for every mob in the spawner
                    int xpGive = 0;
                    for (int i = 0; i < spawnEntities.size(); ++i) {
                        xpGive += 45 + world.random.nextInt(45) + world.random.nextInt(45);
                    }
                    
                    // Drop the XP
                    if (xpGive > 0 && world instanceof ServerWorld serverWorld)
                        this.dropExperience(serverWorld, blockPos, xpGive);
                }
            }
        }
    }
    
    @Override
    public void onPlaced(@NotNull World world, @NotNull BlockPos blockPos, @NotNull BlockState blockState, @NotNull LivingEntity livingEntity, @NotNull ItemStack itemStack) {
        NbtCompound tag = itemStack.getOrCreateNbt();
        if (tag.contains("EntityIds", NbtElement.LIST_TYPE)) {
            NbtList mob = tag.getList("EntityIds", NbtElement.STRING_TYPE);
            
            // Get the mob spawner entity
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof MobSpawnerBlockEntity) {
                NbtCompound spawnerTag = blockEntity.createNbtWithIdentifyingData();
                
                // Get the mobs to spawn
                NbtList spawnPotentials = new NbtList();
                
                for (NbtElement spawn : mob) {
                    String mobIdentifier = spawn.asString();
                    NbtCompound mobTag = new NbtCompound();
                    NbtCompound entity = new NbtCompound();
                    NbtCompound data = new NbtCompound();
                    
                    entity.putString("id", mobIdentifier);
                    mobTag.put("entity", entity);
                    data.putInt("weight", 1);
                    data.put("data", mobTag);
                    
                    spawnPotentials.add(data);
                }
                
                // Update the tag
                spawnerTag.getCompound("SpawnData")
                    .getCompound("entity")
                    .putString("id", mob.get(0).asString());
                spawnerTag.put("SpawnPotentials", spawnPotentials);
                
                // Save to block
                BlockEntity.createFromNbt(blockPos, blockState, spawnerTag);
            }
        }
    }
}
