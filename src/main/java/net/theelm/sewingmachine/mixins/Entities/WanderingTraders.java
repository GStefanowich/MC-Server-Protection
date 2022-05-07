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

package net.theelm.sewingmachine.mixins.Entities;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.objects.WanderingTraderProfileCollection;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.IntUtils;
import net.theelm.sewingmachine.utilities.TradeUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradeOffers;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.feature.StructureFeature;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraders extends MerchantEntity {
    
    public WanderingTraders(EntityType<? extends MerchantEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Shadow
    private native int getDespawnDelay();
    
    /*
     * Created customized traders for the trader
     */
    @Inject(at = @At("HEAD"), method = "fillRecipes", cancellable = true)
    protected void fillRecipes(@NotNull CallbackInfo callback) {
        // If replacing wandering trader trades with better items is enabled
        if (SewConfig.isTrue(SewConfig.IMPROVED_WANDERING_TRADER)) {
            TradeOffers.Factory[] mainFactory = WANDERING_TRADER_TRADES.get(1);
            TradeOffers.Factory[] rareFactory = WANDERING_TRADER_TRADES.get(2);
            if (mainFactory != null && rareFactory != null) {
                // Fill trades from the mainFactory
                TradeOfferList tradeOffers = this.getOffers();
                this.fillRecipesFromPool(tradeOffers, mainFactory, IntUtils.random(this.random, 4, 12));
        
                // Get one random trade from the rare factory
                int randomTrade = this.random.nextInt(rareFactory.length);
                TradeOffers.Factory factory = rareFactory[randomTrade];
        
                // Create a trade using the factory
                TradeOffer tradeOffer = factory.create(this, this.random);
                if (tradeOffer != null)
                    tradeOffers.add(tradeOffer);
            }
            callback.cancel();
        }
    }
    
    /*
     * Tick when the wandering trader will despawn
     */
    @Inject(at = @At("RETURN"), method = "tickDespawnDelay")
    private void onTickDespawning(CallbackInfo callback) {
        boolean sendPacket = false;
        int despawn = this.getDespawnDelay();
        if (despawn >= 0) {
            // If despawn time remaining is over a minute, run every 1m
            if (despawn >= 1200 && despawn % 1200 == 0) {
                // If this entity is THE wandering trader
                if ((sendPacket = EntityUtils.isEntityWanderingTrader(this)) && despawn % 12000 == 0)
                    EntityUtils.wanderingTraderTimeRemaining((WanderingTraderEntity) (Entity) this);
            }
            // If despawn time remaining is less than a minute, run every 2s
            else if (despawn < 1200 && despawn % 20 == 0) {
                // If this entity is THE wandering trader
                sendPacket = EntityUtils.isEntityWanderingTrader(this);
            }
        }
        
        if (sendPacket) {
            // Update the player list time
            MinecraftServer server = this.getServer();
            if (server != null) {
                server.getPlayerManager()
                    .sendToAll(new WanderingTraderProfileCollection(this).getPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME));
            }
        }
    }
    
    /*
     * Change the wander target of the Trader to be their home
     */
    @Inject(at = @At("HEAD"), method = "getWanderTarget", cancellable = true)
    protected void onGetHomePosition(@NotNull CallbackInfoReturnable<BlockPos> callback) {
        if (SewConfig.get(SewConfig.WANDERING_TRADER_FORCE_SPAWN) && SewConfig.equals(SewConfig.WANDERING_TRADER_FORCE_SPAWN_WORLD, this.world.getRegistryKey()) && EntityUtils.isEntityWanderingTrader(this))
            callback.setReturnValue(SewConfig.get(SewConfig.WANDERING_TRADER_FORCE_SPAWN_POS));
    }
    
    @Override
    public void remove(RemovalReason removalReason) {
        if (!this.world.isClient && EntityUtils.isEntityWanderingTrader(this)) {
            // Announce the removal of the wandering trader
            if (SewConfig.get(SewConfig.ANNOUNCE_WANDERING_TRADER))
                EntityUtils.wanderingTraderDeparture((WanderingTraderEntity) (Entity) this);
            
            // Remove wandering trader from player list
            MinecraftServer server = this.getServer();
            if (server != null) {
                server.getPlayerManager()
                    .sendToAll(new WanderingTraderProfileCollection().getPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER));
            }
        }
        super.remove(removalReason);
    }
    
    @Contract("_ -> new")
    private static @NotNull Int2ObjectMap<TradeOffers.Factory[]> copyToFastUtilMap(ImmutableMap<Integer, TradeOffers.Factory[]> immutableMap_1) {
        return new Int2ObjectOpenHashMap<>(immutableMap_1);
    }
    
    private static final Int2ObjectMap<TradeOffers.Factory[]> WANDERING_TRADER_TRADES = copyToFastUtilMap(ImmutableMap.of(
        1, new TradeOffers.Factory[] {
            TradeUtils.createSellItem(1, 1, Items.FERN, 12),
            TradeUtils.createSellItem(1, 1, Items.SUGAR_CANE, 8),
            TradeUtils.createSellItem(1, 1, Items.DANDELION, 12),
            TradeUtils.createSellItem(1, 1, Items.POPPY, 12),
            TradeUtils.createSellItem(1, 1, Items.BLUE_ORCHID, 8),
            TradeUtils.createSellItem(1, 1, Items.ALLIUM, 12),
            TradeUtils.createSellItem(1, 1, Items.AZURE_BLUET, 12),
            TradeUtils.createSellItem(1, 1, Items.RED_TULIP, 12),
            TradeUtils.createSellItem(1, 1, Items.ORANGE_TULIP, 12),
            TradeUtils.createSellItem(1, 1, Items.WHITE_TULIP, 12),
            TradeUtils.createSellItem(1, 1, Items.PINK_TULIP, 12),
            TradeUtils.createSellItem(1, 1, Items.OXEYE_DAISY, 12),
            TradeUtils.createSellItem(1, 1, Items.CORNFLOWER, 12),
            TradeUtils.createSellItem(1, 1, Items.LILY_OF_THE_VALLEY, 7),
            TradeUtils.createSellItem(1, 1, Items.WHEAT_SEEDS, 12),
            TradeUtils.createSellItem(1, 1, Items.BEETROOT_SEEDS, 12),
            TradeUtils.createSellItem(1, 1, Items.PUMPKIN_SEEDS, 12),
            TradeUtils.createSellItem(1, 1, Items.MELON_SEEDS, 12),
            TradeUtils.createSellItem(1, 1, Items.VINE, 12),
            TradeUtils.createSellItem(1, 1, Items.BROWN_MUSHROOM, 12),
            TradeUtils.createSellItem(1, 1, Items.RED_MUSHROOM, 12),
            TradeUtils.createSellItem(1, 2, Items.LILY_PAD, 5),
            TradeUtils.createSellItem(1, 8, Items.SAND, 8),
            TradeUtils.createSellItem(1, 4, Items.RED_SAND, 6),
            /* 2 Emerald Trades */
            TradeUtils.createSellItem(2, 1, Items.SEA_PICKLE, 5),
            TradeUtils.createSellItem(2, 1, Items.GLOWSTONE, 5),
            /* 3 Emerald Trades */
            TradeUtils.createSellItem(3, 1, Items.BRAIN_CORAL_BLOCK, 8),
            TradeUtils.createSellItem(3, 1, Items.BUBBLE_CORAL_BLOCK, 8),
            TradeUtils.createSellItem(3, 1, Items.FIRE_CORAL_BLOCK, 8),
            TradeUtils.createSellItem(3, 1, Items.HORN_CORAL_BLOCK, 8),
            TradeUtils.createSellItem(3, 1, Items.TUBE_CORAL_BLOCK, 8),
            TradeUtils.createSellItem(3, 1, Items.KELP, 12),
            TradeUtils.createSellItem(3, 1, Items.CACTUS, 8),
            /* 4 Emerald Trades */
            TradeUtils.createSellItem(4, 1, Items.SLIME_BALL, 5),
            /* 5 Emerald Trades */
            TradeUtils.createSellItem(5, 1, Items.ACACIA_SAPLING, 8),
            TradeUtils.createSellItem(5, 1, Items.BIRCH_SAPLING, 8),
            TradeUtils.createSellItem(5, 1, Items.DARK_OAK_SAPLING, 8),
            TradeUtils.createSellItem(5, 1, Items.JUNGLE_SAPLING, 8),
            TradeUtils.createSellItem(5, 1, Items.OAK_SAPLING, 8),
            TradeUtils.createSellItem(5, 1, Items.SPRUCE_SAPLING, 8),
            TradeUtils.createSellItem(5, 1, Items.NAUTILUS_SHELL, 5),
            /* Spawn Egg Trades */
            TradeUtils.createSellSpawnEgg(32, Items.BEE_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.MOOSHROOM_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.COW_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.SHEEP_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.PIG_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.CHICKEN_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.HORSE_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.MULE_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.DONKEY_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.LLAMA_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.CAT_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.WOLF_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.TURTLE_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.FOX_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.PANDA_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.PARROT_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.POLAR_BEAR_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.AXOLOTL_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.GOAT_SPAWN_EGG, 5),
            TradeUtils.createSellSpawnEgg(32, Items.GLOW_SQUID_SPAWN_EGG, 5),
            /* Music Discs */
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_11, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_13, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_BLOCKS, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_CAT, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_CHIRP, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_FAR, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_MALL, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_MELLOHI, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_STAL, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_STRAD, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_WAIT, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_WARD, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_PIGSTEP, 1),
            TradeUtils.createSellItem(16, 1, Items.MUSIC_DISC_OTHERSIDE, 1),
            
            TradeUtils.createSellItem(14, 1, Items.NAME_TAG, 5),
            TradeUtils.createSellItem(2, 3, Items.LEAD, 5),
            
            TradeUtils.createSellItem(1, 3, Items.ENDER_PEARL, 4),
            TradeUtils.createSellItem(1, 2, Items.BLAZE_ROD, 5),
            TradeUtils.createSellItem(5, 1, Items.RABBIT_FOOT, 6),
            TradeUtils.createSellItem(12, 1, Items.BELL, 1),
            TradeUtils.createSellItem(6, 1, Items.SADDLE, 3),
            TradeUtils.createSellItem(8, 3, Items.NETHER_WART, 5),
            TradeUtils.createSellItem(14, 1, Items.WITHER_ROSE, 3),
            TradeUtils.createSellItem(5, 1, Items.DRAGON_BREATH, 3),
            
            TradeUtils.createSellItem(1, 1, Items.CAKE, 3),
            TradeUtils.createSellItem(1, 3, Items.COOKED_BEEF, 12),
            TradeUtils.createSellItem(1, 3, Items.COOKED_CHICKEN, 12),
            TradeUtils.createSellItem(1, 8, Items.COOKIE, 8),
            
            TradeUtils.createSellItem(8, 1, Items.CREEPER_BANNER_PATTERN, 1),
            TradeUtils.createSellItem(8, 1, Items.FLOWER_BANNER_PATTERN, 1),
            TradeUtils.createSellItem(8, 1, Items.GLOBE_BANNER_PATTERN, 1),
            TradeUtils.createSellItem(8, 1, Items.MOJANG_BANNER_PATTERN, 1),
            TradeUtils.createSellItem(8, 1, Items.SKULL_BANNER_PATTERN, 1),
            TradeUtils.createSellItem(8, 1, Items.PIGLIN_BANNER_PATTERN, 1),
            
            TradeUtils.createSellItem(2, Items.EMERALD, 7, Items.LEATHER, 1, Items.LEATHER_HORSE_ARMOR, 3),
            TradeUtils.createSellItem(2, Items.EMERALD, 7, Items.IRON_INGOT, 1, Items.IRON_HORSE_ARMOR, 3),
            TradeUtils.createSellItem(2, Items.EMERALD, 7, Items.GOLD_INGOT, 1, Items.GOLDEN_HORSE_ARMOR, 3),
            TradeUtils.createSellItem(2, Items.EMERALD, 7, Items.DIAMOND, 1, Items.DIAMOND_HORSE_ARMOR, 3),
        },
        2, new TradeOffers.Factory[] {
            /* 1 Emerald Trades */
            TradeUtils.createSellItem(1, 1, Items.GUNPOWDER, 8),
            /* 3 Emerald Trades */
            TradeUtils.createSellItem(3, 1, Items.PACKED_ICE, 6),
            TradeUtils.createSellItem(3, 3, Items.PODZOL, 6),
            /* 5 Emerald Trades */
            TradeUtils.createSellItem(5, 1, Items.TROPICAL_FISH_BUCKET, 4),
            TradeUtils.createSellItem(5, 1, Items.PUFFERFISH_BUCKET, 4),
            /* 6 Emerald Trades */
            TradeUtils.createSellItem(6, 1, Items.BLUE_ICE, 6),
            /* Rare Items */
            TradeUtils.createSellItem(3, 1, Items.EXPERIENCE_BOTTLE, 32),
            TradeUtils.createSellItem(54, 2, Items.SHULKER_SHELL, 6),
            TradeUtils.createSellItem(9, Items.EMERALD_BLOCK, 1, Items.HEART_OF_THE_SEA, 1, Items.TRIDENT, 1),
            TradeUtils.createSellItem(1, Items.NETHER_STAR, 1, Items.TRIDENT, 1),
            TradeUtils.createSellItem(1, Items.NETHER_STAR, 1, Items.TOTEM_OF_UNDYING, 1),
            TradeUtils.createSellItem(1, Items.TOTEM_OF_UNDYING, 1, Items.NETHER_STAR, 1),
            TradeUtils.createSellItem(50, 1, Items.TOTEM_OF_UNDYING, 2),
            TradeUtils.createSellItem(16, Items.EMERALD, 1, Items.GOLDEN_APPLE, 1, Items.ENCHANTED_GOLDEN_APPLE, 1),
            /* Spawn more wandering traders */
            TradeUtils.createSellSpawnEgg(55, Items.WANDERING_TRADER_SPAWN_EGG, 5),
            /* Biome Locators */
            TradeUtils.createSellLocator(1, BiomeKeys.MUSHROOM_FIELDS, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.JUNGLE, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.BADLANDS, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.SWAMP, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.SUNFLOWER_PLAINS, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.FLOWER_FOREST, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.STONY_PEAKS, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.FROZEN_PEAKS, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.DARK_FOREST, 100),
            TradeUtils.createSellLocator(1, BiomeKeys.DESERT, 100),
            /* Structure Locators */
            TradeUtils.createSellLocator(1, StructureFeature.BASTION_REMNANT, 100),
            TradeUtils.createSellLocator(1, StructureFeature.DESERT_PYRAMID, 100),
            TradeUtils.createSellLocator(1, StructureFeature.ENDCITY, 100),
            TradeUtils.createSellLocator(1, StructureFeature.VILLAGE, 100),
            TradeUtils.createSellLocator(1, StructureFeature.FORTRESS, 100),
            TradeUtils.createSellLocator(1, StructureFeature.MANSION, 100),
            TradeUtils.createSellLocator(1, StructureFeature.JUNGLE_PYRAMID, 100),
        })
    );
    
}
