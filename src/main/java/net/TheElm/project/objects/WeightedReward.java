package net.TheElm.project.objects;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class WeightedReward {
    
    private final int weight;
    private final Item reward;
    private final Consumer<ItemStack> consumer;
    
    public WeightedReward(@NotNull int weight, @NotNull Item reward ) {
        this(weight, reward, null);
    }
    public WeightedReward(@NotNull int weight, @NotNull Item reward, @Nullable Consumer<ItemStack> consumer ) {
        this.weight = weight;
        this.reward = reward;
        this.consumer = consumer;
    }
    
    public int getWeight() {
        return this.weight;
    }
    
    public ItemStack createItem() {
        ItemStack stack = new ItemStack(this.reward);
        if (this.consumer != null)
            this.consumer.accept(stack);
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
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(this.createItem());
            if (enchantments.size() > 0) {
                Collection<String> list = new ArrayList<>();
                for (Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
                    list.add(enchantment.getKey().getName(enchantment.getValue()).asString() + " " + enchantment.getValue());
                }
                out += " (" + String.join(", ", list) + ")";
            }
        }
        return out;
    }
    
}
