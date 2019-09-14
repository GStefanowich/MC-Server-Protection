package net.TheElm.project.mixins.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.MeCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MeCommand.class)
public final class Me {
    
    @Inject(at = @At("HEAD"), method = "register", cancellable = true)
    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CallbackInfo callback){
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
                    Text text = PlayerNameUtils.getPlayerChatDisplay( player, "* ", ((PlayerChat) player).getChatRoom(), Formatting.WHITE )
                        .append(" ")
                        .append( message );
                    
                    // Send to all players
                    if (server != null) server.getPlayerManager().sendToAll(text);
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
        callback.cancel();
    }
    
}
