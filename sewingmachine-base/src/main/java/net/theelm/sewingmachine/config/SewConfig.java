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
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.registry.Registries;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.protections.logging.EventLogger.LoggingIntervals;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.IntUtils;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.utilities.Sew;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SewConfig extends SewConfigContainer {
    private static final SewConfig INSTANCE = new SewConfig();
    public static final String VERSION_KEY = "config_version";
    private static final String MODULE_KEY = "_";
    private boolean fileExists;
    
    private final List<Runnable> reloadFunctions = new ArrayList<>();
    private final Map<String, SewConfig> configModules = new ConcurrentHashMap<>();
    
    public static boolean preExisting() {
        return SewConfig.INSTANCE.fileExists;
    }
    public static @Nullable JsonElement reload() throws IOException {
        try {
            return SewConfig.INSTANCE.reload(SewConfig.getConfigFile());
        } finally {
            for (Runnable r : SewConfig.INSTANCE.reloadFunctions)
                r.run();
        }
    }
    public @Nullable JsonElement reload(File config) throws IOException {
        //Read the existing config
        return this.loadFromJSON(SewConfig.loadFromFile(config));
    }
    
    public static <T> ConfigOption<T> addConfig( ConfigOption<T> config ) {
        // Check for a non-empty path for saving
        if (config.getPath().isEmpty()) throw new RuntimeException("Config Option path should not be empty");
        
        // Store the config
        SewConfig.INSTANCE.configOptions.add(config);
        return config;
    }
    public static <T> ConfigArray<T> addConfig( ConfigArray<T> config ) {
        SewConfig.INSTANCE.configOptions.add(config);
        return config;
    }
    public static void addConfigClass(@NotNull Class<?> klass) {
        Field[] fields = klass.getDeclaredFields();
        try {
            for (Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers()))
                    continue;
                if (field.get(null) instanceof ConfigOption<?> option)
                    SewConfig.addConfig(option);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * After all modules have called {@code addConfigClass} load the config from the disk
     */
    public static void firstInitialize() {
        File config = null;
        try {
            config = SewConfig.getConfigFile();
            
            /*
             * Read the existing config
             */
            JsonElement loaded = SewConfig.INSTANCE.reload(config);
            
            /*
             * Save any new values
             */
            SewConfig.saveToFile(config, loaded);
            
        } catch (IOException e) {
            CoreMod.logError( e );
        }
        
        SewConfig.INSTANCE.fileExists = ((config != null) && config.exists());
    }
    
    public static <T> void set( @NotNull ConfigOption<T> config, JsonElement value ) {
        config.set(value);
    }
    public static <T> void set( @NotNull ConfigOption<T> config, JsonElement value, boolean wasDefined ) {
        config.set(value, wasDefined);
    }
    public static <T> void set( @NotNull ConfigOption<T> config, T value ) {
        config.set(value);
    }
    
    public static <T> T get( @NotNull ConfigOption<T> config ) {
        return config.get();
    }
    public static <T> List<T> get( @NotNull ConfigArray<T> config ) {
        return config.get();
    }
    public static <T> T get( @NotNull Supplier<T> supplier ) {
        return supplier.get();
    }
    public static <T> T getRandom( ConfigArray<T> config ) {
        return config.getRandom();
    }
    public static <T> T get( ConfigArray<T> config, int pos ) {
        return config.get(pos);
    }
    
    public static <T> boolean equals( ConfigOption<T> a, ConfigOption<T> b ) {
        return Objects.equals(SewConfig.get(a), SewConfig.get(b));
    }
    public static <T> boolean equals( ConfigOption<T> option, T value ) {
        return Objects.equals(SewConfig.get(option), value);
    }
    
    public static boolean isTrue( ConfigOption config ) {
        Object val = config.get();
        if (val instanceof Boolean bool)
            return bool;
        if (val instanceof Number number)
            return number.longValue() >= 0;
        return val != null;
    }
    public static boolean isFalse( ConfigOption config ) {
        return !SewConfig.isTrue(config);
    }
    public static boolean any(ConfigOption... configs ) {
        for (ConfigOption config : configs)
            if (SewConfig.isTrue(config))
                return true;
        return false;
    }
    public static boolean all( ConfigOption... configs ) {
        for (ConfigOption config : configs)
            if (!SewConfig.isTrue(config))
                return false;
        return true;
    }
    
    /**
     * File save / load
     */
    private static @NotNull File getConfigFile() throws IOException {
        // Load from the folder
        final File dir = Sew.getConfDir();
        final File config = new File(dir, "config.json");
        
        if (!config.exists()) {
            if (!config.createNewFile())
                throw new RuntimeException("Error accessing the config");
            /*
             * Create a new config
             */
            SewConfig.saveToFile(config, INSTANCE.saveToJSON());
        }
        return config;
    }
    public static void save() throws IOException {
        SewConfig.saveToFile(SewConfig.getConfigFile(), INSTANCE.saveToJSON());
    }
    
    private static void saveToFile(File configFile, JsonElement json) throws IOException {
        // GSON Parser
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
        
        try (FileWriter fw = new FileWriter(configFile)) {
            fw.append(gson.toJson(SewConfig.sortObject(json)));
        }
    }
    private static JsonObject loadFromFile(File configFile) throws FileNotFoundException {
        return JsonParser.parseReader(new FileReader(configFile))
            .getAsJsonObject();
    }
    
    private JsonElement loadFromJSON(JsonObject json) {
        CoreMod.logInfo("Loading configuration file (" + FormattingUtils.format(this.configOptions.size()) + " options).");
        for ( ConfigBase config : this.configOptions ) {
            JsonObject inner = json;
            String[] path = config.getPath().split("\\.");
            
            int p;
            for (p = 0; p < (path.length - 1); p++) {
                String seg = path[p];
                
                if (inner.has(seg) && inner.get(seg).isJsonObject()) {
                    inner = inner.getAsJsonObject(seg);
                } else {
                    JsonObject segObj = new JsonObject();
                    inner.add(seg, segObj);
                    inner = segObj;
                }
            }
            
            // Set value for config (From file)
            if ( inner.has(path[p]) )
                config.set(inner.get(path[p]), true);
            else
                inner.add(path[p], config.getElement());
        }
        
        return json;
    }
    @Override
    protected JsonObject saveToJSON() {
        final JsonObject baseObject = super.saveToJSON();
        
        JsonObject modules = new JsonObject();
        this.configModules.forEach((key, module) -> {
            // Stuff goes here
            modules.add(key, module.saveToJSON());
        });
        baseObject.add(SewConfig.MODULE_KEY, modules);
        
        return baseObject;
    }
    
    public static void afterReload(Runnable runnable) {
        SewConfig.INSTANCE.reloadFunctions.add(runnable);
    }
    
    /*
     * Special handlers
     */
    public static @NotNull Map<Item, Integer> getItemMap(@NotNull JsonElement root) {
        Map<Item, Integer> out = new HashMap<>();
        
        // Parse each object in the array
        JsonObject list = root.getAsJsonObject();
        for ( Map.Entry<String, JsonElement> row : list.entrySet() ) {
            String token = row.getKey();
            Optional<Item> optional = Registries.ITEM.getOrEmpty(new Identifier(token));
            
            int count = row.getValue().getAsInt();
            optional.ifPresentOrElse(
                item -> out.put(item, count),
                () -> CoreMod.logError("Unable to find starting item \"" + token + "\" in the item registry.")
            );
        }
        
        return out;
    }
    
    public static @NotNull Map<Item, Integer> getItemDespawnMap(@NotNull JsonElement root) {
        Map<Item, Integer> out = new HashMap<>();
        Pattern pattern = Pattern.compile("^([0-9]+)([smhd])$");
        
        // Parse each object in the array
        JsonObject list = root.getAsJsonObject();
        for ( Map.Entry<String, JsonElement> row : list.entrySet() ) {
            String token = row.getKey();
            Optional<Item> optional = Registries.ITEM.getOrEmpty(new Identifier(token));
            
            JsonElement element = row.getValue();
            
            optional.ifPresentOrElse(item -> {
                Integer ticks = EntityUtils.DEFAULT_DESPAWN_TICKS;
                if (element instanceof JsonNull)
                    ticks = null;
                else if (element instanceof JsonPrimitive primitive) {
                    if (primitive.isNumber())
                        ticks = primitive.getAsInt();
                    else if (primitive.isString()) {
                        String string = primitive.getAsString();
                        Matcher matcher = pattern.matcher(string);
                        if (!matcher.matches())
                            ticks = primitive.getAsInt();
                        else {
                            int duration = Integer.parseInt(matcher.group(1));
                            ticks = IntUtils.convertToTicks(duration, IntUtils.getTimeUnit(matcher.group(2)));
                        }
                    }
                }
                
                out.put(item, ticks);
            }, () -> CoreMod.logError(""));
        }
        
        return out;
    }
    
    public static @NotNull LoggingIntervals getAsTimeInterval(@NotNull JsonElement element) {
        if (!LoggingIntervals.contains(element.getAsString()))
            throw new RuntimeException( "Unacceptable time interval \"" + element.getAsString() + "\"" );
        return LoggingIntervals.valueOf(element.getAsString().toUpperCase());
    }
    
    private static JsonElement sortObject(JsonElement element) {
        // If not an object, no sort
        if (!(element instanceof JsonObject object))
            return element;
        
        // Create our new blank object
        JsonObject sorted = new JsonObject();
        List<String> keys = new ArrayList<>();
        
        // Get all keys from the main object
        Set<Map.Entry<String, JsonElement>> set = object.entrySet();
        for ( Map.Entry<String, JsonElement> pair : set ) {
            if ( !SewConfig.VERSION_KEY.equals(pair.getKey()) && !SewConfig.MODULE_KEY.equals(pair.getKey()) )
                keys.add(pair.getKey());
        }
        
        // Add them back in sorted order
        Collections.sort(keys);
        
        keys.add(0, SewConfig.VERSION_KEY); // Add Version - Should always be first in the sorted list
        keys.add(SewConfig.MODULE_KEY); // Add Modules - Should always be last in the sorted list
        
        for ( String key : keys ) {
            if ( object.has(key) )
                sorted.add(key, object.get(key));
        }
        
        return sorted;
    }
    
}
