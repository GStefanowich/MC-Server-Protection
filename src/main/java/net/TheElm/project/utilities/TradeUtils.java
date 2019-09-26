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

import net.minecraft.item.Item;
import net.minecraft.village.TradeOffers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class TradeUtils {
    
    public static TradeOffers.Factory createSellItem(Item item, int stackSize, int int_2, int int_3, int int_4) {
        Constructor<?> sellItem = TradeUtils.getTradeConstructor();
        try {
            return (TradeOffers.Factory) sellItem.newInstance( item, stackSize, int_2, int_3, int_4 );
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException( e );
        }
    }
    
    private static Class<?> getTradeClass() {
        try {
            return Class.forName("net.minecraft.village.TradeOffers$SellItemFactory");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException( e );
        }
    }
    
    private static Constructor<?> getTradeConstructor() {
        Class<?> sellClass = TradeUtils.getTradeClass();
        try {
            return sellClass.getConstructor( Item.class, int.class, int.class, int.class, int.class );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException( e );
        }
    }
    
}
