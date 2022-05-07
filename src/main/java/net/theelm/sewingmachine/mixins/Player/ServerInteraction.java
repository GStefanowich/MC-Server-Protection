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

package net.theelm.sewingmachine.mixins.Player;

import com.mojang.authlib.GameProfile;
import net.theelm.sewingmachine.enums.ChatRooms;
import net.theelm.sewingmachine.enums.Permissions;
import net.theelm.sewingmachine.interfaces.BlockBreakCallback;
import net.theelm.sewingmachine.interfaces.BlockInteractionCallback;
import net.theelm.sewingmachine.interfaces.ItemUseCallback;
import net.theelm.sewingmachine.interfaces.PlayerChat;
import net.theelm.sewingmachine.interfaces.PlayerPermissions;
import net.theelm.sewingmachine.protections.ranks.PlayerRank;
import net.theelm.sewingmachine.utilities.RankUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.UUID;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerInteraction implements PlayerPermissions, PlayerChat {
    
    @Shadow private BlockPos miningPos;
    
    @Shadow public ServerWorld world;
    @Shadow public ServerPlayerEntity player;
    
    /*
     * Chat Handlers
     */
    private ChatRooms chatRoom = ChatRooms.GLOBAL;
    private HashSet<UUID> mutedPlayers = new HashSet<>();
    private boolean isGlobalMuted = false;
    
    @Override @NotNull
    public ChatRooms getChatRoom() {
        return this.chatRoom;
    }
    @Override
    public void setChatRoom(@NotNull ChatRooms room) {
        // Set the chat room
        this.chatRoom = room;
    }
    
    @Override
    public boolean toggleMute() {
        return this.toggleMute(!this.isGlobalMuted);
    }
    @Override
    public boolean toggleMute(boolean muted) {
        return (this.isGlobalMuted = muted);
    }
    @Override
    public boolean toggleMute(GameProfile player) {
        UUID uuid = player.getId();
        if (!this.mutedPlayers.remove(uuid))
            return this.mutedPlayers.add(uuid);
        else return false;
    }
    @Override
    public boolean isMuted() {
        return this.isGlobalMuted && (!RankUtils.hasPermission(this.player, Permissions.CHAT_COMMAND_MUTE_EXEMPT));
    }
    @Override
    public boolean isMuted(@NotNull GameProfile player) {
        return this.mutedPlayers.contains(player.getId());
    }
    
    /*
     * Ranks
     */
    @Override
    public @NotNull PlayerRank[] getRanks() {
        return RankUtils.getPlayerRanks(this.player);
    }
    
    @Inject(at = @At("HEAD"), method = "processBlockBreakingAction", cancellable = true)
    private void onBlockBreakChange(BlockPos blockPos, Action action, Direction direction, int i, CallbackInfo info) {
        ActionResult result = BlockBreakCallback.EVENT.invoker().interact(this.player, this.world, this.player.preferredHand, blockPos, direction, action);
        if ( result != ActionResult.PASS ) {
            // Send the player a failed notice
            this.player.networkHandler.sendPacket(new PlayerActionResponseS2CPacket(blockPos, this.world.getBlockState(blockPos), action, false, "may not interact"));
            this.player.networkHandler.sendPacket(new BlockBreakingProgressS2CPacket(this.player.getId(), blockPos, -1));
            
            // Update the neighboring blocks on the client
            this.updateNeighboringBlockStates(blockPos);
            
            // Update the players mining position
            // Must to set to prevent (Block Mismatch) messages
            this.miningPos = blockPos;
            
            // Create a slow mining event
            //((LogicalWorld)this.world).addTickableEvent(new PlayerNoBreak(this.player, blockPos));
            
            // Cancel the rest of the event
            info.cancel();
        }
    }
    
    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    private void beforeItemInteract(@NotNull final ServerPlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        if (!player.world.isClient) {
            ActionResult result = ItemUseCallback.EVENT.invoker().use(player, world, hand, itemStack);
            if (result != ActionResult.PASS)
                callback.setReturnValue(result);
        }
    }
    
    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    private void beforeBlockInteract(@NotNull final ServerPlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, final BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callback) {
        if (!player.world.isClient) {
            ActionResult result = BlockInteractionCallback.EVENT.invoker().interact(player, world, hand, itemStack, blockHitResult);
            if (result != ActionResult.PASS) {
                this.updateNeighboringBlockStates(blockHitResult.getBlockPos());
                callback.setReturnValue(result);
            }
        }
    }
    
    private void updateNeighboringBlockStates(BlockPos blockPos) {
        final BlockState blockState = this.world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        BlockPos part = null;
        
        if ( block instanceof BedBlock ) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            BedPart bedPart = blockState.get(BedBlock.PART);
            part = blockPos.offset(bedPart == BedPart.HEAD ? facing.getOpposite() : facing);
        } else if ( block instanceof HorizontalFacingBlock ) {
            Direction facing = blockState.get(HorizontalFacingBlock.FACING);
            part = blockPos.offset(facing.getOpposite());
        } else if ( block instanceof TallPlantBlock ) {
            DoubleBlockHalf half = blockState.get(TallPlantBlock.HALF);
            part = blockPos.offset(half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        } else if ( block instanceof DoorBlock ) {
            DoubleBlockHalf half = blockState.get(DoorBlock.HALF);
            part = half == DoubleBlockHalf.LOWER ? blockPos.up() : blockPos.down();
        }
        
        if (part != null) this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.world, part));
        this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, blockState));
    }
}
