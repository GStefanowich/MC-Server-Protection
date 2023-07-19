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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.events.PlayerBalanceCallback;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.base.objects.ShopStats;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;

/*
 * Chest is SELLING
 */
public final class SignShopBuy extends ShopSign.BuyTradeSell {
    public SignShopBuy() {
        super("BUY", ShopSignData.APPLICATOR_GREEN);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
        if ((!signBuilder.textMatchItem(creator, signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
            throw new ShopBuilderException("Sign not formatted correctly.");
        
        // Validate that a container is attached
        this.validateAttachedContainer(signBuilder, creator);
        
        // Update the sign to FREE (Because cost is 0)
        if ( signBuilder.getShopItemPrice() == 0 )
            return this.formatFreeSign(signBuilder, creator);
        
        // Parse the owner of the sign
        signBuilder.textParseOwner(signBuilder.getLines()[3], creator);
        
        this.printCompletedSign(creator, signBuilder);
        return this.renderSign(signBuilder);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        Text[] text = new Text[] {
            // Second row Count - Item Name
            shop.textParseItem(),
            
            // Third Row - Price
            Text.literal("for ").formatted(Formatting.BLACK)
                .append(Text.literal("$" + shop.getShopItemPrice()).styled(ShopSignData.APPLICATOR_RED)),
            
            // Fourth Row - Owner
            shop.textParseOwner()
                .formatted(Formatting.DARK_GRAY)
        };
        
        return shop.setSign(text);
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
        LootableContainerBlockEntity chest = null;
        Inventory chestInventory = null;
        
        // If shops disabled
        if ( !this.isEnabled() )
            return Either.right(Boolean.TRUE);
        
        // These should NOT be null
        if (((sign.getShopItem()) == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null) || (sign.getShopItemDisplay() == null))
            return Either.left(ServerText.translatable(player, "shop.error.database"));
        
        // Refuse to trade with own player
        if (player.getUuid().equals(sign.getShopOwner()))
            return Either.left(ServerText.translatable(player, "shop.error.self_buy"));
        
        // Check if the attached chest exists
        if (!sign.isInfinite() && (chest = InventoryUtils.getAttachedChest(player.getEntityWorld(), signPos)) == null)
            return Either.left(Text.literal("Failed to get attached chest"));
        
        /*
         * Check if chest is valid
         */
        if ( chest != null ) {
            chestInventory = InventoryUtils.getInventoryOf(player.getEntityWorld(), chest.getPos());
            
            // If the chest is open
            if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                return Either.left(ServerText.translatable(player, "shop.error.chest_open"));
            
            // If there is not enough of item in chest
            if (chestInventory == null || InventoryUtils.getInventoryCount(chestInventory, sign::itemMatchPredicate) < sign.getShopItemCount())
                return Either.left(ServerText.translatable(player, "shop.error.stock_chest", sign.getShopItemDisplay()));
        }
        
        // Take the players money
        if (!PlayerBalanceCallback.hasBalance(player, sign.getShopItemPrice()))
            return Either.left(ServerText.translatable(player, "shop.error.money_player"));
        
        // Give item to player from chest
        if (!InventoryUtils.chestToPlayer(player, signPos, chestInventory, player.getInventory(), sign::itemMatchPredicate, sign.getShopItemCount(), true, sign::createItemStack))
            return Either.left(ServerText.translatable(player, "shop.error.stock_chest", sign.getShopItemDisplay()));
        
        player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
        
        // Take the players money
        PlayerBalanceCallback.take(player, sign.getShopItemPrice());
        
        // Give the shop keeper money
        if (!sign.isInfinite())
            PlayerBalanceCallback.give(sign.getShopOwner(), sign.getShopItemPrice());
        
        // Get the shop owner
        Text name = TextUtils.mutable(PlayerNameCallback.getName(server, sign.getShopOwner()))
            .formatted(Formatting.AQUA);
        
        // Tell the player
        TitleUtils.showPlayerAlert(
            player,
            Formatting.YELLOW,
            Text.literal("You bought "),
            Text.literal(FormattingUtils.format( sign.getShopItemCount() ) + " ").formatted(Formatting.AQUA),
            Text.translatable(sign.getShopItemTranslationKey()).formatted(Formatting.AQUA),
            Text.literal(" from "),
            name
        );
        
        // Log the event
        CoreMod.logInfo(player.getName().getString() + " bought " + FormattingUtils.format( sign.getShopItemCount() ) + " " + sign.getShopItemIdentifier() + " for $" + FormattingUtils.format( sign.getShopItemPrice() ) + " from " + name.getString() );
        player.increaseStat(ShopStats.SHOP_TYPE_BOUGHT.getOrCreateStat(sign.getShopItem()), sign.getShopItemCount());
        
        return Either.right(Boolean.TRUE);
    }
}
