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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public final class ToggleWidget implements ButtonWidget.PressAction {
    private @NotNull Text initialText;
    private boolean state;
    
    private final Change onChange;
    
    public ToggleWidget(
        boolean state,
        @NotNull Change onChange
    ) {
        this.state = state;
        this.onChange = onChange;
    }
    
    public void init(@NotNull ClickableWidget widget) {
        this.initialText = widget.getMessage();
        widget.setMessage(this.getTextState());
    }
    
    public boolean get() {
        return this.state;
    }
    
    @Override
    public void onPress(@NotNull ButtonWidget button) {
        // Toggle the state to inverse
        this.state = !this.state;
        
        // Update the button
        button.setMessage(this.getTextState());
        
        // Run the click events
        this.onChange.onToggle(button, this);
    }
    
    private Text getTextState() {
        if (this.initialText == null)
            throw new IllegalStateException("Widget is not ready yet");
        return this.initialText.copy()
            .append(": ")
            .append(this.state ? "Enabled" : "Disabled");
    }
    
    @FunctionalInterface
    public interface Change {
        void onToggle(@NotNull ButtonWidget button, @NotNull ToggleWidget state);
    }
}
