package net.TheElm.project.enums;

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.objects.rewards.WeightedReward;
import net.TheElm.project.objects.rewards.WeightedRewardEnchantedBook;
import net.TheElm.project.objects.rewards.WeightedRewardGenerator;
import net.TheElm.project.objects.rewards.WeightedRewardItem;
import net.TheElm.project.utilities.IntUtils;
import net.TheElm.project.utilities.ItemUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DragonLoot {
    private DragonLoot() {}
    
    private static final Random RANDOM = new Random();
    private static final List<WeightedReward> LOOT_REWARDS = new ArrayList<>();
    
    private static void initRewards() {
        // Clear rewards before adding new ones
        DragonLoot.LOOT_REWARDS.clear();
        
        if (SewConfig.get(SewConfig.DRAGON_LOOT_END_ITEMS)) {
            DragonLoot.itemReward(2800, Items.ELYTRA);
            DragonLoot.itemReward(250, Items.ELYTRA, (player, stack) -> ItemUtils.makeUnbreakable(stack));
            DragonLoot.itemReward(250, Items.FISHING_ROD, (player, stack) -> ItemUtils.makeUnbreakable(stack));
            DragonLoot.itemReward(5430, "Egg chance", (player) -> {
                Random random = player.getRandom();
                ItemStack stack;
                if (random.nextBoolean())
                    stack = new ItemStack(Items.DRAGON_EGG);
                else {
                    stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
                    stack.setCount(IntUtils.random(random, 1, 32));
                }
                return stack;
            });
            DragonLoot.itemReward(1400, "Head chance", (player) -> {
                Random random = player.getRandom();
                return new ItemStack(random.nextBoolean() ? Items.DRAGON_EGG : Items.DRAGON_HEAD);
            });
            /*DragonLoot.itemReward(1400, Items.PLAYER_HEAD, (player, stack) -> {
                if (player != null) {
                    // Assign the SkullOwner tag
                    NbtCompound skullOwner = stack.getOrCreateNbt();
                    skullOwner.putString("SkullOwner", player.getGameProfile().getName());
                }
            });*/
            DragonLoot.itemReward(2300, Items.WITHER_SKELETON_SKULL, (player, stack) -> stack.setCount(IntUtils.random(DragonLoot.RANDOM, 3, 6)));
            DragonLoot.itemReward(1800, Items.ENCHANTED_GOLDEN_APPLE);
            DragonLoot.itemReward(350, Items.NETHERITE_SCRAP, (player, stack) -> stack.setCount(IntUtils.random(DragonLoot.RANDOM, 1, 8)));
            DragonLoot.itemReward(2000, Items.SHULKER_SHELL, (player, stack) -> stack.setCount(IntUtils.random(DragonLoot.RANDOM, 1, 3)));
            DragonLoot.itemReward(750, Items.DIAMOND_BLOCK);
            DragonLoot.itemReward(1400, Items.EMERALD_BLOCK);
            DragonLoot.itemReward(1400, Items.EXPERIENCE_BOTTLE, (player, stack) -> stack.setCount(IntUtils.random(DragonLoot.RANDOM, 10, 32)));
        }
        
        if (SewConfig.get(SewConfig.DRAGON_LOOT_RARE_BOOKS)) {
            DragonLoot.bookReward(440, Enchantments.SHARPNESS, 6);
            DragonLoot.bookReward(300, Enchantments.SHARPNESS, 7);
            DragonLoot.bookReward(150, Enchantments.SHARPNESS, 8);
            DragonLoot.bookReward(70, Enchantments.SHARPNESS, 9);
            DragonLoot.bookReward(30, Enchantments.SHARPNESS, 10);
            
            DragonLoot.bookReward(440, Enchantments.POWER, 6);
            DragonLoot.bookReward(300, Enchantments.POWER, 7);
            DragonLoot.bookReward(150, Enchantments.POWER, 8);
            DragonLoot.bookReward(70, Enchantments.POWER, 9);
            DragonLoot.bookReward(30, Enchantments.POWER, 10);
            
            DragonLoot.bookReward(440, Enchantments.EFFICIENCY, 6);
            DragonLoot.bookReward(300, Enchantments.EFFICIENCY, 7);
            DragonLoot.bookReward(150, Enchantments.EFFICIENCY, 8);
            DragonLoot.bookReward(70, Enchantments.EFFICIENCY, 9);
            DragonLoot.bookReward(30, Enchantments.EFFICIENCY, 10);
            
            DragonLoot.bookReward(220, Enchantments.PROTECTION, 5);
            DragonLoot.bookReward(150, Enchantments.PROTECTION, 6);
            DragonLoot.bookReward(75, Enchantments.PROTECTION, 7);
            DragonLoot.bookReward(35, Enchantments.PROTECTION, 8);
            DragonLoot.bookReward(15, Enchantments.PROTECTION, 9);
            DragonLoot.bookReward(5, Enchantments.PROTECTION, 10);
            
            DragonLoot.bookReward(440, Enchantments.FIRE_PROTECTION, 5);
            DragonLoot.bookReward(300, Enchantments.FIRE_PROTECTION, 6);
            DragonLoot.bookReward(150, Enchantments.FIRE_PROTECTION, 7);
            DragonLoot.bookReward(70, Enchantments.FIRE_PROTECTION, 8);
            DragonLoot.bookReward(30, Enchantments.FIRE_PROTECTION, 9);
            DragonLoot.bookReward(10, Enchantments.FIRE_PROTECTION, 10);
            
            DragonLoot.bookReward(440, Enchantments.PROJECTILE_PROTECTION, 5);
            DragonLoot.bookReward(300, Enchantments.PROJECTILE_PROTECTION, 6);
            DragonLoot.bookReward(150, Enchantments.PROJECTILE_PROTECTION, 7);
            DragonLoot.bookReward(70, Enchantments.PROJECTILE_PROTECTION, 8);
            DragonLoot.bookReward(30, Enchantments.PROJECTILE_PROTECTION, 9);
            DragonLoot.bookReward(10, Enchantments.PROJECTILE_PROTECTION, 10);
            
            DragonLoot.bookReward(440, Enchantments.FEATHER_FALLING, 5);
            DragonLoot.bookReward(300, Enchantments.FEATHER_FALLING, 6);
            DragonLoot.bookReward(150, Enchantments.FEATHER_FALLING, 7);
            DragonLoot.bookReward(70, Enchantments.FEATHER_FALLING, 8);
            DragonLoot.bookReward(30, Enchantments.FEATHER_FALLING, 9);
            DragonLoot.bookReward(10, Enchantments.FEATHER_FALLING, 10);
            
            DragonLoot.bookReward(436, Enchantments.RESPIRATION, 4);
            DragonLoot.bookReward(300, Enchantments.RESPIRATION, 5);
            DragonLoot.bookReward(150, Enchantments.RESPIRATION, 6);
            DragonLoot.bookReward(70, Enchantments.RESPIRATION, 7);
            DragonLoot.bookReward(30, Enchantments.RESPIRATION, 8);
            DragonLoot.bookReward(10, Enchantments.RESPIRATION, 9);
            DragonLoot.bookReward(4, Enchantments.RESPIRATION, 10);
            
            DragonLoot.bookReward(436, Enchantments.DEPTH_STRIDER, 4);
            DragonLoot.bookReward(300, Enchantments.DEPTH_STRIDER, 5);
            DragonLoot.bookReward(150, Enchantments.DEPTH_STRIDER, 6);
            DragonLoot.bookReward(70, Enchantments.DEPTH_STRIDER, 7);
            DragonLoot.bookReward(30, Enchantments.DEPTH_STRIDER, 8);
            DragonLoot.bookReward(10, Enchantments.DEPTH_STRIDER, 9);
            DragonLoot.bookReward(4, Enchantments.DEPTH_STRIDER, 10);
            
            DragonLoot.bookReward(436, Enchantments.UNBREAKING, 4);
            DragonLoot.bookReward(300, Enchantments.UNBREAKING, 5);
            DragonLoot.bookReward(150, Enchantments.UNBREAKING, 6);
            DragonLoot.bookReward(70, Enchantments.UNBREAKING, 7);
            DragonLoot.bookReward(30, Enchantments.UNBREAKING, 8);
            DragonLoot.bookReward(10, Enchantments.UNBREAKING, 9);
            DragonLoot.bookReward(4, Enchantments.UNBREAKING, 10);
        }
    }
    
    private static @NotNull <T extends WeightedReward> T reward(T reward) {
        DragonLoot.LOOT_REWARDS.add(reward);
        return reward;
    }
    private static @NotNull WeightedReward itemReward(int weight, Item item) {
        return DragonLoot.reward(new WeightedRewardItem(weight, item));
    }
    private static @NotNull WeightedReward itemReward(int weight, Item item, @NotNull BiConsumer<PlayerEntity, ItemStack> consumer) {
        return DragonLoot.reward(new WeightedRewardItem(weight, item, consumer));
    }
    private static @NotNull WeightedReward itemReward(int weight, @NotNull String name, @NotNull Function<PlayerEntity, ItemStack> consumer) {
        return DragonLoot.reward(new WeightedRewardGenerator(weight, name, consumer));
    }
    private static @NotNull WeightedReward bookReward(int weight, @NotNull Enchantment enchantment, final int level) {
        return DragonLoot.reward(new WeightedRewardEnchantedBook(weight, enchantment, level));
    }
    
    public static @Nullable WeightedReward getReward() {
        int random = DragonLoot.getRandomNumber();
        if (random < 0)
            return null;
        return DragonLoot.LOOT_REWARDS.get(random);
    }
    private static int getRandomNumber() {
        double totalWeight = 0.0D;
        for (WeightedReward reward : DragonLoot.LOOT_REWARDS)
            totalWeight += reward.getWeight();
        
        double random = Math.random() * totalWeight;
        for (int i = 0; i < DragonLoot.LOOT_REWARDS.size(); i++) {
            random -= DragonLoot.LOOT_REWARDS.get(i).getWeight();
            if (random <= 0D)
                return i;
        }
        
        return -1;
    }
    
    static {
        DragonLoot.initRewards();
        SewConfig.afterReload(DragonLoot::initRewards);
    }
}
