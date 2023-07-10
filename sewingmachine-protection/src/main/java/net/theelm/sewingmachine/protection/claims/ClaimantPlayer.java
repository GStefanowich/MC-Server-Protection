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

package net.theelm.sewingmachine.protection.claims;

import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.enums.ClaimRanks;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.theelm.sewingmachine.protection.objects.ServerClaimCache;
import net.theelm.sewingmachine.protection.objects.ClaimTag;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.mod.Sew;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.Chunk;
import net.theelm.sewingmachine.utilities.text.TextUtils;
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
    
    public ClaimantPlayer(@NotNull ClaimCache cache, @NotNull UUID playerUUID) {
        super(cache, ClaimantType.PLAYER, playerUUID);
    }
    
    public ClaimRanks getPermissionRankRequirement(@Nullable ClaimPermissions permission) {
        if (permission == null)
            return ClaimRanks.ENEMY;
        if (this.rankPermissions.containsKey(permission))
            return this.rankPermissions.get(permission);
        return permission.getDefault();
    }
    
    /* Players Town Reference */
    public @Nullable ClaimantTown getTown() {
        return this.town;
    }
    
    public @Nullable UUID getTownId() {
        ClaimantTown town;
        if ((town = this.getTown()) == null)
            return null;
        return town.getId();
    }
    public void setTown(@Nullable ClaimantTown town) {
        this.town = town;
        this.markDirty();
    }
    public boolean inviteTown(@NotNull ClaimantTown town) {
        if (this.town != null) return false;
        return this.townInvites.add(town);
    }
    public @Nullable ClaimantTown getTownInvite(@NotNull String townName) {
        ClaimantTown out = null;
        for (ClaimantTown town : this.townInvites) {
            if (townName.equals(town.getName().getString())) {
                out = town;
                break;
            }
        }
        if (out != null) this.townInvites.clear();
        return out;
    }
    public Set<ClaimantTown> getTownInvites() {
        return this.townInvites;
    }
    
    /* Player Friend Options */
    @Override
    public ClaimRanks getFriendRank(@Nullable UUID player) {
        if (this.getId().equals(player))
            return ClaimRanks.OWNER;
        return super.getFriendRank(player);
    }
    @Override
    public boolean updateFriend(@NotNull UUID player, @Nullable ClaimRanks rank) {
        return !player.equals(this.getId())
            && super.updateFriend(player, rank);
    }
    
    /* Nickname Override */
    @Override
    public @NotNull MutableText getName() {
        if (this.name == null)
            return FormattingUtils.deepCopy(this.name = this.updateName());
        return FormattingUtils.deepCopy(this.name);
    }
    public @NotNull MutableText updateName() {
        return TextUtils.mutable(PlayerNameCallback.getName(this.getId()));
    }
    
    /* Send Messages */
    @Override
    public void send(@NotNull MinecraftServer server, @NotNull final Text text) {
        ServerPlayerEntity player = this.getPlayer(server);
        if (player != null)
            player.sendMessage(text);
    }
    
    public @Nullable ServerPlayerEntity getPlayer(@NotNull MinecraftServer server) {
        return Sew.getPlayer(server, this.getId());
    }
    
    /* Claimed chunk options */
    public boolean getProtectedChunkSetting(@NotNull ClaimSettings setting) {
        if ( this.chunkClaimOptions.containsKey(setting) )
            return this.chunkClaimOptions.get(setting);
        return setting.getDefault(this.getId());
    }
    public int getMaxChunkLimit() {
        return this.additionalClaims + SewConfig.get(SewProtectionConfig.PLAYER_CLAIMS_LIMIT);
    }
    public int increaseMaxChunkLimit(int by) {
        this.markDirty();
        return (this.additionalClaims += by) + SewConfig.get(SewProtectionConfig.PLAYER_CLAIMS_LIMIT);
    }
    public boolean canClaim(Chunk chunk) {
        // If chunk is already claimed, allow
        if (this.claimedChunks.contains(ClaimTag.of(chunk)))
            return true;
        return (SewConfig.get(SewProtectionConfig.PLAYER_CLAIMS_LIMIT) != 0) && (((this.getCount() + 1) <= this.getMaxChunkLimit()) || (SewConfig.get(SewProtectionConfig.PLAYER_CLAIMS_LIMIT) <= 0));
    }
    
    /* Nbt saving */
    @Override
    public void writeCustomDataToTag(@NotNull NbtCompound tag) {
        // Write the town ID
        if (this.town != null)
            tag.putUuid("town", this.town.getId());
        
        // Write the additional claim limitation
        tag.putInt("claimLimit", this.additionalClaims);
        
        super.writeCustomDataToTag( tag );
    }
    @Override
    public void readCustomDataFromTag(@NotNull NbtCompound tag) {
        // Get the players town
        ClaimantTown town = null;
        if ( NbtUtils.hasUUID(tag, "town") && this.claimCache instanceof ServerClaimCache claimCache ) {
            town = claimCache.getTownClaim(NbtUtils.getUUID(tag, "town"));
            
            // Ensure that the town has the player in the ranks
            if ((town != null) && town.getFriendRank(this.getId()) == null)
                town = null;
        }
        this.town = town;
        
        // Additional claim limit
        this.additionalClaims = (tag.contains("claimLimit", NbtElement.INT_TYPE) ? tag.getInt("claimLimit") : 0);
        
        // Read from SUPER
        super.readCustomDataFromTag(tag);
    }
}
