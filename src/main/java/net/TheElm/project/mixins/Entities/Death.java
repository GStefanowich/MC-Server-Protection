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

package net.TheElm.project.mixins.Entities;

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.utilities.NbtUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class Death extends Entity {
    
    @Shadow
    protected boolean dead;
    
    public Death(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("TAIL"), method = "onDeath")
    public void onDeath(DamageSource damageSource, CallbackInfo callback) {
        // If disabled
        if (!SewingMachineConfig.INSTANCE.SPAWNER_ABSORB_MOBS.get())
            return;
        
        // If not dead or is player
        if ((!this.dead) || (((Entity) this) instanceof PlayerEntity))
            return;
        
        // If attacker is not a player
        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity))
            return;
        
        CompoundTag spawnerTag;
        
        // Get the attacker
        ServerPlayerEntity player = (ServerPlayerEntity) damageSource.getAttacker();
        ItemStack itemStack = player.getStackInHand(Hand.OFF_HAND);
        if ((!(itemStack.getItem() == Items.SPAWNER)) || ((spawnerTag = itemStack.getTag()) == null) || ((spawnerTag = spawnerTag.method_10553()) == null) || (!spawnerTag.containsKey("EntityIds", 9)))
            return;
        
        // Get the identifier of the mob we killed
        StringTag mobId = new StringTag(EntityType.getId(this.getType()).toString());
        
        // Get current entity IDs
        ListTag entityIds = spawnerTag.getList("EntityIds", 8);
        int rolls = 1 + EnchantmentHelper.getLevel(Enchantments.LOOTING, player.getMainHandStack());
        for (int roll = 0; roll < rolls; ++roll) {
            // Test the odds
            if ((!entityIds.contains(mobId)) && (player.world.getRandom().nextInt(800) == 0)) {
                // Add mob to the list
                entityIds.add(mobId);
                
                // Update the dropped items tag
                spawnerTag.put("EntityIds", entityIds);
                spawnerTag.put("display", NbtUtils.getSpawnerDisplay(entityIds));
                
                // Play sound
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f);
                
                // Should drop a new spawner
                boolean dropNew = false;
                
                // Update the existing item in hand
                if (dropNew = (itemStack.getCount() > 1)) {
                    // Update a new item (NOT an entire stack)
                    itemStack.decrement(1);
                    itemStack = new ItemStack(Items.SPAWNER);
                }
                
                // Update the itemstack
                itemStack.setTag(spawnerTag);
                
                // Give the player
                if (dropNew)
                    player.inventory.offerOrDrop(player.world, itemStack);
                break;
            }
        }
    }
    
}
