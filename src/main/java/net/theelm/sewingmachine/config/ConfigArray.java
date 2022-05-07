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

package net.theelm.sewingmachine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.theelm.sewingmachine.interfaces.json.ConfigReader;
import net.theelm.sewingmachine.interfaces.json.ConfigWriter;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class ConfigArray<T extends Object> extends ConfigBase<T> {
    
    private final ConfigReader<T> setter;
    private final List<T> value;
    private @Nullable ConfigWriter<T> serializer;
    
    public ConfigArray(@NotNull String location, ConfigReader<T> setter) {
        this(location, new ArrayList<>(), setter);
    }
    public ConfigArray(@NotNull String location, @NotNull List<T> defaultValue, ConfigReader<T> setter) {
        super(location);
        
        this.value = defaultValue;
        this.setter = setter;
    }
    public ConfigArray(@NotNull String location, @NotNull List<T> defaultValue, @NotNull ConfigReader<T> setter, @Nullable ConfigWriter<T> serializer) {
        super(location);
        
        this.value = defaultValue;
        this.setter = setter;
        this.serializer = serializer;
    }
    
    public ConfigArray<T> serializer(ConfigWriter<T> serializer) {
        this.serializer = serializer;
        return this;
    }
    
    static <T> JsonElement convertToJSON(@NotNull List<T> list, @Nullable ConfigWriter<T> serializer) {
        Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
        JsonArray array = new JsonArray();
        for (T el : list) {
            array.add(serializer == null ? gson.toJsonTree(el) :  serializer.serialize(el, gson));
        }
        
        return array;
    }
    
    @Override
    JsonElement getElement() {
        return ConfigArray.convertToJSON(this.value, this.serializer);
    }
    List<T> get() {
        return this.value;
    }
    T get(int index) {
        return this.value.get(index);
    }
    T getRandom() {
        if (this.value.size() == 1)
            return this.get(0);
        return this.get(ThreadLocalRandom.current().nextInt(this.value.size()));
    }
    @Override
    void set(JsonElement value) {
        // Reset values
        this.value.clear();
        
        // Do nothing if NULL
        if (value == null) return;
        
        // Add all values
        if (!(value instanceof JsonArray))
            this.value.add(this.setter.deserialize(value));
        else for (JsonElement element : value.getAsJsonArray())
            this.value.add(this.setter.deserialize(element));
    }
    
    public static @NotNull ConfigArray<Integer> jInt(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsInt);
    }
    public static @NotNull ConfigArray<Long> jLong(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsLong);
    }
    public static @NotNull ConfigArray<Boolean> jBool(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsBoolean);
    }
    public static @NotNull ConfigArray<String> jString(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsString);
    }
    public static @NotNull <T> ConfigArray<RegistryKey<T>> registry(String location, RegistryKey<? extends Registry<T>> registry, List<RegistryKey<T>> def) {
        return new ConfigArray<>(location, def, (element) -> {
            // Convert from String to RegistryKey
            return RegistryKey.of(registry, new Identifier(element.getAsString()));
        }, (type, gson) -> {
            // Convert from RegistryKey to String
            return gson.toJsonTree(type.getValue().toString());
        });
    }
    public static @NotNull <T> ConfigArray<T> fromRegistry(String location, DefaultedRegistry<T> registry, List<T> defaults) {
        List<T> cast = defaults.stream().map((type) -> registry.get(registry.getId(type))).collect(Collectors.toList());
        return new ConfigArray<>(location, cast, (element) -> {
            // Convert from String to RegistryKey
            return registry.get(new Identifier(element.getAsString()));
        }, (type, gson) -> {
            // Convert from RegistryKey to String
            return gson.toJsonTree(registry.getId(type).toString());
        });
    }
}
