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

package net.TheElm.project.utilities;

import com.mojang.bridge.game.GameVersion;
import net.TheElm.project.ServerCore;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Created on Jun 20 2021 at 6:28 PM.
 * By greg in SewingMachineMod
 */
public class ServerVariables {
    private static final @NotNull Map<String, Function<MinecraftServer, String>> VARIABLES;
    static {
        /*
         * Save our Variables to parse
         */
        Map<String, Function<MinecraftServer, String>> variables = new HashMap<>();
        
        // Version
        variables.put("version", server -> {
            GameVersion version = SharedConstants.getGameVersion();
            if (version == null)
                return "?.??.??";
            return version.getId();
        });
        
        // Time
        variables.put("time", server -> {
            ServerWorld world = server.getWorld(World.OVERWORLD);
            if (world == null) return "time";
            return SleepUtils.timeFromMillis(world.getTimeOfDay());
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
        
        VARIABLES = Collections.unmodifiableMap(variables);
    }
    
    public static @NotNull Set<Map.Entry<String, Function<MinecraftServer, String>>> entrySet() {
        return ServerVariables.VARIABLES.entrySet();
    }
}
