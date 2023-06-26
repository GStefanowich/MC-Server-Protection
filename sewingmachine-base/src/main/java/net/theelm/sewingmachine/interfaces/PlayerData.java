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

package net.theelm.sewingmachine.interfaces;

import com.google.common.collect.ImmutableSet;
import net.theelm.sewingmachine.enums.CompassDirections;
import net.theelm.sewingmachine.utilities.WarpUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public interface PlayerData {
    
    /*
     * Saved warp data
     */
    
    void setWarp(@NotNull WarpUtils.Warp warp);
    void delWarp(@NotNull WarpUtils.Warp warp);
    void delWarp(@NotNull String name);
    @NotNull Map<String, WarpUtils.Warp> getWarps();
    
    /*
     * Player join information
     */
    
    @Nullable Long getFirstJoinAt();
    void updateFirstJoin();
    @Nullable Long getLastJoinAt();
    void updateLastJoin();
    
    /*
     * Player block ruler information
     */
    
    void setRulerA(@Nullable BlockPos blockPos);
    void setRulerB(@Nullable BlockPos blockPos);
    @Nullable BlockPos getRulerA();
    @Nullable BlockPos getRulerB();
    
    /*
     * Portal save locations
     */
    
    void setNetherPortal(@Nullable BlockPos blockPos);
    void setOverworldPortal(@Nullable BlockPos blockPos);
    @Nullable BlockPos getNetherPortal();
    @Nullable BlockPos getOverworldPortal();
    
    /*
     * Compass handling
     */

    Pair<Text, BlockPos> cycleCompass();
    CompassDirections getCompass();
    
    /*
     * Health Bar
     */
    
    default @NotNull ServerBossBar getHealthBar() {
        return this.getHealthBar(true);
    }
    @Contract("true -> !null") ServerBossBar getHealthBar(boolean create);
    
    /*
     * Path Finding
     */
    
    @NotNull PathNodeNavigator getPathNodeNavigator();
    default @Nullable Path findPathTo(@NotNull Entity entity, int distance) {
        return this.findPathTo(entity.getBlockPos(), distance);
    }
    default @Nullable Path findPathTo(@NotNull BlockPos pos, int distance) {
        return this.findPathToAny(ImmutableSet.of(pos), 16, true, distance);
    }
    @Nullable Path findPathToAny(Set<BlockPos> positions, int range, boolean bl, int distance);
}
