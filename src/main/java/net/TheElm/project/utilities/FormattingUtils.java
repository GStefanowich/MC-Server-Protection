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

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormattingUtils {
    
    private static final Pattern regex = Pattern.compile("(&[a-fk-or0-9])");
    
    private FormattingUtils() {}
    
    public static @Nullable Text stringToText(@Nullable String raw) {
        if ((raw == null) || raw.isEmpty())
            return null;
        
        MutableText text = null;
        Formatting[] formattings = null;
        
        for (String segment : stringToColorSegments( raw )) {
            if ( segment.matches(regex.pattern()+'+') ) {
                formattings = codeGroupToFormat(segment);
                continue;
            }
            
            MutableText inside = new LiteralText(segment);
            if (formattings != null) {
                inside.formatted(formattings);
                formattings = null;
            }
            
            if (text == null)
                text = inside;
            else
                text.append(inside);
        }
        
        return text;
    }
    
    private static String[] stringToColorSegments(@NotNull String raw) {
        List<String> segments = new ArrayList<>();
        Matcher matches = Pattern.compile(regex.pattern() + '+').matcher(raw);
        
        int cursor = 0, end = 0;
        while (matches.find()) {
            cursor = matches.start();
            if (cursor > end)
                segments.add(raw.substring(end, cursor));
            end = matches.end();
            if (cursor != end)
                segments.add(raw.substring(cursor, end));
        }
        if (end != raw.length())
            segments.add(raw.substring(end));
        
        return segments.toArray(new String[0]);
    }
    
    private static Formatting[] codeGroupToFormat(String codes) {
        List<Formatting> formatting = new ArrayList<>();
        Matcher matches = regex.matcher(codes);
        while (matches.find()) {
            formatting.add(codeToFormat(codes.substring(matches.start(), matches.end()).toLowerCase()));
        }
        return formatting.toArray(new Formatting[0]);
    }
    private static @Nullable Formatting codeToFormat(String code) {
        if (code.length() != 2) return null;
        switch (code) {
            case "&0":
                return Formatting.BLACK;
            case "&1":
                return Formatting.DARK_BLUE;
            case "&2":
                return Formatting.DARK_GREEN;
            case "&3":
                return Formatting.DARK_AQUA;
            case "&4":
                return Formatting.DARK_RED;
            case "&5":
                return Formatting.DARK_PURPLE;
            case "&6":
                return Formatting.GOLD;
            case "&7":
                return Formatting.GRAY;
            case "&8":
                return Formatting.DARK_GRAY;
            case "&9":
                return Formatting.BLUE;
            case "&a":
                return Formatting.GREEN;
            case "&b":
                return Formatting.AQUA;
            case "&c":
                return Formatting.RED;
            case "&d":
                return Formatting.LIGHT_PURPLE;
            case "&e":
                return Formatting.YELLOW;
            case "&f":
                return Formatting.WHITE;
            case "&k":
                return Formatting.OBFUSCATED;
            case "&l":
                return Formatting.BOLD;
            case "&m":
                return Formatting.STRIKETHROUGH;
            case "&n":
                return Formatting.UNDERLINE;
            case "&o":
                return Formatting.ITALIC;
            case "&r":
                return Formatting.RESET;
        }
        return null;
    }
    
    public static String number(Number number) {
        return NumberFormat.getInstance().format( number );
    }
    
}
