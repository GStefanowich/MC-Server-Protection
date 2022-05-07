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

package net.theelm.sewingmachine.utilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.theelm.sewingmachine.CoreMod;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.PlayerServerLanguage;
import net.theelm.sewingmachine.interfaces.ServerTranslatable;
import net.minecraft.command.CommandSource;
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
import java.util.Objects;

public final class TranslatableServerSide {
    
    private TranslatableServerSide() {}
    
    public static void send(@NotNull CommandSource source, String key, Object... objects) {
        TranslatableServerSide.send(source, true, key, objects);
    }
    public static void send(@NotNull CommandSource source, boolean notify, String key, Object... objects) {
        if (source instanceof ServerCommandSource serverSource)
            serverSource.sendFeedback(TranslatableServerSide.text(source, key, objects), notify);
    }
    public static void send(@NotNull PlayerEntity player, String key, Object... objects) {
        player.sendSystemMessage(TranslatableServerSide.text(player, key, objects), Util.NIL_UUID);
    }
    
    public static @NotNull MutableText text(@NotNull CommandSource source, String key, Object... objects) {
        if (source instanceof ServerCommandSource serverSource && serverSource.getEntity() instanceof ServerPlayerEntity serverPlayer)
            return TranslatableServerSide.text(serverPlayer, key, objects );
        return TranslatableServerSide.text(Locale.getDefault(), key, objects);
    }
    public static @NotNull MutableText text(@NotNull PlayerEntity player, String key, Object... objects) {
        if (!(player instanceof ServerPlayerEntity serverPlayer))
            return null;
        return TranslatableServerSide.text(serverPlayer, key, objects);
    }
    public static @NotNull MutableText text(@NotNull ServerPlayerEntity player, String key, Object... objects) {
        return TranslatableServerSide.text(((PlayerServerLanguage)player).getClientLanguage(), key, objects);
    }
    public static @NotNull MutableText text(@NotNull Locale language, @NotNull String key, @NotNull Object... objects) {
        String translation = TranslatableServerSide.getTranslation(language, key);
        
        for (int i = 0; i < objects.length; ++i) {
            Object obj = objects[i];
            if (obj instanceof Text text) {
                objects[i] = text.shallowCopy();
            } else if (obj == null) {
                objects[i] = "null";
            }
        }
        
        return TranslatableServerSide.replace(language, translation, objects);
    }
    private static @NotNull MutableText replace(@NotNull Locale language, @NotNull String text, @NotNull Object... objects) {
        if ( objects.length <= 0 )
            return new LiteralText(text);
        String[] separated = text.split( "((?<=%[a-z])|(?=%[a-z]))" );
        
        // Get the formatter for numbers
        NumberFormat formatter = NumberFormat.getInstance(language);
        int c = 0;
        
        MutableText out = null;
        for ( String seg : separated ) {
            // If is a variable
            if ( matchAny( seg, "%s", "%d", "%f" ) ) {
                // Get the objects that were provided
                Object obj = objects[c++];
                if (obj instanceof ServerTranslatable translatable)
                    obj = translatable.translate(language).formatted(Formatting.AQUA);
                
                if ( ("%s".equalsIgnoreCase(seg)) && ( obj instanceof MutableText mutableText) ) {
                    // Create if null
                    if (out == null) out = new LiteralText("");
                    // Color translations
                    if (obj instanceof TranslatableText translatableText) translatableText.formatted(Formatting.DARK_AQUA);
                    // Append
                    out.append(mutableText);
                } else if ( ("%d".equalsIgnoreCase( seg )) && (obj instanceof Number number) ) {
                    // Create if null
                    if (out == null) out = new LiteralText("");
                    // Append
                    out.append(new LiteralText(formatter.format(number.longValue())).formatted(Formatting.AQUA));
                } else {
                    // Create if null
                    if (out == null) out = new LiteralText(obj.toString());
                    // Append if not null
                    else out.append(obj.toString());
                }
            } else {
                // If not a variable
                if (out == null) out = new LiteralText(seg);
                else out.append(seg);
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
        if ( !Objects.equals(language, Locale.US) && !object.has(key))
            return TranslatableServerSide.getTranslation(Locale.US, key);
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
        return JsonParser.parseReader(new InputStreamReader( resource )).getAsJsonObject();
    }
    private static @NotNull String getResourcePath(@NotNull Locale locale) {
        return "/assets/" + CoreMod.MOD_ID + "/lang/" + (locale.getLanguage() + "_" + locale.getCountry()).toLowerCase() + ".json";
    }
    
    private static boolean matchAny(@NotNull String needle, @NotNull String... haystack ) {
        for ( String hay : haystack ) {
            if ( needle.equalsIgnoreCase( hay ) )
                return true;
        }
        return false;
    }
    
}
