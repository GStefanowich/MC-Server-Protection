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
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * Created on Mar 24 2021 at 12:15 PM.
 * By greg in SewingMachineMod
 */
public final class StyleApplicator implements UnaryOperator<Style> {
    
    private @Nullable TextColor color = null;
    
    public StyleApplicator(@Nullable TextColor color) {
        this.color = color;
    }
    public StyleApplicator(@NotNull Formatting color) {
        if (color.isColor())
            this.color = TextColor.fromFormatting(color);
    }
    public StyleApplicator(@NotNull String color) {
        TextColor textColor = ColorUtils.getRawTextColor(color);
        if (textColor != null)
            this.color = textColor;
    }
    
    @Override
    public @NotNull Style apply(@NotNull Style style) {
        if (this.color != null)
            style = style.withColor(this.color);
        return style;
    }
}
