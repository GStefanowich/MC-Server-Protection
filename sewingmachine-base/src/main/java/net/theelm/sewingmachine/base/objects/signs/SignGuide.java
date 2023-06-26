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

import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.GuideUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import org.jetbrains.annotations.NotNull;

/*
 * Player guide books
 */
public final class SignGuide extends ShopSign {
    public SignGuide() {
        super("GUIDES", Formatting.DARK_GREEN);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) {
        if (creator.isCreative()) {
            // Set the sign owner
            builder.setShopOwner(CoreMod.SPAWN_ID);
            
            return this.renderSign(builder);
        }
        return false;
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        Text guideName = shop.getSignLine(1);
        if (guideName == null)
            return false;

        return shop.setSign(new Text[] {
            Text.literal(guideName.getString())
        });
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
        // Get the guides title
        String bookRawTitle = sign.getSignLine(1).getString();
        
        // Get the guidebook
        GuideUtils guide;
        try {
            if ((guide = GuideUtils.getBook(bookRawTitle.toLowerCase())) == null)
                return Either.right(Boolean.FALSE);
        } catch (JsonSyntaxException e) {
            CoreMod.logError(e);
            return Either.left(Text.literal("An error occurred getting that guide."));
        }
        
        // Give the player the book
        player.giveItemStack(guide.newStack());
        player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
        
        return Either.right(Boolean.TRUE);
    }
}
