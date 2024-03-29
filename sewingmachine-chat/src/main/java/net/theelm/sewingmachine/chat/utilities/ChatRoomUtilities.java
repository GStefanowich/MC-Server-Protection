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

package net.theelm.sewingmachine.chat.utilities;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.chat.interfaces.PlayerChat;
import net.theelm.sewingmachine.chat.objects.ChatFormat;
import net.theelm.sewingmachine.chat.enums.ChatRooms;
import net.theelm.sewingmachine.events.MessageDeployer;
import net.theelm.sewingmachine.objects.MessageRegion;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Created on Jun 08 2023 at 4:20 AM.
 * By greg in sewingmachine
 */
public final class ChatRoomUtilities {
    private ChatRoomUtilities() {}
    
    // Format a message to chat from a player
    public static @NotNull Text formatPlayerMessage(ServerPlayerEntity player, String raw) {
        return ChatRoomUtilities.formatPlayerMessage(player, Text.literal(raw));
    }
    public static @NotNull Text formatPlayerMessage(ServerPlayerEntity player, Text text) {
        return ChatRoomUtilities.formatPlayerMessage(player, ((PlayerChat)player).getChatRoom(), text);
    }
    public static @NotNull Text formatPlayerMessage(ServerPlayerEntity player, MessageRegion room, String raw) {
        return ChatRoomUtilities.formatPlayerMessage(player, room, Text.literal(raw));
    }
    public static @NotNull Text formatPlayerMessage(ServerPlayerEntity player, MessageRegion room, Text text) {
        return ChatRoomUtilities.formatPlayerMessage(player.getCommandSource(), room, text);
    }
    public static @NotNull Text formatPlayerMessage(@NotNull ServerCommandSource source, MessageRegion room, @NotNull Text text) {
        /*ChatFormat format = room.getFormat();
        return format.format(source, room, text);*/
        return Text.literal("");
    }
    
    // General send
    public static void sendTo(@NotNull ChatRooms room, @NotNull ServerPlayerEntity player, @NotNull Text message) {
        ChatRoomUtilities.sendTo(room, player, Collections.emptyList(), message);
    }
    public static void sendTo(@NotNull ChatRooms room, @NotNull ServerPlayerEntity player, @NotNull Collection<ServerPlayerEntity> tags, @NotNull Text message) {
        /*MessageDeployer.EMIT.invoker()
            .sendMessage(room, player, tags, message);*/
    }
}
