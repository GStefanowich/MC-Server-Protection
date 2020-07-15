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
import net.TheElm.project.interfaces.PlayerPermissions;
import net.TheElm.project.permissions.PermissionNode;
import net.TheElm.project.protections.ranks.PlayerRank;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RankUtils {
    
    private static HashMap<String, PlayerRank> RANKS = new LinkedHashMap<>();
    private static final String GLOBAL_RANK = "*";
    
    private RankUtils() {}
    
    /*
     * Stored ranks
     */
    @Nullable
    public static PlayerRank getRank(@Nullable String identifier) {
        if (identifier == null) return null;
        return RankUtils.RANKS.get( identifier );
    }
    public static Set<String> getRanks() {
        return RANKS.keySet();
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
        return ((PlayerPermissions) player).getRanks();
    }
    public static PlayerRank[] loadPlayerRanks(@NotNull GameProfile profile) {
        String uuid = profile.getId().toString();
        try {
            JsonObject json = RankUtils.filePlayers();
            List<PlayerRank> ranks = new ArrayList<>();
            
            // Everyone is a "GLOBAL"
            PlayerRank rank = getRank(GLOBAL_RANK);
            if (rank != null) ranks.add(rank);
            
            // Get all the players listed ranks
            JsonElement list;
            if ((json.has(uuid) && ((list = json.get(uuid)) instanceof JsonArray)) || (json.has(GLOBAL_RANK) && ((list = json.get(GLOBAL_RANK)) instanceof JsonArray))) {
                for (JsonElement listElement : list.getAsJsonArray()) {
                    String identifier = listElement.getAsString();
                    if (!GLOBAL_RANK.equals(identifier)) {
                        rank = getRank(identifier);
                        if (rank != null) ranks.add(rank);
                    }
                }
            }
            
            // Sort the ranks in ascending order
            ranks.sort(PlayerRank::compareTo);
            
            // Return as a primitive array
            return ranks.toArray(new PlayerRank[0]);
        } catch (FileNotFoundException e) {
            return new PlayerRank[0];
        }
    }
    public static boolean hasPermission(@NotNull ServerCommandSource source, @Nullable PermissionNode permission) {
        if ((!SewingMachineConfig.INSTANCE.HANDLE_PERMISSIONS.get()) || (permission == null))
            return false;
        Entity entity = source.getEntity();
        return ((entity instanceof ServerPlayerEntity) && RankUtils.hasPermission(((ServerPlayerEntity) entity), permission));
    }
    public static boolean hasPermission(@NotNull ServerPlayerEntity player, @NotNull PermissionNode permission) {
        boolean result = false;
        
        PlayerRank[] ranks = RankUtils.getPlayerRanks(player);
        for (int i = (ranks.length - 1); i >= 0; --i) {
            PlayerRank rank = ranks[i];
            
            if (rank.isAdditive(permission.getNode()))
                result = true;
            else if (rank.isSubtractive(permission.getNode()))
                result = false;
        }
        
        return result;
    }
    public static boolean hasPermission(@NotNull PlayerEntity player, @NotNull PermissionNode permission) {
        if (!(player instanceof ServerPlayerEntity)) return true;
        return RankUtils.hasPermission((ServerPlayerEntity)player, permission);
    }
    
    public static boolean reload() {
        return RankUtils.reload( false );
    }
    public static boolean reload( boolean verbose ) {
        // Check if enabled in the config
        if (!SewingMachineConfig.INSTANCE.HANDLE_PERMISSIONS.get())
            return false;
        RankUtils.RANKS.clear();
        
        CoreMod.logInfo("Loading permissions file.");
        
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
                if (rankData.has("display") && (tmp = (rankData.get("display"))).getAsJsonPrimitive().isString())
                    display = tmp.getAsString();
    
                // Create the rank
                PlayerRank rank = new PlayerRank(identifier, display);
    
                // Set the rank parent
                if (rankData.has("inherit") && (tmp = (rankData.get("inherit"))).getAsJsonPrimitive().isString())
                    rank.setParent(tmp.getAsString());
    
                // Add the ranks permissions
                if (rankData.has("permissions") && (tmp = rankData.get("permissions")).isJsonArray()) {
                    for (JsonElement node : tmp.getAsJsonArray()) {
                        if (node.getAsJsonPrimitive().isString())
                            rank.addNode(node.getAsString());
                    }
                }
                
                RANKS.put(identifier, rank);
            }
            
            return true;
        } catch (IOException e) {
            if ( verbose ) CoreMod.logError( e );
            return false;
        } finally {
            // Permission to interact with the world should be given by default, unless taken away
            PlayerRank rank;
            if ((rank = getRank(GLOBAL_RANK)) != null) {
                if (!( rank.isAdditive("world.interact") || rank.isSubtractive("world.interact") )) {
                    rank.addNode("+world.interact");
                    CoreMod.logInfo("Added interact to the EVERYONE perm.");
                } else CoreMod.logInfo("EVERYONE perm has perm.");
            } else CoreMod.logInfo("Could not find EVERYONE perm.");
        }
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
    static { reload(); }
    
}
