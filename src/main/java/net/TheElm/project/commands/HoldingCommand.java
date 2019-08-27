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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.utilities.InventoryUtils;
import net.TheElm.project.utilities.InventoryUtils.ItemRarity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Map;

public final class HoldingCommand {
    
    private HoldingCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (SewingMachineConfig.INSTANCE.COMMAND_EQUIPMENT.get()) {
            // Command to display the object the player is holding
            dispatcher.register(CommandManager.literal("hand")
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                    .executes(HoldingCommand::handMessage)
                )
                .executes(HoldingCommand::hand)
            );
            CoreMod.logDebug("- Registered Hand command");
        }
    }
    
    private static int hand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return HoldingCommand.holding( context, "" );
    }
    private static int handMessage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return HoldingCommand.holding( context, StringArgumentType.getString(context, "message") );
    }
    private static int holding(CommandContext<ServerCommandSource> context, String message) throws CommandSyntaxException {
        // Get player information
        ServerPlayerEntity player = context.getSource().getPlayer();
        
        // Get hand item
        ItemStack stack = player.getMainHandStack();
        Item item = stack.getItem();
        int count = stack.getCount();
        
        if ((count <= 0 ) || item.equals(Items.AIR)) {
            player.sendMessage(new LiteralText("You aren't holding any item.").formatted(Formatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        
        // List all enchantments
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments( stack );
        
        Text enchantsBuilder = ( stack.hasCustomName() ? stack.getName().formatted(Formatting.AQUA) : new TranslatableText(stack.getTranslationKey()) );
        if (enchantments.size() > 0) {
            ItemRarity rarity = InventoryUtils.getItemRarity( stack );
            enchantsBuilder.append(new LiteralText(" " + rarity.name()).formatted(rarity.formatting));
        }
        
        // Append all translations to the hovertext
        for ( Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet() ) {
            Enchantment enchant = enchantment.getKey();
            Integer level = enchantment.getValue();
            
            enchantsBuilder.append( "\n" )
                .append(enchant.getName( level ));
        }
        
        final Text enchantsText = enchantsBuilder;
        final Text output = new LiteralText("[" + count + "x ").formatted( enchantments.size() > 0 ? Formatting.AQUA : Formatting.GRAY )
            .styled((style) -> {
                if (enchantments.size() > 0)
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, enchantsText));
            })
            .append(new TranslatableText(item.getTranslationKey()))
            .append("]");
        
        return MiscCommands.playerSendsMessageAndData( player, message, output );
    }
    
}
