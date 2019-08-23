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
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.utilities.TownNameUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class ClaimantTown extends Claimant {
    
    private final UUID ownerId;
    
    protected ClaimantTown(@NotNull UUID townId, @NotNull UUID founderId, @Nullable Text name) {
        super(ClaimantType.TOWN, townId, name);
        this.ownerId = founderId;
        this.updateFriend( founderId, ClaimRanks.OWNER );
    }
    
    public String getTownType() {
        return TownNameUtils.getTownName( this.chunkCount, this.getResidentCount() );
    }
    public String getOwnerTitle() {
        return TownNameUtils.getOwnerTitle( this.chunkCount, this.getResidentCount(), true );
    }
    public UUID getOwner() {
        return this.ownerId;
    }
    public int getResidentCount() {
        return this.getFriends().size();
    }
    
    @Nullable
    public static ClaimantTown get(UUID ownerId) {
        // If claims are disabled
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return null;
        
        // Return null if null
        if (ownerId == null)
            return null;
        
        // Get the cached object
        if (CoreMod.TOWNS_CACHE.containsKey( ownerId ))
            return CoreMod.TOWNS_CACHE.get( ownerId );
        
        // Create new object
        Pair<Text, UUID> townInfo = TownNameUtils.fetchTownInfo( ownerId );
        
        // If owner is null, return null
        if ( townInfo.getRight() == null )
            return null;
        
        // Return the town object
        ClaimantTown obj = new ClaimantTown( ownerId, townInfo.getRight(), townInfo.getLeft() );
        CoreMod.TOWNS_CACHE.put( ownerId, obj );
        
        return obj;
    }
    public static UUID getPlayersTown(@NotNull UUID playerId) {
        // If claims are disabled
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get()) return null;
        
        UUID townId = null;
        
        try (MySQLStatement statement = CoreMod.getSQL().prepare("SELECT `t`.`townId` FROM `chunk_Towns` AS `t`, `player_Towns` AS `p` WHERE `p`.`townId` = `t`.`townId` AND `p`.`townPlayer` = ?;", false ).addPrepared( playerId )) {
            ResultSet rs = statement.executeStatement();
            while (rs.next()) {
                townId = UUID.fromString( rs.getString( "townId" ) );
            }
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
        
        return townId;
    }
    
}
