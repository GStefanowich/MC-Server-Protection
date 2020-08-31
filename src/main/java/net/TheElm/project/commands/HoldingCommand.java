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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.utilities.ColorUtils;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.TheElm.project.utilities.InventoryUtils.ItemRarity;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Map;

public final class HoldingCommand {
    
    private HoldingCommand() {}
    
    private static final ExceptionTranslatableServerSide PLAYER_EMPTY_HAND = TranslatableServerSide.exception("player.equipment.empty_hand");
    private static final ExceptionTranslatableServerSide PLAYER_EMPTY_SLOT = TranslatableServerSide.exception("player.equipment.empty_slot");
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Command to display the object the player is holding
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            dispatcher.register(CommandManager.literal(slot.getName())
                .requires((source) -> SewConfig.get(SewConfig.COMMAND_EQUIPMENT))
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                    .executes((source) -> HoldingCommand.handMessage(source, slot))
                )
                .executes((source) -> HoldingCommand.slot(source, slot))
            );
        }
        CoreMod.logDebug("- Registered Equipment command");
    }
    
    private static int slot(CommandContext<ServerCommandSource> context, EquipmentSlot slot) throws CommandSyntaxException {
        return HoldingCommand.holding( context, slot,"" );
    }
    private static int handMessage(CommandContext<ServerCommandSource> context, EquipmentSlot slot) throws CommandSyntaxException {
        return HoldingCommand.holding( context, slot, StringArgumentType.getString(context, "message") );
    }
    private static int holding(CommandContext<ServerCommandSource> context, EquipmentSlot slot, String message) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get hand item
        ItemStack stack = player.getEquippedStack( slot );
        if (stack.isEmpty())
            throw (slot.getType() == EquipmentSlot.Type.HAND ? PLAYER_EMPTY_HAND : PLAYER_EMPTY_SLOT).create(source);
        
        Item item = stack.getItem();
        int count = stack.getCount();
        
        // List all enchantments
        final Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        
        final MutableText enchantsBuilder = ( stack.hasCustomName() ? ColorUtils.format(stack.getName(), Formatting.AQUA) : new TranslatableText(stack.getTranslationKey()));
        if (!enchantments.isEmpty()) {
            ItemRarity rarity = InventoryUtils.getItemRarity( stack );
            enchantsBuilder.append(new LiteralText(" " + rarity.name()).formatted(rarity.formatting));
            
            // Append all translations to the hovertext
            for ( Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet() ) {
                Enchantment enchant = enchantment.getKey();
                Integer level = enchantment.getValue();
                
                enchantsBuilder.append( "\n" )
                    .append(enchant.getName( level ));
            }
        }
        
        if (item.isDamageable()) {
            enchantsBuilder.append("\n")
                // Get the items durability
                .append(( stack.isDamageable() ?
                    new TranslatableText("item.durability",
                        FormattingUtils.number(stack.getMaxDamage() - stack.getDamage()),
                        FormattingUtils.number(stack.getMaxDamage())
                    )
                    : new TranslatableText("item.unbreakable")
                ))
                .append("\n")
                // Get the repair cost
                .append(new TranslatableText("container.repair.cost",
                    FormattingUtils.number( stack.getRepairCost() )
                ));
        }
        
        final Text output = new LiteralText("[" + count + "x ").formatted(enchantments.isEmpty() ? Formatting.GRAY : Formatting.AQUA)
            .styled((style) -> {
                if (!enchantments.isEmpty())
                    return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, enchantsBuilder));
                return style;
            })
            .append(new TranslatableText(item.getTranslationKey()))
            .append("]");
        
        return MiscCommands.playerSendsMessageAndData( player, message, output );
    }
    
}
