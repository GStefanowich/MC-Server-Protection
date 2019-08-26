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
import net.TheElm.project.enums.ClaimRanks;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class Claimant {
    
    private final Map<UUID, ClaimRanks> USER_RANKS = Collections.synchronizedMap(new HashMap<>());
    
    private final ClaimantType type;
    private final UUID id;
    private Text name;
    
    protected int chunkCount = 0;
    
    protected Claimant(ClaimantType type, @NotNull UUID owner, @Nullable Text name) {
        this.type = type;
        this.id = owner;
        this.name = name;
        
        String sqlSelector = ( this.type.equals(ClaimantType.PLAYER) ? "chunkOwner" : "chunkTown" );
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT COUNT(`" + sqlSelector + "`) AS `count` FROM `chunk_Claimed` WHERE `" + sqlSelector + "` = ?;", false)) {
            
            ResultSet rs = stmt.addPrepared( owner )
                .executeStatement();
            while (rs.next()) {
                this.chunkCount += rs.getInt( "count" );
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /* Player Friend Options */
    public ClaimRanks getFriendRank( UUID player ) {
        return this.USER_RANKS.getOrDefault( player, ClaimRanks.PASSIVE );
    }
    public final void updateFriend( UUID player, ClaimRanks rank ) {
        this.USER_RANKS.put( player, rank );
    }
    public final void removeFriend( UUID player ) {
        this.USER_RANKS.remove( player );
    }
    protected final Set<UUID> getFriends() {
        return this.USER_RANKS.keySet();
    }
    
    /* Get the latest name */
    public final UUID getId() {
        return this.id;
    }
    public Text getName() {
        return this.name.deepCopy();
    }
    public final Text getName(@NotNull UUID player) {
        ClaimRanks playerRank = this.getFriendRank( player );
        return this.getName().formatted( playerRank.getColor() );
    }
    
    /* Town types */
    public final ClaimantType getType() {
        return this.type;
    }
    public final int adjustCount(int size) {
        return (this.chunkCount += size);
    }
    public final int resetCount() {
        return (this.chunkCount = 0);
    }
    public final int getCount() {
        return this.chunkCount;
    }
    
    enum ClaimantType {
        TOWN,
        PLAYER
    }
}
