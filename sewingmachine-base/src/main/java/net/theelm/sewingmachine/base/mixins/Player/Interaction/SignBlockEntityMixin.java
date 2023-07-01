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

package net.theelm.sewingmachine.base.mixins.Player.Interaction;

import com.google.common.collect.Maps;
import net.minecraft.block.entity.SignText;
import net.minecraft.registry.Registries;
import net.minecraft.server.filter.FilteredMessage;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.ShopSigns;
import net.theelm.sewingmachine.utilities.nbt.NbtGet;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements ShopSignData {
    /*
     * Mixin variables
     */
    
    @Shadow public abstract SignText getFrontText();
    @Shadow private boolean waxed;

    @Shadow protected abstract boolean setFrontText(SignText frontText);

    @Shadow protected abstract SignText parseLines(SignText signText);

    @Shadow private SignText frontText;
    // Shop Owner (Necessary to be a shop sign)
    private @Nullable UUID shopSign_Owner = null;
    private @Nullable ShopSign shopSign_Type = null;
    
    // Item being traded
    private @Nullable Identifier shopSign_item = null;
    private @Nullable List<Identifier> shopSign_itemRecipes = null;
    private final @NotNull Map<Enchantment, Integer> shopSign_itemEnchants = Maps.newLinkedHashMap();
    
    // Price / Count of item transactioning
    private @Nullable Integer shopSign_itemCount = null;
    private @Nullable Integer shopSign_itemPrice = null;
    
    // Region Sign Information
    private @Nullable BlockPos shopSign_posA = null;
    private @Nullable BlockPos shopSign_posB = null;
    
    // Where to play sounds from
    private @Nullable BlockPos shopSign_soundSourcePlayFromPos = null;
    
    /*
     * Mixin Getters
     */
    
    @Override
    public @Nullable LootableContainerBlockEntity getContainer() {
        return InventoryUtils.getAttachedChest(this.world, this.pos);
    }
    @Override
    public @NotNull SignBlockEntity getSign() {
        return (SignBlockEntity)(BlockEntity) this;
    }
    @Override
    public @Nullable Inventory getInventory() {
        LootableContainerBlockEntity container = this.getContainer();
        return container == null ? null : InventoryUtils.getInventoryOf(container);
    }
    
    @Override
    public void setShopOwner(@Nullable UUID uuid) {
        this.shopSign_Owner = uuid;
    }
    @Override
    public @Nullable UUID getShopOwner() {
        return this.shopSign_Owner;
    }
    
    @Override
    public Text getSignLine(int line) {
        return this.getFrontText()
            .getMessage(line, false);
    }
    @Override
    public boolean setSign(@NotNull SignText text) {
        // Set the text directly on the property (When loading)
        this.frontText = this.parseLines(text);
        return true;
    }
    
    @Override
    public boolean setItem(@NotNull ItemStack stack) {
        this.shopSign_item = Registries.ITEM.getId(stack.getItem());
        this.shopSign_itemRecipes = null;
        this.shopSign_itemEnchants.clear();
        this.shopSign_itemEnchants.putAll(EnchantmentHelper.get(stack));
        return !Items.AIR.equals(stack.getItem());
    }
    @Override
    public @Nullable Item getShopItem() {
        Item tradeItem;
        if ((tradeItem = Registries.ITEM.get(this.shopSign_item)) != Items.AIR )
            return tradeItem;
        return null;
    }
    @Override
    public @Nullable Identifier getShopItemIdentifier() {
        return this.shopSign_item;
    }
    
    @Override
    public @Nullable List<? extends Recipe<?>> getShopItemRecipes() {
        if (this.shopSign_itemRecipes == null)
            this.shopSign_itemRecipes = this.findValidShopItemRecipes();
        if (this.shopSign_itemRecipes.isEmpty())
            return null;
        RecipeManager recipeManager = this.getRecipeManager();
        if (recipeManager == null)
            return null;
        return this.shopSign_itemRecipes.stream()
            .map(recipeManager::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
    private @NotNull List<Identifier> findValidShopItemRecipes() {
        RecipeManager recipeManager = this.getRecipeManager();
        if (recipeManager == null || this.shopSign_item == null)
            return Collections.emptyList();
        return recipeManager.listAllOfType(RecipeType.CRAFTING)
            .stream()
            .filter(recipe -> this.shopSign_item.equals(Registries.ITEM.getId(recipe.getOutput(null).getItem())))
            .filter(recipe -> recipe.getIngredients().size() == 1)
            .map(CraftingRecipe::getId)
            .toList();
    }
    private @Nullable RecipeManager getRecipeManager() {
        if (this.world == null)
            return null;
        MinecraftServer server = this.world.getServer();
        return server == null ? null : server.getRecipeManager();
    }
    
    @Override
    public @Nullable Integer getShopItemCount() {
        return this.shopSign_itemCount;
    }
    private void setShopItemCount(@Nullable Integer count) {
        this.shopSign_itemCount = count;
    }
    
    @Override
    public @Nullable Integer getShopItemPrice() {
        return this.shopSign_itemPrice;
    }
    private void setShopItemPrice(@Nullable Integer price) {
        this.shopSign_itemPrice = price;
    }
    
    @Override
    public @Nullable Map<Enchantment, Integer> getShopItemEnchantments() {
        return this.shopSign_itemEnchants;
    }
    
    @Override
    public @Nullable BlockPos getFirstPos() {
        return this.shopSign_posA;
    }
    @Override
    public @Nullable BlockPos getSecondPos() {
        return this.shopSign_posB;
    }
    
    @Override
    public @Nullable ShopSign getShopType() {
        return this.shopSign_Type;
    }
    
    @Override
    public @Nullable BlockPos getSoundSourcePosition() {
        return this.shopSign_soundSourcePlayFromPos;
    }
    @Override
    public void setSoundSourcePosition(@Nullable BlockPos pos) {
        this.shopSign_soundSourcePlayFromPos = pos;
    }
    
    /*
     * Constructor
     */
    
    public SignBlockEntityMixin(BlockEntityType<?> blockEntityType_1, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType_1, blockPos, blockState);
    }
    
    /*
     * Mixin methods
     */
    
    @Inject(at = @At("RETURN"), method = "tryChangeText")
    public void onTryChangeText(PlayerEntity player, boolean front, List<FilteredMessage> messages, CallbackInfo ci) {
        if (front && player instanceof ServerPlayerEntity serverPlayer) {
            // Get the sign
            ShopSignBuilder builder = ShopSignBuilder.create(this.world, this.getPos(), (SignBlockEntity)(BlockEntity)this);
            
            for (int i = 0; i < messages.size(); i++) {
                String line = messages.get(i)
                    .getString();
                builder.setLineText(i, Text.literal(line));
            }
            
            // Ignore if this isn't actually a shop sign;
            if (!builder.build(serverPlayer))
                return;
            
            this.shopSign_Type = builder.getShopType();
            assert this.shopSign_Type != null;
            
            CoreMod.logInfo("Built new shop sign " + this.shopSign_Type.name + " at " + this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ());
            
            // Update the parameters here from the builder
            this.shopSign_Owner = builder.getShopOwner();
            this.shopSign_item = builder.getShopItemIdentifier();
            this.shopSign_itemRecipes = null;
            
            // Copy the enchantments to the sign from the builder
            this.shopSign_itemEnchants.clear();
            this.shopSign_itemEnchants.putAll(builder.getShopItemEnchantments());
            
            // Get the item information
            this.shopSign_itemCount = builder.getShopItemCount();
            this.shopSign_itemPrice = builder.getShopItemPrice();
            
            // Copy the block ranges
            this.shopSign_posA = builder.getFirstPos();
            this.shopSign_posB = builder.getSecondPos();
        }
    }
    
    /*
     * NBT read/write
     */
    
    @Inject(at = @At("RETURN"), method = "writeNbt")
    public void onNbtWrite(@NotNull NbtCompound tag, CallbackInfo callback) {
        if ( this.shopSign_Owner == null )
            return;
        
        // Add shop owner
        tag.putUuid("shop_owner", this.getShopOwner());
        
        // Add item information
        if ( this.shopSign_item != null )
            tag.putString("shop_item", this.getShopItemIdentifier().toString());
        if (this.shopSign_itemCount != null)
            tag.putInt("shop_count", this.getShopItemCount());
        if (this.shopSign_itemPrice != null)
            tag.putInt("shop_price", this.getShopItemPrice());
        
        // Add enchantments
        if (!this.shopSign_itemEnchants.isEmpty())
            tag.put("shop_item_enchants", NbtUtils.enchantsToTag(this.shopSign_itemEnchants));
        
        // Add block ranges
        if ((this.getFirstPos() != null) && (this.getSecondPos() != null)) {
            tag.putLong("shop_blockPosA", this.getFirstPos().asLong());
            tag.putLong("shop_blockPosB", this.getSecondPos().asLong());
        }
        
        // Put where to play the sound from
        if (this.shopSign_soundSourcePlayFromPos != null)
            tag.putLong("shop_sound_location", this.shopSign_soundSourcePlayFromPos.asLong());
    }
    
    @Inject(at = @At("RETURN"), method = "readNbt")
    public void onNbtRead(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        Text type = this.getFrontText()
            .getMessage(0, false);
        
        // Shop signs
        if ((this.shopSign_Type = ShopSigns.getFromText(type)) != null) {
            NbtUtils.tryGet(tag, NbtGet.UUID, "shop_owner", (uuid) -> {
                this.waxed = true;
                String signItem = "";
                
                try {
                    // Get the ITEM for the shop
                    if (tag.contains("shop_item_mod", NbtElement.STRING_TYPE) && tag.contains("shop_item_name", NbtElement.STRING_TYPE)) {
                        this.shopSign_item = new Identifier(signItem = (tag.getString("shop_item_mod") + ":" + tag.getString("shop_item_name")));
                    } else if (tag.contains("shop_item", NbtElement.STRING_TYPE))
                        this.shopSign_item = new Identifier(signItem = tag.getString("shop_item"));
                    this.shopSign_itemRecipes = null;
                } catch (InvalidIdentifierException e) {
                    CoreMod.logError("Invalid item identifier \"" + signItem + "\" for shop sign.", e);
                }
                
                // Get the BLOCK POSITIONS for deed
                if (tag.contains("shop_blockPosA", NbtElement.LONG_TYPE) && tag.contains("shop_blockPosB", NbtElement.LONG_TYPE)) {
                    this.shopSign_posA = BlockPos.fromLong(tag.getLong("shop_blockPosA"));
                    this.shopSign_posB = BlockPos.fromLong(tag.getLong("shop_blockPosB"));
                }
                
                // Get the BLOCK POSITION to play sounds from
                if (tag.contains("shop_sound_location", NbtElement.LONG_TYPE))
                    this.shopSign_soundSourcePlayFromPos = BlockPos.fromLong(tag.getLong("shop_sound_location"));
                
                // Load the enchantments from the tag
                if (tag.contains("shop_item_enchants", NbtElement.LIST_TYPE)) {
                    this.shopSign_itemEnchants.clear();
                    this.shopSign_itemEnchants.putAll(NbtUtils.enchantsFromTag(tag.getList("shop_item_enchants", NbtElement.COMPOUND_TYPE)));
                }
                
                // Save the shop owner UUID
                this.shopSign_Owner = uuid;
                
                // Get the Item COUNT
                NbtUtils.tryGet(tag, NbtGet.INT, "shop_item_count", this::setShopItemCount)
                    .orElse("shop_count");
                
                // Get the Item PRICE
                NbtUtils.tryGet(tag, NbtGet.INT, "shop_price", this::setShopItemPrice);
                
                // Re-Render the sign when loaded
                this.renderSign();
            });
        }
    }
    
}
