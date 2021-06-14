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

package net.TheElm.project.utilities.nbt;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created on Mar 19 2021 at 3:44 PM.
 * By greg in SewingMachineMod
 */
public final class NbtReaderContext<T> implements NbtReader<T> {
    private final @NotNull CompoundTag tag;
    private final @NotNull NbtGet<T> nbtType;
    private final @NotNull Consumer<T> consumer;
    
    NbtReaderContext(@Nullable CompoundTag tag, @NotNull NbtGet<T> nbtType, @NotNull Consumer<T> consumer) {
        this.tag = tag;
        this.nbtType = nbtType;
        this.consumer = consumer;
    }
    
    @Override
    public @NotNull <X> NbtReader<X> orElse(@NotNull String key, @NotNull NbtGet<X> nbtType, @NotNull Consumer<X> consumer) {
        return new NbtReaderContext<>(this.tag, nbtType, consumer)
            .run(key);
    }
    
    @Override
    public @NotNull NbtReader<T> orElse(@NotNull String key) {
        return new NbtReaderContext<>(this.tag, this.nbtType, this.consumer)
            .run(key);
    }
    
    @NotNull NbtReader<T> run(@NotNull final String key) {
        if (this.tag == null || !this.tag.contains(key, this.nbtType.getNbtType()))
            return this;
        
        this.consumer.accept(this.nbtType.apply(this.tag, key));
        return NbtReader.empty();
    }
}
