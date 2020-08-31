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

import com.mojang.datafixers.util.Either;
import net.TheElm.project.MySQL.MySQLConnection;
import net.TheElm.project.MySQL.MySQLHost;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.MySQL.MySQLite;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.protections.logging.EventLogger;
import net.TheElm.project.utilities.LegacyConverter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Stream;

public abstract class CoreMod {
    
    public static final String MOD_ID = "sewing-machine";
    
    // Create ourselves a universal logger
    private static final Logger logger = LogManager.getLogger();
    
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
                    MySQL = ( SewConfig.get(SewConfig.DB_LITE) ?
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
    public static <T extends Claimant> T getFromCache(@NotNull Class<T> type, @NotNull UUID uuid) {
        return CoreMod.getCacheStream( type ).filter((claimant) -> claimant.getId().equals(uuid)).findFirst().orElse( null );
    }
    @Nullable
    public static <T extends Claimant> T getFromCache(@NotNull Class<T> type, @NotNull String name) {
        return CoreMod.getCacheStream( type ).filter((claimant) -> name.equals(claimant.getName().asString())).findFirst().orElse( null );
    }
    public static Stream<Claimant> getCacheStream() {
        return CoreMod.getCacheStream( null );
    }
    public static <T extends Claimant> Stream<T> getCacheStream(@Nullable Class<T> type) {
        List<T> out = new ArrayList<>();
        if ((type == null) || type.equals(ClaimantPlayer.class)) {
            ClaimantPlayer player;
            for (WeakReference<ClaimantPlayer> reference : PLAYER_CLAIM_CACHE.values()) {
                if ((player = reference.get()) != null)
                    out.add((T) player);
            }
        }
        if ((type == null) || type.equals(ClaimantTown.class)) {
            ClaimantTown town;
            for ( WeakReference<ClaimantTown> reference : TOWN_CLAIM_CACHE.values() ) {
                if ((town = reference.get()) != null)
                    out.add((T) town);
            }
        }
        return out.stream();
    }
    
    public void initialize() {
        CoreMod.logInfo( "Sewing Machine utilities mod is starting." );
    }
    
    /*
     * Fabric Elements
     */
    public static Either<MinecraftServer, MinecraftClient> getGameInstance() {
        Object instance = getFabric().getGameInstance();
        if (instance instanceof MinecraftServer)
            return Either.left((MinecraftServer) instance);
        if (instance instanceof MinecraftClient)
            return Either.right((MinecraftClient) instance);
        throw new RuntimeException("Could not access game instance.");
    }
    public static FabricLoader getFabric() {
        return FabricLoader.getInstance();
    }
    public static ModContainer getMod() {
        return CoreMod.getFabric().getModContainer(CoreMod.MOD_ID).orElseThrow(RuntimeException::new);
    }
    public static ModMetadata getModMetaData() {
        return CoreMod.getMod().getMetadata();
    }
    public static String getModVersion() {
        return CoreMod.getModMetaData().getVersion().getFriendlyString();
    }
    public static boolean isDebugging() {
        return CoreMod.getFabric().isDevelopmentEnvironment();
    }
    public static boolean isServer() {
        return getGameInstance().left().isPresent();
    }
    
    /*
     * Configurations
     */
    protected static boolean initDB() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        ArrayList<String> alters = new ArrayList<>();
        
        if (( SewConfig.get(SewConfig.LOG_CHUNKS_CLAIMED) || SewConfig.get(SewConfig.LOG_CHUNKS_UNCLAIMED) ) && ( SewConfig.get(SewConfig.LOG_BLOCKS_BREAKING) || SewConfig.get(SewConfig.LOG_BLOCKS_PLACING) )) {
            String blockUpdateEnums = getDatabaseReadyEnumerators( EventLogger.BlockAction.class );
            
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
        if (!LegacyConverter.isLegacy())
            return;
        
        // Convert our database
        String[] notice = new String[]{
            "====================================================",
            "| SEWING MACHINE UTILS:",
            "|  YOU HAVE UPGRADED FROM A LEGACY VERSION",
            "|  PLEASE CONVERT YOUR CHUNK CLAIMS, IF ENABLED",
            "|  USING THE FOLLOWING COMMAND:",
            "|",
            "|      /protection legacy-import",
            "|",
            "| IF YOU DO NOT CONVERT TO THE NEW FORMAT, ALL",
            "| EXISTING CLAIM INFORMATION WILL BE LOST",
            "|",
            "| PLAYERS CANNOT JOIN DURING THE CONVERSION",
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
    public static void logInfo(@Nullable Text message ) {
        MutableText out = new LiteralText(logPrefix);
        if (message == null) out.append("NULL");
        else out.append(message);
        logger.info(out.getString());
    }
    public static void logInfo(@Nullable String message ) {
        CoreMod.logInfo(new LiteralText(message == null ? "NULL" : message));
    }
    public static void logInfo(@Nullable Object message ) {
        CoreMod.logInfo( message == null ? "NULL" : message.toString() );
    }
    
    public static void logDebug(@Nullable String message ) {
        if (CoreMod.isDebugging())
            CoreMod.logInfo( message );
    }
    public static void logDebug( @Nullable Text message ) {
        if (message == null) CoreMod.logDebug("NULL");
        else if (CoreMod.isDebugging()) CoreMod.logInfo(message);
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
    public static void logError( String message, Throwable error ) {
        logger.error( logPrefix + message, error );
    }
    public static void logError(@Nullable Object message ) {
        CoreMod.logError( message == null ? "NULL" : message.toString() );
    }
    
}
