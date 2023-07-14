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

package net.theelm.sewingmachine.chat;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.chat.commands.ChatroomCommands;
import net.theelm.sewingmachine.chat.commands.NickNameCommand;
import net.theelm.sewingmachine.chat.commands.TagUserCommand;
import net.theelm.sewingmachine.chat.enums.ChatRooms;
import net.theelm.sewingmachine.chat.interfaces.ChatMessageFunction;
import net.theelm.sewingmachine.chat.interfaces.PlayerChat;
import net.theelm.sewingmachine.chat.utilities.PlayerNameUtils;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.events.MessageDeployer;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.objects.MessageRegion;
import net.theelm.sewingmachine.utilities.EntityVariables;
import net.theelm.sewingmachine.utilities.mod.Sew;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * Created on Jul 17 2022 at 11:01 AM.
 * By greg in SewingMachineMod
 */
public final class ServerCore implements ModInitializer, SewPlugin {
    public static final RegistryKey<MessageType> CUSTOM_FORMATTING = RegistryKey.of(RegistryKeys.MESSAGE_TYPE, Sew.modIdentifier("chat_format"));
    
    @Override
    public void onInitialize() {
        PlayerNameCallback.INSTANCE.register(new PlayerNameCallback() {
            @Override
            public @Nullable Text getDisplayName(PlayerEntity player) {
                return PlayerNameUtils.getPlayerDisplayName((ServerPlayerEntity) player);
            }
            
            @Override
            public @NotNull Text getDisplayName(@NotNull MinecraftServer server, @NotNull UUID uuid) {
                return PlayerNameUtils.fetchPlayerNick(server, uuid);
            }
        });
        
        MessageRegion local = new LocalMessageRegion();
        MessageRegion global = new GlobalMessageRegion();
        
        // Register for what happens when messages are sent
        MessageDeployer.EMIT.register((player, target, tags, message) -> {
            MessageRegion room = ((PlayerChat) player).getChatRoom();
            if (room == null)
                room = global;
            return room.enabled(player) && room.broadcast(player, target, tags, message);
        });
        
        // Register our message regions
        MessageRegion.add(local);
        MessageRegion.add(global);
        
        // Get the nickname (Or regular name)
        EntityVariables.add("nick", (source, room, message, casing) -> {
            if (source.getEntity() instanceof ServerPlayerEntity player)
                return PlayerNameUtils.getPlayerDisplayName(player);
            return source.getDisplayName();
        });
        
        // Chat room
        EntityVariables.add("chat", (ChatMessageFunction)(room, message, casing) -> room instanceof ChatRooms rooms ? TextUtils.literal(rooms.name(), casing) : TextUtils.literal());
        
        // Get the chat message
        EntityVariables.add("message", (ChatMessageFunction)(room, message, casing) -> message);
    }
    
    @Override
    public @NotNull SewCommand[] getCommands() {
        return new SewCommand[] {
            //new ChatroomCommands(),
            new NickNameCommand(),
            //new TagUserCommand()
        };
    }
    
    private final class LocalMessageRegion extends MessageRegion {
        protected LocalMessageRegion() {
            super("local");
        }
        
        @Override
        public boolean broadcast(@NotNull ServerPlayerEntity player, @Nullable ServerPlayerEntity target, @NotNull Collection<ServerPlayerEntity> tags, @NotNull SignedMessage message) {
            //return MessageUtils.sendToLocal(player.getWorld(), player.getBlockPos(), tags, message);
            return false;
        }
    }
    private final class GlobalMessageRegion extends MessageRegion {
        protected GlobalMessageRegion() {
            super("global");
        }
        
        @Override
        public boolean broadcast(@NotNull ServerPlayerEntity player, @Nullable ServerPlayerEntity target, @NotNull Collection<ServerPlayerEntity> tags, @NotNull SignedMessage message) {
            //return MessageUtils.sendToAll(message, tags);
            return false;
        }
    }
}
