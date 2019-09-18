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
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClaimantPlayer extends Claimant {
    
    private final Set<ClaimantTown> townInvites = Collections.synchronizedSet(new HashSet<>());
    private ClaimantTown town;
    
    private ClaimantPlayer(@NotNull UUID playerUUID) {
        super( ClaimantType.PLAYER, playerUUID );
        
        /*
         * Get Rank Permissions
         */
        /*try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `settingOption`, `settingRank` FROM `chunk_Settings` WHERE `settingOwner` = ?;")) {
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
        }*/
        
        /*
         * Get additional options
         */
        /*try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `optionName`, `optionValue` FROM `chunk_Options` WHERE `optionOwner` = ?;")) {
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
        }*/
        
        /*
         * Get Friend Ranks
         */
        /*try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `chunkFriend`, `chunkRank` FROM `chunk_Friends` WHERE `chunkOwner` = ?;")) {
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
        }*/
    }
    
    public final ClaimRanks getPermissionRankRequirement( ClaimPermissions permission ) {
        if ( this.RANK_PERMISSIONS.containsKey( permission ) )
            return this.RANK_PERMISSIONS.get( permission );
        return permission.getDefault();
    }
    
    /* Players Town Reference */
    @Nullable
    public final ClaimantTown getTown() {
        return this.town;
    }
    @Nullable
    public final UUID getTownId() {
        ClaimantTown town;
        if ((town = this.getTown()) == null)
            return null;
        return town.getId();
    }
    public final void setTown(@Nullable ClaimantTown town) {
        this.town = town;
        this.markDirty();
    }
    public final boolean inviteTown(@NotNull ClaimantTown town) {
        if (this.town != null) return false;
        return this.townInvites.add(town);
    }
    @Nullable
    public final ClaimantTown getTownInvite(@NotNull String townName) {
        ClaimantTown out = null;
        for (ClaimantTown town : this.townInvites) {
            if (townName.equals(town.getName().asString())) {
                out = town;
                break;
            }
        }
        if (out != null) this.townInvites.clear();
        return out;
    }
    public final Set<ClaimantTown> getTownInvites() {
        return this.townInvites;
    }
    
    /* Player Friend Options */
    @Override
    public final ClaimRanks getFriendRank(@Nullable UUID player) {
        if ( this.getId().equals( player ) )
            return ClaimRanks.OWNER;
        if (this.town != null)
            return this.town.getFriendRank( player );
        return super.getFriendRank( player );
    }
    
    /* Nickname Override */
    @Override
    public final Text getName() {
        if (this.name == null)
            return (this.name = PlayerNameUtils.fetchPlayerNick( this.getId() ));
        return this.name;
    }
    
    /* Claimed chunk options */
    public final boolean getProtectedChunkSetting(ClaimSettings setting) {
        if ( this.CHUNK_CLAIM_OPTIONS.containsKey( setting ) )
            return this.CHUNK_CLAIM_OPTIONS.get( setting );
        if ( CoreMod.spawnID.equals( this.getId() ) )
            return setting.getSpawnDefault();
        return setting.getPlayerDefault();
    }
    public final int getMaxChunkLimit() {
        return SewingMachineConfig.INSTANCE.PLAYER_CLAIMS_LIMIT.get();
    }
    
    /* Nbt saving */
    @Override
    public final void writeCustomDataToTag(@NotNull CompoundTag tag) {
        // Write the town ID
        if (this.town != null)
            tag.putUuid("town", this.town.getId());
        
        super.writeCustomDataToTag( tag );
    }
    @Override
    public final void readCustomDataFromTag(@NotNull CompoundTag tag) {
        // Get the players town
        ClaimantTown town = null;
        if ( tag.hasUuid("town") ) {
            try {
                town = ClaimantTown.get( tag.getUuid("town") );
                // Ensure that the town has the player in the ranks
                if ((town != null) && town.getFriendRank(this.getId()) == null) town = null;
            } catch (NbtNotFoundException ignored) {}
        }
        this.town = town;
        
        // Read from SUPER
        super.readCustomDataFromTag( tag );
    }
    
    /* Get the PlayerPermissions object from the cache */
    @Nullable
    public static ClaimantPlayer get(@NotNull UUID playerUUID) {
        Claimant player;
        
        // If claims are disabled
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return null;
        
        // If contained in the cache
        if ((player = CoreMod.getFromCache( ClaimantType.PLAYER, playerUUID )) != null)
            return (ClaimantPlayer) player;
        
        // Create new object
        return new ClaimantPlayer( playerUUID );
    }
    
}
