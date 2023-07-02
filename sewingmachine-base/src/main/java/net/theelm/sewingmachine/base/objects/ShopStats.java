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

package net.theelm.sewingmachine.base.objects;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.theelm.sewingmachine.base.CoreMod;
import net.minecraft.item.Item;
import net.minecraft.stat.StatType;
import net.theelm.sewingmachine.utilities.Sew;
import org.jetbrains.annotations.NotNull;

public final class ShopStats {
    public static final StatType<Item> SHOP_TYPE_BOUGHT = registerItem("bought");
    public static final StatType<Item> SHOP_TYPE_SOLD = registerItem("sold");
    //public static final StatType<Identifier> SHOP_TYPE_MONEY = Registry.register(Registry.STAT_TYPE, "sew:money", new StatType<>(Registry.CUSTOM_STAT));
    //public static final Identifier SHOP_MONEY_EARNED = ShopStats.registerWith(ShopStats.SHOP_TYPE_MONEY, "sew:earned", StatFormatter.DEFAULT);
    //public static final Identifier SHOP_MONEY_SPENT = ShopStats.registerWith(ShopStats.SHOP_TYPE_MONEY, "sew:spent", StatFormatter.DEFAULT);
    
    private static @NotNull StatType<Item> registerItem(@NotNull String key) {
        return Registry.register(Registries.STAT_TYPE, Sew.modIdentifier(key), new StatType<>(Registries.ITEM));
    }
    
    public static void init() {}
}
