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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class Clumps extends Entity {
    
    @Shadow private int amount;
    @Shadow public int orbAge;
    
    @Shadow
    public abstract int getExperienceAmount();
    
    public Clumps(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("TAIL"), method = "tick")
    public void onTick(CallbackInfo callback) {
        if (!this.world.isClient) {
            // Get orbs of same tile
            this.world.getEntities(
                ExperienceOrbEntity.class,
                this.getBoundingBox(),
                (orb -> (!orb.getUuid().equals(this.getUuid())) && orb.isAlive())
            ).stream().filter((orb) -> {
                // Get where found orbs are younger than the current (Let the oldest live)
                return orb.orbAge < this.orbAge;
            }).findAny().ifPresent((orb) -> {
                // Remove the orbs
                orb.remove();
                this.remove();
                
                // Spawn a new orb
                world.spawnEntity(new ExperienceOrbEntity(
                    world,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    this.getExperienceAmount() + orb.getExperienceAmount()
                ));
            });
        }
    }
    
    /*@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;)Ljava/util/Map$Entry;"), method = "onPlayerCollision")
    public Entry<EquipmentSlot, ItemStack> getRandomMendable(Enchantment enchantment, PlayerEntity entity) {
        // Get all equipment from the player with the enchantment
        Map<EquipmentSlot, ItemStack> equipment = enchantment.getEquipment(entity);
        if (equipment.isEmpty())
            return null;
        
        int most = 0;
        Entry<EquipmentSlot, ItemStack> slot = null;
        
        for (Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            ItemStack stack = entry.getValue();
            if ((!stack.isEmpty()) && (EnchantmentHelper.getLevel(enchantment, stack) > 0)) {
                int damage = stack.getDamage();
                if ((damage >= most) && ((most = damage) > 0))
                    slot = entry;
            }
        }
        
        return slot;
    }*/ 
    
}
