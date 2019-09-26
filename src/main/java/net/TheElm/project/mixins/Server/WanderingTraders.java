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

package net.TheElm.project.mixins.Server;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.TheElm.project.utilities.TradeUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TraderOfferList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraders extends AbstractTraderEntity {
    
    public WanderingTraders(EntityType<? extends AbstractTraderEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    private static Int2ObjectMap<TradeOffers.Factory[]> copyToFastUtilMap(ImmutableMap<Integer, TradeOffers.Factory[]> immutableMap_1) {
        return new Int2ObjectOpenHashMap( immutableMap_1 );
    }
    
    @Inject(at = @At("HEAD"), method = "fillRecipes", cancellable = true)
    protected void fillRecipes(CallbackInfo callback) {
        TradeOffers.Factory[] mainFactory = WANDERING_TRADER_TRADES.get(1);
        TradeOffers.Factory[] rareFactory = WANDERING_TRADER_TRADES.get(2);
        if (mainFactory != null && rareFactory != null) {
            TraderOfferList traderOfferList_1 = this.getOffers();
            this.fillRecipesFromPool(traderOfferList_1, mainFactory, 5);
            
            int int_1 = this.random.nextInt(rareFactory.length);
            
            TradeOffers.Factory tradeOffers$Factory_1 = rareFactory[int_1];
            TradeOffer tradeOffer_1 = tradeOffers$Factory_1.create(this, this.random);
            if (tradeOffer_1 != null) {
                traderOfferList_1.add(tradeOffer_1);
            }
        }
        callback.cancel();
    }
    
    // TODO: Make wandering traders have useful trades. Spawn eggs?
    private static final Int2ObjectMap<TradeOffers.Factory[]> WANDERING_TRADER_TRADES = copyToFastUtilMap(
        ImmutableMap.of(
            1, new TradeOffers.Factory[]{
                TradeUtils.createSellItem(Items.SEA_PICKLE, 64, 1, 5, 1),
                TradeUtils.createSellItem(Items.SLIME_BALL, 64, 1, 5, 1),
                TradeUtils.createSellItem(Items.GLOWSTONE, 64, 1, 5, 1),
                TradeUtils.createSellItem(Items.NAUTILUS_SHELL, 64, 1, 5, 1),
                TradeUtils.createSellItem(Items.FERN, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.SUGAR_CANE, 64, 1, 8, 1),
                TradeUtils.createSellItem(Items.PUMPKIN, 64, 1, 4, 1),
                TradeUtils.createSellItem(Items.KELP, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.CACTUS, 64, 1, 8, 1),
                TradeUtils.createSellItem(Items.DANDELION, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.POPPY, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.BLUE_ORCHID, 64, 1, 8, 1),
                TradeUtils.createSellItem(Items.ALLIUM, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.AZURE_BLUET, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.RED_TULIP, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.ORANGE_TULIP, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.WHITE_TULIP, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.PINK_TULIP, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.OXEYE_DAISY, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.CORNFLOWER, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.LILY_OF_THE_VALLEY, 64, 1, 7, 1),
                TradeUtils.createSellItem(Items.WHEAT_SEEDS, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.BEETROOT_SEEDS, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.PUMPKIN_SEEDS, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.MELON_SEEDS, 64, 1, 12, 1),
                TradeUtils.createSellItem(Items.ACACIA_SAPLING, 5, 1, 8, 1),
                TradeUtils.createSellItem(Items.BIRCH_SAPLING, 5, 1, 8, 1),
                TradeUtils.createSellItem(Items.DARK_OAK_SAPLING, 5, 1, 8, 1),
                TradeUtils.createSellItem(Items.JUNGLE_SAPLING, 5, 1, 8, 1),
                TradeUtils.createSellItem(Items.OAK_SAPLING, 5, 1, 8, 1),
                TradeUtils.createSellItem(Items.SPRUCE_SAPLING, 5, 1, 8, 1),
                TradeUtils.createSellItem(Items.RED_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.WHITE_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.BLUE_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.PINK_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.BLACK_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.GREEN_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.LIGHT_GRAY_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.MAGENTA_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.YELLOW_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.GRAY_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.PURPLE_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.LIGHT_BLUE_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.LIME_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.ORANGE_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.BROWN_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.CYAN_DYE, 1, 3, 12, 1),
                TradeUtils.createSellItem(Items.BRAIN_CORAL_BLOCK, 3, 1, 8, 1),
                TradeUtils.createSellItem(Items.BUBBLE_CORAL_BLOCK, 3, 1, 8, 1),
                TradeUtils.createSellItem(Items.FIRE_CORAL_BLOCK, 3, 1, 8, 1),
                TradeUtils.createSellItem(Items.HORN_CORAL_BLOCK, 3, 1, 8, 1),
                TradeUtils.createSellItem(Items.TUBE_CORAL_BLOCK, 3, 1, 8, 1),
                TradeUtils.createSellItem(Items.VINE, 1, 1, 12, 1),
                TradeUtils.createSellItem(Items.BROWN_MUSHROOM, 1, 1, 12, 1),
                TradeUtils.createSellItem(Items.RED_MUSHROOM, 1, 1, 12, 1),
                TradeUtils.createSellItem(Items.LILY_PAD, 1, 2, 5, 1),
                TradeUtils.createSellItem(Items.SAND, 1, 8, 8, 1),
                TradeUtils.createSellItem(Items.RED_SAND, 1, 4, 6, 1)
            },
        2, new TradeOffers.Factory[]{
                TradeUtils.createSellItem(Items.TROPICAL_FISH_BUCKET, 5, 1, 4, 1),
                TradeUtils.createSellItem(Items.PUFFERFISH_BUCKET, 5, 1, 4, 1),
                TradeUtils.createSellItem(Items.PACKED_ICE, 3, 1, 6, 1),
                TradeUtils.createSellItem(Items.BLUE_ICE, 6, 1, 6, 1),
                TradeUtils.createSellItem(Items.GUNPOWDER, 1, 1, 8, 1),
                TradeUtils.createSellItem(Items.PODZOL, 3, 3, 6, 1)
            }
        )
    );
    
}
