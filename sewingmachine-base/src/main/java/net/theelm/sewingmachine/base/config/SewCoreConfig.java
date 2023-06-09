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

package net.theelm.sewingmachine.base.config;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.config.ConfigArray;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protections.logging.EventLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created on Jun 08 2023 at 8:06 PM.
 * By greg in sewingmachine
 */
public class SewCoreConfig {
    private SewCoreConfig() {}
    
    // Mod version
    public static final ConfigOption<String> CONFIG_VERSION = ConfigOption.json(SewConfig.VERSION_KEY, CoreMod.getModVersion());
    public static final ConfigOption<Boolean> HOT_RELOADING = ConfigOption.json("config_hot_reload", true);
    
    // Database
    @Debug
    public static final ConfigOption<Boolean> DB_LITE = ConfigOption.json("database.sqlite", false);
    
    /*
     * Database handling
     */
    
    public static final ConfigOption<String> DB_HOST = ConfigOption.json("database.host", "");
    public static final ConfigOption<String> DB_NAME = ConfigOption.json("database.name", "");
    public static final ConfigOption<String> DB_USER = ConfigOption.json("database.user", "");
    public static final ConfigOption<String> DB_PASS = ConfigOption.json("database.pass", "");
    public static final ConfigOption<Integer> DB_PORT = ConfigOption.json("database.port", 3306);
    
    /*
     * Death chests
     */
    
    public static final ConfigOption<Boolean> DO_DEATH_CHESTS = ConfigOption.json("death_chest.enabled", true);
    public static final ConfigOption<Integer> MAX_DEATH_SCAN = ConfigOption.json("death_chest.max_distance", 4);
    public static final ConfigOption<Integer> MAX_DEATH_ELEVATION = ConfigOption.json("death_chest.max_elevation", -1);
    public static final ConfigOption<Boolean> PRINT_DEATH_CHEST_LOC = ConfigOption.json("death_chest.print_coordinates", true);
    
    // Player Combat
    public static final ConfigOption<Boolean> PVP_DISABLE_DEATH_CHEST = ConfigOption.json("player.pvp.no_death_chest", true);
    public static final ConfigOption<Boolean> PVP_COMBAT_LOG = ConfigOption.json("player.pvp.kill_on_logout", false);
    
    /*
     * Player Combat
     */
    
    public static final ConfigOption<Integer> PVP_COMBAT_SECONDS = ConfigOption.json("player.pvp.combat_seconds", 30);
    
    /*
     * Primary Functions Booleans
     */
    
    public static final ConfigOption<Boolean> DO_CLAIMS = ConfigOption.json("claims.enabled", true);
    public static final ConfigOption<Boolean> CLAIM_CREATIVE_BYPASS = ConfigOption.json("claims.creative_bypass", true);
    public static final ConfigOption<Boolean> DISABLE_VANILLA_PROTECTION = ConfigOption.json("claims.disable_vanilla", true);
    
    public static final ConfigOption<String> NAME_SPAWN = ConfigOption.json("claims.name.spawn", "Spawn");
    public static final ConfigOption<String> NAME_WILDERNESS = ConfigOption.json("claims.name.wild", "Wilderness");
    
    /*
     * Claiming Options
     */
    
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_ENDERMAN = ConfigOption.json("claims.allow_player_override.griefing.enderman", false);
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_CREEPER = ConfigOption.json("claims.allow_player_override.griefing.creeper", false);
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_GHAST = ConfigOption.json("claims.allow_player_override.griefing.ghast", false);
    public static final ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_WEATHER = ConfigOption.json("claims.allow_player_override.griefing.weather", false);
    public static final ConfigOption<Boolean> CLAIM_ALLOW_PLAYER_COMBAT = ConfigOption.json("claims.allow_player_override.player_pvp", true);
    public static final ConfigOption<Boolean> CLAIM_ALLOW_CROP_AUTOREPLANT = ConfigOption.json("claims.allow_player_override.crop_autoreplant", true);
    public static final ConfigOption<Boolean> CLAIM_ALLOW_FIRE_SPREAD = ConfigOption.json("claims.allow_player_override.fire_spread", false);
    
    /*
     * Protections
     */
    
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_SPAWN = ConfigOption.json("claims.op_level.spawn", 1);
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_OTHER = ConfigOption.json("claims.op_level.other_player", 1);
    
    // Claim Regions
    public static final ConfigOption<Integer> MAXIMUM_REGION_WIDTH = ConfigOption.json("claims.regions.max_width", 32);
    public static final ConfigOption<Integer> MINIMUM_REGION_WIDTH = ConfigOption.json("claims.regions.min_width", 3);
    
    /*
     * Logging
     */
    
    public static final ConfigOption<Boolean> LOG_BLOCKS_BREAKING = ConfigOption.json("logging.blocks.break", false);
    public static final ConfigOption<Boolean> LOG_BLOCKS_PLACING = ConfigOption.json("logging.blocks.place", false);
    public static final ConfigOption<Boolean> LOG_CHUNKS_CLAIMED = ConfigOption.json("logging.chunks.claimed", false);
    public static final ConfigOption<Boolean> LOG_CHUNKS_UNCLAIMED = ConfigOption.json("logging.chunks.wilderness", false);
    public static final ConfigOption<EventLogger.LoggingIntervals> LOG_RESET_INTERVAL = new ConfigOption<>("logging.reset.interval", EventLogger.LoggingIntervals.DAY, SewConfig::getAsTimeInterval);
    public static final ConfigOption<Long> LOG_RESET_TIME = ConfigOption.json("logging.reset.time", 7L);
    public static final ConfigOption<Integer> LOG_VIEW_OP_LEVEL = ConfigOption.json("logging.read.op_level", 1);
    
    /*
     * Starting items
     */
    
    public static final ConfigOption<Map<Item, Integer>> STARTING_ITEMS = new ConfigOption<>("player.starting_items", new HashMap<>(), SewConfig::getItemMap);
    public static final ConfigOption<Boolean> FRIEND_WHITELIST = ConfigOption.json("server.whitelist.friends_add_friends", false);
    
    public static final ConfigOption<Boolean> START_WITH_RECIPES = ConfigOption.json("player.recipes.unlock_all", false);
    
    /*
     * Claiming
     */
    
    public static final ConfigOption<Integer> PLAYER_CLAIMS_LIMIT = ConfigOption.json("claims.players.limit", 40);
    public static final ConfigOption<Boolean> PLAYER_LIMIT_INCREASE = ConfigOption.json("claims.players.limit_increase.enabled", false);
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_LIMIT = ConfigOption.json("claims.players.limit_increase.maximum", -1);
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_COST = ConfigOption.json("claims.players.limit_increase.cost", 200);
    
    /*
     * Inventory
     */
    
    public static final ConfigOption<Boolean> TOTEM_ANYWHERE = ConfigOption.json("player.inventory.totem_of_undying", true);
    
    /*
     * Backpacks
     */
    
    public static final ConfigOption<Boolean> ALLOW_BACKPACKS = ConfigOption.json("player.inventory.backpacks.enable", true);
    public static final ConfigOption<Boolean> BACKPACK_SEQUENTIAL = ConfigOption.json("player.inventory.backpacks.require_sequential", true);
    public static final ConfigOption<Integer> BACKPACK_STARTING_ROWS = ConfigOption.json("player.inventory.backpacks.starting_rows", 0);
    
    // Towns
    public static final ConfigOption<Integer> TOWN_FOUND_COST = ConfigOption.json("claims.towns.cost", 500);
    public static final ConfigOption<Integer> TOWN_CLAIMS_LIMIT = ConfigOption.json("claims.towns.limit", 200);
    
    public static final ConfigOption<Boolean> TOWN_VILLAGERS_INCLUDE = ConfigOption.json("claims.towns.villagers.include", true);
    public static final ConfigOption<Integer> TOWN_VILLAGERS_VALUE = ConfigOption.json("claims.towns.villagers.value", 3);
    
    /*
     * Money options
     */
    
    public static final ConfigOption<Boolean> DO_MONEY = ConfigOption.json("money.enabled", true);
    public static final ConfigOption<Integer> STARTING_MONEY = ConfigOption.json("money.starting", 0, 0, Integer.MAX_VALUE);
    public static final ConfigOption<Integer> DAILY_ALLOWANCE = ConfigOption.json("money.daily_reward", 0, 0, Integer.MAX_VALUE);
    
    public static final ConfigOption<Boolean> SHOP_SIGNS = ConfigOption.json("money.shops.enabled", true);
    public static final ConfigOption<Integer> SERVER_SALES_TAX = ConfigOption.json("money.shops.tax_percentage", 0, 0, 100);
    
    /*
     * Worlds
     */
    
    public static final ConfigOption<RegistryKey<World>> DEFAULT_WORLD = ConfigOption.registry("server.worlds.spawn_world", RegistryKeys.WORLD, World.OVERWORLD);
    public static final ConfigOption<Boolean> WORLD_DIMENSION_FOLDERS = ConfigOption.json("server.worlds.use_dimension_folder", false);
    
    public static final Supplier<Boolean> WORLD_SEPARATE_PROPERTIES = () -> SewConfig.get(SewCoreConfig.WORLD_SPECIFIC_TIME)
        || SewConfig.get(SewCoreConfig.WORLD_SPECIFIC_SPAWN)
        || SewConfig.get(SewCoreConfig.WORLD_SPECIFIC_WEATHER)
        || SewConfig.get(SewCoreConfig.WORLD_SPECIFIC_GAME_RULES)
        || SewConfig.get(SewCoreConfig.WORLD_SPECIFIC_GAMEMODE)
        || SewConfig.get(SewCoreConfig.WORLD_SPECIFIC_WORLD_BORDER);
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_SPAWN = ConfigOption.json("server.worlds.separate_properties.spawn_pos", false);
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_TIME = ConfigOption.json("server.worlds.separate_properties.time", false);
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_WEATHER = ConfigOption.json("server.worlds.separate_properties.weather", false);
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_GAME_RULES = ConfigOption.json("server.worlds.separate_properties.gamerules", false);
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_GAMEMODE = ConfigOption.json("server.worlds.separate_properties.gamemode", false);
    public static final ConfigOption<Boolean> WORLD_SPECIFIC_WORLD_BORDER = ConfigOption.json("server.worlds.separate_properties.worldborder", false);
    
    /*
     * Warping
     */
    
    public static final ConfigOption<RegistryKey<World>> WARP_DIMENSION = ConfigOption.registry("warp.world", RegistryKeys.WORLD, World.OVERWORLD);
    public static final ConfigOption<Integer> WARP_MAX_DISTANCE = ConfigOption.json("warp.max_distance", 1000000);
    public static final ConfigOption<Integer> WARP_WAYSTONE_COST = ConfigOption.json("warp.waystone.cost", 2000);
    public static final ConfigOption<Integer> WARP_WAYSTONES_ALLOWED = ConfigOption.json("warp.waystone.maximum", 3);
    
    /*
     * Ender Dragon Options
     */
    
    public static final ConfigOption<Integer> DRAGON_PLAYERS = ConfigOption.json("bosses.dragon.players", 5);
    public static final ConfigOption<Boolean> DRAGON_LOOT_END_ITEMS = ConfigOption.json("bosses.dragon.loot.end_loot", true);
    public static final ConfigOption<Boolean> DRAGON_LOOT_RARE_BOOKS = ConfigOption.json("bosses.dragon.loot.rare_books", false);
    public static final ConfigOption<Integer> DRAGON_ADDITIONAL_HEALTH = ConfigOption.json("bosses.dragon.health_boost", 100);
    
    /*
     * Commands
     */
    
    public static final ConfigOption<Boolean> COMMAND_MODS_LIST = ConfigOption.json("commands.mods_list", true);
    public static final ConfigOption<Boolean> COMMAND_WARP_TPA = ConfigOption.json("commands.warp_tpa", true);
    public static final ConfigOption<Boolean> COMMAND_EQUIPMENT = ConfigOption.json("commands.misc.equipment", true);
    public static final ConfigOption<Boolean> COMMAND_SHRUG = ConfigOption.json("commands.misc.shrug", true);
    public static final ConfigOption<Boolean> COMMAND_TABLEFLIP = ConfigOption.json("commands.misc.tableflip", true);
    
    /*
     * Server list MOTD
     */
    
    public static final ConfigArray<String> SERVER_MOTD_LIST = ConfigArray.jString("server.motd");
    public static final ConfigArray<String> SERVER_ICON_LIST = ConfigArray.jString("server.icons");
    
    /*
     * Permission options
     */
    
    public static final ConfigOption<Boolean> HANDLE_PERMISSIONS = ConfigOption.json("server.permissions.enabled", true);
    
    /*
     * Miscellaneous
     */
    
    public static final ConfigOption<Map<Item, Integer>> ITEM_DESPAWN_TIMES = new ConfigOption<>("server.items.despawn", new HashMap<>(), SewConfig::getItemDespawnMap);
    
    public static final ConfigOption<Boolean> OVERWORLD_PORTAL_LOC = ConfigOption.json("fun.world.portal_fix.overworld", false);
    public static final ConfigOption<Boolean> NETHER_PORTAL_LOC = ConfigOption.json("fun.world.portal_fix.nether", true);
    
    public static final ConfigOption<Boolean> LIMIT_SKELETON_ARROWS = ConfigOption.json("fun.mobs.skeletons.limit_arrows", true);
    
    public static final ConfigOption<Boolean> COWS_EAT_MYCELIUM = ConfigOption.json("fun.mobs.cows.eat_mycelium", true);
    
    public static final ConfigOption<Boolean> EXTINGUISH_CAMPFIRES = ConfigOption.json("fun.world.extinguish_campfires", true);
    
    public static final ConfigOption<Boolean> ANVIL_DISABLE_COST_LIMIT = ConfigOption.json("fun.anvil.disable_level_cap", false);
    public static final ConfigOption<Boolean> ANVIL_DISABLE_COST_REPAIR = ConfigOption.json("fun.anvil.disable_repair_increment", false);
    
    public static final ConfigOption<Boolean> ENDERMEN_FARMS_DROP_NO_LOOT = ConfigOption.json("fun.mobs.enderman.farms_no_loot", true);
    public static final ConfigOption<Boolean> PREVENT_NETHER_ENDERMEN = ConfigOption.json("fun.mobs.enderman.no_nether", false);
    public static final ConfigOption<Boolean> NETHER_INFINITE_LAVA = ConfigOption.json("fun.world.nether.infinite_lava", false);
    public static final ConfigOption<Boolean> CAULDRON_HARDEN = ConfigOption.json("fun.world.cauldron_hardens", false);
    
    public static final ConfigOption<Boolean> RANDOM_NAME_VILLAGERS = ConfigOption.json("fun.mobs.villagers.random_names", true);
    
    public static final ConfigOption<Boolean> IMPROVED_WANDERING_TRADER = ConfigOption.json("fun.mobs.wandering_trader.improve_trades", true);
    @Debug
    public static final ConfigOption<Boolean> PER_PLAYER_WANDERING_TRADER = ConfigOption.json("fun.mobs.wandering_trader.per_player_stock", false);
    public static final ConfigOption<Boolean> ANNOUNCE_WANDERING_TRADER = ConfigOption.json("fun.mobs.wandering_trader.announce", false);
    public static final ConfigOption<Boolean> WANDERING_TRADER_CAMPFIRES = ConfigOption.json("fun.mobs.wandering_trader.toggle_campfires", false);
    public static final ConfigOption<Boolean> WANDERING_TRADER_HITCH = ConfigOption.json("fun.mobs.wandering_trader.hitch_llamas", false);
    public static final ConfigOption<Boolean> WANDERING_TRADER_FORCE_SPAWN = ConfigOption.json("fun.mobs.wandering_trader.force_spawn.enable", false);
    public static final ConfigOption<RegistryKey<World>> WANDERING_TRADER_FORCE_SPAWN_WORLD = ConfigOption.registry("fun.mobs.wandering_trader.force_spawn.world", RegistryKeys.WORLD, World.OVERWORLD);
    public static final ConfigOption<BlockPos> WANDERING_TRADER_FORCE_SPAWN_POS = ConfigOption.blockPos("fun.mobs.wandering_trader.force_spawn.pos", BlockPos.ZERO);
    
    public static final ConfigOption<Integer> WOLF_DAMAGE_RESIST = ConfigOption.json("fun.mobs.wolf.buffs.resistance_multiplier", 3);
    public static final ConfigOption<Integer> WOLF_DAMAGE_BUFF = ConfigOption.json("fun.mobs.wolf.buffs.attack_multiplier", 3);
    public static final ConfigOption<Boolean> WOLF_DAMAGE_BOOST_PLAYERS = ConfigOption.json("fun.mobs.wolf.buffs.work_against_players", false);
    
    public static final ConfigOption<String> CALENDAR_YEAR_EPOCH = ConfigOption.json("fun.calendar.epoch", "After Creation");
    public static final ConfigOption<Integer> CALENDAR_DAYS = ConfigOption.json("fun.calendar.days", 365);
    
    public static final ConfigOption<Boolean> END_FALL_FROM_SKY = ConfigOption.json("fun.world.fall_from_end", false);
    public static final ConfigOption<Boolean> VOID_FALL_TO_SPAWN = ConfigOption.json("fun.world.void_tp_spawn", false);
    
    /*
     * Mob Spawners
     */
    
    public static final ConfigOption<Boolean> SILK_TOUCH_SPAWNERS = ConfigOption.json("fun.spawners.silk_touch", false);
    public static final ConfigOption<Integer> SPAWNER_PICKUP_DAMAGE = ConfigOption.json("fun.spawners.silk_touch_damage", 390);
    public static final ConfigOption<Boolean> SPAWNER_ABSORB_MOBS = ConfigOption.json("fun.spawners.absorb_mob_souls", true);
    public static final ConfigArray<EntityType<?>> SPAWNER_ABSORB_BLACKLIST = ConfigArray.fromRegistry("fun.spawners.mob_blacklist", Registries.ENTITY_TYPE, Arrays.asList(EntityType.IRON_GOLEM, EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.WITHER, EntityType.ELDER_GUARDIAN, EntityType.ENDER_DRAGON));
}
