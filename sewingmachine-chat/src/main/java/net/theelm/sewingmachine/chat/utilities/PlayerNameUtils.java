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

package net.theelm.sewingmachine.chat.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.chat.interfaces.Nicknamable;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.utilities.DimensionUtils;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public final class PlayerNameUtils {
    
    private PlayerNameUtils() {}
    
    public static @NotNull MutableText getPlayerDisplayName(@NotNull ServerPlayerEntity player) {
        if (((Nicknamable)player).getPlayerNickname() == null)
            return PlayerNameUtils.applyPlayerNameStyle(((MutableText)player.getName()).formatted(Formatting.GOLD), player);
        
        return TextUtils.literal().append(PlayerNameUtils.applyPlayerNameStyle(
            FormattingUtils.deepCopy(player.getDisplayName()),
            player
        ));
    }
    private static @NotNull MutableText applyPlayerNameStyle(@NotNull MutableText text, @NotNull ServerPlayerEntity player) {
        final String name = player.getGameProfile().getName();
        
        return text.styled((styler) ->
            styler.withHoverEvent(new HoverEvent( HoverEvent.Action.SHOW_TEXT, Text.literal("Name: ").formatted(Formatting.GRAY).append(Text.literal( name ).formatted(Formatting.AQUA))))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + name + " "))
        );
    }
    
    /*
     * Message Components
     */
    public static @NotNull MutableText formattedWorld(@Nullable World world) {
        return PlayerNameUtils.formattedWorld(world == null ? null : world.getRegistryKey(), false);
    }
    public static @NotNull MutableText formattedWorld(@Nullable RegistryKey<World> world, boolean showAsLong) {
        // Create the text
        MutableText longer = DimensionUtils.longDimensionName(world);
        
        // Create the hover event
        StyleApplicator formatting = DimensionUtils.dimensionColor(world);
        
        return (showAsLong ? longer : DimensionUtils.shortDimensionName(world))
            .styled(formatting)
            .styled(MessageUtils.simpleHoverText(longer.styled(formatting)));
    }
    
    public static @NotNull MutableText fetchPlayerNick(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        MutableText out;
        if ((!uuid.equals(CoreMod.SPAWN_ID)) && ((out = PlayerNameUtils.getOfflinePlayerNickname(uuid)) != null))
            return out;
        return PlayerNameUtils.fetchPlayerName(server, uuid);
    }
    public static @NotNull MutableText fetchPlayerName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        // If we're looking up UUID 0, 0 (Spawn) don't try to do a lookup
        if ( uuid.equals( CoreMod.SPAWN_ID) )
            return Text.literal(SewConfig.get(SewCoreConfig.NAME_SPAWN));
        
        // Check if there is an online player with UUID (No unnecessary web calls)
        MutableText playerName;
        if ((playerName = PlayerNameUtils.getOnlinePlayerName(server, uuid)) != null)
            return playerName;
        
        String cachedName;
        if (((cachedName = PlayerNameUtils.getCachedPlayerName(server, uuid)) != null) && (!StringUtils.isBlank(cachedName)))
            return Text.literal(cachedName);
        
        // Log that a request is being made
        CoreMod.logInfo("Looking up username of " + uuid);
        
        HttpURLConnection connection = null;
        try {
            // Create the URL
            URL url = new URL( "https://api.mojang.com/user/profiles/" + PlayerNameUtils.stripUUID(uuid) + "/names" );
            
            // Opening connection 
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod( "GET" );
            
            // Buffered reader
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            )) {
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                
                JsonArray nameHistory = JsonParser.parseString(content.toString()).getAsJsonArray();
                JsonObject nameLatest = nameHistory.get( nameHistory.size() - 1 ).getAsJsonObject();
                
                GameProfile profile = new GameProfile(
                    uuid,
                    nameLatest.get("name").getAsString()
                );
                
                playerName = Text.literal(profile.getName());
                
                // Save the player name to the cache
                server.getUserCache()
                    .add(profile);
            }
            
        } catch (IOException e) {
            CoreMod.logError( e );
            
        } finally {
            if (connection != null)
                connection.disconnect();
            if ( playerName == null )
                CoreMod.logInfo("Player name of " + uuid + " [LOOKUP FAILED]");
            else CoreMod.logInfo("Player name of " + uuid + " is " + playerName.getString());
        }
        
        return ( playerName == null ? Text.literal("Unknown player") : playerName );
    }
    private static @Nullable MutableText getOnlinePlayerName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        ServerPlayerEntity player;
        if ((player = server.getPlayerManager().getPlayer(uuid)) == null)
            return null;
        return (MutableText) player.getName();
    }
    private static @Nullable String getCachedPlayerName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        return server.getUserCache()
            .getByUuid(uuid)
            .map(GameProfile::getName)
            .orElse(null);
    }
    private static @Nullable MutableText getOfflinePlayerNickname(@NotNull UUID uuid) {
        try {
            NbtCompound tag = NbtUtils.readOfflinePlayerData(uuid);
            if ((tag != null) && tag.contains("PlayerNickname", NbtElement.STRING_TYPE))
                return Text.Serializer.fromJson(tag.getString("PlayerNickname"));
        } catch (NbtNotFoundException ignored) {}
        return null;
    }
    
    private static @NotNull String stripUUID(@NotNull UUID uuid) {
        return uuid.toString().replace("-", "");
    }
    
}
