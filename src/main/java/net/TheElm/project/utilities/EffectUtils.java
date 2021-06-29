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

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public final class EffectUtils {
    
    private EffectUtils() {}
    
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final LivingEntity mob) {
        EffectUtils.particleSwirl( particle, mob, 1 );
    }
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final LivingEntity mob, final int count) {
        if (mob.world.isClient)
            return;
        new Thread(() -> {
            ServerWorld world = (ServerWorld)mob.getEntityWorld();
            try {
                
                int counter = 0;
                int steps = 16;
                double TWO_PI = Math.PI * 2;
                double step = TWO_PI / steps;
                float radius = mob.getWidth() + 1.5f;
                
                for ( double theta = 0; theta <= TWO_PI; theta += step ) {
                    EffectUtils.summonSwirl(
                        particle,
                        world,
                        mob.getPos(),
                        mob.getVelocity(),
                        (((double) ++counter / steps) * mob.getHeight()),
                        theta,
                        step,
                        radius,
                        count
                    );
                    
                    Thread.sleep( 75 );
                }
                
            } catch(InterruptedException ignore) {}
        }).start();
    }
    
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final ServerWorld world, final Vec3d mobPos) {
        EffectUtils.particleSwirl(particle, world, mobPos, 1);
    }
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final ServerWorld world, final Vec3d mobPos, final int count) {
        new Thread(() -> {
            try {
                int counter = 0;
                int steps = 16;
                double TWO_PI = Math.PI * 2;
                double step = TWO_PI / steps;
                float radius = 1.5f;
                
                for ( double theta = 0; theta <= TWO_PI; theta += step ) {
                    EffectUtils.summonSwirl(
                        particle,
                        world,
                        mobPos,
                        new Vec3d( 0, 0, 0 ),
                        (((double) ++counter / steps) * 2),
                        theta,
                        step,
                        radius,
                        count
                    );
                    
                    Thread.sleep( 75 );
                }
            } catch(InterruptedException ignore) {}
        }).start();
    }
    
    private static <T extends ParticleEffect> void summonSwirl(@NotNull final T particle, @NotNull final ServerWorld world, @NotNull final Vec3d mobPos, @NotNull final Vec3d velocity, final double height, final double theta, final double step, final float radius, final int count) {
        double speed = 0D;
        Vec3d pos;
        
        // Get X/Z coordinates
        double x = Math.cos( theta ) - Math.cos( theta - step );
        double z = Math.sin( theta ) - Math.sin( theta - step );
        
        // Spawn main side particle
        pos = mobPos.add(
            x * radius,
            0,
            z * radius
        );
        
        world.spawnParticles(
            particle,
            pos.getX(),
            pos.getY() + height,
            pos.getZ(),
            count,
            velocity.getX(),
            velocity.getY(),
            velocity.getZ(),
            speed
        );
        
        // Spawn opposite side particle
        pos = mobPos.add(
            x * (-radius),
            0,
            z * (-radius)
        );
        world.spawnParticles(
            particle,
            pos.getX(),
            pos.getY() + height,
            pos.getZ(),
            count,
            velocity.getX(),
            velocity.getY(),
            velocity.getZ(),
            speed
        );
    }
    
    public static <T extends ParticleEffect> void summonBreadcrumbs(@NotNull final T particle, @NotNull final ServerPlayerEntity player, @NotNull Path path) {
        BlockPos end = path.getTarget();
        BlockPos now = player.getBlockPos();
        
        int elevationChange = end.getY() - now.getY();
        int xDirChange = end.getX() - now.getX();
        int zDirChange = end.getZ() - now.getZ();
        
        int absX = Math.abs(xDirChange);
        int absY = (int)(Math.abs((float) elevationChange) / 1.5);
        int absZ = Math.abs(zDirChange);
        
        String helper = null,
            prefix = null,
            suffix = null;
        if (absY > absX && absY > absZ && absY != 0)
            helper = elevationChange > 0 ? "Ascend" : "Descend";
        else {
            double angle = (player.yaw + BlockUtils.angleBetween(end, now)) % 360;
            if (angle < 0)
                angle += 360;
            
            int force = 0;
            if (angle >= 165 && angle <= 195)
                helper = "Go Forward";
            else if (angle >= 0 && angle < 165) {
                force = (int)((180 - angle) / 30);
                helper = "Go Right";
                suffix = StringUtils.repeat(">", force);
            } else if (angle <= 360 && angle > 195) {
                force = (int)((angle - 180) / 30);
                helper = "Go Left";
                prefix = StringUtils.repeat("<", force);
            }
        }
        
        // Send text to the players hotbar
        if (helper != null) {
            MutableText bar = new LiteralText("")
                .formatted(Formatting.YELLOW);
            
            // Append the prefix
            if (prefix != null)
                bar.append(new LiteralText(prefix + " ")
                    .formatted(Formatting.AQUA));
            
            // Append the normal text
            bar.append(helper);
            
            // Append the suffix
            if (suffix != null)
                bar.append(new LiteralText(" " + suffix)
                    .formatted(Formatting.AQUA));
            
            player.sendMessage(bar, true);
        }
        
        // Spawn obsidian particles for each node of the path
        for (int i = 0; i < path.getLength(); i++) {
            BlockPos navPos = path.method_31031(i);
            player.networkHandler.sendPacket(new ParticleS2CPacket(
                particle,
                false,
                navPos.getX(),
                navPos.getY() - 0.5D,
                navPos.getZ(),
                1.0F,
                0.5F,
                1.0F,
                0.0F,
                10
            ));
        }
    }
    
}
