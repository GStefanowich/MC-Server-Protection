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

package net.TheElm.project.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.protections.ranks.PlayerRank;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RankUtils {
    
    private static HashMap<String, PlayerRank> RANKS = new LinkedHashMap<>();
    
    private RankUtils() {}
    
    /*
     * Stored ranks
     */
    @Nullable
    public static PlayerRank getRank(@Nullable String identifier) {
        if (identifier == null) return null;
        return RankUtils.RANKS.get( identifier );
    }
    
    /*
     * Get player ranks
     */
    public static PlayerRank[] getPlayerRanks(@NotNull GameProfile profile) {
        ServerPlayerEntity player = ServerCore.getPlayer( profile.getId() );
        if (player == null)
            return new PlayerRank[0];
        return RankUtils.getPlayerRanks(player);
    }
    public static PlayerRank[] getPlayerRanks(@NotNull ServerPlayerEntity player) {
        return ((PlayerData) player).getRanks();
    }
    public static PlayerRank[] loadPlayerRanks(@NotNull GameProfile profile) {
        String uuid = profile.getId().toString();
        try {
            JsonObject json = RankUtils.filePlayers();
            List<PlayerRank> ranks = new ArrayList<>();
            
            // Everyone is a "GLOBAL"
            PlayerRank rank = getRank("*");
            if (rank != null) ranks.add(rank);
            
            // Get all the players listed ranks
            JsonElement list;
            if ((json.has(uuid) && ((list = json.get(uuid)) instanceof JsonArray)) || (json.has("*") && ((list = json.get("*")) instanceof JsonArray))) {
                for (JsonElement listElement : list.getAsJsonArray()) {
                    String identifier = listElement.getAsString();
                    if (!"*".equals(identifier)) {
                        rank = getRank(identifier);
                        if (rank != null) ranks.add(rank);
                    }
                }
            }
            
            // Sort the ranks in ascending order
            ranks.sort(PlayerRank::compareTo);
            
            for (PlayerRank tmp : ranks) {
                System.out.println( tmp.getIdentifier() );
            }
            
            return ranks.toArray(new PlayerRank[0]);
        } catch (FileNotFoundException e) {
            return new PlayerRank[0];
        }
    }
    public static boolean hasPermission(@NotNull ServerPlayerEntity player, String permission) {
        boolean allowed = false;
        
        PlayerRank[] ranks = RankUtils.getPlayerRanks( player );
        for (int i = (ranks.length - 1); i >= 0; --i) {
            PlayerRank rank = ranks[i];
            
            if (rank.isAdditive( permission ))
                allowed = true;
            else if (rank.isSubtractive( permission ))
                allowed = false;
        }
        
        return allowed;
    }
    
    public static void init() {
        // Check if enabled in the config
        if (!SewingMachineConfig.INSTANCE.HANDLE_PERMISSIONS.get())
            return;
        RankUtils.RANKS.clear();
        
        // Read from the `ranks.json` file
        try {
            JsonObject object = RankUtils.fileRanks();
            for (Map.Entry<String, JsonElement> list : object.entrySet()) {
                // Get the pair information
                String identifier = list.getKey();
                JsonElement tmp = list.getValue();
                if (!tmp.isJsonObject()) continue;
                
                // Get the rank information
                JsonObject rankData = tmp.getAsJsonObject();
                
                // Get how the rank should be displayed
                String display = null;
                if (rankData.has( "display" ) && (tmp = (rankData.get("display"))).getAsJsonPrimitive().isString())
                    display = tmp.getAsString();
                
                // Create the rank
                PlayerRank rank = new PlayerRank( identifier, display );
                
                // Set the rank parent
                if (rankData.has("inherit") && (tmp = (rankData.get("inherit"))).getAsJsonPrimitive().isString())
                    rank.setParent( tmp.getAsString() );
                
                // Add the ranks permissions
                if (rankData.has("permissions") && (tmp = rankData.get("permissions")).isJsonArray()) {
                    for (JsonElement node : tmp.getAsJsonArray()) {
                        if (node.getAsJsonPrimitive().isString())
                            rank.addNode(node.getAsString());
                    }
                }
                
                RANKS.put( identifier, rank );
            }
        } catch (FileNotFoundException ignored) {}
    }
    private static JsonObject fileRanks() throws FileNotFoundException {
        JsonObject main = RankUtils.fileLoad();
        JsonElement ranks;
        if (main.has("ranks") && ((ranks = main.get("ranks")) instanceof JsonObject))
            return (JsonObject) ranks;
        return new JsonObject();
    }
    private static JsonObject filePlayers() throws FileNotFoundException {
        JsonObject main = RankUtils.fileLoad();
        JsonElement players;
        if (main.has("players") && ((players = main.get("players")) instanceof JsonObject))
            return (JsonObject) players;
        return new JsonObject();
    }
    private static JsonObject fileLoad() throws FileNotFoundException {
        File ranksFile = new File(
            CoreMod.getConfDir(),
            "permissions.json"
        );
        
        JsonParser jp = new JsonParser();
        JsonElement element = jp.parse(new FileReader(ranksFile));
        return element.getAsJsonObject();
    }
    static { init(); }
    
}
