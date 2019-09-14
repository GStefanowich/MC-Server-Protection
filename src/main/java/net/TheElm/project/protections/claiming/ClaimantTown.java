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
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.utilities.NbtUtils;
import net.TheElm.project.utilities.TownNameUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ClaimantTown extends Claimant {
    
    private UUID ownerId;
    
    protected ClaimantTown(@NotNull UUID townId) {
        super(ClaimantType.TOWN, townId);
    }
    protected ClaimantTown(@NotNull UUID townId, @NotNull Text townName) {
        this( townId );
        this.name = townName;
    }
    
    public final String getTownType() {
        return TownNameUtils.getTownName( this.getCount(), this.getResidentCount() );
    }
    public final String getOwnerTitle() {
        return TownNameUtils.getOwnerTitle( this.getCount(), this.getResidentCount(), true );
    }
    public final UUID getOwner() {
        return this.ownerId;
    }
    public final void setOwner(@NotNull UUID owner) {
        this.updateFriend( owner, ClaimRanks.OWNER );
        this.ownerId = owner;
        this.markDirty();
    }
    public final int getResidentCount() {
        return this.getFriends().size();
    }
    
    @Override
    public Text getName() {
        return this.name.deepCopy();
    }
    
    /* Player Friend Options */
    @Override
    public final ClaimRanks getFriendRank( UUID player ) {
        if ( this.getOwner().equals( player ) )
            return ClaimRanks.OWNER;
        return super.getFriendRank( player );
    }
    @Override
    public final boolean updateFriend(@NotNull final UUID player, @Nullable final ClaimRanks rank) {
        if ( super.updateFriend( player, rank ) ) {
            ClaimantPlayer claim = ClaimantPlayer.get( player );
            if (claim != null)
                claim.setTown(rank == null ? null : this);
            return true;
        }
        return false;
    }
    
    /* Nbt saving */
    @Override
    public final void writeCustomDataToTag(@NotNull CompoundTag tag) {
        if (this.ownerId == null) throw new RuntimeException("Town owner should not be null");
        
        tag.putUuid( "owner", this.ownerId );
        
        if ( this.name != null )
            tag.putString("name", Text.Serializer.toJson( this.name ));
        
        // Write to tag
        super.writeCustomDataToTag( tag );
    }
    @Override
    public final void readCustomDataFromTag(@NotNull CompoundTag tag) {
        // Read the towns ranks
        if (tag.containsKey("members", 9)) {
            for (Tag member : tag.getList("members", 10)) {
                super.USER_RANKS.put(
                    ((CompoundTag) member).getUuid("i"),
                    ClaimRanks.valueOf(((CompoundTag) member).getString("r"))
                );
            }
        }
        
        // Get the towns owner
        this.ownerId = (tag.hasUuid("owner") ? tag.getUuid("owner") : null);
        
        // Get the town name
        if (tag.containsKey("name",8))
            this.name = Text.Serializer.fromJson(tag.getString("name"));
        
        // Read from tag
        super.readCustomDataFromTag( tag );
    }
    
    // TODO: Town deleting / erasure
    public final void delete() {
        // Remove all players from the town
        
        
        // Remove from the cache (So it doesn't save again)
        CoreMod.removeFromCache( this );
    }
    
    @Nullable
    public static ClaimantTown get(UUID townId) {
        Claimant town;
        
        // If claims are disabled
        if ((!SewingMachineConfig.INSTANCE.DO_CLAIMS.get()) || (townId == null))
            return null;
        
        // If contained in the cache
        if ((town = CoreMod.getFromCache( ClaimantType.TOWN, townId )) != null)
            return (ClaimantTown) town;
        
        // Return the town object
        return new ClaimantTown( townId );
    }
    public static ClaimantTown makeTown(@NotNull ServerPlayerEntity founder, @NotNull Text townName) {
        // Generate a random UUID
        UUID townUUID;
        do {
            townUUID = UUID.randomUUID();
        } while (NbtUtils.exists( ClaimantType.TOWN, townUUID ));
        return ClaimantTown.makeTown( townUUID, founder.getUuid(), townName );
    }
    public static ClaimantTown makeTown(@NotNull UUID townUUID, @NotNull UUID founder, @NotNull Text townName) {
        // Create our town
        ClaimantTown town = new ClaimantTown( townUUID, townName );
    
        // Save the town
        town.setOwner( founder );
        town.save();
    
        // Return the town
        return town;
    }
    
}
