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

package net.theelm.sewingmachine.events;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Get the name to use for displaying a Player
 */
@FunctionalInterface
public interface PlayerNameCallback {
    Event<PlayerNameCallback> INSTANCE = EventFactory.createArrayBacked(PlayerNameCallback.class, (listeners) -> (player) -> {
        for (PlayerNameCallback callback : listeners) {
            Text name = callback.getDisplayName(player);
            if (name != null)
                return name;
        }
        
        return player.getDisplayName();
    });
    
    @Nullable Text getDisplayName(PlayerEntity player);
    
    default @NotNull Text getDisplayName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        // Blank UUID is always Spawn/Server
        if (Objects.equals(CoreMod.SPAWN_ID, uuid))
            return Text.literal("Spawn");
        
        PlayerManager manager = server.getPlayerManager();
        ServerPlayerEntity player = manager.getPlayer(uuid);
        if (player != null) {
            Text display = this.getDisplayName(player);
            if (display != null)
                return display;
            
            return player.getName();
        }
        
        UserCache cache = server.getUserCache();
        String name = cache.getByUuid(uuid)
            .map(GameProfile::getName)
            .orElseGet(uuid::toString);
        
        return Text.literal(name);
    }
    
    static @NotNull Text getName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        return PlayerNameCallback.INSTANCE.invoker()
            .getDisplayName(server, uuid);
    }
    
    /**
     * Get the name (or nickname) of the {@code player}
     * @param player The player we want the name of
     * @return The players name (or nickname)
     */
    static @NotNull Text getName(@NotNull PlayerEntity player) {
        Text name = PlayerNameCallback.INSTANCE.invoker()
            .getDisplayName(player);
        return name == null ? player.getName() : name;
    }
    
    /**
     * Get the name of the player without any special color formatting
     * @param server The minecraft server
     * @param uuid The player we want the name of
     * @return The players name
     */
    static @NotNull MutableText getPlainName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        return Text.literal(PlayerNameCallback.getName(server, uuid).getString());
    }
    
    /**
     * Get the name of the {@code player} without any special color formatting
     * @param player The player we want the name of
     * @return The players name
     */
    static @NotNull MutableText getPlainName(@NotNull PlayerEntity player) {
        return Text.literal(PlayerNameCallback.getName(player).getString());
    }
}
