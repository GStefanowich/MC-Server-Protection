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

package net.TheElm.project.mixins.Player;

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.utilities.Assert;
import net.TheElm.project.utilities.InventoryUtils;
import net.TheElm.project.utilities.NbtUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilCost extends ForgingScreenHandler {
    
    @Shadow @Final
    private Property levelCost;
    
    public AnvilCost(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }
    
    @ModifyConstant(method = "updateResult", constant = @Constant(intValue = 40))
    private int anvilMaxLevelOverride(int oldValue) {
        if (SewConfig.isTrue(SewConfig.ANVIL_DISABLE_COST_LIMIT))
            return Integer.MAX_VALUE;
        return oldValue;
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/inventory/Inventory.setStack(ILnet/minecraft/item/ItemStack;)V"), method = "updateResult")
    public void onUpdateOutput(Inventory inventory, int slot, ItemStack item) {
        if ((!item.isEmpty()) && item.hasEnchantments()) {
            CompoundTag display = item.getOrCreateSubTag("display");;
            
            // Get the rarity of the new output item
            InventoryUtils.ItemRarity rarity = InventoryUtils.getItemRarity(item);
            
            // Generate the lore
            Text lore = new LiteralText("One ")
                .append(new LiteralText(rarity.name()))
                .append(" ").append(new TranslatableText(item.getTranslationKey()));
            Text madeBy = new LiteralText("Forged by ")
                .append(this.player.getEntityName());
            display.put("Lore", NbtUtils.toList(Arrays.asList(lore, madeBy), Text.Serializer::toJson));
        }
        
        inventory.setStack(slot, item);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/enchantment/Enchantment.getMaxLevel()I"), method = "updateResult")
    public int getMaxLevel(Enchantment enchantment) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);
        if (Assert.ifAny(ItemStack::isEmpty, left, right) || InventoryUtils.areBooksAtMaximum(enchantment, left, right))
            return enchantment.getMaxLevel();
        return 10;
    }
}
