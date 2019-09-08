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
import net.TheElm.project.interfaces.ShopSignBlockEntity;
import net.TheElm.project.utilities.ShopSignBuilder;
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
    private UUID shopSign_Owner = null;
    private Identifier shopSign_item = null;
    private Integer shopSign_itemCount = null;
    private Integer shopSign_itemPrice = null;
    
    /*
     * Mixin Getters
     */
    
    @Override @Nullable
    public UUID getShopOwner() {
        return this.shopSign_Owner;
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
    @Override
    public Text getSignLine(int line) {
        return this.text[line];
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
                
                // Update the parameters here from the builder
                this.shopSign_Owner = builder.shopOwner();
                this.shopSign_item = builder.getItem();
                this.shopSign_itemCount = builder.itemSize();
                this.shopSign_itemPrice = builder.shopPrice();
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
        
        tag.putUuid( "shop_owner", this.shopSign_Owner );
        if ( this.shopSign_item != null ) {
            tag.putString("shop_item_mod", this.shopSign_item.getNamespace());
            tag.putString("shop_item_name", this.shopSign_item.getPath());
        }
        tag.putInt( "shop_item_count", this.shopSign_itemCount );
        tag.putInt( "shop_price", this.shopSign_itemPrice );
        
        callback.setReturnValue( tag );
    }
    
    @Inject(at = @At("RETURN"), method = "fromTag")
    public void nbtRead(CompoundTag tag, CallbackInfo callback) {
        if ( tag.hasUuid( "shop_owner" ) ) {
            String signItem = tag.getString("shop_item_mod") + ":" + tag.getString("shop_item_name");
            try {
                this.shopSign_item = new Identifier( signItem );
                this.shopSign_Owner = tag.getUuid("shop_owner");
                this.shopSign_itemCount = tag.getInt("shop_item_count");
                this.shopSign_itemPrice = tag.getInt("shop_price");
            } catch (InvalidIdentifierException e) {
                CoreMod.logError( "Could not find item \"" + signItem + "\" for shop sign." );
                CoreMod.logError( e );
            }
        }
    }
    
}
