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

import net.TheElm.project.MySQL.MySQLConnection;
import net.TheElm.project.MySQL.MySQLHost;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.MySQL.MySQLite;
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.LoggingUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

public abstract class CoreMod {
    
    public static final String MOD_ID = "sewing-machine";
    
    // Create ourselves a universal logger
    private static final Logger logger = LogManager.getLogger( MOD_ID );
    
    // Mod memory cache for claims
    public static final Map<ServerPlayerEntity, UUID> PLAYER_LOCATIONS = Collections.synchronizedMap(new WeakHashMap<>()); // Reference of where players are
    public static final Map<ServerPlayerEntity, UUID> PLAYER_WARP_INVITES = Collections.synchronizedMap(new WeakHashMap<>()); // Reference of warp invitations
    private static final Map<UUID, WeakReference<ClaimantPlayer>> PLAYER_CLAIM_CACHE = Collections.synchronizedMap(new HashMap<>()); // Reference from player UUID
    private static final Map<UUID, WeakReference<ClaimantTown>> TOWN_CLAIM_CACHE = Collections.synchronizedMap(new HashMap<>()); // Reference from owner UUID
    
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
    
    /*
     * Claimant storage
     */
    public static void addToCache(Claimant claimant) {
        if (claimant instanceof ClaimantPlayer)
            PLAYER_CLAIM_CACHE.put(claimant.getId(), new WeakReference<>((ClaimantPlayer) claimant));
        else if (claimant instanceof ClaimantTown)
            TOWN_CLAIM_CACHE.put(claimant.getId(), new WeakReference<>((ClaimantTown) claimant));
    }
    @Nullable
    public static Claimant removeFromCache(Claimant claimant) {
        if (claimant instanceof ClaimantPlayer) {
            WeakReference<ClaimantPlayer> reference;
            if ((reference = PLAYER_CLAIM_CACHE.remove(claimant.getId())) != null)
                return reference.get();
        } else if (claimant instanceof ClaimantTown) {
            WeakReference<ClaimantTown> reference;
            if ((reference = TOWN_CLAIM_CACHE.remove(claimant.getId())) != null)
                return reference.get();
        }
        return null;
    }
    @Nullable
    public static Claimant getFromCache(@NotNull Claimant.ClaimantType type, @NotNull UUID uuid) {
        return CoreMod.getCacheStream( type ).filter((claimant -> claimant.getId().equals(uuid))).findFirst().orElse( null );
    }
    public static Stream<Claimant> getCacheStream() {
        return CoreMod.getCacheStream( null );
    }
    public static Stream<Claimant> getCacheStream(@Nullable Claimant.ClaimantType type) {
        List<Claimant> out = new ArrayList<>();
        if ((type == null) || type.equals(Claimant.ClaimantType.PLAYER)) {
            ClaimantPlayer player;
            for (WeakReference<ClaimantPlayer> reference : PLAYER_CLAIM_CACHE.values()) {
                if ((player = reference.get()) != null)
                    out.add(player);
            }
        }
        if ((type == null) || type.equals(Claimant.ClaimantType.TOWN)) {
            ClaimantTown town;
            for ( WeakReference<ClaimantTown> reference : TOWN_CLAIM_CACHE.values() ) {
                if ((town = reference.get()) != null)
                    out.add( town );
            }
        }
        return out.stream();
    }
    
    /*
     * Fabric Elements
     */
    @NotNull
    public static MinecraftServer getServer() {
        Object server;
        if ((server = getFabric().getGameInstance()) instanceof MinecraftServer)
            return (MinecraftServer) server;
        throw new RuntimeException("Called Server object from illegal position.");
    }
    public static FabricLoader getFabric() {
        return FabricLoader.getInstance();
    }
    public static ModContainer getMod() {
        return CoreMod.getFabric().getModContainer(CoreMod.MOD_ID).orElseThrow(RuntimeException::new);
    }
    public static boolean isDebugging() {
        return CoreMod.getFabric().isDevelopmentEnvironment();
    }
    
    /*
     * Configurations
     */
    protected static boolean initDB() throws SQLException {
        SewingMachineConfig CONFIG = SewingMachineConfig.INSTANCE;
        ArrayList<String> tables = new ArrayList<>();
        ArrayList<String> alters = new ArrayList<>();
        
        /*if ( CONFIG.DO_MONEY.get() ) {
            tables.addAll(Collections.singletonList(
                "CREATE TABLE IF NOT EXISTS `player_Data` (`dataOwner` varchar(36) NOT NULL, `dataMoney` bigint(20) UNSIGNED NOT NULL, UNIQUE KEY `dataOwner` (`dataOwner`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;"
            ));
        }*/
        /*if ( CONFIG.DO_CLAIMS.get() ) {
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
        }*/
        if (( CONFIG.LOG_CHUNKS_CLAIMED.get() || CONFIG.LOG_CHUNKS_UNCLAIMED.get() ) && ( CONFIG.LOG_BLOCKS_BREAKING.get() || CONFIG.LOG_BLOCKS_PLACING.get() )) {
            String blockUpdateEnums = getDatabaseReadyEnumerators( LoggingUtils.BlockAction.class );
            
            tables.add(
                "CREATE TABLE IF NOT EXISTS `logging_Blocks` (`blockWorld` int(11) NOT NULL, `blockX` bigint(20) NOT NULL, `blockY` bigint(20) NOT NULL, `blockZ` bigint(20) NOT NULL, `block` blob NOT NULL, `updatedBy` varchar(36) NOT NULL, `updatedEvent` enum(" + blockUpdateEnums + ") NOT NULL, `updatedAt` datetime NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=latin1;"
            );
            alters.add(
                "ALTER TABLE `logging_Blocks` CHANGE `updatedEvent` `updatedEvent` ENUM(" + blockUpdateEnums + ") CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;"
            );
        }
        
        for ( String table : tables ) {
            String[] expl = table.split("`", 3);
            
            // Prepare the statement
            MySQLStatement statement = getSQL().prepare(table, false );
            
            // Execute
            CoreMod.logDebug( "Checking that database has table: " + expl[1] );
            statement.executeUpdate( true );
        }
        
        if (!alters.isEmpty()) {
            CoreMod.logInfo("Checking database enumerator fields");
            for (String prepare : alters) {
                // Prepare the statement
                getSQL().prepare(prepare, false)
                    .executeUpdate(true);
            }
        }
        
        return !tables.isEmpty();
    }
    protected static void checkLegacyDatabase() {
        ConfigOption<String> version = SewingMachineConfig.INSTANCE.CONFIG_VERSION;
        boolean isLegacy = (SewingMachineConfig.INSTANCE.preExisting() && (!version.wasUserDefined()));
        
        // Check if our version is considered legacy
        if (!isLegacy) {
            String versionString = version.get();
            switch (versionString) {
                case "${version}":
                case "1.0.0":
                case "1.0.1":
                case "1.0.2":
                case "1.0.3":
                case "1.0.4": {
                    isLegacy = true;
                    break;
                }
                default:
                    return;
            }
        }
        
        // Convert our database
        String[] notice = new String[]{
            "====================================================",
            "| SEWING MACHINE UTILS:",
            "|  YOU HAVE UPGRADED FROM A LEGACY VERSION",
            "|  PLEASE CONVERT YOUR CHUNK CLAIMS, IF ENABLED",
            "|  USING THE FOLLOWING COMMAND:",
            "|      /protection legacy-import",
            "====================================================="
        };
        CoreMod.logInfo(
            "Detected a legacy version"
            + System.lineSeparator() + String.join( System.lineSeparator(), notice )
        );
    }
    public static File getConfDir() throws RuntimeException {
        // Get the directory
        final File config = CoreMod.getFabric().getConfigDirectory();
        final File dir = new File(config, CoreMod.MOD_ID);
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
    public static final String logPrefix = "[SEW] ";
    public static void logInfo(@Nullable String message ) {
        logger.info( logPrefix + message );
    }
    public static void logInfo(@Nullable Text message ) {
        CoreMod.logInfo( message == null ? "NULL" : message.getString() );
    }
    public static void logInfo(@Nullable Object message ) {
        CoreMod.logInfo( message == null ? "NULL" : message.toString() );
    }
    
    public static void logDebug( String message ) {
        if (CoreMod.isDebugging())
            CoreMod.logInfo( message );
        else
            logger.debug( logPrefix + message );
    }
    public static void logDebug( @Nullable Text message ) {
        logDebug( message == null ? "NULL" : message.getString() );
    }
    
    public static void logError( String message ) {
        logger.error( logPrefix + message );
    }
    public static void logError( Text message ) {
        logError( message.getString() );
    }
    public static void logError( Throwable t ) {
        logger.catching( t );
    }
    
}
