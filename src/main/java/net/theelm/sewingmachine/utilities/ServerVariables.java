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

package net.theelm.sewingmachine.utilities;

import com.mojang.bridge.game.GameVersion;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.MotdFunction;
import net.minecraft.SharedConstants;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Created on Jun 20 2021 at 6:28 PM.
 * By greg in SewingMachineMod
 */
public class ServerVariables {
    private static final @NotNull Map<String, MotdFunction> VARIABLES;
    static {
        /*
         * Save our Variables to parse
         */
        Map<String, MotdFunction> variables = new HashMap<>();
        
        // Version
        variables.put("version", server -> {
            GameVersion version = SharedConstants.getGameVersion();
            if (version == null)
                return "?.??.??";
            return version.getId();
        });
        
        // Time (Ingame time of day)
        variables.put("time", server -> {
            ServerWorld world = server.getWorld(World.OVERWORLD);
            if (world == null) return "time";
            return SleepUtils.timeFromMillis(world.getTimeOfDay());
        });
        
        // Time (Ingame day)
        variables.put("time.day", server -> {
            ServerWorld world = server.getWorld(World.OVERWORLD);
            long worldDay;
            
            if (world == null)
                worldDay = 0L;
            else {
                worldDay = IntUtils.timeToDays(world);
                long worldYear = worldDay / SewConfig.get(SewConfig.CALENDAR_DAYS);
                worldDay = worldDay - (worldYear * SewConfig.get(SewConfig.CALENDAR_DAYS));
            }
            
            return FormattingUtils.format(worldDay);
        });
        
        // Time (Ingame year)
        variables.put("time.year", server -> {
            ServerWorld world = server.getWorld(World.OVERWORLD);
            long worldYear;
            
            if (world == null)
                worldYear = 0L;
            else {
                long worldDay = IntUtils.timeToDays(world);
                worldYear = worldDay / SewConfig.get(SewConfig.CALENDAR_DAYS);
            }
            
            return FormattingUtils.format(worldYear);
        });
        
        // Players online
        variables.put("players", server -> {
            PlayerManager playerManager = server.getPlayerManager();
            return FormattingUtils.format(playerManager == null ? 0 : playerManager.getCurrentPlayerCount());
        });
        
        // Max player count
        variables.put("slots", server -> {
            PlayerManager playerManager = server.getPlayerManager();
            return FormattingUtils.format(playerManager == null ? 0 : playerManager.getMaxPlayerCount());
        });
        
        // Number of players that can join
        variables.put("slots.free", server -> {
            PlayerManager playerManager = server.getPlayerManager();
            return FormattingUtils.format(playerManager == null ? 0 : Math.max(0, playerManager.getMaxPlayerCount() - playerManager.getCurrentPlayerCount()));
        });
        
        // Weather
        variables.put("weather", server -> {
            ServerWorld world = server.getWorld(World.OVERWORLD);
            if (world == null)
                return "unknown";
            if (world.isThundering())
                return "storm";
            if (world.isRaining())
                return "rain";
            return "clear";
        });
        
        // Weather sentence
        variables.put("weather.adj", server -> {
            ServerWorld world = server.getWorld(World.OVERWORLD);
            if (world == null)
                return "strange";
            if (world.isThundering())
                return "stormy";
            if (world.isRaining())
                return "rainy";
            return "beautiful";
        });
        
        // Difficulty
        variables.put("difficulty", server -> {
            SaveProperties properties = server.getSaveProperties();
            if (properties.isHardcore())
                return "hardcore";
            Difficulty difficulty = properties.getDifficulty();
            ServerWorld world = StreamSupport.stream(server.getWorlds().spliterator(), false).findFirst().orElse(null);
            if (world != null)
                difficulty = world.getLevelProperties().getDifficulty();
            return difficulty.getName();
        });
        
        // Year epoch
        variables.put("config.epoch", server -> CasingUtils.acronym(SewConfig.get(SewConfig.CALENDAR_YEAR_EPOCH), true));
        
        VARIABLES = Collections.unmodifiableMap(variables);
    }
    
    public static @NotNull Set<Map.Entry<String, MotdFunction>> entrySet() {
        return ServerVariables.VARIABLES.entrySet();
    }
}
