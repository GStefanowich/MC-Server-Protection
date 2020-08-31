package net.TheElm.project.mixins.Commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.utilities.RankUtils;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.command.arguments.MessageArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(BanCommand.class)
public class Ban {
    
    private static final SimpleCommandExceptionType BAN_EXEMPT = new SimpleCommandExceptionType(new LiteralText("That player cannot be banned."));
    
    /**
     * @author TheElm
     * @reason Added use of permission node to command
     * @param dispatcher Command Dispatcher
     */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ban")
            .requires((source) -> source.hasPermissionLevel(OpLevels.KICK_BAN_OP) || RankUtils.hasPermission(source, Permissions.VANILLA_COMMAND_BAN))
            .then(CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("reason", MessageArgumentType.message())
                    .executes((context) -> Ban.ban(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), MessageArgumentType.getMessage(context, "reason")))
                )
                .executes((context) -> Ban.ban(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets"), null))
            )
        );
    }
    
    @Shadow
    private static int ban(ServerCommandSource serverCommandSource, Collection<GameProfile> collection, @Nullable Text text) {
        return Command.SINGLE_SUCCESS;
    }
    
}
