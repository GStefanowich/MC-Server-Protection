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
import net.TheElm.project.enums.ShopSigns;
import net.TheElm.project.interfaces.BlockInteractionCallback;
import net.TheElm.project.interfaces.ShopSignBlockEntity;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.utilities.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.packet.GuiSlotUpdateS2CPacket;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockInteraction {
    private BlockInteraction() {}
    
    /**
     * Initialize our callback listener for Block Interaction
     */
    public static void init() {
        BlockInteractionCallback.EVENT.register(BlockInteraction::blockInteract);
    }
    
    private static ActionResult blockInteract(ServerPlayerEntity player, World world, Hand hand, ItemStack itemStack, BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        final Block block = world.getBlockState(blockPos).getBlock();
        final BlockEntity blockEntity = world.getBlockEntity( blockPos );
        
        // Check if the block interacted with is a sign (For shop signs)
        if ( blockEntity instanceof ShopSignBlockEntity && blockEntity instanceof SignBlockEntity) {
            SignBlockEntity sign = (SignBlockEntity) blockEntity;
            ShopSignBlockEntity shopSign = (ShopSignBlockEntity) blockEntity;
            
            ShopSigns shopSignType;
            // Interact with the sign
            if ((shopSign.getShopOwner() != null) && ((shopSignType = ShopSigns.valueOf( sign.text[0] )) != null)) {
                shopSignType.onInteract((ServerPlayerEntity) player, blockPos, shopSign)
                    // Literal Text (Error)
                    .ifLeft((text) -> {
                        player.playSound(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
                        ((ServerPlayerEntity) player).sendChatMessage(text.formatted(Formatting.RED), MessageType.GAME_INFO);
                    })
                    // Boolean if success/fail
                    .ifRight((bool) -> {
                        if (!bool)
                            player.playSound(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
                    });
                return ActionResult.PASS;
            }
        }
        
        // If player is in creative ignore permissions
        if ( (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get()) || player.isCreative() || ( blockEntity instanceof EnderChestBlockEntity) )
            return ActionResult.PASS;
        
        // If block is a button, door, trapdoor, or gate
        if ( block instanceof AbstractButtonBlock || block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapdoorBlock) {
            ClaimedChunk claimedChunkInfo = ClaimedChunk.convert( player.getEntityWorld(), blockPos );
            
            if (ChunkUtils.canPlayerToggleDoor( player, claimedChunkInfo ))
                return ActionResult.PASS;
            
            return ActionResult.FAIL;
        }

        // If the block is something that can be accessed (Like a chest)
        if ( (!player.isSneaking() || (!(itemStack.getItem() instanceof BlockItem))) && ( isLockable( blockEntity ) || isLockable( block ) ) ) {
            if ( player.isSpectator() )
                return ActionResult.PASS;
            
            ClaimedChunk claimedChunkInfo = ClaimedChunk.convert( player.getEntityWorld(), blockPos );
            
            if (ChunkUtils.canPlayerLootChestsInChunk( player, claimedChunkInfo ))
                return ActionResult.PASS;
            
            // Play a sound to the player
            world.playSound( null, blockPos, EntityUtils.getLockSound( block ), SoundCategory.BLOCKS, 0.5f, 1f );

            // Display that this item can't be opened
            TitleUtils.showPlayerAlert( player, Formatting.WHITE, TranslatableServerSide.text( player, "claim.block.locked",
                EntityUtils.getLockedName( block ),
                ( claimedChunkInfo == null ? new LiteralText( "unknown player" ).formatted(Formatting.LIGHT_PURPLE) : claimedChunkInfo.getOwnerName( player ) )
            ));
            
            return ActionResult.FAIL;
        }
        
        ActionResult placeResult = BlockInteraction.blockPlace( player, world, hand, itemStack, blockHitResult);
        // Adjust the stack size of the players placed block
        if (placeResult == ActionResult.FAIL) {
            PlayerInventory inventory = player.inventory;
            int slot = inventory.selectedSlot;
            player.networkHandler.sendPacket(new GuiSlotUpdateS2CPacket(-2, slot, inventory.getInvStack(slot)));
        }
        return placeResult;
    }
    private static ActionResult blockPlace(ServerPlayerEntity player, World world, Hand hand, ItemStack itemStack, BlockHitResult blockHitResult) {
        // Get where the block is being placed at
        BlockPos blockPos = blockHitResult.getBlockPos();
        
        // Get the block material
        BlockState blockState = world.getBlockState( blockPos );
        Block block = blockState.getBlock();
        Material material = block.getMaterial( blockState );
        
        // Adjust the block offset
        if (player.isSneaking() || (!material.isReplaceable()))
            blockPos = blockPos.offset(blockHitResult.getSide());
        
        // Test if allowed
        ActionResult result;
        if (((result = BlockInteraction.canBlockPlace(player, blockPos, blockHitResult)) != ActionResult.FAIL) && SewingMachineConfig.INSTANCE.LOG_BLOCKS_BREAKING.get())
            LoggingUtils.logAction( LoggingUtils.BlockAction.PLACE, itemStack.getItem(), blockPos, player );
        
        return result;
    }
    private static ActionResult canBlockPlace(ServerPlayerEntity player, BlockPos blockPos, BlockHitResult blockHitResult) {
        // Test the players permissions to the chunk
        if (ChunkUtils.canPlayerBreakInChunk( player, blockPos ))
            return ActionResult.PASS;
        
        // If cannot break, prevent the action
        return ActionResult.FAIL;
    }
    
    /**
     * @param block A block to determine if it should be lockable
     * @return If the block type provided is a protected container
     */
    private static boolean isLockable( Block block ) {
        if ( block instanceof FletchingTableBlock )
            return true;
        if ( block instanceof SmithingTableBlock )
            return true;
        if ( block instanceof CraftingTableBlock )
            return true;
        if ( block instanceof BeaconBlock )
            return true;
        if ( block instanceof EnchantingTableBlock )
            return true;
        if ( block instanceof GrindstoneBlock )
            return true;
        if ( block instanceof LoomBlock )
            return true;
        if ( block instanceof StonecutterBlock )
            return true;
        if ( block instanceof AnvilBlock )
            return true;
        if ( block instanceof CartographyTableBlock )
            return true;
        if ( block instanceof BellBlock )
            return true;
        if ( block instanceof LecternBlock )
            return true;
        if ( block instanceof FlowerPotBlock )
            return true;
        return false;
    }
    
    /**
     * @param block A block entity to determine if it should be lockable
     * @return If the block type provided is a protected container
     */
    private static boolean isLockable( BlockEntity block ) {
        return ( block instanceof LockableContainerBlockEntity);
    }
}
