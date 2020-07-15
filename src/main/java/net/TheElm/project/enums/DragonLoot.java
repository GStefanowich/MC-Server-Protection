package net.TheElm.project.enums;

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.objects.WeightedReward;
import net.TheElm.project.utilities.IntUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public final class DragonLoot {
    private DragonLoot() {}
    
    private static final Random RANDOM = new Random();
    private static final List<WeightedReward> LOOT_REWARDS = new ArrayList<>();
    
    private static void initRewards() {
        // Clear rewards before adding new ones
        DragonLoot.LOOT_REWARDS.clear();
        
        if (SewingMachineConfig.INSTANCE.DRAGON_LOOT_END_ITEMS.get()) {
            DragonLoot.itemReward(2800, Items.ELYTRA);
            DragonLoot.itemReward(250, Items.ELYTRA, (item) -> {
                CompoundTag nbt = item.getOrCreateTag();
                nbt.putBoolean("Unbreakable", true);
            });
            DragonLoot.itemReward(6830/*2700*/, Items.DRAGON_EGG);
            DragonLoot.itemReward(2300, Items.WITHER_SKELETON_SKULL, (item) -> item.setCount(IntUtils.random(RANDOM, 3, 6)));
            DragonLoot.itemReward(1800, Items.ENCHANTED_GOLDEN_APPLE);
            DragonLoot.itemReward(750, Items.DIAMOND_BLOCK);
            DragonLoot.itemReward(1400, Items.EMERALD_BLOCK);
            DragonLoot.itemReward(1400, Items.EXPERIENCE_BOTTLE, (item) -> item.setCount(IntUtils.random(RANDOM, 1, 15)));
        }
        
        if (SewingMachineConfig.INSTANCE.DRAGON_LOOT_RARE_BOOKS.get()) {
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
            
            DragonLoot.bookReward(300, Enchantments.PROJECTILE_PROTECTION, 6);
            DragonLoot.bookReward(440, Enchantments.PROJECTILE_PROTECTION, 5);
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
        
        // TODO: Add a Callable for config reloads
    }
    
    private static @NotNull WeightedReward itemReward(int weight, Item item) {
        WeightedReward reward = new WeightedReward(weight, item);
        DragonLoot.LOOT_REWARDS.add(reward);
        return reward;
    }
    private static @NotNull WeightedReward itemReward(int weight, Item item, Consumer<ItemStack> consumer) {
        WeightedReward reward = new WeightedReward(weight, item, consumer);
        DragonLoot.LOOT_REWARDS.add(reward);
        return reward;
    }
    private static @NotNull WeightedReward bookReward(int weight, Enchantment enchantment, final int level) {
        final Identifier enchantmentId = Registry.ENCHANTMENT.getId(enchantment);
        return DragonLoot.itemReward(weight, Items.ENCHANTED_BOOK, (book) -> {
            CompoundTag nbt = book.getOrCreateTag();
            ListTag enchantments = new ListTag();
            CompoundTag enchantmentTag = new CompoundTag();
            
            enchantments.add(enchantmentTag);
            nbt.put("StoredEnchantments", enchantments);
            
            enchantmentTag.putString("id", enchantmentId.toString());
            enchantmentTag.putInt("lvl", level);
        });
    }
    
    public static @Nullable ItemStack createReward() {
        WeightedReward reward = DragonLoot.getReward();
        if (reward == null)
            return null;
        return reward.createItem();
    }
    public static @Nullable WeightedReward getReward() {
        int random = DragonLoot.getRandomNumber();
        if (random < 0)
            return null;
        return LOOT_REWARDS.get(random);
    }
    private static int getRandomNumber() {
        double totalWeight = 0.0D;
        for (WeightedReward reward : LOOT_REWARDS)
            totalWeight += reward.getWeight();
        
        double random = Math.random() * totalWeight;
        for (int i = 0; i < LOOT_REWARDS.size(); i++) {
            random -= LOOT_REWARDS.get(i).getWeight();
            if (random <= 0D)
                return i;
        }
        
        return -1;
    }
        
    static {
        DragonLoot.initRewards();
        SewingMachineConfig.INSTANCE.afterReload(DragonLoot::initRewards);
    }
}
