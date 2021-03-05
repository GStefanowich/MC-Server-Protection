package net.TheElm.project.objects;

import net.TheElm.project.CoreMod;
import net.minecraft.item.Item;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.StatType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

public final class ShopStats {
    public static final StatType<Item> SHOP_TYPE_BOUGHT = Registry.register(Registry.STAT_TYPE, CoreMod.modIdentifier("bought"), new StatType<>(Registry.ITEM));
    public static final StatType<Item> SHOP_TYPE_SOLD = Registry.register(Registry.STAT_TYPE, CoreMod.modIdentifier("sold"), new StatType<>(Registry.ITEM));
    //public static final StatType<Identifier> SHOP_TYPE_MONEY = Registry.register(Registry.STAT_TYPE, "sew:money", new StatType<>(Registry.CUSTOM_STAT));
    //public static final Identifier SHOP_MONEY_EARNED = ShopStats.registerWith(ShopStats.SHOP_TYPE_MONEY, "sew:earned", StatFormatter.DEFAULT);
    //public static final Identifier SHOP_MONEY_SPENT = ShopStats.registerWith(ShopStats.SHOP_TYPE_MONEY, "sew:spent", StatFormatter.DEFAULT);
    
    private static @NotNull Identifier registerWith(@NotNull StatType<Identifier> type, @NotNull String key, @NotNull StatFormatter formatter) {
        Identifier identifier = new Identifier(key);
        Registry.register(Registry.CUSTOM_STAT, key, identifier);
        type.getOrCreateStat(identifier, formatter);
        return identifier;
    }
}
