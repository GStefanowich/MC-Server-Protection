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

package net.TheElm.project.mixins.Server;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.item.WritableBookItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.charset.StandardCharsets;

@Mixin(WritableBookItem.class)
public class Books {
    
    /**
     * @author TheElm
     * @reason To fix the chunk saving bug
     * @param compound The tag to verify if the book is valid
     * @return Returns if the book is valid
     */
    @Overwrite
    public static boolean isValid(@Nullable CompoundTag compound) {
        if (compound == null)
            return false;
        if (!compound.containsKey("pages", NbtType.LIST))
            return false;
        
        int bytes = 0;
        ListTag pages = compound.getList("pages", NbtType.STRING);
        
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
