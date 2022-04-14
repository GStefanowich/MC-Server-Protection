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
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.objects.ticking.ClaimCache;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.TheElm.project.utilities.TownNameUtils;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClaimantTown extends Claimant {
    
    private boolean deleted = false;
    private UUID ownerId;
    private Set<UUID> villagers;
    
    public ClaimantTown(@NotNull ClaimCache cache, @NotNull UUID townId) {
        super(cache, ClaimantType.TOWN, townId);
    }
    public ClaimantTown(@NotNull ClaimCache cache, @NotNull UUID townId, @NotNull MutableText townName) {
        this(cache, townId);
        this.name = townName;
    }
    
    public @NotNull String getTownType() {
        return TownNameUtils.getTownName( this.getCount(), this.getResidentCount() );
    }
    public @NotNull String getOwnerTitle() {
        return TownNameUtils.getOwnerTitle( this.getCount(), this.getResidentCount(), true );
    }
    public @NotNull MutableText getOwnerName() {
        return PlayerNameUtils.fetchPlayerNick(this.getOwner());
    }
    
    public UUID getOwner() {
        return this.ownerId;
    }
    public void setOwner(@NotNull UUID owner) {
        this.updateFriend( owner, ClaimRanks.OWNER );
        this.ownerId = owner;
        this.markDirty();
    }
    
    public int getResidentCount() {
        return this.getFriends().size()
            + (SewConfig.get(SewConfig.TOWN_VILLAGERS_INCLUDE)
                && SewConfig.get(SewConfig.TOWN_VILLAGERS_VALUE) > 0 ? this.getVillagers().size() / SewConfig.get(SewConfig.TOWN_VILLAGERS_VALUE) : 0);
    }
    
    @Override
    public @NotNull MutableText getName() {
        return FormattingUtils.deepCopy(this.name);
    }
    public @NotNull HoverEvent getHoverText() {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Town: ")
            .append(this.getName()
                .formatted(Formatting.DARK_AQUA, Formatting.BOLD))
            .append("\nOwner: ")
            .append(this.getOwnerName()
                .formatted(Formatting.DARK_AQUA, Formatting.BOLD)));
    }
    
    /* Villager Options */
    public Set<UUID> getVillagers() {
        return this.villagers;
    }
    public boolean addVillager(VillagerEntity villager) {
        if (this.villagers.add(villager.getUuid())) {
            CoreMod.logInfo("Added a new villager to " + this.getName().getString() + "!");
            this.markDirty();
            return true;
        }
        return false;
    }
    public boolean removeVillager(VillagerEntity villager) {
        if (this.villagers.remove(villager.getUuid())) {
            this.markDirty();
            return true;
        }
        return false;
    }
    
    /* Player Friend Options */
    @Override
    public ClaimRanks getFriendRank(@Nullable UUID player) {
        if ( this.getOwner().equals( player ) )
            return ClaimRanks.OWNER;
        return super.getFriendRank( player );
    }
    @Override
    public boolean updateFriend(@NotNull final UUID player, @Nullable final ClaimRanks rank) {
        if ( super.updateFriend( player, rank ) ) {
            ClaimantPlayer claim = this.claimCache.getPlayerClaim(player);
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
    public void send(@NotNull MinecraftServer server, @NotNull final Text text, @NotNull final MessageType type, @NotNull final UUID from) {
        this.getFriends().forEach((uuid) -> {
            ServerPlayerEntity player = ServerCore.getPlayer(server, uuid);
            if (player != null)
                player.sendMessage(text, type, from);
        });
    }
    
    /* Tax Options */
    public int getTaxRate() {
        return 0;
    }
    
    /* Nbt saving */
    @Override
    public void writeCustomDataToTag(@NotNull NbtCompound tag) {
        if (this.ownerId == null) throw new RuntimeException("Town owner should not be null");
        
        tag.putUuid("owner", this.ownerId);
        
        if ( this.name != null )
            tag.putString("name", Text.Serializer.toJson( this.name ));
        
        tag.put("villagers", NbtUtils.toList(this.getVillagers(), UUID::toString));
        
        // Write to tag
        super.writeCustomDataToTag( tag );
    }
    @Override
    public void readCustomDataFromTag(@NotNull NbtCompound tag) {
        // Get the towns owner
        this.ownerId = (NbtUtils.hasUUID(tag, "owner") ? NbtUtils.getUUID(tag, "owner") : null);
        
        // Get the town name
        if (tag.contains("name", NbtElement.STRING_TYPE))
            this.name = Text.Serializer.fromJson(tag.getString("name"));
        
        // Get the towns villagers
        this.villagers = new HashSet<>();
        if (tag.contains("villagers", NbtElement.LIST_TYPE))
            this.villagers.addAll(NbtUtils.fromList(tag.getList("villagers", NbtElement.STRING_TYPE), UUID::fromString));
        
        // Read from tag
        super.readCustomDataFromTag( tag );
    }
    
    public void delete(@NotNull PlayerManager playerManager) {
        ServerPlayerEntity player;
        
        this.deleted = true;
        
        // Remove all players from the town
        for (UUID member : this.getFriends()) {
            if ((player = playerManager.getPlayer(member)) != null)
                ((PlayerData)player).getClaim().setTown(null);
        }
        
        // Remove from the cache (So it doesn't save again)
        this.claimCache.removeFromCache(this);
        
        NbtUtils.delete(this);
        CoreMod.logInfo("Deleted town " + this.getName().getString() + " (" + this.getId() + ")");
    }
    @Override
    public boolean forceSave() {
        if (!this.deleted)
            return super.forceSave();
        return true;
    }
}
