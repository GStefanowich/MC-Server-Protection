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

public final class SewingMachineConfig {
    public static final SewingMachineConfig INSTANCE;
    private final String version_key = "config_version";
    private final boolean fileExists;
    
    static {
        INSTANCE = new SewingMachineConfig();
    }
    
    private final List<Runnable> reloadFunctions = new ArrayList<>();
    private final List<ConfigBase> configOptions = new ArrayList<>();
    
    // Mod version
    public final ConfigOption<String> CONFIG_VERSION;
    
    // Database
    public final ConfigOption<Boolean> DB_LITE;
    public final ConfigOption<String> DB_HOST;
    public final ConfigOption<String> DB_NAME;
    public final ConfigOption<String> DB_USER;
    public final ConfigOption<String> DB_PASS;
    public final ConfigOption<Integer> DB_PORT;
    
    // Chat
    public final ConfigOption<Boolean> CHAT_MODIFY;
    public final ConfigOption<Boolean> CHAT_SHOW_TOWNS;
    public final ConfigOption<ChatFormat> CHAT_FORMAT;
    
    public final ConfigOption<Boolean> CHAT_MUTE_SELF;
    public final ConfigOption<Boolean> CHAT_MUTE_OP;
    
    // Player Death Chests
    public final ConfigOption<Boolean> DO_DEATH_CHESTS;
    public final ConfigOption<Integer> MAX_DEATH_SCAN;
    public final ConfigOption<Integer> MAX_DEATH_ELEVATION;
    public final ConfigOption<Boolean> PRINT_DEATH_CHEST_LOC;
    
    // Player Combat
    public final ConfigOption<Boolean> PVP_DISABLE_DEATH_CHEST;
    public final ConfigOption<Boolean> PVP_COMBAT_LOG;
    public final ConfigOption<Integer> PVP_COMBAT_SECONDS;
    
    // Player nicks
    public final ConfigOption<Boolean> DO_PLAYER_NICKS;
    public final ConfigOption<Integer> NICKNAME_COST;
    
    // Claiming
    public final ConfigOption<Boolean> DO_CLAIMS;
    public final ConfigOption<Boolean> LIGHT_UP_SPAWN;
    public final ConfigOption<Boolean> CLAIM_CREATIVE_BYPASS;
    public final ConfigOption<String> NAME_SPAWN;
    public final ConfigOption<String> NAME_WILDERNESS;
    
    // Claiming Enabled Options
    public final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_ENDERMAN;
    public final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_CREEPER;
    public final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_GHAST;
    public final ConfigOption<Boolean> CLAIM_ALLOW_PLAYER_COMBAT;
    public final ConfigOption<Boolean> CLAIM_ALLOW_CROP_AUTOREPLANT;
    
    public final ConfigOption<Boolean> CLAIM_ALLOW_TREE_CAPACITATOR;
    public final ConfigOption<Boolean> CLAIM_ALLOW_VEIN_MINER;
    
    public final ConfigOption<Integer> CLAIM_OP_LEVEL_SPAWN;
    public final ConfigOption<Integer> CLAIM_OP_LEVEL_OTHER;
    
    // Claim Regions
    public final ConfigOption<Integer> MAXIMUM_REGION_WIDTH;
    public final ConfigOption<Integer> MINIMUM_REGION_WIDTH;
    
    // Logging
    public final ConfigOption<Boolean> LOG_BLOCKS_BREAKING;
    public final ConfigOption<Boolean> LOG_BLOCKS_PLACING;
    public final ConfigOption<Boolean> LOG_CHUNKS_CLAIMED;
    public final ConfigOption<Boolean> LOG_CHUNKS_UNCLAIMED;
    public final ConfigOption<LoggingIntervals> LOG_RESET_INTERVAL;
    public final ConfigOption<Long> LOG_RESET_TIME;
    public final ConfigOption<Integer> LOG_VIEW_OP_LEVEL;
    
    // Players
    public final ConfigOption<Map<Item, Integer>> STARTING_ITEMS;
    public final ConfigOption<Boolean> FRIEND_WHITELIST;
    
    public final ConfigOption<Boolean> START_WITH_RECIPES;
    
    public final ConfigOption<Integer> PLAYER_CLAIMS_LIMIT;
    public final ConfigOption<Boolean> PLAYER_LIMIT_INCREASE;
    public final ConfigOption<Integer> PLAYER_CLAIM_BUY_LIMIT;
    public final ConfigOption<Integer> PLAYER_CLAIM_BUY_COST;
    
    // Player Inventory
    public final ConfigOption<Boolean> TOTEM_ANYWHERE;
    
    // Backpacks
    public final ConfigOption<Boolean> ALLOW_BACKPACKS;
    public final ConfigOption<Boolean> BACKPACK_SEQUENTIAL;
    public final ConfigOption<Integer> BACKPACK_STARTING_ROWS;
    
    // Towns
    public final ConfigOption<Integer> TOWN_FOUND_COST;
    public final ConfigOption<Integer> TOWN_CLAIMS_LIMIT;
    
    public final ConfigOption<Boolean> TOWN_VILLAGERS_INCLUDE;
    public final ConfigOption<Integer> TOWN_VILLAGERS_VALUE;
    
    // Economy
    public final ConfigOption<Boolean> DO_MONEY;
    public final ConfigOption<Integer> STARTING_MONEY;
    public final ConfigOption<Integer> DAILY_ALLOWANCE;
    
    // Sleeping
    public final ConfigOption<Boolean> DO_SLEEP_VOTE;
    public final ConfigOption<Integer> SLEEP_PERCENT;
    
    // Warp options
    public final ConfigOption<Integer> WARP_MAX_DISTANCE;
    public final ConfigOption<Integer> WARP_WAYSTONE_COST;
    
    // End
    public final ConfigOption<Integer> DRAGON_PLAYERS;
    public final ConfigOption<Boolean> DRAGON_LOOT_END_ITEMS;
    public final ConfigOption<Boolean> DRAGON_LOOT_RARE_BOOKS;
    public final ConfigOption<Integer> DRAGON_ADDITIONAL_HEALTH;
        
    // Commands
    public final ConfigOption<Boolean> COMMAND_MODS_LIST;
    public final ConfigOption<Boolean> COMMAND_WARP_TPA;
    public final ConfigOption<Boolean> COMMAND_EQUIPMENT;
    public final ConfigOption<Boolean> COMMAND_SHRUG;
    public final ConfigOption<Boolean> COMMAND_TABLEFLIP;
    
    // MOTD
    public final ConfigArray<String> SERVER_MOTD_LIST;
    public final ConfigArray<String> SERVER_ICON_LIST;
    
    // Permissions
    public final ConfigOption<Boolean> HANDLE_PERMISSIONS;
    
    // Miscellaneous
    public final ConfigOption<Boolean> OVERWORLD_PORTAL_LOC;
    public final ConfigOption<Boolean> NETHER_PORTAL_LOC;
    public final ConfigOption<Boolean> LIMIT_SKELETON_ARROWS;
    public final ConfigOption<Boolean> COWS_EAT_MYCELIUM;
    public final ConfigOption<Boolean> EXTINGUISH_CAMPFIRES;
    public final ConfigOption<Boolean> ANVIL_DISABLE_COST_LIMIT;
    public final ConfigOption<Boolean> ANVIL_DISABLE_COST_REPAIR;
    public final ConfigOption<Boolean> PREVENT_NETHER_ENDERMEN;
    public final ConfigOption<Boolean> NETHER_INFINITE_LAVA;
    public final ConfigOption<Boolean> RANDOM_NAME_VILLAGERS;
    
    public final ConfigOption<String> CALENDAR_YEAR_EPOCH;
    public final ConfigOption<Integer> CALENDAR_DAYS;
    
    public final ConfigOption<Boolean> SILK_TOUCH_SPAWNERS;
    public final ConfigOption<Integer> SPAWNER_PICKUP_DAMAGE;
    public final ConfigOption<Boolean> SPAWNER_ABSORB_MOBS;
    
    private SewingMachineConfig() {
        // Initialize all configurations
        this.CONFIG_VERSION = this.addConfig( new ConfigOption<>(this.version_key, CoreMod.getModVersion(), JsonElement::getAsString));
        
        /*
         * Database handling
         */
        this.DB_LITE = this.addConfig(new ConfigOption<>("database.sqlite", true, JsonElement::getAsBoolean));
        this.DB_HOST = this.addConfig(new ConfigOption<>("database.host", "", JsonElement::getAsString));
        this.DB_NAME = this.addConfig(new ConfigOption<>("database.name", "", JsonElement::getAsString));
        this.DB_USER = this.addConfig(new ConfigOption<>("database.user", "", JsonElement::getAsString));
        this.DB_PASS = this.addConfig(new ConfigOption<>("database.pass", "", JsonElement::getAsString));
        this.DB_PORT = this.addConfig(new ConfigOption<>("database.port", 3306, JsonElement::getAsInt));
        
        /*
         * Primary Functions Booleans
         */
        this.DO_CLAIMS = this.addConfig(new ConfigOption<>("claims.enabled", true, JsonElement::getAsBoolean));
        this.CLAIM_CREATIVE_BYPASS = this.addConfig(new ConfigOption<>("claims.creative_bypass", true, JsonElement::getAsBoolean));
        
        this.LIGHT_UP_SPAWN = new ConfigOption<>("claims.light.spawn", false, JsonElement::getAsBoolean);
        
        /*
         * Chat Booleans
         */
        this.CHAT_MODIFY = this.addConfig(new ConfigOption<>("chat.modify", true, JsonElement::getAsBoolean));
        this.CHAT_FORMAT = this.addConfig(new ConfigOption<>("chat.format", ChatFormat.parse("[%w] %n: %m"), ChatFormat::parse, ChatFormat::serializer));
        
        this.CHAT_MUTE_SELF = this.addConfig(new ConfigOption<>("chat.mute.personal_mute", true, JsonElement::getAsBoolean));
        this.CHAT_MUTE_OP = this.addConfig(new ConfigOption<>("chat.mute.moderator_mute", true, JsonElement::getAsBoolean));
        
        this.CHAT_SHOW_TOWNS = this.addConfig(new ConfigOption<>("chat.show_towns", true, JsonElement::getAsBoolean));
        
        /*
         * Sleep
         */
        this.DO_SLEEP_VOTE = this.addConfig(new ConfigOption<>("sleep.voting", true, JsonElement::getAsBoolean));
        this.SLEEP_PERCENT = this.addConfig(new ConfigOption<>("sleep.percent", 50, JsonElement::getAsInt));
        
        /*
         * Money options
         */
        this.DO_MONEY = this.addConfig(new ConfigOption<>("money.enabled", true, JsonElement::getAsBoolean));
        this.STARTING_MONEY = this.addConfig(new ConfigOption<>("money.starting", 0, JsonElement::getAsInt));
        this.DAILY_ALLOWANCE = this.addConfig(new ConfigOption<>("money.daily_reward", 0, JsonElement::getAsInt));
        
        /*
         * Starting items
         */
        this.STARTING_ITEMS = this.addConfig(new ConfigOption<>("player.starting_items", new HashMap<>(), this::getItemMap));
        
        /*
         * Death chests
         */
        this.DO_DEATH_CHESTS = this.addConfig(new ConfigOption<>("death_chest.enabled", true, JsonElement::getAsBoolean));
        this.MAX_DEATH_SCAN = this.addConfig(new ConfigOption<>("death_chest.max_distance", 4, JsonElement::getAsInt));
        this.MAX_DEATH_ELEVATION = this.addConfig(new ConfigOption<>("death_chest.max_elevation", 256, JsonElement::getAsInt));
        this.PRINT_DEATH_CHEST_LOC = this.addConfig(new ConfigOption<>("death_chest.print_coordinates", true, JsonElement::getAsBoolean));
        
        /*
         * Player Combat
         */
        this.PVP_COMBAT_SECONDS = this.addConfig(new ConfigOption<>("player.pvp.combat_seconds", 30, JsonElement::getAsInt));
        this.PVP_DISABLE_DEATH_CHEST = this.addConfig(new ConfigOption<>("player.pvp.no_death_chest", true, JsonElement::getAsBoolean));
        this.PVP_COMBAT_LOG = this.addConfig(new ConfigOption<>("player.pvp.kill_on_logout", false, JsonElement::getAsBoolean));
        
        /*
         * Inventory
         */
        this.TOTEM_ANYWHERE = this.addConfig(new ConfigOption<>("player.inventory.totem_of_undying", true, JsonElement::getAsBoolean));
        
        /*
         * Backpacks
         */
        this.ALLOW_BACKPACKS = this.addConfig(new ConfigOption<>("player.inventory.backpacks.enable", true, JsonElement::getAsBoolean));
        this.BACKPACK_STARTING_ROWS = this.addConfig(new ConfigOption<>("player.inventory.backpacks.starting_rows", 0, JsonElement::getAsInt));
        this.BACKPACK_SEQUENTIAL = this.addConfig(new ConfigOption<>("player.inventory.backpacks.require_sequential", true, JsonElement::getAsBoolean));
        
        /*
         * Naming
         */
        this.DO_PLAYER_NICKS = this.addConfig(new ConfigOption<>("player.nicks", true, JsonElement::getAsBoolean));
        this.NICKNAME_COST = this.addConfig(new ConfigOption<>("player.nick_cost", 0, JsonElement::getAsInt));
        
        this.NAME_SPAWN = this.addConfig(new ConfigOption<>("claims.name.spawn", "Spawn", JsonElement::getAsString));
        this.NAME_WILDERNESS = this.addConfig(new ConfigOption<>("claims.name.wild", "Wilderness", JsonElement::getAsString));
        
        /*
         * Protections
         */
        this.CLAIM_OP_LEVEL_SPAWN = this.addConfig(new ConfigOption<>("claims.op_level.spawn", 1, JsonElement::getAsInt));
        this.CLAIM_OP_LEVEL_OTHER = this.addConfig(new ConfigOption<>("claims.op_level.other_player", 1, JsonElement::getAsInt));
        
        this.MAXIMUM_REGION_WIDTH = this.addConfig(new ConfigOption<>("claims.regions.max_width", 32, JsonElement::getAsInt));
        this.MINIMUM_REGION_WIDTH = this.addConfig(new ConfigOption<>("claims.regions.min_width", 3, JsonElement::getAsInt));
        
        /*
         * Logging
         */
        this.LOG_BLOCKS_BREAKING = this.addConfig(new ConfigOption<>("logging.blocks.break", false, JsonElement::getAsBoolean));
        this.LOG_BLOCKS_PLACING = this.addConfig(new ConfigOption<>("logging.blocks.place", false, JsonElement::getAsBoolean));
        this.LOG_CHUNKS_CLAIMED = this.addConfig(new ConfigOption<>("logging.chunks.claimed", false, JsonElement::getAsBoolean));
        this.LOG_CHUNKS_UNCLAIMED = this.addConfig(new ConfigOption<>("logging.chunks.wilderness", false, JsonElement::getAsBoolean));
        this.LOG_RESET_INTERVAL = this.addConfig(new ConfigOption<>("logging.reset.interval", LoggingIntervals.DAY, this::getAsTimeInterval));
        this.LOG_RESET_TIME = this.addConfig(new ConfigOption<>("logging.reset.time", 7L, JsonElement::getAsLong));
        this.LOG_VIEW_OP_LEVEL = this.addConfig(new ConfigOption<>("logging.read.op_level", 1, JsonElement::getAsInt));
        
        /*
         * Claiming
         */
        this.PLAYER_CLAIMS_LIMIT = this.addConfig(new ConfigOption<>("claims.players.limit", 40, JsonElement::getAsInt));
        this.TOWN_FOUND_COST = this.addConfig(new ConfigOption<>("claims.towns.cost", 500, JsonElement::getAsInt));
        this.TOWN_CLAIMS_LIMIT = this.addConfig(new ConfigOption<>("claims.towns.limit", 200, JsonElement::getAsInt));
        
        this.TOWN_VILLAGERS_INCLUDE = this.addConfig(new ConfigOption<>("claims.towns.villagers.include", true, JsonElement::getAsBoolean));
        this.TOWN_VILLAGERS_VALUE = this.addConfig(new ConfigOption<>("claims.towns.villagers.value", 3, JsonElement::getAsInt));
        
        this.PLAYER_LIMIT_INCREASE = this.addConfig(new ConfigOption<>("claims.players.limit_increase.enabled", false, JsonElement::getAsBoolean));
        this.PLAYER_CLAIM_BUY_LIMIT = this.addConfig(new ConfigOption<>("claims.players.limit_increase.maximum", -1, JsonElement::getAsInt));
        this.PLAYER_CLAIM_BUY_COST = this.addConfig(new ConfigOption<>("claims.players.limit_increase.cost", 200, JsonElement::getAsInt));
        
        /*
         * Claiming Options
         */
        this.CLAIM_ALLOW_GRIEFING_ENDERMAN = this.addConfig(new ConfigOption<>("claims.allow_player_override.griefing.enderman", false, JsonElement::getAsBoolean));
        this.CLAIM_ALLOW_GRIEFING_CREEPER = this.addConfig(new ConfigOption<>("claims.allow_player_override.griefing.creeper", false, JsonElement::getAsBoolean));
        this.CLAIM_ALLOW_GRIEFING_GHAST = this.addConfig(new ConfigOption<>("claims.allow_player_override.griefing.ghast", false, JsonElement::getAsBoolean));
        
        this.CLAIM_ALLOW_PLAYER_COMBAT = this.addConfig(new ConfigOption<>("claims.allow_player_override.player_pvp", true, JsonElement::getAsBoolean));
        this.CLAIM_ALLOW_CROP_AUTOREPLANT = this.addConfig(new ConfigOption<>("claims.allow_player_override.crop_autoreplant", true, JsonElement::getAsBoolean));
        
        this.CLAIM_ALLOW_TREE_CAPACITATOR = new ConfigOption<>("claims.allow_player_override.tree_capacitate", CoreMod.isDebugging(), JsonElement::getAsBoolean);
        this.CLAIM_ALLOW_VEIN_MINER = new ConfigOption<>("claims.allow_player_override.vein_miner", CoreMod.isDebugging(), JsonElement::getAsBoolean);
        
        /*
         * Warping
         */
        this.WARP_MAX_DISTANCE = this.addConfig(new ConfigOption<>( "warp.max_distance", 1000000, JsonElement::getAsInt));
        this.WARP_WAYSTONE_COST = this.addConfig(new ConfigOption<>( "warp.waystone.cost", 2000, JsonElement::getAsInt));
        
        /*
         * Ender Dragon Options
         */
        this.DRAGON_PLAYERS = this.addConfig(new ConfigOption<>("bosses.dragon.players", 5, JsonElement::getAsInt));
        this.DRAGON_ADDITIONAL_HEALTH = this.addConfig(new ConfigOption<>("bosses.dragon.health_boot", 100, JsonElement::getAsInt));
        this.DRAGON_LOOT_END_ITEMS = this.addConfig(new ConfigOption<>("bosses.dragon.loot.end_loot", true, JsonElement::getAsBoolean));
        this.DRAGON_LOOT_RARE_BOOKS = this.addConfig(new ConfigOption<>("bosses.dragon.loot.rare_books", false, JsonElement::getAsBoolean));
        
        /*
         * Commands
         */
        this.COMMAND_MODS_LIST = this.addConfig(new ConfigOption<>("commands.mods_list", true, JsonElement::getAsBoolean));
        this.COMMAND_WARP_TPA = this.addConfig(new ConfigOption<>("commands.warp_tpa", true, JsonElement::getAsBoolean));
        
        this.COMMAND_EQUIPMENT = this.addConfig(new ConfigOption<>("commands.misc.equipment", true, JsonElement::getAsBoolean));
        this.COMMAND_SHRUG = this.addConfig(new ConfigOption<>("commands.misc.shrug", true, JsonElement::getAsBoolean));
        this.COMMAND_TABLEFLIP = this.addConfig(new ConfigOption<>("commands.misc.tableflip", true, JsonElement::getAsBoolean));
        
        /*
         * Mob Spawners
         */
        this.SILK_TOUCH_SPAWNERS = this.addConfig(new ConfigOption<>("fun.spawners.silk_touch", false, JsonElement::getAsBoolean));
        this.SPAWNER_PICKUP_DAMAGE = this.addConfig(new ConfigOption<>("fun.spawners.silk_touch_damage", 390, JsonElement::getAsInt));
        this.SPAWNER_ABSORB_MOBS = this.addConfig(new ConfigOption<>("fun.spawners.absorb_mob_souls", true, JsonElement::getAsBoolean));
        
        /*
         * Server list MOTD
         */
        this.SERVER_MOTD_LIST = this.addConfig(new ConfigArray<>("server.motd", JsonElement::getAsString));
        this.SERVER_ICON_LIST = this.addConfig(new ConfigArray<>("server.icons", JsonElement::getAsString));
        
        /*
         * Permission options
         */
        this.HANDLE_PERMISSIONS = this.addConfig(new ConfigOption<>("server.permissions.enabled", true, JsonElement::getAsBoolean));
        this.FRIEND_WHITELIST = this.addConfig(new ConfigOption<>("server.whitelist.friends_add_friends", false, JsonElement::getAsBoolean));
        
        /*
         * Miscellaneous
         */
        this.OVERWORLD_PORTAL_LOC = this.addConfig(new ConfigOption<>("fun.world.portal_fix.overworld", false, JsonElement::getAsBoolean));
        this.NETHER_PORTAL_LOC = this.addConfig(new ConfigOption<>("fun.world.portal_fix.nether", true, JsonElement::getAsBoolean));
        
        this.START_WITH_RECIPES = this.addConfig(new ConfigOption<>("player.recipes.unlock_all", false, JsonElement::getAsBoolean));
        
        this.NETHER_INFINITE_LAVA = this.addConfig(new ConfigOption<>("fun.world.nether.infinite_lava", false, JsonElement::getAsBoolean));
        
        this.CALENDAR_YEAR_EPOCH = this.addConfig(new ConfigOption<>("fun.calendar.epoch", "After Creation", JsonElement::getAsString));
        this.CALENDAR_DAYS = this.addConfig(new ConfigOption<>("fun.calendar.days", 365, JsonElement::getAsInt));
        
        this.EXTINGUISH_CAMPFIRES = this.addConfig(new ConfigOption<>("fun.world.extinguish_campfires", true, JsonElement::getAsBoolean));
        this.COWS_EAT_MYCELIUM = this.addConfig(new ConfigOption<>("fun.mobs.cows.eat_mycelium", true, JsonElement::getAsBoolean));
        this.LIMIT_SKELETON_ARROWS = this.addConfig(new ConfigOption<>("fun.mobs.skeletons.limit_arrows", true, JsonElement::getAsBoolean));
        this.PREVENT_NETHER_ENDERMEN = this.addConfig(new ConfigOption<>("fun.mobs.enderman.no_nether", false, JsonElement::getAsBoolean));
        this.RANDOM_NAME_VILLAGERS = this.addConfig(new ConfigOption<>("fun.mobs.villagers.random_names", true, JsonElement::getAsBoolean));
        this.ANVIL_DISABLE_COST_LIMIT = this.addConfig(new ConfigOption<>("fun.anvil.disable_level_cap", false, JsonElement::getAsBoolean));
        this.ANVIL_DISABLE_COST_REPAIR = this.addConfig(new ConfigOption<>("fun.anvil.disable_repair_increment", false, JsonElement::getAsBoolean));
        
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
    
    public final boolean preExisting() {
        return this.fileExists;
    }
    public final @Nullable JsonElement reload() throws IOException {
        try {
            return this.reload(this.getConfigFile());
        } finally {
            for (Runnable r : this.reloadFunctions)
                r.run();
        }
    }
    public final @Nullable JsonElement reload(File config) throws IOException {
        CoreMod.logInfo( "Loading configuration file." );
        
        //Read the existing config
        return this.loadFromJSON(this.loadFromFile(config));
    }
    
    private <T> ConfigOption<T> addConfig( ConfigOption<T> config ) {
        // Check for a non-empty path for saving
        if (config.getPath().isEmpty()) throw new RuntimeException("Config Option path should not be empty");
        
        // Store the config
        this.configOptions.add( config );
        return config;
    }
    private <T> ConfigArray<T> addConfig( ConfigArray<T> config ) {
        this.configOptions.add( config );
        return config;
    }
    
    /*
     * File save / load
     */
    @NotNull
    private File getConfigFile() throws IOException {
        // Load from the folder
        final File dir = CoreMod.getConfDir();
        final File config = new File( dir, "config.json" );
        
        if (!config.exists()) {
            if (!config.createNewFile())
                throw new RuntimeException("Error accessing the config");
            /*
             * Create a new config
             */
            this.saveToFile(config, this.saveToJSON());
        }
        return config;
    }
    public void save() throws IOException {
        this.saveToFile( this.getConfigFile(), this.saveToJSON() );
    }
    
    private void saveToFile(File configFile, JsonElement json) throws IOException {
        // GSON Parser
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
        
        try (FileWriter fw = new FileWriter(configFile)) {
            fw.append(gson.toJson(sortObject( json )));
        }
    }
    private JsonObject loadFromFile(File configFile) throws FileNotFoundException {
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
    
    public void afterReload(Runnable runnable) {
        this.reloadFunctions.add(runnable);
    }
    
    /*
     * Special handlers
     */
    private Map<Item, Integer> getItemMap(JsonElement element) {
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
    
    private LoggingIntervals getAsTimeInterval(JsonElement element) {
        if (!LoggingIntervals.contains(element.getAsString()))
            throw new RuntimeException( "Unacceptable time interval \"" + element.getAsString() + "\"" );
        return LoggingIntervals.valueOf(element.getAsString().toUpperCase());
    }
    
    private JsonElement sortObject(JsonElement element) {
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
            if ( !this.version_key.equals( pair.getKey() ) )
                keys.add( pair.getKey() );
        }
        
        // Add them back in sorted order
        Collections.sort( keys );
        
        // Add Version - Should always be first in the sorted list
        keys.add( 0, this.version_key );
        
        for ( String key : keys ) {
            if ( object.has( key ) )
                sorted.add( key, object.get( key ) );
        }
        
        return sorted;
    }
    
}
