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

package net.theelm.sewingmachine.base.objects;

import com.mojang.datafixers.util.Either;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.ContainerAccessCallback;
import net.theelm.sewingmachine.events.TaxCollection;
import net.theelm.sewingmachine.exceptions.ShopBuilderException;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.ShopSignBuilder;
import net.theelm.sewingmachine.utilities.ShopSigns;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;

/**
 * Created on Jun 25 2023 at 9:44 PM.
 * By greg in sewingmachine
 */
public abstract class ShopSign {
    public final @NotNull String name;
    private final @NotNull StyleApplicator applicator;
    
    protected ShopSign(@NotNull String name, @NotNull Formatting color) {
        this(name, TextColor.fromFormatting(color));
    }
    protected ShopSign(@NotNull String name, @Nullable TextColor color) {
        this(name, new StyleApplicator(color == null ? Objects.requireNonNull(TextColor.fromFormatting(Formatting.WHITE)) : color));
    }
    protected ShopSign(@NotNull String name, @NotNull StyleApplicator applicator) {
        this.name = name;
        this.applicator = applicator;
    }
    
    public boolean isEnabled() { return true; }
    
    public final @NotNull StyleApplicator getApplicator() {
        return this.applicator;
    }
    
    /**
     * Called when a Player interacts with the ShopSign
     * @param server The server
     * @param player The player that interacted with the sign
     * @param signPos Where the sign is positioned in the world
     * @param sign The shopsign data
     * @return Either a Text error, or a Boolean success
     */
    public abstract Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign);
    
    /**
     * Called after the ShopSignBuilder has completed
     * @param signBuilder The builder
     * @param creator The player that created the shop
     * @return If the formatting on the sign is correct, if false the sign will break
     * @throws ShopBuilderException
     */
    public abstract boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException;
    
    /**
     * Called after the ShopSignBuilder is completed, and when the shop is loaded
     * Will do things like updating usernames on shopsigns, and cleaning up rendering
     * @param shop The shop data
     * @return If the formatting on the sign is correct, if false the sign will break
     */
    public abstract boolean renderSign(@NotNull final ShopSignData shop);
    
    /**
     * Get a ShopSign using the text present on a Sign
     * @param text The text present on the sign
     * @return A registered ShopSign, if one exists
     */
    public static @Nullable ShopSign valueOf(@NotNull Text text) {
        return ShopSigns.getFromText(text);
    }
    
    public static abstract class BuyTradeSell extends ShopSign {
        protected BuyTradeSell(@NotNull String name, @NotNull Formatting color) {
            super(name, color);
        }
        protected BuyTradeSell(@NotNull String name, @Nullable TextColor color) {
            super(name, color);
        }
        protected BuyTradeSell(@NotNull String name, @NotNull StyleApplicator applicator) {
            super(name, applicator);
        }
        
        /**
         * Format the sign as FREE / ITEM-GIVEAWAY
         * @param signBuilder
         * @param creator
         * @return
         * @throws ShopBuilderException
         */
        protected boolean formatFreeSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            if (!signBuilder.textMatchItem(creator, signBuilder.getLines()[1]))
                throw new ShopBuilderException("Sign not formatted correctly.");
            
            // Validate that a container is attached
            this.validateAttachedContainer(signBuilder, creator);
            
            // Parse the owner of the sign
            signBuilder.textParseOwner(signBuilder.getLines()[3], creator);
            
            return this.renderSign(signBuilder);
        }
        
        protected void validateAttachedContainer(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            LootableContainerBlockEntity container = null;
            if (!( creator.isCreative() || ((container = InventoryUtils.getAttachedChest(signBuilder)) != null)))
                throw new ShopBuilderException("Could not find storage for sign.");
            
            if (container != null && !ContainerAccessCallback.canAccess(creator, container.getPos()))
                throw new ShopBuilderException("Missing permission to access that container.");
        }
        
        protected void printCompletedSign(@NotNull final ServerPlayerEntity player, @NotNull final ShopSignBuilder signBuilder) {
            Income value = new Income(player, signBuilder.getShopItemPrice());
            MutableText output = Text.literal("Created new shop for ").formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(signBuilder.getShopItemCount()))
                .append(" ")
                .append(MessageUtils.formatObject(signBuilder.getShopItem()));
            
            TaxCollection.EVENT.invoker()
                .collect(value, player.getServerWorld(), signBuilder.getSign().getPos());
            
            Iterator<Tax> taxes = value.getTaxes()
                .iterator();
            
            while (taxes.hasNext()) {
                Tax tax = taxes.next();
                Text name = Text.literal(tax.name().getString())
                    .formatted(Formatting.AQUA);
                
                if (tax.value() > 0) {
                    output.append("\n| ")
                        .append(name)
                        .append(" taxes are ")
                        .append(MessageUtils.formatNumber(tax.percent()))
                        .append("% ($")
                        .append(MessageUtils.formatNumber(tax.value()))
                        .append(")");
                } else if (tax.percent() > 0) {
                    output.append("\n| ")
                        .append(name)
                        .append(" taxes do not apply.");
                }
            }
            
            boolean taxed = value.getValue() != value.getValueAfterTaxes();
            
            output.append("\n| Recipient gets $")
                .append(MessageUtils.formatNumber(value.getValueAfterTaxes()))
                .append(taxed ? " after taxes." : ".");
            
            player.sendMessage(output, false);
        }
        
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewBaseConfig.DO_MONEY) && SewConfig.get(SewBaseConfig.SHOP_SIGNS);
        }
    }
}
