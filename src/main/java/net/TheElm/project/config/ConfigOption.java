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

package net.TheElm.project.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class ConfigOption<T extends Object> extends ConfigBase<T> {
    
    private final Function<JsonElement, T> setter;
    private T value;
    private JsonSerializer<T> serializer = null;
    
    public ConfigOption(@NotNull String location, @Nullable Function<JsonElement, T> setter) {
        this(location, null, setter);
    }
    public ConfigOption(@NotNull String location, @Nullable T defaultValue, @Nullable Function<JsonElement, T> setter) {
        this(location, defaultValue, setter, null);
    }
    public ConfigOption(@NotNull String location, @Nullable T defaultValue, @Nullable Function<JsonElement, T> setter, @Nullable JsonSerializer<T> serializer) {
        super(location);
        
        this.value = defaultValue;
        this.setter = setter;
        this.serializer = serializer;
    }
    
    @Override
    public final void set( JsonElement value ) {
        this.value = ( value == null ? null : this.setter.apply( value ) );
    }
    public final T get() {
        return this.value;
    }
    
    public final boolean isTrue() {
        T val = this.get();
        if (val instanceof Boolean)
            return (Boolean)val;
        if (val instanceof Number)
            return ((Number)val).longValue() >= 0;
        return val != null;
    }
    
    @Override
    public final JsonElement getElement() {
        return ConfigOption.convertToJSON( this.value, this.serializer );
    }
    
    public final ConfigOption<T> serializer(JsonSerializer<T> serializer) {
        this.serializer = serializer;
        return this;
    }
    
    public static JsonElement convertToJSON( @Nullable Object src ) {
        return ConfigOption.convertToJSON( src, null );
    }
    public static <T extends Object> JsonElement convertToJSON( @Nullable T src, @Nullable JsonSerializer<T> serializer ) {
        GsonBuilder builder = new GsonBuilder();
        if (src != null && serializer != null)
            builder.registerTypeAdapter(src.getClass(), serializer);
        return builder.create().toJsonTree( src );
    }
    
}
