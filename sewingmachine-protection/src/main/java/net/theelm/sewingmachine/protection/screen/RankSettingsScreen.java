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

package net.theelm.sewingmachine.protection.screen;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.enums.ClaimRanks;
import net.theelm.sewingmachine.interfaces.NameCache;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.packets.ClaimRankPacket;
import net.theelm.sewingmachine.screens.SettingScreen;
import net.theelm.sewingmachine.screens.SettingScreenListWidget;
import net.theelm.sewingmachine.utilities.ArrayUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import net.theelm.sewingmachine.utilities.TradeUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class RankSettingsScreen extends SettingScreen {
    protected RankSettingsScreen(@NotNull Text title) {
        super(title);
    }
    
    @Override
    protected void addButtons(@NotNull SettingScreenListWidget list) {
        ClaimantPlayer claim = ((PlayerClaimData) this.client).getClaim();
        Map<ClaimRanks, Set<GameProfile>> ranked = this.getRankedPlayers(claim);
        Map<ClaimRanks, Text> tooltips = this.getTooltips(claim);
        
        for (ClaimRanks rank : ClaimRanks.values()) {
            Set<GameProfile> profiles = ranked.get(rank);
            if (profiles == null)
                continue;
            
            for (GameProfile profile : profiles) {
                // Skip over the self player (Can't change own rank)
                UUID self = this.client.getSession()
                    .getUuidOrNull();
                UUID player = profile.getId();
                
                // Always skip over yourself, as well as the Wandering Trader (MHF_Villager)
                if ((self != null && player.equals(self)) || player.equals(TradeUtils.WANDERING_TRADER))
                    continue;
                
                list.addCycleButton(
                    ClaimRanks.class,
                    Text.literal(profile.getName()),
                    this.tooltip(tooltips, rank),
                    claim.getFriendRank(player),
                    (button, state) -> {
                        ClaimRanks current = state.get();
                        
                        // Update the button tooltip
                        button.setTooltip(this.tooltip(tooltips, current));
                        
                        NetworkingUtils.send(this.client, new ClaimRankPacket(player, current));
                    }
                );
            }
        }
    }
    
    private @NotNull Tooltip tooltip(@NotNull Map<ClaimRanks, Text> map, @NotNull ClaimRanks rank) {
        MutableText base = Text.literal("Access:");
        
        Text text = map.get(rank);
        if (text != null)
            base.append(text);
        else {
            base.append(Text.literal("\n None")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        
        return Tooltip.of(base);
    }
    
    private @NotNull Map<ClaimRanks, Text> getTooltips(@NotNull ClaimantPlayer claim) {
        Map<ClaimRanks, Text> map = new HashMap<>();
        Map<ClaimRanks, EnumSet<ClaimPermissions>> permissions = new HashMap<>();
        
        for (ClaimRanks rank : ClaimRanks.values()) {
            EnumSet<ClaimPermissions> set = EnumSet.noneOf(ClaimPermissions.class);
            
            for (ClaimPermissions permission : ClaimPermissions.values()) {
                ClaimRanks required = claim.getPermissionRankRequirement(permission);
                if (rank == required)
                    set.add(permission);
                
                /*if (required.canPerform(rank)) {
                    list.append("\n ")
                        .append(Text.translatable(permission.getTranslationKey()));
                }*/
            }
            
            /*if (count == 0)
                list.append(Text.literal("\n None")
                    .formatted(Formatting.GRAY, Formatting.ITALIC));*/
            
            permissions.put(rank, set);
        }
        
        for (ClaimRanks other : ClaimRanks.values()) {
            MutableText text = null;
            
            for (ClaimRanks required : ClaimRanks.values()) {
                if (!required.canPerform(other))
                    continue;
                EnumSet<ClaimPermissions> set = permissions.get(required);
                if (set == null)
                    continue;
                for (ClaimPermissions permission : ArrayUtils.sortedEnum(set)) {
                    Text line = Text.translatable(permission.getTranslationKey())
                        .styled(style -> style.withColor(required.getColor()).withItalic(true));
                    
                    if (text == null)
                        text = TextUtils.literal();
                    
                    text.append("\n ")
                        .append(line);
                }
            }
            
            if (text != null)
                map.put(other, text);
        }
        
        return map;
    }
    
    private @NotNull Map<ClaimRanks, Set<GameProfile>> getRankedPlayers(@NotNull ClaimantPlayer claim) {
        Map<ClaimRanks, Set<GameProfile>> map = new HashMap<>();
        for (GameProfile profile : this.getAllPlayers(claim)) {
            ClaimRanks rank = claim.getFriendRank(profile.getId());
            
            Set<GameProfile> profiles = map.get(rank);
            if (profiles == null) {
                profiles = new TreeSet<>(ProfileComparator.INSTANCE);
                map.put(rank, profiles);
            }
            
            profiles.add(profile);
        }
        
        return map;
    }
    
    private @NotNull List<GameProfile> getAllPlayers(@NotNull ClaimantPlayer claim) {
        List<GameProfile> players = new ArrayList<>();
        
        // Add friends
        for (UUID uuid : claim.getFriends()) {
            // Get the friend name
            Text name = ((NameCache) this.client).getPlayerName(uuid);
            players.add(new GameProfile(uuid, name == null ? "Offline player" : name.getString()));
        }
        
        // Add players that are online
        Collection<PlayerListEntry> playerList = this.client.getNetworkHandler()
            .getPlayerList();
        for (PlayerListEntry entry : playerList)
            players.add(entry.getProfile());
        
        return players;
    }
    
    private static final class ProfileComparator implements Comparator<GameProfile> {
        public static final @NotNull ProfileComparator INSTANCE = new ProfileComparator();
        
        @Override
        public int compare(GameProfile one, GameProfile two) {
            return one.getName().compareTo(two.getName());
        }
    }
}
