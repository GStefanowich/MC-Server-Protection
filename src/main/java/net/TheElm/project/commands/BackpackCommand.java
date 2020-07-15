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

package net.TheElm.project.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.objects.PlayerBackpack;
import net.minecraft.command.arguments.ItemStackArgumentType;
import net.minecraft.container.SimpleNamedContainerFactory;
import net.minecraft.item.Item;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public final class BackpackCommand {
    
    private static final ExceptionTranslatableServerSide PLAYERS_NO_BACKPACK = new ExceptionTranslatableServerSide("player.no_backpack");
    
    private BackpackCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("backpack")
            .requires((context) -> SewingMachineConfig.INSTANCE.ALLOW_BACKPACKS.get())
            .then(CommandManager.literal("pickup")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                    .executes(BackpackCommand::AutoPickup)
                )
            )
            .executes(BackpackCommand::OpenBackpack)
        );
    }
    
    private static int AutoPickup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        PlayerBackpack backpack = ((BackpackCarrier) player).getBackpack();
        if (backpack == null)
            throw PLAYERS_NO_BACKPACK.create( player );
        
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item")
            .getItem();
        
        // Add to autopickup
        boolean added = backpack.addAutoPickup( item );
        
        player.sendMessage(new LiteralText( added ?
            "Backpack will now automatically pick up "
            : "Backpack will no longer pick up "
        ).formatted(Formatting.YELLOW).append(new TranslatableText(item.getTranslationKey()).formatted(Formatting.AQUA)).append("."));
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int OpenBackpack(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        PlayerBackpack backpack = ((BackpackCarrier) player).getBackpack();
        if (backpack == null)
            throw PLAYERS_NO_BACKPACK.create( player );
        
        player.openContainer(new SimpleNamedContainerFactory((i, playerInventory, playerEntityx) ->
            backpack.createContainer(i, playerInventory),
        backpack.getName()));
        
        return Command.SINGLE_SUCCESS;
    }
    
}
