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

package net.theelm.sewingmachine.base.mixins.Player;


import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.theelm.sewingmachine.events.BlockInteractionCallback;
import net.theelm.sewingmachine.interfaces.ItemUseCallback;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = net.minecraft.server.network.ServerPlayerInteractionManager.class, priority = 10000)
public abstract class ServerPlayerInteractionManager {
    /**
     * Item interaction callback (Used to test attempts to consume items)
     * @param player
     * @param world
     * @param itemStack
     * @param hand
     * @param callback
     */
    @Inject(at = @At("HEAD"), method = "interactItem", cancellable = true)
    private void beforeItemInteract(@NotNull final ServerPlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        if (!player.getWorld().isClient) {
            ActionResult result = ItemUseCallback.EVENT.invoker().use(player, world, hand, itemStack);
            if (result != ActionResult.PASS)
                callback.setReturnValue(result);
        }
    }
    
    /**
     * Block interaction callback (Used for accessing BlockEntities like containers, but also shopsigns)
     * @param player
     * @param world
     * @param itemStack
     * @param hand
     * @param blockHitResult
     * @param callback
     */
    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    private void beforeBlockInteract(@NotNull final ServerPlayerEntity player, final World world, final ItemStack itemStack, final Hand hand, final BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callback) {
        if (!player.getWorld().isClient) {
            ActionResult result = BlockInteractionCallback.EVENT.invoker().interact(player, world, hand, itemStack, blockHitResult);
            if (result != ActionResult.PASS)
                callback.setReturnValue(result);
        }
    }
}
