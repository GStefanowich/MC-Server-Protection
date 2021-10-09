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

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created on Mar 19 2021 at 9:43 AM.
 * By greg in SewingMachineMod
 */
public final class NbtGet<T> {
    
    public static final NbtGet<UUID> UUID = new NbtGet<>(NbtType.INT_ARRAY, NbtUtils::getUUID);
    public static final NbtGet<Byte> BYTE = new NbtGet<>(NbtType.BYTE, NbtCompound::getByte);
    public static final NbtGet<Short> SHORT = new NbtGet<>(NbtType.SHORT, NbtCompound::getShort);
    public static final NbtGet<Integer> INT = new NbtGet<>(NbtType.INT, NbtCompound::getInt);
    public static final NbtGet<Long> LONG = new NbtGet<>(NbtType.LONG, NbtCompound::getLong);
    public static final NbtGet<Float> FLOAT = new NbtGet<>(NbtType.FLOAT, NbtCompound::getFloat);
    public static final NbtGet<Double> DOUBLE = new NbtGet<>(NbtType.DOUBLE, NbtCompound::getDouble);
    public static final NbtGet<String> STRING = new NbtGet<>(NbtType.STRING, NbtCompound::getString);
    public static final NbtGet<NbtCompound> COMPOUND = new NbtGet<>(NbtType.COMPOUND, NbtCompound::getCompound);
    //public static final NbtGet<NbtList> LIST = new NbtGet<>(NbtType.LIST, NbtCompound::getList);
    
    private final int nbtType;
    private final CompoundTagGetter<T> applicator;
    
    public NbtGet(int nbtType, @NotNull CompoundTagGetter<T> applicator) {
        this.nbtType = nbtType;
        this.applicator = applicator;
    }
    
    public int getNbtType() {
        return this.nbtType;
    }
    
    public T apply(@NotNull NbtCompound tag, @NotNull String key) {
        return this.applicator.apply(tag, key);
    }
    
    @FunctionalInterface
    public interface CompoundTagGetter<T> {
        T apply(@NotNull NbtCompound tag, @NotNull String key);
    }
}
