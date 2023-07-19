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

package net.theelm.sewingmachine.base.objects.signs;

import com.mojang.datafixers.util.Either;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.ServerText;
import org.jetbrains.annotations.NotNull;

/*
 * Chest is FREE
 */
public final class SignShopFree extends ShopSign.BuyTradeSell {
    public SignShopFree() {
        super("FREE", ShopSignData.APPLICATOR_GREEN);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
        return this.formatFreeSign(signBuilder, creator);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        Text[] text = new Text[]{
            // Second row Count - Item Name
            shop.textParseItem(),
            
            // Third Row - Price
            Text.literal("for ").formatted(Formatting.BLACK)
                .append(Text.literal("Free").styled(ShopSignData.APPLICATOR_GREEN))
                .append("!"),
            // Fourth Row - Owner
            shop.textParseOwner().formatted(Formatting.DARK_GRAY)
        };
        
        return shop.setSign(text);
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
        LootableContainerBlockEntity chest = null;
        Inventory chestInventory = null;
        
        // If shops disabled
        if ( !SewConfig.get(SewBaseConfig.DO_MONEY) )
            return Either.right(Boolean.TRUE);
        
        // Check if the attached chest exists
        if (sign.isInfinite() || ((chest = InventoryUtils.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
            if (player.getUuid().equals(sign.getShopOwner()))
                return Either.left(Text.literal("Cannot buy items from yourself."));
            
            // These should NOT be null
            if ((sign.getShopItem() == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemDisplay() == null))
                return Either.left(ServerText.translatable(player, "shop.error.database"));
            
            /*
             * Check if chest is valid
             */
            if ( chest != null ) {
                chestInventory = InventoryUtils.getInventoryOf(player.getEntityWorld(), chest.getPos());
                
                // If the chest is open
                if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                    return Either.left(Text.literal("Cannot do that while chest is open."));
                
                // If there is not enough of item in chest
                if (chestInventory == null || InventoryUtils.getInventoryCount(chestInventory, sign::itemMatchPredicate) < sign.getShopItemCount())
                    return Either.left(Text.literal("Chest is out of " + sign.getShopItemDisplay() + "."));
            }
            
            // Give item to player from chest
            if (!InventoryUtils.chestToPlayer(player, signPos, chestInventory, player.getInventory(), sign::itemMatchPredicate, sign.getShopItemCount(), true ))
                return Either.left(Text.literal("Chest is out of " + sign.getShopItemDisplay() + "."));
            
            Text name = PlayerNameCallback.getName(server, sign.getShopOwner());
            
            // Log the event
            CoreMod.logInfo(player.getName().getString() + " got " + FormattingUtils.format(sign.getShopItemCount()) + " " + sign.getShopItemDisplay() + " from " + name.getString());
            
            return Either.right(Boolean.TRUE);
        }
        return Either.right(Boolean.FALSE);
    }
}
