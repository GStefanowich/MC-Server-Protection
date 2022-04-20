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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.StyleApplicator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
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
        
        return new LiteralText("").append(PlayerNameUtils.applyPlayerNameStyle(
            FormattingUtils.deepCopy(player.getDisplayName()),
            player
        ));
    }
    public static @NotNull MutableText getPlayerRawName(@NotNull PlayerEntity player) {
        return new LiteralText(player.getDisplayName().getString());
    }
    private static @NotNull MutableText applyPlayerNameStyle(@NotNull MutableText text, @NotNull ServerPlayerEntity player) {
        final String name = player.getGameProfile().getName();
        
        return text.styled((styler) ->
            styler.withHoverEvent(new HoverEvent( HoverEvent.Action.SHOW_TEXT, new LiteralText("Name: ").formatted(Formatting.GRAY).append(new LiteralText( name ).formatted(Formatting.AQUA))))
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
    public static @NotNull MutableText formattedChat(@NotNull ChatRooms chatRoom) {
        String name = CasingUtils.sentence( chatRoom.name() );
        return new LiteralText( name.substring( 0, 1 ) )
            .formatted( Formatting.DARK_GRAY )
            .styled((style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText( name ).formatted( Formatting.WHITE )))));
    }
    
    public static @NotNull MutableText fetchPlayerNick(@NotNull UUID uuid) {
        MutableText out;
        if ((!uuid.equals(CoreMod.SPAWN_ID)) && ((out = PlayerNameUtils.getOfflinePlayerNickname(uuid)) != null))
            return out;
        return PlayerNameUtils.fetchPlayerName( uuid );
    }
    public static @NotNull MutableText fetchPlayerName(@NotNull UUID uuid) {
        // If we're looking up UUID 0, 0 (Spawn) don't try to do a lookup
        if ( uuid.equals( CoreMod.SPAWN_ID) )
            return new LiteralText(SewConfig.get(SewConfig.NAME_SPAWN));
        
        // Check if there is an online player with UUID (No unnecessary web calls)
        MutableText playerName;
        if ((playerName = getOnlinePlayerName(uuid)) != null)
            return playerName;
        
        String cachedName;
        if (((cachedName = getCachedPlayerName(uuid)) != null) && (!StringUtils.isBlank(cachedName)))
        
        // Log that a request is being made
        CoreMod.logInfo( "Looking up username of " + uuid.toString() );
        
        HttpURLConnection connection = null;
        try {
            // Create the URL
            URL url = new URL( "https://api.mojang.com/user/profiles/" + PlayerNameUtils.stripUUID( uuid ) + "/names" );
            
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
                
                playerName = new LiteralText(nameLatest.get( "name" ).getAsString());
                
                // Save the player name to the cache
                ServerCore.get().getUserCache().add(new GameProfile(
                    uuid,
                    playerName.getString()
                ));
            }
            
        } catch (IOException e) {
            CoreMod.logError( e );
            
        } finally {
            if (connection != null)
                connection.disconnect();
            if ( playerName == null )
                CoreMod.logInfo("Player name of " + uuid.toString() + " [LOOKUP FAILED]");
            else CoreMod.logInfo("Player name of " + uuid.toString() + " is " + playerName.getString());
        }
        
        return ( playerName == null ? new LiteralText("Unknown player") : playerName );
    }
    private static @Nullable MutableText getOnlinePlayerName(@NotNull UUID uuid) {
        MinecraftServer server = ServerCore.get();
        ServerPlayerEntity player;
        if ((player = server.getPlayerManager().getPlayer( uuid )) == null)
            return null;
        return (MutableText) player.getName();
    }
    private static @Nullable String getCachedPlayerName(@NotNull UUID uuid) {
        return ServerCore.get()
            .getUserCache()
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
