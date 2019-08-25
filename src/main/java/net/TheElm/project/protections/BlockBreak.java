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

package net.TheElm.project.protections;

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.BlockBreakCallback;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.utilities.LoggingUtils;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.packet.PlayerActionC2SPacket.Action;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public final class BlockBreak {
    
    private BlockBreak() {}
    
    public static void init() {
        BlockBreakCallback.EVENT.register(BlockBreak::blockBreak);
    }
    
    private static ActionResult blockBreak(PlayerEntity player, World world, Hand hand, BlockPos blockPos, Direction blockFace, Action action) {
        ActionResult result;
        if (((result = BlockBreak.canBlockBreak( player, world, hand, blockPos, blockFace, action)) != ActionResult.FAIL) && SewingMachineConfig.INSTANCE.LOG_BLOCKS_BREAKING.get() && (action == Action.STOP_DESTROY_BLOCK))
            LoggingUtils.logAction( LoggingUtils.BlockAction.BREAK, world.getBlockState(blockPos).getBlock(), blockPos, player );
        return result;
    }
    private static ActionResult canBlockBreak(PlayerEntity player, World world, Hand hand, BlockPos blockPos, Direction blockFace, Action action) {
        // If player is in creative
        if (player.isCreative() || (action == Action.ABORT_DESTROY_BLOCK))
            return ActionResult.PASS;
        
        BlockState blockState = world.getBlockState(blockPos);
        Block block = world.getBlockState(blockPos).getBlock();
        if ( block instanceof PumpkinBlock || block instanceof MelonBlock ) {
            
            /*
             * If Block is PUMPKIN or MELON and player is allowed to FARM
             */
            if (ChunkUtils.canPlayerHarvestCrop(player, blockPos))
                return ActionResult.PASS;
            
        } else if ( block instanceof SugarCaneBlock ) {
            
            /*
             * If Block is SUGARCANE, is NOT the bottom block, and player is allowed to FARM
             */
            BlockState groundState = world.getBlockState(blockPos.down());
            Block ground = groundState.getBlock();
            
            if ( ( ground instanceof SugarCaneBlock ) && ChunkUtils.canPlayerHarvestCrop(player, blockPos) )
                return ActionResult.PASS;
            
        } else if ( block instanceof CropBlock ) {
            
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
            if ( playerCanBreak || (cropFullyGrown && playerCanHarvest) ) {
                
                if ( cropFullyGrown ) {
                    // Get the chunk information if we should replant
                    ClaimedChunk chunkData = ClaimedChunk.convert(world, blockPos);
                    if ((chunkData != null) && chunkData.isSetting(ClaimSettings.CROP_AUTOREPLANT)) {
                        /*
                         * Automatically Replant the plant
                         */
                        
                        // Get the crops seed
                        Item cropSeed = BlockBreak.getCropSeed( cropBlock );
                        
                        // Get the crop state with 0 growth
                        BlockState cropFresh = cropBlock.withAge(0);
                        
                        // Get the drops
                        List<ItemStack> drops = Block.getDroppedStacks( blockState, (ServerWorld) world, blockPos, world.getBlockEntity( blockPos ), player, player.getStackInHand( hand ) );
                        for ( ItemStack stack : drops ) {
                            // Check that item matches
                            if ( !stack.getItem().equals( cropSeed ) )
                                continue;
                            
                            // Negate a single seed
                            if ( stack.getCount() > 0 ) {
                                stack.setCount(stack.getCount() - 1);
                                break;
                            }
                        }
                        
                        // Drop the items
                        drops.forEach(itemStack -> ItemScatterer.spawn( world, blockPos.getX(), blockPos.getY(), blockPos.getZ(), itemStack ));
                        
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
        return (ChunkUtils.canPlayerBreakInChunk(player, blockPos) ? ActionResult.PASS : ActionResult.FAIL );
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
