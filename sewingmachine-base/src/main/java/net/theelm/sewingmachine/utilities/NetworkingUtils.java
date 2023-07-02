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

package net.theelm.sewingmachine.utilities;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.base.packets.interfaces.ClientPlayHandler;
import net.theelm.sewingmachine.base.packets.interfaces.ServerPlayHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Jul 01 2023 at 1:04 AM.
 * By greg in sewingmachine
 */
public final class NetworkingUtils {
    private NetworkingUtils() {}
    
    public static <T extends FabricPacket> void send(@NotNull MinecraftClient client, @NotNull T packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        
        NetworkingUtils.send(client,
            packet.getType()
                .getId(),
            buf
        );
    }
    
    public static void send(@NotNull MinecraftClient client, @NotNull Identifier identifier) {
        NetworkingUtils.send(client, identifier, PacketByteBufs.empty());
    }
    
    public static void send(@NotNull MinecraftClient client, @NotNull Identifier identifier, @NotNull PacketByteBuf buf) {
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null)
            return;
        networkHandler.sendPacket(ClientPlayNetworking.createC2SPacket(identifier, buf));
    }
    
    public static <T extends FabricPacket> void clientReceiver(@NotNull PacketType<T> type, @NotNull ClientPlayHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type.getId(), (client, network, buf, sender) -> {
            T packet = type.read(buf);
            
            if (client.isOnThread()) {
                // Do not submit to the render thread if we're already running there.
                // Normally, packets are handled on the network IO thread - though it is
                // not guaranteed (for example, with 1.19.4 S2C packet bundling)
                // Since we're handling it right now, connection check is redundant.
                handler.receive(client, network, packet, sender);
            } else {
                client.execute(() -> {
                    if (network.getConnection().isOpen())
                        handler.receive(client, network, packet, sender);
                });
            }
        });
    }
    
    public static <T extends FabricPacket> void serverReceiver(@NotNull PacketType<T> type, @NotNull ServerPlayHandler<T> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type.getId(), (server, player, network, buf, sender) -> {
            T packet = type.read(buf);
            
            if (server.isOnThread()) {
                handler.receive(server, player, network, packet, sender);
            } else {
                server.execute(() -> {
                    if (network.isConnectionOpen())
                        handler.receive(server, player, network, packet, sender);
                });
            }
        });
    }
}
