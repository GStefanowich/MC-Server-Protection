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
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public final class PlayerNameUtils {
    
    private PlayerNameUtils() {}
    
    public static Text getPlayerChatDisplay(@NotNull ServerPlayerEntity player) {
        return PlayerNameUtils.getPlayerChatDisplay( player, null );
    }
    public static Text getPlayerChatDisplay(@NotNull ServerPlayerEntity player, @Nullable String prepend, Formatting... playerColors) {
        ClaimantPlayer playerPermissions;
        Text playerDisplay = PlayerNameUtils.getPlayerDisplayName( player );
        if ( playerColors.length > 0 )
            playerDisplay.formatted( playerColors );
        
        // Add the players world
        Text format = new LiteralText( "[" ).formatted(Formatting.WHITE)
            .append( formattedWorld( player.dimension ) );
        
        // If the player is in a town, prepend the town name
        if (SewingMachineConfig.INSTANCE.CHAT_SHOW_TOWNS.get() && ((playerPermissions = ClaimantPlayer.get( player.getUuid() )) != null) && (playerPermissions.getTown() != null)) {
            ClaimantTown town = ClaimantTown.get( playerPermissions.getTown() );
            
            // Add the players town
            format.append( "|" )
                .append( town.getName().formatted(Formatting.DARK_AQUA) )
                .append( "] " );
            
            if ( prepend != null )
                format.append( prepend );
            
            // Add the players title
            if ( town.getOwner().equals( player.getUuid() ) )
                format.append( CasingUtils.Sentence( town.getOwnerTitle() ) + " " );
            
        } else {
            format.append( "] " );
            
            if ( prepend != null )
                format.append( prepend );
        }
        
        // Append the player name and return
        return format.append( playerDisplay );
    }
    private static Text getPlayerDisplayName(@NotNull ServerPlayerEntity player) {
        final String name = player.getGameProfile().getName();
        if (((Nicknamable)player).getPlayerNickname() == null)
            return PlayerNameUtils.applyPlayerNameStyle( player.getName().formatted(Formatting.GOLD), player );
        
        return new LiteralText("").append(PlayerNameUtils.applyPlayerNameStyle(
            player.getDisplayName().deepCopy(),
            player
        ));
    }
    private static Text applyPlayerNameStyle(@NotNull Text text, @NotNull ServerPlayerEntity player) {
        final String name = player.getGameProfile().getName();
        return text.styled((styler) ->
            styler.setHoverEvent(new HoverEvent( HoverEvent.Action.SHOW_TEXT, new LiteralText( "Name: " ).formatted(Formatting.GRAY).append( new LiteralText( name ).formatted(Formatting.AQUA) ) ))
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + name + " "))
        );
    }
    
    public static Text formattedWorld(DimensionType dimension) {
        String world = null;
        
        Formatting color = Formatting.OBFUSCATED;
        if (dimension == DimensionType.OVERWORLD) {
            world = "Surface";
            color = Formatting.GREEN;
        } else if (dimension == DimensionType.THE_END) {
            world = "End";
            color = Formatting.DARK_GRAY;
        } else if (dimension == DimensionType.THE_NETHER) {
            world = "Nether";
            color = Formatting.RED;
        }
        
        final HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText( world ).formatted(color));
        
        Text text = new LiteralText( world == null ? "~" : world.substring( 0, 1 ) )
            .formatted( color );
        
        if ( world != null ) {
            text.styled((style) -> {
                style.setHoverEvent( hover );
            });
        }
        return text;
    }
    public static Text fetchPlayerNick(@NotNull UUID uuid) {
        Text out;
        if ((!uuid.equals(CoreMod.spawnID)) && ((out = PlayerNameUtils.getOfflinePlayerNickname( uuid )) != null))
            return out;
        return PlayerNameUtils.fetchPlayerName( uuid );
    }
    public static Text fetchPlayerName(@NotNull UUID uuid) {
        // If we're looking up UUID 0, 0 (Spawn) don't try to do a lookup
        if ( uuid.equals( CoreMod.spawnID ) )
            return new LiteralText(SewingMachineConfig.INSTANCE.NAME_SPAWN.get());
        
        // Check if there is an online player with UUID (No unnecessary web calls)
        Text playerName;
        if ((playerName = getOnlinePlayerName(uuid)) != null)
            return playerName;
        
        // Log that a request is being made
        CoreMod.logMessage( "Looking up username of " + uuid.toString() );
        
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
                
                JsonArray nameHistory = new JsonParser().parse( content.toString() ).getAsJsonArray();
                JsonObject nameLatest = nameHistory.get( nameHistory.size() - 1 ).getAsJsonObject();
                
                playerName = new LiteralText( nameLatest.get( "name" ).getAsString() );
            }
            
        } catch (IOException e) {
            CoreMod.logError( e );
            
        } finally {
            if (connection != null)
                connection.disconnect();
            if ( playerName == null )
                CoreMod.logMessage( "Player name of " + uuid.toString() + " [LOOKUP FAILED]" );
            else CoreMod.logMessage( "Player name of " + uuid.toString() + " is " + playerName.getString() );
        }
        
        return ( playerName == null ? new LiteralText("Unknown player") : playerName );
    }
    @Nullable
    private static Text getOnlinePlayerName(@NotNull UUID uuid) {
        MinecraftServer server;
        if ((server = CoreMod.getServer()) == null)
            return null;
        ServerPlayerEntity player;
        if ((player = server.getPlayerManager().getPlayer( uuid )) == null)
            return null;
        return player.getName();
    }
    @Nullable
    private static Text getOfflinePlayerNickname(@NotNull UUID uuid) {
        CompoundTag tag = NbtUtils.readOfflinePlayerData( uuid );
        if ((tag != null) && tag.containsKey("PlayerNickname", 8))
            return Text.Serializer.fromJson(tag.getString("PlayerNickname"));
        return null;
    }
    
    private static String stripUUID(@NotNull UUID uuid) {
        return uuid.toString().replace("-", "");
    }
    
}
