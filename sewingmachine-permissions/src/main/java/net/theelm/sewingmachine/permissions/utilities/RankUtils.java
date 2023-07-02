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

package net.theelm.sewingmachine.permissions.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.permissions.PermissionNode;
import net.theelm.sewingmachine.permissions.objects.PlayerRank;
import net.theelm.sewingmachine.utilities.Sew;
import net.theelm.sewingmachine.utilities.text.TextUtils;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class RankUtils {

    private static final HashMap<String, PlayerRank> RANKS = new LinkedHashMap<>();
    private static final ConcurrentHashMap<UUID, PlayerRank[]> PLAYER_RANKS = new ConcurrentHashMap<>();
    private static final String GLOBAL_RANK = "*";

    private RankUtils() {}
    
    public static @NotNull CompletableFuture<Suggestions> getSuggestions(@NotNull SuggestionsBuilder builder) {
        Set<Map.Entry<String, PlayerRank>> ranks = RankUtils.RANKS.entrySet();
        for (Map.Entry<String, PlayerRank> entry : ranks) {
            String name = entry.getKey();
            PlayerRank rank = entry.getValue();

            String inherits = rank.getParent();
            Text tooltip = Text.literal("Inherits: ")
                .append(Text.literal(inherits == null ? "*" : inherits).formatted(Formatting.AQUA));
            builder.suggest(name.equals("*") ? TextUtils.quoteWrap(name) : name, tooltip);
        }
        
        return builder.buildFuture();
    }
    
    public static @NotNull <S> CompletableFuture<Suggestions> suggestRanks(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        return RankUtils.getSuggestions(builder);
    }
    
    /*
     * Stored ranks
     */
    public static @Nullable PlayerRank getRank(@Nullable String identifier) {
        if (identifier == null) return null;
        return RankUtils.RANKS.get(identifier);
    }
    public static @NotNull Set<String> getRanks() {
        return RankUtils.RANKS.keySet();
    }

    /*
     * Get player ranks
     */
    public static @NotNull PlayerRank[] getPlayerRanks(@NotNull UUID uuid) {
        return RankUtils.PLAYER_RANKS.computeIfAbsent(uuid, RankUtils::loadPlayerRanks);
    }
    public static @NotNull PlayerRank[] getPlayerRanks(@NotNull GameProfile profile) {
        return RankUtils.getPlayerRanks(profile.getId());
    }
    public static @NotNull PlayerRank[] getPlayerRanks(@NotNull PlayerEntity player) {
        return RankUtils.getPlayerRanks(player.getUuid());
    }
    public static @NotNull PlayerRank[] loadPlayerRanks(@NotNull GameProfile profile) {
        return RankUtils.loadPlayerRanks(profile.getId());
    }
    public static @NotNull PlayerRank[] loadPlayerRanks(@NotNull UUID profile) {
        String uuid = profile.toString();
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

    public static void clearRanks() {
        RankUtils.PLAYER_RANKS.clear();
    }
    
    public static boolean hasPermission(@NotNull PlayerEntity player, @NotNull PermissionNode node) {
        return RankUtils.hasPermission(player, node.getNode());
    }
    public static boolean hasPermission(@NotNull PlayerEntity player, @NotNull String node) {
        boolean result = false;
        
        PlayerRank[] ranks = RankUtils.getPlayerRanks(player);
        for (int i = (ranks.length - 1); i >= 0; --i) {
            PlayerRank rank = ranks[i];
            
            if (rank.isAdditive(node))
                result = true;
            else if (rank.isSubtractive(node))
                result = false;
        }
        
        return result;
    }
    
    public static boolean reload() {
        return RankUtils.reload( false );
    }
    public static boolean reload( boolean verbose ) {
        // Check if enabled in the config
        if (!SewConfig.get(SewCoreConfig.HANDLE_PERMISSIONS))
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
    private static @NotNull JsonObject fileRanks() throws FileNotFoundException {
        JsonObject main = RankUtils.fileLoad();
        if (main.has("ranks") && (main.get("ranks") instanceof JsonObject jsonObject))
            return jsonObject;
        return new JsonObject();
    }
    private static @NotNull JsonObject filePlayers() throws FileNotFoundException {
        JsonObject main = RankUtils.fileLoad();
        if (main.has("players") && (main.get("players") instanceof JsonObject jsonObject))
            return jsonObject;
        return new JsonObject();
    }
    private static @NotNull JsonObject fileLoad() throws FileNotFoundException {
        File ranksFile = new File(
            Sew.getConfDir(),
            "permissions.json"
        );

        JsonElement element = JsonParser.parseReader(new FileReader(ranksFile));
        return element.getAsJsonObject();
    }
    static { reload(); }
}
