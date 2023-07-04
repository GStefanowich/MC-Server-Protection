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

package net.theelm.sewingmachine.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ArgumentSuggestions {
    public static @NotNull <S> CompletableFuture<Suggestions> suggestColors(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        return ArgumentSuggestions.suggest(ColorUtils.COLORS.entrySet().stream().map(stringColorEntry -> {
            Text tooltip = Text.literal(stringColorEntry.getKey())
                .styled(new StyleApplicator(ColorUtils.getNearestTextColor(stringColorEntry.getValue())));
            return new AbstractMap.SimpleEntry<>(stringColorEntry.getKey(), tooltip);
        }), builder);
    }
    
    public static @NotNull CompletableFuture<Suggestions> suggest(@NotNull Collection<Map.Entry<String, Text>> suggestions, @NotNull SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        
        for (Map.Entry<String, Text> suggestion : suggestions) {
            String name = suggestion.getKey();
            if (CommandSource.shouldSuggest(remaining, name.toLowerCase(Locale.ROOT)))
                builder.suggest(name);
        }
        
        return builder.buildFuture();
    }
    public static @NotNull CompletableFuture<Suggestions> suggest(@NotNull Stream<Map.Entry<String, Text>> suggestions, @NotNull SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        
        suggestions.filter((suggestion) -> {
            String name = suggestion.getKey();
            return CommandSource.shouldSuggest(remaining, name.toLowerCase(Locale.ROOT));
        }).forEach(pair -> builder.suggest(pair.getKey(), pair.getValue()));
        
        return builder.buildFuture();
    }
}
