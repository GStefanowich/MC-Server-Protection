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

package net.theelm.sewingmachine.base.mixins.Commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.Permissions;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(BanCommand.class)
public class BanCommandMixin {
    private static final SimpleCommandExceptionType BAN_EXEMPT = new SimpleCommandExceptionType(Text.literal("That player cannot be banned."));
    
    /**
     * @author TheElm
     * @reason Added use of permission node to command
     * @param dispatcher Command Dispatcher
     */
    @Overwrite
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ban")
            .requires(CommandPredicate.opLevel(OpLevels.KICK_BAN_OP).or(Permissions.VANILLA_COMMAND_BAN))
            .then(CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("reason", MessageArgumentType.message())
                    .executes((context) -> BanCommandMixin.ban(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), MessageArgumentType.getMessage(context, "reason")))
                )
                .executes((context) -> BanCommandMixin.ban(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), null))
            )
        );
    }
    
    @Shadow
    private static int ban(@NotNull ServerCommandSource serverCommandSource, Collection<GameProfile> collection, @Nullable Text text) {
        return Command.SINGLE_SUCCESS;
    }
}
