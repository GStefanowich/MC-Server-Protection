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
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.utilities.ColorUtils;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.command.CommandSource;

import java.util.concurrent.CompletableFuture;

public class ArgumentSuggestions {
    
    public static <S> CompletableFuture<Suggestions> suggestNodes(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Permissions.keys(), builder);
    }
    
    public static <S> CompletableFuture<Suggestions> suggestRanks(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(RankUtils.getRanks(), builder);
    }
    
    public static <S> CompletableFuture<Suggestions> suggestColors(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(ColorUtils.getSuggestedNames(), builder);
    }
    
}
