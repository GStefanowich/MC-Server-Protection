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

import net.minecraft.block.BeetrootsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarrotsBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Material;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.PotatoesBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.IntProperty;

import java.util.Collections;

public final class CropUtils {
    
    private CropUtils() {}
    
    public static boolean isTree(BlockState blockState) {
        Block block = blockState.getBlock();
        if (!(block instanceof PillarBlock))
            return false;
        Material material = blockState.getMaterial();
        return Material.WOOD.equals(material) || Material.NETHER_WOOD.equals(material);
    }
    
    public static boolean isCrop(Block block) {
        return block instanceof CropBlock || block instanceof NetherWartBlock;
    }
    
    public static boolean isMature(BlockState state) {
        IntProperty property = CropUtils.getAgeProperty(state.getBlock());
        if (property == null)
            return false;
        int stage = state.get(property); // Get the crop stage
        int max = Collections.max(property.getValues()); // Get the max age
        return stage >= max;
    }
    
    public static BlockState withAge(Block block, int age) {
        return block.getDefaultState().with(CropUtils.getAgeProperty(block), age);
    }
    
    private static IntProperty getAgeProperty(Block block) {
        if ( block instanceof NetherWartBlock )
            return NetherWartBlock.AGE;
        if ( block instanceof CarrotsBlock)
            return CarrotsBlock.AGE;
        if ( block instanceof PotatoesBlock)
            return PotatoesBlock.AGE;
        if ( block instanceof BeetrootsBlock)
            return BeetrootsBlock.AGE;
        if ( block instanceof CropBlock )
            return CropBlock.AGE;
        return null;
    }
    
    public static Item getSeed(Block crop) {
        if ( crop instanceof NetherWartBlock )
            return Items.NETHER_WART;
        if ( crop instanceof CarrotsBlock)
            return Items.CARROT;
        if ( crop instanceof PotatoesBlock)
            return Items.POTATO;
        if ( crop instanceof BeetrootsBlock)
            return Items.BEETROOT_SEEDS;
        if ( crop instanceof CropBlock )
            return Items.WHEAT_SEEDS;
        return null;
    }
}
