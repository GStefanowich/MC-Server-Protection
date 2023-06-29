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

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.enums.OpLevels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Created on Jun 27 2023 at 4:29 PM.
 * By greg in sewingmachine
 */
@FunctionalInterface
public interface PlayerTeleportCallback {
    Event<PlayerTeleportCallback> TEST = EventFactory.createArrayBacked(PlayerTeleportCallback.class, (listeners) -> (server, player, target) -> {
        for (PlayerTeleportCallback callback : listeners) {
            Boolean allowed = callback.canTeleportTo(server, player, target);
            if (allowed != null)
                return allowed;
        }
        
        // Allow teleporting if the player is an OP
        return player.hasPermissionLevel(OpLevels.CHEATING);
    });
    
    @Nullable Boolean canTeleportTo(@NotNull MinecraftServer server, @NotNull PlayerEntity player, @Nullable UUID uuid);
    
    static boolean canTeleport(@NotNull MinecraftServer server, @NotNull PlayerEntity player) {
        return PlayerTeleportCallback.TEST.invoker()
            .canTeleportTo(server, player, null) == Boolean.TRUE;
    }
    static boolean canTeleport(@NotNull MinecraftServer server, @NotNull PlayerEntity player, @Nullable UUID target) {
        return PlayerTeleportCallback.TEST.invoker()
            .canTeleportTo(server, player, target) == Boolean.TRUE;
    }
    static boolean canTeleport(@NotNull MinecraftServer server, @NotNull UUID player, @Nullable UUID target) {
        PlayerManager manager = server.getPlayerManager();
        ServerPlayerEntity entity = manager.getPlayer(player);
        return entity != null
            && PlayerTeleportCallback.canTeleport(server, entity, target);
    }
}
