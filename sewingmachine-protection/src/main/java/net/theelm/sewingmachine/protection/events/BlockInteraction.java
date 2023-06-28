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
import net.minecraft.block.ButtonBlock;
import net.minecraft.sound.BlockSoundGroup;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.interfaces.BlockInteractionCallback;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.EntityLockUtils;
import net.theelm.sewingmachine.protections.logging.BlockEvent;
import net.theelm.sewingmachine.protections.logging.EventLogger;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
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
    public static void register(@NotNull Event<BlockInteractionCallback> event) {
        // Interactions with redstone objects
        event.register(BlockInteraction::interactSwitches);
        
        // Interactions with doors
        event.register(BlockInteraction::interactDoors);
    }
    
    private static ActionResult interactSwitches(@NotNull ServerPlayerEntity player, @NotNull World world, Hand hand, @NotNull ItemStack itemStack, @NotNull BlockHitResult blockHitResult) {
        final BlockPos blockPos = blockHitResult.getBlockPos();
        final BlockState blockState = world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        
        // Block must be a button or lever
        if (!(block instanceof ButtonBlock || block instanceof LeverBlock))
            return ActionResult.PASS;
        
        // If allowed to bypass permissions in creative mode
        if (SewConfig.get(SewCoreConfig.CLAIM_CREATIVE_BYPASS) && player.isCreative())
            return ActionResult.SUCCESS;
        
        WorldChunk chunk = player.getEntityWorld()
            .getWorldChunk(blockPos);
        return ClaimChunkUtils.canPlayerToggleMechanisms(player, chunk, blockPos) ? ActionResult.SUCCESS : ActionResult.FAIL;
    }
    
    private static ActionResult interactDoors(@NotNull ServerPlayerEntity player, @NotNull World world, Hand hand, @NotNull ItemStack itemStack, @NotNull BlockHitResult blockHitResult) {
        final BlockPos blockPos = blockHitResult.getBlockPos();
        final BlockState blockState = world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        
        // If block is a door, trapdoor, or gate
        if (!(block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapdoorBlock))
            return ActionResult.PASS;
        
        // If allowed to bypass permissions in creative mode
        if (SewConfig.get(SewCoreConfig.CLAIM_CREATIVE_BYPASS) && player.isCreative())
            return ActionResult.SUCCESS;
        
        WorldChunk chunk = player.getEntityWorld()
            .getWorldChunk(blockPos);
        if (ClaimChunkUtils.canPlayerToggleDoor(player, chunk, blockPos)) {
            // TODO: Remove double door function out of the PROTECTION module
            // Toggle double doors
            if ((!player.isSneaking()) && block instanceof DoorBlock && (blockState.getSoundGroup() != BlockSoundGroup.METAL)) {
                DoubleBlockHalf doorHalf = blockState.get(DoorBlock.HALF);
                Direction doorDirection = blockState.get(DoorBlock.FACING);
                DoorHinge doorHinge = blockState.get(DoorBlock.HINGE);

                BlockPos otherDoorPos = blockPos.offset(doorHinge == DoorHinge.LEFT ? doorDirection.rotateYClockwise() : doorDirection.rotateYCounterclockwise())
                    .offset(Direction.UP, doorHalf == DoubleBlockHalf.UPPER ? 0 : 1);
                BlockState otherDoorState = world.getBlockState(otherDoorPos);

                // Other block is DOOR, Material matches, is same door half, and opposite hinge
                if ((otherDoorState.getBlock() instanceof DoorBlock) && (blockState.getSoundGroup() == otherDoorState.getSoundGroup()) && (otherDoorState.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) && (doorHinge != otherDoorState.get(DoorBlock.HINGE))) {
                    boolean doorIsOpen = blockState.get(DoorBlock.OPEN);
                    boolean otherIsOpen = otherDoorState.get(DoorBlock.OPEN);

                    if (doorIsOpen == otherIsOpen) {
                        // Toggle the doors
                        world.setBlockState(otherDoorPos, otherDoorState.with(DoorBlock.OPEN, !doorIsOpen), 10);
                        world.syncWorldEvent(player, otherIsOpen ? 1006 : 1012, otherDoorPos, 0);
                    }
                }
            }
            
            return ActionResult.PASS;
        }
        
        return ActionResult.FAIL;
    }
    
    private static ActionResult blockInteract(@NotNull ServerPlayerEntity player, @NotNull World world, Hand hand, @NotNull ItemStack itemStack, @NotNull BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        final BlockState blockState = world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        final BlockEntity blockEntity = world.getBlockEntity(blockPos);
        
        final Lazy<WorldChunk> claimedChunkInfo = new Lazy<>(() -> player.getEntityWorld().getWorldChunk(blockPos));
        
        // Check if the block interacted with is a sign (For shop signs)
        if ( blockEntity instanceof ShopSignData shopSign && blockEntity instanceof SignBlockEntity) {
            ShopSign shopSignType;
            
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
        
        // If the block is something that can be accessed (Like a chest)
        if ( (!player.shouldCancelInteraction() || (!(itemStack.getItem() instanceof BlockItem || itemStack.getItem() instanceof BucketItem))) ) {
            if ( player.isSpectator() || (blockEntity instanceof EnderChestBlockEntity))
                return ActionResult.PASS;
            
            // Get the permission of the block
            ClaimPermissions blockPermission;
            if ((((blockPermission = EntityLockUtils.getLockPermission(blockEntity)) != null) || ((blockPermission = EntityLockUtils.getLockPermission(block)) != null))) {
                // Check if allowed to open storages in this location
                if (ClaimChunkUtils.canPlayerDoInChunk(blockPermission, player, claimedChunkInfo.get(), blockPos)) {
                    // Check if the chest is NOT part of a shop, Or the player owns that shop
                    ShopSignData shopSign;
                    if ((!EntityUtils.isValidShopContainer(blockEntity)) || ((shopSign = EntityUtils.getAttachedShopSign(world, blockPos)) == null) || player.getUuid().equals(shopSign.getShopOwner()))
                        return ActionResult.PASS;
                }
                
                // Play a sound to the player
                EntityLockUtils.playLockSoundFromSource(blockEntity, blockState, player);
                
                // FAIL that the result is not allowed
                return ActionResult.FAIL;
            }
        }
        
        // Test if the block can be placed
        return BlockInteraction.blockPlace(player, world, hand, itemStack, blockHitResult);
    }
    private static ActionResult blockPlace(@NotNull ServerPlayerEntity player, @NotNull World world, Hand hand, @NotNull ItemStack itemStack, @NotNull BlockHitResult blockHitResult) {
        // Get the item being used
        final Item item = itemStack.getItem();
        
        // Get where the block is being placed at
        BlockPos blockPos = blockHitResult.getBlockPos();
        
        // Get the block material
        BlockState blockState = world.getBlockState( blockPos );
        
        // Adjust the block offset
        if (player.isSneaking() || ((item instanceof BlockItem) && (!blockState.isReplaceable())))
            blockPos = blockPos.offset(blockHitResult.getSide());
        
        // Test if allowed
        ActionResult result;
        if (((result = BlockInteraction.canBlockPlace(player, blockPos, blockHitResult)) != ActionResult.FAIL) && SewConfig.get(SewCoreConfig.LOG_BLOCKS_BREAKING)) {
            if (item instanceof BlockItem blockItem)
                EventLogger.log(new BlockEvent(player, EventLogger.BlockAction.PLACE, blockItem.getBlock(), blockPos));
            else
                CoreMod.logDebug("Player \"placed\" non-block item \"" + item.getTranslationKey() + "\" at " + MessageUtils.xyzToString(blockPos));
        }
        return result;
    }
    private static ActionResult canBlockPlace(@NotNull ServerPlayerEntity player, @NotNull BlockPos blockPos, @NotNull BlockHitResult blockHitResult) {
        // Test the players permissions to the chunk
        if (ClaimChunkUtils.canPlayerBreakInChunk(player, blockPos))
            return ActionResult.PASS;
        
        // If cannot break, prevent the action
        return ActionResult.FAIL;
    }
    
}
