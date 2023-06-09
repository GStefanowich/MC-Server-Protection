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

package net.theelm.sewingmachine.base.mixins.Entities;

import net.theelm.sewingmachine.interfaces.BlockBreakCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonEntityMixin extends MobEntity implements Monster {
    protected EnderDragonEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }
    
    /**
     * Only play the dragon death sound in the dragons world
     * @param world The world the dragon is in
     * @param eventId The eventID of the sound
     * @param pos The position the dragon died in
     * @param data Int data
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/world/World.syncGlobalEvent(ILnet/minecraft/util/math/BlockPos;I)V"), method = "updatePostDeath")
    public void deathSoundPlay(@NotNull World world, int eventId, @NotNull BlockPos pos, int data) {
        // Play the global event only in the world
        world.syncWorldEvent(eventId, pos, data);
    }
    
    /**
     * Change the dragon to BREAK blocks instead of making them disappear
     * @param world The world the dragon is in
     * @param pos The block position to break
     * @param move If the block was moved
     * @return If destroying was successful
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"), method = "destroyBlocks")
    public boolean breakBlocks(@NotNull World world, @NotNull BlockPos pos, boolean move) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir() && world instanceof ServerWorld serverWorld) {
            ActionResult result = BlockBreakCallback.EVENT.invoker().interact(this, serverWorld, Hand.MAIN_HAND, pos, null, null);
            if (result != ActionResult.PASS)
                return false;
        }
        
        return world.breakBlock(pos, true, this);
    }
}
