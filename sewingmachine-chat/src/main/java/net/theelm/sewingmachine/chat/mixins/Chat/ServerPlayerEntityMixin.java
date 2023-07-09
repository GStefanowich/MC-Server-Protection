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

package net.theelm.sewingmachine.chat.mixins.Chat;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.chat.enums.ChatRooms;
import net.theelm.sewingmachine.chat.interfaces.Nicknamable;
import net.theelm.sewingmachine.chat.interfaces.PlayerChat;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created on Jun 09 2023 at 3:39 AM.
 * By greg in sewingmachine
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements PlayerChat {
    /*
     * Chat Rooms (Handled cross dimension)
     */
    
    @Shadow @Final public ServerPlayerInteractionManager interactionManager;
    
    @Override
    public @NotNull ChatRooms getChatRoom() {
        return ((PlayerChat)this.interactionManager).getChatRoom();
    }
    @Override
    public void setChatRoom(@NotNull ChatRooms room) {
        ((PlayerChat)this.interactionManager).setChatRoom( room );
    }
    
    @Override
    public boolean toggleMute() {
        return ((PlayerChat)this.interactionManager).toggleMute();
    }
    @Override
    public boolean toggleMute(boolean muted) {
        return ((PlayerChat)this.interactionManager).toggleMute(muted);
    }
    @Override
    public boolean toggleMute(GameProfile player) {
        return ((PlayerChat)this.interactionManager).toggleMute( player );
    }
    @Override
    public boolean isMuted() {
        return ((PlayerChat)this.interactionManager).isMuted();
    }
    @Override
    public boolean isMuted(GameProfile player) {
        return ((PlayerChat)this.interactionManager).isMuted( player );
    }
    
    @Inject(at = @At("HEAD"), method = "getPlayerListName", cancellable = true)
    public void getServerlistDisplayName(@NotNull CallbackInfoReturnable<Text> callback) {
        Text nickname = ((Nicknamable) this).getPlayerNickname();
        if (nickname != null)
            callback.setReturnValue(nickname);
    }
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(@NotNull NbtCompound tag, CallbackInfo callback) {
        Text nickname = ((Nicknamable)this).getPlayerNickname();
        
        // Store the players nickname
        if ( nickname != null )
            tag.putString("PlayerNickname", Text.Serializer.toJson(nickname));
        
        tag.putBoolean("chatMuted", this.isMuted());
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(@NotNull NbtCompound tag, CallbackInfo callback) {
        // Get the nickname
        if (tag.contains("PlayerNickname", NbtElement.STRING_TYPE))
            ((Nicknamable) this).setPlayerNickname(Text.Serializer.fromJson(tag.getString("PlayerNickname")));
        
        // Read if player is muted
        if (tag.contains("chatMuted", NbtElement.BYTE_TYPE))
            this.toggleMute(tag.getBoolean("chatMuted"));
    }
    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void onCopyData(@NotNull ServerPlayerEntity player, boolean alive, CallbackInfo callback) {
        // Copy the players nick over
        ((Nicknamable) this).setPlayerNickname(
            ((Nicknamable) player).getPlayerNickname()
        );
        
        // Keep the chat room cross-dimension
        this.setChatRoom(((PlayerChat) player).getChatRoom());
    }
}
