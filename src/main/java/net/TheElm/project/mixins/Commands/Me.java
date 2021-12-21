package net.TheElm.project.mixins.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.TextUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.MeCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MeCommand.class)
public final class Me {
    
    /**
     * @author TheElm
     * @reason Overwrite the "/me" message format
     * @param dispatcher The command dispatcher
     */
    @Overwrite
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("me")
            .then( CommandManager.argument( "action", StringArgumentType.greedyString())
                .executes((context) -> {
                    // Get player
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    
                    // Get message that player added
                    String message = StringArgumentType.getString(context, "action");
                    
                    // Send to all players
                    MessageUtils.sendTo(
                        ((PlayerChat) player).getChatRoom(),
                        player,
                        MessageUtils.formatPlayerMessage(player, new LiteralText("* " + message).formatted(Formatting.ITALIC))
                    );
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
    
}
