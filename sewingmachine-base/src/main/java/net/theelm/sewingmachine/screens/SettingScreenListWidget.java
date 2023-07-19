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

package net.theelm.sewingmachine.screens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.objects.widgets.CycleWidget;
import net.theelm.sewingmachine.objects.widgets.ToggleWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Created on Jul 05 2023 at 1:59 AM.
 * By greg in sewingmachine
 */
public class SettingScreenListWidget extends EntryListWidget<SettingScreenListWidget.Entry> {
    private final @NotNull SettingScreen screen;
    
    public SettingScreenListWidget(@NotNull MinecraftClient minecraftClient, @NotNull SettingScreen screen, int top, int bottom, int itemHeight) {
        super(minecraftClient, screen.width, screen.height, top, bottom, itemHeight);
        this.screen = screen;
        
        // Don't render the dirt background
        this.setRenderBackground(false);
        this.setRenderHeader(false, 0);
        this.setRenderHorizontalShadows(false);
        
        // Don't render a box around selected inputs
        this.setRenderSelection(false);
    }
    
    public ButtonWidget addClickable(@NotNull ButtonWidget widget) {
        this.addEntry(new Entry(widget));
        return widget;
    }
    public ButtonWidget addClickable(@NotNull ButtonWidget.Builder builder) {
        return this.addClickable(builder.build());
    }
    
    public @NotNull ClickableWidget addButton(@NotNull Text text, @Nullable Text tooltip, ButtonWidget.@NotNull PressAction action) {
        return this.addButton(text, this.tooltip(tooltip), action);
    }
    public @NotNull ClickableWidget addButton(@NotNull Text text, @Nullable Tooltip tooltip, ButtonWidget.@NotNull PressAction action) {
        ButtonWidget.Builder builder = ButtonWidget.builder(text, action);
        builder.size(100, 20);
        if (tooltip != null)
            builder.tooltip(tooltip);
        
        // Add the drawable
        return this.addClickable(builder);
    }
    
    public void addScreenButton(@NotNull Text text, @Nullable Text tooltip, @NotNull Supplier<@NotNull Screen> supplier) {
        this.addScreenButton(text, this.tooltip(tooltip), supplier);
    }
    public void addScreenButton(@NotNull Text text, @Nullable Tooltip tooltip, @NotNull Supplier<@NotNull Screen> supplier) {
        this.addButton(text, tooltip, button -> this.client.setScreen(supplier.get()));
    }
    
    public @NotNull ClickableWidget addToggleButton(@NotNull Text text, @Nullable Text tooltip, boolean state, ToggleWidget.@NotNull Change onChange) {
        ToggleWidget toggle = new ToggleWidget(state, onChange);
        ClickableWidget widget = this.addButton(text, tooltip, toggle);
        
        // Run the init method
        toggle.init(widget);
        
        return widget;
    }
    
    public <E extends Enum<E>> @NotNull ClickableWidget addCycleButton(@NotNull Class<E> clazz, @NotNull Text text, @Nullable Text tooltip, @NotNull E state, CycleWidget.@NotNull Change<E> onChange) {
        return this.addCycleButton(clazz, text, this.tooltip(tooltip), state, onChange);
    }
    public <E extends Enum<E>> @NotNull ClickableWidget addCycleButton(@NotNull Class<E> clazz, @NotNull Text text, @Nullable Tooltip tooltip, @NotNull E state, CycleWidget.@NotNull Change<E> onChange) {
        CycleWidget cycle = new CycleWidget<>(clazz, state, onChange);
        ClickableWidget widget = this.addButton(text, tooltip, cycle);
        
        // Run the init method
        cycle.init(widget);
        
        return widget;
    }
    
    private @Nullable Tooltip tooltip(@Nullable Text text) {
        return text == null ? null : Tooltip.of(text);
    }
    
    @Override
    public int getRowWidth() {
        return this.screen.backgroundWidth - 18;
    }
    
    @Override
    protected int getScrollbarPositionX() {
        return this.screen.x + this.screen.backgroundWidth - 10;
    }
    
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        
    }
    
    public static class Entry extends EntryListWidget.Entry<Entry> {
        private static final int X_OFFSET = -6;
        private static final int Y_OFFSET = -3;
        private final @NotNull ButtonWidget widget;
        
        public Entry(@NotNull ButtonWidget widget) {
            this.widget = widget;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.widget.setWidth(entryWidth);
            this.widget.setPosition(x + X_OFFSET, y + Y_OFFSET);
            this.widget.render(context, mouseX, mouseY, tickDelta);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.widget.mouseClicked(mouseX, mouseY, button);
        }
        
        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.widget.mouseReleased(mouseX, mouseY, button);
        }
    }
}
