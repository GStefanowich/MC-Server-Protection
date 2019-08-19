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

package net.TheElm.project;

import net.TheElm.project.MySQL.MySQLHost;
import net.TheElm.project.MySQL.MySQLite;
import net.TheElm.project.commands.*;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.protections.BlockBreak;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.TheElm.project.MySQL.MySQLConnection;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.protections.EntityAttack;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class CoreMod implements DedicatedServerModInitializer {
    
    public static final String MOD_ID = "sewing-machine";
    
    // Create ourselves a universal logger
    private static final Logger logger = LogManager.getLogger( MOD_ID );
    
    public static final Map<ServerPlayerEntity, UUID> PLAYER_LOCATIONS = Collections.synchronizedMap(new WeakHashMap<>()); // Reference of where players are
    public static final Map<ServerPlayerEntity, UUID> PLAYER_WARP_INVITES = Collections.synchronizedMap(new WeakHashMap<>()); // Reference of warp invitations
    public static final Map<UUID, ClaimantPlayer> OWNER_CACHE = Collections.synchronizedMap(new HashMap<>()); // Reference from player UUID
    public static final Map<UUID, ClaimantTown> TOWNS_CACHE = Collections.synchronizedMap(new HashMap<>()); // Reference from owner UUID
    public static final Map<WorldChunk, ClaimedChunk> CHUNK_CACHE = Collections.synchronizedMap(new WeakHashMap<>()); // Reference from loaded Chunks
    
    public static final UUID spawnID = new UUID( 0, 0 );
    
    // MySQL Host
    private static MySQLHost MySQL = null;
    public static MySQLHost getSQL() {
        if ( MySQL == null ) {
            synchronized ( CoreMod.class ) {
                if ( MySQL == null )
                    MySQL = ( SewingMachineConfig.INSTANCE.DB_LITE.get() ?
                        new MySQLite() : new MySQLConnection()
                    );
            }
        }
        return MySQL;
    }
    
    @Override
    public void onInitializeServer() {
        CoreMod.logMessage( "Sewing Machine utilities mod is starting." );
        
        CommandRegistry REGISTRY = CommandRegistry.INSTANCE;
        
        CoreMod.logMessage( "Registering our commands." );
        REGISTRY.register(true, ClaimCommand::register );
        REGISTRY.register(true, GameModesCommand::register );
        REGISTRY.register(true, HoldingCommand::register );
        REGISTRY.register(true, MiscCommands::register );
        REGISTRY.register(true, MoneyCommand::register );
        REGISTRY.register(true, NickNameCommand::register );
        REGISTRY.register(true, PlayerSpawnCommand::register );
        REGISTRY.register(true, TeleportsCommand::register );
        REGISTRY.register(true, WaystoneCommand::register );
        
        // Create registry based listeners
        BlockBreak.init();
        EntityAttack.init();
        
        CoreMod.logMessage( "Initializing Database." );
        try {
            if (CoreMod.initDB())
                CoreMod.logMessage( "Database initialization finished" );
            else CoreMod.logMessage( "Skipping Database Initialization (Unused)" );
        } catch (SQLException e) {
            CoreMod.logMessage( "Error executing MySQL Database setup." );
            
            throw new RuntimeException( "Could not connect to database server.", e );
        }
        
        // Alert the mod presence
        CoreMod.logMessage( "Finished loading." );
    }
    
    private static boolean initDB() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        ArrayList<String> alters = new ArrayList<>();
        
        if ( SewingMachineConfig.INSTANCE.DO_MONEY.get() ) {
            tables.addAll(Collections.singletonList(
                "CREATE TABLE IF NOT EXISTS `player_Data` (`dataOwner` varchar(36) NOT NULL, `dataMoney` bigint(20) UNSIGNED NOT NULL, UNIQUE KEY `dataOwner` (`dataOwner`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;"
            ));
        }
        if ( SewingMachineConfig.INSTANCE.DO_CLAIMS.get() ) {
            String permissionEnum = getDatabaseReadyEnumerators( ClaimPermissions.class );
            String ranksEnum = getDatabaseReadyEnumerators( ClaimRanks.class );
            String settingEnum = getDatabaseReadyEnumerators( ClaimSettings.class );
            
            tables.addAll(Arrays.asList(
                "CREATE TABLE IF NOT EXISTS `chunk_Claimed` (`chunkX` bigint(20) NOT NULL, `chunkZ` bigint(20) NOT NULL,`chunkOwner` varchar(36) NOT NULL,`chunkTown` varchar(36) DEFAULT NULL,`chunkWorld` int(11) NOT NULL, UNIQUE KEY `chunkX` (`chunkX`,`chunkZ`,`chunkWorld`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;",
                "CREATE TABLE IF NOT EXISTS `chunk_Towns` (`townId` varchar(36) NOT NULL, `townOwner` varchar(36) NOT NULL,`townName` varchar(256) NOT NULL,UNIQUE KEY `townId` (`townId`),UNIQUE KEY `townOwner` (`townOwner`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;",
                "CREATE TABLE IF NOT EXISTS `chunk_Friends` (`chunkOwner` varchar(36) NOT NULL, `chunkFriend` varchar(36) NOT NULL,`chunkRank` enum(" + ranksEnum + ") NOT NULL, UNIQUE KEY `chunkOwner` (`chunkOwner`,`chunkFriend`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;",
                "CREATE TABLE IF NOT EXISTS `chunk_Settings` (`settingOwner` varchar(36) NOT NULL, `settingOption` enum(" + permissionEnum + ") NOT NULL, `settingRank` enum('OWNER','ALLY','PASSIVE','ENEMY') NOT NULL, UNIQUE KEY `settingOwner` (`settingOwner`,`settingOption`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;",
                "CREATE TABLE IF NOT EXISTS `chunk_Options` (`optionOwner` varchar(36) NOT NULL, `optionName` enum(" + settingEnum + ") NOT NULL, `optionValue` enum('TRUE','FALSE') NOT NULL, UNIQUE KEY `optionOwner` (`optionOwner`,`optionName`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;",
                "CREATE TABLE IF NOT EXISTS `player_Towns` (`townId` varchar(36) NOT NULL, `townPlayer` varchar(36) NOT NULL, UNIQUE KEY `townPlayer` (`townPlayer`), KEY `town_Reference` (`townId`), CONSTRAINT `town_Reference` FOREIGN KEY (`townId`) REFERENCES `chunk_Towns` (`townId`) ON DELETE CASCADE ON UPDATE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='player_Towns';"
            ));
            alters.addAll(Arrays.asList(
                "ALTER TABLE `chunk_Friends` CHANGE `chunkRank` `chunkRank` ENUM(" + ranksEnum + ") CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;",
                "ALTER TABLE `chunk_Settings` CHANGE `settingOption` `settingOption` ENUM(" + permissionEnum + ") CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;",
                "ALTER TABLE `chunk_Options` CHANGE `optionName` `optionName` ENUM(" + settingEnum + ") CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;"
            ));
        }
        
        for ( String table : tables ) {
            String[] expl = table.split("`", 3);
            
            // Prepare the statement
            MySQLStatement statement = getSQL().prepare(table, false );
            
            // Execute
            CoreMod.logDebug( "Checking that database has table: " + expl[1] );
            statement.executeUpdate( true );
        }
        
        if ( alters.size() > 0 ) {
            CoreMod.logMessage("Checking database enumerator fields");
            for (String prepare : alters) {
                // Prepare the statement
                getSQL().prepare(prepare, false)
                        .executeUpdate(true);
            }
        }
        
        return tables.size() > 0;
    }
    public static File getConfDir() throws RuntimeException {
        // Get the directory
        final File dir = new File("config" + File.separator + CoreMod.MOD_ID);
        // Make sure the directory exists
        if (!(dir.exists() || dir.mkdirs()))
            throw new RuntimeException("Error accessing the config");
        // Return the directory
        return dir;
    }
    
    /*
     * Get the name of our ENUMs for entry into the database
     */
    private static <T extends Enum<T>> String getDatabaseReadyEnumerators( Class<T> enumClass ) {
        Enum[] values = enumClass.getEnumConstants();
        List<String> out = new ArrayList<>();
        for ( Enum e : values ) {
            out.add( '\'' + e.name() + '\'' );
        }
        return String.join( ",", out );
    }
    
    /*
     * Our logger
     */
    public static void logMessage( String message ) {
        logger.info( "[SEW] " + message );
    }
    public static void logMessage( @Nullable Text message ) {
        logMessage( message == null ? "NULL" : message.getString() );
    }
    
    public static void logDebug( String message ) {
        logger.debug( "[SEW] " + message );
    }
    public static void logDebug( @Nullable Text message ) {
        logDebug( message == null ? "NULL" : message.getString() );
    }
    
    public static void logError( String message ) {
        logger.error( "[SEW] " + message );
    }
    public static void logError( Text message ) {
        logError( message.getString() );
    }
    public static void logError( Throwable t ) {
        logger.catching( t );
    }
    
}
