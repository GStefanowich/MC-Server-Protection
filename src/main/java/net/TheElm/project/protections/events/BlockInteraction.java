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
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ShopSigns;
import net.TheElm.project.interfaces.BlockInteractionCallback;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.ShopSignData;
import net.TheElm.project.protections.logging.BlockEvent;
import net.TheElm.project.protections.logging.EventLogger;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.Material;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Lazy;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

public final class BlockInteraction {
    private BlockInteraction() {}
    
    /**
     * Initialize our callback listener for Block Interaction
     */
    public static void init() {
        BlockInteractionCallback.EVENT.register(BlockInteraction::blockInteract);
    }
    
    private static ActionResult blockInteract(@NotNull ServerPlayerEntity player, @NotNull World world, Hand hand, @NotNull ItemStack itemStack, @NotNull BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        final BlockState blockState = world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        final BlockEntity blockEntity = world.getBlockEntity(blockPos);
        
        final Lazy<WorldChunk> claimedChunkInfo = new Lazy<>(() -> player.getEntityWorld().getWorldChunk(blockPos));
        
        // Check if the block interacted with is a sign (For shop signs)
        if ( blockEntity instanceof ShopSignData shopSign && blockEntity instanceof SignBlockEntity sign) {
            ShopSigns shopSignType;
            
            // Interact with the sign
            if ((shopSign.getShopOwner() != null) && ((shopSignType = shopSign.getShopType()) != null)) {
                shopSignType.onInteract(world.getServer(), player, blockPos, shopSign)
                    // Literal Text (Error)
                    .ifLeft((text) -> {
                        shopSign.playSound(player, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL);
                        TitleUtils.showPlayerAlert(player, Formatting.RED, text);
                    })
                    // Boolean if success/fail
                    .ifRight((bool) -> {
                        if (!bool)
                            shopSign.playSound(player, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL);
                        else if (shopSign.getSoundSourcePosition() != null && world.random.nextInt(12) == 0)
                            shopSign.playSound(player, SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL);
                    });
                return ActionResult.SUCCESS;
            }
        }
        
        // If block is a button or lever
        if (block instanceof AbstractButtonBlock || block instanceof LeverBlock) {
            if ((!SewConfig.get(SewConfig.DO_CLAIMS))
                || (SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS) && player.isCreative())
                || ChunkUtils.canPlayerToggleMechanisms(player, claimedChunkInfo.get(), blockPos))
            {
                return ActionResult.PASS;
            }
        }
        
        // If block is a button, door, trapdoor, or gate
        if (block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapdoorBlock) {
            if ((!SewConfig.get(SewConfig.DO_CLAIMS))
                || (SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS) && player.isCreative())
                || ChunkUtils.canPlayerToggleDoor(player, claimedChunkInfo.get(), blockPos))
            {
                // Toggle double doors
                if ((!player.isSneaking()) && block instanceof DoorBlock && (blockState.getMaterial() != Material.METAL)) {
                    DoubleBlockHalf doorHalf = blockState.get(DoorBlock.HALF);
                    Direction doorDirection = blockState.get(DoorBlock.FACING);
                    DoorHinge doorHinge = blockState.get(DoorBlock.HINGE);
                    
                    BlockPos otherDoorPos = blockPos.offset(doorHinge == DoorHinge.LEFT ? doorDirection.rotateYClockwise() : doorDirection.rotateYCounterclockwise())
                        .offset(Direction.UP, doorHalf == DoubleBlockHalf.UPPER ? 0 : 1);
                    BlockState otherDoorState = world.getBlockState(otherDoorPos);
                    
                    // Other block is DOOR, Material matches, is same door half, and opposite hinge
                    if ((otherDoorState.getBlock() instanceof DoorBlock) && (blockState.getMaterial() == otherDoorState.getMaterial()) && (otherDoorState.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) && (doorHinge != otherDoorState.get(DoorBlock.HINGE))) {
                        boolean doorIsOpen = blockState.get(DoorBlock.OPEN);
                        boolean otherIsOpen = otherDoorState.get(DoorBlock.OPEN);
                        
                        if (doorIsOpen == otherIsOpen) {
                            // Toggle the doors
                            world.setBlockState(otherDoorPos, otherDoorState.with(DoorBlock.OPEN, !doorIsOpen), 10);
                            world.syncWorldEvent(player, otherIsOpen ? 1006 : 1012, otherDoorPos, 0);
                        }
                    }
                }
                
                // Allow the action
                return ActionResult.PASS;
            }
            
            return ActionResult.FAIL;
        }
        
        // If the block is something that can be accessed (Like a chest)
        if ( (!player.shouldCancelInteraction() || (!(itemStack.getItem() instanceof BlockItem || itemStack.getItem() instanceof BucketItem))) ) {
            if ( player.isSpectator() || (blockEntity instanceof EnderChestBlockEntity))
                return ActionResult.PASS;
            
            // Get the permission of the block
            ClaimPermissions blockPermission;
            if ((((blockPermission = EntityUtils.getLockPermission(blockEntity)) != null) || ((blockPermission = EntityUtils.getLockPermission(block)) != null))) {
                // Check if allowed to open storages in this location
                if (ChunkUtils.canPlayerDoInChunk(blockPermission, player, claimedChunkInfo.get(), blockPos)) {
                    // Check if the chest is NOT part of a shop, Or the player owns that shop
                    ShopSignData shopSign;
                    if ((!EntityUtils.isValidShopContainer(blockEntity)) || ((shopSign = EntityUtils.getAttachedShopSign(world, blockPos)) == null) || player.getUuid().equals(shopSign.getShopOwner()))
                        return ActionResult.PASS;
                }
                
                // Play a sound to the player
                world.playSound(null, blockPos, EntityUtils.getLockSound(block, blockState, blockEntity), SoundCategory.BLOCKS, 0.5f, 1f);
                
                // Display that this item can't be opened
                TitleUtils.showPlayerAlert(player, Formatting.WHITE, TranslatableServerSide.text(player, "claim.block.locked",
                    EntityUtils.getLockedName(block),
                    (claimedChunkInfo.get() == null ? new LiteralText("unknown player").formatted(Formatting.LIGHT_PURPLE) : ((IClaimedChunk) claimedChunkInfo.get()).getOwnerName(player, blockPos))
                ));
                
                return ActionResult.FAIL;
            }
        }
        
        // Test if the block can be placed
        ActionResult placeResult = BlockInteraction.blockPlace(player, world, hand, itemStack, blockHitResult);
        
        // Adjust the stack size of the players placed block
        if (placeResult == ActionResult.FAIL)
            EntityUtils.resendInventory(player);
        return placeResult;
    }
    private static ActionResult blockPlace(@NotNull ServerPlayerEntity player, @NotNull World world, Hand hand, @NotNull ItemStack itemStack, @NotNull BlockHitResult blockHitResult) {
        // Get the item being used
        final Item item = itemStack.getItem();
        
        // Get where the block is being placed at
        BlockPos blockPos = blockHitResult.getBlockPos();
        
        // Get the block material
        BlockState blockState = world.getBlockState( blockPos );
        Material material = blockState.getMaterial();
        
        // Adjust the block offset
        if (player.isSneaking() || ((item instanceof BlockItem) && (!material.isReplaceable())))
            blockPos = blockPos.offset(blockHitResult.getSide());
        
        // Test if allowed
        ActionResult result;
        if (((result = BlockInteraction.canBlockPlace(player, blockPos, blockHitResult)) != ActionResult.FAIL) && SewConfig.get(SewConfig.LOG_BLOCKS_BREAKING)) {
            if (item instanceof BlockItem blockItem)
                EventLogger.log(new BlockEvent(player, EventLogger.BlockAction.PLACE, blockItem.getBlock(), blockPos));
            else
                CoreMod.logDebug("Player \"placed\" non-block item \"" + item.getTranslationKey() + "\" at " + MessageUtils.xyzToString(blockPos));
        }
        return result;
    }
    private static ActionResult canBlockPlace(@NotNull ServerPlayerEntity player, @NotNull BlockPos blockPos, @NotNull BlockHitResult blockHitResult) {
        // Test the players permissions to the chunk
        if (ChunkUtils.canPlayerBreakInChunk(player, blockPos))
            return ActionResult.PASS;
        
        // If cannot break, prevent the action
        return ActionResult.FAIL;
    }
    
}
