package net.theelm.sewingmachine.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.utilities.CommandUtils;
import org.jetbrains.annotations.NotNull;

public final class RideCommand extends SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "ride", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("entity", EntityArgumentType.entity())
                .executes(this::ride)
            )
        );
    }
    
    private int ride(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get the new entity to ride
        Entity entity = EntityArgumentType.getEntity(context, "entity");
        
        // If no entity was found, don't try riding
        if (entity == null)
            return 0;
        
        // Attempt to set the player as riding the entity
        return player.startRiding(entity) ? 1 : 0;
    }
    
}
