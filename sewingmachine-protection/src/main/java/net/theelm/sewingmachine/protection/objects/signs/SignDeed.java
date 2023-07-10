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
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.base.objects.ShopSign;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protections.BlockRange;
import net.theelm.sewingmachine.utilities.CasingUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.ServerText;
import org.jetbrains.annotations.NotNull;

/*
 * Allow players to sell chunks/region in their towns
 */
public final class SignDeed extends ShopSign {
    public SignDeed() {
        super("DEED", Formatting.DARK_GRAY);
    }
    
    @Override
    public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
        SignBlockEntity sign = builder.getSign();
        
        // Get the cost for the DEED
        if (!builder.textMatchPrice(builder.getLines()[2]))
            throw new ShopBuilderException(Text.literal("Sign is missing a cost"));
        
        // These should NOT be null
        if (sign.getWorld() == null)
            throw new ShopBuilderException(ServerText.text(creator, "shop.error.database"));
        
        WorldChunk chunk = sign.getWorld().getWorldChunk( sign.getPos() );
        ClaimantTown town = null;
        
        // Get the deed type
        String deedType = ( builder.getLines()[1].getString().equalsIgnoreCase("region") ? "region" : "chunk" );
        
        Text[] text = new Text[3];
        
        // Handle the deed type
        switch (deedType) {
            case "chunk": {
                // Check that the sign is within a town
                if ((chunk == null) || ((town = ((IClaimedChunk) chunk).getTown()) == null))
                    throw new ShopBuilderException(Text.literal("Deed sign must be placed within a town."));
                
                // Check who placed the sign
                if (!(creator.getUuid().equals(town.getOwnerId())) && creator.getUuid().equals(((IClaimedChunk) chunk).getOwnerId()))
                    throw new ShopBuilderException(Text.literal("Deed signs may only be placed in chunks belonging to the town owner, by the town owner."));
                
                text[0] = Text.literal(CasingUtils.sentence(deedType));
                break;
            }
            case "region": {
                BlockPos firstPos;
                BlockPos secondPos;
                
                // Check that the region is defined
                if (((firstPos = ((PlayerData) creator).getRulerA()) == null) || ((secondPos = ((PlayerData) creator).getRulerB()) == null))
                    throw new ShopBuilderException(Text.literal("Deed sign must be within a valid region. Use \"").append(Text.literal("/ruler").formatted(Formatting.AQUA)).append("\" command to select two points"));
                
                // Check that the region contains the sign
                BlockRange region = BlockRange.between(firstPos, secondPos);
                if (!region.isWithin(sign.getPos()))
                    throw new ShopBuilderException(Text.literal("Deed sign must be within a valid region. Use \"").append(Text.literal("/ruler").formatted(Formatting.AQUA)).append("\" command to select two points"));
                
                // Clear ruler area
                ((PlayerData) creator).setRulerA(null);
                ((PlayerData) creator).setRulerB(null);
                
                // Validate the minimum and maximum widths
                int maxWidth = SewConfig.get(SewProtectionConfig.MAXIMUM_REGION_WIDTH);
                int minWidth = SewConfig.get(SewProtectionConfig.MINIMUM_REGION_WIDTH);
                
                // Check the size of the region
                if ((maxWidth > 0) && ((region.getNorthSouth() > maxWidth) || (region.getEastWest() > maxWidth)))
                    throw new ShopBuilderException(Text.literal("Deed region is too large."));
                if ((region.getNorthSouth() < minWidth) || (region.getEastWest() < minWidth))
                    throw new ShopBuilderException(Text.literal("Deed region is too small."));
                
                // Update the sign to display the width
                text[0] = region.displayDimensions();
                builder.regionPositioning(firstPos, secondPos);
                break;
            }
            default:
                return false;
        }
        
        text[1] = Text.literal("for ").formatted(Formatting.BLACK).append(
            Text.literal("$" + builder.getShopItemPrice()).formatted(Formatting.DARK_BLUE)
        );
        text[2] = (town == null ? Text.literal("") : town.getName()).formatted(Formatting.DARK_GRAY);
        
        // Set the sign owner
        builder.setShopOwner(creator.getUuid());
        return builder.setSign(text);
    }
    
    @Override
    public boolean renderSign(@NotNull ShopSignData shop) {
        return true;
    }
    
    @Override
    public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
        // If shops disabled
        if (!SewConfig.get(SewBaseConfig.DO_MONEY))
            return Either.right(Boolean.TRUE);
        
        if ((sign.getFirstPos() == null) || (sign.getSecondPos() == null))
            return Either.left(Text.literal("Invalid deed sign"));
        
        return Either.right(Boolean.FALSE);
    }
    
    @Override
    public boolean isEnabled() {
        return SewConfig.get(SewBaseConfig.DO_MONEY);
    }
}
