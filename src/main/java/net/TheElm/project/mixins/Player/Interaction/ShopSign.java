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

package net.TheElm.project.mixins.Player.Interaction;

import net.TheElm.project.CoreMod;
import net.TheElm.project.enums.ShopSigns;
import net.TheElm.project.interfaces.ShopSignBlockEntity;
import net.TheElm.project.utilities.ShopSignBuilder;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(SignBlockEntity.class)
public abstract class ShopSign extends BlockEntity implements ShopSignBlockEntity {
    
    @Shadow public Text[] text;
    @Shadow public abstract PlayerEntity getEditor();
    
    /*
     * Mixin variables
     */
    // Shop Owner (Necessary to be a shop sign)
    private UUID shopSign_Owner = null;
    private ShopSigns shopSign_Type = null;
    
    // Item being traded
    private Identifier shopSign_item = null;
    
    // Price / Count of item transactioning
    private Integer shopSign_itemCount = null;
    private Integer shopSign_itemPrice = null;
    
    // Region Sign Information
    private BlockPos shopSign_posA = null;
    private BlockPos shopSign_posB = null;
    
    /*
     * Mixin Getters
     */
    
    @Override @Nullable
    public UUID getShopOwner() {
        return this.shopSign_Owner;
    }
    @Override
    public Text getSignLine(int line) {
        return this.text[line];
    }
    
    @Override @Nullable
    public Item getShopItem() {
        Item tradeItem;
        if (( tradeItem = Registry.ITEM.get( this.shopSign_item ) ) != Items.AIR )
            return tradeItem;
        return null;
    }
    @Override @Nullable
    public Text getShopItemDisplay() {
        return new TranslatableText(Registry.ITEM.get( this.shopSign_item ).getTranslationKey());
    }
    @Override @Nullable
    public Integer getShopItemCount() {
        return this.shopSign_itemCount;
    }
    @Override @Nullable
    public Integer getShopItemPrice() {
        return this.shopSign_itemPrice;
    }
    
    @Override @Nullable
    public BlockPos getFirstPos() {
        return this.shopSign_posA;
    }
    @Override @Nullable
    public BlockPos getSecondPos() {
        return this.shopSign_posB;
    }
    
    @Override @Nullable
    public ShopSigns getShopType() {
        return this.shopSign_Type;
    }
    
    /*
     * Constructor
     */
    public ShopSign(BlockEntityType<?> blockEntityType_1) {
        super(blockEntityType_1);
    }
    
    /*
     * Mixin methods
     */
    
    @Inject(at = @At("RETURN"), method = "setTextOnRow")
    public void onRowUpdated(int lineNum, Text text, final CallbackInfo callback) {
        if (!( this.getEditor() == null )) {
            // Get the sign
            ShopSignBuilder builder = ShopSignBuilder.create( this.getWorld(), this.getPos(), (SignBlockEntity)(BlockEntity)this );
            
            // Update the lines
            if ( builder.setLineText( lineNum, text ) ) {
                // If last line came through, call build
                if (lineNum != 3)
                    return;
                
                // Ignore if this isn't actually a shop sign
                if ( !builder.build((ServerPlayerEntity) this.getEditor()) )
                    return;
                
                this.shopSign_Type = builder.getType();
                assert this.shopSign_Type != null;
                
                CoreMod.logInfo("Built new shop sign " + this.shopSign_Type.name() + " at " + this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ());
                
                // Update the parameters here from the builder
                this.shopSign_Owner = builder.shopOwner();
                this.shopSign_item = builder.getItem();
                
                this.shopSign_itemCount = builder.itemSize();
                this.shopSign_itemPrice = builder.shopPrice();
                
                if (this.shopSign_Type == ShopSigns.DEED) {
                    this.shopSign_posA = builder.regionPosA();
                    this.shopSign_posB = builder.regionPosB();
                }
            }
        }
    }
    
    /*
     * NBT read/write
     */
    
    @Inject(at = @At("RETURN"), method = "toTag", cancellable = true)
    public void nbtWrite(CompoundTag originalTag, CallbackInfoReturnable<CompoundTag> callback) {
        CompoundTag tag = callback.getReturnValue();
        
        if ( this.shopSign_Owner == null )
            return;
        
        tag.putUuid("shop_owner", this.shopSign_Owner );
        if ( this.shopSign_item != null ) {
            tag.putString("shop_item_mod", this.shopSign_item.getNamespace());
            tag.putString("shop_item_name", this.shopSign_item.getPath());
        }
        if (this.shopSign_itemCount != null) tag.putInt("shop_item_count", this.shopSign_itemCount );
        if (this.shopSign_itemPrice != null) tag.putInt("shop_price", this.shopSign_itemPrice );
        
        if ((this.getFirstPos() != null) && (this.getSecondPos() != null)) {
            tag.putLong("shop_blockPosA", this.getFirstPos().asLong());
            tag.putLong("shop_blockPosB", this.getSecondPos().asLong());
        }
        
        callback.setReturnValue( tag );
    }
    
    @Inject(at = @At("RETURN"), method = "fromTag")
    public void nbtRead(BlockState state, CompoundTag tag, CallbackInfo callback) {
        // Shop signs
        if ( tag.containsUuid( "shop_owner" ) && (this.shopSign_Type = ShopSigns.valueOf(this.text[0])) != null) {
            // Get the ITEM for the shop
            if (tag.contains("shop_item_mod", NbtType.STRING) && tag.contains("shop_item_name", NbtType.STRING)) {
                String signItem = tag.getString("shop_item_mod") + ":" + tag.getString("shop_item_name");
                try {
                    this.shopSign_item = new Identifier(signItem);
                } catch (InvalidIdentifierException e) {
                    CoreMod.logError("Could not find item \"" + signItem + "\" for shop sign.", e);
                }
            }
            
            // Get the BLOCK POSITIONS for deed
            if (tag.contains("shop_blockPosA", NbtType.LONG) && tag.contains("shop_blockPosB", NbtType.LONG)) {
                this.shopSign_posA = BlockPos.fromLong(tag.getLong("shop_blockPosA"));
                this.shopSign_posB = BlockPos.fromLong(tag.getLong("shop_blockPosB"));
            }
            
            // Save other relevant shop sign data
            this.shopSign_Owner = tag.getUuid("shop_owner");
            if (tag.contains("shop_item_count", NbtType.NUMBER)) this.shopSign_itemCount = tag.getInt("shop_item_count");
            if (tag.contains("shop_price", NbtType.NUMBER)) this.shopSign_itemPrice = tag.getInt("shop_price");
        }
    }
    
}
