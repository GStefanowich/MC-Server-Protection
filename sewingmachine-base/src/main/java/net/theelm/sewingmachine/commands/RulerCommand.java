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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.protections.BlockRange;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public final class RulerCommand extends SewCommand {
    private static final ExceptionTranslatableServerSide BLOCK_NOT_HIT = ServerText.exception("ruler.no_block");
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "ruler", builder -> builder
            .then(CommandManager.argument("test", BlockPosArgumentType.blockPos()))
            .executes(this::ruler)
        );
    }
    
    private int ruler(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
            
            player.sendMessage(Text.literal("First position set to ").formatted(Formatting.YELLOW)
                .append(MessageUtils.xyzToText( newPos ))
                .append(", run command again at a second position."));
            
        } else {
            // Update ruler position
            playerData.setRulerB(newPos);
            
            BlockPos firstPos = playerData.getRulerA();
            
            /*
             * Output RULER calculation
             */
            BlockRange region = BlockRange.between(firstPos, newPos);
            
            MutableText distance = Text.literal("Distance: ").formatted(Formatting.YELLOW)
                .append(region.displayDimensions());
            
            // East-West
            if (region.getEastWest() != 0) {
                distance.append("\n- ")
                    .append(Text.literal(region.getEastWest() + " ").formatted(Formatting.AQUA))
                    .append(region.isEastOrWest().getName().toLowerCase());
            }
            // North-South
            if (region.getNorthSouth() != 0) {
                distance.append("\n- ")
                    .append(Text.literal(region.getNorthSouth() + " ").formatted(Formatting.AQUA))
                    .append(region.isNorthOrSouth().getName().toLowerCase());
            }
            // Up-Down
            if (region.getUpDown() != 0) {
                distance.append("\n- ")
                    .append(Text.literal(region.getUpDown() + " ").formatted(Formatting.AQUA))
                    .append(region.isUpOrDown().getName().toLowerCase());
            }
            if (region.hasDistinctVolume())
                distance.append("\n").append(region.formattedVolume()).append(" block volume");
            
            player.sendMessage(distance);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
}
