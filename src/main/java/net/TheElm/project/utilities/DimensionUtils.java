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

package net.TheElm.project.utilities;

import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.WorldBorderS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderS2CPacket.Type;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;

public final class DimensionUtils {
    
    private DimensionUtils() {}
    
    public static void addWorldBorderListener(@NotNull final ServerWorld world) {
        world.getWorldBorder()
            .addListener(new IndividualWorldListener(world));
    }

    public static boolean isOutOfBuildLimitVertically(@NotNull World world, @NotNull BlockPos pos) {
        return DimensionUtils.isOutOfBuildLimitVertically(world.getDimension(), pos);
    }
    public static boolean isOutOfBuildLimitVertically(@NotNull DimensionType dimension, @NotNull BlockPos pos) {
        return DimensionUtils.isOutOfBuildLimitVertically(dimension, pos.getY());
    }
    
    public static boolean isOutOfBuildLimitVertically(@NotNull World world, int y) {
        return DimensionUtils.isOutOfBuildLimitVertically(world.getDimension(), y);
    }
    public static boolean isOutOfBuildLimitVertically(@NotNull DimensionType dimension, int y) {
        return y < 0 || y >= dimension.getLogicalHeight();
    }
    
    private static final class IndividualWorldListener implements WorldBorderListener {
        private final ServerWorld world;
        
        private IndividualWorldListener(ServerWorld world) {
            this.world = world;
        }
        
        public RegistryKey<World> getKey() {
            return this.world.getRegistryKey();
        }
        
        public void sendToAll(Packet<?> packet) {
            this.world.getServer().getPlayerManager()
                .sendToDimension(packet, this.getKey());
        }
        
        @Override
        public void onSizeChange(WorldBorder border, double size) {
            this.sendToAll(new WorldBorderS2CPacket(border, Type.SET_SIZE));
        }
        
        @Override
        public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
            this.sendToAll(new WorldBorderS2CPacket(border, Type.LERP_SIZE));
        }
        
        @Override
        public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
            this.sendToAll(new WorldBorderS2CPacket(border, Type.SET_CENTER));
        }
        
        @Override
        public void onWarningTimeChanged(WorldBorder border, int warningTime) {
            this.sendToAll(new WorldBorderS2CPacket(border, Type.SET_WARNING_TIME));
        }
        
        @Override
        public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {
            this.sendToAll(new WorldBorderS2CPacket(border, Type.SET_WARNING_BLOCKS));
        }
        
        @Override
        public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {}
        
        @Override
        public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {}
    }
    
}
