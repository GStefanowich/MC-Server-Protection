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

package net.theelm.sewingmachine.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.theelm.sewingmachine.CoreMod;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.objects.PlayerBackpack;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.network.MessageType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public final class BackpackCommand {
    
    private static final ExceptionTranslatableServerSide PLAYERS_NO_BACKPACK = TranslatableServerSide.exception("player.no_backpack");
    
    private BackpackCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "Backpack", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.ALLOW_BACKPACKS))
            .then(CommandManager.literal("pickup")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                    .executes(BackpackCommand::autoPickup)
                )
            )
            .executes(BackpackCommand::openBackpack)
        );
    }
    
    private static int autoPickup(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
        ).formatted(Formatting.YELLOW).append(new TranslatableText(item.getTranslationKey()).formatted(Formatting.AQUA)).append("."), MessageType.GAME_INFO, CoreMod.SPAWN_ID);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int openBackpack(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Check if the player has a backpack
        PlayerBackpack backpack = ((BackpackCarrier) player).getBackpack();
        if (backpack == null)
            throw PLAYERS_NO_BACKPACK.create(player);
        
        // Open the backpack screen on the client
        OptionalInt i = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(BackpackCommand::openBackpack, backpack.getName()));
        
        // Success!
        return i.isPresent() ? Command.SINGLE_SUCCESS : 0;
    }
    
    private static @Nullable ScreenHandler openBackpack(int i, @NotNull PlayerInventory inventory, @NotNull PlayerEntity player) {
        PlayerBackpack backpack = ((BackpackCarrier) player).getBackpack();
        if (backpack == null)
            return null;
        return backpack.createContainer(i, inventory);
    }
}
