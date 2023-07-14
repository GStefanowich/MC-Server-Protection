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
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

@FunctionalInterface
public interface MessageDeployer {
    Event<MessageDeployer> EMIT = EventFactory.createArrayBacked(
        MessageDeployer.class,
        (room, player, tags, message) -> {
            MinecraftServer server = player.getServer();
            PlayerManager playerManager = server.getPlayerManager();
            playerManager.broadcast(message, player, MessageType.params(MessageType.CHAT, player));
            return true;
        },
        (listeners) -> (player, target, tags, message) -> {
            for (MessageDeployer deployer : listeners)
                if (deployer.sendMessage(player, target, tags, message))
                    return true;
            return false;
        }
    );
    
    boolean sendMessage(@NotNull ServerPlayerEntity player, @Nullable ServerPlayerEntity target, @NotNull Collection<ServerPlayerEntity> tags, @NotNull SignedMessage message);
    
    static boolean sendMessage(@NotNull ServerPlayerEntity player, @NotNull SignedMessage message) {
        return MessageDeployer.EMIT.invoker()
            .sendMessage(player, null, Collections.emptyList(), message);
    }
    static boolean sendWhisper(@NotNull ServerPlayerEntity player, @NotNull ServerPlayerEntity target, @NotNull SignedMessage message) {
        return MessageDeployer.EMIT.invoker()
            .sendMessage(player, target, Collections.emptyList(), message);
    }
}
