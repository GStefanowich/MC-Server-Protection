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

package net.theelm.sewingmachine.protection.objects;

import com.mojang.authlib.GameProfile;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.interfaces.TickableContext;
import net.theelm.sewingmachine.interfaces.TickingAction;
import net.theelm.sewingmachine.protection.claims.Claimant;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.protection.utilities.ClaimNbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Created on Apr 14 2022 at 2:32 PM.
 * By greg in SewingMachineMod
 */
public final class ClaimCache implements TickingAction {
    // Reference from player UUID
    private final Map<UUID, PlayerCacheEntry> playerClaimCache = Collections.synchronizedMap(new HashMap<>());
    // Reference from owner UUID
    private final Map<UUID, TownCacheEntry> townClaimCache = Collections.synchronizedMap(new HashMap<>());
    
    private final MinecraftServer server;
    
    private int index = 1;
    private List<UUID> players = Collections.emptyList();
    private List<UUID> towns = Collections.emptyList();
    
    public ClaimCache(@NotNull MinecraftServer server, @NotNull ServerWorld mainWorld) {
        this.server = server;
        
        // Make sure that the cleanup event is ticked by attaching to the main world
        ((LogicalWorld)mainWorld).addTickableEvent(this);
    }
    
    /*
     * Cache
     */
    
    public @Nullable ClaimCacheEntry<?> addToCache(@Nullable Claimant claimant) {
        if (claimant instanceof ClaimantPlayer claimantPlayer)
            return this.playerClaimCache.computeIfAbsent(claimant.getId(), id -> new PlayerCacheEntry(claimantPlayer));
        
        if (claimant instanceof ClaimantTown claimantTown)
            return this.townClaimCache.computeIfAbsent(claimant.getId(), id -> new TownCacheEntry(claimantTown));
        
        return null;
    }
    public @Nullable Claimant removeFromCache(@Nullable Claimant claimant) {
        ClaimCacheEntry<?> entry;
        if (claimant instanceof ClaimantPlayer)
            entry = this.playerClaimCache.remove(claimant.getId());
        else if (claimant instanceof ClaimantTown)
            entry = this.townClaimCache.remove(claimant.getId());
        else return null;
        
        return entry == null ? null : entry.getValue();
    }
    private <T extends Claimant> @Nullable T getFromCache(@NotNull Map<UUID, ? extends ClaimCacheEntry<T>> entries, @NotNull UUID uuid) {
        ClaimCacheEntry<T> entry = entries.get(uuid);
        return entry == null ? null : entry.getValue();
    }
    public @NotNull Stream<ClaimantPlayer> getPlayerCaches() {
        return this.playerClaimCache.values().stream().map(ClaimCacheEntry::getValue)
            .filter(Objects::nonNull);
    }
    public @NotNull Stream<ClaimantTown> getTownCaches() {
        return this.townClaimCache.values().stream().map(ClaimCacheEntry::getValue)
            .filter(Objects::nonNull);
    }
    public @NotNull Stream<Claimant> getCaches() {
        return Stream.concat(
            this.getPlayerCaches().map(player -> (Claimant) player),
            this.getTownCaches().map(town -> (Claimant) town)
        );
    }
    
    public MinecraftServer getServer() {
        return this.server;
    }
    
    /*
     * Players
     */
    
    // Get the PlayerPermissions object from the cache
    public @NotNull ClaimantPlayer getPlayerClaim(@NotNull UUID playerUUID) {
        ClaimantPlayer player;
        
        // If contained in the cache
        if ((player = this.getFromCache(this.playerClaimCache, playerUUID)) != null)
            return player;
        
        // Create new object
        return new ClaimantPlayer(this, playerUUID);
    }
    public @NotNull ClaimantPlayer getPlayerClaim(@NotNull GameProfile profile) {
        return this.getPlayerClaim(profile.getId());
    }
    public @NotNull ClaimantPlayer getPlayerClaim(@NotNull ServerPlayerEntity player) {
        return this.getPlayerClaim(player.getUuid());
    }
    
    /*
     * Towns
     */
    public @Nullable ClaimantTown getTownClaim(UUID townId) {
        ClaimantTown town;
        
        // If claims are disabled
        if (townId == null)
            return null;
        
        try {
            ClaimNbtUtils.assertExists(Claimant.ClaimantType.TOWN, townId);
            
            // If contained in the cache
            if ((town = this.getFromCache(this.townClaimCache, townId)) != null)
                return town;
            
            // Return the town object
            return new ClaimantTown(this, townId);
        } catch (NbtNotFoundException e) {
            return null;
        }
    }
    public @Nullable ClaimantTown getTownClaim(String name) {
        return this.townClaimCache.values()
            .stream()
            .map(ClaimCacheEntry::getValue)
            .filter(town -> town != null && Objects.equals(name, town.getName().getString()))
            .findAny()
            .orElse(null);
    }
    public @NotNull ClaimantTown makeTownClaim(@NotNull ServerPlayerEntity founder, @NotNull MutableText townName) {
        // Generate a random UUID
        UUID townUUID;
        do {
            townUUID = UUID.randomUUID();
        } while (ClaimNbtUtils.exists(Claimant.ClaimantType.TOWN, townUUID));
        return this.makeTownClaim(townUUID, founder.getUuid(), townName);
    }
    public @NotNull ClaimantTown makeTownClaim(@NotNull UUID townUUID, @NotNull UUID founder, @NotNull MutableText townName) {
        // Create our town
        ClaimantTown town = new ClaimantTown(this, townUUID, townName);
        
        // Save the town
        town.setOwner(founder);
        town.save();
        
        // Return the town
        return town;
    }
    
    /*
     * Handle ticking
     */
    
    @Override
    public boolean isCompleted(@NotNull TickableContext tickable) {
        if (tickable.getTicks() % 100 == 0)
            this.tick();
        
        // Only return TRUE if the Tickable is disposed
        return tickable.isRemoved();
    }
    
    public void tick() {
        int players = this.playerClaimCache.size();
        int towns = this.townClaimCache.size();
        int sum = players + towns;
        
        if (this.players.size() != players || this.towns.size() != towns)
            this.reset(this.players.size() != players, this.towns.size() != towns);
        else if (sum > this.index)
            this.index++;
        else this.index = 1;
        
        if (sum == 0)
            return;
        
        boolean onTowns = this.index > players;
        int pos;
        List<UUID> list;
        Map<UUID, ? extends ClaimCacheEntry<?>> reference;
        
        if (onTowns) {
            reference = this.townClaimCache;
            list = this.towns;
            pos = this.index - players - 1;
        } else {
            reference = this.playerClaimCache;
            list = this.players;
            pos = this.index - 1;
        }
        
        UUID uuid = list.get(pos);
        if (uuid == null)
            return;
        
        ClaimCacheEntry<?> claim = reference.get(uuid);
        if (claim == null)
            return;
        
        if (claim.isRemovable()) {
            reference.remove(uuid);
            this.reset(!onTowns, onTowns);
        }
    }
    
    private void reset() {
        this.reset(true, true);
    }
    private void reset(boolean players, boolean towns) {
        if (players)
            this.players = new ArrayList<>(this.playerClaimCache.keySet());
        if (towns)
            this.towns = new ArrayList<>(this.townClaimCache.keySet());
        
        this.index = MathHelper.clamp(this.index, 1, this.players.size() + this.towns.size());
    }
    
    private class PlayerCacheEntry extends ClaimCacheEntry<ClaimantPlayer> {
        public PlayerCacheEntry(ClaimantPlayer value) {
            super(value);
        }
    }
    private class TownCacheEntry extends ClaimCacheEntry<ClaimantTown> {
        public TownCacheEntry(ClaimantTown value) {
            super(value);
        }
    }
}
