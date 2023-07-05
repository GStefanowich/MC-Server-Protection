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

package net.theelm.sewingmachine.deathchests.config;

import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.ConfigOption;

/**
 * Created on Jul 05 2023 at 5:28 PM.
 * By greg in sewingmachine
 */
public final class SewDeathConfig {
    private SewDeathConfig() {}
    
    public static final ConfigOption<Integer> MAX_DEATH_SCAN = ConfigOption.json("death_chest.max_distance", 4);
    public static final ConfigOption<Integer> MAX_DEATH_ELEVATION = ConfigOption.json("death_chest.max_elevation", -1);
    public static final ConfigOption<Boolean> PRINT_DEATH_CHEST_LOC = ConfigOption.json("death_chest.print_coordinates", true);
    
    // Player Combat
    public static final ConfigOption<Boolean> PVP_DISABLE_DEATH_CHEST = ConfigOption.json("player.pvp.no_death_chest", true);
}
