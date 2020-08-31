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

import com.mojang.authlib.GameProfile;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.enums.ClaimRanks;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.objects.ClaimTag;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClaimantPlayer extends Claimant {
    
    private int additionalClaims;
    private final Set<ClaimantTown> townInvites = Collections.synchronizedSet(new HashSet<>());
    private ClaimantTown town;
    
    private ClaimantPlayer(@NotNull UUID playerUUID) {
        super( ClaimantType.PLAYER, playerUUID );
    }
    
    public final ClaimRanks getPermissionRankRequirement(@Nullable ClaimPermissions permission) {
        if (permission == null)
            return ClaimRanks.ENEMY;
        if (this.RANK_PERMISSIONS.containsKey(permission))
            return this.RANK_PERMISSIONS.get(permission);
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
        return super.getFriendRank( player );
    }
    
    /* Nickname Override */
    @Override
    public final MutableText getName() {
        if (this.name == null)
            return (this.name = this.updateName()).copy();
        return this.name.copy();
    }
    public final MutableText updateName() {
        return PlayerNameUtils.fetchPlayerNick( this.getId() );
    }
    
    /* Send Messages */
    @Override
    public final void send(Text text, MessageType type, UUID from) {
        UUID playerId = this.getId();
        ServerPlayerEntity player = ServerCore.getPlayer(playerId);
        if (player != null) player.sendMessage(text, type, from);
    }
    
    /* Claimed chunk options */
    public final boolean getProtectedChunkSetting(ClaimSettings setting) {
        if ( this.CHUNK_CLAIM_OPTIONS.containsKey( setting ) )
            return this.CHUNK_CLAIM_OPTIONS.get( setting );
        return setting.getDefault( this.getId() );
    }
    public final int getMaxChunkLimit() {
        return this.additionalClaims + SewConfig.get(SewConfig.PLAYER_CLAIMS_LIMIT);
    }
    public final int increaseMaxChunkLimit(int by) {
        this.markDirty();
        return (this.additionalClaims += by) + SewConfig.get(SewConfig.PLAYER_CLAIMS_LIMIT);
    }
    public final boolean canClaim(WorldChunk chunk) {
        // If chunk is already claimed, allow
        if (this.CLAIMED_CHUNKS.contains(new ClaimTag(chunk)))
            return true;
        return (SewConfig.get(SewConfig.PLAYER_CLAIMS_LIMIT) != 0) && (((this.getCount() + 1) <= this.getMaxChunkLimit()) || (SewConfig.get(SewConfig.PLAYER_CLAIMS_LIMIT) <= 0));
    }
    
    /* Nbt saving */
    @Override
    public final void writeCustomDataToTag(@NotNull CompoundTag tag) {
        // Write the town ID
        if (this.town != null)
            tag.putUuid("town", this.town.getId());
        
        // Write the additional claim limitation
        tag.putInt("claimLimit", this.additionalClaims);
        
        super.writeCustomDataToTag( tag );
    }
    @Override
    public final void readCustomDataFromTag(@NotNull CompoundTag tag) {
        // Get the players town
        ClaimantTown town = null;
        if ( tag.containsUuid("town") ) {
            try {
                town = ClaimantTown.get( tag.getUuid("town") );
                // Ensure that the town has the player in the ranks
                if ((town != null) && town.getFriendRank(this.getId()) == null) town = null;
            } catch (NbtNotFoundException ignored) {}
        }
        this.town = town;
        
        // Additional claim limit
        this.additionalClaims = ( tag.contains("claimLimit", NbtType.INT) ? tag.getInt("claimLimit") : 0 );
        
        // Read from SUPER
        super.readCustomDataFromTag( tag );
    }
    
    /* Get the PlayerPermissions object from the cache */
    public static @NotNull ClaimantPlayer get(@NotNull UUID playerUUID) {
        ClaimantPlayer player;
        
        // If contained in the cache
        if ((player = CoreMod.getFromCache( ClaimantPlayer.class, playerUUID )) != null)
            return player;
        
        // Create new object
        return new ClaimantPlayer( playerUUID );
    }
    public static @NotNull ClaimantPlayer get(@NotNull GameProfile profile) {
        return ClaimantPlayer.get(profile.getId());
    }
    
}
