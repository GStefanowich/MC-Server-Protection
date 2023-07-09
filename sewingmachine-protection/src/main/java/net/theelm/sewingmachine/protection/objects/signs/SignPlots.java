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

package net.theelm.sewingmachine.protection.objects.signs;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.exceptions.NotEnoughMoneyException;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.packets.ClaimCountPacket;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.ModUtils;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;

/*
 * Allow players to buy chunk claims
 */
public final class SignPlots extends ShopSign {
    public SignPlots() {
        super("PLOTS", Formatting.GREEN);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) {
        // Break if not in creative mode
        if (!creator.isCreative())
            return false;
        
        // Parse the String from the sign
        if ((!builder.textMatchCount(builder.getLines()[1])) || (!builder.textMatchPrice(builder.getLines()[2])))
            return false;
        
        // Set the sign owner to SPAWN
        builder.setShopOwner(CoreMod.SPAWN_ID);
        
        return this.renderSign(builder);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        NumberFormat formatter = NumberFormat.getInstance();
        
        return shop.setSign(new Text[] {
            Text.literal(formatter.format(shop.getShopItemCount()) + " chunks"),
            Text.literal("for ").formatted(Formatting.BLACK)
                .append(Text.literal("$" + shop.getShopItemPrice()).formatted(Formatting.DARK_BLUE))
        });
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull ServerPlayerEntity player, @NotNull BlockPos signPos, ShopSignData sign) {
        // These should NOT be null
        if ((sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null))
            return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
        
        // If shops disabled
        if ( !this.isEnabled() )
            return Either.right(Boolean.TRUE);
        
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        
        try {
            // Take the players money
            if (!MoneyUtils.takePlayerMoney(player, sign.getShopItemPrice()))
                return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
            
            if ((SewConfig.get(SewProtectionConfig.PLAYER_CLAIM_BUY_LIMIT) > 0) && ((claim.getMaxChunkLimit() + sign.getShopItemCount()) > SewConfig.get(SewProtectionConfig.PLAYER_CLAIM_BUY_LIMIT)))
                return Either.left(Text.translatable("Can't buy any more of that."));
            
            // Increase the players chunk count
            ((PlayerClaimData) player).getClaim()
                .increaseMaxChunkLimit( sign.getShopItemCount() );
            
            // Log the transaction
            CoreMod.logInfo(player.getName().getString() + " bought " + FormattingUtils.format( sign.getShopItemCount() ) + " chunks for $" + FormattingUtils.format(sign.getShopItemPrice()));
            
        } catch (NotEnoughMoneyException e) {
            return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
        }
        
        // Send a network update packet
        if (ModUtils.hasModule(player, "protection"))
            NetworkingUtils.send(player, new ClaimCountPacket(claim));
        
        player.sendMessage(Text.translatable("Chunks claimed ").formatted(Formatting.YELLOW)
            .append(Text.translatable(FormattingUtils.format(claim.getCount())).formatted(Formatting.AQUA))
            .append(" / ")
            .append(Text.translatable(FormattingUtils.format(claim.getMaxChunkLimit())).formatted(Formatting.AQUA))
        );
        
        return Either.right(Boolean.TRUE);
    }
    
    @Override
    public boolean isEnabled() {
        return SewConfig.get(SewBaseConfig.DO_MONEY)
            && SewConfig.get(SewProtectionConfig.PLAYER_CLAIM_BUY_LIMIT) != 0;
    }
}
