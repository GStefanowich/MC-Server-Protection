package net.TheElm.project.objects;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WeightedReward {
    
    private final int weight;
    private final @NotNull Item reward;
    private final @NotNull BiConsumer<PlayerEntity, ItemStack> consumer;
    
    public WeightedReward(@NotNull int weight, @NotNull Item reward ) {
        this(weight, reward, (p, s) -> {});
    }
    public WeightedReward(@NotNull int weight, @NotNull Item reward, @NotNull BiConsumer<PlayerEntity, ItemStack> consumer) {
        this.weight = weight;
        this.reward = reward;
        this.consumer = consumer;
    }
    
    public int getWeight() {
        return this.weight;
    }
    
    public ItemStack createItem(@Nullable PlayerEntity player) {
        ItemStack stack = new ItemStack(this.reward);
        this.consumer.accept(player, stack);
        return stack;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        return (object instanceof WeightedReward)
            && (((WeightedReward) object).weight == this.weight)
            && (((WeightedReward) object).reward == this.reward)
            && (((WeightedReward) object).consumer == this.consumer);
    }
    @Override
    public int hashCode() {
        return Objects.hash(
            this.reward,
            this.weight,
            this.consumer
        );
    }
    @Override
    public String toString() {
        String out = this.reward.toString();
        if (this.reward == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(this.createItem(null));
            if (enchantments.size() > 0) {
                Collection<String> list = new ArrayList<>();
                for (Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
                    list.add(enchantment.getKey().getName(enchantment.getValue()).getString() + " " + enchantment.getValue());
                }
                out += " (" + String.join(", ", list) + ")";
            }
        }
        return out;
    }
    
}
