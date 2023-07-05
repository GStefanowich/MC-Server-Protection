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

package net.theelm.sewingmachine.protection.utilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.events.RegionUpdateCallback;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.enums.ClaimRanks;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.packets.ClaimPermissionPacket;
import net.theelm.sewingmachine.protection.packets.ClaimRankPacket;
import net.theelm.sewingmachine.protection.packets.ClaimSettingPacket;
import net.theelm.sewingmachine.utilities.ModUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created on Jul 05 2023 at 4:05 AM.
 * By greg in sewingmachine
 */
public final class ClaimPropertyUtils {
    private ClaimPropertyUtils() {}
    
    public static void updateSetting(@NotNull ServerPlayerEntity player, @NotNull ClaimSettings setting, boolean enabled) {
        // Update the runtime
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        claim.updateSetting(setting, enabled);
        
        // Notify other players
        if (ClaimSettings.PLAYER_COMBAT.equals(setting)) {
            RegionUpdateCallback.EVENT.invoker()
                .update(player);
        }
        
        // Send a packet notifying the player of their change
        if (ModUtils.hasModule(player, "protection"))
            NetworkingUtils.send(player, new ClaimSettingPacket(setting, enabled));
    }
    public static void updatePermission(@NotNull ServerPlayerEntity player, @NotNull ClaimPermissions permission, @NotNull ClaimRanks rank) {
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        
        // Save the permission to the claim
        claim.updatePermission(permission, rank);
        
        // Send a packet notifying the player of their change
        if (ModUtils.hasModule(player, "protection"))
            NetworkingUtils.send(player, new ClaimPermissionPacket(permission, rank));
    }

    public static void updateRank(@NotNull ServerPlayerEntity player, @NotNull UUID friend, @NotNull ClaimRanks rank) {
        ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
        
        // Save the update to the claim
        if (!claim.updateFriend(friend, rank))
            return;
        
        // Send a packet notifying the player of their change
        if (ModUtils.hasModule(player, "protection")) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                Text display = PlayerNameCallback.getPlainName(server, friend)
                    .copyContentOnly();
                
                NetworkingUtils.send(player, new ClaimRankPacket(friend, display, rank));
            }
        }
    }
}
