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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.MessageArgumentType;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class HoldingCommand extends SewCommand {
    private static final ExceptionTranslatableServerSide PLAYER_EMPTY_HAND = TranslatableServerSide.exception("player.equipment.empty_hand");
    private static final ExceptionTranslatableServerSide PLAYER_EMPTY_SLOT = TranslatableServerSide.exception("player.equipment.empty_slot");
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        // Command to display the object the player is holding
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ServerCore.register(dispatcher, slot.getName(), builder -> builder
                .requires(CommandPredicate.isEnabled(SewCoreConfig.COMMAND_EQUIPMENT))
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                    .executes((source) -> this.handMessage(source, slot))
                )
                .executes((source) -> this.slot(source, slot))
            );
        }
    }
    
    private int slot(@NotNull CommandContext<ServerCommandSource> context, @NotNull EquipmentSlot slot) throws CommandSyntaxException {
        Text holding = this.holding(context, slot);
        return this.playerSendsMessageAndData(
            context.getSource(),
            null,
            holding
        );
    }
    private int handMessage(@NotNull CommandContext<ServerCommandSource> context, @NotNull EquipmentSlot slot) throws CommandSyntaxException {
        Text holding = this.holding(context, slot);
        MessageArgumentType.getSignedMessage(context, "message", message -> {
            this.playerSendsMessageAndData(context.getSource(), message, holding);
        });
        return Command.SINGLE_SUCCESS;
    }
    private @NotNull Text holding(@NotNull CommandContext<ServerCommandSource> context, @NotNull EquipmentSlot slot) throws CommandSyntaxException {
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
        return Text.literal("[" + count + "x ").formatted(enchantments.isEmpty() ? Formatting.GRAY : Formatting.AQUA)
            .styled((style) -> {
                if (!enchantments.isEmpty())
                    return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(stack)));
                return style;
            })
            .append(Text.translatable(item.getTranslationKey()))
            .append("]");
    }
    
}
