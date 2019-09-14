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
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class LegacyConverter implements AutoCloseable {
    
    private static LegacyConverter INSTANCE = null;
    private static boolean RUNNING = false;
    
    private final long startedAt;
    private boolean success = false;
    private boolean clearTowns = false;
    private boolean clearPlayers = false;
    private boolean clearClaims = false;
    
    private LegacyConverter() {
        LegacyConverter.RUNNING = true;
        try {
            this.startedAt = new Date().getTime();
            
            // Disconnect players during conversion
            EntityUtils.kickAllPlayers(new LiteralText("Server is now performing an update."));
            
            this.clearTowns = this.convertTowns(CoreMod.getServer());
            this.clearPlayers = this.convertPlayers(CoreMod.getServer());
            this.clearClaims = this.convertClaims(CoreMod.getServer());
            this.success = true;
        } finally {
            LegacyConverter.RUNNING = false;
        }
    }
    
    public boolean convertTowns(MinecraftServer server) {
        if (true) return false;
        try {
            // TODO: Import towns from the database
            try (MySQLStatement stmt = CoreMod.getSQL().prepare("")) {
                
            }
            try (MySQLStatement stmt = CoreMod.getSQL().prepare("")) {
                
            }
        } catch (SQLException e) {
            // If table doesn't exist
            if (e.getErrorCode() != 1146) {
                CoreMod.logInfo(e);
                return false;
            }
        }
        return true;
    }
    public boolean convertPlayers(MinecraftServer server) {
        try {
            // TODO: Import player settings from the database
            try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `d`.`dataOwner`, `d`.`dataMoney`, `t`.`townId` FROM `player_Data` AS `d` LEFT JOIN `player_Towns` AS `t` ON `d`.`dataOwner` = `t`.`townPlayer`")) {
                try (ResultSet rs = stmt.executeStatement()) {
                    while (rs.next()) {
                        UUID playerUUID = UUID.fromString(rs.getString("dataOwner"));
                        UUID townID = (rs.getString("dataOwner") == null ? null : UUID.fromString(rs.getString("dataOwner")));
                        int money = rs.getInt("dataMoney");
    
                        
                    }
                }
            }
        } catch (SQLException e) {
            // If table doesn't exist
            if (e.getErrorCode() != 1146) {
                CoreMod.logInfo(e);
                return false;
            }
        }
        return true;
    }
    public boolean convertClaims(MinecraftServer server) {
        if (true) return false;
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `chunkX`, `chunkZ`, `chunkOwner`, `chunkTown`, `chunkWorld` FROM `chunk_Claimed` ORDER BY `chunkWorld` ASC, `chunkX` ASC, `chunkZ` ASC;" )) {
            // Iterate chunk results
            try (ResultSet rs = stmt.executeStatement()){
                while (rs.next()) {
                    // Get the world based on ID
                    World world = server.getWorld(DimensionType.byRawId(rs.getInt("chunkWorld")));
    
                    // If our world exists
                    if (world != null) {
                        int x = rs.getInt("chunkX");
                        int z = rs.getInt("chunkZ");
    
                        // Obtain the chunk
                        CoreMod.logInfo(String.format("Converting chunk %d, %d", x, z));
                        WorldChunk chunk = (WorldChunk) world.getChunk(x, z, ChunkStatus.FULL);
                        IClaimedChunk claim = (IClaimedChunk) chunk;
    
                        // Get the UUIDs from the database
                        String ownerUUID = rs.getString("chunkOwner");
                        String townUUID = rs.getString("chunkTown");
    
                        // Update the chunk
                        claim.updatePlayerOwner(ownerUUID == null ? null : UUID.fromString(ownerUUID));
                        claim.updateTownOwner(townUUID == null ? null : UUID.fromString(townUUID));
                    }
                }
            }
            
            // Save the world after completion
            server.save(true, false, true);
        } catch (SQLException e) {
            // If table doesn't exist
            if (e.getErrorCode() != 1146) {
                CoreMod.logInfo(e);
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void close() {
        long millis = new Date().getTime() - this.startedAt;
        if ( this.success ) {
            String message = "";
            Set<String> tables = new HashSet<>();
            
            // Remove old databases
            if ( this.clearClaims ) {
                message += ( message.isEmpty() ? "" : System.lineSeparator()) + "- Importing Claimed Chunks completed.";
                tables.add("chunk_Claimed");
            }
            if ( this.clearPlayers ) {
                message += ( message.isEmpty() ? "" : System.lineSeparator()) + "- Importing Player Settings completed.";
                tables.addAll(Arrays.asList(
                    "player_Data",
                    "chunk_Friends",
                    "chunk_Options",
                    "chunk_Settings"
                ));
            }
            if ( this.clearTowns ) {
                message += ( message.isEmpty() ? "" : System.lineSeparator()) + "- Importing Town Settings completed.";
                tables.add("chunk_Towns");
            }
            if ( this.clearPlayers && this.clearTowns )
                tables.add( "player_Towns" );
            
            if ( (!message.isEmpty()) && (!tables.isEmpty()) ) {
                message += "  Tables may now be manually deleted in MySQL:"
                    + System.lineSeparator()
                    + "   \"USE `minecraft_vanilla_claims`; DROP TABLE IF EXISTS " + String.join(", ", tables) + ";\"";
            }
            
            // Completed
            CoreMod.logInfo( "Legacy conversion completed. [" + millis + "ms]" );
            return;
        }
        CoreMod.logError( "Legacy conversion failed. [" + millis + "ms]" );
    }
    
    @Nullable
    public static LegacyConverter create() {
        if ( LegacyConverter.INSTANCE != null ) return null;
        synchronized ( LegacyConverter.class ) {
            if ( LegacyConverter.INSTANCE != null ) return null;
            LegacyConverter out = new LegacyConverter();
            return ( LegacyConverter.INSTANCE = out );
        }
    }
    public static boolean exists() {
        return LegacyConverter.INSTANCE != null;
    }
    public static boolean running() {
        return LegacyConverter.RUNNING;
    }
    public static boolean isLegacy() {
        ConfigOption<String> version = SewingMachineConfig.INSTANCE.CONFIG_VERSION;
        boolean isLegacy = (SewingMachineConfig.INSTANCE.preExisting() && (!version.wasUserDefined()));
        
        // Check if our version is considered legacy
        if (isLegacy)
            return true;
        
        String versionString = version.get();
        switch (versionString) {
            case "${version}":
            case "1.0.0":
            case "1.0.1":
            case "1.0.2":
            case "1.0.3":
            case "1.0.4": {
                return true;
            }
            default:
                return false;
        }
    }
    
}
