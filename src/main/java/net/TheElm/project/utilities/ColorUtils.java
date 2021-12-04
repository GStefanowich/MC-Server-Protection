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

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Material;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ColorUtils {
    
    public static final @NotNull Map<String, Color> COLORS;
    public static final @NotNull String LEGACY_TAG = "§";
    
    private static final Random RANDOM;
    private ColorUtils() {}
    
    static {
        Map<String, Color> colors = new HashMap<>();
        colors.put("AliceBlue", new Color(240, 248, 255));
        colors.put("AntiqueWhite", new Color(250, 235, 215));
        colors.put("Aqua", new Color(0, 255, 255));
        colors.put("Aquamarine", new Color(127, 255, 212));
        colors.put("Azure", new Color(240, 255, 255));
        colors.put("Beige", new Color(245, 245, 220));
        colors.put("Bisque", new Color(255, 228, 196));
        colors.put("Black", new Color(0, 0, 0));
        colors.put("BlanchedAlmond", new Color(255, 235, 205));
        colors.put("Blue", new Color(0, 0, 255));
        colors.put("BlueViolet", new Color(138, 43, 226));
        colors.put("Brown", new Color(165, 42, 42));
        colors.put("BurlyWood", new Color(222, 184, 135));
        colors.put("CadetBlue", new Color(95, 158, 160));
        colors.put("Chartreuse", new Color(127, 255, 0));
        colors.put("Chocolate", new Color(210, 105, 30));
        colors.put("Coral", new Color(255, 127, 80));
        colors.put("CornflowerBlue", new Color(100, 149, 237));
        colors.put("Cornsilk", new Color(255, 248, 220));
        colors.put("Crimson", new Color(220, 20, 60));
        colors.put("Cyan", new Color(0, 255, 255));
        colors.put("DarkBlue", new Color(0, 0, 139));
        colors.put("DarkCyan", new Color(0, 139, 139));
        colors.put("DarkGoldenRod", new Color(184, 134, 11));
        colors.put("DarkGray", new Color(169, 169, 169));
        colors.put("DarkGrey", new Color(169, 169, 169));
        colors.put("DarkGreen", new Color(0, 100, 0));
        colors.put("DarkKhaki", new Color(189, 183, 107));
        colors.put("DarkMagenta", new Color(139, 0, 139));
        colors.put("DarkOliveGreen", new Color(85, 107, 47));
        colors.put("DarkOrange", new Color(255, 140, 0));
        colors.put("DarkOrchid", new Color(153, 50, 204));
        colors.put("DarkRed", new Color(139, 0, 0));
        colors.put("DarkSalmon", new Color(233, 150, 122));
        colors.put("DarkSeaGreen", new Color(143, 188, 143));
        colors.put("DarkSlateBlue", new Color(72, 61, 139));
        colors.put("DarkSlateGray", new Color(47, 79, 79));
        colors.put("DarkSlateGrey", new Color(47, 79, 79));
        colors.put("DarkTurquoise", new Color(0, 206, 209));
        colors.put("DarkViolet", new Color(148, 0, 211));
        colors.put("DeepPink", new Color(255, 20, 147));
        colors.put("DeepSkyBlue", new Color(0, 191, 255));
        colors.put("DimGray", new Color(105, 105, 105));
        colors.put("DimGrey", new Color(105, 105, 105));
        colors.put("DodgerBlue", new Color(30, 144, 255));
        colors.put("FireBrick", new Color(178, 34, 34));
        colors.put("FloralWhite", new Color(255, 250, 240));
        colors.put("ForestGreen", new Color(34, 139, 34));
        colors.put("Fuchsia", new Color(255, 0, 255));
        colors.put("Gainsboro", new Color(220, 220, 220));
        colors.put("GhostWhite", new Color(248, 248, 255));
        colors.put("Gold", new Color(255, 215, 0));
        colors.put("GoldenRod", new Color(218, 165, 32));
        colors.put("Gray", new Color(128, 128, 128));
        colors.put("Grey", new Color(128, 128, 128));
        colors.put("Green", new Color(0, 128, 0));
        colors.put("GreenYellow", new Color(173, 255, 47));
        colors.put("HoneyDew", new Color(240, 255, 240));
        colors.put("HotPink", new Color(255, 105, 180));
        colors.put("IndianRed ", new Color(205, 92, 92));
        colors.put("Indigo ", new Color(75, 0, 130));
        colors.put("Ivory", new Color(255, 255, 240));
        colors.put("Khaki", new Color(240, 230, 140));
        colors.put("Lavender", new Color(230, 230, 250));
        colors.put("LavenderBlush", new Color(255, 240, 245));
        colors.put("LawnGreen", new Color(124, 252, 0));
        colors.put("LemonChiffon", new Color(255, 250, 205));
        colors.put("LightBlue", new Color(173, 216, 230));
        colors.put("LightCoral", new Color(240, 128, 128));
        colors.put("LightCyan", new Color(224, 255, 255));
        colors.put("LightGoldenRodYellow", new Color(250, 250, 210));
        colors.put("LightGray", new Color(211, 211, 211));
        colors.put("LightGrey", new Color(211, 211, 211));
        colors.put("LightGreen", new Color(144, 238, 144));
        colors.put("LightPink", new Color(255, 182, 193));
        colors.put("LightSalmon", new Color(255, 160, 122));
        colors.put("LightSeaGreen", new Color(32, 178, 170));
        colors.put("LightSkyBlue", new Color(135, 206, 250));
        colors.put("LightSlateGray", new Color(119, 136, 153));
        colors.put("LightSlateGrey", new Color(119, 136, 153));
        colors.put("LightSteelBlue", new Color(176, 196, 222));
        colors.put("LightYellow", new Color(255, 255, 224));
        colors.put("Lime", new Color(0, 255, 0));
        colors.put("LimeGreen", new Color(50, 205, 50));
        colors.put("Linen", new Color(250, 240, 230));
        colors.put("Magenta", new Color(255, 0, 255));
        colors.put("Maroon", new Color(128, 0, 0));
        colors.put("MediumAquaMarine", new Color(102, 205, 170));
        colors.put("MediumBlue", new Color(0, 0, 205));
        colors.put("MediumOrchid", new Color(186, 85, 211));
        colors.put("MediumPurple", new Color(147, 112, 216));
        colors.put("MediumSeaGreen", new Color(60, 179, 113));
        colors.put("MediumSlateBlue", new Color(123, 104, 238));
        colors.put("MediumSpringGreen", new Color(0, 250, 154));
        colors.put("MediumTurquoise", new Color(72, 209, 204));
        colors.put("MediumVioletRed", new Color(199, 21, 133));
        colors.put("MidnightBlue", new Color(25, 25, 112));
        colors.put("MintCream", new Color(245, 255, 250));
        colors.put("MistyRose", new Color(255, 228, 225));
        colors.put("Moccasin", new Color(255, 228, 181));
        colors.put("NavajoWhite", new Color(255, 222, 173));
        colors.put("Navy", new Color(0, 0, 128));
        colors.put("OldLace", new Color(253, 245, 230));
        colors.put("Olive", new Color(128, 128, 0));
        colors.put("OliveDrab", new Color(107, 142, 35));
        colors.put("Orange", new Color(255, 165, 0));
        colors.put("OrangeRed", new Color(255, 69, 0));
        colors.put("Orchid", new Color(218, 112, 214));
        colors.put("PaleGoldenRod", new Color(238, 232, 170));
        colors.put("PaleGreen", new Color(152, 251, 152));
        colors.put("PaleTurquoise", new Color(175, 238, 238));
        colors.put("PaleVioletRed", new Color(216, 112, 147));
        colors.put("PapayaWhip", new Color(255, 239, 213));
        colors.put("PeachPuff", new Color(255, 218, 185));
        colors.put("Peru", new Color(205, 133, 63));
        colors.put("Pink", new Color(255, 192, 203));
        colors.put("Plum", new Color(221, 160, 221));
        colors.put("PowderBlue", new Color(176, 224, 230));
        colors.put("Purple", new Color(128, 0, 128));
        colors.put("Red", new Color(255, 0, 0));
        colors.put("RosyBrown", new Color(188, 143, 143));
        colors.put("RoyalBlue", new Color(65, 105, 225));
        colors.put("SaddleBrown", new Color(139, 69, 19));
        colors.put("Salmon", new Color(250, 128, 114));
        colors.put("SandyBrown", new Color(244, 164, 96));
        colors.put("SeaGreen", new Color(46, 139, 87));
        colors.put("SeaShell", new Color(255, 245, 238));
        colors.put("Sienna", new Color(160, 82, 45));
        colors.put("Silver", new Color(192, 192, 192));
        colors.put("SkyBlue", new Color(135, 206, 235));
        colors.put("SlateBlue", new Color(106, 90, 205));
        colors.put("SlateGray", new Color(112, 128, 144));
        colors.put("SlateGrey", new Color(112, 128, 144));
        colors.put("Snow", new Color(255, 250, 250));
        colors.put("SpringGreen", new Color(0, 255, 127));
        colors.put("SteelBlue", new Color(70, 130, 180));
        colors.put("Tan", new Color(210, 180, 140));
        colors.put("Teal", new Color(0, 128, 128));
        colors.put("Thistle", new Color(216, 191, 216));
        colors.put("Tomato", new Color(255, 99, 71));
        colors.put("Turquoise", new Color(64, 224, 208));
        colors.put("Violet", new Color(238, 130, 238));
        colors.put("Wheat", new Color(245, 222, 179));
        colors.put("White", new Color(255, 255, 255));
        colors.put("WhiteSmoke", new Color(245, 245, 245));
        colors.put("Yellow", new Color(255, 255, 0));
        colors.put("YellowGreen", new Color(154, 205, 50));
        
        COLORS = colors;
    }
    
    public static @NotNull MutableText format(@NotNull Text text, @NotNull Formatting... formatting) {
        if (text instanceof MutableText mutableText)
            return mutableText.formatted(formatting);
        return ColorUtils.format(new LiteralText("")
            .append(text), formatting);
    }
    public static @NotNull MutableText format(@NotNull Text text, @NotNull TextColor color) {
        if (text instanceof MutableText mutableText)
            return mutableText.styled((style) -> style.withColor(color));
        return ColorUtils.format(new LiteralText("")
            .append(text), color);
    }
    
    private static @NotNull Color materialToColor(@NotNull Material material) {
        return new Color(material.getColor().color);
    }
    private static @NotNull Color dyeToColor(@NotNull DyeColor color) {
        float[] components = color.getColorComponents();
        return new Color(components[0], components[1], components[2]);
    }
    private static @NotNull Color formatToColor(Formatting color) {
        return new Color(ColorUtils.formatToInt( color ));
    }
    private static int formatToInt(@NotNull Formatting color) {
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
    private static double colorDistance(@NotNull Color one, @NotNull Color two) {
        return Math.pow((one.getRed() - two.getRed()) * 0.30, 2)
            + Math.pow((one.getGreen() - two.getGreen()) * 0.59, 2)
            + Math.pow((one.getBlue() - two.getBlue()) * 0.11, 2);
    }
    
    public static @NotNull DyeColor getNearestDye(@NotNull Formatting color) {
        DyeColor dye = DyeColor.byName(color.getName(), null);
        if (dye != null)
            return dye;
        return ColorUtils.getNearestDye(ColorUtils.formatToColor( color ));
    }
    public static @NotNull DyeColor getNearestDye(@NotNull String hex) {
        return ColorUtils.getNearestDye(Color.decode( hex ));
    }
    public static @NotNull DyeColor getNearestDye(float r, float g, float b) {
        return ColorUtils.getNearestDye(new Color(r, g, b));
    }
    public static @NotNull DyeColor getNearestDye(@NotNull Color colorA) {
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
    
    public static @Nullable TextColor getNearestTextColor(@NotNull Formatting formatting) {
        return TextColor.fromFormatting(formatting);
    }
    public static @NotNull TextColor getNearestTextColor(@NotNull String hex) {
        return ColorUtils.getNearestTextColor(Color.decode(hex));
    }
    public static @NotNull TextColor getNearestTextColor(float r, float g, float b) {
        int rgb = (int)r;
        rgb = (rgb << 8) + (int)g;
        rgb = (rgb << 8) + (int)b;
        return TextColor.fromRgb(rgb);
    }
    public static @NotNull TextColor getNearestTextColor(@NotNull Color color) {
        return ColorUtils.getNearestTextColor(color.getRed(), color.getGreen(), color.getBlue());
    }
    
    public static @NotNull Color[] getRange(@NotNull Color start, @NotNull Color end, final int scale) {
        if (scale == 1)
            return new Color[] { start };
        if (scale == 2)
            return new Color[] { start, end };
        
        float r = (float)(end.getRed() - start.getRed()) / scale,
            g = (float)(end.getGreen() - start.getGreen()) / scale,
            b = (float)(end.getBlue() - start.getBlue()) / scale;
        
        Color[] colors = new Color[scale]; // Create the scale
        colors[0] = start; // Add starting color
        for (int i = 1; i < scale; i++) {
            Color previous = colors[i - 1]; // Set the previous
            colors[i] = ColorUtils.createColor(previous.getRed() + r, previous.getGreen() + g, previous.getBlue() + b); //
        }
        
        return colors;
    }
    
    public static @NotNull Formatting getNearestFormatting(@NotNull DyeColor color) {
        Formatting formatting = Formatting.byName(color.getName());
        if (formatting != null)
            return formatting;
        return ColorUtils.getNearestFormatting(ColorUtils.dyeToColor( color ));
    }
    public static @NotNull Formatting getNearestFormatting(@NotNull String hex) {
        return ColorUtils.getNearestFormatting(Color.decode(hex));
    }
    public static @NotNull Formatting getNearestFormatting(float r, float g, float b) {
        return ColorUtils.getNearestFormatting(new Color(r, g, b));
    }
    public static @NotNull Formatting getNearestFormatting(@NotNull Color color) {
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
    public static @NotNull Formatting getNearestFormatting(@NotNull TextColor color) {
        String name = color.toString();
        if (name.startsWith("#"))
            return ColorUtils.getNearestFormatting(name);
        switch (name) {
            case "black":
                return Formatting.BLACK;
            case "dark_blue":
                return Formatting.DARK_BLUE;
            case "dark_green":
                return Formatting.DARK_GREEN;
            case "dark_aqua":
                return Formatting.DARK_AQUA;
            case "dark_red":
                return Formatting.DARK_RED;
            case "dark_purple":
                return Formatting.DARK_PURPLE;
            case "gold":
                return Formatting.GOLD;
            case "gray":
                return Formatting.GRAY;
            case "dark_gray":
                return Formatting.DARK_GRAY;
            case "blue":
                return Formatting.BLUE;
            case "green":
                return Formatting.GREEN;
            case "aqua":
                return Formatting.AQUA;
            case "red":
                return Formatting.RED;
            case "light_purple":
                return Formatting.LIGHT_PURPLE;
            case "yellow":
                return Formatting.YELLOW;
            case "white":
                return Formatting.WHITE;
            default: return Formatting.RESET;
        }
    }
    
    public static @NotNull Collection<Formatting> formattingColors() {
        return ImmutableList.of(
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
            Formatting.WHITE
        );
    }
    
    public static @NotNull String getLegacyTag(@NotNull Formatting formatting) {
        char letter;
        switch (formatting) {
            case BLACK:
                letter = '0';
                break;
            case DARK_BLUE:
                letter = '1';
                break;
            case DARK_GREEN:
                letter = '2';
                break;
            case DARK_AQUA:
                letter = '3';
                break;
            case DARK_RED:
                letter = '4';
                break;
            case DARK_PURPLE:
                letter = '5';
                break;
            case GOLD:
                letter = '6';
                break;
            case GRAY:
                letter = '7';
                break;
            case DARK_GRAY:
                letter = '8';
                break;
            case BLUE:
                letter = '9';
                break;
            case GREEN:
                letter = 'a';
                break;
            case AQUA:
                letter = 'b';
                break;
            case RED:
                letter = 'c';
                break;
            case LIGHT_PURPLE:
                letter = 'd';
                break;
            case YELLOW:
                letter = 'e';
                break;
            case WHITE:
                letter = 'f';
                break;
            case OBFUSCATED:
                letter = 'k';
                break;
            case BOLD:
                letter = 'l';
                break;
            case STRIKETHROUGH:
                letter = 'm';
                break;
            case UNDERLINE:
                letter = 'n';
                break;
            case ITALIC:
                letter = 'o';
                break;
            case RESET:
                letter = 'r';
                break;
            default:
                return "";
        }
        return ColorUtils.LEGACY_TAG + letter;
    }
    
    public static @NotNull DyeColor getRandomDye() {
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
    public static @NotNull Formatting getRandomFormat() {
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
    
    public static @NotNull Set<String> getSuggestedNames() {
        return ColorUtils.COLORS.keySet();
    }
    public static @Nullable TextColor getRawTextColor(@NotNull String value) {
        if (!value.startsWith("#"))
            return ColorUtils.getNearestTextColor(ColorUtils.COLORS.get(value));
        return TextColor.parse(value);
    }
    public static @Nullable Color getRawColor(@NotNull String value) {
        if (!value.startsWith("#"))
            return ColorUtils.COLORS.get(value);
        try {
            return new Color(Integer.parseInt(value.substring(1), 16));
        } catch (NumberFormatException var2) {}
        return null;
    }
    
    public static @NotNull Color createColor(int r, int g, int b) {
        return new Color(
            ColorUtils.withinScale(r) / 255,
            ColorUtils.withinScale(g) / 255,
            ColorUtils.withinScale(b) / 255
        );
    }
    public static @NotNull Color createColor(float r, float g, float b) {
        return new Color(
            ColorUtils.withinScale(r) / 255,
            ColorUtils.withinScale(g) / 255,
            ColorUtils.withinScale(b) / 255
        );
    }
    public static int withinScale(int c) {
        return Math.max(0, Math.min(c, 255));
    }
    public static float withinScale(float c) {
        return Math.max(0, Math.min(c, 255));
    }
    
    static {
        RANDOM = new Random();
    }
}
