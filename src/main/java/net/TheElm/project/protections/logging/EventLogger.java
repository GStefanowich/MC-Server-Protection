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

package net.TheElm.project.protections.logging;

import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.utilities.NbtUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

public final class EventLogger implements Runnable {
    
    private static final EventLogger INSTANCE = new EventLogger();
    private static final LinkedBlockingDeque<LoggableEvent> actionLog = new LinkedBlockingDeque<>();
    
    private final Thread thread;
    
    private EventLogger() {
        this.thread = new Thread(this);
        this.thread.setName("Block logger");
    }
    
    @Override
    public void run() {
        CoreMod.logInfo("Starting logger");
        while (true) {
            try {
                this.wrapper();
            } catch (InterruptedException e) {
                
                CoreMod.logInfo("Cleaning up backlog");
                
                // Log the remaining values and exit
                actionLog.forEach(this::saveToDatabase);
                break;
                
            } catch (Exception e) {
                // Log and continue (Thread should not die)
                CoreMod.logError( e );
            }
        }
        
        CoreMod.logInfo("Done logging");
    }
    
    @SuppressWarnings("InfiniteLoopStatement")
    private void wrapper() throws InterruptedException {
        while ( true ) this.saveToDatabase(actionLog.takeFirst());
    }
    
    /*
     * Logging Handlers for different tables
     */
    private boolean saveToDatabase(BlockEvent event) {
        // Get the event information
        World world = event.getWorld();
        Entity source = event.getSource();
        BlockPos blockPos = event.getPosition();
        String translationKey = event.getBlock().getTranslationKey();
        BlockAction action = event.getAction();
        
        // Get the dimension
        RegistryKey<World> dimension = world.getRegistryKey();
        
        UUID responsible = source instanceof PlayerEntity ? source.getUuid() : ( source instanceof TameableEntity ? ((TameableEntity)source).getOwnerUuid() : null);
        if (responsible == null)
            return false;
        
        // Save the change
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT INTO `logging_Blocks` ( `blockWorld`, `blockX`, `blockY`, `blockZ`, `block`, `updatedBy`, `updatedEvent`, `updatedAt` ) VALUES ( ?, ?, ?, ?, ?, ?, ?, NOW() );")
            .addPrepared(NbtUtils.worldToTag(dimension))
            .addPrepared(blockPos.getX())
            .addPrepared(blockPos.getY())
            .addPrepared(blockPos.getZ())
            .addPrepared(translationKey)
            .addPrepared(responsible)
            .addPrepared(action)) {
            
            stmt.executeUpdate();
            
            return true;
        } catch (SQLException e) {
            CoreMod.logError(e);
            
            return false;
        }
    }
    private boolean saveToDatabase(TransferEvent event) {
        System.out.println("Logging transfer!");
        return true;
    }
    private boolean saveToDatabase(LoggableEvent event) {
        if (event instanceof BlockEvent) return saveToDatabase((BlockEvent) event);
        if (event instanceof TransferEvent) return saveToDatabase((TransferEvent) event);
        CoreMod.logError(new Exception("Missing handler for " + event.getClass().getName()));
        return false;
    }
    
    // Static method for logging interactions
    public static boolean log(LoggableEvent action) {
        // If logging is disabled for a certain type, ignore
        if (action instanceof BlockEvent) {
            BlockEvent blockAction = (BlockEvent) action;
            if (
                (blockAction.getAction() == BlockAction.BREAK) && (!SewConfig.get(SewConfig.LOG_BLOCKS_BREAKING))
                || (blockAction.getAction() == BlockAction.PLACE) && (!SewConfig.get(SewConfig.LOG_BLOCKS_PLACING))
            ) return false;
        }
        // Store the log action
        return actionLog.offer(action);
    }
    
    // Static method to start
    public static EventLogger start() {
        // Start the runnable in a new Thread
        if (!INSTANCE.thread.isAlive())
            INSTANCE.thread.start();
        
        return INSTANCE;
    }
    public static void stop() {
        CoreMod.logInfo("Closing Logger");
        INSTANCE.interrupt();
    }
    private void interrupt() {
        INSTANCE.thread.interrupt();
    }
    
    // Log clean
    public static void doCleanup() {
        // Ignore if disabled
        if (SewConfig.get(SewConfig.LOG_RESET_TIME) <= 0)
            return;
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("DELETE FROM `logging_Blocks` WHERE `updatedAt` <= (NOW() - INTERVAL ? MINUTE)")
            .addPrepared(SewConfig.get(SewConfig.LOG_RESET_INTERVAL).converToMinutes(SewConfig.get(SewConfig.LOG_RESET_TIME)))) {
            
            stmt.executeUpdate();
            CoreMod.logInfo( "Database cleanup completed" );
            
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
    }
    
    // Logging helpers
    public enum BlockAction {
        PLACE,
        BREAK,
        EXPLODE;
    }
    public enum LoggingIntervals {
        MINUTE( ChronoUnit.MINUTES ),
        HOUR( ChronoUnit.HOURS ),
        HALF_DAY( ChronoUnit.HALF_DAYS ),
        DAY( ChronoUnit.DAYS ),
        WEEK( ChronoUnit.WEEKS ),
        MONTHS( ChronoUnit.MONTHS ),
        YEAR( ChronoUnit.YEARS ),
        DECADE( ChronoUnit.DECADES );
        
        private final ChronoUnit timeUnit;
        
        LoggingIntervals(ChronoUnit timeUnit) {
            this.timeUnit = timeUnit;
        }
        
        public long converToMinutes( Long span ) {
            return Duration.of( span, this.timeUnit ).toMinutes();
        }
        
        public static boolean contains(@NotNull String key) {
            for ( LoggingIntervals i : LoggingIntervals.values() ) {
                if ( key.equalsIgnoreCase(i.name()) )
                    return true;
            }
            
            return false;
        }
    }
}
