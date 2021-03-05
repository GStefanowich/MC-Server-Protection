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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndermanEntity.class)
public abstract class EndermenFixes extends HostileEntity implements Angerable {
    
    private boolean possibleEntityFarm = false;
    
    protected EndermenFixes(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Override
    public boolean canSpawn(WorldAccess view, SpawnReason reason) {
        return ((reason != SpawnReason.NATURAL) || sewCanEndermanSpawn(view)) && super.canSpawn(view, reason);
    }
    
    @Override
    public boolean canSpawn(WorldView view) {
        return sewCanEndermanSpawn(view) && super.canSpawn(view);
    }
    
    @Override
    protected boolean shouldDropLoot() {
        if (SewConfig.get(SewConfig.ENDERMEN_FARMS_DROP_NO_LOOT) && this.possibleEntityFarm)
            return false;
        return super.shouldDropLoot();
    }
    
    @Inject(at = @At("HEAD"), method = "setTarget")
    public void onUpdateTarget(@Nullable LivingEntity target, CallbackInfo callback) {
        if (target instanceof EndermiteEntity)
            this.possibleEntityFarm = true;
    }
    
    private static boolean sewCanEndermanSpawn(@NotNull WorldView view) {
        if (!SewConfig.get(SewConfig.PREVENT_NETHER_ENDERMEN))
            return true;
        
        World world;
        if (view instanceof World)
            world = (World)view;
        else if (view instanceof ChunkRegion)
            world = ((ChunkRegion)view).toServerWorld();
        else return true;
        
        // If world is NOT the Nether
        return !World.NETHER.equals(world.getRegistryKey());
    }
    
}
