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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Created on Mar 24 2021 at 12:15 PM.
 * By greg in SewingMachineMod
 */
public final class StyleApplicator implements UnaryOperator<Style> {
    
    private @Nullable TextColor color = null;
    private @Nullable HoverEvent hover = null;
    private @Nullable ClickEvent click = null;
    private final @NotNull List<Formatting> formattings = new ArrayList<>();
    
    public StyleApplicator() {}
    public StyleApplicator(@Nullable TextColor color) {
        this.withStyle(color);
    }
    public StyleApplicator(@Nullable Formatting color) {
        this.withStyle(color);
    }
    public StyleApplicator(@Nullable String color) {
        this.withStyle(color);
    }
    public <T> StyleApplicator(HoverEvent.Action<T> action, T contents) {
        this.withHover(action, contents);
    }
    public StyleApplicator(ClickEvent.Action action, String value) {
        this.withClick(action, value);
    }
    
    public StyleApplicator withStyle(@Nullable TextColor color) {
        this.color = color;
        return this;
    }
    public StyleApplicator withStyle(@Nullable Formatting formatting) {
        if (formatting != null) {
            if (formatting == Formatting.RESET)
                this.clear();
            else if (formatting.isColor())
                this.color = TextColor.fromFormatting(formatting);
            else this.formattings.add(formatting);
        }
        return this;
    }
    public StyleApplicator withStyle(@Nullable String color) {
        TextColor textColor;
        if (color != null && (textColor = ColorUtils.getRawTextColor(color)) != null)
            this.color = textColor;
        return this;
    }
    public <T> StyleApplicator withHover(@NotNull HoverEvent.Action<T> action, @NotNull T contents) {
        this.hover = new HoverEvent(action, contents);
        return this;
    }
    public StyleApplicator withClick(@NotNull ClickEvent.Action action, @NotNull String value) {
        this.click = new ClickEvent(action, value);
        return this;
    }
    
    public boolean isEmpty() {
        return this.color == null
            && this.hover == null
            && this.click == null
            && this.formattings.isEmpty();
    }
    public void clear() {
        this.color = null;
        this.hover = null;
        this.click = null;
        this.formattings.clear();
    }
    
    @Override
    public @NotNull Style apply(@NotNull Style style) {
        if (this.color != null)
            style = style.withColor(this.color);
        if (this.hover != null)
            style = style.withHoverEvent(this.hover);
        if (this.click != null)
            style = style.withClickEvent(this.click);
        return style.withFormatting(this.formattings.toArray(new Formatting[0]));
    }
}
