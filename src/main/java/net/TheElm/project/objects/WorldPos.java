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

package net.TheElm.project.objects;

import net.TheElm.project.ServerCore;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.WorldPosition;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class WorldPos implements BlockPointer, WorldPosition {
    
    private final RegistryKey<World> dimensionType;
    private final BlockPos blockPos;
    
    public WorldPos(@NotNull RegistryKey<World> dimensionType, @NotNull BlockPos blockPos) {
        this.dimensionType = dimensionType;
        this.blockPos = blockPos;
    }
    
    @Override
    public double getX() {
        return this.blockPos.getX();
    }
    
    @Override
    public double getY() {
        return this.blockPos.getY();
    }
    
    @Override
    public double getZ() {
        return this.blockPos.getZ();
    }
    
    @Override
    public BlockPos getBlockPos() {
        return this.blockPos;
    }
    
    @Override
    public BlockState getBlockState() {
        return this.getWorld().getBlockState( this.getBlockPos() );
    }
    
    @Override
    public <T extends BlockEntity> T getBlockEntity() {
        return (T) this.getWorld().getBlockEntity( this.getBlockPos() );
    }
    
    @Override
    public World getWorld() {
        MinecraftServer server = ServerCore.get();
        return server.getWorld(this.dimensionType);
    }
}
