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
import net.minecraft.text.TranslatableTextContent;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.PlayerServerLanguage;
import net.theelm.sewingmachine.interfaces.ServerTranslatable;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.objects.SewModules;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public final class ServerText {
    
    private ServerText() {}
    
    public static void send(@NotNull CommandSource source, String key, Object... objects) {
        ServerText.send(source, true, key, objects);
    }
    public static void send(@NotNull CommandSource source, boolean notify, String key, Object... objects) {
        if (source instanceof ServerCommandSource serverSource)
            serverSource.sendFeedback(() -> ServerText.translatable(source, key, objects), notify);
    }
    public static void send(@NotNull PlayerEntity player, String key, Object... objects) {
        player.sendMessage(ServerText.translatable(player, key, objects));
    }
    
    public static @NotNull MutableText translatable(@NotNull CommandSource source, String key, Object... objects) {
        if (source instanceof ServerCommandSource serverSource && serverSource.getEntity() instanceof ServerPlayerEntity serverPlayer)
            return ServerText.translatable(serverPlayer, key, objects );
        return ServerText.translatable(Locale.getDefault(), key, objects);
    }
    public static @NotNull MutableText translatable(@NotNull PlayerEntity player, String key, Object... objects) {
        if (!(player instanceof ServerPlayerEntity serverPlayer))
            return Text.translatable(key, objects); // If done client side, return as a translation key to be handled clientside
        return ServerText.translatable(serverPlayer, key, objects);
    }
    public static @NotNull MutableText translatable(@NotNull ServerPlayerEntity player, String key, Object... objects) {
        return ServerText.translatable(((PlayerServerLanguage) player).getClientLanguage(), key, objects);
    }
    public static @NotNull MutableText translatable(@NotNull Locale language, @NotNull String key, @NotNull Object... objects) {
        String value = ServerText.getTranslation(language, key);
        
        for (int i = 0; i < objects.length; ++i) {
            Object obj = objects[i];
            if (obj instanceof Text text) {
                objects[i] = TextUtils.literal(text);
            } else if (obj == null) {
                objects[i] = "null";
            }
        }
        
        return ServerText.replace(language, key, value, objects);
    }
    
    /**
     * Convert our objects to be handled by the client
     * @param language The user language
     * @param key The translation key
     * @param fallback tHe translation key value
     * @param objects Values to be used in the translation key
     * @return
     */
    private static @NotNull MutableText replace(@NotNull Locale language, @NotNull String key, @NotNull String fallback, @NotNull Object... objects) {
        if ( objects.length == 0 )
            return Text.translatableWithFallback(key, fallback);
        String[] separated = fallback.split("((?<=%[a-z])|(?=%[a-z]))");
        
        // Get the formatter for numbers
        NumberFormat formatter = NumberFormat.getInstance(language);
        int c = 0;
        
        MutableText out = null;
        for ( String seg : separated ) {
            // If is a variable
            if ( matchAny(seg, "%s", "%d", "%f") ) {
                // Get the objects that were provided
                Object obj = objects[c++];
                if (obj instanceof ServerTranslatable translatable)
                    obj = translatable.translate(language).formatted(Formatting.AQUA);
                
                if ( ("%s".equalsIgnoreCase(seg)) && ( obj instanceof Text text) ) {
                    // Create if null
                    if (out == null)
                        out = Text.literal("");
                    // Color translations
                    if (text.getContent() instanceof TranslatableTextContent translatableText && text instanceof MutableText mutableText)
                        mutableText.formatted(Formatting.DARK_AQUA);
                    // Append
                    out.append(text);
                } else if ( ("%d".equalsIgnoreCase( seg )) && (obj instanceof Number number) ) {
                    // Create if null
                    if (out == null)
                        out = Text.literal("");
                    // Append
                    out.append(Text.literal(formatter.format(number.longValue())).formatted(Formatting.AQUA));
                } else {
                    // Create if null
                    if (out == null)
                        out = Text.literal(obj.toString());
                    // Append if not null
                    else out.append(obj.toString());
                }
            } else {
                // If not a variable
                if (out == null)
                    out = Text.literal(seg);
                else out.append(seg);
            }
        }
        
        return (out == null ? Text.translatable(key) : out);
    }
    
    public static @NotNull ExceptionTranslatableServerSide exception(String key) {
        return new ExceptionTranslatableServerSide(key);
    }
    public static @NotNull ExceptionTranslatableServerSide exception(String key, int num) {
        return new ExceptionTranslatableServerSide(key);
    }
    
    private static @NotNull String getTranslation(Locale language, String key) {
        JsonObject object = ServerText.readLanguageFile(language);
        if ( !Objects.equals(language, Locale.US) && !object.has(key))
            return ServerText.getTranslation(Locale.US, key);
        JsonElement element = object.get(key);
        if ( element == null ) {
            CoreMod.logInfo("Missing translation key \"" + key + "\"!");
            return "";
        }
        return element.getAsString();
    }
    private static JsonObject readLanguageFile(Locale language) {
        String filePath;
        InputStream resource = CoreMod.class.getResourceAsStream(
            filePath = ServerText.getResourcePath(language)
        );
        if (resource == null) {
            // If not already using English, Fallback to English
            if (language != Locale.US)
                return ServerText.readLanguageFile(Locale.US);
            // Throw an exception
            throw new NullPointerException("Could not read language file \"" + filePath + "\"");
        }
        
        // Return the JSON language file
        return JsonParser.parseReader(new InputStreamReader( resource )).getAsJsonObject();
    }
    private static @NotNull String getResourcePath(@NotNull Locale locale) {
        return "/assets/" + SewModules.MODULE + "/lang/" + (locale.getLanguage() + "_" + locale.getCountry()).toLowerCase() + ".json";
    }
    
    private static boolean matchAny(@NotNull String needle, @NotNull String... haystack ) {
        for ( String hay : haystack ) {
            if ( needle.equalsIgnoreCase( hay ) )
                return true;
        }
        return false;
    }
    
}
