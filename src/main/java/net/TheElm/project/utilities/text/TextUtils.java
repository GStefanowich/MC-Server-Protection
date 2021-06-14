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

package net.TheElm.project.utilities.text;

import net.TheElm.project.utilities.ColorUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;

/**
 * Created on Mar 24 2021 at 12:15 PM.
 * By greg in SewingMachineMod
 */
public final class TextUtils {
    private TextUtils() {}
    
    public static @NotNull MutableText literal() {
        return new LiteralText("");
    }
    public static @NotNull MutableText literal(@NotNull String text) {
        return new LiteralText(text);
    }
    public static @NotNull MutableText literal(@NotNull String text, @Nullable Formatting formatting) {
        return new LiteralText(text)
            .styled(new StyleApplicator(formatting));
    }
    public static @NotNull MutableText literal(@NotNull final String text, @Nullable final Color color) {
        return TextUtils.literal(text, color, color);
    }
    public static @NotNull MutableText literal(@NotNull final String text, @Nullable final Color start, @Nullable final Color end) {
        if (start == null || end == null || Objects.equals(start, end))
            return new LiteralText(text)
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
}
