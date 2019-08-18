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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EnumArgumentType<E extends Enum<E>> implements ArgumentType<Enum<E>>, Serializable {
    private final EnumSet<E> enumValues;
    
    private EnumArgumentType( final Class<E> enumClass ) {
        this.enumValues = EnumSet.allOf( enumClass );
    }
    
    public static EnumArgumentType getEnumArguments(CommandContext<ServerCommandSource> commandContext, String string) {
        return commandContext.getArgument(string, EnumArgumentType.class);
    }
    
    public static <T extends Enum<T>> EnumArgumentType<T> create( Class<T> enumClass ) {
        return new EnumArgumentType<>( enumClass );
    }
    @Nullable
    public static <T extends Enum<T>> T getEnum(Class<T> tClass, String search) {
        return EnumSet.allOf( tClass ).stream().filter((enumValue) -> {
            return enumValue.name().equalsIgnoreCase( search );
        }).findFirst().orElse( null );
    }
    
    @Override
    public Enum<E> parse(StringReader reader) throws CommandSyntaxException {
        final String check = reader.readUnquotedString();
        Optional<E> findVal = enumValues.stream().filter((enumValue) -> enumValue.name().equals( check )).findFirst();
        return findVal.orElse(null);
    }
    
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder suggestionsBuilder) {
        return CommandSource.suggestMatching(
            enumValues.stream().map((enumValue) -> {
                return enumValue.name().toLowerCase();
            }), suggestionsBuilder
        );
    }
    
}
