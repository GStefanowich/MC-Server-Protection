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

package net.theelm.sewingmachine.objects;

import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for chat rooms
 */
public abstract class MessageRegion {
    private static final @NotNull Map<String, MessageRegion> REGISTRY = new ConcurrentHashMap<>();
    
    public static void add(@NotNull MessageRegion region) {
        MessageRegion.REGISTRY.put(region.name(), region);
    }
    
    public static @NotNull Set<String> names() {
        return MessageRegion.REGISTRY.keySet();
    }
    public static @NotNull Collection<MessageRegion> get() {
        return MessageRegion.REGISTRY.values();
    }
    public static @Nullable MessageRegion get(@Nullable String name) {
        return name == null ? null : MessageRegion.REGISTRY.get(name);
    }
    
    private @NotNull String name;
    
    protected MessageRegion(@NotNull String name) {
        this.name = name.toLowerCase();
    }
    
    public final @NotNull String name() {
        return this.name;
    }
    
    public @NotNull String getFormat() {
        return ""; // TODO: Make abstract
    }
    
    /**
     * If a player can access the MessageRegion
     * @param source
     * @return
     */
    public boolean enabled(@NotNull ServerCommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity player
            && this.enabled(player);
    }
    
    /**
     * If a player can access the MessageRegion
     * @param player
     * @return
     */
    public boolean enabled(@NotNull ServerPlayerEntity player) {
        return true;
    }
    
    /**
     * Send a message in the MessageRegion
     * @param player
     * @param target
     * @param tags
     * @param message
     */
    public abstract boolean broadcast(@NotNull ServerPlayerEntity player, @Nullable ServerPlayerEntity target, @NotNull Collection<ServerPlayerEntity> tags, @NotNull SignedMessage message);
}