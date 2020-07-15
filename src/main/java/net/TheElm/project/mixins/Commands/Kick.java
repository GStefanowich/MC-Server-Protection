package net.TheElm.project.mixins.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.MessageArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.KickCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(KickCommand.class)
public class Kick {
    
    private static final SimpleCommandExceptionType KICK_EXEMPT = new SimpleCommandExceptionType(new LiteralText("That player cannot be kicked."));
    
    /**
     * @author TheElm
     * @reason Added use of permission node to command
     * @param dispatcher Command Dispatcher
     */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("kick")
            .requires((source) -> source.hasPermissionLevel(OpLevels.KICK_BAN_OP) || RankUtils.hasPermission(source, Permissions.VANILLA_COMMAND_KICK))
            .then(CommandManager.argument("targets", EntityArgumentType.players())
                .then(CommandManager.argument("reason", MessageArgumentType.message())
                    .executes((sourceCommandContext) -> Kick.execute(sourceCommandContext.getSource(), EntityArgumentType.getPlayers(sourceCommandContext, "targets"), MessageArgumentType.getMessage(sourceCommandContext, "reason")))
                )
                .executes((context) -> Kick.execute(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), new TranslatableText("multiplayer.disconnect.kicked", new Object[0])))
            )
        );
    }
    
    @Shadow
    private static int execute(ServerCommandSource serverCommandSource, Collection<ServerPlayerEntity> collection, Text text) {
        return Command.SINGLE_SUCCESS;
    }
    
}
