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
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.CommandPredicate;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public final class SpawnerCommand {
    
    private SpawnerCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "spawner", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.SILK_TOUCH_SPAWNERS).and(OpLevels.CHEATING))
            .then(CommandManager.argument("type", EntitySummonArgumentType.entitySummon())
                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                .executes((context) -> {
                    // Get command information
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    ServerWorld world = player.getServerWorld();
                    
                    // Get item information
                    ItemStack spawner = new ItemStack(Items.SPAWNER);
                    Identifier mobIdentifier = EntitySummonArgumentType.getEntitySummon(context, "type");
                    NbtString mob = NbtString.of(mobIdentifier.toString());
                    
                    // Add mob to the list
                    NbtCompound tag = spawner.getOrCreateNbt();
                    NbtList list;
                    if (tag.contains("EntityIds", NbtType.LIST))
                        list = tag.getList("EntityIds", NbtType.STRING);
                    else {
                        list = new NbtList();
                        tag.put("EntityIds", list);
                    }
                    
                    list.add(mob);
                    
                    // Give the spawner
                    player.getInventory()
                        .offerOrDrop(spawner);
                    
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
    
}
