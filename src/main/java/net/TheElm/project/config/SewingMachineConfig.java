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

import com.google.gson.*;
import net.TheElm.project.CoreMod;
import net.TheElm.project.utilities.LoggingUtils.LoggingIntervals;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class SewingMachineConfig {
    public static final SewingMachineConfig INSTANCE;
    
    static {
        INSTANCE = new SewingMachineConfig();
    }
    
    private final List<ConfigOption<?>> configOptions = new ArrayList<>();
    
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
    
    // Player Death Chests
    public final ConfigOption<Boolean> DO_DEATH_CHESTS;
    public final ConfigOption<Integer> MAX_DEATH_SCAN;
    public final ConfigOption<Integer> MAX_DEATH_ELEVATION;
    
    // Player nicks
    public final ConfigOption<Boolean> DO_PLAYER_NICKS;
    
    // Claiming
    public final ConfigOption<Boolean> DO_CLAIMS;
    public final ConfigOption<String> NAME_SPAWN;
    public final ConfigOption<String> NAME_WILDERNESS;
    
    public final ConfigOption<Integer> CLAIM_OP_LEVEL_SPAWN;
    public final ConfigOption<Integer> CLAIM_OP_LEVEL_OTHER;
    
    // Logging
    public final ConfigOption<Boolean> LOG_BLOCKS_BREAKING;
    public final ConfigOption<Boolean> LOG_BLOCKS_PLACING;
    public final ConfigOption<Boolean> LOG_CHUNKS_CLAIMED;
    public final ConfigOption<Boolean> LOG_CHUNKS_UNCLAIMED;
    public final ConfigOption<LoggingIntervals> LOG_RESET_INTERVAL;
    public final ConfigOption<Long> LOG_RESET_TIME;
    public final ConfigOption<Integer> LOG_VIEW_OP_LEVEL;
    
    // Players
    public final ConfigOption<Integer> PLAYER_CLAIMS_LIMIT;
    
    // Towns
    public final ConfigOption<Integer> TOWN_FOUND_COST;
    public final ConfigOption<Integer> TOWN_CLAIMS_LIMIT;
    
    // Economy
    public final ConfigOption<Boolean> DO_MONEY;
    public final ConfigOption<Long> STARTING_MONEY;
    
    // Sleeping
    public final ConfigOption<Boolean> DO_SLEEP_VOTE;
    public final ConfigOption<Integer> SLEEP_PERCENT;
    
    // Warp options
    public final ConfigOption<Integer> WARP_MAX_DISTANCE;
    public final ConfigOption<Integer> WARP_WAYSTONE_COST;
    
    // End
    public final ConfigOption<Integer> DRAGON_PLAYERS;
    
    // Commands
    public final ConfigOption<Integer> COMMAND_SPAWN_OP_LEVEL;
    public final ConfigOption<Integer> COMMAND_MODS_OP_LEVEL;
    public final ConfigOption<Boolean> COMMAND_WARP_TPA;
    public final ConfigOption<Boolean> COMMAND_EQUIPMENT;
    public final ConfigOption<Boolean> COMMAND_SHRUG;
    public final ConfigOption<Boolean> COMMAND_TABLEFLIP;
    
    // Miscellaneous
    public final ConfigOption<Boolean> LIMIT_SKELETON_ARROWS;
    public final ConfigOption<Boolean> EXTINGUISH_CAMPFIRES;
    
    private SewingMachineConfig() {
        // Initialize all configurations
        
        /*
         * Database handling
         */
        this.DB_LITE = this.addConfig( new ConfigOption<>( "database.sqlite", true, JsonElement::getAsBoolean ) );
        this.DB_HOST = this.addConfig( new ConfigOption<>("database.host", "", JsonElement::getAsString ));
        this.DB_NAME = this.addConfig( new ConfigOption<>("database.name", "", JsonElement::getAsString ));
        this.DB_USER = this.addConfig( new ConfigOption<>("database.user", "", JsonElement::getAsString ));
        this.DB_PASS = this.addConfig( new ConfigOption<>("database.pass", "", JsonElement::getAsString ));
        this.DB_PORT = this.addConfig( new ConfigOption<>("database.port", 3306, JsonElement::getAsInt ));
        
        /*
         * Primary Functions Booleans
         */
        this.DO_CLAIMS = this.addConfig( new ConfigOption<>("claims.enabled", true, JsonElement::getAsBoolean ));
        
        /*
         * Chat Booleans
         */
        this.CHAT_MODIFY = this.addConfig( new ConfigOption<>("chat.modify", true, JsonElement::getAsBoolean ));
        this.CHAT_SHOW_TOWNS = this.addConfig( new ConfigOption<>("chat.show_towns", true, JsonElement::getAsBoolean ));
        
        /*
         * Sleep
         */
        this.DO_SLEEP_VOTE = this.addConfig( new ConfigOption<>("sleep.voting", true, JsonElement::getAsBoolean));
        this.SLEEP_PERCENT = this.addConfig( new ConfigOption<>("sleep.percent", 50, JsonElement::getAsInt));
        
        /*
         * Money options
         */
        this.DO_MONEY = this.addConfig( new ConfigOption<>("money.enabled", true, JsonElement::getAsBoolean));
        this.STARTING_MONEY = this.addConfig( new ConfigOption<>("money.starting", 0L, JsonElement::getAsLong));
        
        /*
         * Death chests
         */
        this.DO_DEATH_CHESTS = this.addConfig( new ConfigOption<>("death_chest.enabled", true, JsonElement::getAsBoolean));
        this.MAX_DEATH_SCAN = this.addConfig( new ConfigOption<>("death_chest.max_distance", 4, JsonElement::getAsInt));
        this.MAX_DEATH_ELEVATION = this.addConfig( new ConfigOption<>("death_chest.max_elevation", 256, JsonElement::getAsInt));
        
        /*
         * Naming
         */
        this.DO_PLAYER_NICKS = this.addConfig( new ConfigOption<>("player.nicks", true, JsonElement::getAsBoolean));
        this.NAME_SPAWN = this.addConfig( new ConfigOption<>("claims.name.spawn", "Spawn", JsonElement::getAsString));
        this.NAME_WILDERNESS = this.addConfig( new ConfigOption<>("claims.name.wild", "Wilderness", JsonElement::getAsString));
        
        /*
         * Protections
         */
        this.CLAIM_OP_LEVEL_SPAWN = this.addConfig( new ConfigOption<>("claims.op_level.spawn", 1, JsonElement::getAsInt));
        this.CLAIM_OP_LEVEL_OTHER = this.addConfig( new ConfigOption<>("claims.op_level.other_player", 1, JsonElement::getAsInt));
        
        /*
         * Logging
         */
        this.LOG_BLOCKS_BREAKING = this.addConfig( new ConfigOption<>("logging.blocks.break", true, JsonElement::getAsBoolean));
        this.LOG_BLOCKS_PLACING = this.addConfig( new ConfigOption<>("logging.blocks.place", true, JsonElement::getAsBoolean));
        this.LOG_CHUNKS_CLAIMED = this.addConfig( new ConfigOption<>("logging.chunks.claimed", true, JsonElement::getAsBoolean));
        this.LOG_CHUNKS_UNCLAIMED = this.addConfig( new ConfigOption<>("logging.chunks.wilderness", true, JsonElement::getAsBoolean));
        this.LOG_RESET_INTERVAL = this.addConfig( new ConfigOption<>("logging.reset.interval", LoggingIntervals.DAY, this::getAsTimeInterval));
        this.LOG_RESET_TIME = this.addConfig( new ConfigOption<>("logging.reset.time", 7L, JsonElement::getAsLong));
        this.LOG_VIEW_OP_LEVEL = this.addConfig( new ConfigOption<>("logging.read.op_level", 1, JsonElement::getAsInt));
        
        /*
         * Claiming
         */
        this.PLAYER_CLAIMS_LIMIT = this.addConfig( new ConfigOption<>( "claims.players.limit", 40, JsonElement::getAsInt));
        this.TOWN_FOUND_COST = this.addConfig( new ConfigOption<>( "claims.towns.cost", 500, JsonElement::getAsInt));
        this.TOWN_CLAIMS_LIMIT = this.addConfig( new ConfigOption<>( "claims.towns.limit", 200, JsonElement::getAsInt));
        
        /*
         * Warping
         */
        this.WARP_MAX_DISTANCE = this.addConfig( new ConfigOption<>( "warp.max_distance", 1000000, JsonElement::getAsInt ));
        this.WARP_WAYSTONE_COST = this.addConfig( new ConfigOption<>( "warp.waystone.cost", 2000, JsonElement::getAsInt ));
        
        /*
         * Ender Dragon Options
         */
        this.DRAGON_PLAYERS = this.addConfig( new ConfigOption<>("bosses.dragon.players", 5, JsonElement::getAsInt ));
        
        /*
         * Commands
         */
        this.COMMAND_SPAWN_OP_LEVEL = this.addConfig( new ConfigOption<>("commands.spawn_tp_level", 2, JsonElement::getAsInt));
        this.COMMAND_MODS_OP_LEVEL = this.addConfig( new ConfigOption<>("commands.mods_op_level", 0, JsonElement::getAsInt));
        this.COMMAND_WARP_TPA = this.addConfig( new ConfigOption<>("commands.warp_tpa", true, JsonElement::getAsBoolean));
        this.COMMAND_EQUIPMENT = this.addConfig( new ConfigOption<>("commands.misc.equipment", true, JsonElement::getAsBoolean));
        this.COMMAND_SHRUG = this.addConfig( new ConfigOption<>("commands.misc.shrug", true, JsonElement::getAsBoolean));
        this.COMMAND_TABLEFLIP = this.addConfig( new ConfigOption<>("commands.misc.tableflip", true, JsonElement::getAsBoolean));
        
        /*
         * Miscellaneous
         */
        this.EXTINGUISH_CAMPFIRES = this.addConfig( new ConfigOption<>("fun.world.extinguish_campfires", true, JsonElement::getAsBoolean));
        this.LIMIT_SKELETON_ARROWS = this.addConfig( new ConfigOption<>("fun.mobs.skeletons.limit_arrows", true, JsonElement::getAsBoolean));
        
        this.initialize();
    }
    
    private <T> ConfigOption<T> addConfig( ConfigOption<T> config ) {
        this.configOptions.add( config );
        return config;
    }
    
    /*
     * File save / load
     */
    private void initialize() {
        CoreMod.logMessage( "Loading configuration file." );
        
        // Load from the folder
        try {
            final File dir = CoreMod.getConfDir();
            final File config = new File( dir.getAbsolutePath() + File.separator + "config.json" );
            
            if (config.exists()) {
            } else if (config.createNewFile()) {
                /*
                 * Create a new config
                 */
                this.saveToFile(config, this.saveToJSON());
            } else {
                throw new RuntimeException("Error accessing the config");
            }
            
            /*
             * Read the existing config
             */
            JsonElement loaded = this.loadFromJSON(this.loadFromFile(config));
            
            /*
             * Save any new values
             */
            this.saveToFile(config, loaded);
            
        } catch (IOException e) {
            CoreMod.logError( e );
        }
    }
    
    private void saveToFile(File configFile, JsonElement json) throws IOException {
        // GSON Parser
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        
        try (FileWriter fw = new FileWriter(configFile)) {
            fw.append(gson.toJson( json ));
        }
    }
    private JsonObject loadFromFile(File configFile) throws FileNotFoundException {
        JsonParser jp = new JsonParser();
        JsonElement element = jp.parse(new FileReader(configFile));
        return element.getAsJsonObject();
    }
    
    private JsonElement loadFromJSON(JsonObject json ) {
        for ( ConfigOption<?> config : this.configOptions ) {
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
                config.set(inner.get(path[p]));
            else if ( config.get() instanceof String || ( config.get() == null ) )
                inner.addProperty( path[p], (String) config.get());
            else if ( config.get() instanceof Number )
                inner.addProperty( path[p], (Number) config.get());
            else if ( config.get() instanceof Boolean )
                inner.addProperty( path[p], (Boolean) config.get());
            else if ( config.get() instanceof Enum )
                inner.addProperty( path[p], ((Enum) config.get()).name() );
        }
        
        return json;
    }
    private JsonObject saveToJSON() {
        final JsonObject baseObject = new JsonObject();
        
        for ( ConfigOption<?> config : this.configOptions ) {
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
            
            if ( config.get() instanceof String || ( config.get() == null ) )
                inner.addProperty( path[p], (String) config.get());
            else if ( config.get() instanceof Number )
                inner.addProperty( path[p], (Number) config.get());
            else if ( config.get() instanceof Boolean )
                inner.addProperty( path[p], (Boolean) config.get());
            else if ( config.get() instanceof Enum )
                inner.addProperty( path[p], ((Enum)config.get()).name() );
        }
        
        return baseObject;
    }
    
    private LoggingIntervals getAsTimeInterval(JsonElement element) {
        if (!LoggingIntervals.contains(element.getAsString()))
            throw new RuntimeException( "Unacceptable time interval \"" + element.getAsString() + "\"" );
        return LoggingIntervals.valueOf(element.getAsString().toUpperCase());
    }
    
}
