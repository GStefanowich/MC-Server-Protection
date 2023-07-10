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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.exceptions.NotEnoughMoneyException;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.base.objects.PlayerBackpack;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.ServerText;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;

/*
 * Player backpacks
 */
public final class SignBackpack extends ShopSign {
    public SignBackpack() {
        super("BACKPACK", Formatting.YELLOW);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
        if (!creator.isCreative())
            return false;
        
        // Parse the String from the sign
        if ((!builder.textMatchCount(builder.getLines()[1])) || (!builder.textMatchPrice(builder.getLines()[2])))
            return false;
        
        if ((builder.getShopItemCount() % 9 != 0 ) || builder.getShopItemCount() > 54)
            throw new ShopBuilderException("Backpack size must be multiples of 9, no larger than 54.");
        
        // Set the sign owner to SPAWN
        builder.setShopOwner(CoreMod.SPAWN_ID);
        
        // Render the sign
        return this.renderSign(builder);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        NumberFormat formatter = NumberFormat.getInstance();
        
        Text[] text = new Text[] {
            Text.literal(formatter.format(shop.getShopItemCount()) + " slots"),
            Text.literal("for ").formatted(Formatting.BLACK)
                .append(Text.literal("$" + shop.getShopItemPrice()).formatted(Formatting.DARK_BLUE)),
            MutableText.of(new LiteralTextContent(""))
        };
        
        return shop.setSign(text);
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull ServerPlayerEntity player, @NotNull BlockPos signPos, ShopSignData sign) {
        // These should NOT be null
        if ((sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null))
            return Either.left(ServerText.text(player, "shop.error.database"));
        
        // Get the number of rows in the backpack
        int newPackRows = sign.getShopItemCount() / 9;
        
        // If shops disabled
        if ( !this.isEnabled() )
            return Either.right(Boolean.TRUE);
        
        BackpackCarrier backpackCarrier = (BackpackCarrier) player;
        PlayerBackpack backpack = backpackCarrier.getBackpack();
        
        try {
            // Check the current backpack size
            if ((backpack != null) && (newPackRows <= backpack.getRows()))
                return Either.left(ServerText.text(player, "backpack.no_downsize"));
            
            // If backpacks must be purchased in order
            if ( SewConfig.get(SewBaseConfig.BACKPACK_SEQUENTIAL)) {
                int currentRows = ( backpack == null ? 0 : backpack.getRows() );
                if ((newPackRows - 1) > currentRows)
                    return Either.left(ServerText.text(player, "backpack.need_previous"));
            }
            
            // Take the players money
            if (!MoneyUtils.takePlayerMoney(player, sign.getShopItemPrice()))
                return Either.left(ServerText.text(player, "shop.error.money_player"));
            
            backpackCarrier.setBackpack((backpack == null ?
                new PlayerBackpack(player, newPackRows)
                : new PlayerBackpack(backpack, newPackRows)
            ), true);
            
            player.sendMessage(Text.literal("Backpack size is now ").formatted(Formatting.YELLOW)
                .append(Text.literal(FormattingUtils.format(sign.getShopItemCount())).formatted(Formatting.AQUA))
            );
            
            // Log the transaction
            CoreMod.logInfo(player.getName().getString() + " bought a " + FormattingUtils.format(sign.getShopItemCount()) + " slot backpack for $" + FormattingUtils.format(sign.getShopItemPrice()));
            
        } catch (NotEnoughMoneyException e) {
            return Either.left(ServerText.text(player, "shop.error.money_player"));
        }
        
        return Either.right(Boolean.TRUE);
    }
    
    @Override
    public boolean isEnabled() {
        return (SewConfig.get(SewBaseConfig.DO_MONEY) && SewConfig.get(SewBaseConfig.ALLOW_BACKPACKS));
    }
}
