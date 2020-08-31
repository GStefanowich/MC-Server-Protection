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
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.utilities.NbtUtils;
import net.TheElm.project.utilities.TownNameUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClaimantTown extends Claimant {
    
    private boolean deleted = false;
    private UUID ownerId;
    private Set<UUID> villagers;
    
    protected ClaimantTown(@NotNull UUID townId) {
        super(ClaimantType.TOWN, townId);
    }
    protected ClaimantTown(@NotNull UUID townId, @NotNull MutableText townName) {
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
        return this.getFriends().size()
            + (SewConfig.get(SewConfig.TOWN_VILLAGERS_INCLUDE)
                && SewConfig.get(SewConfig.TOWN_VILLAGERS_VALUE) > 0 ? this.getVillagers().size() / SewConfig.get(SewConfig.TOWN_VILLAGERS_VALUE) : 0);
    }
    
    @Override
    public MutableText getName() {
        return this.name.copy();
    }
    
    /* Villager Options */
    public final Set<UUID> getVillagers() {
        return this.villagers;
    }
    public final boolean addVillager(VillagerEntity villager) {
        if (this.villagers.add(villager.getUuid())) {
            CoreMod.logInfo("Added a new villager to " + this.getName().getString() + "!");
            this.markDirty();
            return true;
        }
        return false;
    }
    public final boolean removeVillager(VillagerEntity villager) {
        if (this.villagers.remove(villager.getUuid())) {
            this.markDirty();
            return true;
        }
        return false;
    }
    
    /* Player Friend Options */
    @Override
    public final ClaimRanks getFriendRank(@Nullable UUID player) {
        if ( this.getOwner().equals( player ) )
            return ClaimRanks.OWNER;
        return super.getFriendRank( player );
    }
    @Override
    public final boolean updateFriend(@NotNull final UUID player, @Nullable final ClaimRanks rank) {
        if ( super.updateFriend( player, rank ) ) {
            ClaimantPlayer claim = ClaimantPlayer.get( player );
            claim.setTown(rank == null ? null : this);
            return true;
        }
        return false;
    }
    @Override
    public boolean updateFriend(@NotNull ServerPlayerEntity player, @Nullable ClaimRanks rank) {
        if ( super.updateFriend( player, rank ) ) {
            ClaimantPlayer claim = ((PlayerData) player).getClaim();
            claim.setTown(rank == null ? null : this);
            return true;
        }
        return false;
    }
    
    /* Send Messages */
    @Override
    public final void send(Text text, MessageType type, UUID from) {
        this.getFriends().forEach((uuid) -> {
            ServerPlayerEntity player = ServerCore.getPlayer(uuid);
            if (player != null) player.sendMessage(text, type, from);
        });
    }
    
    /* Nbt saving */
    @Override
    public final void writeCustomDataToTag(@NotNull CompoundTag tag) {
        if (this.ownerId == null) throw new RuntimeException("Town owner should not be null");
        
        tag.putUuid("owner", this.ownerId);
        
        if ( this.name != null )
            tag.putString("name", Text.Serializer.toJson( this.name ));
        
        tag.put("villagers", NbtUtils.toList(this.getVillagers(), UUID::toString));
        
        // Write to tag
        super.writeCustomDataToTag( tag );
    }
    @Override
    public final void readCustomDataFromTag(@NotNull CompoundTag tag) {
        // Get the towns owner
        this.ownerId = (tag.containsUuid("owner") ? tag.getUuid("owner") : null);
        
        // Get the town name
        if (tag.contains("name", NbtType.STRING))
            this.name = Text.Serializer.fromJson(tag.getString("name"));
        
        // Get the towns villagers
        this.villagers = new HashSet<>();
        if (tag.contains("villagers", NbtType.LIST))
            this.villagers.addAll(NbtUtils.fromList(tag.getList("villagers", NbtType.STRING), UUID::fromString));
        
        // Read from tag
        super.readCustomDataFromTag( tag );
    }
    
    public final void delete() {
        PlayerManager playerManager = ServerCore.get().getPlayerManager();
        ServerPlayerEntity player;
        
        this.deleted = true;
        
        // Remove all players from the town
        for (UUID member : this.getFriends()) {
            if ((player = playerManager.getPlayer(member)) != null)
                ((PlayerData)player).getClaim().setTown( null );
        }
        
        // Remove from the cache (So it doesn't save again)
        CoreMod.removeFromCache( this );
        NbtUtils.delete( this );
        CoreMod.logInfo("Deleted town " + this.getName().asString() + " (" + this.getId() + ")");
    }
    @Override
    public boolean forceSave() {
        if (!this.deleted)
            return super.forceSave();
        return true;
    }
    
    @Nullable
    public static ClaimantTown get(UUID townId) throws NbtNotFoundException {
        ClaimantTown town;
        
        // If claims are disabled
        if ((!SewConfig.get(SewConfig.DO_CLAIMS)) || (townId == null))
            return null;
        
        NbtUtils.assertExists( ClaimantType.TOWN, townId );
        
        // If contained in the cache
        if ((town = CoreMod.getFromCache( ClaimantTown.class, townId )) != null)
            return town;
        
        // Return the town object
        return new ClaimantTown( townId );
    }
    public static ClaimantTown makeTown(@NotNull ServerPlayerEntity founder, @NotNull MutableText townName) {
        // Generate a random UUID
        UUID townUUID;
        do {
            townUUID = UUID.randomUUID();
        } while (NbtUtils.exists( ClaimantType.TOWN, townUUID ));
        return ClaimantTown.makeTown( townUUID, founder.getUuid(), townName );
    }
    public static ClaimantTown makeTown(@NotNull UUID townUUID, @NotNull UUID founder, @NotNull MutableText townName) {
        // Create our town
        ClaimantTown town = new ClaimantTown(townUUID, townName);
    
        // Save the town
        town.setOwner( founder );
        town.save();
    
        // Return the town
        return town;
    }
    
}
