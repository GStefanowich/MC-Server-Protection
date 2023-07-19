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

package net.theelm.sewingmachine.objects.widgets;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.interfaces.TranslationDescriber;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class CycleWidget<E extends Enum<E>> implements ButtonWidget.PressAction {
    private @NotNull Text initialText;
    private final @NotNull List<E> values;
    private final @NotNull Change<E> onChange;
    
    private int index = 0;
    
    public CycleWidget(@NotNull Class<E> clazz, @NotNull E value, @NotNull Change<E> onChange) {
        this.values = new ArrayList<>(EnumSet.allOf(clazz));
        for (E val : this.values) {
            if (val == value)
                break;
            this.index++;
        }
        this.onChange = onChange;
    }
    
    public void init(@NotNull ClickableWidget widget) {
        this.initialText = widget.getMessage();
        widget.setMessage(this.getTextState());
    }
    
    public E get() {
        return this.values.get(this.index);
    }
    
    @Override
    public void onPress(ButtonWidget button) {
        this.index++;
        if (this.index >= this.values.size())
            this.index = 0;
        
        // Update the button
        button.setMessage(this.getTextState());
        
        this.onChange.onCycle(button, this);
    }
    
    private Text getTextState() {
        if (this.initialText == null)
            throw new IllegalStateException("Widget is not ready yet");
        Text value;
        E current = this.get();
        if (current instanceof TranslationDescriber provider)
            value = Text.translatable(provider.getTranslationKey())
                .styled(style -> style.withColor(provider.getColor()));
        else value = Text.literal(current.name());
        
        return this.initialText.copy()
            .append(": ")
            .append(value);
    }
    
    @FunctionalInterface
    public interface Change<E extends Enum<E>> {
        void onCycle(@NotNull ButtonWidget button, @NotNull CycleWidget<E> state);
    }
}
