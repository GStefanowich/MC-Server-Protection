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
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.protections.claiming.ClaimantPlayer;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
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
    public static @NotNull Text formatPlayerMessage(ServerPlayerEntity player, ChatRooms chatRoom, String raw) {
        return ChatRoomUtilities.formatPlayerMessage(player, chatRoom, Text.literal(raw));
    }
    public static @NotNull Text formatPlayerMessage(ServerPlayerEntity player, ChatRooms chatRoom, Text text) {
        return ChatRoomUtilities.formatPlayerMessage(player.getCommandSource(), chatRoom, text);
    }
    public static @NotNull Text formatPlayerMessage(@NotNull ServerCommandSource source, ChatRooms chatRoom, @NotNull Text text) {
        try {
            ChatFormat format = chatRoom.getFormat();
            return format.format(source, chatRoom, text);
        } catch (StackOverflowError e) {
            CoreMod.logError(e);
        }
        return TextUtils.literal();
    }
    
    // General send
    public static void sendTo(@NotNull ChatRooms chatRoom, @NotNull ServerPlayerEntity player, @NotNull Text chatText) {
        ChatRoomUtilities.sendTo(chatRoom, player, Collections.emptyList(), chatText);
    }
    public static boolean sendTo(@NotNull ChatRooms chatRoom, @NotNull ServerPlayerEntity player, @NotNull Collection<ServerPlayerEntity> tags, @NotNull Text chatText) {
        switch (chatRoom) {
            // Local message
            case LOCAL: {
                return MessageUtils.sendToLocal(player.getWorld(), player.getBlockPos(), tags, chatText);
            }
            // Global message
            case GLOBAL: {
                MessageUtils.sendToAll(chatText, tags);
                return true;
            }
            // Message to the players town
            case TOWN: {
                ClaimantPlayer claimantPlayer = ((PlayerData) player).getClaim();
                return MessageUtils.sendToTown(claimantPlayer.getTown(), tags, chatText);
            }
        }
        return false;
    }
}
