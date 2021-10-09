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

package net.TheElm.project.mixins.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.utilities.CommandUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.StopCommand;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(StopCommand.class)
public class Stop {
    
    /**
     * @author TheElm
     * @reason Stop command can have a reason
     * @param dispatcher Command Dispatcher
     */
    @Overwrite
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("stop")
            .requires(CommandPredicate.opLevel(OpLevels.STOP).and(CommandUtils::isDedicatedServer))
            .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                .executes((context) -> {
                    ServerCommandSource source = context.getSource();
                    MinecraftServer server = source.getMinecraftServer();
                    
                    // Get the reason
                    String reason = StringArgumentType.getString(context, "reason");
                    
                    // Tell the player we're stopping the server
                    source.sendFeedback(new TranslatableText("commands.stop.stopping"), true);
                    
                    // Disconnect all players with a reason
                    Stop.closeServer(
                        source.getMinecraftServer(),
                        new TranslatableText("commands.stop.stopping")
                            .append(": ")
                            .append(reason)
                    );
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
            .executes((context -> {
                ServerCommandSource source = context.getSource();
                
                // Tell the player we're stopping the server
                source.sendFeedback(new TranslatableText("commands.stop.stopping"), true);
                
                // Disconnect all players with a reason
                Stop.closeServer(
                    source.getMinecraftServer(),
                    new TranslatableText("commands.stop.stopping")
                );
                
                return Command.SINGLE_SUCCESS;
            }))
        );
    }
    
    private static void closeServer(@NotNull MinecraftServer server, @Nullable Text reason) {
        // Disconnect all players with a reason
        if (reason != null)
            EntityUtils.kickAllPlayers(reason);
        
        // Stop the server
        server.stop(false);
    }
    
}
