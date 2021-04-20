package net.TheElm.project.mixins.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.MeCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal( "me" )
            .then( CommandManager.argument( "action", StringArgumentType.greedyString())
                .executes((context) -> {
                    // Get player
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    
                    // Get message that player added
                    String message = StringArgumentType.getString( context, "action" );
                    
                    // Get Server
                    MinecraftServer server = player.getServer();
                    
                    // Create the player display for chat
                    Text text = PlayerNameUtils.getPlayerChatDisplay(player, "* ", ((PlayerChat) player).getChatRoom(), Formatting.WHITE)
                        .append(" ")
                        .append( message );
                    
                    // Send to all players
                    MessageUtils.sendTo(
                        ((PlayerChat) player).getChatRoom(),
                        player,
                        text
                    );
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
    
}
