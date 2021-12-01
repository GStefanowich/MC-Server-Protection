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
import net.TheElm.project.config.addons.SewBluemapConfig;
import net.TheElm.project.objects.ChatFormat;
import net.TheElm.project.protections.logging.EventLogger.LoggingIntervals;
import net.TheElm.project.utilities.DevUtils;
import net.TheElm.project.utilities.FormattingUtils;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class SewConfig extends SewConfigContainer {
    private static final SewConfig INSTANCE = new SewConfig();
    private static final String VERSION_KEY = "config_version";
    private static final String MODULE_KEY = "_";
    private boolean fileExists;
    
    protected final List<Runnable> reloadFunctions = new ArrayList<>();
    private final Map<String, SewConfig> configModules = new ConcurrentHashMap<>();
    
    // Mod API Modules
    public static final SewConfigContainer BLUE_MAP = new SewBluemapConfig();
    
    // Mod version
    public static final ConfigOption<String> CONFIG_VERSION = SewConfig.addConfig(ConfigOption.json(SewConfig.VERSION_KEY, CoreMod.getModVersion()));
    
    // Database
    public static final ConfigOption<Boolean> DB_LITE = SewConfig.addConfig(ConfigOption.json("database.sqlite", true));
    
    /*
     * Database handling
     */
    
    public static final ConfigOption<String> DB_HOST = SewConfig.addConfig(ConfigOption.json("database.host", ""));
    public static final ConfigOption<String> DB_NAME = SewConfig.addConfig(ConfigOption.json("database.name", ""));
    public static final ConfigOption<String> DB_USER = SewConfig.addConfig(ConfigOption.json("database.user", ""));
    public static final ConfigOption<String> DB_PASS = SewConfig.addConfig(ConfigOption.json("database.pass", ""));
    public static final ConfigOption<Integer> DB_PORT = SewConfig.addConfig(ConfigOption.json("database.port", 3306));
    
    /*
     * Chat Booleans
     */
    
    public static final ConfigOption<Boolean> CHAT_MODIFY = SewConfig.addConfig(ConfigOption.json("chat.modify", true));
    public static final ConfigOption<Boolean> CHAT_SHOW_TOWNS = SewConfig.addConfig(ConfigOption.json("chat.show_towns", true));
    public static final ConfigOption<ChatFormat> CHAT_FORMAT = SewConfig.addConfig(new ConfigOption<>("chat.format", ChatFormat.parse("[%w] %n: %m"), ChatFormat::parse, ChatFormat::serializer));
    
    public static final ConfigOption<Boolean> CHAT_MUTE_SELF = SewConfig.addConfig(ConfigOption.json("chat.mute.personal_mute", true));
    public static final ConfigOption<Boolean> CHAT_MUTE_OP = SewConfig.addConfig(ConfigOption.json("chat.mute.moderator_mute", true));
    
    /*
     * Death chests
     */
    
    public static final ConfigOption<Boolean> DO_DEATH_CHESTS = SewConfig.addConfig(ConfigOption.json("death_chest.enabled", true));
    public static final ConfigOption<Integer> MAX_DEATH_SCAN = SewConfig.addConfig(ConfigOption.json("death_chest.max_distance", 4));
    public static final ConfigOption<Integer> MAX_DEATH_ELEVATION = SewConfig.addConfig(ConfigOption.json("death_chest.max_elevation", -1));
    public static final ConfigOption<Boolean> PRINT_DEATH_CHEST_LOC = SewConfig.addConfig(ConfigOption.json("death_chest.print_coordinates", true));
    
    // Player Combat
    public static final ConfigOption<Boolean> PVP_DISABLE_DEATH_CHEST = SewConfig.addConfig(ConfigOption.json("player.pvp.no_death_chest", true));
    public static final ConfigOption<Boolean> PVP_COMBAT_LOG = SewConfig.addConfig(ConfigOption.json("player.pvp.kill_on_logout", false));
    
    /*
     * Player Combat
     */
    
    public static final ConfigOption<Integer> PVP_COMBAT_SECONDS = SewConfig.addConfig(ConfigOption.json("player.pvp.combat_seconds", 30));
    
    /*
     * Naming
     */
    
    public static final ConfigOption<Boolean> DO_PLAYER_NICKS = SewConfig.addConfig(ConfigOption.json("player.nicks", true));
    public static final ConfigOption<Integer> NICKNAME_COST = SewConfig.addConfig(ConfigOption.json("player.nick_cost", 0));
    
    /*
     * Primary Functions Booleans
     */
    
    public static final ConfigOption<Boolean> DO_CLAIMS = SewConfig.addConfig(ConfigOption.json("claims.enabled", true));
    public static final ConfigOption<Boolean> CLAIM_CREATIVE_BYPASS = SewConfig.addConfig(ConfigOption.json("claims.creative_bypass", true));
    public static final ConfigOption<Boolean> DISABLE_VANILLA_PROTECTION = SewConfig.addConfig(ConfigOption.json("claims.disable_vanilla", true));
    
    public static final ConfigOption<String> NAME_SPAWN = SewConfig.addConfig(ConfigOption.json("claims.name.spawn", "Spawn"));
    public static final ConfigOption<String> NAME_WILDERNESS = SewConfig.addConfig(ConfigOption.json("claims.name.wild", "Wilderness"));
    
    /*
     * Claiming Options
     */
    
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_ENDERMAN = SewConfig.addConfig(ConfigOption.json("claims.allow_player_override.griefing.enderman", false));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_CREEPER = SewConfig.addConfig(ConfigOption.json("claims.allow_player_override.griefing.creeper", false));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_GHAST = SewConfig.addConfig(ConfigOption.json("claims.allow_player_override.griefing.ghast", false));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_WEATHER = SewConfig.addConfig(ConfigOption.json("claims.allow_player_override.griefing.weather", false));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_PLAYER_COMBAT = SewConfig.addConfig(ConfigOption.json("claims.allow_player_override.player_pvp", true));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_CROP_AUTOREPLANT = SewConfig.addConfig(ConfigOption.json("claims.allow_player_override.crop_autoreplant", true));
    public static final ConfigOption<Boolean> CLAIM_ALLOW_FIRE_SPREAD = SewConfig.addConfig(ConfigOption.json("claims.allow_player_override.fire_spread", false));
    
    public static final ConfigOption<Boolean> CLAIM_ALLOW_TREE_CAPACITATOR = ConfigOption.json("claims.allow_player_override.tree_capacitate", DevUtils.isDebugging());
    public static final ConfigOption<Boolean> CLAIM_ALLOW_VEIN_MINER = ConfigOption.json("claims.allow_player_override.vein_miner", DevUtils.isDebugging());
    
    /*
     * Protections
     */
    
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_SPAWN = SewConfig.addConfig(ConfigOption.json("claims.op_level.spawn", 1));
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_OTHER = SewConfig.addConfig(ConfigOption.json("claims.op_level.other_player", 1));
    
    // Claim Regions
    public static final ConfigOption<Integer> MAXIMUM_REGION_WIDTH = SewConfig.addConfig(ConfigOption.json("claims.regions.max_width", 32));
    public static final ConfigOption<Integer> MINIMUM_REGION_WIDTH = SewConfig.addConfig(ConfigOption.json("claims.regions.min_width", 3));
    
    /*
     * Logging
     */
    
    public static final ConfigOption<Boolean> LOG_BLOCKS_BREAKING = SewConfig.addConfig(ConfigOption.json("logging.blocks.break", false));
    public static final ConfigOption<Boolean> LOG_BLOCKS_PLACING = SewConfig.addConfig(ConfigOption.json("logging.blocks.place", false));
    public static final ConfigOption<Boolean> LOG_CHUNKS_CLAIMED = SewConfig.addConfig(ConfigOption.json("logging.chunks.claimed", false));
    public static final ConfigOption<Boolean> LOG_CHUNKS_UNCLAIMED = SewConfig.addConfig(ConfigOption.json("logging.chunks.wilderness", false));
    public static final ConfigOption<LoggingIntervals> LOG_RESET_INTERVAL = SewConfig.addConfig(new ConfigOption<>("logging.reset.interval", LoggingIntervals.DAY, SewConfig::getAsTimeInterval));
    public static final ConfigOption<Long> LOG_RESET_TIME = SewConfig.addConfig(ConfigOption.json("logging.reset.time", 7L));
    public static final ConfigOption<Integer> LOG_VIEW_OP_LEVEL = SewConfig.addConfig(ConfigOption.json("logging.read.op_level", 1));
    
    /*
     * Starting items
     */
    
    public static final ConfigOption<Map<Item, Integer>> STARTING_ITEMS = SewConfig.addConfig(new ConfigOption<>("player.starting_items", new HashMap<>(), SewConfig::getItemMap));
    public static final ConfigOption<Boolean> FRIEND_WHITELIST = SewConfig.addConfig(ConfigOption.json("server.whitelist.friends_add_friends", false));
    
    public static final ConfigOption<Boolean> START_WITH_RECIPES = SewConfig.addConfig(ConfigOption.json("player.recipes.unlock_all", false));
    
    /*
     * Claiming
     */
    
    public static final ConfigOption<Integer> PLAYER_CLAIMS_LIMIT = SewConfig.addConfig(ConfigOption.json("claims.players.limit", 40));
    public static final ConfigOption<Boolean> PLAYER_LIMIT_INCREASE = SewConfig.addConfig(ConfigOption.json("claims.players.limit_increase.enabled", false));
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_LIMIT = SewConfig.addConfig(ConfigOption.json("claims.players.limit_increase.maximum", -1));
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_COST = SewConfig.addConfig(ConfigOption.json("claims.players.limit_increase.cost", 200));
    
    /*
     * Inventory
     */
    
    public static final ConfigOption<Boolean> TOTEM_ANYWHERE = SewConfig.addConfig(ConfigOption.json("player.inventory.totem_of_undying", true));
    
    /*
     * Backpacks
     */
    
    public static final ConfigOption<Boolean> ALLOW_BACKPACKS = SewConfig.addConfig(ConfigOption.json("player.inventory.backpacks.enable", true));
    public static final ConfigOption<Boolean> BACKPACK_SEQUENTIAL = SewConfig.addConfig(ConfigOption.json("player.inventory.backpacks.require_sequential", true));
    public static final ConfigOption<Integer> BACKPACK_STARTING_ROWS = SewConfig.addConfig(ConfigOption.json("player.inventory.backpacks.starting_rows", 0));
    
    // Towns
    public static final ConfigOption<Integer> TOWN_FOUND_COST = SewConfig.addConfig(ConfigOption.json("claims.towns.cost", 500));
    public static final ConfigOption<Integer> TOWN_CLAIMS_LIMIT = SewConfig.addConfig(ConfigOption.json("claims.towns.limit", 200));
    
    public static final ConfigOption<Boolean> TOWN_VILLAGERS_INCLUDE = SewConfig.addConfig(ConfigOption.json("claims.towns.villagers.include", true));
    public static final ConfigOption<Integer> TOWN_VILLAGERS_VALUE = SewConfig.addConfig(ConfigOption.json("claims.towns.villagers.value", 3));
    
    /*
     * Money options
     */
    
    public static final ConfigOption<Boolean> DO_MONEY = SewConfig.addConfig(ConfigOption.json("money.enabled", true));
    public static final ConfigOption<Integer> STARTING_MONEY = SewConfig.addConfig(ConfigOption.json("money.starting", 0, 0, Integer.MAX_VALUE));
    public static final ConfigOption<Integer> DAILY_ALLOWANCE = SewConfig.addConfig(ConfigOption.json("money.daily_reward", 0, 0, Integer.MAX_VALUE));
    
    public static final ConfigOption<Boolean> SHOP_SIGNS = SewConfig.addConfig(ConfigOption.json("money.shops.enabled", true));
    public static final ConfigOption<Integer> SERVER_SALES_TAX = SewConfig.addConfig(ConfigOption.json("money.shops.tax_percentage", 0, 0, 100));
    
    /*
     * Worlds
     */
    
    public static final ConfigOption<RegistryKey<World>> DEFAULT_WORLD = SewConfig.addConfig(ConfigOption.registry("server.worlds.spawn_world", World.OVERWORLD, Registry.WORLD_KEY));
    public static final ConfigOption<Boolean> WORLD_DIMENSION_FOLDERS = SewConfig.addConfig(ConfigOption.json("server.worlds.use_dimension_folder", false));
    
    public static final Supplier<Boolean> WORLD_SEPARATE_PROPERTIES = () -> SewConfig.get(SewConfig.WORLD_SPECIFIC_TIME)
        || SewConfig.get(SewConfig.WORLD_SPECIFIC_SPAWN)
        || SewConfig.get(SewConfig.WORLD_SPECIFIC_WEATHER)
        || SewConfig.get(SewConfig.WORLD_SPECIFIC_GAME_RULES)
        || SewConfig.get(SewConfig.WORLD_SPECIFIC_GAMEMODE)
        || SewConfig.get(SewConfig.WORLD_SPECIFIC_WORLD_BORDER);
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_SPAWN = SewConfig.addConfig(ConfigOption.json("server.worlds.separate_properties.spawn_pos", false));
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_TIME = SewConfig.addConfig(ConfigOption.json("server.worlds.separate_properties.time", false));
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_WEATHER = SewConfig.addConfig(ConfigOption.json("server.worlds.separate_properties.weather", false));
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_GAME_RULES = SewConfig.addConfig(ConfigOption.json("server.worlds.separate_properties.gamerules", false));
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_GAMEMODE = SewConfig.addConfig(ConfigOption.json("server.worlds.separate_properties.gamemode", false));
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_WORLD_BORDER = SewConfig.addConfig(ConfigOption.json("server.worlds.separate_properties.worldborder", false));
    
    /*
     * Warping
     */
    
    public static final ConfigOption<RegistryKey<World>> WARP_DIMENSION = SewConfig.addConfig(ConfigOption.registry("warp.world", World.OVERWORLD, Registry.WORLD_KEY));
    public static final ConfigOption<Integer> WARP_MAX_DISTANCE = SewConfig.addConfig(ConfigOption.json("warp.max_distance", 1000000));
    public static final ConfigOption<Integer> WARP_WAYSTONE_COST = SewConfig.addConfig(ConfigOption.json("warp.waystone.cost", 2000));
    public static final ConfigOption<Integer> WARP_WAYSTONES_ALLOWED = SewConfig.addConfig(ConfigOption.json("warp.waystone.maximum", 3));
    
    /*
     * Ender Dragon Options
     */
    
    public static final ConfigOption<Integer> DRAGON_PLAYERS = SewConfig.addConfig(ConfigOption.json("bosses.dragon.players", 5));
    public static final ConfigOption<Boolean> DRAGON_LOOT_END_ITEMS = SewConfig.addConfig(ConfigOption.json("bosses.dragon.loot.end_loot", true));
    public static final ConfigOption<Boolean> DRAGON_LOOT_RARE_BOOKS = SewConfig.addConfig(ConfigOption.json("bosses.dragon.loot.rare_books", false));
    public static final ConfigOption<Integer> DRAGON_ADDITIONAL_HEALTH = SewConfig.addConfig(ConfigOption.json("bosses.dragon.health_boost", 100));
    
    /*
     * Commands
     */
    
    public static final ConfigOption<Boolean> COMMAND_MODS_LIST = SewConfig.addConfig(ConfigOption.json("commands.mods_list", true));
    public static final ConfigOption<Boolean> COMMAND_WARP_TPA = SewConfig.addConfig(ConfigOption.json("commands.warp_tpa", true));
    public static final ConfigOption<Boolean> COMMAND_EQUIPMENT = SewConfig.addConfig(ConfigOption.json("commands.misc.equipment", true));
    public static final ConfigOption<Boolean> COMMAND_SHRUG = SewConfig.addConfig(ConfigOption.json("commands.misc.shrug", true));
    public static final ConfigOption<Boolean> COMMAND_TABLEFLIP = SewConfig.addConfig(ConfigOption.json("commands.misc.tableflip", true));
    
    /*
     * Server list MOTD
     */
    
    public static final ConfigArray<String> SERVER_MOTD_LIST = SewConfig.addConfig(ConfigArray.jString("server.motd"));
    public static final ConfigArray<String> SERVER_ICON_LIST = SewConfig.addConfig(ConfigArray.jString("server.icons"));
    
    /*
     * Permission options
     */
    
    public static final ConfigOption<Boolean> HANDLE_PERMISSIONS = SewConfig.addConfig(ConfigOption.json("server.permissions.enabled", true));
    
    /*
     * Miscellaneous
     */
    
    public static final ConfigOption<Boolean> OVERWORLD_PORTAL_LOC = SewConfig.addConfig(ConfigOption.json("fun.world.portal_fix.overworld", false));
    public static final ConfigOption<Boolean> NETHER_PORTAL_LOC = SewConfig.addConfig(ConfigOption.json("fun.world.portal_fix.nether", true));
    
    public static final ConfigOption<Boolean> LIMIT_SKELETON_ARROWS = SewConfig.addConfig(ConfigOption.json("fun.mobs.skeletons.limit_arrows", true));
    
    public static final ConfigOption<Boolean> COWS_EAT_MYCELIUM = SewConfig.addConfig(ConfigOption.json("fun.mobs.cows.eat_mycelium", true));
    
    public static final ConfigOption<Boolean> EXTINGUISH_CAMPFIRES = SewConfig.addConfig(ConfigOption.json("fun.world.extinguish_campfires", true));
    
    public static final ConfigOption<Boolean> ANVIL_DISABLE_COST_LIMIT = SewConfig.addConfig(ConfigOption.json("fun.anvil.disable_level_cap", false));
    public static final ConfigOption<Boolean> ANVIL_DISABLE_COST_REPAIR = SewConfig.addConfig(ConfigOption.json("fun.anvil.disable_repair_increment", false));
    
    public static final ConfigOption<Boolean> ENDERMEN_FARMS_DROP_NO_LOOT = SewConfig.addConfig(ConfigOption.json("fun.mobs.enderman.farms_no_loot", true));
    public static final ConfigOption<Boolean> PREVENT_NETHER_ENDERMEN = SewConfig.addConfig(ConfigOption.json("fun.mobs.enderman.no_nether", false));
    public static final ConfigOption<Boolean> NETHER_INFINITE_LAVA = SewConfig.addConfig(ConfigOption.json("fun.world.nether.infinite_lava", false));
    public static final ConfigOption<Boolean> CAULDRON_HARDEN = SewConfig.addConfig(ConfigOption.json("fun.world.cauldron_hardens", false));
    
    public static final ConfigOption<Boolean> RANDOM_NAME_VILLAGERS = SewConfig.addConfig(ConfigOption.json("fun.mobs.villagers.random_names", true));
    
    public static final ConfigOption<Boolean> IMPROVED_WANDERING_TRADER = SewConfig.addConfig(ConfigOption.json("fun.mobs.wandering_trader.improve_trades", true));
    public static final ConfigOption<Boolean> PER_PLAYER_WANDERING_TRADER = SewConfig.addConfig(ConfigOption.json("fun.mobs.wandering_trader.per_player_stock", false));
    public static final ConfigOption<Boolean> ANNOUNCE_WANDERING_TRADER = SewConfig.addConfig(ConfigOption.json("fun.mobs.wandering_trader.announce", false));
    public static final ConfigOption<Boolean> WANDERING_TRADER_CAMPFIRES = SewConfig.addConfig(ConfigOption.json("fun.mobs.wandering_trader.toggle_campfires", false));
    public static final ConfigOption<Boolean> WANDERING_TRADER_HITCH = SewConfig.addConfig(ConfigOption.json("fun.mobs.wandering_trader.hitch_llamas", false));
    public static final ConfigOption<Boolean> WANDERING_TRADER_FORCE_SPAWN = SewConfig.addConfig(ConfigOption.json("fun.mobs.wandering_trader.force_spawn.enable", false));
    public static final ConfigOption<RegistryKey<World>> WANDERING_TRADER_FORCE_SPAWN_WORLD = SewConfig.addConfig(ConfigOption.registry("fun.mobs.wandering_trader.force_spawn.world", World.OVERWORLD, Registry.WORLD_KEY));
    public static final ConfigOption<BlockPos> WANDERING_TRADER_FORCE_SPAWN_POS = SewConfig.addConfig(ConfigOption.blockPos("fun.mobs.wandering_trader.force_spawn.pos", BlockPos.ZERO));
    
    public static final ConfigOption<Integer> WOLF_DAMAGE_RESIST = SewConfig.addConfig(ConfigOption.json("fun.mobs.wolf.buffs.resistance_multiplier", 3));
    public static final ConfigOption<Integer> WOLF_DAMAGE_BUFF = SewConfig.addConfig(ConfigOption.json("fun.mobs.wolf.buffs.attack_multiplier", 3));
    public static final ConfigOption<Boolean> WOLF_DAMAGE_BOOST_PLAYERS = SewConfig.addConfig(ConfigOption.json("fun.mobs.wolf.buffs.work_against_players", false));
    
    public static final ConfigOption<String> CALENDAR_YEAR_EPOCH = SewConfig.addConfig(ConfigOption.json("fun.calendar.epoch", "After Creation"));
    public static final ConfigOption<Integer> CALENDAR_DAYS = SewConfig.addConfig(ConfigOption.json("fun.calendar.days", 365));
    
    public static final ConfigOption<Boolean> END_FALL_FROM_SKY = SewConfig.addConfig(ConfigOption.json("fun.world.fall_from_end", false));
    public static final ConfigOption<Boolean> VOID_FALL_TO_SPAWN = SewConfig.addConfig(ConfigOption.json("fun.world.void_tp_spawn", false));
    
    /*
     * Mob Spawners
     */
    
    public static final ConfigOption<Boolean> SILK_TOUCH_SPAWNERS = SewConfig.addConfig(ConfigOption.json("fun.spawners.silk_touch", false));
    public static final ConfigOption<Integer> SPAWNER_PICKUP_DAMAGE = SewConfig.addConfig(ConfigOption.json("fun.spawners.silk_touch_damage", 390));
    public static final ConfigOption<Boolean> SPAWNER_ABSORB_MOBS = SewConfig.addConfig(ConfigOption.json("fun.spawners.absorb_mob_souls", true));
    
    static {
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
    public final @Nullable JsonElement reload(File config) throws IOException {
        //Read the existing config
        return this.loadFromJSON(SewConfig.loadFromFile(config));
    }
    
    private static <T> ConfigOption<T> addConfig( ConfigOption<T> config ) {
        // Check for a non-empty path for saving
        if (config.getPath().isEmpty()) throw new RuntimeException("Config Option path should not be empty");
        
        // Store the config
        SewConfig.INSTANCE.configOptions.add(config);
        return config;
    }
    private static <T> ConfigArray<T> addConfig( ConfigArray<T> config ) {
        SewConfig.INSTANCE.configOptions.add(config);
        return config;
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
    
    /**
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
            fw.append(gson.toJson(SewConfig.sortObject(json)));
        }
    }
    private static JsonObject loadFromFile(File configFile) throws FileNotFoundException {
        JsonParser jp = new JsonParser();
        JsonElement element = jp.parse(new FileReader(configFile));
        return element.getAsJsonObject();
    }
    
    protected JsonElement loadFromJSON(JsonObject json) {
        CoreMod.logInfo("Loading configuration file (" + FormattingUtils.number(this.configOptions.size()) + " options).");
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
