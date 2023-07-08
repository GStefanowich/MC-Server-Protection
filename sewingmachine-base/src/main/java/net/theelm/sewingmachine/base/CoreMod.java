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

package net.theelm.sewingmachine.base;

import com.mojang.brigadier.Message;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.theelm.sewingmachine.MySQL.MySQLConnection;
import net.theelm.sewingmachine.MySQL.MySQLHost;
import net.theelm.sewingmachine.MySQL.MySQLStatement;
import net.theelm.sewingmachine.MySQL.MySQLite;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.base.objects.inventory.BackpackScreenHandler;
import net.theelm.sewingmachine.blocks.entities.LecternGuideBlockEntity;
import net.theelm.sewingmachine.blocks.entities.LecternWarpsBlockEntity;
import net.theelm.sewingmachine.commands.ModCommands;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.base.objects.ShopStats;
import net.theelm.sewingmachine.objects.SewModules;
import net.theelm.sewingmachine.protections.logging.EventLogger;
import net.theelm.sewingmachine.utilities.DevUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;

import net.theelm.sewingmachine.utilities.mod.Sew;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public abstract class CoreMod {
    // Create ourselves a universal logger
    private static final Logger logger = LogManager.getLogger();
    
    // Mod memory cache for claims
    public static final Map<ServerPlayerEntity, Pair<UUID, String>> PLAYER_WARP_INVITES = Collections.synchronizedMap(new WeakHashMap<>()); // Reference of warp invitations
    
    public static final @NotNull UUID SPAWN_ID = Util.NIL_UUID;    
    
    public static final BlockEntityType<LecternGuideBlockEntity> GUIDE_BLOCK_ENTITY;
    public static final BlockEntityType<LecternWarpsBlockEntity> WARPS_BLOCK_ENTITY;
    
    public static final ScreenHandlerType<BackpackScreenHandler> BACKPACK;
    
    static {
        GUIDE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, Sew.modIdentifier("guide_lectern"), FabricBlockEntityTypeBuilder.create(LecternGuideBlockEntity::new, Blocks.LECTERN).build(null));
        WARPS_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, Sew.modIdentifier("warps_lectern"), FabricBlockEntityTypeBuilder.create(LecternWarpsBlockEntity::new, Blocks.LECTERN).build(null));

        BACKPACK = Registry.register(Registries.SCREEN_HANDLER, Sew.modIdentifier("backpack"), new ScreenHandlerType<>(BackpackScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    }
    
    // MySQL Host
    private static MySQLHost MySQL = null;
    public static MySQLHost getSQL() {
        if ( CoreMod.MySQL == null ) {
            synchronized ( CoreMod.class ) {
                if ( CoreMod.MySQL == null )
                    CoreMod.MySQL = ( SewConfig.get(SewCoreConfig.DB_LITE) ?
                        new MySQLite() : new MySQLConnection()
                    );
            }
        }
        return CoreMod.MySQL;
    }
    
    public void initialize() {
        // Log that we're starting!
        CoreMod.logInfo("Sewing Machine utilities mod is starting.");
        
        // Make sure the Stats we're using are in the Registry before it gets Frozen
        ShopStats.init();
        
        List<SewCommand> commands = new ArrayList<>();
        List<SewPlugin> plugins = this.getPlugins();
        
        commands.add(new ModCommands(plugins));
        
        for (SewPlugin plugin : plugins) {
            CoreMod.logInfo("Sew Plugin: " + plugin.getClass());
            
            // Add the configs of any plugin
            plugin.getConfigClass()
                .ifPresent(SewConfig::addConfigClass);
            
            SewCommand[] pluginCommands = plugin.getCommands();
            if (pluginCommands != null)
                for (SewCommand command : pluginCommands)
                    if (command != null)
                        commands.add(command);
        }
        
        SewConfig.firstInitialize();
        
        // Register the server commands
        if (commands.isEmpty())
            CoreMod.logInfo("No commands to register.");
        else {
            CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
                CoreMod.logInfo("Registering our " + commands.size() + " commands.");
                
                // Register commands from plugins
                for (SewCommand command : commands)
                    if (command != null)
                        command.register(dispatcher, registry);
            });
        }
    }
    
    protected @NotNull List<@NotNull SewPlugin> getPlugins() {
        List<SewPlugin> plugins = new ArrayList<>();
        for (EntrypointContainer<Object> entry : this.getEntryPoints()) {
            ModContainer container = entry.getProvider();
            ModMetadata metadata = container.getMetadata();
            if (
                metadata.getId().startsWith(SewModules.MOD_PREFIX)
                && entry.getEntrypoint() instanceof SewPlugin plugin
            ) plugins.add(plugin);
        }
        
        return plugins;
    }
    protected @NotNull List<ModMetadata> getPluginMetadata() {
        List<ModMetadata> metadatas = new ArrayList<>();
        for (EntrypointContainer<Object> entry : this.getEntryPoints()) {
            ModContainer container = entry.getProvider();
            ModMetadata metadata = container.getMetadata();
            if (
                metadata.getId().startsWith(SewModules.MOD_PREFIX)
                && entry.getEntrypoint() instanceof SewPlugin
            ) metadatas.add(metadata);
        }
        
        return metadatas;
    }
    protected @NotNull Map<String, String> matchPluginMetadata(@NotNull Map<String, String> modules) {
        return this.matchPluginMetadata(modules, false);
    }
    protected @NotNull Map<String, String> matchPluginMetadata(@NotNull Map<String, String> modules, boolean trim) {
        List<ModMetadata> metadatas = this.getPluginMetadata();
        Map<String, String> match = new HashMap<>();
        
        // Iterate the modules that the client has
        for (ModMetadata metadata : metadatas) {
            String id = metadata.getId();
            String compare = modules.get(id);
            if (compare == null)
                continue;
            
            if (trim && id.startsWith(SewModules.MOD_PREFIX))
                id = id.substring(SewModules.MOD_PREFIX.length());
            
            String version = metadata.getVersion()
                .getFriendlyString();
            if (compare.equals(version))
                match.put(id, version);
        }
        
        return match;
    }
    private @NotNull List<EntrypointContainer<Object>> getEntryPoints() {
        FabricLoader fabric = Sew.getFabric();
        List<String> environments = new ArrayList<>();
        
        environments.add("main");
        environments.add(fabric.getEnvironmentType()
            .name()
            .toLowerCase());
        
        List<EntrypointContainer<Object>> entries = new ArrayList<>();
        for (String environment : environments)
            entries.addAll(fabric.getEntrypointContainers(environment, Object.class));
        return entries;
    }
    
    /*
     * Fabric Elements
     */
    public static @NotNull FabricLoader getFabric() {
        return FabricLoader.getInstance();
    }
    public static @NotNull ModMetadata getModMetaData() {
        return Sew.getMod(SewModules.BASE).getMetadata();
    }
    public static @NotNull String getModVersion() {
        return CoreMod.getModMetaData().getVersion().getFriendlyString();
    }
    
    protected static boolean initDB() throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        ArrayList<String> alters = new ArrayList<>();
        
        if (( SewConfig.get(SewCoreConfig.LOG_CHUNKS_CLAIMED) || SewConfig.get(SewCoreConfig.LOG_CHUNKS_UNCLAIMED) ) && ( SewConfig.get(SewCoreConfig.LOG_BLOCKS_BREAKING) || SewConfig.get(SewCoreConfig.LOG_BLOCKS_PLACING) )) {
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
    
    /*
     * Get the name of our ENUMs for entry into the database
     */
    private static @NotNull <T extends Enum<T>> String getDatabaseReadyEnumerators(@NotNull Class<T> enumClass) {
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
    public static void logInfo(@Nullable Text message) {
        MutableText out = Text.literal(logPrefix);
        if (message == null) out.append("NULL");
        else out.append(message);
        logger.info(out.getString());
    }
    public static void logInfo(@Nullable String message) {
        CoreMod.logInfo(Text.literal(message == null ? "NULL" : message));
    }
    public static void logInfo(@Nullable Object message) {
        CoreMod.logInfo( message == null ? "NULL" : message.toString() );
    }
    
    public static void logDebug(@Nullable String message) {
        if (DevUtils.isDebugging())
            CoreMod.logInfo( message );
    }
    public static void logDebug(@Nullable Text message) {
        if (message == null) CoreMod.logDebug("NULL");
        else if (DevUtils.isDebugging()) CoreMod.logInfo(message);
    }
    
    public static void logError(String message) {
        logger.error( logPrefix + message );
    }
    public static void logError(@NotNull Message message) {
        logError(message.getString());
    }
    public static void logError(Throwable t) {
        logger.catching(t);
    }
    public static void logError(String message, Throwable error) {
        logger.error( logPrefix + message, error );
    }
    public static void logError(@Nullable Object message ) {
        CoreMod.logError(message == null ? "NULL" : message.toString());
    }
    
}
