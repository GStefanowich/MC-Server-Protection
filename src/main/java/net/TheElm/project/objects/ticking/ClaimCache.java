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

package net.TheElm.project.objects.ticking;

import com.mojang.authlib.GameProfile;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.objects.DetachedTickable;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created on Apr 14 2022 at 2:32 PM.
 * By greg in SewingMachineMod
 */
public final class ClaimCache implements Predicate<DetachedTickable> {
    // Reference from player UUID
    private final Map<UUID, ClaimantPlayer> playerClaimCache = Collections.synchronizedMap(new HashMap<>());
    // Reference from owner UUID
    private final Map<UUID, ClaimantTown> townClaimCache = Collections.synchronizedMap(new HashMap<>());
    
    private final MinecraftServer server;
    
    public ClaimCache(MinecraftServer server) {
        this.server = server;
    }
    
    /*
     * Cache
     */
    
    public void addToCache(@Nullable Claimant claimant) {
        if (claimant instanceof ClaimantPlayer claimantPlayer)
            this.playerClaimCache.put(claimant.getId(), claimantPlayer);
        else if (claimant instanceof ClaimantTown claimantTown)
            this.townClaimCache.put(claimant.getId(), claimantTown);
    }
    @Nullable
    public Claimant removeFromCache(@Nullable Claimant claimant) {
        if (claimant instanceof ClaimantPlayer)
            return this.playerClaimCache.remove(claimant.getId());
        if (claimant instanceof ClaimantTown)
            return this.townClaimCache.remove(claimant.getId());
        return null;
    }
    @Nullable
    public <T extends Claimant> T getFromCache(@NotNull Class<T> type, @NotNull UUID uuid) {
        return this.getCacheStream( type ).filter((claimant) -> claimant.getId().equals(uuid)).findFirst().orElse(null);
    }
    @Nullable
    public <T extends Claimant> T getFromCache(@NotNull Class<T> type, @NotNull String name) {
        return this.getCacheStream( type ).filter((claimant) -> name.equals(claimant.getName().getString())).findFirst().orElse(null);
    }
    public Stream<Claimant> getCacheStream() {
        return this.getCacheStream(null);
    }
    public <T extends Claimant> Stream<T> getCacheStream(@Nullable Class<T> type) {
        List<T> out = new ArrayList<>();
        if ((type == null) || type.equals(ClaimantPlayer.class)) {
            for (ClaimantPlayer player : this.playerClaimCache.values())
                out.add((T) player);
        }
        if ((type == null) || type.equals(ClaimantTown.class)) {
            for ( ClaimantTown town : this.townClaimCache.values() )
                out.add((T) town);
        }
        return out.stream();
    }
    
    /*
     * Players
     */
    
    // Get the PlayerPermissions object from the cache
    public @NotNull ClaimantPlayer getPlayerClaim(@NotNull UUID playerUUID) {
        ClaimantPlayer player;
        
        // If contained in the cache
        if ((player = this.getFromCache(ClaimantPlayer.class, playerUUID)) != null)
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
        if ((!SewConfig.get(SewConfig.DO_CLAIMS)) || (townId == null))
            return null;
        
        try {
            NbtUtils.assertExists(Claimant.ClaimantType.TOWN, townId);
            
            // If contained in the cache
            if ((town = this.getFromCache(ClaimantTown.class, townId)) != null)
                return town;
            
            // Return the town object
            return new ClaimantTown(this, townId);
        } catch (NbtNotFoundException e) {
            return null;
        }
    }
    public @NotNull ClaimantTown makeTownClaim(@NotNull ServerPlayerEntity founder, @NotNull MutableText townName) {
        // Generate a random UUID
        UUID townUUID;
        do {
            townUUID = UUID.randomUUID();
        } while (NbtUtils.exists(Claimant.ClaimantType.TOWN, townUUID));
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
    public boolean test(@NotNull DetachedTickable detachedTickable) {
        return false;
    }
}
