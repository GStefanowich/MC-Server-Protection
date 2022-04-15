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

package net.TheElm.project.protections.claiming;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Created on Apr 14 2022 at 8:37 PM.
 * By greg in SewingMachineMod
 */
public abstract class ClaimCacheEntry<V extends Claimant> {
    private V value;
    private WeakReference<V> reference;
    
    protected ClaimCacheEntry(V value) {
        this.value = null;
        this.reference = new WeakReference<>(value);
    }
    
    public void markDirty() {
        this.value = this.reference.get();
    }
    public void markClean() {
        this.value = null;
    }
    public boolean isDirty() {
        return this.value != null;
    }
    public boolean isRemovable() {
        return !this.isDirty() && this.getValue() == null;
    }
    
    public @Nullable V getValue() {
        if (this.value != null)
            return this.value;
        return this.reference.get();
    }
}
