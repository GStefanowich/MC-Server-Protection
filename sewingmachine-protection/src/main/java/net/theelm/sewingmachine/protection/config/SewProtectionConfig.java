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

package net.theelm.sewingmachine.protection.config;

import net.theelm.sewingmachine.annotations.Public;
import net.theelm.sewingmachine.config.ConfigOption;

/**
 * Created on Jul 05 2023 at 5:30 PM.
 * By greg in sewingmachine
 */
public final class SewProtectionConfig {
    private SewProtectionConfig() {}
    
    public static final ConfigOption<Boolean> CLAIM_CREATIVE_BYPASS = ConfigOption.json("claims.creative_bypass", true);
    public static final ConfigOption<Boolean> DISABLE_VANILLA_PROTECTION = ConfigOption.json("claims.disable_vanilla", true);
    
    /*
     * Claiming Options
     */
    
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_ENDERMAN = ConfigOption.json("claims.allow_player_override.griefing.enderman", false);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_CREEPER = ConfigOption.json("claims.allow_player_override.griefing.creeper", false);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_GHAST = ConfigOption.json("claims.allow_player_override.griefing.ghast", false);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_GRIEFING_WEATHER = ConfigOption.json("claims.allow_player_override.griefing.weather", false);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_PLAYER_COMBAT = ConfigOption.json("claims.allow_player_override.player_pvp", true);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_CROP_AUTOREPLANT = ConfigOption.json("claims.allow_player_override.crop_autoreplant", true);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_FIRE_SPREAD = ConfigOption.json("claims.allow_player_override.fire_spread", false);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_PHANTOMS = ConfigOption.json("claims.allow_player_override.phantom_spawns", false);
    public static final @Public ConfigOption<Boolean> CLAIM_ALLOW_HOSTILES = ConfigOption.json("claims.allow_player_override.hostile_spawns", false);
    
    /*
     * Protections
     */
    
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_SPAWN = ConfigOption.json("claims.op_level.spawn", 1);
    public static final ConfigOption<Integer> CLAIM_OP_LEVEL_OTHER = ConfigOption.json("claims.op_level.other_player", 1);
    
    // Claim Regions
    public static final ConfigOption<Integer> MAXIMUM_REGION_WIDTH = ConfigOption.json("claims.regions.max_width", 32);
    public static final ConfigOption<Integer> MINIMUM_REGION_WIDTH = ConfigOption.json("claims.regions.min_width", 3);
    
    /*
     * Claiming
     */
    
    public static final @Public ConfigOption<Integer> PLAYER_CLAIMS_LIMIT = ConfigOption.json("claims.players.limit", 40);
    public static final ConfigOption<Boolean> PLAYER_LIMIT_INCREASE = ConfigOption.json("claims.players.limit_increase.enabled", false);
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_LIMIT = ConfigOption.json("claims.players.limit_increase.maximum", -1);
    public static final ConfigOption<Integer> PLAYER_CLAIM_BUY_COST = ConfigOption.json("claims.players.limit_increase.cost", 200);
    
    /*
     * Towns
     */
    
    public static final @Public ConfigOption<Integer> TOWN_FOUND_COST = ConfigOption.json("claims.towns.cost", 500);
    public static final @Public ConfigOption<Integer> TOWN_CLAIMS_LIMIT = ConfigOption.json("claims.towns.limit", 200);
    
    public static final ConfigOption<Boolean> TOWN_VILLAGERS_INCLUDE = ConfigOption.json("claims.towns.villagers.include", true);
    public static final ConfigOption<Integer> TOWN_VILLAGERS_VALUE = ConfigOption.json("claims.towns.villagers.value", 3);
}
