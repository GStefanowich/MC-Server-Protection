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

import net.TheElm.project.config.SewConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeletonEntity.class)
public abstract class LimitedSkeletons extends HostileEntity implements RangedAttackMob {
    
    @Shadow public native void updateAttackType();
    
    private Integer arrowStack = null;
    
    protected LimitedSkeletons(EntityType<? extends AbstractSkeletonEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("RETURN"), method = "attack")
    public void onAttack(LivingEntity livingEntity, float damage, CallbackInfo callback) {
        if (this.world.isClient || (!SewConfig.get(SewConfig.LIMIT_SKELETON_ARROWS)))
            return;
        
        this.createArrowQuiver();
        if ( --this.arrowStack <= 0 ) {
            this.playSound(SoundEvents.ENTITY_ITEM_BREAK, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.equipStack(EquipmentSlot.MAINHAND, this.getEquipmentSword(this.world.getLocalDifficulty(this.getBlockPos())));
            this.updateAttackType();
        }
    }
    
    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag( tag );
        this.createArrowQuiver();
        tag.putInt( "arrowStack", this.arrowStack );
    }
    
    @Inject(at=@At("TAIL"), method = "readCustomDataFromTag")
    public void onReadingData(CompoundTag tag, CallbackInfo callback) {
        if ( tag.contains( "arrowStack" ) ) {
            this.arrowStack = tag.getInt("arrowStack");
        }
        this.createArrowQuiver();
    }
    
    private ItemStack getEquipmentSword(LocalDifficulty localDifficulty) {
        Difficulty difficulty = this.world.getDifficulty();
        if (this.random.nextFloat() < 0.15F * localDifficulty.getClampedLocalDifficulty()) {
            int int_1 = this.random.nextInt(2);
            
            if (this.random.nextFloat() < 0.095F) ++int_1;
            if (this.random.nextFloat() < 0.095F) ++int_1;
            if (this.random.nextFloat() < 0.095F) ++int_1;
            
            Item sword;
            if (( sword = this.getRandomSword( int_1 ) ) != null)
                return new ItemStack( sword );
        }
        
        return new ItemStack( difficulty == Difficulty.EASY ? Items.WOODEN_SWORD : Items.STONE_SWORD );
    }
    @Nullable
    private Item getRandomSword( int int_1 ) {
        if (int_1 >= 4)
            return Items.DIAMOND_SWORD;
        if (int_1 >= 3)
            return Items.IRON_SWORD;
        if (int_1 >= 2)
            return Items.GOLDEN_SWORD;
        if (int_1 >= 1)
            return Items.STONE_SWORD;
        
        return null;
    }
    
    private void createArrowQuiver() {
        if (this.arrowStack == null) this.arrowStack = this.getQuiverMaximum( this.world.getDifficulty() );
    }
    private int getQuiverMaximum( Difficulty difficulty ) {
        int arrows = this.getRandom().nextInt( 16 );
        int tier = difficulty.getId();
        if ( tier < 3 )
            arrows += this.getRandom().nextInt( 16 );
        if ( tier < 2 )
            arrows += this.getRandom().nextInt( 16 );
        if ( tier < 1 )
            arrows += this.getRandom().nextInt( 16 );
        return arrows;
    }
    
}
