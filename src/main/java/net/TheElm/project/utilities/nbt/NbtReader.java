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

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created on Mar 19 2021 at 3:51 PM.
 * By greg in SewingMachineMod
 */
public interface NbtReader<T> {
    @NotNull NbtReader FINAL = new NbtReader() {};
    
    default @NotNull <X> NbtReader<X> orElse(@NotNull String key, @NotNull NbtGet<X> nbtType, @NotNull Consumer<X> consumer) {
        return (NbtReader<X>) this;
    }
    default @NotNull NbtReader<T> orElse(@NotNull String key) {
        return this;
    }
    
    @SuppressWarnings("unchecked")
    static <T> NbtReader<T> empty() {
        return NbtReader.FINAL;
    }
}
