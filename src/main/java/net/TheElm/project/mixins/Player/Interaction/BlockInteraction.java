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

package net.TheElm.project.mixins.Player.Interaction;

import net.TheElm.project.enums.ShopSigns;
import net.TheElm.project.interfaces.ShopSignBlockEntity;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.network.DebugRendererInfoManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class BlockInteraction {
    
    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    private void beforeItemInteract(final PlayerEntity playerEntity, final World world, final ItemStack itemStack, final Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        //System.out.println( "Interacted with an item" );
    }
    
    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    private void beforeBlockInteract(final PlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, final BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callback) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        final Block block = world.getBlockState(blockPos).getBlock();
        final BlockEntity blockEntity = world.getBlockEntity( blockPos );
        
        // Check if the block interacted with is a sign (For shop signs)
        if ( blockEntity instanceof ShopSignBlockEntity && blockEntity instanceof SignBlockEntity ) {
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
                return;
            }
        }
        
        // If player is in creative ignore permissions
        if ( player.isCreative() || ( blockEntity instanceof EnderChestBlockEntity ) )
            return;
        
        // If block is a button, door, trapdoor, or gate
        if ( block instanceof AbstractButtonBlock || block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapdoorBlock ) {
            
            ClaimedChunk claimedChunkInfo = ClaimedChunk.convert( player.getEntityWorld(), blockPos );
            
            if (ChunkUtils.canPlayerToggleDoor( player, claimedChunkInfo ))
                return;
            
            // Cancel the event
            callback.setReturnValue(ActionResult.FAIL);
            
            // Update neighboring blocks
            DebugRendererInfoManager.sendNeighborUpdate( world, blockPos );
            
            return;
        }
        
        // If the block is something that can be accessed (Like a chest)
        if ( (!player.isSneaking() || (!(itemStack.getItem() instanceof BlockItem))) && ( isLockable( blockEntity ) || isLockable( block ) ) ) {
            if ( player.isSpectator() )
                return;
            
            ClaimedChunk claimedChunkInfo = ClaimedChunk.convert( player.getEntityWorld(), blockPos );
            
            if (ChunkUtils.canPlayerLootChestsInChunk( player, claimedChunkInfo ))
                return;
            
            // Cancel the event
            callback.setReturnValue(ActionResult.FAIL);
            
            // Play a sound to the player
            world.playSound( (PlayerEntity)null, blockPos, EntityUtils.getLockSound( block ), SoundCategory.BLOCKS, 0.5f, 1f );
            
            // Display that this item can't be opened
            TitleUtils.showPlayerAlert( player, Formatting.WHITE, TranslatableServerSide.text( player, "claim.block.locked",
                EntityUtils.getLockedName( block ),
                ( claimedChunkInfo == null ? new LiteralText( "unknown player" ).formatted(Formatting.LIGHT_PURPLE) : claimedChunkInfo.getOwnerName() )
            ));
            
            return;
        }
        
        // Get where the block is being placed at
        BlockState blockState   = world.getBlockState( blockPos );
        Material material = block.getMaterial( blockState );
        if (player.isSneaking() || (!material.isReplaceable()))
            blockPos = blockPos.offset(blockHitResult.getSide());
        
        // Test the players permissions to the chunk
        if (ChunkUtils.canPlayerBreakInChunk( player, blockPos ))
            return;
        
        // If cannot break, prevent the action
        callback.setReturnValue(ActionResult.FAIL);
    }
    
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
    private static boolean isLockable( BlockEntity block ) {
        return ( block instanceof LockableContainerBlockEntity );
    }
}
