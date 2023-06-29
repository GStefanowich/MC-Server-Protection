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

package net.theelm.sewingmachine.base.objects.fireworks;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.random.Random;
import net.theelm.sewingmachine.utilities.CollectionUtils;
import net.theelm.sewingmachine.utilities.IntUtils;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created on Aug 25 2021 at 2:50 AM.
 * By greg in SewingMachineMod
 */
public final class FireworkSchemes {
    private FireworkSchemes() {}
    
    public static @NotNull Scheme USA = new FireworkSchemes.Builder()
        .withColors(DyeColor.BLUE, DyeColor.WHITE, DyeColor.RED)
        .withShapes(FireworkRocketItem.Type.LARGE_BALL, FireworkRocketItem.Type.SMALL_BALL, FireworkRocketItem.Type.STAR, FireworkRocketItem.Type.BURST)
        .build();
    public static @NotNull Scheme CHRISTMAS = new FireworkSchemes.Builder()
        .withColors(DyeColor.BLUE, DyeColor.LIGHT_BLUE, DyeColor.WHITE, DyeColor.RED, DyeColor.GREEN)
        .withShapes(FireworkRocketItem.Type.LARGE_BALL, FireworkRocketItem.Type.SMALL_BALL, FireworkRocketItem.Type.STAR, FireworkRocketItem.Type.BURST)
        .build();
    public static @NotNull Scheme ALL = new FireworkSchemes.Builder()
        .withColors(DyeColor.values())
        .withShapes(FireworkRocketItem.Type.values())
        .build();
    
    public static class Scheme {
        private final @NotNull List<DyeColor> colors;
        private final @NotNull List<FireworkRocketItem.Type> shapes;
        
        protected Scheme(@NotNull List<DyeColor> colors, @NotNull List<FireworkRocketItem.Type> shapes) {
            this.colors = colors;
            this.shapes = shapes;
        }
        
        public NbtCompound generate(@NotNull Random random) {
            NbtCompound main = new NbtCompound();
            
            List<Integer> burstColors = new ArrayList<>();
            List<Integer> fadeColors = new ArrayList<>();

            FireworkRocketItem.Type shape = CollectionUtils.getRandom(this.shapes, random);
            
            // Generate burst colors
            int max = IntUtils.random(random, 1, 4);
            for (int i = 0; i < max; i++)
                burstColors.add(this.getRandomColor(random));
            
            // Generate fade colors
            if (max > 0) {
                max = IntUtils.random(random, 1, max);
                for (int i = 0; i < max; i++)
                    fadeColors.add(this.getRandomColor(random));
            }
            
            main.putInt("Shape", shape.getId());
            main.putBoolean("Flicker", random.nextBoolean());
            main.putBoolean("Trail", random.nextBoolean());
            main.putIntArray("Colors", burstColors);
            main.putIntArray("FadeColors", fadeColors);
            
            return main;
        }
        
        private int getRandomColor(@NotNull Random random) {
            DyeColor color = CollectionUtils.getRandom(this.colors, random);
            return color.getFireworkColor();
        }
    }
    public static class Builder {
        private final @NotNull List<DyeColor> colors = new ArrayList<>();
        private final @NotNull List<FireworkRocketItem.Type> shapes = new ArrayList<>();
        
        public FireworkSchemes.Builder withColors(@NotNull DyeColor... formattings) {
            return this.withColors(Arrays.asList(formattings));
        }
        public FireworkSchemes.Builder withColors(@NotNull Collection<? extends DyeColor> formattings) {
            this.colors.addAll(formattings);
            return this;
        }
        public FireworkSchemes.Builder withShapes(@NotNull FireworkRocketItem.Type... types) {
            return this.withShapes(Arrays.asList(types));
        }
        public FireworkSchemes.Builder withShapes(@NotNull Collection<? extends FireworkRocketItem.Type> types) {
            this.shapes.addAll(types);
            return this;
        }
        public FireworkSchemes.Scheme build() {
            return new Scheme(
                ImmutableList.copyOf(this.colors),
                ImmutableList.copyOf(this.shapes)
            );
        }
    }
}
