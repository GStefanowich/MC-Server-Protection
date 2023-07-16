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

package net.theelm.sewingmachine.base.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.base.objects.PlayerBackpack;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

public final class BackpackCommand implements SewCommand {
    
    private static final ExceptionTranslatableServerSide PLAYERS_NO_BACKPACK = ServerText.exception("player.no_backpack");
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "Backpack", builder -> builder
            .requires(CommandPredicate.isEnabled(SewBaseConfig.ALLOW_BACKPACKS))
            .then(CommandManager.literal("pickup")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registry))
                    .executes(this::autoPickup)
                )
            )
            .executes(this::openBackpack)
        );
    }
    
    private int autoPickup(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        PlayerBackpack backpack = ((BackpackCarrier) player).getBackpack();
        if (backpack == null)
            throw PLAYERS_NO_BACKPACK.create( player );
        
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item")
            .getItem();
        
        // Add to autopickup
        boolean added = backpack.addAutoPickup( item );
        
        player.sendMessage(
            Text.literal(added ?
                "Backpack will now automatically pick up "
                : "Backpack will no longer pick up "
            ).formatted(Formatting.YELLOW).append(Text.translatable(item.getTranslationKey()).formatted(Formatting.AQUA)).append(".")
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int openBackpack(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        // Open the backpack screen on the client
        OptionalInt i = InventoryUtils.openBackpack(player);
        
        // Check if the player has a backpack
        if (!i.isPresent())
            throw PLAYERS_NO_BACKPACK.create(player);
        
        // Success!
        return Command.SINGLE_SUCCESS;
    }
}
