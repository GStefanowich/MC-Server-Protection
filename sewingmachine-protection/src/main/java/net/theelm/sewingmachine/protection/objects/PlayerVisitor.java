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

package net.theelm.sewingmachine.protection.objects;

import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.protection.claims.Claimant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created on Jan 04 2023 at 10:25 AM.
 * By greg in SewingMachineMod
 */
public final class PlayerVisitor {
    private final @NotNull ServerPlayerEntity player;
    private final @Nullable UUID location;
    private final @NotNull Set<@NotNull Claimant> locations = Collections.synchronizedSet(new HashSet<>());
    
    public PlayerVisitor(@NotNull ServerPlayerEntity player) {
        this.player = player;
        this.location = null;
    }
    public PlayerVisitor(@NotNull ServerPlayerEntity player, @NotNull UUID uuid) {
        this.player = player;
        this.location = uuid;
    }
    
    public @Nullable UUID get() {
        return this.location;
    }
    
    public void player(@NotNull Consumer<ServerPlayerEntity> consumer) {
        consumer.accept(this.player);
    }
    
    public boolean addLocation(@NotNull Claimant claimant) {
        return this.locations.add(claimant);
    }
    
    public void exit() {
        for (Claimant claimant : this.locations)
            claimant.removeVisitor(this);
    }
}
