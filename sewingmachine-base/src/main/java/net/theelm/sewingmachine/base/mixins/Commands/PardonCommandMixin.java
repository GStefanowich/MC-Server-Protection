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
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.Permissions;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.PardonCommand;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(PardonCommand.class)
public class PardonCommandMixin {
    
    /**
     * @author TheElm
     * @reason Added use of permission node to command
     * @param dispatcher Command Dispatcher
     */
    @Overwrite
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("pardon")
            .requires(CommandPredicate.opLevel(OpLevels.KICK_BAN_OP).or(Permissions.VANILLA_COMMAND_UNBAN))
            .then(CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerManager().getUserBanList().getNames(), suggestionsBuilder))
                .executes((context) -> PardonCommandMixin.pardon(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets")))
            )
        );
    }
    
    @Shadow
    private static int pardon(@NotNull ServerCommandSource serverCommandSource, Collection<GameProfile> collection) {
        return Command.SINGLE_SUCCESS;
    }
    
}
