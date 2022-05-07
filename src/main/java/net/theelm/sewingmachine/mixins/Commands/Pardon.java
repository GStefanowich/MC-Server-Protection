package net.theelm.sewingmachine.mixins.Commands;

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
public class Pardon {
    
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
                .executes((context) -> Pardon.pardon(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets")))
            )
        );
    }
    
    @Shadow
    private static int pardon(@NotNull ServerCommandSource serverCommandSource, Collection<GameProfile> collection) {
        return Command.SINGLE_SUCCESS;
    }
    
}
