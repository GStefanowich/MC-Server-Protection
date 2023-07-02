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

package net.theelm.sewingmachine.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.base.packets.CancelMinePacket;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protections.logging.BlockEvent;
import net.theelm.sewingmachine.protections.logging.EventLogger;
import net.theelm.sewingmachine.utilities.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface BlockBreakCallback {
    Event<BlockBreakCallback> TEST = EventFactory.createArrayBacked(BlockBreakCallback.class, (listeners) -> (entity, world, hand, pos, direction, action) -> {
        ActionResult result = ActionResult.PASS;
        
        for (BlockBreakCallback event : listeners) {
            result = event.destroy(entity, world, hand, pos, direction, action);
            if (result != ActionResult.PASS)
                break;
        }
        
        if (result != ActionResult.FAIL && SewConfig.get(SewCoreConfig.LOG_BLOCKS_BREAKING) && action == Action.STOP_DESTROY_BLOCK)
            BlockBreakCallback.onSuccess(entity, world, hand, pos, direction);
        else if (result == ActionResult.FAIL)
            BlockBreakCallback.onFailure(entity, world, hand, pos, direction);
        
        return result;
    });
    
    ActionResult destroy(@NotNull Entity entity, @NotNull ServerWorld world, @NotNull Hand hand, @NotNull BlockPos pos, @Nullable Direction direction, @Nullable Action action);
    static boolean canDestroy(@NotNull Entity entity, @NotNull ServerWorld world, @NotNull Hand hand, @NotNull BlockPos pos, @Nullable Direction direction, @Nullable Action action) {
        return BlockBreakCallback.TEST.invoker()
            .destroy(entity, world, hand, pos, direction, action) != ActionResult.FAIL;
    }
    
    /**
     * When a block is successfully run, perform actions based on the block
     * @param entity The entity responsible for breaking the block
     * @param world The world that the block was broken in
     * @param hand The hand used to break the block
     * @param blockPos The position that the block was broken at
     * @param blockFace The block face that was broken
     */
    private static void onSuccess(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace) {
        // Log the block being broken
        BlockBreakCallback.logBlockBreak(entity, world, blockPos);
        
        // Take additional actions if the entity breaking is a player
        if (entity instanceof ServerPlayerEntity player)
            BlockBreakEventCallback.EVENT.invoker()
                .activate(player, world, hand, blockPos, blockFace);
    }
    
    /**
     * When a block break is failed, perform actions based on the block
     * @param entity The entity responsible for breaking the block
     * @param world The world that the block was broken in
     * @param hand The hand used to break the block
     * @param blockPos The position that the block was broken at
     * @param blockFace The block face that was broken
     */
    private static void onFailure(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace) {
        BlockState blockState = world.getBlockState(blockPos);
        if (entity instanceof ServerPlayerEntity player) {
            // Send a packet to stop mining
            ServerPlayNetworking.send(player, new CancelMinePacket(blockPos));
            
            // Resend the current entity state
            if (EntityUtils.hasClientBlockData(blockState)) {
                BlockEntity blockEntity = world.getBlockEntity(blockPos);
                if (blockEntity != null)
                    player.networkHandler.sendPacket(blockEntity.toUpdatePacket());
            }
        }
    }
    
    /**
     * Log the event of our block being broken into SQL
     * @param entity The entity responsible for breaking the block
     * @param world The world that the block was broken in
     * @param blockPos The position that the block was broken at
     */
    private static void logBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final BlockPos blockPos) {
        EventLogger.log(new BlockEvent(entity, EventLogger.BlockAction.BREAK, world.getBlockState(blockPos).getBlock(), blockPos));
    }
}
