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

package net.TheElm.project.mixins.World;

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.PlayerData;
import net.minecraft.block.PortalBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PortalForcer;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalForcer.class)
public abstract class Portals implements Nameable, CommandOutput {
    
    @Shadow @Final
    private static PortalBlock PORTAL_BLOCK;
    
    @Nullable @Shadow
    public native BlockPattern.TeleportTarget getPortal(BlockPos blockPos_1, Vec3d vec3d_1, Direction direction_1, double double_1, double double_2, boolean boolean_1);
    
    @Inject(at = @At("HEAD"), method = "usePortal", cancellable = true)
    public void onDimensionUpdate(Entity entity, float fl, CallbackInfoReturnable<Boolean> callback) {
        Vec3d vec3d = entity.getLastPortalDirectionVector();
        Direction dir = entity.getLastPortalDirection();
        if ((entity instanceof ServerPlayerEntity) && SewingMachineConfig.INSTANCE.RETURN_PORTALS.get()) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            ServerWorld world = player.getServerWorld();
            
            DimensionType dimType = world.getDimension().getType();
            
            // Search for a saved portal location (In the opposite world) to go to
            BlockPos lastPos = null;
            if (dimType == DimensionType.OVERWORLD) lastPos = ((PlayerData) player).getNetherPortal();
            else if (dimType == DimensionType.THE_NETHER) lastPos = ((PlayerData) player).getOverworldPortal();
            
            // If not set, let Vanilla take over
            if (lastPos == null) return;
            
            // Check for a portal at the block
            BlockPattern.Result pattern = PORTAL_BLOCK.findPortal(world, lastPos);
            BlockPattern.TeleportTarget teleportTarget = pattern.method_18478(dir, lastPos, vec3d.y, entity.getVelocity(), vec3d.x);
            if (teleportTarget == null) {
                if (dimType == DimensionType.OVERWORLD) ((PlayerData) player).setOverworldPortal( null );
                else if (dimType == DimensionType.THE_NETHER) ((PlayerData) player).setNetherPortal( null );
                
                return; // If no portal was found, let Vanilla take over
            }
            
            // Update the player
            player.setVelocity( teleportTarget.velocity );
            player.yaw = fl + (float)teleportTarget.yaw;
            
            // Send the teleport
            player.networkHandler.requestTeleport(lastPos.getX(), lastPos.getY(), lastPos.getZ(), player.yaw, player.pitch);
            player.networkHandler.syncWithPlayerPosition();
            callback.setReturnValue( true );
        }
    }
    
}
