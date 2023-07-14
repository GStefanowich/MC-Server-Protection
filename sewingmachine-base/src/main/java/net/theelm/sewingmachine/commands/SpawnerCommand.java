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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.base.utilities.SpawnerUtils;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class SpawnerCommand extends SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "spawner", builder -> builder
            .requires(CommandPredicate.isEnabled(SewBaseConfig.SILK_TOUCH_SPAWNERS).and(OpLevels.CHEATING))
            .then(CommandManager.argument("type", RegistryEntryArgumentType.registryEntry(registry, RegistryKeys.ENTITY_TYPE))
                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                .then(CommandManager.argument("weight", IntegerArgumentType.integer(1))
                    .executes(this::giveWithWeight)
                )
                .executes(this::give)
            )
        );
    }
    
    private int give(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.giveWithWeight(context, 1);
    }
    private int giveWithWeight(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int weight = IntegerArgumentType.getInteger(context, "weight");
        return this.giveWithWeight(context, weight);
    }
    
    private int giveWithWeight(@NotNull CommandContext<ServerCommandSource> context, int weight) throws CommandSyntaxException {
        // Get command information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        // Get item information
        RegistryKey<EntityType<?>> mob = RegistryEntryArgumentType.getEntityType(context, "type")
            .registryKey();
        EntityType<?> type = Registries.ENTITY_TYPE.get(mob);
        
        ItemStack stack = player.getMainHandStack();
        boolean holdingSpawner = Objects.equals(stack.getItem(), Items.SPAWNER);
        ItemStack spawner = holdingSpawner ? stack : new ItemStack(Items.SPAWNER);
        
        if (SpawnerUtils.addEntity(spawner, mob, weight)) {
            // Give the spawner
            if (!holdingSpawner) {
                player.getInventory()
                    .offerOrDrop(spawner);
                
                source.sendFeedback(
                    () -> Text.literal("Created a ")
                        .append(Text.translatable(type.getTranslationKey()))
                        .append(" spawner!"),
                    false
                );
            } else {
                source.sendFeedback(
                    () -> Text.literal("Added ")
                        .append(Text.translatable(type.getTranslationKey()))
                        .append(" to your mob spawner"),
                    false
                );
            }
            
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFeedback(
                () -> Text.literal(holdingSpawner ? "Failed to add " : "Failed to create ")
                    .append(Text.translatable(type.getTranslationKey()))
                    .append((holdingSpawner ? " to" : "") + " Spawner"),
                false
            );
            return 0;
        }
    }
}
