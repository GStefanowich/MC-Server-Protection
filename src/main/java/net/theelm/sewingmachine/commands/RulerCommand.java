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
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.protections.BlockRange;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public final class RulerCommand {
    
    private static final ExceptionTranslatableServerSide BLOCK_NOT_HIT = TranslatableServerSide.exception("ruler.no_block");
    
    private RulerCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "ruler", builder -> builder
            .then(CommandManager.argument("test", BlockPosArgumentType.blockPos()))
            .executes(RulerCommand::ruler)
        );
    }
    
    private static int ruler(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the command information
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        
        // Get the block that the player is facing
        BlockHitResult search = BlockUtils.getLookingBlock(world, player, 8);
        if (search.getType() == HitResult.Type.MISS)
            throw RulerCommand.BLOCK_NOT_HIT.create( player );
        
        BlockPos newPos = search.getBlockPos().offset(search.getSide());
        PlayerData playerData = (PlayerData) player;
        
        // Start a new RULER calculation
        if ((playerData.getRulerB() != null) || (playerData.getRulerA() == null)) {
            // Update ruler position
            playerData.setRulerA(newPos);
            playerData.setRulerB(null);
            
            player.sendSystemMessage(new LiteralText("First position set to ").formatted(Formatting.YELLOW)
                .append(MessageUtils.xyzToText( newPos ))
                .append(", run command again at a second position."), Util.NIL_UUID);
            
        } else {
            // Update ruler position
            playerData.setRulerB(newPos);
            
            BlockPos firstPos = playerData.getRulerA();
            
            /*
             * Output RULER calculation
             */
            BlockRange region = BlockRange.between(firstPos, newPos);
            
            MutableText distance = new LiteralText("Distance: ").formatted(Formatting.YELLOW)
                .append(region.displayDimensions());
            
            // East-West
            if (region.getEastWest() != 0) {
                distance.append("\n- ")
                    .append(new LiteralText(region.getEastWest() + " ").formatted(Formatting.AQUA))
                    .append(region.isEastOrWest().getName().toLowerCase());
            }
            // North-South
            if (region.getNorthSouth() != 0) {
                distance.append("\n- ")
                    .append(new LiteralText(region.getNorthSouth() + " ").formatted(Formatting.AQUA))
                    .append(region.isNorthOrSouth().getName().toLowerCase());
            }
            // Up-Down
            if (region.getUpDown() != 0) {
                distance.append("\n- ")
                    .append(new LiteralText(region.getUpDown() + " ").formatted(Formatting.AQUA))
                    .append(region.isUpOrDown().getName().toLowerCase());
            }
            if (region.hasDistinctVolume())
                distance.append("\n").append(region.formattedVolume()).append(" block volume");
            
            player.sendSystemMessage(distance, CoreMod.SPAWN_ID);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
}
