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

import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.interfaces.WhitelistedPlayer;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.theelm.sewingmachine.protection.objects.PlayerVisitor;
import net.theelm.sewingmachine.protection.enums.ClaimRanks;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.protection.objects.ServerClaimCache;
import net.theelm.sewingmachine.protection.objects.ClaimCacheEntry;
import net.theelm.sewingmachine.protection.objects.ClaimTag;
import net.theelm.sewingmachine.protection.utilities.ClaimNbtUtils;
import net.theelm.sewingmachine.utilities.DevUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class Claimant {
    protected final ClaimCache claimCache;
    protected final Map<UUID, ClaimRanks> userRanks = Collections.synchronizedMap(new HashMap<>());
    protected final Map<ClaimSettings, Boolean> chunkClaimOptions = Collections.synchronizedMap(new HashMap<>());
    protected final Map<ClaimPermissions, ClaimRanks> rankPermissions = Collections.synchronizedMap(new HashMap<>());
    protected final Set<ClaimTag> claimedChunks = Collections.synchronizedSet(new LinkedHashSet<>());
    
    private final @NotNull ClaimantType type;
    private final @NotNull UUID id;
    
    private final @NotNull Set<PlayerVisitor> visitors = Collections.synchronizedSet(new LinkedHashSet<>());
    
    private final @Nullable ClaimCacheEntry<?> saveHandle;
    
    protected MutableText name = null;
    
    protected Claimant(@NotNull ClaimCache cache, @NotNull ClaimantType type, @NotNull UUID uuid) {
        this.claimCache = cache;
        this.type = type;
        this.id = uuid;
        
        // Save to the cache BEFORE loading (For synchronocity!)
        this.saveHandle = this.claimCache.addToCache(this);
        
        // Load all information about the claim
        if (cache instanceof ServerClaimCache)
            this.readCustomDataFromTag(ClaimNbtUtils.readClaimData(this.type, this.id));
    }
    
    /* Player Friend Options */
    public ClaimRanks getFriendRank(@Nullable UUID player) {
        if (player == null) return ClaimRanks.ENEMY;
        return this.userRanks.getOrDefault( player, ClaimRanks.PASSIVE );
    }
    public boolean isFriend(@Nullable UUID player) {
        if (player == null) return false;
        switch (this.getFriendRank(player)) {
            case OWNER:
            case ALLY:
                return true;
            default:
                return false;
        }
    }
    public boolean isFriend(@Nullable Entity entity) {
        return entity instanceof PlayerEntity player && this.isFriend(player.getUuid());
    }
    public boolean isFriend(@Nullable WhitelistedPlayer player) {
        return player != null && this.isFriend(player.getUUID());
    }
    public boolean updateFriend(@NotNull UUID player, @Nullable ClaimRanks rank) {
        boolean changed = false;
        if (rank == null) {
            changed = (this.userRanks.remove(player) != null);
        } else {
            if (((!this.userRanks.containsKey(player)) || (!this.userRanks.get(player).equals(rank)))) {
                this.userRanks.put(player, rank);
                changed = true;
            }
        }
        if (changed) this.markDirty();
        return changed;
    }
    public boolean updateFriend(@NotNull ServerPlayerEntity player, @Nullable ClaimRanks rank) {
        return this.updateFriend(player.getUuid(), rank);
    }
    public final Set<UUID> getFriends() {
        return this.userRanks.keySet();
    }
    
    /* Owner Options */
    public final void updateSetting(ClaimSettings setting, Boolean bool) {
        this.chunkClaimOptions.put(setting, bool);
        this.markDirty();
    }
    public final void updatePermission(ClaimPermissions permission, ClaimRanks rank) {
        this.rankPermissions.put(permission, rank);
        this.markDirty();
    }
    
    /* Get the latest name */
    public final UUID getId() {
        return this.id;
    }
    public abstract MutableText getName();
    public final @NotNull MutableText getName(@NotNull PlayerEntity player) {
        return this.getName(player.getUuid());
    }
    public final @NotNull MutableText getName(@Nullable UUID player) {
        ClaimRanks playerRank = this.getFriendRank(player);
        return Text.literal(this.getName().getString()).formatted(playerRank.getColor());
    }
    
    /* Send Messages */
    public abstract void send(@NotNull final MinecraftServer server, @NotNull final Text text);
    
    public final void visitors(@NotNull Consumer<ServerPlayerEntity> consumer) {
        for (PlayerVisitor visitor : this.visitors)
            visitor.player(consumer);
    }
    public final boolean addVisitor(@NotNull PlayerVisitor visitor) {
        visitor.addLocation(this);
        return this.visitors.add(visitor);
    }
    public final boolean removeVisitor(@NotNull PlayerVisitor visitor) {
        return this.visitors.remove(visitor);
    }
    
    /* Town types */
    public final ClaimantType getType() {
        return this.type;
    }
    
    public final void addToCount(@NotNull WorldChunk... chunks) {
        for (WorldChunk chunk : chunks)
            this.claimedChunks.add(ClaimTag.of(chunk));
        this.markDirty();
    }
    public final void removeFromCount(@NotNull WorldChunk... chunks) {
        for (WorldChunk chunk : chunks) {
            ChunkPos pos = chunk.getPos();
            if (this.claimedChunks.removeIf((iteration) -> (
                Objects.equals(
                    iteration.getDimension(),
                    chunk.getWorld().getRegistryKey()
                )
                && iteration.getX() == pos.x
                && iteration.getZ() == pos.z
            ))) this.markDirty();
        }
    }
    
    public final int getCount() {
        return this.claimedChunks.size();
    }
    public final void forEachChunk(Consumer<ClaimTag> action) {
        for (ClaimTag it : new LinkedHashSet<>(this.claimedChunks))
            action.accept(it);
    }
    public final @NotNull Collection<ClaimTag> getChunks() {
        return this.claimedChunks;
    }
    
    public boolean isSpawn() {
        return Objects.equals(this.getId(), CoreMod.SPAWN_ID);
    }
    
    /* Nbt saving */
    @Override
    public final void finalize() {
        // Save before trashing
        this.save();
    }
    public final void markDirty() {
        if (this.saveHandle != null)
            this.saveHandle.markDirty();
    }
    public final void save() {
        if (this.saveHandle != null && this.saveHandle.isDirty()) {
            this.forceSave();
            this.saveHandle.markClean();
        }
    }
    public boolean forceSave() {
        if (DevUtils.isDebugging()) CoreMod.logInfo("Saving " + this.getType().name().toLowerCase() + " data for " + (this.isSpawn() ? "Spawn" : this.getId()) + "." );
        boolean success = ClaimNbtUtils.writeClaimData(this);
        if (!success) CoreMod.logInfo("FAILED TO SAVE " + this.getType().name() + " DATA, " + (this.isSpawn() ? "Spawn" : this.getId()) + ".");
        return success;
    }
    public void writeCustomDataToTag(@NotNull NbtCompound tag) {
        // Save our chunks
        NbtList chunkList = new NbtList();
        chunkList.addAll(this.claimedChunks);
        tag.put("landChunks", chunkList);
        
        // Save our list of friends
        NbtList rankList = new NbtList();
        for (Map.Entry<UUID, ClaimRanks> friend : this.userRanks.entrySet()) {
            // Make a map for the ranked player
            NbtCompound friendTag = new NbtCompound();
            friendTag.putUuid("i", friend.getKey());
            friendTag.putString("r", friend.getValue().name());
            
            // Save the ranked player to our data tag
            rankList.add(friendTag);
        }
        tag.put(rankNbtTag(this), rankList);
        
        // Save our list of permissions
        NbtList permList = new NbtList();
        for (Map.Entry<ClaimPermissions, ClaimRanks> permission : this.rankPermissions.entrySet()) {
            // Make a map for the permission
            NbtCompound permTag = new NbtCompound();
            permTag.putString("k", permission.getKey().name());
            permTag.putString("v", permission.getValue().name());
            
            // Save the permission to our data tag
            permList.add( permTag );
        }
        tag.put("permissions", permList);
        
        // Save our list of settings
        NbtList settingList = new NbtList();
        for (Map.Entry<ClaimSettings, Boolean> setting : this.chunkClaimOptions.entrySet()) {
            // Make a map for the setting
            NbtCompound settingTag = new NbtCompound();
            settingTag.putString("k", setting.getKey().name());
            settingTag.putBoolean("b", setting.getValue());
            
            // Save the setting to our data tag
            settingList.add( settingTag );
        }
        tag.put("settings", settingList);
    }
    public void readCustomDataFromTag(@NotNull NbtCompound tag) {
        if (!(tag.getString("type").equals(this.type.name()) && Objects.equals(NbtUtils.getUUID(tag, "iden"), this.id)))
            throw new RuntimeException("Invalid NBT data match");
        
        // Get the claim size
        if (tag.contains("landChunks", NbtElement.LIST_TYPE)) {
            ClaimTag claim;
            
            // Get from Int Array
            for (NbtElement it : tag.getList("landChunks",NbtElement.INT_ARRAY_TYPE)) {
                claim = ClaimTag.fromArray((NbtIntArray) it);
                if (claim != null) this.claimedChunks.add(claim);
            }
            // Get from Compound
            for (NbtElement it : tag.getList("landChunks", NbtElement.COMPOUND_TYPE)) {
                claim = ClaimTag.fromCompound((NbtCompound) it);
                if (claim != null) this.claimedChunks.add(claim);
            }
        }
        
        // Read friends
        if (tag.contains(rankNbtTag(this), NbtElement.LIST_TYPE)) {
            for (NbtElement it : tag.getList(rankNbtTag(this), NbtElement.COMPOUND_TYPE)) {
                NbtCompound friend = (NbtCompound) it;
                this.userRanks.put(
                    NbtUtils.getUUID(friend, "i"),
                    ClaimRanks.valueOf(friend.getString("r"))
                );
            }
        }
        
        // Read permissions
        if (tag.contains("permissions", NbtElement.LIST_TYPE)) {
            for (NbtElement it : tag.getList("permissions", NbtElement.COMPOUND_TYPE)) {
                NbtCompound permission = (NbtCompound) it;
                this.rankPermissions.put(
                    ClaimPermissions.valueOf(permission.getString("k")),
                    ClaimRanks.valueOf(permission.getString("v"))
                );
            }
        }
        
        // Read settings
        if (tag.contains("settings", NbtElement.LIST_TYPE)) {
            for (NbtElement it : tag.getList("settings", NbtElement.COMPOUND_TYPE)) {
                NbtCompound setting = (NbtCompound) it;
                this.chunkClaimOptions.put(
                    ClaimSettings.valueOf(setting.getString("k")),
                    setting.getBoolean("b")
                );
            }
        }
        
        // TODO: Use the zone and write to bluemap
        /*new ChunkZone.Builder(this)
            .add(this.CLAIMED_CHUNKS)
            .build();*/
    }
    
    private static @NotNull String rankNbtTag(@Nullable Claimant claimant) {
        return (claimant instanceof ClaimantTown ? "members" : "friends");
    }
    public enum ClaimantType {
        TOWN,
        PLAYER
    }
}
