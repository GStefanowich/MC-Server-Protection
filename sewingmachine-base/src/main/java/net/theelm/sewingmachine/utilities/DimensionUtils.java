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

package net.theelm.sewingmachine.utilities;

import net.minecraft.network.packet.Packet;
import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.minecraft.network.packet.s2c.play.WorldBorderCenterChangedS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderInterpolateSizeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderSizeChangedS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderWarningBlocksChangedS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderWarningTimeChangedS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DimensionUtils {
    
    private static final StyleApplicator OVERWORLD_APPLICATOR = new StyleApplicator(ColorUtils.getRawTextColor("LightSkyBlue"));
    private static final StyleApplicator NETHER_APPLICATOR = new StyleApplicator(ColorUtils.getRawTextColor("FireBrick"));
    private static final StyleApplicator END_APPLICATOR = new StyleApplicator(ColorUtils.getRawTextColor("PaleGoldenRod"));
    private static final StyleApplicator DEFAULT_APPLICATOR = new StyleApplicator(ColorUtils.getNearestTextColor(Formatting.WHITE));
    
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
        return y < DimensionUtils.getWorldDepth(dimension) || y >= DimensionUtils.getWorldHeight(dimension);
    }
    
    public static int getWorldDepth(@NotNull World world) {
        return DimensionUtils.getWorldDepth(world.getDimension());
    }
    public static int getWorldDepth(@NotNull DimensionType dimension) {
        return dimension.minY();
    }
    public static int getWorldHeight(@NotNull World world) {
        return DimensionUtils.getWorldHeight(world.getDimension());
    }
    public static int getWorldHeight(@NotNull DimensionType dimension) {
        return DimensionUtils.getWorldDepth(dimension) + dimension.height();
    }
    
    public static boolean isWithinProtectedZone(@NotNull WorldChunk chunk) {
        ChunkPos start = chunk.getPos();
        return DimensionUtils.isWithinProtectedZone(chunk.getWorld(), start.getStartPos())
            || DimensionUtils.isWithinProtectedZone(chunk.getWorld(), start.getBlockPos(15, 0, 15))
            || DimensionUtils.isWithinProtectedZone(chunk.getWorld(), start.getBlockPos(15, 0, 0))
            || DimensionUtils.isWithinProtectedZone(chunk.getWorld(), start.getBlockPos(0, 0, 15));
    }
    public static boolean isWithinProtectedZone(@NotNull World world, BlockPos pos) {
        return DimensionUtils.isWithinProtectedZone(world.getRegistryKey(), pos);
    }
    public static boolean isWithinProtectedZone(@NotNull RegistryKey<World> world, BlockPos pos) {
        if (world.equals(World.END)) {
            int x = Math.abs(pos.getX());
            int z = Math.abs(pos.getZ());
            return x <= 200 & z <= 200;
        }
        return false;
    }
    
    public static @NotNull String dimensionIdentifier(@NotNull World world) {
        return DimensionUtils.dimensionIdentifier(world.getRegistryKey());
    }
    public static @NotNull String dimensionIdentifier(@Nullable RegistryKey<World> world) {
        return world == null ? "" : world.getValue().toString();
    }
    public static @NotNull MutableText longDimensionName(@Nullable RegistryKey<World> world) {
        return DimensionUtils.longDimensionName(world, CasingUtils.Casing.DEFAULT);
    }
    public static @NotNull MutableText longDimensionName(@Nullable RegistryKey<World> world, @NotNull CasingUtils.Casing casing) {
        if (World.OVERWORLD.equals(world))
            return TextUtils.literal("Surface", casing);
        if (World.NETHER.equals(world))
            return TextUtils.literal("Nether", casing);
        if (World.END.equals(world))
            return TextUtils.literal("The End", casing);
        return TextUtils.literal("Server", casing);
    }
    public static @NotNull MutableText shortDimensionName(@Nullable RegistryKey<World> world) {
        return DimensionUtils.shortDimensionName(world, CasingUtils.Casing.DEFAULT);
    }
    public static @NotNull MutableText shortDimensionName(@Nullable RegistryKey<World> world, @NotNull CasingUtils.Casing casing) {
        if (World.OVERWORLD.equals(world))
            return TextUtils.literal("S", casing);
        if (World.NETHER.equals(world))
            return TextUtils.literal("N", casing);
        if (World.END.equals(world))
            return TextUtils.literal("E", casing);
        return TextUtils.literal("~");
    }
    public static @NotNull StyleApplicator dimensionColor(@Nullable RegistryKey<World> world) {
        if (World.OVERWORLD.equals(world))
            return OVERWORLD_APPLICATOR;
        if (World.NETHER.equals(world))
            return NETHER_APPLICATOR;
        if (World.END.equals(world))
            return END_APPLICATOR;
        return DEFAULT_APPLICATOR;
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
            this.sendToAll(new WorldBorderSizeChangedS2CPacket(border));
        }
        
        @Override
        public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
            this.sendToAll(new WorldBorderInterpolateSizeS2CPacket(border));
        }
        
        @Override
        public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
            this.sendToAll(new WorldBorderCenterChangedS2CPacket(border));
        }
        
        @Override
        public void onWarningTimeChanged(WorldBorder border, int warningTime) {
            this.sendToAll(new WorldBorderWarningTimeChangedS2CPacket(border));
        }
        
        @Override
        public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {
            this.sendToAll(new WorldBorderWarningBlocksChangedS2CPacket(border));
        }
        
        @Override
        public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {}
        
        @Override
        public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {}
    }
    
}
