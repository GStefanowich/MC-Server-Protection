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

package net.theelm.sewingmachine.protection.events;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.mob.EndermanEntity;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.events.BlockBreakCallback;
import net.theelm.sewingmachine.interfaces.ConstructableEntity;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.interfaces.OwnableEntity;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.utilities.CropUtils;
import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class BlockBreak {
    
    private BlockBreak() {}
    
    /**
     * Initialize our callback listener for Block Breaking
     */
    public static void register(@NotNull Event<BlockBreakCallback> event) {
        // Register the block break event
        event.register(BlockBreak::canPlayerBlockBreak);
        event.register(BlockBreak::canEndermenBlockBreak);
        event.register(BlockBreak::canEnderdragonBlockBreak);
        event.register(BlockBreak::canGhastsBlockBreak);
        event.register(BlockBreak::canCreepersBlockBreak);
        event.register(BlockBreak::canExplosiveBlockBreak);
    }
    
    public static ActionResult canPlayerBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        if (!(entity instanceof ServerPlayerEntity player))
            return ActionResult.PASS;
        
        // If player is in creative
        if ((player.isCreative() && SewConfig.get(SewCoreConfig.CLAIM_CREATIVE_BYPASS)) || (action == Action.ABORT_DESTROY_BLOCK))
            return ActionResult.PASS;
        
        BlockState blockState = world.getBlockState(blockPos);
        Block block = world.getBlockState(blockPos).getBlock();
        if (CropUtils.isGourd(block)) {
            /*
             * If Block is PUMPKIN or MELON and player is allowed to FARM
             */
            if (ClaimChunkUtils.canPlayerHarvestCrop(player, blockPos) || true) {
                Direction dir = Direction.NORTH;
                for (int i = 0; i < 4; i++) {
                    BlockState stem = world.getBlockState(blockPos.offset(dir));
                    if (stem.getBlock() instanceof AttachedStemBlock && stem.get(AttachedStemBlock.FACING) == dir.getOpposite())
                        return ActionResult.PASS;
                    
                    dir = dir.rotateYClockwise();
                }
            }
            
        } else if (block instanceof SugarCaneBlock) {
            /*
             * If Block is SUGARCANE, is NOT the bottom block, and player is allowed to FARM
             */
            BlockState groundState = world.getBlockState(blockPos.down());
            Block ground = groundState.getBlock();
            
            if ((ground instanceof SugarCaneBlock) && ClaimChunkUtils.canPlayerHarvestCrop(player, blockPos))
                return ActionResult.PASS;

        } else if (CropUtils.isCrop(block)) {
            /*
             * If block is a CROP, and the player is allowed to FARM
             */
            
            // Check growth
            boolean cropFullyGrown = CropUtils.isMature(blockState);
            
            // If the crop can be broken
            if (ClaimChunkUtils.canPlayerBreakInChunk(player, blockPos) || (cropFullyGrown && ClaimChunkUtils.canPlayerHarvestCrop(player, blockPos))) {
                if (cropFullyGrown) {
                    // Get the chunk information if we should replant
                    WorldChunk chunk = world.getWorldChunk(blockPos);
                    if ((chunk != null) && ((IClaimedChunk) chunk).isSetting(blockPos, ClaimSettings.CROP_AUTOREPLANT)) {
                        /*
                         * Automatically Replant the plant
                         */
                        
                        // Get the crops seed
                        Item cropSeed = CropUtils.getSeed(block);
                        
                        // Get the crop state with 0 growth
                        BlockState cropFresh = CropUtils.withAge(block, 0);
                        
                        // Get the drops
                        List<ItemStack> drops = Block.getDroppedStacks(blockState, world, blockPos, world.getBlockEntity(blockPos), player, player.getStackInHand(hand));
                        if (cropSeed != null) { // Only remove a seed if the seed exists
                            for (ItemStack stack : drops) {
                                // Check that item matches
                                if (!stack.getItem().equals(cropSeed))
                                    continue;
                                
                                // Negate a single seed
                                if (stack.getCount() > 0) {
                                    stack.setCount(stack.getCount() - 1);
                                    break;
                                }
                            }
                        }
                        
                        // Drop the items
                        drops.forEach(itemStack -> ItemScatterer.spawn(world, blockPos.getX(), blockPos.getY(), blockPos.getZ(), itemStack));
                        
                        // Set the crop to the baby plant
                        world.setBlockState(blockPos, cropFresh);
                        
                        // Fail the break
                        return ActionResult.FAIL;
                    }
                }
                
                return ActionResult.PASS;
            }
        }
        
        // If player has permission to break blocks
        return (ClaimChunkUtils.canPlayerBreakInChunk(player, blockPos) ? ActionResult.PASS : ActionResult.FAIL);
    }
    public static ActionResult canEndermenBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        if (entity instanceof EndermanEntity) {
            /*
             * Prevent an enderman from breaking claimed blocks
             */
            if (!ClaimChunkUtils.isSetting(ClaimSettings.ENDERMAN_GRIEFING, world, blockPos))
                return ActionResult.FAIL;
        }
        
        return ActionResult.PASS;
    }
    public static ActionResult canEnderdragonBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        if (entity instanceof EnderDragonEntity) {
            /*
             * Prevent the dragon from breaking items within SPAWN
             */
            IClaimedChunk chunk = ((IClaimedChunk)world.getChunk(blockPos));
            if (Objects.equals(chunk.getOwnerId(blockPos), CoreMod.SPAWN_ID) && !chunk.canPlayerDo(blockPos, null, ClaimPermissions.BLOCKS))
                return ActionResult.FAIL;
        }
        
        return ActionResult.PASS;
    }
    public static ActionResult canGhastsBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        if (entity instanceof GhastEntity) {
            /*
             * Prevent a ghast from breaking claimed blocks
             */
            if (!ClaimChunkUtils.isSetting(ClaimSettings.GHAST_GRIEFING, world, blockPos))
                return ActionResult.FAIL;
        }
        
        return ActionResult.PASS;
    }
    public static ActionResult canCreepersBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        if (entity instanceof CreeperEntity) {
            /*
             * Prevent a creeper from breaking claimed blocks
             */
            if (!ClaimChunkUtils.isSetting(ClaimSettings.CREEPER_GRIEFING, world, blockPos))
                return ActionResult.FAIL;
        }
        
        return ActionResult.PASS;
    }
    public static ActionResult canExplosiveBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        if (entity instanceof TntEntity) {
            OwnableEntity tnt = (OwnableEntity) entity;
            if (!ClaimChunkUtils.canPlayerBreakInChunk(tnt.getEntityOwner(), world, blockPos))
                return ActionResult.FAIL;
        }
        else if (entity instanceof ConstructableEntity constructableEntity) {
            /*
             * Prevent an origin-based entity from breaking blocks outside of it's origin
             */
            UUID entitySource = constructableEntity.getEntityOwner();
            Optional<UUID> chunkOwner = ClaimChunkUtils.getPosOwner(world, blockPos);
            if (chunkOwner.isPresent() && !ClaimChunkUtils.canPlayerBreakInChunk(entitySource, world, blockPos))
                return ActionResult.FAIL;
        }
        else if (entity instanceof ExplosiveProjectileEntity explosiveProjectile) {
            Entity owner = explosiveProjectile.getOwner();
            if (owner != null) {
                // Everybodys favorite gameshow: Recursion!
                return BlockBreakCallback.TEST.invoker().destroy(
                    owner,
                    world,
                    hand,
                    blockPos,
                    blockFace,
                    action
                );
            }
        }
        
        return ActionResult.PASS;
    }
}
