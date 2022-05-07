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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on Aug 18 2021 at 7:07 PM.
 * By greg in SewingMachineMod
 */
public final class WrittenBookBuilder {
    private final @NotNull JsonArray pages = new JsonArray();
    private @NotNull JsonObject last = WrittenBookBuilder.blankBookComponent();
    public WrittenBookBuilder() {
        this.add(this.last);
    }
    
    private WrittenBookBuilder add(@NotNull JsonObject obj) {
        this.pages.add(obj);
        this.last = obj;
        return this;
    }
    public WrittenBookBuilder addString(@Nullable String text) {
        return this.add(WrittenBookBuilder.stringBookComponent(text));
    }
    public WrittenBookBuilder addString(@Nullable String text, @Nullable Formatting... formatting) {
        return this.add(WrittenBookBuilder.stringBookComponent(text, formatting));
    }
    public WrittenBookBuilder addString(@NotNull Text text) {
        return this.add(WrittenBookBuilder.stringBookComponent(text.getString()));
    }
    public WrittenBookBuilder addString(@NotNull Text text, @Nullable Formatting... formatting) {
        return this.add(WrittenBookBuilder.stringBookComponent(text.getString(), formatting));
    }
    public WrittenBookBuilder addTranslation(@Nullable String text) {
        return this.add(WrittenBookBuilder.translateBookComponent(text));
    }
    public WrittenBookBuilder addTranslation(@Nullable String text, @Nullable Formatting... formatting) {
        return this.add(WrittenBookBuilder.translateBookComponent(text, formatting));
    }
    public WrittenBookBuilder addLine() {
        return this.addLines(1);
    }
    public WrittenBookBuilder addLines(int count) {
        if (count <= 0)
            return this;
        JsonObject obj = this.last;
        String newLines = StringUtils.repeat('\n', count);
        if (obj.has("text")) {
            JsonElement element = obj.get("text");
            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isString()) {
                    obj.addProperty("text", primitive.getAsString() + newLines);
                    return this;
                }
            }
        }
        
        return this.addString(newLines);
    }
    
    @Override
    public String toString() {
        return this.pages.toString();
    }
    
    public static @NotNull JsonObject blankBookComponent() {
        return WrittenBookBuilder.stringBookComponent(null);
    }
    public static @NotNull JsonObject stringBookComponent(@Nullable String text, @Nullable Formatting... formatting) {
        JsonObject obj = new JsonObject();
        
        obj.addProperty("text", text == null ? "" : text);
        WrittenBookBuilder.applyFormatting(obj, formatting);
        
        return obj;
    }
    public static @NotNull JsonObject translateBookComponent(@Nullable String text, @Nullable Formatting... formatting) {
        JsonObject obj = new JsonObject();

        obj.addProperty("translate", text == null ? "" : text);
        WrittenBookBuilder.applyFormatting(obj, formatting);

        return obj;
    }
    private static void applyFormatting(@NotNull JsonObject obj, @Nullable Formatting... formattings) {
        if (formattings != null) for (Formatting formatting : formattings) {
            if (formatting.isColor())
                obj.addProperty("color", formatting.getName());
            else if (formatting.isModifier())
                obj.addProperty(formatting.getName(), true);
        }
    }
}
