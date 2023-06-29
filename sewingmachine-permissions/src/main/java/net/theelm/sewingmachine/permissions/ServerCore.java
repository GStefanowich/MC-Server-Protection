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

package net.theelm.sewingmachine.permissions;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.events.CommandPermissionCallback;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.permissions.utilities.RankUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Jun 09 2023 at 1:42 AM.
 * By greg in sewingmachine
 */
public final class ServerCore implements DedicatedServerModInitializer, SewPlugin {
    @Override
    public void onInitializeServer() {
        CommandPermissionCallback.EVENT.register((player, scope) -> RankUtils.hasPermission(player, scope));
    }
    
    @Override
    public void updatePrimaryCommand(@NotNull ArgumentBuilder<ServerCommandSource, ?> builder, @NotNull CommandRegistryAccess access) {
        CommandNode<ServerCommandSource> node = CommandUtils.getLiteral("reload", builder);
        if (node != null) {
            node.addChild(CommandManager.literal("permissions")
                .requires(CommandPredicate.isEnabled(SewCoreConfig.HANDLE_PERMISSIONS))
                .executes((context) -> {
                    boolean success = RankUtils.reload();
                    ServerCommandSource source = context.getSource();
                    
                    if (!success)
                        source.sendFeedback(
                            () -> Text.literal("Failed to reload permissions, see console for errors").formatted(Formatting.RED),
                            true
                        );
                    else {
                        RankUtils.clearRanks();
                        CommandUtils.resendTree(source.getServer());
                        
                        source.sendFeedback(
                            () -> Text.literal("Permissions file has been reloaded").formatted(Formatting.GREEN),
                            true
                        );
                    }
                    
                    return success ? Command.SINGLE_SUCCESS : -1;
                })
                .build()
            );
        }
    }
}
