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

import net.minecraft.block.MaterialColor;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class ColorUtils {
    
    private static final Random RANDOM;
    private ColorUtils() {}
    
    public static MutableText format(Text text, Formatting... formatting) {
        if (text instanceof MutableText)
            return ((MutableText)text).formatted(formatting);
        return ColorUtils.format(new LiteralText("")
            .append(text), formatting);
    }
    public static MutableText format(Text text, TextColor color) {
        if (text instanceof MutableText)
            return ((MutableText)text).styled((style) -> style.withColor(color));
        return ColorUtils.format(new LiteralText("")
            .append(text), color);
    }
    
    private static Color materialToColor(MaterialColor material) {
        return new Color(material.color);
    }
    private static Color dyeToColor(DyeColor color) {
        float[] components = color.getColorComponents();
        return new Color(components[0], components[1], components[2]);
    }
    private static Color formatToColor(Formatting color) {
        return new Color(ColorUtils.formatToInt( color ));
    }
    private static int formatToInt(Formatting color) {
        switch ( color ) {
            case BLACK:
                return 0;
            case DARK_BLUE:
                return 170;
            case DARK_GREEN:
                return 43520;
            case DARK_AQUA:
                return 43690;
            case DARK_RED:
                return 11141120;
            case DARK_PURPLE:
                return 11141290;
            case GOLD:
                return 16755200;
            case GRAY:
                return 11184810;
            case DARK_GRAY:
                return 5592405;
            case BLUE:
                return 5592575;
            case GREEN:
                return 5635925;
            case AQUA:
                return 5636095;
            case RED:
                return 16733525;
            case LIGHT_PURPLE:
                return 16733695;
            case YELLOW:
                return 16777045;
            case WHITE:
                return 16777215;
            default:
                throw new IllegalArgumentException(( color.isColor() ? "Unhandled color " + color.name() : "Invalid formatting, " + color.name() + " is not a color." ));
        }
    }
    private static double colorDistance(Color one, Color two) {
        return Math.pow((one.getRed() - two.getRed()) * 0.30, 2)
            + Math.pow((one.getGreen() - two.getGreen()) * 0.59, 2)
            + Math.pow((one.getBlue() - two.getBlue()) * 0.11, 2);
    }
    
    public static DyeColor getNearestDye(Formatting color) {
        DyeColor dye = DyeColor.byName(color.getName(), null);
        if (dye != null)
            return dye;
        return ColorUtils.getNearestDye(ColorUtils.formatToColor( color ));
    }
    public static DyeColor getNearestDye(String hex) {
        return ColorUtils.getNearestDye(Color.decode( hex ));
    }
    public static DyeColor getNearestDye(float r, float g, float b) {
        return ColorUtils.getNearestDye(new Color(r, g, b));
    }
    public static DyeColor getNearestDye(Color colorA) {
        HashMap<DyeColor, Double> list = new HashMap<>();
        DyeColor nearest = null;
        
        for (DyeColor colorB : DyeColor.values()) {
            list.put( colorB,
                ColorUtils.colorDistance(
                    colorA,
                    ColorUtils.dyeToColor( colorB )
                )
            );
        }
        
        Double closeness = null;
        for (Map.Entry<DyeColor, Double> set : list.entrySet()) {
            if ((closeness == null) || (set.getValue() < closeness)) {
                nearest = set.getKey();
                closeness = set.getValue();
            }
        }
        
        return nearest;
    }
    
    public static TextColor getNearestTextColor(Formatting formatting) {
        return TextColor.fromFormatting(formatting);
    }
    public static TextColor getNearestTextColor(String hex) {
        return ColorUtils.getNearestTextColor(Color.decode(hex));
    }
    public static TextColor getNearestTextColor(float r, float g, float b) {
        return ColorUtils.getNearestTextColor(new Color(r, g, b));
    }
    public static TextColor getNearestTextColor(Color color) {
        return TextColor.fromRgb(color.getRGB());
    }
    
    public static Formatting getNearestFormatting(DyeColor color) {
        Formatting formatting = Formatting.byName(color.getName());
        if (formatting != null)
            return formatting;
        return ColorUtils.getNearestFormatting(ColorUtils.dyeToColor( color ));
    }
    public static Formatting getNearestFormatting(String hex) {
        return ColorUtils.getNearestFormatting(Color.decode( hex ));
    }
    public static Formatting getNearestFormatting(float r, float g, float b) {
        return ColorUtils.getNearestFormatting(new Color(r, g, b));
    }
    public static Formatting getNearestFormatting(Color color) {
        HashMap<Formatting, Double> list = new HashMap<>();
        Formatting nearest = null;
        
        for (Formatting format : Formatting.values()) {
            if (!format.isColor()) continue;
            list.put( format,
                ColorUtils.colorDistance(
                    color,
                    ColorUtils.formatToColor( format )
                )
            );
        }
        
        Double closeness = null;
        for (Map.Entry<Formatting, Double> set : list.entrySet()) {
            if ((closeness == null) || (set.getValue() < closeness)) {
                nearest = set.getKey();
                closeness = set.getValue();
            }
        }
        
        return nearest;
    }
    
    public static DyeColor getRandomDye() {
        DyeColor[] dyes = new DyeColor[]{
            DyeColor.WHITE,
            DyeColor.ORANGE,
            DyeColor.MAGENTA,
            DyeColor.LIGHT_BLUE,
            DyeColor.YELLOW,
            DyeColor.LIME,
            DyeColor.PINK,
            DyeColor.GRAY,
            DyeColor.LIGHT_GRAY,
            DyeColor.CYAN,
            DyeColor.PURPLE,
            DyeColor.BLUE,
            DyeColor.BROWN,
            DyeColor.GREEN,
            DyeColor.RED,
            DyeColor.BLACK
        };
        
        return dyes[RANDOM.nextInt( dyes.length )];
    }
    public static Formatting getRandomFormat() {
        Formatting[] array = new Formatting[]{
            Formatting.BLACK,
            Formatting.DARK_BLUE,
            Formatting.DARK_GREEN,
            Formatting.DARK_AQUA,
            Formatting.DARK_RED,
            Formatting.DARK_PURPLE,
            Formatting.GOLD,
            Formatting.GRAY,
            Formatting.DARK_GRAY,
            Formatting.BLUE,
            Formatting.GREEN,
            Formatting.AQUA,
            Formatting.RED,
            Formatting.LIGHT_PURPLE,
            Formatting.YELLOW,
            Formatting.WHITE,
        };
        
        return array[RANDOM.nextInt( array.length )];
    }
    
    static {
        RANDOM = new Random();
    }
}
