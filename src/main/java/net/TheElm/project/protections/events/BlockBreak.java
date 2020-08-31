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

package net.TheElm.project.protections.events;

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.BlockBreakCallback;
import net.TheElm.project.interfaces.BlockBreakEventCallback;
import net.TheElm.project.interfaces.ConstructableEntity;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.OwnableEntity;
import net.TheElm.project.protections.logging.BlockEvent;
import net.TheElm.project.protections.logging.EventLogger;
import net.TheElm.project.protections.logging.EventLogger.BlockAction;
import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.block.BeetrootsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarrotsBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.MelonBlock;
import net.minecraft.block.PotatoesBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import java.util.Optional;
import java.util.UUID;

public final class BlockBreak {
    
    private BlockBreak() {}
    
    /**
     * Initialize our callback listener for Block Breaking
     */
    public static void init() {
        // Register the block break event
        BlockBreakCallback.EVENT.register(BlockBreak::blockBreak);
        
        // Register actions to take after blocks have been broken
        BlockBreakEventCallback.EVENT.register(BlockEvents::eventTreeCapacitator);
        BlockBreakEventCallback.EVENT.register(BlockEvents::eventVeinMiner);
    }
    
    /**
     * Check if a block can be broken, and log it if it can
     * @param entity The entity that broke the block
     * @param world The world that the block is in
     * @param hand The hand that the player is using
     * @param blockPos The blocks X, Y, Z position
     * @param blockFace The side of the block that is being broken from
     * @param action The break status of the block
     * @return If the block is allowed to be broken
     */
    private static ActionResult blockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        ActionResult result;
        if (((result = BlockBreak.canBlockBreak( entity, world, hand, blockPos, blockFace, action)) != ActionResult.FAIL) && SewConfig.get(SewConfig.LOG_BLOCKS_BREAKING) && (action == Action.STOP_DESTROY_BLOCK))
            BlockBreak.onSucceedBreak( entity, world, hand, blockPos, blockFace );
        return result;
    }
    
    /**
     * Check if a block can be broken
     * @param entity The entity that broke the block
     * @param world The world that the block is in
     * @param hand The hand that the player is using
     * @param blockPos The blocks X, Y, Z position
     * @param blockFace The side of the block that is being broken from
     * @param action The break status of the block
     * @return If the block is allowed to be broken
     */
    public static ActionResult canBlockBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace, @Nullable final Action action) {
        if (entity == null && CoreMod.isDebugging())
            CoreMod.logError(new NullPointerException("'entity' is a Null."));
        
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            
            // If player is in creative
            if ((player.isCreative() && SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS)) || (action == Action.ABORT_DESTROY_BLOCK))
                return ActionResult.PASS;
            
            BlockState blockState = world.getBlockState(blockPos);
            Block block = world.getBlockState(blockPos).getBlock();
            if (block instanceof PumpkinBlock || block instanceof MelonBlock) {
                /*
                 * If Block is PUMPKIN or MELON and player is allowed to FARM
                 */
                if (ChunkUtils.canPlayerHarvestCrop(player, blockPos))
                    return ActionResult.PASS;
                
            } else if (block instanceof SugarCaneBlock) {
                
                /*
                 * If Block is SUGARCANE, is NOT the bottom block, and player is allowed to FARM
                 */
                BlockState groundState = world.getBlockState(blockPos.down());
                Block ground = groundState.getBlock();
                
                if ((ground instanceof SugarCaneBlock) && ChunkUtils.canPlayerHarvestCrop(player, blockPos))
                    return ActionResult.PASS;
                
            } else if (block instanceof CropBlock) {
                
                /*
                 * If block is a CROP, and the player is allowed to FARM
                 */
                
                // Cast the crop
                CropBlock cropBlock = (CropBlock) block;
                
                // Check growth
                boolean cropFullyGrown = cropBlock.isMature(blockState);
                
                // Check player permissions
                boolean playerCanHarvest = ChunkUtils.canPlayerHarvestCrop(player, blockPos);
                boolean playerCanBreak = ChunkUtils.canPlayerBreakInChunk(player, blockPos);
                
                // If the crop can be broken
                if (playerCanBreak || (cropFullyGrown && playerCanHarvest)) {
                    
                    if (cropFullyGrown) {
                        // Get the chunk information if we should replant
                        WorldChunk chunk = world.getWorldChunk(blockPos);
                        if ((chunk != null) && ((IClaimedChunk) chunk).isSetting(blockPos, ClaimSettings.CROP_AUTOREPLANT)) {
                            /*
                             * Automatically Replant the plant
                             */
                            
                            // Get the crops seed
                            Item cropSeed = BlockBreak.getCropSeed(cropBlock);
                            
                            // Get the crop state with 0 growth
                            BlockState cropFresh = cropBlock.withAge(0);
                            
                            // Get the drops
                            List<ItemStack> drops = Block.getDroppedStacks(blockState, world, blockPos, world.getBlockEntity(blockPos), player, player.getStackInHand(hand));
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
            return (ChunkUtils.canPlayerBreakInChunk(player, blockPos) ? ActionResult.PASS : ActionResult.FAIL);
        }
        else if (entity instanceof TntEntity) {
            OwnableEntity tnt = (OwnableEntity) entity;
            if (!ChunkUtils.canPlayerBreakInChunk(tnt.getEntityOwner(), world, blockPos))
                return ActionResult.FAIL;
        }
        else if (entity instanceof GhastEntity) {
            /*
             * Prevent a ghast from breaking claimed blocks
             */
            if (!ChunkUtils.isSetting(ClaimSettings.GHAST_GRIEFING, world, blockPos))
                return ActionResult.FAIL;
        }
        else if (entity instanceof CreeperEntity) {
            /*
             * Prevent a creeper from breaking claimed blocks
             */
            if (!ChunkUtils.isSetting(ClaimSettings.CREEPER_GRIEFING, world, blockPos))
                return ActionResult.FAIL;
        }
        else if (entity instanceof ConstructableEntity) {
            /*
             * Prevent an origin-based entity from breaking blocks outside of its origin
             */
            UUID entitySource = ((ConstructableEntity)entity).getEntityOwner();
            Optional<UUID> chunkOwner = ChunkUtils.getPosOwner(world, blockPos);
            if (chunkOwner.isPresent() && !ChunkUtils.canPlayerBreakInChunk( entitySource, world, blockPos ))
                return ActionResult.FAIL;
        }
        else if (entity instanceof ExplosiveProjectileEntity) {
            /*
             * Get the owner of the projectile
             */
            return BlockBreak.canBlockBreak(
                ((ExplosiveProjectileEntity)entity).getOwner(),
                world,
                hand,
                blockPos,
                blockFace,
                action
            );
        }
        else {
            if (entity != null) CoreMod.logDebug( entity.getClass().getCanonicalName() + " broke a block!" );
        }
        
        return ActionResult.PASS;
    }
    
    /**
     * When a block is successfully run, perform actions based on the block
     * @param entity 
     * @param world 
     * @param hand 
     * @param blockPos 
     * @param blockFace 
     */
    private static void onSucceedBreak(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final Hand hand, @NotNull final BlockPos blockPos, @Nullable final Direction blockFace) {
        // Log the block being broken
        BlockBreak.logBlockBreakEvent( entity, world, blockPos );
        
        // Take additional actions if the entity breaking is a player
        if (entity instanceof ServerPlayerEntity)
            BlockBreakEventCallback.EVENT.invoker().activate((ServerPlayerEntity) entity, world, hand, blockPos, blockFace);
    }
    
    /**
     * Log the event of our block being broken into SQL
     * @param entity The entity responsible for breaking the block
     * @param world The world that the block was broken in
     * @param blockPos The position that the block was broken at
     */
    private static void logBlockBreakEvent(@Nullable final Entity entity, @NotNull final ServerWorld world, @NotNull final BlockPos blockPos) {
        EventLogger.log(new BlockEvent(entity, BlockAction.BREAK, world.getBlockState(blockPos).getBlock(), blockPos));
    }
    
    public static Item getCropSeed(Block crop) {
        if ( crop instanceof CarrotsBlock )
            return Items.CARROT;
        if ( crop instanceof PotatoesBlock )
            return Items.POTATO;
        if ( crop instanceof BeetrootsBlock )
            return Items.BEETROOT_SEEDS;
        return Items.WHEAT_SEEDS;
    }
}
