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

import net.TheElm.project.interfaces.SleepingWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public final class SleepUtils {
    
    public static void entityBedToggle(final LivingEntity entity, final boolean isInBed, final boolean isSleeping) {
        if (!( entity instanceof ServerPlayerEntity))
            return;
        
        SleepUtils.playerBedToggle( (ServerPlayerEntity)entity, isInBed, isSleeping );
    }
    public static void playerBedToggle(final PlayerEntity player, final boolean isInBed, final boolean isSleeping) {
        // Get the server object
        MinecraftServer server = player.getServer();
        if ( server != null ) {
            World world = player.getEntityWorld();
            int percentage;
            
            if ( isInBed ) {
                // Get the percentage (AFTER updating the list of UUIDs)
                percentage = SleepUtils.getSleepingPercentage( world );
                
                if ( world.getPlayers().size() > 1 ) {
                    TitleUtils.showPlayerAlert((ServerWorld) player.world,
                        ColorUtils.format(player.getDisplayName(), Formatting.AQUA),
                        new LiteralText(isSleeping ? " is now sleeping (" + percentage + "%)." : " is now in bed")
                    );
                }
                
            } else {
                // Get the percentage (AFTER updating the list of UUIDs)
                percentage = SleepUtils.getSleepingPercentage( world );
                
                if ( world.getPlayers().size() > 1 ) {
                    TitleUtils.showPlayerAlert((ServerWorld) player.world,
                        ColorUtils.format(player.getDisplayName(), Formatting.AQUA),
                        new LiteralText(" left their bed.")
                    );
                }
            }
        }
    }
    
    /**
     * @param world The world in which to get the sleeping count (Per world setup)
     * @return An int between (0 - 100)
     */
    public static int getSleepingPercentage(@NotNull final World world) {
        if (!(world instanceof SleepingWorld))
            return 0;
        SleepingWorld sleepingWorld = (SleepingWorld) world;
        
        int applicablePlayers = world.getPlayers().size();
        
        // Don't divide by zero
        if ( applicablePlayers == 0 )
            return 100;
        
        // Get actually sleeping players
        long remSleepers = world.getPlayers().stream().filter((player) -> player.isSleeping() && player.isSleepingLongEnough()).count();
        
        int percentage = (int)( ( (float)remSleepers / (float)applicablePlayers ) * 100.0f );
        
        // If percentage meets
        return percentage;
    }
    
    public static String timeFromMillis(long millis) {
        if (millis >= 23000)
            return "sunrise";
        if (millis >= 18000)
            return "midnight";
        if (millis >= 13000)
            return "night";
        if (millis >= 12000)
            return "sunset";
        if (millis >= 6000)
            return "noon";
        if (millis >= 1000)
            return "day";
        return "morning";
    }
    
}
