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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.ItemUtils;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

/**
 * Created on Aug 17 2021 at 9:25 AM.
 * By greg in SewingMachineMod
 */
public class HeadCommand extends SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        ServerCore.register(dispatcher, "skull", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.literal("player")
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                        .executes(context -> this.getPlayerHead(context, IntegerArgumentType.getInteger(context, "count")))
                    )
                    .executes(context -> this.getPlayerHead(context, 1))
                )
            )
            .then(CommandManager.literal("texture")
                .then(CommandManager.argument("texture", StringArgumentType.word())
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                        .executes(context -> this.getTextureHead(context, IntegerArgumentType.getInteger(context, "count")))
                    )
                    .executes(context -> this.getTextureHead(context, 1))
                )
            )
        );
    }
    
    private int getPlayerHead(@NotNull CommandContext<ServerCommandSource> context, int count) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String name = StringArgumentType.getString(context, "player");
        
        ItemUtils.insertItems(source.getPlayer(), Items.PLAYER_HEAD, count, stack -> {
            // Assign the SkullOwner tag
            NbtCompound skullOwner = stack.getOrCreateNbt();
            skullOwner.putString("SkullOwner", name);
        });
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getTextureHead(@NotNull CommandContext<ServerCommandSource> context, int count) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        final String texture = StringArgumentType.getString(context, "texture");
        final String base64 = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + texture + "\"}}}").getBytes());
        
        ItemUtils.insertItems(source.getPlayer(), Items.PLAYER_HEAD, count, stack -> {
            // Assign the Properties
            NbtCompound skullOwner = stack.getOrCreateSubNbt("SkullOwner"),
                listItem = new NbtCompound(),
                properties;
            listItem.putString("Value", base64);
            
            NbtList textures;
            skullOwner.putUuid("Id", CoreMod.SPAWN_ID);
            skullOwner.put("Properties", properties = new NbtCompound());
            properties.put("textures", textures = new NbtList());
            textures.add(listItem);
        });
        
        return count;
    }
}
