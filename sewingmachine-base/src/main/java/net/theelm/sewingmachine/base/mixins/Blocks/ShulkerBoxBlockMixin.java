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

package net.theelm.sewingmachine.base.mixins.Blocks;

import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.interfaces.BossLootableContainer;
import net.theelm.sewingmachine.objects.LootInventory;
import net.theelm.sewingmachine.utilities.BossLootRewards;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public abstract class ShulkerBoxBlockMixin extends BlockWithEntity {
    protected ShulkerBoxBlockMixin(Settings settings) {
        super(settings);
    }
    
    @Inject(at = @At("HEAD"), method = "onUse", cancellable = true)
    public void onInteract(BlockState blockState, World world, BlockPos blockPos, PlayerEntity player, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callback) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof BossLootableContainer lootableContainer) {
                Identifier identifier = lootableContainer.getBossLootIdentifier();
                if (identifier != null) {
                    BossLootRewards rewards = BossLootRewards.get(identifier);
                    if (rewards == null)
                        serverPlayer.sendMessage(Text.literal("Couldn't find any loot for that boss.").formatted(Formatting.RED), MessageType.GAME_INFO, CoreMod.SPAWN_ID);
                    else {
                        LootInventory inventory = rewards.getPlayerLoot(player.getUuid());
                        if (inventory.isEmpty())
                            serverPlayer.sendMessage(Text.literal("You don't have any loot from the ").formatted(Formatting.RED).append(rewards.getEntityName()).append("."), MessageType.GAME_INFO, CoreMod.SPAWN_ID);
                        else {
                            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                                inventory.createContainer(i, playerInventory),
                            rewards.getContainerName()));
                        }
                    }
                    
                    callback.setReturnValue(ActionResult.SUCCESS);
                }
            }
        }
    }
}
