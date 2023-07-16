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

package net.theelm.sewingmachine.commands.abstraction;

import com.mojang.brigadier.Command;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A dynamic command registration handling
 */
public interface AbstractSewCommand<T> {
    void register(@NotNull T container, @NotNull CommandRegistryAccess registry);
    
    default int playerSendsMessageAndData(@NotNull ServerCommandSource source, @NotNull String main) {
        return this.playerSendsMessageAndData(source, null, main);
    }
    default int playerSendsMessageAndData(@NotNull ServerCommandSource source, @Nullable SignedMessage message, @NotNull String main) {
        return this.playerSendsMessageAndData(source, message, Text.literal(main));
    }
    default int playerSendsMessageAndData(@NotNull ServerCommandSource source, @Nullable SignedMessage message, @NotNull Text main) {
        MinecraftServer server = source.getServer();
        PlayerManager playerManager = server.getPlayerManager();

        playerManager.broadcast(message, source, MessageType.params(MessageType.EMOTE_COMMAND, source));

        return Command.SINGLE_SUCCESS;
    }
}
