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

import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.IntProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class CropUtils {
    
    private CropUtils() {}
    
    public static boolean isTree(@NotNull BlockState blockState) {
        Block block = blockState.getBlock();
        if (!(block instanceof PillarBlock))
            return false;
        Material material = blockState.getMaterial();
        return Material.WOOD.equals(material) || Material.NETHER_WOOD.equals(material);
    }
    
    public static boolean isCrop(@Nullable Block block) {
        return block instanceof CropBlock || block instanceof NetherWartBlock;
    }
    
    public static boolean isGourd(@Nullable Block block) {
        return block instanceof MelonBlock || block instanceof PumpkinBlock;
    }
    
    public static boolean isMature(@NotNull BlockState state) {
        IntProperty property = CropUtils.getAgeProperty(state.getBlock());
        if (property == null)
            return false;
        int stage = state.get(property); // Get the crop stage
        int max = Collections.max(property.getValues()); // Get the max age
        return stage >= max;
    }
    
    public static @NotNull BlockState withAge(@NotNull Block block, int age) {
        return block.getDefaultState()
            .with(CropUtils.getAgeProperty(block), age);
    }
    
    private static @Nullable IntProperty getAgeProperty(@Nullable Block block) {
        if (block instanceof NetherWartBlock)
            return NetherWartBlock.AGE;
        if (block instanceof CarrotsBlock)
            return CarrotsBlock.AGE;
        if (block instanceof PotatoesBlock)
            return PotatoesBlock.AGE;
        if (block instanceof BeetrootsBlock)
            return BeetrootsBlock.AGE;
        if (block instanceof CropBlock)
            return CropBlock.AGE;
        return null;
    }
    
    public static @Nullable Item getSeed(@Nullable Block crop) {
        if (crop instanceof NetherWartBlock)
            return Items.NETHER_WART;
        if (crop instanceof CarrotsBlock)
            return Items.CARROT;
        if (crop instanceof PotatoesBlock)
            return Items.POTATO;
        if (crop instanceof BeetrootsBlock)
            return Items.BEETROOT_SEEDS;
        if (crop instanceof CropBlock)
            return Items.WHEAT_SEEDS;
        if (crop instanceof StemBlock)
            return CropUtils.getSeed(((StemBlock) crop).getGourdBlock());
        if (crop instanceof MelonBlock)
            return Items.MELON_SEEDS;
        if (crop instanceof PumpkinBlock)
            return Items.PUMPKIN_SEEDS;
        return null;
    }
}
