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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.defaults.ConfigBoolean;
import net.TheElm.project.config.defaults.ConfigString;
import net.TheElm.project.objects.ChatFormat;
import net.TheElm.project.protections.logging.EventLogger.LoggingIntervals;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SewConfig {
    private static final SewConfig INSTANCE;
    private static final String VERSION_KEY = "config_version";
    private final boolean fileExists;
    
    static {
        INSTANCE = new SewConfig();
    }
    
    private final List<Runnable> reloadFunctions = new ArrayList<>();
    private final List<ConfigBase> configOptions = new ArrayList<>();
    
    // Mod version
    public static final ConfigOption<String> CONFIG_VERSION = addConfig(new ConfigString(VERSION_KEY, CoreMod.getModVersion()));
    
    // Database
    public static final ConfigOption<Boolean> DB_LITE = addConfig(new ConfigBoolean("database.sqlite", true));
    
    /*
     * Database handling
     */
    
    public static final ConfigOption<String> DB_HOST = addConfig(new ConfigString("database.host", ""));
    public static final ConfigOption<String> DB_NAME = addConfig(new ConfigString("database.name", ""));
    public static final ConfigOption<String> DB_USER = addConfig(new ConfigString("database.user", ""));
    public static final ConfigOption<String> DB_PASS = addConfig(new ConfigString("database.pass", ""));
    public static final ConfigOption<Integer> DB_PORT = addConfig(new ConfigOption<>("database.port", 3306, JsonElement::getAsInt));
    
    /*
     * Chat Booleans
     */
    
    public static final ConfigOption<Boolean> CHAT_MODIFY = addConfig(new ConfigBoolean("chat.modify", true));
    public static final ConfigOption<Boolean> CHAT_SHOW_TOWNS = addConfig(new ConfigBoolean("chat.show_towns", true));
    public static final ConfigOption<ChatFormat> CHAT_FORMAT = addConfig(new ConfigOption<>("chat.format", ChatFormat.parse("[%w] %n: %m"), ChatFormat::parse, ChatFormat::serializer));
    
    public static final ConfigOption<Boolean> CHAT_MUTE_SELF = addConfig(new ConfigBoolean("chat.mute.personal_mute", true));
    public static final ConfigOption<Boolean> CHAT_MUTE_OP = addConfig(new ConfigBoolean("chat.mute.moderator_mute", true));
    
    /*
     * Death chests
     */
    
    public static final ConfigOption<Boolean> DO_DEATH_CHESTS = addConfig(new ConfigBoolean("death_chest.enabled", true));
    public static final ConfigOption<Integer> MAX_DEATH_SCAN = addConfig(new ConfigOption<>("death_chest.max_distance", 4, JsonElement::getAsInt));
    public static final ConfigOption<Integer> MAX_DEATH_ELEVATION = addConfig(new ConfigOption<>("death_chest.max_elevation", 256, JsonElement::getAsInt));
    public static final ConfigOption<Boolean> PRINT_DEATH_CHEST_LOC = addConfig(new ConfigBoolean("death_chest.print_coordinates", true));
    
    // Player Combat
    public static final ConfigOption<Boolean> PVP_DISABLE_DEATH_CHEST = addConfig(new ConfigOption<>("player.pvp.no_death_chest", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> PVP_COMBAT_LOG = addConfig(new ConfigOption<>("player.pvp.kill_on_logout", false, JsonElement::getAsBoolean));
    
    /*
     * Player Combat
     */
    
    public static final ConfigOption<Integer> PVP_COMBAT_SECONDS = addConfig(new ConfigOption<>("player.pvp.combat_seconds", 30, JsonElement::getAsInt));
    
    /*
     * Naming
     */
    
    public static final ConfigOption<Boolean> DO_PLAYER_NICKS = addConfig(new ConfigOption<>("player.nicks", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> NICKNAME_COST = addConfig(new ConfigOption<>("player.nick_cost", 0, JsonElement::getAsInt));
    
    /*
     * Primary Functions Booleans
     */
    
    public static final ConfigOption<Boolean> DO_CLAIMS = addConfig(new ConfigOption<>("claims.enabled", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> LIGHT_UP_SPAWN = new ConfigOption<>("claims.light.spawn", false, JsonElement::getAsBoolean);
    public static final ConfigOption<Boolean> CLAIM_CREATIVE_BYPASS = addConfig(new ConfigOption<>("claims.creative_bypass", true, JsonElement::getAsBoolean));
    public static final ConfigOption<String> NAME_SPAWN = addConfig(new ConfigOption<>("claims.name.spawn", "Spawn", JsonElement::getAsString));
    public static final ConfigOption<String> NAME_WILDERNESS = addConfig(new ConfigOption<>("claims.name.wild", "Wilderness", JsonElement::getAsString));
    
    /*
     * Claiming Options
     */
    
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_ENDERMAN = addConfig(new ConfigOption<>("claims.allow_player_override.griefing.enderman", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_CREEPER = addConfig(new ConfigOption<>("claims.allow_player_override.griefing.creeper", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_GHAST = addConfig(new ConfigOption<>("claims.allow_player_override.griefing.ghast", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_PLAYER_COMBAT = addConfig(new ConfigOption<>("claims.allow_player_override.player_pvp", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_CROP_AUTOREPLANT = addConfig(new ConfigOption<>("claims.allow_player_override.crop_autoreplant", true, JsonElement::getAsBoolean));
    
    public static final ConfigOption<Boolean> CLAIM_ALLOW_TREE_CAPACITATOR = new ConfigOption<>("claims.allow_player_override.tree_capacitate", CoreMod.isDebugging(), JsonElement::getAsBoolean);
    public static final ConfigOption<Boolean> CLAIM_ALLOW_VEIN_MINER = new ConfigOption<>("claims.allow_player_override.vein_miner", CoreMod.isDebugging(), JsonElement::getAsBoolean);
    
    /*
     * Protections
     */
    
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_SPAWN = addConfig(new ConfigOption<>("claims.op_level.spawn", 1, JsonElement::getAsInt));
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_OTHER = addConfig(new ConfigOption<>("claims.op_level.other_player", 1, JsonElement::getAsInt));
    
    // Claim Regions
    public static final ConfigOption<Integer> MAXIMUM_REGION_WIDTH = addConfig(new ConfigOption<>("claims.regions.max_width", 32, JsonElement::getAsInt));
    public static final ConfigOption<Integer> MINIMUM_REGION_WIDTH = addConfig(new ConfigOption<>("claims.regions.min_width", 3, JsonElement::getAsInt));
    
    /*
     * Logging
     */
    
    public static final ConfigOption<Boolean> LOG_BLOCKS_BREAKING = addConfig(new ConfigOption<>("logging.blocks.break", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> LOG_BLOCKS_PLACING = addConfig(new ConfigOption<>("logging.blocks.place", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> LOG_CHUNKS_CLAIMED = addConfig(new ConfigOption<>("logging.chunks.claimed", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> LOG_CHUNKS_UNCLAIMED = addConfig(new ConfigOption<>("logging.chunks.wilderness", false, JsonElement::getAsBoolean));
    public static final ConfigOption<LoggingIntervals> LOG_RESET_INTERVAL = addConfig(new ConfigOption<>("logging.reset.interval", LoggingIntervals.DAY, SewConfig::getAsTimeInterval));
    public static final ConfigOption<Long> LOG_RESET_TIME = addConfig(new ConfigOption<>("logging.reset.time", 7L, JsonElement::getAsLong));
    public static final ConfigOption<Integer> LOG_VIEW_OP_LEVEL = addConfig(new ConfigOption<>("logging.read.op_level", 1, JsonElement::getAsInt));
    
    /*
     * Starting items
     */
    
    public static final ConfigOption<Map<Item, Integer>> STARTING_ITEMS = addConfig(new ConfigOption<>("player.starting_items", new HashMap<>(), SewConfig::getItemMap));
    public static final ConfigOption<Boolean> FRIEND_WHITELIST = addConfig(new ConfigOption<>("server.whitelist.friends_add_friends", false, JsonElement::getAsBoolean));
    
    public static final ConfigOption<Boolean> START_WITH_RECIPES = addConfig(new ConfigOption<>("player.recipes.unlock_all", false, JsonElement::getAsBoolean));
    
    /*
     * Claiming
     */
    
    public static final ConfigOption<Integer> PLAYER_CLAIMS_LIMIT = addConfig(new ConfigOption<>("claims.players.limit", 40, JsonElement::getAsInt));
    public static final ConfigOption<Boolean> PLAYER_LIMIT_INCREASE = addConfig(new ConfigOption<>("claims.players.limit_increase.enabled", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_LIMIT = addConfig(new ConfigOption<>("claims.players.limit_increase.maximum", -1, JsonElement::getAsInt));
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_COST = addConfig(new ConfigOption<>("claims.players.limit_increase.cost", 200, JsonElement::getAsInt));
    
    /*
     * Inventory
     */
    
    public static final ConfigOption<Boolean> TOTEM_ANYWHERE = addConfig(new ConfigOption<>("player.inventory.totem_of_undying", true, JsonElement::getAsBoolean));
    
    /*
     * Backpacks
     */
    
    public static final ConfigOption<Boolean> ALLOW_BACKPACKS = addConfig(new ConfigOption<>("player.inventory.backpacks.enable", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> BACKPACK_SEQUENTIAL = addConfig(new ConfigOption<>("player.inventory.backpacks.require_sequential", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> BACKPACK_STARTING_ROWS = addConfig(new ConfigOption<>("player.inventory.backpacks.starting_rows", 0, JsonElement::getAsInt));
    
    // Towns
    public static final ConfigOption<Integer> TOWN_FOUND_COST = addConfig(new ConfigOption<>("claims.towns.cost", 500, JsonElement::getAsInt));
    public static final ConfigOption<Integer> TOWN_CLAIMS_LIMIT = addConfig(new ConfigOption<>("claims.towns.limit", 200, JsonElement::getAsInt));
    
    public static final ConfigOption<Boolean> TOWN_VILLAGERS_INCLUDE = addConfig(new ConfigOption<>("claims.towns.villagers.include", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> TOWN_VILLAGERS_VALUE = addConfig(new ConfigOption<>("claims.towns.villagers.value", 3, JsonElement::getAsInt));
    
    /*
     * Money options
     */
    
    public static final ConfigOption<Boolean> DO_MONEY = addConfig(new ConfigOption<>("money.enabled", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> STARTING_MONEY = addConfig(new ConfigOption<>("money.starting", 0, JsonElement::getAsInt));
    public static final ConfigOption<Integer> DAILY_ALLOWANCE = addConfig(new ConfigOption<>("money.daily_reward", 0, JsonElement::getAsInt));
    
    /*
     * Sleep
     */
    
    public static final ConfigOption<Boolean> DO_SLEEP_VOTE = addConfig(new ConfigOption<>("sleep.voting", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> SLEEP_PERCENT = addConfig(new ConfigOption<>("sleep.percent", 50, JsonElement::getAsInt));
    
    /*
     * Warping
     */
    
    public static final ConfigOption<Integer> WARP_MAX_DISTANCE = addConfig(new ConfigOption<>( "warp.max_distance", 1000000, JsonElement::getAsInt));
    public static final ConfigOption<Integer> WARP_WAYSTONE_COST = addConfig(new ConfigOption<>( "warp.waystone.cost", 2000, JsonElement::getAsInt));
    
    /*
     * Ender Dragon Options
     */
    
    public static final ConfigOption<Integer> DRAGON_PLAYERS = addConfig(new ConfigOption<>("bosses.dragon.players", 5, JsonElement::getAsInt));
    public static final ConfigOption<Boolean> DRAGON_LOOT_END_ITEMS = addConfig(new ConfigOption<>("bosses.dragon.loot.end_loot", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> DRAGON_LOOT_RARE_BOOKS = addConfig(new ConfigOption<>("bosses.dragon.loot.rare_books", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> DRAGON_ADDITIONAL_HEALTH = addConfig(new ConfigOption<>("bosses.dragon.health_boot", 100, JsonElement::getAsInt));
    
    /*
     * Commands
     */
    
    public static final ConfigOption<Boolean> COMMAND_MODS_LIST = addConfig(new ConfigOption<>("commands.mods_list", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> COMMAND_WARP_TPA = addConfig(new ConfigOption<>("commands.warp_tpa", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> COMMAND_EQUIPMENT = addConfig(new ConfigOption<>("commands.misc.equipment", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> COMMAND_SHRUG = addConfig(new ConfigOption<>("commands.misc.shrug", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> COMMAND_TABLEFLIP = addConfig(new ConfigOption<>("commands.misc.tableflip", true, JsonElement::getAsBoolean));
    
    /*
     * Server list MOTD
     */
    public static final ConfigArray<String> SERVER_MOTD_LIST = addConfig(new ConfigArray<>("server.motd", JsonElement::getAsString));
    public static final ConfigArray<String> SERVER_ICON_LIST = addConfig(new ConfigArray<>("server.icons", JsonElement::getAsString));
    
    /*
     * Permission options
     */
    
    public static final ConfigOption<Boolean> HANDLE_PERMISSIONS = addConfig(new ConfigOption<>("server.permissions.enabled", true, JsonElement::getAsBoolean));
    
    /*
     * Miscellaneous
     */
    
    public static final ConfigOption<Boolean> OVERWORLD_PORTAL_LOC = addConfig(new ConfigOption<>("fun.world.portal_fix.overworld", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> NETHER_PORTAL_LOC = addConfig(new ConfigOption<>("fun.world.portal_fix.nether", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> LIMIT_SKELETON_ARROWS = addConfig(new ConfigOption<>("fun.mobs.skeletons.limit_arrows", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> COWS_EAT_MYCELIUM = addConfig(new ConfigOption<>("fun.mobs.cows.eat_mycelium", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> EXTINGUISH_CAMPFIRES = addConfig(new ConfigOption<>("fun.world.extinguish_campfires", true, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> ANVIL_DISABLE_COST_LIMIT = addConfig(new ConfigOption<>("fun.anvil.disable_level_cap", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> ANVIL_DISABLE_COST_REPAIR = addConfig(new ConfigOption<>("fun.anvil.disable_repair_increment", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> PREVENT_NETHER_ENDERMEN = addConfig(new ConfigOption<>("fun.mobs.enderman.no_nether", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> NETHER_INFINITE_LAVA = addConfig(new ConfigOption<>("fun.world.nether.infinite_lava", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Boolean> RANDOM_NAME_VILLAGERS = addConfig(new ConfigOption<>("fun.mobs.villagers.random_names", true, JsonElement::getAsBoolean));
    
    public static final ConfigOption<String> CALENDAR_YEAR_EPOCH = addConfig(new ConfigOption<>("fun.calendar.epoch", "After Creation", JsonElement::getAsString));
    public static final ConfigOption<Integer> CALENDAR_DAYS = addConfig(new ConfigOption<>("fun.calendar.days", 365, JsonElement::getAsInt));
    
    /*
     * Mob Spawners
     */
    
    public static final ConfigOption<Boolean> SILK_TOUCH_SPAWNERS = addConfig(new ConfigOption<>("fun.spawners.silk_touch", false, JsonElement::getAsBoolean));
    public static final ConfigOption<Integer> SPAWNER_PICKUP_DAMAGE = addConfig(new ConfigOption<>("fun.spawners.silk_touch_damage", 390, JsonElement::getAsInt));
    public static final ConfigOption<Boolean> SPAWNER_ABSORB_MOBS = addConfig(new ConfigOption<>("fun.spawners.absorb_mob_souls", true, JsonElement::getAsBoolean));
    
    private SewConfig() {
        File config = null;
        try {
            config = this.getConfigFile();
            
            /*
             * Read the existing config
             */
            JsonElement loaded = this.reload(config);
            
            /*
             * Save any new values
             */
            this.saveToFile(config, loaded);
            
        } catch (IOException e) {
            CoreMod.logError( e );
        }
        
        this.fileExists = ((config != null) && config.exists());
    }
    
    public static boolean preExisting() {
        return INSTANCE.fileExists;
    }
    public static @Nullable JsonElement reload() throws IOException {
        try {
            return INSTANCE.reload(INSTANCE.getConfigFile());
        } finally {
            for (Runnable r : INSTANCE.reloadFunctions)
                r.run();
        }
    }
    public final @Nullable JsonElement reload(File config) throws IOException {
        CoreMod.logInfo( "Loading configuration file." );
        
        //Read the existing config
        return this.loadFromJSON(this.loadFromFile(config));
    }
    
    private static <T> ConfigOption<T> addConfig( ConfigOption<T> config ) {
        // Check for a non-empty path for saving
        if (config.getPath().isEmpty()) throw new RuntimeException("Config Option path should not be empty");
        
        // Store the config
        INSTANCE.configOptions.add(config);
        return config;
    }
    private static <T> ConfigArray<T> addConfig( ConfigArray<T> config ) {
        INSTANCE.configOptions.add(config);
        return config;
    }
    
    public static <T> void set( ConfigOption<T> config, JsonElement value ) {
        config.set(value);
    }
    public static <T> void set( ConfigOption<T> config, JsonElement value, boolean wasDefined ) {
        config.set(value, wasDefined);
    }
    
    
    public static <T> T get( ConfigOption<T> config ) {
        return config.get();
    }
    public static <T> List<T> get( ConfigArray<T> config ) {
        return config.get();
    }
    public static <T> T getRandom( ConfigArray<T> config ) {
        return config.getRandom();
    }
    public static <T> T get( ConfigArray<T> config, int pos ) {
        return config.get(pos);
    }
    
    public static boolean isTrue( ConfigOption config ) {
        Object val = config.get();
        if (val instanceof Boolean)
            return (Boolean)val;
        if (val instanceof Number)
            return ((Number)val).longValue() >= 0;
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
    
    /*
     * File save / load
     */
    private static @NotNull File getConfigFile() throws IOException {
        // Load from the folder
        final File dir = CoreMod.getConfDir();
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
            fw.append(gson.toJson(sortObject( json )));
        }
    }
    private static JsonObject loadFromFile(File configFile) throws FileNotFoundException {
        JsonParser jp = new JsonParser();
        JsonElement element = jp.parse(new FileReader(configFile));
        return element.getAsJsonObject();
    }
    
    private JsonElement loadFromJSON(JsonObject json) {
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
                    inner.add( seg, segObj );
                    inner = segObj;
                }
            }
            
            // Set value for config (From file)
            if ( inner.has(path[p]) )
                config.set(inner.get(path[p]), true);
            else
                inner.add( path[p], config.getElement() );
        }
        
        return json;
    }
    private JsonObject saveToJSON() {
        final JsonObject baseObject = new JsonObject();
        
        for ( ConfigBase config : this.configOptions ) {
            JsonObject inner = baseObject;
            
            String[] path = config.getPath().split("\\.");
            
            int p;
            for ( p = 0; p < ( path.length - 1 ); p++ ) {
                String seg = path[p];
                
                if (inner.has(seg) && inner.get(seg).isJsonObject()) {
                    inner = inner.getAsJsonObject(seg);
                } else {
                    JsonObject segObj = new JsonObject();
                    inner.add( seg, segObj );
                    inner = segObj;
                }
            }
            
            inner.add( path[p], config.getElement() );
        }
        
        return baseObject;
    }
    
    public static void afterReload(Runnable runnable) {
        INSTANCE.reloadFunctions.add(runnable);
    }
    
    /*
     * Special handlers
     */
    private static Map<Item, Integer> getItemMap(JsonElement element) {
        Map<Item, Integer> out = new HashMap<>();
        
        // Parse each object in the array
        JsonObject list = element.getAsJsonObject();
        for ( Map.Entry<String, JsonElement> row : list.entrySet() ) {
            String token = row.getKey();
            int count = row.getValue().getAsInt();
            
            if (count > 0) {
                out.put(
                    Registry.ITEM.get(new Identifier(token)),
                    count
                );
            }
        }
        
        return out;
    }
    
    private static LoggingIntervals getAsTimeInterval(JsonElement element) {
        if (!LoggingIntervals.contains(element.getAsString()))
            throw new RuntimeException( "Unacceptable time interval \"" + element.getAsString() + "\"" );
        return LoggingIntervals.valueOf(element.getAsString().toUpperCase());
    }
    
    private static JsonElement sortObject(JsonElement element) {
        // If not an object, no sort
        if (!(element instanceof JsonObject))
            return element;
        
        // Get our element as an object
        JsonObject object = element.getAsJsonObject();
        
        // Create our new blank object
        JsonObject sorted = new JsonObject();
        List<String> keys = new ArrayList<>();
        
        // Get all keys from the main object
        Set<Map.Entry<String, JsonElement>> set = object.entrySet();
        for ( Map.Entry<String, JsonElement> pair : set ) {
            if ( !VERSION_KEY.equals( pair.getKey() ) )
                keys.add( pair.getKey() );
        }
        
        // Add them back in sorted order
        Collections.sort( keys );
        
        // Add Version - Should always be first in the sorted list
        keys.add( 0, VERSION_KEY);
        
        for ( String key : keys ) {
            if ( object.has( key ) )
                sorted.add( key, object.get( key ) );
        }
        
        return sorted;
    }
    
}
