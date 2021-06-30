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

package net.TheElm.project.interfaces;

import com.mojang.authlib.GameProfile;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.ShopSigns;
import net.TheElm.project.utilities.text.StyleApplicator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface ShopSignBlockEntity {
    @NotNull StyleApplicator APPLICATOR_GREEN = new StyleApplicator("#32CD32");
    @NotNull StyleApplicator APPLICATOR_RED = new StyleApplicator("#B22222");
    
    @Nullable Text getSignLine(int line);
    void setSignLine(int row, @Nullable Text text);
    
    void setShopOwner(@Nullable UUID uuid);
    @Nullable UUID getShopOwner();
    default @Nullable GameProfile getShopOwnerProfile() {
        return this.getShopOwner() == null ? null : ServerCore.getGameProfile(this.getShopOwner());
    }
    @Nullable Item getShopItem();
    @Nullable Identifier getShopItemIdentifier();
    default @Nullable Map<Enchantment, Integer> getShopItemEnchantments() {
        return new HashMap<>();
    }
    default @Nullable Text getShopItemDisplay() {
        Item item = this.getShopItem();
        if (item == null)
            return new LiteralText("");
        return new TranslatableText(item.getTranslationKey());
    }
    @Nullable Integer getShopItemCount();
    @Nullable Integer getShopItemPrice();
    
    @Nullable BlockPos getFirstPos();
    @Nullable BlockPos getSecondPos();
    
    /*@Nullable Inventory getInventory();*/
    
    @Nullable ShopSigns getShopType();
    
    default MutableText textParseItem() {
        Integer itemSize = this.getShopItemCount();
        Item tradeItem = this.getShopItem();
        Map<Enchantment, Integer> enchantments = this.getShopItemEnchantments();
        if (itemSize == null || tradeItem == null || enchantments == null)
            return null;
        
        MutableText baseText = new LiteralText(itemSize == 1 ? "" : (itemSize + " "));
        TranslatableText translatable = new TranslatableText(tradeItem.getTranslationKey());
        
        if (Items.ENCHANTED_BOOK.equals(tradeItem) && enchantments.size() == 1) {
            Optional<Map.Entry<Enchantment, Integer>> optional = enchantments.entrySet().stream()
                .findAny();
            if (optional.isPresent()) {
                Map.Entry<Enchantment, Integer> entry = optional.get();
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();

                // Set the text
                translatable = new TranslatableText(enchantment.getTranslationKey());

                // Add the level of the enchantment
                if (level != 1 || enchantment.getMaxLevel() != 1) {
                    translatable.append(" ")
                        .append(new TranslatableText("enchantment.level." + level));
                }
            }
        }
        
        return baseText.formatted(Formatting.BLACK)
            .append(translatable.formatted(Formatting.DARK_AQUA));
    }
    default MutableText textParseOwner() {
        GameProfile profile = this.getShopOwnerProfile();
        return profile == null || Objects.equals(CoreMod.SPAWN_ID, profile.getId()) ? new LiteralText("") : new LiteralText(profile.getName());
    }
    
    default boolean renderSign(@NotNull final UUID creator) {
        ShopSigns type = this.getShopType();
        return type != null && type.renderSign(this, creator);
    }
    default boolean renderSign(@NotNull final GameProfile creator) {
        ShopSigns type = this.getShopType();
        return type != null && type.renderSign(this, creator);
    }
    default boolean renderSign(@NotNull final ServerPlayerEntity creator) {
        ShopSigns type = this.getShopType();
        return type != null && type.renderSign(this, creator);
    }
}
