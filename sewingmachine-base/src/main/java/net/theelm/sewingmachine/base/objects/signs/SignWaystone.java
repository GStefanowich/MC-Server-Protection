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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.PlayerBalanceCallback;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.events.BlockBreakCallback;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.DimensionUtils;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.IntUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.theelm.sewingmachine.utilities.WarpUtils;
import org.jetbrains.annotations.NotNull;

/*
 * Move the players warp
 */
public final class SignWaystone extends ShopSign {
    public SignWaystone() {
        super("WAYSTONE", Formatting.DARK_PURPLE);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder sign, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
        // Set the signs price
        sign.setTradePrice(SewConfig.get(SewCoreConfig.WARP_WAYSTONE_COST));
        
        String name;
        Text nameText = sign.getSignLine(1);
        if (nameText == null || (name = nameText.getString()) == null || name.isEmpty())
            name = WarpUtils.PRIMARY_DEFAULT_HOME;
        else {
            if (!IntUtils.between(1, name.length(), 15))
                throw new ShopBuilderException(TranslatableServerSide.text(creator, "warp.notice.name.too_long", 15));
            
            if (!WarpUtils.validateName(name))
                throw new ShopBuilderException(TranslatableServerSide.text(creator, "warp.notice.name.invalid"));
        }
        
        // Set the sign owner to SPAWN
        sign.setShopOwner(CoreMod.SPAWN_ID);
        return this.renderSign(sign);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        Text name = shop.getSignLine(1);
        if (name == null)
            return false;
        
        Text priceText = Text.literal("$" + FormattingUtils.format(shop.getShopItemPrice()))
            .formatted(Formatting.DARK_BLUE);
        
        return shop.setSign(new Text[] {
            Text.literal(name.getString())
                .formatted(Formatting.GREEN),
            Text.literal("Build here")
                .formatted(Formatting.BLACK),
            Text.literal("for ")
                .formatted(Formatting.BLACK)
                .append(priceText)
        });
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
        ServerWorld world = player.getServerWorld();
        
        // Check the build permissions
        if (
            DimensionUtils.isOutOfBuildLimitVertically(world, signPos)
                || DimensionUtils.isWithinProtectedZone(world, signPos)
                || !BlockBreakCallback.canDestroy(player, world, Hand.MAIN_HAND, signPos, null, null)
        ) return Either.left(Text.literal("Can't build that here"));
        
        final String warpName = sign.getSignLine(1)
            .getString();
        
        int warps = WarpUtils.getWarps(player)
            .size();
        int allowed = SewConfig.get(SewCoreConfig.WARP_WAYSTONES_ALLOWED);
        
        if (warps >= allowed && (WarpUtils.getWarp(player, warpName) == null))
            return Either.left(Text.literal("Too many waystones. Can't build any more."));
        
        int cost = SewConfig.get(SewCoreConfig.WARP_WAYSTONE_COST);
        
        PlayerBalanceCallback bank = PlayerBalanceCallback.EVENT.invoker();
        
        if (!bank.hasBalance(player, cost))
            return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
        
        WarpUtils warp = new WarpUtils(warpName, player, player.getServerWorld(),signPos.down());
        if (!warp.claimAndBuild(() -> {
            bank.take(player, cost);

            warp.save(warp.getSafeTeleportPos(), player);
        })) {
            // Notify the player
            player.sendMessage(
                Text.literal("Can't build that here")
                    .formatted(Formatting.RED)
            );
        }
        
        return Either.right(Boolean.TRUE);
    }
}
