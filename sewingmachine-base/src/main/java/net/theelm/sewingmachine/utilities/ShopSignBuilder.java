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

package net.theelm.sewingmachine.utilities;

import com.google.common.collect.Maps;
import net.minecraft.block.entity.SignText;
import net.minecraft.registry.Registries;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.enums.ShopSigns;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopSignBuilder implements ShopSignData {
    
    /*
     * Builder information
     */
    private final @NotNull SignBlockEntity sign;
    private final @NotNull World world;
    private final @NotNull BlockPos blockPos;
    private Text[] lines = new Text[4];
    
    /* 
     * Shop information
     */
    private @Nullable UUID ownerUUID = null;
    private @Nullable ShopSigns signType = null;
    
    private @Nullable Identifier tradeItemIdentifier = null;
    private final @NotNull Map<Enchantment, Integer> tradeItemEnchants = Maps.newLinkedHashMap();
    private @Nullable Item tradeItem = null;
    private int tradePrice = 0;
    private int stackSize = 0;
    
    private BlockPos regionPosA = null;
    private BlockPos regionPosB = null;
    
    private ShopSignBuilder(@NotNull World world, @NotNull BlockPos blockPos, @NotNull SignBlockEntity sign) {
        this.world = world;
        this.blockPos = blockPos;
        this.sign = sign;
    }
    
    /*
     * Get information to save to our sign
     */
    public Text[] getLines() {
        return this.lines;
    }
    
    @Override
    public @Nullable Text getSignLine(int line) {
        return this.lines[line];
    }
    @Override
    public boolean setSign(@NotNull SignText text) {
        return this.getSign()
            .setText(text, true);
    }
    
    @Override
    public @Nullable UUID getShopOwner() {
        return this.ownerUUID;
    }
    @Override
    public void setShopOwner(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
    }
    
    @Override
    public @NotNull Item getShopItem() {
        return Registries.ITEM.get(this.getShopItemIdentifier());
    }
    @Override
    public boolean setItem(@NotNull ItemStack stack) {
        this.tradeItemIdentifier = Registries.ITEM.getId(stack.getItem());
        this.tradeItemEnchants.putAll(EnchantmentHelper.get(stack));
        // Return if the item is AIR
        return !Items.AIR.equals(this.tradeItem = stack.getItem());
    }
    
    @Override
    public @Nullable ShopSigns getShopType() {
        return this.signType;
    }
    @Override
    public @Nullable Identifier getShopItemIdentifier() {
        return this.tradeItemIdentifier;
    }
    @Override
    public @NotNull Map<Enchantment, Integer> getShopItemEnchantments() {
        return this.tradeItemEnchants;
    }
    @Override
    public @NotNull Integer getShopItemPrice() {
        return this.tradePrice;
    }
    @Override
    public @NotNull Integer getShopItemCount() {
        return this.stackSize;
    }
    @Override
    public @Nullable List<? extends Recipe<?>> getShopItemRecipes() {
        return null;
    }
    
    @Override
    public @Nullable LootableContainerBlockEntity getContainer() {
        return InventoryUtils.getAttachedChest(this.world, this.blockPos);
    }
    @Override
    public @NotNull SignBlockEntity getSign() {
        return this.sign;
    }
    @Override
    public @Nullable Inventory getInventory() {
        LootableContainerBlockEntity container = this.getContainer();
        return container == null ? null : InventoryUtils.getInventoryOf(this.world, container.getPos());
    }
    
    @Override
    public @Nullable BlockPos getFirstPos() {
        return this.regionPosA;
    }
    @Override
    public @Nullable BlockPos getSecondPos() {
        return this.regionPosB;
    }
    
    public void regionPositioning(@Nullable BlockPos first, @Nullable BlockPos second) {
        this.regionPosA = first;
        this.regionPosB = second;
    }
    
    @Nullable
    public ShopSigns getType() {
        return this.signType;
    }
    
    /*
     * Builder
     */
    public boolean setLineText( int line, Text text ) {
        // If the line was already previously updated, skip
        if ( this.lines[line] != null )
            return false;
        
        this.lines[line] = text;
        
        return true;
    }
    public boolean build(@NotNull final ServerPlayerEntity player) {
        /* Signs:
         * [BUY]
         * [SELL]
         * [FREE]
         * [HEAL]
         */
        try {
            this.signType = ShopSigns.valueOf(this.lines[0]);
            if ((this.signType == null) || (!this.signType.isEnabled()))
                return false;
            this.formatOrBreak(player);
            return true;
        } finally {
            // Remove from the map
            buildingSigns.remove(createIdentifier(this.world, this.blockPos));
        }
    }
    private void formatOrBreak(@NotNull final ServerPlayerEntity creator) {
        try {
            if (this.signType.formatSign(this, creator))
                return;
        } catch (ShopBuilderException e) {
            creator.sendMessage(e.getErrorMessage());
        }
        this.breakSign();
    }
    
    /*
     * Build individual sign types
     */
    public boolean textMatchItem(@NotNull ServerPlayerEntity player, @NotNull Text text) {
        String str = text.getString();
        Pattern p = Pattern.compile( "^(\\d+) (.*)$" );
        
        // Check matches
        Matcher m = p.matcher( str );
        if ( !m.find() )
            return false;
        
        this.stackSize = Integer.parseUnsignedInt(m.group(1));
        String itemName = m.group(2).replace(" ", "_")
            .toLowerCase();
        
        if ( "hand".equals(itemName)) {
            // Get the item being held by the player
            return this.setItem(player.getOffHandStack());
        } else if ( "inventory".equals(itemName) ) {
            // Get the item from the attached chest
            Inventory inventory = this.getInventory();
            
            // If an item was found in the container that isn't AIR
            return inventory != null && this.setItem(InventoryUtils.getFirstStack(inventory));
        } else {
            // Try to get the trade identifier
            try {
                // Get the identifier from the sign
                if (!itemName.contains(":"))
                    this.tradeItemIdentifier = new Identifier("minecraft:" + itemName);
                else this.tradeItemIdentifier = new Identifier(itemName);
            } catch (InvalidIdentifierException e) {
                return false;
            }
            
            // If the item identifier is not AIR
            return !Items.AIR.equals(this.tradeItem = this.getShopItem());
        }
    }
    public boolean textMatchCount(@NotNull Text text) {
        try {
            this.stackSize = Integer.parseUnsignedInt(text.getString());
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
    public boolean textMatchPrice(@NotNull Text text) {
        String str = text.getString();
        if ( str.startsWith( "$" ) )
            str = str.substring( 1 );
        
        try {
            this.tradePrice = Integer.parseUnsignedInt(str);
            return true;
        } catch ( NumberFormatException e ) {
            return false;
        }
    }
    public void setTradePrice(int tradePrice) {
        this.tradePrice = tradePrice;
    }
    public void textParseOwner(@NotNull Text text, @NotNull ServerPlayerEntity player) {
        String str = text.getString();
        if ( "server".equalsIgnoreCase(str) && player.isCreative() ) {
            // Set the owner
            this.ownerUUID = CoreMod.SPAWN_ID;
            //return Text.literal("");
        } else {
            // Set the owner
            this.ownerUUID = player.getUuid();
            //return ((MutableText)player.getName()).formatted(Formatting.DARK_GRAY);
        }
    }
    public MutableText textParseItem() {
        MutableText baseText = Text.literal(this.getShopItemCount() == 1 ? "" : (this.getShopItemCount() + " "));
        MutableText translatable = Text.translatable(this.getShopItem().getTranslationKey());
        
        if (Items.ENCHANTED_BOOK.equals(this.tradeItem) && this.tradeItemEnchants.size() == 1) {
            Optional<MutableText> optional = this.tradeItemEnchants.entrySet()
                .stream()
                .findFirst()
                .map(MessageUtils::enchantmentToText);
            if (optional.isPresent())
                translatable = optional.get();
        }
        
        return baseText.formatted(Formatting.BLACK)
            .append(translatable.formatted(Formatting.DARK_AQUA));
    }
    
    private void breakSign() {
        this.world.breakBlock(this.blockPos, true);
    }
    
    /*
     * Static methods
     */
    private static final Map<String, ShopSignBuilder> buildingSigns = Collections.synchronizedMap(new HashMap<>());
    public static ShopSignBuilder create(@NotNull final World world, @NotNull final BlockPos blockPos, @NotNull final SignBlockEntity sign) {
        String worldLocation = ShopSignBuilder.createIdentifier(world, blockPos);
        if (buildingSigns.containsKey(worldLocation))
            return buildingSigns.get(worldLocation);
        
        ShopSignBuilder builder = new ShopSignBuilder(world, blockPos, sign);
        buildingSigns.put( worldLocation, builder );
        
        return builder;
    }
    private static @NotNull String createIdentifier(@NotNull final World world, @NotNull final BlockPos blockPos) {
        return IdUtils.get(world) + ":" + MessageUtils.xyzToString(blockPos);
    }
}
