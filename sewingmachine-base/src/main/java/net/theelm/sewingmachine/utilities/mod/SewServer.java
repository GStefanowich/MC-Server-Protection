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

package net.theelm.sewingmachine.utilities.mod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Created on Jul 07 2023 at 9:41 PM.
 * By greg in sewingmachine
 */
public final class SewServer {
    private SewServer() {}
    
    /**
     * Gets the MinecraftServer
     *   If running as the server, will return the DedicatedMinecraftServer
     *   If running as the client, will return the IntegratedMinecraftServer
     * @return
     */
    public static @NotNull MinecraftServer get() {
        return Sew.getGameInstance()
            .left()
            // Don't method reference otherwise it'll import and check client classes
            .orElseGet(() -> SewClient.getServer());
    }
    
    public static @NotNull MinecraftServer get(@NotNull PlayerEntity player) {
        return Objects.requireNonNull(player.getServer());
    }
    
    public static @Nullable ServerPlayerEntity getPlayer(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        return server.getPlayerManager().getPlayer(uuid);
    }

}
