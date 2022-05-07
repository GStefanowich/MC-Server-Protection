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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
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
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class HoldingCommand {
    
    private HoldingCommand() {}
    
    private static final ExceptionTranslatableServerSide PLAYER_EMPTY_HAND = TranslatableServerSide.exception("player.equipment.empty_hand");
    private static final ExceptionTranslatableServerSide PLAYER_EMPTY_SLOT = TranslatableServerSide.exception("player.equipment.empty_slot");
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        // Command to display the object the player is holding
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ServerCore.register(dispatcher, slot.getName(), builder -> builder
                .requires(CommandPredicate.isEnabled(SewConfig.COMMAND_EQUIPMENT))
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                    .executes((source) -> HoldingCommand.handMessage(source, slot))
                )
                .executes((source) -> HoldingCommand.slot(source, slot))
            );
        }
    }
    
    private static int slot(@NotNull CommandContext<ServerCommandSource> context, @NotNull EquipmentSlot slot) throws CommandSyntaxException {
        return HoldingCommand.holding(context, slot,"");
    }
    private static int handMessage(@NotNull CommandContext<ServerCommandSource> context, @NotNull EquipmentSlot slot) throws CommandSyntaxException {
        return HoldingCommand.holding(context, slot, StringArgumentType.getString(context, "message"));
    }
    private static int holding(@NotNull CommandContext<ServerCommandSource> context, @NotNull EquipmentSlot slot, String message) throws CommandSyntaxException {
        // Get player information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Get hand item
        ItemStack stack = player.getEquippedStack(slot);
        if (stack.isEmpty())
            throw (slot.getType() == EquipmentSlot.Type.HAND ? PLAYER_EMPTY_HAND : PLAYER_EMPTY_SLOT).create(source);
        
        Item item = stack.getItem();
        int count = stack.getCount();
        
        // List all enchantments
        final Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        final Text output = new LiteralText("[" + count + "x ").formatted(enchantments.isEmpty() ? Formatting.GRAY : Formatting.AQUA)
            .styled((style) -> {
                if (!enchantments.isEmpty())
                    return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(stack)));
                return style;
            })
            .append(new TranslatableText(item.getTranslationKey()))
            .append("]");
        
        return MiscCommands.playerSendsMessageAndData( player, new LiteralText(message).append(" ").append(output));
    }
    
}
