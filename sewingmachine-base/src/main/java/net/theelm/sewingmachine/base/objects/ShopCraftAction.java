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

package net.theelm.sewingmachine.base.objects;

import net.minecraft.registry.Registries;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created on Apr 25 2022 at 6:35 PM.
 * By greg in SewingMachineMod
 */
public final class ShopCraftAction {
    private final @NotNull Identifier input;
    
    private final int inputCount;
    private final int remainder;
    
    private final @NotNull ShopSignData shop;
    private final @NotNull BlockPos shopPos;
    private final @NotNull Inventory container;
    
    public ShopCraftAction(@NotNull Recipe<?> recipe, @NotNull ShopSignData shop, @NotNull BlockPos signPos, @NotNull Inventory inventory) {
        this.shop = shop;
        this.shopPos = signPos;
        this.container = inventory;
        
        // Get the ITEM that is used for the craft
        Ingredient ingredient = recipe.getIngredients()
            .get(0);
        ItemStack stack = ingredient.getMatchingStacks()[0];
        this.input = Registries.ITEM.getId(stack.getItem());
        
        int needs = 0;
        int outputs = 0;
        
        while (outputs < shop.getShopItemCount()) {
            needs += stack.getCount();
            outputs += recipe.getOutput(null)
                .getCount();
        }
        
        this.inputCount = needs;
        this.remainder = outputs - shop.getShopItemCount();
    }
    
    private boolean itemMatchesInput(ItemStack stack) {
        return stack != null && Objects.equals(this.input, Registries.ITEM.getId(stack.getItem()));
    }
    
    public boolean craft(@NotNull ServerPlayerEntity player) {
        if (!InventoryUtils.playerToChest(player, this.shopPos, player.getInventory(), this.container, this::itemMatchesInput, this.inputCount, true))
            return false;
        
        if (this.remainder > 0) {
            player.getInventory()
                .offerOrDrop(new ItemStack(this.shop.getShopItem(), this.remainder));
        }
        
        return true;
    }
}
