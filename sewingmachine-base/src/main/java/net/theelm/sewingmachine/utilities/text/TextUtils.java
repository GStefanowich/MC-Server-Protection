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

package net.theelm.sewingmachine.utilities.text;

import net.theelm.sewingmachine.utilities.CasingUtils;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Iterator;
import java.util.Objects;

/**
 * Created on Mar 24 2021 at 12:15 PM.
 * By greg in SewingMachineMod
 */
public final class TextUtils {
    private TextUtils() {}
    
    public static @NotNull MutableText literal() {
        return TextUtils.literal("");
    }
    public static @NotNull MutableText literal(@NotNull String text, @NotNull CasingUtils.Casing casing) {
        return TextUtils.literal(casing.apply(text));
    }
    public static @NotNull MutableText literal(@NotNull String text) {
        return Text.literal(text);
    }
    public static @NotNull MutableText literal(@NotNull String text, @Nullable Formatting formatting) {
        return Text.literal(text)
            .styled(new StyleApplicator(formatting));
    }
    public static @NotNull MutableText literal(@NotNull final String text, @Nullable final Color color) {
        return TextUtils.literal(text, color, color);
    }
    public static @NotNull MutableText literal(@NotNull final String text, @Nullable final Color start, @Nullable final Color end) {
        if (start == null || end == null || Objects.equals(start, end))
            return Text.literal(text)
                .styled(new StyleApplicator(start == null ? null : ColorUtils.getNearestTextColor(start)));
        
        MutableText main = null;
        char[] chars = text.toCharArray();
        Color[] colors = ColorUtils.getRange(start, end, chars.length);
        
        for (int i = 0; i < chars.length; i++) {
            MutableText segment = TextUtils.literal(String.valueOf(chars[i]), colors[i]);
            if (main == null)
                main = segment;
            else main.append(segment);
        }
        
        return Objects.requireNonNull(main);
    }
    public static @NotNull MutableText literal(int i) {
        return Text.literal(i + "");
    }
    public static @NotNull MutableText literal(long l) {
        return Text.literal(l + "");
    }
    public static @NotNull MutableText literal(double d) {
        return Text.literal(d + "");
    }
    
    public static @NotNull MutableText mutable(@Nullable Text text) {
        if (text instanceof MutableText mutable)
            return mutable;
        return TextUtils.literal()
            .append(text);
    }
    
    public static @NotNull String legacyConvert(@NotNull Text text) {
        StringBuilder out = new StringBuilder();
        Formatting formatting = null;
        
        Text i = text;
        Iterator<Text> iterator = text.getSiblings().iterator();
        do {
            Style style = i.getStyle();
            
            // Apply any styling to the given text
            if (style.isObfuscated())
                out.append(ColorUtils.getLegacyTag(Formatting.OBFUSCATED));
            if (style.isBold())
                out.append(ColorUtils.getLegacyTag(Formatting.BOLD));
            if (style.isStrikethrough())
                out.append(ColorUtils.getLegacyTag(Formatting.STRIKETHROUGH));
            if (style.isUnderlined())
                out.append(ColorUtils.getLegacyTag(Formatting.UNDERLINE));
            if (style.isItalic())
                out.append(ColorUtils.getLegacyTag(Formatting.ITALIC));
            
            TextColor color = style.getColor();
            if (color != null) {
                formatting = ColorUtils.getNearestFormatting(color);
                out.append(ColorUtils.getLegacyTag(formatting));
            } else if (formatting != null) {
                out.append(ColorUtils.LEGACY_TAG + "r");
                formatting = null;
            }
            
            out.append(i.getString());
        } while (iterator.hasNext() && (i = iterator.next()) != null);
        
        // Final formatting reset
        if (formatting != null)
            out.append(ColorUtils.LEGACY_TAG + "r");
        
        return out.toString();
    }
    
    public static @NotNull String quoteWrap(@NotNull String text) {
        return "\"" + text + "\"";
    }
    public static @NotNull MutableText quoteWrap(@NotNull MutableText text) {
        return Text.literal("\"").append(text).append("\"");
    }
    
    public static @Contract("!null -> !null") Text deepCopy(@Nullable Text formatted) {
        if (formatted == null)
            return null;
        MutableText copy = formatted.copy();
        
        // Deep copy siblings
        for (Text sibling : formatted.getSiblings())
            copy.append(TextUtils.deepCopy(sibling));
        
        // Copy the style
        copy.setStyle(formatted.getStyle());
        
        return copy;
    }
}
