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

package net.TheElm.project.mixins.Items;

import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.ItemUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorStandItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.LeadItem;
import net.minecraft.item.MinecartItem;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.WritableBookItem;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
    FlintAndSteelItem.class,
    ShovelItem.class,
    BlockItem.class,
    EnderEyeItem.class,
    AxeItem.class,
    HoeItem.class,
    ShovelItem.class,
    LeadItem.class,
    MinecartItem.class,
    MusicDiscItem.class,
    WrittenBookItem.class,
    WritableBookItem.class,
    ArmorStandItem.class,
    EndCrystalItem.class
})
public abstract class UsableItems extends Item {
    
    public UsableItems(Settings settings) {
        super(settings);
    }
    
    @Inject(at = @At("HEAD"), method = "useOnBlock", cancellable = true)
    public void onUseOfItem(ItemUsageContext context, CallbackInfoReturnable<ActionResult> callback) {
        PlayerEntity player = context.getPlayer();
        if (player != null) {
            if (!ChunkUtils.canPlayerDoInChunk(ItemUtils.getPermission(context.getStack()), player, context.getBlockPos())) {
                // Update the inventory slot
                EntityUtils.resendInventory(player);
                
                // Set the return value
                callback.setReturnValue(ActionResult.PASS);
            }
        }
    }
    
}
