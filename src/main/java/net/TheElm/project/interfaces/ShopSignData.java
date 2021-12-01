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
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.TheElm.project.utilities.text.StyleApplicator;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface ShopSignData {
    @NotNull StyleApplicator APPLICATOR_GREEN = new StyleApplicator("#32CD32");
    @NotNull StyleApplicator APPLICATOR_RED = new StyleApplicator("#B22222");
    
    @Nullable LootableContainerBlockEntity getContainer();
    @NotNull SignBlockEntity getSign();
    
    @Nullable Text getSignLine(int line);
    void setSignLine(int row, @Nullable Text text);
    
    void setShopOwner(@Nullable UUID uuid);
    @Nullable UUID getShopOwner();
    
    default @NotNull Optional<GameProfile> getShopOwnerProfile() {
        UUID uuid = this.getShopOwner();
        if (uuid == null)
            return null;
        if (Objects.equals(uuid, CoreMod.SPAWN_ID))
            return Optional.of(new GameProfile(uuid, "Server"));
        UserCache cache = ServerCore.get()
            .getUserCache();
        return cache.getByUuid(uuid);
    }
    @Nullable Item getShopItem();
    @Nullable Identifier getShopItemIdentifier();
    default @NotNull String getShopItemTranslationKey() {
        Item item = this.getShopItem();
        return (item == null ? Items.AIR : item).getTranslationKey();
    }
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
    
    boolean setItem(@NotNull ItemStack stack);
    
    @Nullable BlockPos getFirstPos();
    @Nullable BlockPos getSecondPos();
    
    @Nullable Inventory getInventory();
    
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
        Optional<GameProfile> lookup = this.getShopOwnerProfile();
        return lookup.map(profile -> Objects.equals(CoreMod.SPAWN_ID, profile.getId()) ? null : new LiteralText(profile.getName()))
            .orElseGet(() -> new LiteralText(""));
    }
    
    default boolean renderSign() {
        ShopSigns type = this.getShopType();
        return type != null && type.renderSign(this);
    }
    default void removeEditor() {
        SignBlockEntity sign = this.getSign();
        
        UUID editor = sign.getEditor();
        
        // Close the sign editor screen if still editing the sign
        if (editor != null) {
            // TODO: Check if the player is editing the sign and exit it
        }
        
        sign.setEditor(null);
    }
    
    default void setSoundSourcePosition(@Nullable BlockPos pos) {
    }
    default @Nullable BlockPos getSoundSourcePosition() {
        return null;
    }
    default void playSound(@NotNull ServerPlayerEntity player, @NotNull SoundEvent event, @NotNull SoundCategory category) {
        ServerWorld world = player.getWorld();
        BlockPos source;
        if (world == null || (source = this.getSoundSourcePosition()) == null)
            player.playSound(event, category, 1.0f, 1.0f);
        else
            world.playSound(null, source, event, category, 1.0f, 1.0f);
    }
    
    default boolean itemMatchPredicate(@NotNull ItemStack stack) {
        // Items must be equals
        if (!Objects.equals(this.getShopItem(), stack.getItem()))
            return false;
        
        // Don't accept damaged goods
        if (stack.isDamaged())
            return false;
        
        // Make sure the enchantments match
        Map<Enchantment, Integer> enchantments = this.getShopItemEnchantments();
        return enchantments == null || NbtUtils.enchantsEquals(enchantments, EnchantmentHelper.get(stack));
    }
    default ItemStack createItemStack(int count) {
        Item item = this.getShopItem();
        if (item == null)
            item = Items.AIR;
        ItemStack stack = new ItemStack(item, Collections.min(Arrays.asList(count, item.getMaxCount())));
        
        Map<Enchantment, Integer> enchantments = this.getShopItemEnchantments();
        if (enchantments != null && !enchantments.isEmpty())
            EnchantmentHelper.set(enchantments, stack);
        
        return stack;
    }
}
