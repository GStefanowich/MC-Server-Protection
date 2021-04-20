package net.TheElm.project.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.utilities.CommandUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

public final class RideCommand {
    private RideCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "ride", builder -> builder
            .requires(CommandUtils.requires(OpLevels.CHEATING))
            .then(CommandManager.argument("entity", EntityArgumentType.entity())
                    .executes(RideCommand::ride)
            )
        );
    }
    
    private static int ride(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        Entity entity = EntityArgumentType.getEntity(context, "entity");
        
        return player.startRiding(entity) ? 1 : 0;
    }
    
}
