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
import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.permissions.PermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Jun 09 2023 at 12:49 AM.
 * By greg in sewingmachine
 */
@FunctionalInterface
public interface CommandPermissionCallback {
    Event<CommandPermissionCallback> EVENT = EventFactory.createArrayBacked(CommandPermissionCallback.class,
        // If we have no registered permission checks, simply use the OP_LEVEL
        ((player, scope) -> false),
        // Run through the permission handlers and see if it is allowed
        callbacks -> ((player, scope) -> {
            for (CommandPermissionCallback callback : callbacks)
                if (!callback.hasPermission(player, scope))
                    return false;
            return true;
        })
    );
    
    /**
     * Test if the CommandSource has a permission level
     * @param player The Command Source attempting to run the command
     * @param scope The scope being tested
     * @return if the permission passes
     */
    boolean hasPermission(@NotNull ServerPlayerEntity player, @NotNull String scope);
    
    /**
     * Test if the CommandSource has a permission level
     * @param player The Command Source attempting to run the command
     * @param node The scope being tested
     * @return if the permission passes
     */
    default boolean hasPermission(@NotNull ServerPlayerEntity player, @NotNull PermissionNode node) {
        return this.hasPermission(player, node.getNode());
    }
}
