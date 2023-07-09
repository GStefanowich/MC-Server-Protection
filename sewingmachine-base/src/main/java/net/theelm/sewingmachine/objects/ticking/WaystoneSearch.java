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

package net.theelm.sewingmachine.objects.ticking;

import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.interfaces.TickableContext;
import net.theelm.sewingmachine.interfaces.TickingAction;
import net.theelm.sewingmachine.objects.DetachedTickableContext;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.theelm.sewingmachine.utilities.WarpUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on Aug 25 2021 at 11:28 AM.
 * By greg in SewingMachineMod
 */
public class WaystoneSearch implements TickingAction {
    private final @Nullable ServerWorld world;
    private final @NotNull BlockPos worldSpawn;
    private final @NotNull ServerPlayerEntity player;
    private final @Nullable WarpUtils warp;
    private final boolean initFail;
    
    private boolean hasWarpPos = false;
    private boolean hasVerified = false;
    private boolean hasBuilt = false;
    
    private @Nullable DetachedTickableContext child = null;
    private @Nullable ChunkVerifyUnowned search = null;
    
    public WaystoneSearch(@Nullable ServerWorld world, @NotNull ServerPlayerEntity player) {
        String warpName = WarpUtils.PRIMARY_DEFAULT_HOME;
        
        this.world = world;
        this.worldSpawn = this.world == null ? BlockPos.ORIGIN : WarpUtils.getWorldSpawn(this.world);
        this.player = player;
        this.initFail = this.world == null;
        if (this.initFail)
            this.warp = null;
        else {
            // Tell the player
            this.player.sendMessage(TranslatableServerSide.text(
                this.player,
                "warp.random.search"
            ));
            
            int count = 0;
            while (WarpUtils.hasWarp(this.player, warpName))
                warpName = WarpUtils.PRIMARY_DEFAULT_HOME + (++count);
            
            this.warp = new WarpUtils(
                warpName,
                this.player,
                this.world,
                this.worldSpawn
            );
        }
    }
    
    public boolean isPlayerInWorld() {
        return this.isPlayerOnline() && !this.player.isRemoved();
    }
    public boolean isPlayerOnline() {
        return !this.player.isDisconnected();
    }
    
    @Override
    public boolean isCompleted(@NotNull TickableContext detachedTickable) {
        // If completed or construction failed, return remove
        if (detachedTickable.isRemoved() || this.initFail)
            return true;
        // If ticks is not divisible by 20 OR if child is running
        if ((detachedTickable.getTicks() % 20 != 0) || (this.child != null && !this.child.isRemoved()))
            return false;
        
        /*int counts = detachedTickable.getTicks() / 20;
        System.out.println(counts);*/
        
        boolean location  = (this.hasWarpPos || (this.hasWarpPos = this.getNewWarpPosition()));
        boolean unclaimed = location && (this.hasVerified || (this.hasVerified = this.verifyWarpUnclaimed()));
        boolean building  = unclaimed && (this.hasBuilt || (this.hasBuilt = this.claimAndBuild()));
        if (building) {
            // Build the return warp
            this.player.sendMessage(TranslatableServerSide.text(
                this.player,
                "warp.random.build"
            ));
        }
        return building;
    }
    
    private boolean getNewWarpPosition() {
        if (this.warp == null)
            return false;
        this.hasVerified = false; // Make dirt (Must check again)
        return this.warp.getNewWarpPositionIn();
    }
    
    private boolean verifyWarpUnclaimed() {
        if (this.world == null || this.warp == null)
            return false;
        BlockPos warpPos = this.warp.getLastWarpPositionIn();
        if (warpPos == null)
            return false;
        
        // Create a new chunk verification
        if (this.search == null || this.child == null || (this.child.isRemoved() && !this.search.isSuccess())) {
            this.search = new ChunkVerifyUnowned(warpPos, 5);
            this.child = ((LogicalWorld) this.world).addTickableEvent(this.search);
        }
        
        // If the check was successful
        return this.child.isRemoved() && this.search.isSuccess();
    }
    
    private boolean claimAndBuild() {
        if (this.warp == null)
            return false;
        return this.warp.claimAndBuild(this::finish);
    }
    
    public void finish() {
        assert this.world != null;
        assert this.warp != null;
        
        // Get the distance
        BlockPos warpPos = this.warp.getLastWarpPositionIn();
        int distance = warpPos.getManhattanDistance(this.worldSpawn);
        
        // Select a safe teleport location for the player
        BlockPos safeTeleportPos = this.warp.getSafeTeleportPos();
        
        // Save the warp for later
        this.warp.save(safeTeleportPos, this.player);
        
        if (!this.isPlayerOnline())
            return;
        if (!this.isPlayerInWorld()) {
            this.player.sendMessage(Text.literal(""), false);
        } else {
            WarpUtils.teleportEntityAndAttached(this.world, this.player, safeTeleportPos);
            
            // Notify the player of their new location
            if ((!SewConfig.get(SewBaseConfig.WORLD_SPECIFIC_SPAWN)) || SewConfig.equals(SewBaseConfig.WARP_DIMENSION, SewBaseConfig.DEFAULT_WORLD))
                TranslatableServerSide.send(this.player, "warp.random.teleported", distance);
            else
                TranslatableServerSide.send(this.player, "warp.random.teleported_world");
        }
    }
}
