package net.theelm.sewingmachine.objects;

import net.theelm.sewingmachine.CoreMod;
import net.minecraft.item.Item;
import net.minecraft.stat.StatType;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

public final class ShopStats {
    public static final StatType<Item> SHOP_TYPE_BOUGHT = registerItem("bought");
    public static final StatType<Item> SHOP_TYPE_SOLD = registerItem("sold");
    //public static final StatType<Identifier> SHOP_TYPE_MONEY = Registry.register(Registry.STAT_TYPE, "sew:money", new StatType<>(Registry.CUSTOM_STAT));
    //public static final Identifier SHOP_MONEY_EARNED = ShopStats.registerWith(ShopStats.SHOP_TYPE_MONEY, "sew:earned", StatFormatter.DEFAULT);
    //public static final Identifier SHOP_MONEY_SPENT = ShopStats.registerWith(ShopStats.SHOP_TYPE_MONEY, "sew:spent", StatFormatter.DEFAULT);
    
    private static @NotNull StatType<Item> registerItem(@NotNull String key) {
        return Registry.register(Registry.STAT_TYPE, CoreMod.modIdentifier(key), new StatType<>(Registry.ITEM));
    }
    
    public static void init() {}
}
