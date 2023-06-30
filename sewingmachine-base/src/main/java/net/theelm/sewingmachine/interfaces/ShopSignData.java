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

package net.theelm.sewingmachine.interfaces;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.util.DyeColor;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.objects.ShopSign;import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface ShopSignData {
    @NotNull StyleApplicator APPLICATOR_GREEN = new StyleApplicator("#32CD32");
    @NotNull StyleApplicator APPLICATOR_RED = new StyleApplicator("#B22222");
    
    /**
     * Get the attached container Entity for the Sign
     * @return The attached container, or NULL if none found
     */
    @Nullable LootableContainerBlockEntity getContainer();
    
    /**
     * Get the Signs Entity
     * @return The Signs Entity
     */
    @NotNull SignBlockEntity getSign();
    
    /**
     * Find the attached inventory for the Sign (The inventory of the container)
     * @return The attached inventory, or NULL if none found
     */
    @Nullable Inventory getInventory();
    
    /**
     * Get the text present on a specified line of the sign
     * @return
     */
    @Nullable Text getSignLine(int line);
    
    /**
     * Set the signs front-side text
     * @return
     */
    boolean setSign(@NotNull SignText text);
    
    /**
     * Set the signs front-side text with the applied first-line preformatting
     * @return
     */
    default boolean setSign(@NotNull Text[] text) {
        Text[] render = new Text[4];
        ShopSign shop = this.getShopType();
        
        render[0] = MutableText.of(new LiteralTextContent("[" + shop.name + "]" )).styled(shop.getApplicator());
        
        for (int i = 0; i < 3; i++) {
            render[i + 1] = i >= text.length ? Text.literal("") : text[i];
        }
        
        return this.setSign(new SignText(render, render, DyeColor.BLACK, false));
    }
    
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
            return Text.literal("");
        return Text.translatable(item.getTranslationKey());
    }
    
    @Nullable List<? extends Recipe<?>> getShopItemRecipes();
    
    @Nullable Integer getShopItemCount();
    @Nullable Integer getShopItemPrice();
    
    boolean setItem(@NotNull ItemStack stack);
    
    @Nullable BlockPos getFirstPos();
    @Nullable BlockPos getSecondPos();
    
    @Nullable ShopSign getShopType();
    
    /**
     * Check if item-transfer can be ignored
     * @return If infinite
     */
    default boolean isInfinite() {
        return Objects.equals(CoreMod.SPAWN_ID, this.getShopOwner());
    }
    
    /**
     * Read the Item from the Sign and return the Formatting of the item
     * @return The formatted item name
     */
    default MutableText textParseItem() {
        Integer itemSize = this.getShopItemCount();
        Item tradeItem = this.getShopItem();
        Map<Enchantment, Integer> enchantments = this.getShopItemEnchantments();
        if (itemSize == null || tradeItem == null || enchantments == null)
            return null;
        
        MutableText baseText = Text.translatable(itemSize == 1 ? "" : (itemSize + " "));
        MutableText translatable = Text.translatable(tradeItem.getTranslationKey());
        
        if (Items.ENCHANTED_BOOK.equals(tradeItem) && enchantments.size() == 1) {
            Optional<MutableText> optional = enchantments.entrySet()
                .stream()
                .findAny()
                .map(MessageUtils::enchantmentToText);
            if (optional.isPresent())
                translatable = optional.get();
        }
        
        return baseText.formatted(Formatting.BLACK)
            .append(translatable.formatted(Formatting.DARK_AQUA));
    }
    
    /**
     * Read the shop owner from the Sign and return the Formatted name
     * @return The formatted owners name
     */
    default MutableText textParseOwner() {
        Optional<GameProfile> lookup = this.getShopOwnerProfile();
        return lookup.map(profile -> Objects.equals(CoreMod.SPAWN_ID, profile.getId()) ? null : Text.literal(profile.getName()))
            .orElseGet(() -> Text.literal(""));
    }
    
    /**
     * Rerun the sign formatter. Updates the owners name (If changed) and any formatting
     * @return If the sign was successfully rendered
     */
    default boolean renderSign() {
        ShopSign type = this.getShopType();
        return type != null && type.renderSign(this);
    }
    
    /**
     * Set the position that any sounds will come from
     * @param pos The sound source
     */
    default void setSoundSourcePosition(@Nullable BlockPos pos) {
    }
    
    /**
     * Get the position that sounds should be played from
     * @return The sound source
     */
    default @Nullable BlockPos getSoundSourcePosition() {
        return null;
    }
    
    /**
     * Play a sound for interacting with the sign
     * @param player The player to play the sound for (Will play for everyone nearby if the sign has set a SoundSourcePosition)
     * @param event The sound to play
     * @param category The category to use for the sounds volume
     */
    default void playSound(@NotNull ServerPlayerEntity player, @NotNull SoundEvent event, @NotNull SoundCategory category) {
        ServerWorld world = player.getServerWorld();
        BlockPos source;
        if (world == null || (source = this.getSoundSourcePosition()) == null)
            player.playSound(event, category, 1.0f, 1.0f);
        else
            world.playSound(null, source, event, category, 1.0f, 1.0f);
    }
    
    /**
     * Test if an ItemStack matches the Item exactly for this sign
     * @param stack The ItemStack to check
     * @return If the ItemStack matches the Item and Enchantments of this sign
     */
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
    
    /**
     * Item Spawner if this Sign is Infinite
     * @param count The stack count to create
     * @return An ItemStack
     */
    default @NotNull ItemStack createItemStack(int count) {
        Item item = this.getShopItem();
        if (item == null)
            item = Items.AIR;
        ItemStack stack = new ItemStack(item, Math.min(count, item.getMaxCount()));
        
        // Apply enchantments to the stack
        Map<Enchantment, Integer> enchantments = this.getShopItemEnchantments();
        if (enchantments != null && !enchantments.isEmpty())
            EnchantmentHelper.set(enchantments, stack);
        
        return stack;
    }
}
