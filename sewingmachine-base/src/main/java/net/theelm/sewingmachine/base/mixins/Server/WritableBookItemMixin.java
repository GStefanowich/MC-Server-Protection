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

package net.theelm.sewingmachine.base.mixins.Server;

import net.minecraft.item.WritableBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.charset.StandardCharsets;

@Mixin(WritableBookItem.class)
public class WritableBookItemMixin {
    /**
     * @author TheElm
     * @reason To fix the chunk saving bug
     * @param compound The tag to verify if the book is valid
     * @return Returns if the book is valid
     */
    @Overwrite
    public static boolean isValid(@Nullable NbtCompound compound) {
        if (compound == null)
            return false;
        if (!compound.contains("pages", NbtElement.LIST_TYPE))
            return false;
        
        int bytes = 0;
        NbtList pages = compound.getList("pages", NbtElement.STRING_TYPE);
        
        // Iterate pages
        for(int i = 0; i < pages.size(); ++i) {
            String page = pages.getString(i);
            
            // If the ByteSize has added up to exceeding the max book length
            if ((bytes += page.getBytes(StandardCharsets.UTF_8).length) > 12800)
                return false;
        }
        
        return true;
    }
}
