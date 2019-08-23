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

package net.TheElm.project.protections.claiming;

import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class ClaimantPlayer extends Claimant {
    
    private static MySQLStatement PERMISSIONS_LOOKUP;
    
    private final Map<ClaimSettings, Boolean> CHUNK_CLAIM_OPTIONS = Collections.synchronizedMap(new HashMap<>());
    private final Map<ClaimPermissions, ClaimRanks> RANK_PERMISSIONS = Collections.synchronizedMap(new HashMap<>());
    
    private UUID townID;
    
    private ClaimantPlayer(@NotNull UUID ownerId) {
        super( ClaimantType.PLAYER, ownerId, PlayerNameUtils.fetchPlayerName( ownerId ) );
        
        this.townID = ClaimantTown.getPlayersTown( this.getId() );
        
        /*
         * Get Rank Permissions
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `settingOption`, `settingRank` FROM `chunk_Settings` WHERE `settingOwner` = ?;")) {
            stmt.addPrepared(this.getId());
            try (ResultSet ranks = stmt.executeStatement()) {
                while (ranks.next()) {
                    // Get rank options
                    ClaimPermissions perm = ClaimPermissions.valueOf(ranks.getString("settingOption"));
                    ClaimRanks rank = ClaimRanks.valueOf(ranks.getString("settingRank"));

                    // Save rank option
                    this.updatePermission(perm, rank);
                }
            }
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
        
        /*
         * Get additional options
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `optionName`, `optionValue` FROM `chunk_Options` WHERE `optionOwner` = ?;")) {
            stmt.addPrepared(this.getId());
            try (ResultSet settings = stmt.executeStatement()) {
                while (settings.next()) {
                    // Get rank options
                    ClaimSettings setting = ClaimSettings.valueOf(settings.getString("optionName"));
                    Boolean enabled = Boolean.parseBoolean(settings.getString("optionValue"));
                    // Save options
                    this.updateSetting(setting, enabled);
                }
            }
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
        
        /*
         * Get Friend Ranks
         */
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `chunkFriend`, `chunkRank` FROM `chunk_Friends` WHERE `chunkOwner` = ?;")) {
            stmt.addPrepared(this.getId());
            try (ResultSet friends = stmt.executeStatement()) {
                while (friends.next()) {
                    // Get friend information
                    UUID friend = UUID.fromString(friends.getString("chunkFriend"));
                    ClaimRanks rank = ClaimRanks.valueOf(friends.getString("chunkRank"));
                    // Save friend
                    this.updateFriend(friend, rank);
                }
            }
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
    }
    
    public final ClaimRanks getPermissionRankRequirement( ClaimPermissions permission ) {
        if ( this.RANK_PERMISSIONS.containsKey( permission ) )
            return this.RANK_PERMISSIONS.get( permission );
        return permission.getDefault();
    }
    
    /* Owner Options */
    public final void updateSetting(ClaimSettings setting, Boolean bool) {
        this.CHUNK_CLAIM_OPTIONS.put( setting, bool );
    }
    public final void updatePermission(ClaimPermissions permission, ClaimRanks rank) {
        this.RANK_PERMISSIONS.put( permission, rank );
    }
    
    /* Players Town Reference */
    public void updateTown(UUID townID) {
        ClaimantTown town;
        // Update chunks of old-town
        if ((this.townID != null) && ((town = ClaimantTown.get(this.townID)) != null)) town.chunkCount--;
        
        this.townID = townID;
        
        // Update chunks of new-town
        if ((this.townID != null) && ((town = ClaimantTown.get(this.townID)) != null)) town.chunkCount++;
    }
    public UUID getTown() {
        return this.townID;
    }
    
    /* Claimed chunk options */
    public final boolean getProtectedChunkSetting(ClaimSettings setting) {
        if ( this.CHUNK_CLAIM_OPTIONS.containsKey( setting ) )
            return this.CHUNK_CLAIM_OPTIONS.get( setting );
        if ( CoreMod.spawnID.equals( this.getId() ) )
            return setting.getSpawnDefault();
        return setting.getPlayerDefault();
    }
    
    /* Get the PlayerPermissions object from the cache */
    @Nullable
    public static ClaimantPlayer get(@NotNull PlayerEntity player) {
        return ClaimantPlayer.get( player.getUuid() );
    }
    @Nullable
    public static ClaimantPlayer get(@NotNull UUID ownerId ) {
        // If claims are disabled
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return null;
        
        // If contained in the cache
        if (CoreMod.OWNER_CACHE.containsKey( ownerId ))
            return CoreMod.OWNER_CACHE.get( ownerId );
        
        // Create new object
        ClaimantPlayer obj = new ClaimantPlayer( ownerId );
        CoreMod.OWNER_CACHE.put( ownerId, obj );
        
        return obj;
    }
    
}
