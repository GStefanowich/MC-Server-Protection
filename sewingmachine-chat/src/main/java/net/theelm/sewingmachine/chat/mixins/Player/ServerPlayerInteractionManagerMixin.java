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

package net.theelm.sewingmachine.chat.mixins.Player;


import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.theelm.sewingmachine.chat.enums.ChatRooms;
import net.theelm.sewingmachine.chat.interfaces.PlayerChat;
import net.theelm.sewingmachine.enums.Permissions;
import net.theelm.sewingmachine.events.CommandPermissionCallback;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.UUID;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin implements PlayerChat {
    @Shadow @Final protected ServerPlayerEntity player;
    
    /*
     * Chat Handlers
     */
    private ChatRooms chatRoom = ChatRooms.GLOBAL;
    private HashSet<UUID> mutedPlayers = new HashSet<>();
    private boolean isGlobalMuted = false;
    
    @Override @NotNull
    public ChatRooms getChatRoom() {
        return this.chatRoom;
    }
    @Override
    public void setChatRoom(@NotNull ChatRooms room) {
        // Set the chat room
        this.chatRoom = room;
    }
    
    @Override
    public boolean toggleMute() {
        return this.toggleMute(!this.isGlobalMuted);
    }
    @Override
    public boolean toggleMute(boolean muted) {
        return (this.isGlobalMuted = muted);
    }
    @Override
    public boolean toggleMute(GameProfile player) {
        UUID uuid = player.getId();
        if (!this.mutedPlayers.remove(uuid))
            return this.mutedPlayers.add(uuid);
        else return false;
    }
    @Override
    public boolean isMuted() {
        return this.isGlobalMuted && (!CommandPermissionCallback.EVENT.invoker().hasPermission(this.player, Permissions.CHAT_COMMAND_MUTE_EXEMPT));
    }
    @Override
    public boolean isMuted(@NotNull GameProfile player) {
        return this.mutedPlayers.contains(player.getId());
    }

}
