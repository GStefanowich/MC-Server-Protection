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

package net.TheElm.project.enums;

import net.TheElm.project.ServerCore;
import net.TheElm.project.utilities.WarpUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public enum CompassDirections {
    SPAWN {
        @Override @NotNull
        public Pair<Text, BlockPos> getPos(@NotNull ServerPlayerEntity player, @NotNull BlockPos current) {
            return new Pair<>(new LiteralText("World Spawn"), ServerCore.getSpawn(player.getWorld()));
        }
        @Override
        public CompassDirections getNext(@NotNull ServerPlayerEntity player, @NotNull BlockPos current) { return WARP; }
    },
    WARP {
        @Override
        public @Nullable Pair<Text, BlockPos> getPos(@NotNull ServerPlayerEntity player, @NotNull BlockPos current) {
            Iterator<Map.Entry<String, WarpUtils.Warp>> warps = WarpUtils.getWarps(player)
                .entrySet()
                .iterator();
            
            WarpUtils.Warp first = null;
            while (warps.hasNext()) {
                WarpUtils.Warp warp = warps.next()
                    .getValue();
                if (first == null && warp.isIn(player.world))
                    first = warp;
                if (warp.isAt(player.world, current) && warps.hasNext())
                    return this.pairWarp(warps.next()
                        .getValue());
            }
            
            return first == null ? null : this.pairWarp(first);
        }
        @Override
        public CompassDirections getNext(@NotNull ServerPlayerEntity player, @NotNull BlockPos current) {
            Iterator<Map.Entry<String, WarpUtils.Warp>> warps = WarpUtils.getWarps(player)
                .entrySet()
                .iterator();
            
            while (warps.hasNext()) {
                WarpUtils.Warp warp = warps.next()
                    .getValue();
                if (warp.isAt(player.world, current) && warps.hasNext())
                    return WARP;
            }
            
            return BED;
        }
        public @NotNull Pair<Text, BlockPos> pairWarp(@NotNull WarpUtils.Warp warp) {
            return new Pair<>(new LiteralText("Waystone \"")
                .append(new LiteralText(warp.name).formatted(Formatting.AQUA))
                .append("\""), warp.warpPos);
        }
    },
    BED {
        @Override
        public @Nullable Pair<Text, BlockPos> getPos(@NotNull ServerPlayerEntity player, @NotNull BlockPos current) {
            BlockPos playerSpawnPos = player.getSpawnPointPosition();
            RegistryKey<World> playerSpawnDim = player.getSpawnPointDimension();
            
            return playerSpawnPos == null || playerSpawnDim == null
                || Objects.equals(SPAWN.getPos(player, current).getRight(), playerSpawnPos)
                || !Objects.equals(playerSpawnDim, player.world.getRegistryKey())
            ? null : new Pair<>(new LiteralText("Bed"), playerSpawnPos);
        }
        @Override
        public CompassDirections getNext(@NotNull ServerPlayerEntity player, @NotNull BlockPos current) { return SPAWN; }
    };
    
    @Nullable
    public abstract Pair<Text, BlockPos> getPos(@NotNull ServerPlayerEntity player, @NotNull BlockPos current);
    public abstract CompassDirections getNext(@NotNull ServerPlayerEntity player, @NotNull BlockPos current);
    
    public Text text() {
        return new LiteralText(this.name()).formatted(Formatting.AQUA);
    }
}
