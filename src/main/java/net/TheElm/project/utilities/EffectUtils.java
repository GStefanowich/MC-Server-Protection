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

import net.TheElm.project.interfaces.LogicalWorld;
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
    
    private static final int TICK_DELAY = 2;
    private static final int TOTAL_STEPS = 16;
    private static final double TWO_PI = Math.PI * 2;
    private static final double PER_STEP = EffectUtils.TWO_PI / EffectUtils.TOTAL_STEPS;
    
    private EffectUtils() {}
    
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final LivingEntity mob, final boolean up) {
        EffectUtils.particleSwirl(particle, mob, up, 1);
    }
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final LivingEntity mob, final boolean up, final int count) {
        if (mob.world.isClient)
            return;
        ((LogicalWorld)mob.world).addTickableEvent(event -> {
            int ticks = event.getTicks();
            if (event.isRemoved() || (ticks / EffectUtils.TICK_DELAY) > EffectUtils.TOTAL_STEPS)
                return true;
            if (ticks % EffectUtils.TICK_DELAY == 0) {
                int current = ticks / EffectUtils.TICK_DELAY;
                int counter = up ? 0 : 15;
                float radius = mob.getWidth() + 1.5f;
                
                double theta = current * EffectUtils.PER_STEP;
                double height = (((double)(up ? counter + current : counter - current) / EffectUtils.TOTAL_STEPS) * mob.getHeight());
                
                EffectUtils.summonSwirl(
                    particle,
                    event.getWorld(),
                    mob.getPos(),
                    mob.getVelocity(),
                    height,
                    theta,
                    EffectUtils.PER_STEP,
                    radius,
                    count
                );
            }
            return false;
        });
    }
    
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final ServerWorld world, final Vec3d mobPos, final boolean up) {
        EffectUtils.particleSwirl(particle, world, mobPos, up, 1);
    }
    public static <T extends ParticleEffect> void particleSwirl(@NotNull final T particle, @NotNull final ServerWorld world, final Vec3d mobPos, final boolean up, final int count) {
        if (world.isClient)
            return;
        ((LogicalWorld)world).addTickableEvent(event -> {
            int ticks = event.getTicks();
            if (event.isRemoved() || (ticks / EffectUtils.TICK_DELAY) > EffectUtils.TOTAL_STEPS)
                return true;
            if (ticks % EffectUtils.TICK_DELAY == 0) {
                int current = ticks / EffectUtils.TICK_DELAY;
                int counter = up ? 0 : 15;
                float radius = 1.5f;
                
                double theta = current * EffectUtils.PER_STEP;
                double height = (((double)(up ? counter + current : counter - current) / EffectUtils.TOTAL_STEPS) * 2);
                
                EffectUtils.summonSwirl(
                    particle,
                    world,
                    mobPos,
                    Vec3d.ZERO,
                    height,
                    theta,
                    EffectUtils.PER_STEP,
                    radius,
                    count
                );
            }
            return false;
        });
    }
    
    private static <T extends ParticleEffect> void summonSwirl(@NotNull final T particle, @NotNull final ServerWorld world, @NotNull final Vec3d mobPos, @NotNull final Vec3d velocity, final double height, final double theta, final double step, final float radius, final int count) {
        double speed = 0D;
        
        // Get X/Z coordinates
        double x = Math.cos( theta ) - Math.cos( theta - step );
        double z = Math.sin( theta ) - Math.sin( theta - step );
        
        // Spawn main side particle
        EffectUtils.summonParticle(
            particle,
            world,
            mobPos.add(
                x * radius,
                0,
                z * radius
            ),
            velocity,
            height,
            count,
            speed
        );
        
        // Spawn opposite side particle
        EffectUtils.summonParticle(
            particle,
            world,
            mobPos.add(
                x * -radius,
                0,
                z * -radius
            ),
            velocity,
            height,
            count,
            speed
        );
    }
    private static <T extends ParticleEffect> void summonParticle(@NotNull final T particle, @NotNull final ServerWorld world, @NotNull final Vec3d pos, @NotNull final Vec3d velocity, final double height, final int count, final double speed) {
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
            double angle = (player.getYaw() + BlockUtils.angleBetween(end, now)) % 360;
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
            BlockPos navPos = path.getNodePos(i);
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
