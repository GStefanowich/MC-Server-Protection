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

package net.TheElm.project.commands.ArgumentTypes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.TheElm.project.interfaces.BoolEnums;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class EnumArgumentType<E extends Enum<E>> implements SuggestionProvider<ServerCommandSource> {
    public static final DynamicCommandExceptionType INVALID_COMPONENT_EXCEPTION = new DynamicCommandExceptionType((obj) -> new TranslatableText("argument.component.invalid", obj));
    
    private final EnumSet<E> enumValues;
    private final Function<E, String> nameFormatter;
    private final Function<E, Text> tooltipFormatter;
    
    private EnumArgumentType(@NotNull final Class<E> enumClass) {
        this(enumClass, null);
    }
    private EnumArgumentType(@NotNull final Class<E> enumClass, @Nullable Function<E, Text> tooltips) {
        this(enumClass, tooltips, (enumValue) -> enumValue.name().toLowerCase().replace('_', '-'));
    }
    private EnumArgumentType(@NotNull final Class<E> enumClass, @Nullable Function<E, Text> tooltips, @NotNull Function<E, String> names) {
        this.enumValues = EnumSet.allOf(enumClass);
        this.nameFormatter = names;
        this.tooltipFormatter = tooltips;
    }
    
    public static <T extends Enum<T>> T getEnum(Class<T> tClass, String search) throws CommandSyntaxException {
        return EnumSet.allOf(tClass).stream().filter((enumValue) -> {
            return enumValue.name().replace('_', '-').equalsIgnoreCase(search);
        }).findFirst().orElseThrow(() -> INVALID_COMPONENT_EXCEPTION.create(search));
    }
    
    @Override
    public @NotNull CompletableFuture<Suggestions> getSuggestions(@NotNull CommandContext<ServerCommandSource> context, @NotNull SuggestionsBuilder builder) {
        final String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        this.enumValues.stream()
            .filter((eVal) -> ((!(eVal instanceof BoolEnums)) || ((BoolEnums)eVal).isEnabled()))
            .filter((eName) -> CommandSource.shouldSuggest(remaining, eName.name().toLowerCase(Locale.ROOT)))
            .forEach((eVal) -> {
                Text text = this.tooltipFormatter != null ? this.tooltipFormatter.apply(eVal) : null;
                builder.suggest(this.nameFormatter.apply(eVal), text);
            });
        return builder.buildFuture();
    }
    public static @NotNull <E extends Enum<E>> SuggestionProvider<ServerCommandSource> enumerate(@NotNull Class<E> claimRanksClass) {
        return new EnumArgumentType<>(claimRanksClass);
    }
    public static @NotNull <E extends Enum<E>> SuggestionProvider<ServerCommandSource> enumerate(@NotNull Class<E> claimRanksClass, @NotNull Function<E, Text> tooltips) {
        return new EnumArgumentType<>(claimRanksClass, tooltips);
    }
    public static @NotNull <E extends Enum<E>> SuggestionProvider<ServerCommandSource> enumerate(@NotNull Class<E> claimRanksClass, @NotNull Function<E, Text> tooltips, @NotNull Function<E, String> names) {
        return new EnumArgumentType<>(claimRanksClass, tooltips, names);
    }
}
