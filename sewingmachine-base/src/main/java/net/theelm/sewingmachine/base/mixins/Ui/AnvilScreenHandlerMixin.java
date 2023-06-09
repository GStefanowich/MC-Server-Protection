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

package net.theelm.sewingmachine.base.mixins.Ui;

import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.utilities.Assert;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.ItemUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
    
    @Shadow @Final
    private Property levelCost;
    
    public AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }
    
    @ModifyConstant(method = "updateResult", constant = @Constant(intValue = 40))
    private int anvilMaxLevelOverride(int oldValue) {
        if (SewConfig.isTrue(SewCoreConfig.ANVIL_DISABLE_COST_LIMIT))
            return Integer.MAX_VALUE;
        return oldValue;
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/inventory/CraftingResultInventory.setStack(ILnet/minecraft/item/ItemStack;)V"), method = "updateResult")
    public void onUpdateOutput(@NotNull CraftingResultInventory inventory, int slot, @NotNull ItemStack item) {
        // If the output is a stack that has enchantments
        if ((!item.isEmpty()) && item.hasEnchantments())
            ItemUtils.setStackAuthor(item, this.player);
        
        // Still do the vanilla
        inventory.setStack(slot, item);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/enchantment/Enchantment.getMaxLevel()I"), method = "updateResult")
    public int getMaxLevel(Enchantment enchantment) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);
        if (Assert.ifAny(ItemStack::isEmpty, left, right) || InventoryUtils.areItemsAboveMaxLevel(enchantment, left, right))
            return InventoryUtils.getMaximumCombinedLevel(enchantment, left, right);
        return enchantment.getMaxLevel();
    }
}
