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

package net.TheElm.project.objects;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Tickable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Created on Aug 20 2021 at 2:05 AM.
 * By greg in SewingMachineMod
 */
public final class DetachedTickable implements Tickable {
    private final @NotNull ServerWorld world;
    private final @NotNull Predicate<DetachedTickable> predicate;
    private int ticks = 0;
    private boolean removed = false;
    
    public DetachedTickable(@NotNull ServerWorld world, @NotNull Predicate<DetachedTickable> predicate) {
        this.world = world;
        this.predicate = predicate;
    }
    
    @Override
    public void tick() {
        // Run the tickable and see if we should remove it
        if (!this.removed)
            this.removed = this.predicate.test(this);
        this.ticks++;
    }
    
    public @NotNull ServerWorld getWorld() {
        return this.world;
    }
    public int getTicks() {
        return this.ticks;
    }
    
    public boolean isRemoved() {
        return this.removed;
    }
}
