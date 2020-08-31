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

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.MoneyHolder;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.minecraft.datafixer.NbtOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LegacyConverter implements AutoCloseable {
    
    private static LegacyConverter INSTANCE = null;
    private static boolean RUNNING = false;
    
    private final long startedAt;
    private boolean success = false;
    private boolean clearTowns = false;
    private boolean clearPlayers = false;
    private boolean clearClaims = false;
    
    private final Map<UUID, ClaimantTown> modTownData = new HashMap<>();
    private final Map<UUID, ClaimantPlayer> modPlayerData = new HashMap<>();
    
    private LegacyConverter() {
        LegacyConverter.RUNNING = true;
        this.startedAt = new Date().getTime();
        try {
            // Disconnect players during conversion
            EntityUtils.kickAllPlayers(new LiteralText("Server is now performing an update."));
            
            this.clearTowns = this.convertTowns(ServerCore.get());
            this.clearPlayers = this.convertPlayers(ServerCore.get());
            this.clearClaims = this.convertClaims(ServerCore.get());
            this.success = true;
        } catch (NullPointerException e) {
            CoreMod.logError("Failed to complete conversion from legacy version.", e);
        } finally {
            LegacyConverter.RUNNING = false;
        }
    }
    
    public boolean convertTowns(MinecraftServer server) {
        try {
            /*
             * Import towns from the database
             */
            try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `townId`, `townOwner`, `townName` FROM `chunk_Towns`;")) {
                try (ResultSet rs = stmt.executeStatement()) {
                    while (rs.next()) {
                        MutableText townName = new LiteralText(rs.getString("townName"));
                        UUID townUUID = UUID.fromString(rs.getString("townId"));
                        UUID ownerUUID = UUID.fromString(rs.getString("townOwner"));
                        
                        // Make a NEW town
                        ClaimantTown town = ClaimantTown.makeTown(townUUID, ownerUUID, townName);
                        
                        // Store the town for later user
                        this.modTownData.put(townUUID, town);
                    }
                }
            }
            /*
             * Get the towns members
             */
            try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `townId`, `townPlayer` FROM `player_Towns`;")) {
                try (ResultSet rs = stmt.executeStatement()) {
                    while (rs.next()) {
                        ClaimantTown town = this.getTownDat(UUID.fromString(rs.getString("townId")));
                        if (town == null) continue;
                        
                        UUID playerUUID = UUID.fromString(rs.getString("townPlayer"));
                        if (playerUUID.equals(town.getOwner())) continue;
                        
                        town.updateFriend( playerUUID, ClaimRanks.ALLY );
                        town.markDirty();
                    }
                }
            }
        } catch (SQLException e) {
            if (this.shouldFail( e )) return false;
        }
        return true;
    }
    public boolean convertPlayers(MinecraftServer server) {
        /*
         * Get the players MONEY and TOWN
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `d`.`dataOwner`, `d`.`dataMoney`, `t`.`townId` FROM `player_Data` AS `d` LEFT JOIN `player_Towns` AS `t` ON `d`.`dataOwner` = `t`.`townPlayer`")) {
            try (ResultSet rs = stmt.executeStatement()) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("dataOwner"));
                    ClaimantTown town = (rs.getString("townId") == null ? null : this.getTownDat(UUID.fromString(rs.getString("townId"))));
                    int money = rs.getInt("dataMoney");
                    
                    // Get the players NBT data
                    CompoundTag playerDat;
                    try {
                        playerDat = NbtUtils.readOfflinePlayerData( playerUUID );
                        
                        // Store the NBT data to the tag
                        playerDat.putInt(MoneyHolder.SAVE_KEY, money);
                        
                        // Save
                        NbtUtils.writeOfflinePlayerData( playerUUID, playerDat );
                    } catch (NbtNotFoundException ignored) {}
                    
                    // Get the claimant NBT
                    ClaimantPlayer modDat = this.getPlayerDat(playerUUID);
                    
                    // Update the players town
                    if (town != null) modDat.setTown( town );
                }
            }
        } catch (SQLException e) {
            if (this.shouldFail( e )) return false;
        }
        /*
         * Get the players FRIENDS
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `chunkOwner`, `chunkFriend`, `chunkRank` FROM `chunk_Friends`;")) {
            try (ResultSet rs = stmt.executeStatement()) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("chunkOwner"));
                    UUID friendUUID = UUID.fromString(rs.getString("chunkFriend"));
                    ClaimRanks rank = ClaimRanks.valueOf(rs.getString("chunkRank"));
                    
                    ClaimantPlayer player = this.getPlayerDat(playerUUID);
                    player.updateFriend( friendUUID, rank );
                }
            }
        } catch (SQLException e) {
            if (this.shouldFail( e )) return false;
        }
        /*
         * Get the players PERMISSIONS
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `settingOwner`, `settingOption`, `settingRank` FROM `chunk_Settings`;")) {
            try (ResultSet rs = stmt.executeStatement()) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("settingOwner"));
                    ClaimPermissions permission = ClaimPermissions.valueOf(rs.getString("settingOption"));
                    ClaimRanks rank = ClaimRanks.valueOf(rs.getString("settingRank"));
                    
                    ClaimantPlayer player = this.getPlayerDat(playerUUID);
                    player.updatePermission( permission, rank );
                }
            }
        } catch (SQLException e) {
            if (this.shouldFail( e )) return false;
        }
        /*
         * Get the players OPTIONS
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `optionOwner`, `optionName`, `optionValue` FROM `chunk_Options`;")) {
            try (ResultSet rs = stmt.executeStatement()) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("optionOwner"));
                    ClaimSettings setting = ClaimSettings.valueOf(rs.getString("optionName"));
                    boolean enabled = Boolean.valueOf(rs.getString("optionValue"));
                    
                    ClaimantPlayer player = this.getPlayerDat(playerUUID);
                    player.updateSetting(setting, enabled);
                }
            }
        } catch (SQLException e) {
            if (this.shouldFail( e )) return false;
        }
        return true;
    }
    public boolean convertClaims(MinecraftServer server) {
        long check = System.currentTimeMillis() / 1000;
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `chunkX`, `chunkZ`, `chunkOwner`, `chunkTown`, `chunkWorld` FROM `chunk_Claimed` ORDER BY `chunkWorld` ASC, `chunkX` ASC, `chunkZ` ASC;" )) {
            // Iterate chunk results
            try (ResultSet rs = stmt.executeStatement()){
                while (rs.next()) {
                    // Get the dimension
                    byte dimId = (byte) rs.getInt("chunkWorld");
                    RegistryKey<World> dimension = LegacyConverter.getWorldFromId(dimId);
                    
                    // Get the world based on ID
                    ServerWorld world = server.getWorld(dimension);
                    
                    // Wait for 2 seconds, every second
                    if (check < ((System.currentTimeMillis() / 1000) - 1)) {
                        Thread.sleep( 2000 );
                        check = System.currentTimeMillis() / 1000;
                    }
                    
                    // If our world exists
                    if (world != null) {
                        int x = rs.getInt("chunkX");
                        int z = rs.getInt("chunkZ");
                        
                        WorldChunk chunk = (WorldChunk) world.getChunk(x, z, ChunkStatus.FULL);
                        IClaimedChunk claim = (IClaimedChunk) chunk;
                        
                        // Get the UUIDs from the database
                        String uuid;
                        UUID ownerUUID = UUID.fromString(rs.getString("chunkOwner"));
                        UUID townUUID = ((uuid = rs.getString("chunkTown")) == null ? null : UUID.fromString( uuid ));
                        
                        // Save chunk to player NBT
                        ClaimantPlayer player = this.getPlayerDat( ownerUUID );
                        player.addToCount( chunk );
                        
                        // Update the chunk
                        claim.updatePlayerOwner( ownerUUID );
                        claim.updateTownOwner( townUUID );
                        
                        // ENSURE THE CHUNK GETS SAVED
                        chunk.markDirty();
                        
                        // Obtain the chunk
                        CoreMod.logInfo(String.format("Converting chunk %d, %d to \"%s\" in %s", x, z, (ownerUUID.equals(CoreMod.spawnID) ? "Spawn" : ownerUUID.toString()), dimension.toString()));
                    }
                }
            }
            
            // Save the world after completion
            server.save(true, false, true);
        } catch (SQLException e) {
            if (this.shouldFail( e )) return false;
        } catch (InterruptedException e) {
            CoreMod.logError( e );
            return false;
        }
        return true;
    }
    
    @Override
    public void close() {
        try {
            long millis = new Date().getTime() - this.startedAt;
            if (this.success) {
                String message = "";
                Set<String> tables = new HashSet<>();
                
                // Remove old databases
                if (this.clearClaims) {
                    message += (message.isEmpty() ? "" : System.lineSeparator()) + "- Importing Claimed Chunks completed.";
                    tables.add("`chunk_Claimed`");
                }
                if (this.clearPlayers) {
                    message += (message.isEmpty() ? "" : System.lineSeparator()) + "- Importing Player Settings completed.";
                    tables.addAll(Arrays.asList(
                        "`player_Data`",
                        "`chunk_Friends`",
                        "`chunk_Options`",
                        "`chunk_Settings`"
                    ));
                }
                if (this.clearTowns) {
                    message += (message.isEmpty() ? "" : System.lineSeparator()) + "- Importing Town Settings completed.";
                    tables.add("`chunk_Towns`");
                }
                if (this.clearPlayers && this.clearTowns)
                    tables.add("`player_Towns`");
                
                if ((!message.isEmpty()) && (!tables.isEmpty())) {
                    message += System.lineSeparator()
                        + "  Tables may now be manually deleted in MySQL:"
                        + System.lineSeparator() + System.lineSeparator()
                        + "   USE `minecraft_vanilla_claims`; DROP TABLE IF EXISTS " + String.join(", ", tables) + ";"
                        + System.lineSeparator();
                }
                
                // Completed
                CoreMod.logInfo("Legacy conversion completed. [" + millis + "ms]"
                    + System.lineSeparator() + message
                );
                return;
            }
            CoreMod.logError("Legacy conversion failed. [" + millis + "ms]");
        } finally {
            this.modTownData.clear();
            this.modPlayerData.clear();
        }
    }
    
    /*
     * Mod NBT Data
     */
    private @NotNull ClaimantPlayer getPlayerDat(UUID playerUUID) {
        if (!this.modPlayerData.containsKey(playerUUID))
            this.modPlayerData.put(playerUUID, ClaimantPlayer.get(playerUUID));
        return this.modPlayerData.get(playerUUID);
    }
    private @Nullable ClaimantTown getTownDat(UUID townUUID) {
        if (!this.modTownData.containsKey(townUUID))
            return null;
        return this.modTownData.get(townUUID);
    }
    
    /*
     * Exception handling
     */
    private boolean shouldFail(SQLException exception) {
        // If table doesn't exist
        if (exception.getErrorCode() != 1146) {
            CoreMod.logError( exception );
            return true;
        }
        return false;
    }
    
    /*
     * Static GETTERs
     */
    public static @Nullable LegacyConverter create() {
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
        ConfigOption<String> version = SewConfig.CONFIG_VERSION;
        boolean isLegacy = (SewConfig.preExisting() && (!version.wasUserDefined()));
        
        // Check if our version is considered legacy
        if (isLegacy)
            return true;
        
        String versionString = SewConfig.get(version);
        switch (versionString) {
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
    
    public static @NotNull RegistryKey<World> getWorldFromId(int id) {
        return LegacyConverter.getWorldFromId(IntTag.of(id));
    }
    public static @NotNull RegistryKey<World> getWorldFromId(IntTag id) {
        DataResult<RegistryKey<World>> worlds = DimensionType.method_28521(new Dynamic<>(NbtOps.INSTANCE, id));
        return worlds.resultOrPartial(CoreMod::logError).orElse(World.OVERWORLD);
    }
    
}
