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

package net.theelm.sewingmachine.mixins.World;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.dimension.AreaHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AreaHelper.class)
public abstract class Portals {
    
    @Inject(at = @At("HEAD"), method = "createPortal")
    public void onCreatePortal(CallbackInfo callback) {
    }
    
    @Inject(at = @At("HEAD"), method = "entityPosInPortal")
    private static void onX(BlockLocating.Rectangle rect, Direction.Axis axis, Vec3d vec3d, EntityDimensions size, CallbackInfoReturnable<Vec3d> callback) {
    }
    
    @Inject(at = @At("HEAD"), method = "getNetherTeleportTarget")
    private static void onY(ServerWorld world, BlockLocating.Rectangle rect, Direction.Axis axis, Vec3d vec3d, EntityDimensions size, Vec3d vec3d2, float f, float g, CallbackInfoReturnable<TeleportTarget> callback) {
    }
    
    /*@Shadow @Final private ServerWorld world;
    @Shadow @Final private Random random;*/
    
    /*@Shadow
    public native @Nullable TeleportTarget getPortal(BlockPos blockPos, Vec3d velocity, Direction direction, double x, double z, boolean isPlayer);*/
    
    /*@Inject(at = @At("HEAD"), method = "usePortal", cancellable = true)
    public void onDimensionUpdate(Entity entity, float fl, CallbackInfoReturnable<Boolean> callback) {
        Vec3d vec3d = entity.getLastNetherPortalDirectionVector();
        Direction dir = entity.getLastNetherPortalDirection();
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            ServerWorld world = player.getServerWorld();
            
            RegistryKey<World> dimType = world.getRegistryKey();
            
            // Search for a saved portal location (In the opposite world) to go to
            BlockPos lastPos = null;
            if (SewConfig.get(SewConfig.OVERWORLD_PORTAL_LOC) && World.OVERWORLD.equals(dimType)) lastPos = ((PlayerData) player).getNetherPortal();
            else if (SewConfig.get(SewConfig.NETHER_PORTAL_LOC) && World.NETHER.equals(dimType)) lastPos = ((PlayerData) player).getOverworldPortal();
            
            // If not set, let Vanilla take over
            if (lastPos == null) return;
            
            // Check for a portal at the block
            BlockPattern.Result pattern = NetherPortalBlock.findPortal(world, lastPos);
            TeleportTarget teleportTarget = pattern.getTeleportTarget(dir, lastPos, vec3d.y, entity.getVelocity(), vec3d.x);
            if (teleportTarget == null) {
                if (World.OVERWORLD.equals(dimType)) ((PlayerData) player).setOverworldPortal( null );
                else if (World.NETHER.equals(dimType)) ((PlayerData) player).setNetherPortal( null );
                
                return; // If no portal was found, let Vanilla take over
            }
            
            // Update the player
            player.setVelocity( teleportTarget.velocity );
            player.yaw = fl + (float)teleportTarget.yaw;
            
            // Send the teleport
            player.networkHandler.requestTeleport(lastPos.getX() + 0.5D, lastPos.getY(), lastPos.getZ() + 0.5D, player.yaw, player.pitch);
            player.networkHandler.syncWithPlayerPosition();
            callback.setReturnValue( true );
        }
    }*/
    
    // TODO: Search for a portal that has a matching sign on it
    /*@Inject(at = @At("HEAD"), method = "getPortal", cancellable = true)
    public void onFindPortal(BlockPos start, Vec3d velocity, Direction direction, double x, double z, boolean isPlayer, CallbackInfoReturnable<TeleportTarget> callback) {
        PointOfInterestStorage pointOfInterestStorage = this.world.getPointOfInterestStorage();
        pointOfInterestStorage.method_22439(this.world, start, 128);
        
        Stream<PointOfInterest> points = pointOfInterestStorage.method_22383((pointOfInterestType) -> {
            return pointOfInterestType == PointOfInterestType.NETHER_PORTAL;
        }, start, 128, PointOfInterestStorage.OccupationStatus.ANY).collect(Collectors.toList()).stream();
        
        Optional<PointOfInterest> optional = points.min(Comparator.comparingDouble((ToDoubleFunction<PointOfInterest>) pointOfInterest -> {
            // F
            return pointOfInterest.getPos().getSquaredDistance(start);
        }).thenComparingInt((pointOfInterest) -> {
            // F
            return pointOfInterest.getPos().getY();
        }));
        
        Optional<TeleportTarget> target = optional.map((pointOfInterest) -> {
            BlockPos blockPos = pointOfInterest.getPos();
            this.world.getChunkManager().addTicket(ChunkTicketType.PORTAL, new ChunkPos(blockPos), 3, blockPos);
            BlockPattern.Result result = NetherPortalBlock.findPortal(this.world, blockPos);
            return result.getTeleportTarget(direction, blockPos, z, velocity, x);
        });
        
        // Set the return value if we found one
        target.ifPresent(callback::setReturnValue);
    }*/
    
}
