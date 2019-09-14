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

package net.TheElm.project.exceptions;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ExceptionTranslatableServerSide implements CommandExceptionType {
    
    private final String translationKey;
    private final int expectedArguments;
    
    public ExceptionTranslatableServerSide(final String key) {
        this( key, 0 );
    }
    public ExceptionTranslatableServerSide(final String key, final int expected) {
        this.translationKey = key;
        this.expectedArguments = expected;
    }
    
    public CommandSyntaxException create(final ServerCommandSource source, final Object... objects) {
        return new CommandSyntaxException(this, TranslatableServerSide.text( source, this.translationKey, objects ));
    }
    public CommandSyntaxException create(final ServerPlayerEntity player, final Object... objects) {
        return new CommandSyntaxException(this, TranslatableServerSide.text( player, this.translationKey, objects ));
    }
    public CommandSyntaxException createWithContext(final ImmutableStringReader reader, final ServerPlayerEntity player, final Object... objects) {
        if (objects.length != this.expectedArguments)
            throw new IllegalArgumentException("Invalid amount of arguments provided for ExceptionTranslatableServerSide");
        return new CommandSyntaxException(this, TranslatableServerSide.text( player, this.translationKey, objects ), reader.getString(), reader.getCursor());
    }
    
}
