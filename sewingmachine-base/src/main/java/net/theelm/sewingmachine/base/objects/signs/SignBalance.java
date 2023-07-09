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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import org.jetbrains.annotations.NotNull;

/*
 * Check player balance
 */
public final class SignBalance extends ShopSign {
    public SignBalance() {
        super("BALANCE", Formatting.GOLD);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder sign, @NotNull final ServerPlayerEntity creator) {
        // Set the sign owner to SPAWN
        sign.setShopOwner(CoreMod.SPAWN_ID);
        return this.renderSign(sign);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        return shop.setSign(new Text[0]);
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
        // If shops disabled
        if ( !SewConfig.get(SewBaseConfig.DO_MONEY) )
            return Either.right(Boolean.TRUE);
        
        long playerHas = MoneyUtils.getPlayerMoney( player );
        player.sendMessage(TranslatableServerSide.text( player, "player.money",
            playerHas
        ));
        
        return Either.right(Boolean.TRUE);
    }
    
    @Override
    public boolean isEnabled() {
        return SewConfig.get(SewBaseConfig.DO_MONEY);
    }
}
