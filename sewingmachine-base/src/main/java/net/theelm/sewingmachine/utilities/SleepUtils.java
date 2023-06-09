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

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class SleepUtils {
    
    public static void entityBedToggle(@NotNull final LivingEntity entity, final boolean isInBed) {
        if (!( entity instanceof ServerPlayerEntity player))
            return;
        
        // Get the server object
        MinecraftServer server = player.getServer();
        
        // If the server isn't null
        if ( server != null ) {
            ServerWorld world = player.getWorld();
            
            // Only announce if there is more than 1 player around
            if ( world.getPlayers().size() > 1 ) {
                TitleUtils.showPlayerAlert(world,
                    PlayerNameUtils.getPlayerRawName(player).formatted(Formatting.AQUA),
                    Text.literal(isInBed ? " is now in bed" : " left their bed.")
                );
            }
        }
    }
    
    public static @NotNull String timeFromMillis(long millis) {
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
