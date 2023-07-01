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
import net.minecraft.recipe.Recipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.PlayerBalanceCallback;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.exceptions.NotEnoughMoneyException;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.base.objects.ShopCraftAction;
import net.theelm.sewingmachine.base.objects.ShopStats;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/*
 * Chest is BUYING
 */
public final class SignShopSell extends ShopSign.BuyTradeSell {
    public SignShopSell() {
        super("SELL", ShopSignData.APPLICATOR_RED);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
        if ((!signBuilder.textMatchItem(creator, signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
            throw new ShopBuilderException("Sign not formatted correctly.");
        
        // Validate that a container is attached
        this.validateAttachedContainer(signBuilder, creator);
        
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
                .append("$" + shop.getShopItemPrice()).styled(ShopSignData.APPLICATOR_GREEN),
            
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
        if ( !SewConfig.get(SewCoreConfig.DO_MONEY) )
            return Either.right(Boolean.TRUE);
        
        // These should NOT be null
        if (((sign.getShopItem()) == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null) || (sign.getShopItemDisplay() == null))
            return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
        
        // Check if the attached chest exists
        if (sign.isInfinite() || ((chest = InventoryUtils.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
            if (player.getUuid().equals(sign.getShopOwner()))
                return Either.left(TranslatableServerSide.text(player, "shop.error.self_sell"));
            
            /*
             * Check if chest is valid
             */
            if ((chest != null)) {
                chestInventory = InventoryUtils.getInventoryOf(player.getEntityWorld(), chest.getPos());
                
                // If the chest is open
                if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                    return Either.left(TranslatableServerSide.text(player, "shop.error.chest_open"));
                
                // If player does not have any of item
                if (InventoryUtils.getInventoryCount(player.getInventory(), sign::itemMatchPredicate) < sign.getShopItemCount())
                    return Either.left(TranslatableServerSide.text(player, "shop.error.stock_player", sign.getShopItemDisplay()));
            }
            
            // Take shop keepers money
            if (!(sign.isInfinite() || PlayerBalanceCallback.hasBalance(sign.getShopOwner(), sign.getShopItemPrice())))
                return Either.left(TranslatableServerSide.text(player, "shop.error.money_chest"));
            
            // Put players item into chest
            if (!InventoryUtils.playerToChest(player, signPos, player.getInventory(), chestInventory, sign::itemMatchPredicate, sign.getShopItemCount(), true)) {
                boolean crafted = false;
                
                List<? extends Recipe<?>> recipes = sign.getShopItemRecipes();
                if (sign.isInfinite() && recipes != null) {
                    for (Recipe recipe : recipes) {
                        ShopCraftAction craft = new ShopCraftAction(recipe, sign, signPos, chestInventory);
                        
                        if (craft.craft(player)) {
                            crafted = true;
                            break;
                        }
                    }
                }
                
                if (!crafted)
                    return Either.left(TranslatableServerSide.text(player, "shop.error.stock_player", sign.getShopItemDisplay()));
            }
            
            // Give player money for item
            PlayerBalanceCallback.give(player, sign.getShopItemPrice());
            
            // Get shop owner
            Text name = TextUtils.mutable(PlayerNameCallback.getName(server, sign.getShopOwner()))
                .formatted(Formatting.AQUA);
            
            // Tell the player
            TitleUtils.showPlayerAlert(
                player,
                Formatting.YELLOW,
                Text.literal("You sold "),
                Text.literal(FormattingUtils.format( sign.getShopItemCount() ) + " ").formatted(Formatting.AQUA),
                Text.translatable(sign.getShopItemTranslationKey()).formatted(Formatting.AQUA),
                Text.literal(" to "),
                name
            );
            
            // Log the event
            CoreMod.logInfo(player.getName().getString() + " sold " + FormattingUtils.format(sign.getShopItemCount()) + " " + sign.getShopItemIdentifier() + " for $" + FormattingUtils.format(sign.getShopItemPrice()) + " to " + name.getString());
            player.increaseStat(ShopStats.SHOP_TYPE_SOLD.getOrCreateStat(sign.getShopItem()), sign.getShopItemCount());
            
            return Either.right(Boolean.TRUE);
        }
        return Either.right(Boolean.FALSE);
    }
}
