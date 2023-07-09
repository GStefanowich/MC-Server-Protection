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

import com.mojang.datafixers.util.Either;
import net.theelm.sewingmachine.interfaces.TextModifier;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.utilities.text.VariableTextContent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormattingUtils {
    
    private static final @NotNull Pattern REGEX = Pattern.compile("([&§]([a-fk-or0-9]|#[a-fA-F0-9]{6}|#[a-fA-F0-9]{3}))");
    
    private FormattingUtils() {}
    
    public static @Nullable Text stringToText(@Nullable String raw) {
        if ((raw == null) || raw.isEmpty())
            return null;
        
        StyleApplicator applicator = new StyleApplicator();
        MutableText text = null;
        
        for (String segment : stringToColorSegments(raw)) {
            //System.out.println("Segment: '" + segment + "'");
            if ( segment.matches(FormattingUtils.REGEX.pattern() + '+') ) {
                applicator = FormattingUtils.codeGroupApplicator(applicator, segment);
                continue;
            }
            
            MutableText inside = MutableText.of(new VariableTextContent(segment));
            if (!applicator.isEmpty())
                inside.styled(applicator);
            
            // Create the main text if it is null
            if (text == null)
                text = TextUtils.literal();
            
            // Append the next section to the main text
            text.append(inside);
        }
        
        return text;
    }
    
    public static @NotNull MutableText deepCopy(@NotNull final Text text) {
        return text.copy();
    }
    
    private static @NotNull String[] stringToColorSegments(@NotNull String raw) {
        List<String> segments = new ArrayList<>();
        Matcher matches = Pattern.compile(REGEX.pattern() + '+').matcher(raw);
        
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
    
    private static @NotNull StyleApplicator codeGroupApplicator(@NotNull StyleApplicator applicator, @NotNull String codes) {
        Matcher matches = FormattingUtils.REGEX.matcher(codes);
        while (matches.find()) {
            @Nullable Either<Formatting, TextColor> either = FormattingUtils.codeForApplicator(codes.substring(matches.start(), matches.end()).toLowerCase());
            if (either != null) {
                // Apply either the formatting or the text color to the StyleApplicator
                either.ifLeft(applicator::withStyle)
                    .ifRight(applicator::withStyle);
            }
        }
        return applicator;
    }
    private static @Nullable Either<Formatting, TextColor> codeForApplicator(@NotNull String code) {
        int len = code.length();
        if (!(len == 2 || len == 5 || len == 8))
            return null;
        return switch (code) {
            case "&0", "§0",
                 "&1", "§1",
                 "&2", "§2",
                 "&3", "§3",
                 "&4", "§4",
                 "&5", "§5",
                 "&6", "§6",
                 "&7", "§7",
                 "&8", "§8",
                 "&9", "§9",
                 "&a", "§a",
                 "&b", "§b",
                 "&c", "§c",
                 "&d", "§d",
                 "&e", "§e",
                 "&f", "§f" -> Either.right(FormattingUtils.codeToTextColor(code));
            case "&k", "§k",
                 "&l", "§l",
                 "&m", "§m",
                 "&n", "§n",
                 "&o", "§o",
                 "&r", "§r" -> Either.left(FormattingUtils.codeToFormat(code));
            default -> Either.right(FormattingUtils.hexToFormat(code));
        };
    }
    private static @Nullable TextColor codeToTextColor(@NotNull String code) {
        Formatting formatting = switch(code) {
            case "&0", "§0" -> Formatting.BLACK;
            case "&1", "§1" -> Formatting.DARK_BLUE;
            case "&2", "§2" -> Formatting.DARK_GREEN;
            case "&3", "§3" -> Formatting.DARK_AQUA;
            case "&4", "§4" -> Formatting.DARK_RED;
            case "&5", "§5" -> Formatting.DARK_PURPLE;
            case "&6", "§6" -> Formatting.GOLD;
            case "&7", "§7" -> Formatting.GRAY;
            case "&8", "§8" -> Formatting.DARK_GRAY;
            case "&9", "§9" -> Formatting.BLUE;
            case "&a", "§a" -> Formatting.GREEN;
            case "&b", "§b" -> Formatting.AQUA;
            case "&c", "§c" -> Formatting.RED;
            case "&d", "§d" -> Formatting.LIGHT_PURPLE;
            case "&e", "§e" -> Formatting.YELLOW;
            case "&f", "§f" -> Formatting.WHITE;
            default -> null;
        };
        return formatting == null ? null : ColorUtils.getNearestTextColor(formatting);
    }
    private static @Nullable Formatting codeToFormat(@NotNull String code) {
        return switch(code) {
            case "&k", "§k" -> Formatting.OBFUSCATED;
            case "&l", "§l" -> Formatting.BOLD;
            case "&m", "§m" -> Formatting.STRIKETHROUGH;
            case "&n", "§n" -> Formatting.UNDERLINE;
            case "&o", "§o" -> Formatting.ITALIC;
            case "&r", "§r" -> Formatting.RESET;
            default -> null;
        };
    }
    private static @Nullable TextColor hexToFormat(@NotNull String hex) {
        int len = hex.length();
        if (len == 5 || len == 8)
            return ColorUtils.getRawTextColor(hex.substring(1));
        return null;
    }
    
    public static @Contract("!null, _ -> !null") Text visitVariables(@NotNull String msg, @NotNull TextModifier modifier) {
        return FormattingUtils.visitVariables(FormattingUtils.stringToText(msg), modifier);
    }
    public static @Contract("!null, _ -> !null") Text visitVariables(@Nullable Text main, @NotNull TextModifier modifier) {
        // If text is null, don't do any parsing
        if (main == null)
            return null;
        
        MutableText updated = null;
        
        // Iterate the siblings looking for replaceable text
        for (Text text : main.getSiblings()) {
            Text update;
            if (text.getContent() instanceof VariableTextContent variable)
                update = variable.execute(TextUtils.mutable(text), modifier);
            else {
                System.out.println(text.getClass());
                update = text;
            }
            
            if (updated == null)
                updated = TextUtils.mutable(update);
            else updated.append(update);
        }
        
        return updated == null ? main : updated;
    }
    
    public static @NotNull String format(@NotNull Number number) {
        return NumberFormat.getInstance()
            .format(number);
    }
    public static @NotNull String format(int number) {
        return NumberFormat.getNumberInstance()
            .format(number);
    }
    public static @NotNull String format(long number) {
        return NumberFormat.getNumberInstance()
            .format(number);
    }
    public static @NotNull String format(float number) {
        return NumberFormat.getNumberInstance()
            .format(number);
    }
}
