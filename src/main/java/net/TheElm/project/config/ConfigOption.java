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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import net.TheElm.project.CoreMod;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ConfigOption<T extends Object> extends ConfigBase<T> {
    
    private final @NotNull Function<JsonElement, T> setter;
    private @Nullable T value;
    private @Nullable JsonSerializer<T> serializer;
    
    public ConfigOption(@NotNull String location, @NotNull Function<JsonElement, T> setter) {
        this(location, null, setter);
    }
    public ConfigOption(@NotNull String location, @Nullable T defaultValue, @NotNull Function<JsonElement, T> setter) {
        this(location, defaultValue, setter, null);
    }
    public ConfigOption(@NotNull String location, @Nullable T defaultValue, @NotNull Function<JsonElement, T> setter, @Nullable JsonSerializer<T> serializer) {
        super(location);
        
        this.value = defaultValue;
        this.setter = setter;
        this.serializer = serializer;
    }
    
    @Override
    final void set( JsonElement value ) {
        this.value = ( value == null ? null : this.setter.apply(value) );
    }
    final void set( T value ) {
        this.value = value;
    }
    final T get() {
        return this.value;
    }
    
    @Override
    final JsonElement getElement() {
        return ConfigOption.convertToJSON(this.value, this.serializer);
    }
    
    public final ConfigOption<T> serializer(JsonSerializer<T> serializer) {
        this.serializer = serializer;
        return this;
    }
    
    public static JsonElement convertToJSON( @Nullable Object src ) {
        return ConfigOption.convertToJSON( src, null );
    }
    public static <T> JsonElement convertToJSON(@Nullable T src, @Nullable JsonSerializer<T> serializer ) {
        GsonBuilder builder = new GsonBuilder();
        if (src != null && serializer != null)
            builder.registerTypeAdapter(src.getClass(), serializer);
        return builder.create().toJsonTree( src );
    }
    
    public static @NotNull ConfigOption<String> json(@NotNull String location, @Nullable String defaultValue) {
        return ConfigOption.jString(location, defaultValue);
    }
    public static @NotNull ConfigOption<Boolean> json(@NotNull String location, @Nullable Boolean defaultValue) {
        return ConfigOption.jBool(location, defaultValue);
    }
    public static @NotNull ConfigOption<Integer> json(@NotNull String location, @Nullable Integer defaultValue) {
        return ConfigOption.jInt(location, defaultValue);
    }
    public static @NotNull ConfigOption<Integer> json(@NotNull String location, @Nullable Integer defaultValue, Integer minimum, Integer maximum) {
        return ConfigOption.comparable(location, defaultValue, minimum, maximum, JsonElement::getAsInt);
    }
    public static @NotNull ConfigOption<Long> json(@NotNull String location, @Nullable Long defaultValue) {
        return ConfigOption.jLong(location, defaultValue);
    }
    public static @NotNull ConfigOption<Long> json(@NotNull String location, @Nullable Long defaultValue, Long minimum, Long maximum) {
        return ConfigOption.comparable(location, defaultValue, minimum, maximum, JsonElement::getAsLong);
    }
    
    public static @NotNull ConfigOption<Integer> jInt(@NotNull String location) {
        return new ConfigOption<>(location, JsonElement::getAsInt);
    }
    public static @NotNull ConfigOption<Integer> jInt(@NotNull String location, Integer def) {
        return new ConfigOption<>(location, def, JsonElement::getAsInt);
    }
    public static @NotNull ConfigOption<Long> jLong(@NotNull String location) {
        return new ConfigOption<>(location, JsonElement::getAsLong);
    }
    public static @NotNull ConfigOption<Long> jLong(@NotNull String location, Long def) {
        return new ConfigOption<>(location, def, JsonElement::getAsLong);
    }
    public static @NotNull ConfigOption<Boolean> jBool(@NotNull String location) {
        return new ConfigOption<>(location, JsonElement::getAsBoolean);
    }
    public static @NotNull ConfigOption<Boolean> jBool(@NotNull String location, Boolean def) {
        return new ConfigOption<>(location, def, JsonElement::getAsBoolean);
    }
    public static @NotNull ConfigOption<String> jString(@NotNull String location) {
        return new ConfigOption<>(location, JsonElement::getAsString);
    }
    public static @NotNull ConfigOption<String> jString(@NotNull String location, String def) {
        return new ConfigOption<>(location, def, JsonElement::getAsString);
    }
    public static @NotNull ConfigOption<BlockPos> blockPos(@NotNull String location, Vec3i def) {
        return new ConfigOption<>(location, (def instanceof BlockPos ? (BlockPos)def : new BlockPos(def)), (e) -> {
            try {
                if (e instanceof JsonArray) {
                    // Get the X, Y, Z coordinates
                    JsonArray array = (JsonArray)e;
                    return new BlockPos(
                        array.get(0).getAsInt(),
                        array.get(1).getAsInt(),
                        array.get(2).getAsInt()
                    );
                } else if (e instanceof JsonPrimitive) {
                    JsonPrimitive prim = (JsonPrimitive)e;
                    return BlockPos.fromLong(prim.getAsLong());
                }
            } catch (NumberFormatException ex) {
                CoreMod.logError(new Exception("Failed to parse BlockPos in config, using fallback value", ex));
            }
            return BlockPos.ORIGIN;
        }, (s, type, json) -> {
            // Create an array
            JsonArray array = new JsonArray();
            
            // Add the coordinates
            array.add(s.getX());
            array.add(s.getY());
            array.add(s.getZ());
            
            return array;
        });
    }
    public static @NotNull <T extends Object & Comparable<? super T>> ConfigOption<T> comparable(@NotNull String location, @Nullable T def, final @NotNull T minimum, final @NotNull T maximum, final @NotNull Function<JsonElement, T> setter ) {
        return new ConfigOption<>(location, def, (raw) -> {
            T setTo = setter.apply(raw);
            return (setTo.compareTo(minimum) < 0 || maximum.compareTo(setTo) < 0) ? def : setTo;
        });
    }
    public static @NotNull <T> ConfigOption<RegistryKey<T>> registry(String location, RegistryKey<T> def, RegistryKey<? extends Registry<T>> registry) {
        return new ConfigOption<>(location, def, (element) -> {
            // Convert from String to RegistryKey
            return RegistryKey.of(registry, new Identifier(element.getAsString()));
        }, (world, type, json) -> {
            // Convert from RegistryKey to String
            return json.serialize(world.getValue().toString());
        });
    }
}
