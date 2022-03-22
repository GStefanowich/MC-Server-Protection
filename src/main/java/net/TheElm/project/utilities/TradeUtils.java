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
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class TradeUtils {
    
    public static @NotNull TradeOffers.Factory createSellItem(int countWants, @NotNull Item itemWants, int countOffers, @NotNull Item itemOffers, int maxTrades) {
        return new SellTradeFactory(itemWants, countWants, itemOffers, countOffers, maxTrades);
    }
    public static @NotNull TradeOffers.Factory createSellItem(int countWants1, @NotNull Item itemWants1, int countWants2, @NotNull Item itemWants2, int countOffers, @NotNull Item itemOffers, int maxTrades) {
        return new SellTradeFactory(itemWants1, countWants1, itemWants2, countWants2, itemOffers, countOffers, maxTrades);
    }
    public static @NotNull TradeOffers.Factory createSellItem(int countEmeralds, int countOffers, Item itemOffers, int maxTrades) {
        return TradeUtils.createSellItem(countEmeralds, Items.EMERALD, countOffers, itemOffers, maxTrades);
    }
    public static @NotNull TradeOffers.Factory createSellSpawnEgg(int minimumEmeralds, @NotNull Item itemOffers, int maxTrades) {
        if (!(itemOffers instanceof SpawnEggItem spawnEggItem))
            throw new IllegalArgumentException("Provided trade item is not a Spawn Egg.");
        return new EggTradeFactory(spawnEggItem, minimumEmeralds,maxTrades);
    }
    public static @NotNull <FC extends FeatureConfig> TradeOffers.Factory createSellLocator(int minimumEmeralds, @NotNull StructureFeature<FC> structure, int pearlUses) {
        return new StructureEnderLocatorTradeFactory<>(structure, minimumEmeralds, pearlUses);
    }
    public static @NotNull TradeOffers.Factory createSellLocator(int minimumEmeralds, @NotNull RegistryKey<Biome> biome, int pearlUses) {
        return new BiomeEnderLocatorTradeFactory(biome, minimumEmeralds, pearlUses);
    }
    
    private static class SellTradeFactory implements TradeOffers.Factory {
        private final Item itemWants1;
        private final int countWants1;
        
        private final Item itemWants2;
        private final int countWants2;
        
        private final Item itemOffers;
        private final int countOffers;
        
        private final int maxTrades;
        
        public SellTradeFactory(@NotNull Item itemWants, int countWants, @NotNull Item itemOffers, int countOffers, int maxTrades) {
            this(itemWants, countWants, null, 0, itemOffers, countOffers, maxTrades);
        }
        public SellTradeFactory(@NotNull Item itemWants1, int countWants1, @Nullable Item itemWants2, int countWants2, @NotNull Item itemOffers, int countOffers, int maxTrades) {
            this.itemWants1 = itemWants1;
            this.itemWants2 = itemWants2;
            
            this.countWants1 = countWants1;
            this.countWants2 = countWants2;
            
            this.itemOffers = itemOffers;
            this.countOffers = countOffers;
            
            this.maxTrades = maxTrades;
        }
        
        @Override
        public TradeOffer create(Entity entity, Random random) {
            return new TradeOffer(
                // Wants
                new ItemStack(this.itemWants1, this.countWants1),
                new ItemStack(this.itemWants2, this.countWants2),
                // Offers
                new ItemStack(this.itemOffers, this.countOffers),
                this.maxTrades, // Max trades
                1, // Experience
                0.05F // Multiplier
            );
        }
    }
    private static class EggTradeFactory implements TradeOffers.Factory {
        private final SpawnEggItem spawnEgg;
        private final int minimumCost;
        
        private final int maxTrades;
        
        private EggTradeFactory(@NotNull SpawnEggItem spawnEgg, int minimumCost, int maxTrades) {
            this.spawnEgg = spawnEgg;
            this.minimumCost = minimumCost;
            
            this.maxTrades = maxTrades;
        }
        
        @Override
        public TradeOffer create(Entity entity, Random random) {
            ItemStack spawnEgg = new ItemStack(this.spawnEgg, 1);
            
            if (!this.spawnEgg.equals(Items.WANDERING_TRADER_SPAWN_EGG))
                ItemUtils.setLore(spawnEgg, new LiteralText("You can hear a ")
                    .append(new TranslatableText(this.spawnEgg.getEntityType(null).getTranslationKey()))
                    .append(" rumbling around inside."));
            else {
                spawnEgg.setCustomName(new TranslatableText(Items.EGG.getTranslationKey()));
                ItemUtils.setLore(spawnEgg, new LiteralText("I 'wander' where this came from?"));
            }
            
            return new TradeOffer(
                // Wants
                new ItemStack(Items.EMERALD, IntUtils.random(random, this.minimumCost, 64)),
                new ItemStack(Items.EGG, 1),
                // Offers
                spawnEgg,
                this.maxTrades,
                1,
                0.05F
            );
        }
    }
    private static abstract class EnderLocatorTradeFactory implements TradeOffers.Factory {
        private static Item THROWABLE = Items.ENDER_EYE;
        protected final int emeralds;
        protected final int uses;
        
        protected EnderLocatorTradeFactory(int emeralds, int uses) {
            this.emeralds = emeralds;
            this.uses = uses;
        }
        
        @Override
        public @Nullable TradeOffer create(Entity entity, Random random) {
            int strength = IntUtils.random(random, 0, 2);
            
            ItemStack pearl = this.createPearl();
            pearl.setCustomName(this.itemName(strength));
            pearl.getOrCreateNbt()
                .putInt("strength", strength);
            
            return new TradeOffer(
                // Wants
                new ItemStack(Items.EMERALD, IntUtils.random(random, this.emeralds, 64)),
                new ItemStack(Items.ENDER_EYE, 1),
                // Offers
                pearl,
                1, // Max trades
                1, // Experience
                0.05F // Multiplier
            );
        }
        
        protected @NotNull ItemStack createPearl() {
            return ItemUtils.setLore(new ItemStack(THROWABLE, 1), this.usesDescription());
        }
        protected @NotNull ItemStack createPearl(@NotNull MutableText description) {
            return ItemUtils.setLore(new ItemStack(THROWABLE, 1), new LiteralText("Throw to locate: ").append(description.formatted(Formatting.WHITE)), this.usesDescription());
        }
        protected abstract @NotNull Text itemName(int strength);
        private @NotNull Text usesDescription() {
            // ToDo: Change Ender Pearls from % breaking to # Uses
            return new LiteralText("Fragile, may break after a couple uses");
            /*return new LiteralText("Can be thrown ")
                .append(new LiteralText(FormattingUtils.number(this.uses)).formatted(Formatting.WHITE))
                .append(" more times");*/
        }
    }
    private static class StructureEnderLocatorTradeFactory<FC extends FeatureConfig> extends EnderLocatorTradeFactory implements TradeOffers.Factory {
        @NotNull
        private final StructureFeature<FC> structure;
        
        public StructureEnderLocatorTradeFactory(@NotNull StructureFeature<FC> structure, int minimumEmeralds, int pearlUses) {
            super(minimumEmeralds, pearlUses);
            this.structure = structure;
        }
        
        @Override
        protected @NotNull ItemStack createPearl() {
            String structureName = this.structure.getClass().getName();
            
            ItemStack pearl = super.createPearl(new LiteralText(this.description(structureName)));
            NbtCompound throwDat = pearl.getOrCreateSubNbt("throw");
            
            throwDat.putString("structure", structureName);
            throwDat.putInt("uses", this.uses);
            
            return pearl;
        }
        
        @Override
        protected @NotNull Text itemName(int strength) {
            return switch (strength) {
                case 0 -> new LiteralText("Weak Structure Locator");
                case 1 -> new LiteralText("Normal Structure Locator");
                default -> new LiteralText("Strong Structure Locator");
            };
        }
        
        protected @NotNull String description(@Nullable String key) {
            if (key == null)
                return "Unknown";
            return switch (key) {
                case "bastion_remnant" -> "Bastion";
                case "desert_pyramid" -> "Desert Pyramid";
                case "endcity" -> "End City";
                case "village" -> "Village";
                case "fortress" -> "Fortress";
                case "mansion" -> "Mansion";
                case "jungle_pyramid" -> "Jungle Pyramid";
                default -> CasingUtils.sentence(key);
            };
        }
    }
    private static class BiomeEnderLocatorTradeFactory extends EnderLocatorTradeFactory implements TradeOffers.Factory {
        @NotNull
        private final RegistryKey<Biome> biome;
        
        public BiomeEnderLocatorTradeFactory(@NotNull RegistryKey<Biome> biome, int minimumEmeralds, int pearlUses) {
            super(minimumEmeralds, pearlUses);
            this.biome = biome;
        }
        
        @Override
        protected @NotNull ItemStack createPearl() {
            Identifier biomeId = this.biome.getValue();
            ItemStack pearl = super.createPearl(this.description(biomeId));
            NbtCompound throwDat = pearl.getOrCreateSubNbt("throw");
            throwDat.putString("biome", biomeId.toString());
            throwDat.putInt("uses", this.uses);
            
            return pearl;
        }
        
        @Override
        protected @NotNull Text itemName(int strength) {
            return switch (strength) {
                case 0 -> new LiteralText("Weak Biome Locator");
                case 1 -> new LiteralText("Normal Biome Locator");
                default -> new LiteralText("Strong Biome Locator");
            };
        }
        
        protected @NotNull MutableText description(@Nullable Identifier identifier) {
            if (identifier == null)
                return new LiteralText("Unknown");
            String key = identifier.toString().replace(":", ".");
            return new TranslatableText("biome." + key);
        }
    }
}
