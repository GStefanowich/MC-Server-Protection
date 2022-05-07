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

package net.theelm.sewingmachine.objects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.theelm.sewingmachine.CoreMod;
import net.theelm.sewingmachine.enums.ChatRooms;
import net.theelm.sewingmachine.interfaces.chat.ChatFunction;
import net.theelm.sewingmachine.utilities.CasingUtils;
import net.theelm.sewingmachine.utilities.ChatVariables;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatFormat {
    private final String raw;
    private final Text formatted;
    
    private ChatFormat(@NotNull String raw) {
        this.raw = raw;
        this.formatted = FormattingUtils.stringToText(raw);
        
        // Debug
        CoreMod.logDebug(raw);
    }
    
    public @NotNull Text format(@NotNull final ServerCommandSource source, @NotNull final ChatRooms chatRoom, @NotNull final Text message) {
        return FormattingUtils.visitVariables(
            TextUtils.deepCopy(this.formatted),
            (text, segment) -> ChatFormat.replaceVariables(text, segment, source, chatRoom, message)
        );
    }
    
    @Override
    public String toString() {
        return this.raw;
    }
    
    public static @NotNull ChatFormat parse(@NotNull String string) {
        return new ChatFormat(string);
    }
    public static @NotNull ChatFormat parse(@NotNull JsonElement config) {
        return ChatFormat.parse(config.getAsString());
    }
    public static JsonElement serializer(@NotNull ChatFormat src, @NotNull Gson gson) {
        return gson.toJsonTree(src.toString());
    }
    private static @Contract("_, null, _, _, _ -> null") String replaceVariables(
        @NotNull final MutableText text,
        @Nullable final String segment,
        @NotNull final ServerCommandSource source,
        @NotNull final ChatRooms chatRoom,
        @NotNull final Text message
    ) {
        String out = segment;
        if (out != null) {
            // If description contains
            Pattern pattern = Pattern.compile("\\$\\{([A-Za-z.]+)([\\^_]{0,2})}");
            Matcher matcher = pattern.matcher(segment);
            int end = 0;
            
            while (matcher.find()) {
                String key = matcher.group(1);
                ChatFunction function = ChatVariables.get(key.toLowerCase());
                if (function != null) {
                    if (!function.canBeParsed(source))
                        continue;
                    
                    // Change val casing
                    String mod = matcher.group(2);
                    CasingUtils.Casing casing = switch (mod) {
                        case "__" -> CasingUtils.Casing.LOWER;
                        case "^^" -> CasingUtils.Casing.UPPER;
                        case "^" -> CasingUtils.Casing.WORDS;
                        default -> CasingUtils.Casing.DEFAULT;
                    };
                    
                    // Prefixed text
                    String pre = segment.substring(end, matcher.start());
                    if (end == 0) out = pre; else text.append(pre);
                    
                    // Add
                    text.append(function.parseVar(source, chatRoom, message, casing));
                    
                    // Suffixed text
                    end = matcher.end();
                }
            }
            
            // Append anything trailing the text
            if (end != 0)
                text.append(segment.substring(end));
        }
        return out;
    }
}
