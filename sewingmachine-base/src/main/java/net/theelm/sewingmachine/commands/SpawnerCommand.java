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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.registry.RegistryKeys;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public final class SpawnerCommand extends SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        ServerCore.register(dispatcher, "spawner", builder -> builder
            .requires(CommandPredicate.isEnabled(SewCoreConfig.SILK_TOUCH_SPAWNERS).and(OpLevels.CHEATING))
            .then(CommandManager.argument("type", RegistryEntryArgumentType.registryEntry(registry, RegistryKeys.ENTITY_TYPE))
                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                .executes((context) -> {
                    // Get command information
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    
                    // Get item information
                    ItemStack spawner = new ItemStack(Items.SPAWNER);
                    Identifier mobIdentifier = RegistryEntryArgumentType.getEntityType(context, "type")
                        .registryKey()
                        .getValue();
                    NbtString mob = NbtString.of(mobIdentifier.toString());
                    
                    // Add mob to the list
                    NbtCompound tag = spawner.getOrCreateNbt();
                    NbtList list;
                    if (tag.contains("EntityIds", NbtElement.LIST_TYPE))
                        list = tag.getList("EntityIds", NbtElement.STRING_TYPE);
                    else {
                        list = new NbtList();
                        tag.put("EntityIds", list);
                    }
                    
                    list.add(mob);
                    tag.put("display", NbtUtils.getSpawnerDisplay(list));
                    
                    // Give the spawner
                    player.getInventory()
                        .offerOrDrop(spawner);
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
    
}
