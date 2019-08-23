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
    public final ConfigOption<Boolean> WARP_SPAWN_REQUIRES_OP;
    
    // End
    public final ConfigOption<Integer> DRAGON_PLAYERS;
    
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
        
        this.WARP_SPAWN_REQUIRES_OP = this.addConfig( new ConfigOption<>( "warp.command.spawn.requires_op", true, JsonElement::getAsBoolean));
        
        /*
         * Ender Dragon Options
         */
        this.DRAGON_PLAYERS = this.addConfig( new ConfigOption<>("bosses.dragon.players", 5, JsonElement::getAsInt ));
        
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
        }
        
        return baseObject;
    }
    
}
