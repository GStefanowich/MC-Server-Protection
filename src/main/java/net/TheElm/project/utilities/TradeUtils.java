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

package net.TheElm.project.utilities;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Random;

public class TradeUtils {
    
    public static TradeOffers.Factory createSellItem(int countWants, Item itemWants, int countOffers, Item itemOffers, int maxTrades, int experience) {
        return new SellTradeFactory(itemWants, countWants, itemOffers, countOffers, maxTrades, experience);
    }
    public static TradeOffers.Factory createSellItem(int countWants1, Item itemWants1, int countWants2, Item itemWants2, int countOffers, Item itemOffers, int maxTrades, int experience) {
        return new SellTradeFactory(itemWants1, countWants1, itemWants2, countWants2, itemOffers, countOffers, maxTrades, experience);
    }
    public static TradeOffers.Factory createSellItem(int countEmeralds, int countOffers, Item itemOffers, int maxTrades, int experience) {
        return TradeUtils.createSellItem(countEmeralds, Items.EMERALD, countOffers, itemOffers, maxTrades, experience);
    }
    public static TradeOffers.Factory createSellSpawnEgg(int minimumEmeralds, Item itemOffers, int maxTrades, int experience) {
        return new EggTradeFactory(itemOffers, minimumEmeralds,maxTrades, experience);
    }
    
    private static class SellTradeFactory implements TradeOffers.Factory {
        private final Item itemWants1;
        private final int countWants1;
        
        private final Item itemWants2;
        private final int countWants2;
        
        private final Item itemOffers;
        private final int countOffers;
        
        private final int maxTrades;
        private final int experience;
        
        public SellTradeFactory(@NotNull Item itemWants, int countWants, @NotNull Item itemOffers, int countOffers, int maxTrades, int experience) {
            this(itemWants, countWants, null, 0, itemOffers, countOffers, maxTrades, experience);
        }
        public SellTradeFactory(@NotNull Item itemWants1, int countWants1, @Nullable Item itemWants2, int countWants2, @NotNull Item itemOffers, int countOffers, int maxTrades, int experience) {
            this.itemWants1 = itemWants1;
            this.itemWants2 = itemWants2;
            
            this.countWants1 = countWants1;
            this.countWants2 = countWants2;
            
            this.itemOffers = itemOffers;
            this.countOffers = countOffers;
            
            this.maxTrades = maxTrades;
            this.experience = experience;
        }
        
        @Override
        public TradeOffer create(Entity entity, Random random) {
            System.out.println(entity.getDisplayName().asString());
            
            return new TradeOffer(
                // Wants
                new ItemStack(this.itemWants1, this.countWants1),
                new ItemStack(this.itemWants2, this.countWants2),
                // Offers
                new ItemStack(this.itemOffers, this.countOffers),
                this.maxTrades, // Max trades
                this.experience, // Experience
                0.05F // Multiplier
            );
        }
    }
    private static class EggTradeFactory implements TradeOffers.Factory {
        private final Item spawnEgg;
        private final int minimumCost;
        
        private final int maxTrades;
        private final int experience;
        
        private EggTradeFactory(@NotNull Item spawnEgg, int minimumCost, int maxTrades, int experience) {
            this.spawnEgg = spawnEgg;
            this.minimumCost = minimumCost;
            
            this.maxTrades = maxTrades;
            this.experience = experience;
        }
        
        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack spawnEgg = new ItemStack(this.spawnEgg, 1);
            
            if (this.spawnEgg.equals(Items.WANDERING_TRADER_SPAWN_EGG)) {
                spawnEgg.setCustomName(new TranslatableText(Items.EGG.getTranslationKey()));
                
                CompoundTag display = spawnEgg.getOrCreateSubTag("display");
                display.put("Lore", NbtUtils.toList(Collections.singleton(new LiteralText("I 'wander' where this came from?")), Text.Serializer::toJson));
            }
            
            return new TradeOffer(
                // Wants
                new ItemStack(Items.EMERALD, IntUtils.random(random, this.minimumCost, 64)),
                new ItemStack(Items.EGG, 1),
                // Offers
                spawnEgg,
                this.maxTrades,
                this.experience,
                0.05F
            );
        }
    }
    
}
