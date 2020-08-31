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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.TheElm.project.CoreMod;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.interfaces.PlayerServerLanguage;
import net.TheElm.project.interfaces.ServerTranslatable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Locale;

public final class TranslatableServerSide {
    
    private TranslatableServerSide() {}
    
    public static void send(@NotNull ServerCommandSource source, String key, Object... objects) {
        source.sendFeedback(TranslatableServerSide.text(source, key, objects), true);
    }
    public static void send(@NotNull PlayerEntity player, String key, Object... objects) {
        player.sendSystemMessage(TranslatableServerSide.text(player, key, objects), Util.NIL_UUID);
    }
    
    public static @NotNull MutableText text(ServerCommandSource source, String key, Object... objects) {
        if (source.getEntity() instanceof ServerPlayerEntity)
            return TranslatableServerSide.text( (ServerPlayerEntity)source.getEntity(), key, objects );
        return TranslatableServerSide.text( Locale.getDefault(), key, objects );
    }
    public static @NotNull MutableText text(PlayerEntity player, String key, Object... objects) {
        if (!(player instanceof ServerPlayerEntity))
            return null;
        return TranslatableServerSide.text((ServerPlayerEntity) player, key, objects);
    }
    public static @NotNull MutableText text(ServerPlayerEntity player, String key, Object... objects) {
        return TranslatableServerSide.text( ((PlayerServerLanguage)player).getClientLanguage(), key, objects );
    }
    public static @NotNull MutableText text(Locale language, String key, Object... objects) {
        String text = TranslatableServerSide.getTranslation( language, key );
        
        for (int i = 0; i < objects.length; ++i) {
            Object obj = objects[i];
            if (obj instanceof Text) {
                objects[i] = ((Text) obj).copy();
            } else if (obj == null) {
                objects[i] = "null";
            }
        }
        
        return TranslatableServerSide.replace( language, text, objects);
    }
    private static @NotNull MutableText replace(Locale language, String text, Object... objects) {
        if ( objects.length <= 0 )
            return new LiteralText( text );
        String[] separated = text.split( "((?<=%[a-z])|(?=%[a-z]))" );
        
        // Get the formatter for numbers
        NumberFormat formatter = NumberFormat.getInstance( language );
        int O = 0;
    
        MutableText out = null;
        for ( String seg : separated ) {
            // If is a variable
            if ( matchAny( seg, "%s", "%d", "%f" ) ) {
                // Get the objects that were provided
                Object obj = objects[ O++ ];
                if (obj instanceof ServerTranslatable)
                    obj = ((ServerTranslatable)obj).translate( language ).formatted(Formatting.AQUA);
                
                if ( ("%s".equalsIgnoreCase( seg )) && ( obj instanceof MutableText ) ) {
                    // Create if null
                    if (out == null) out = new LiteralText("");
                    // Color translations
                    if (obj instanceof TranslatableText) ((MutableText) obj).formatted(Formatting.DARK_AQUA);
                    // Append
                    out.append( (Text)obj );
                } else if ( ("%d".equalsIgnoreCase( seg )) && ( obj instanceof Number ) ) {
                    // Create if null
                    if (out == null) out = new LiteralText("");
                    // Append
                    out.append(new LiteralText( formatter.format( ((Number) obj).longValue() ) ).formatted(Formatting.AQUA));
                } else {
                    // Create if null
                    if (out == null) out = new LiteralText( obj.toString() );
                    // Append if not null
                    else out.append( obj.toString() );
                }
            } else {
                // If not a variable
                if (out == null) out = new LiteralText( seg );
                else out.append( seg );
            }
        }
        
        return (out == null ? new LiteralText( "" ) : out);
    }
    
    public static @NotNull ExceptionTranslatableServerSide exception(String key) {
        return new ExceptionTranslatableServerSide(key);
    }
    public static @NotNull ExceptionTranslatableServerSide exception(String key, int num) {
        return new ExceptionTranslatableServerSide(key);
    }
    
    private static String getTranslation(Locale language, String key) {
        JsonObject object = TranslatableServerSide.readLanguageFile( language );
        if ( (language != Locale.US) && (!object.has( key )) )
            return TranslatableServerSide.getTranslation( Locale.US, key );
        JsonElement element = object.get( key );
        if ( element == null ) {
            CoreMod.logInfo( "Missing translation key \"" + key + "\"!" );
            return "";
        }
        return element.getAsString();
    }
    private static JsonObject readLanguageFile(Locale language) {
        String filePath;
        InputStream resource = CoreMod.class.getResourceAsStream(
            filePath = TranslatableServerSide.getResourcePath( language )
        );
        if (resource == null) {
            // If not already using English, Fallback to English
            if (language != Locale.US)
                return TranslatableServerSide.readLanguageFile(Locale.US);
            // Throw an exception
            throw new NullPointerException("Could not read language file \"" + filePath + "\"");
        }
        // Return the JSON language file
        return new JsonParser().parse(new InputStreamReader( resource )).getAsJsonObject();
    }
    private static String getResourcePath(Locale locale) {
        return "/assets/" + CoreMod.MOD_ID + "/lang/" + (locale.getLanguage() + "_" + locale.getCountry()).toLowerCase() + ".json";
    }
    
    private static boolean matchAny(@NotNull String needle, String... haystack ) {
        for ( String hay : haystack ) {
            if ( needle.equalsIgnoreCase( hay ) )
                return true;
        }
        return false;
    }
    
}
