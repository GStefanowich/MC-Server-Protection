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

import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.config.SewingMachineConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class LoggingUtils {
    
    private LoggingUtils() {}
    
    public static void doCleanup() {
        // Ignore if disabled
        if (SewingMachineConfig.INSTANCE.LOG_RESET_TIME.get() <= 0)
            return;
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("DELETE FROM `logging_Blocks` WHERE `updatedAt` <= (NOW() - INTERVAL ? ?)")
            .addPrepared(SewingMachineConfig.INSTANCE.LOG_RESET_TIME.get())
            .addPrepared(SewingMachineConfig.INSTANCE.LOG_RESET_INTERVAL.get())) {
            
            stmt.executeUpdate();
            CoreMod.logMessage( "Database cleanup completed" );
            
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
    }
    public static void logAction(BlockAction blockAction, Block block, BlockPos blockPos, PlayerEntity player) {
        // Return if AIR
        if (block == Blocks.AIR) return;
        LoggingUtils.logAction(blockAction, block.getTranslationKey(), blockPos, player);
    }
    public static void logAction(BlockAction blockAction, Item item, BlockPos blockPos, PlayerEntity player) {
        // Return if AIR
        if (item == Items.AIR) return;
        LoggingUtils.logAction(blockAction, item.getTranslationKey(), blockPos, player);
    }
    private static void logAction(BlockAction blockAction, String translationKey, BlockPos blockPos, PlayerEntity player) {
        // If logging is disabled for a certain type, ignore
        if (blockAction == BlockAction.BREAK && (!SewingMachineConfig.INSTANCE.LOG_BLOCKS_BREAKING.get())) return;
        if (blockAction == BlockAction.PLACE && (!SewingMachineConfig.INSTANCE.LOG_BLOCKS_PLACING.get())) return;
        
        // Don't process on the client
        World world = player.getEntityWorld();
        if (!world.isClient) {
            
            // Get the dimension
            DimensionType dimension = world.dimension.getType();
            
            // Save the change
            try (MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT INTO `logging_Blocks` ( `blockWorld`, `blockX`, `blockY`, `blockZ`, `block`, `updatedBy`, `updatedEvent`, `updatedAt` ) VALUES ( ?, ?, ?, ?, ?, ?, ?, NOW() );")
                .addPrepared(dimension.getRawId())
                .addPrepared(blockPos.getX())
                .addPrepared(blockPos.getY())
                .addPrepared(blockPos.getZ())
                .addPrepared(translationKey)
                .addPrepared(player.getUuid())
                .addPrepared(blockAction)) {
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                CoreMod.logError(e);
            }
        }
    }
    
    public enum LoggingIntervals {
        DAY,
        HOUR,
        MINUTE,
        MONTHS,
        QUARTER,
        SECOND,
        WEEK,
        YEAR;
        
        public static boolean contains(@NotNull String key) {
            for ( LoggingIntervals i : LoggingIntervals.values() ) {
                if ( key.equalsIgnoreCase(i.name()) )
                    return true;
            }
            
            return false;
        }
    }
    public enum BlockAction {
        PLACE,
        BREAK;
    }
    
}
