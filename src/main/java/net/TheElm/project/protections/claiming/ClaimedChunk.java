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
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ClaimedChunk {
    
    private UUID chunkTown   = null;
    private UUID chunkPlayer = null;
    
    private final int world;
    private final int x;
    private final int z;
    
    private ClaimedChunk(World world, BlockPos blockPos) throws SQLException {
        // Get the chunk details
        this.world = world.getDimension().getType().getRawId();
        this.x = blockPos.getX() >> 4;
        this.z = blockPos.getZ() >> 4;
        
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `chunkOwner`, `chunkTown` FROM `chunk_Claimed` WHERE `chunkX` = ? AND `chunkZ` = ? AND `chunkWorld` = ?;")) {
            
            // Check the database for the chunk owner
            stmt.addPrepared(this.getX())
                .addPrepared(this.getZ())
                .addPrepared(this.getWorld());
            
            try ( ResultSet rs = stmt.executeStatement() ) {
                while (rs.next()) {
                    this.updatePlayerOwner(UUID.fromString(rs.getString("chunkOwner")));
                    if (rs.getString("chunkTown") != null)
                        this.updateTownOwner(UUID.fromString(rs.getString("chunkTown")));
                }
            }
        }
    }
    
    public int getX() {
        return this.x;
    }
    public int getZ() {
        return this.z;
    }
    
    public int getWorld() {
        return this.world;
    }
    
    public void updateTownOwner(@Nullable UUID owner) {
        
        
        this.chunkTown = owner;
        
        // Make sure we have the towns permissions cached
        if (owner != null)
            ClaimantTown.get( owner );
    }
    public void updatePlayerOwner(@Nullable UUID owner) {
        this.chunkPlayer = owner;
        
        // Make sure we have the players permissions cached
        if (owner != null)
            ClaimantPlayer.get( this.chunkPlayer);
    }
    
    @Nullable
    public UUID getOwner() {
        return this.chunkPlayer;
    }
    @Nullable
    public ClaimantTown getTown() {
        if ( this.chunkTown == null ) {
            if ( this.chunkPlayer == null )
                return null;
            ClaimantPlayer claimaint = ClaimantPlayer.get( this.chunkPlayer );
            if ( claimaint.getTown() != null ) {
                ClaimantTown town = ClaimantTown.get( claimaint.getTown() );
                if ( this.chunkPlayer.equals( town.getOwner() ) )
                    this.updateTownOwner(claimaint.getTown());
                return town;
            }
            return null;
        }
        ClaimantTown town = ClaimantTown.get( this.chunkTown );
        if ( town == null )
            this.updateTownOwner( null );
        return town;
    }
    
    public Text getOwnerName() {
        return this.getOwnerName( null );
    }
    public Text getOwnerName(@Nullable UUID zonePlayer) {
        if ( this.chunkPlayer == null ) {
            return new LiteralText(SewingMachineConfig.INSTANCE.NAME_WILDERNESS.get())
                .formatted(Formatting.GREEN);
        } else {
            // Get the permissions
            ClaimantPlayer permissions = ClaimantPlayer.get( this.chunkPlayer);
            
            // Get the players rank
            ClaimRanks zonePlayerRank = permissions.getFriendRank( zonePlayer );
            
            // Get the owners name
            Text output = permissions.getName();
            
            // Color based on the players rank with the owner
            return output.formatted(zonePlayerRank.getColor());
        }
    }
    
    public boolean canUserDo(@NotNull UUID player, ClaimPermissions perm) {
        if ( ( this.chunkPlayer == null ) || player.equals( this.chunkPlayer ) )
            return true;
        if ( ( this.getTown() != null ) && player.equals( this.getTown().getOwner() ) )
            return true;
        
        // Check our chunk permissions
        ClaimantPlayer permissions = ClaimantPlayer.get( this.chunkPlayer );
        
        // Get the ranks of the user and the rank required for performing
        ClaimRanks userRank = permissions.getFriendRank( player );
        ClaimRanks permReq = permissions.getPermissionRankRequirement( perm );
        
        // Return the test if the user can perform the action
        return permReq.canPerform( userRank );
    }
    public boolean isSetting(@NotNull ClaimSettings setting) {
        boolean permission;
        if ( this.chunkPlayer == null )
            permission = setting.getPlayerDefault();
        else 
            permission = ClaimantPlayer.get( this.chunkPlayer)
                .getProtectedChunkSetting( setting );
        return permission;
    }
    
    @Nullable
    public static ClaimedChunk convert(World world, BlockPos blockPos) {
        // If claims are disabled
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get()) return null;
        
        try {
            
            return ClaimedChunk.convertNonNull( world, blockPos );
            
        } catch (SQLException e) {
            CoreMod.logError( e );
        }
        
        return null;
    }
    public static ClaimedChunk convertNonNull(World world, BlockPos blockPos) throws SQLException {
        // Get the cached claim
        WorldChunk worldChunk = world.getWorldChunk( blockPos );
        if ( CoreMod.CHUNK_CACHE.containsKey( worldChunk ) )
            return CoreMod.CHUNK_CACHE.get( worldChunk );
        
        // Save this chunk to the cache (To be delisted when the chunk unloads)
        ClaimedChunk chunk = new ClaimedChunk(world, blockPos);
        CoreMod.CHUNK_CACHE.put(worldChunk, chunk);
        
        return chunk;
    }
    
    public static boolean isOwnedAround( BlockPos blockPos, int leniency ) {
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        
        int count = 0;
        try (MySQLStatement statement = CoreMod.getSQL().prepare("SELECT `chunkOwner` FROM `chunk_Claimed` WHERE `chunkX` > ? - " + leniency + " AND `chunkX` < ? + " + leniency + " AND `chunkZ` > ? - " + leniency + " AND `chunkZ` < ? +" + leniency, false)
            .addPrepared(chunkX)
            .addPrepared(chunkX)
            .addPrepared(chunkZ)
            .addPrepared(chunkZ)) {
            
            ResultSet resultSet = statement.executeStatement();
            while (resultSet.next())
                ++count;
            
        } catch (SQLException e) {
            e.printStackTrace();
            
        }
        
        return count > 0;
    }
    
}
