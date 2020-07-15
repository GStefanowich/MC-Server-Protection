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

import net.TheElm.project.enums.ClaimPermissions;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LeadItem;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.WritableBookItem;
import net.minecraft.item.WrittenBookItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemUtils {
    private ItemUtils() {}
    
    public static @Nullable ClaimPermissions getPermission(@NotNull ItemStack stack) {
        if (stack.isEmpty())
            return null;
        return ItemUtils.getPermission(stack.getItem());
    }
    public static @NotNull ClaimPermissions getPermission(@NotNull Item item) {
        if (item instanceof BlockItem)
            return ClaimPermissions.BLOCKS;
        if (item instanceof ToolItem || item instanceof BucketItem)
            return ClaimPermissions.BLOCKS;
        if (item instanceof EndCrystalItem)
            return ClaimPermissions.BLOCKS;
        if (item instanceof MusicDiscItem)
            return ClaimPermissions.STORAGE;
        if (item instanceof WritableBookItem || item instanceof WrittenBookItem)
            return ClaimPermissions.STORAGE;
        if (item instanceof EnderEyeItem)
            return ClaimPermissions.STORAGE;
        if (item instanceof LeadItem)
            return ClaimPermissions.CREATURES;
        return ClaimPermissions.BLOCKS;
    }
}
