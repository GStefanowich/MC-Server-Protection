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
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import net.minecraft.block.BedBlock;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerSpawnCommand implements SewCommand {
    private static final ExceptionTranslatableServerSide SPAWN_NOT_AT_BED = ServerText.exception("spawn.set.missing_bed");
    public static final Set<UUID> commandRanUUIDs = Collections.synchronizedSet(new HashSet<>());
    public static final String COMMAND_NAME = "SetSpawn";
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        /*
         * Move the players spawn
         */
        CommandUtils.register(dispatcher, PlayerSpawnCommand.COMMAND_NAME, builder -> builder
            .then(CommandManager.argument("bed_position", BlockPosArgumentType.blockPos() )
                .executes(this::moveSpawn)
            )
        );
    }
    
    private int moveSpawn(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = source.getWorld();
        
        // Check that the position is a bed
        BlockPos bedPos = BlockPosArgumentType.getBlockPos( context, "bed_position" );
        if (!( world.getBlockState(bedPos).getBlock() instanceof BedBlock))
            throw SPAWN_NOT_AT_BED.create(player);
        
        // Add a bypass to the setting of player spawn
        PlayerSpawnCommand.commandRanUUIDs.add( player.getUuid() );
        
        // Log our definition
        CoreMod.logInfo("Player " + player.getName().getString() + " spawn updated to X " + bedPos.getX() + ", Z " + bedPos.getZ() + ", Y " + bedPos.getY());
        player.setSpawnPoint(world.getRegistryKey(), bedPos, 0.0F, false, true);
        
        return Command.SINGLE_SUCCESS;
    }
}
