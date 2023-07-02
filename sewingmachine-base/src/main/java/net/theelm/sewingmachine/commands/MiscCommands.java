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
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.objects.ticking.Carver;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class MiscCommands extends SewCommand {
    private static final @NotNull String FLIP = "(╯°□°)╯︵ ┻━┻";
    private static final @NotNull String SHRUG = "¯\\_(ツ)_/¯";
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandUtils.register(dispatcher, "shrug", builder -> builder
            .requires(CommandPredicate.isEnabled(SewCoreConfig.COMMAND_SHRUG))
            .then(CommandManager.argument("message", MessageArgumentType.message())
                .executes(this::shrugMessage)
            )
            .executes(this::shrug)
        );
        
        CommandUtils.register(dispatcher, "tableflip", builder -> builder
            .requires(CommandPredicate.isEnabled(SewCoreConfig.COMMAND_TABLEFLIP))
            .then(CommandManager.argument("message", MessageArgumentType.message())
                .executes(this::flipMessage)
            )
            .executes(this::flip)
        );
        
        CommandUtils.register(dispatcher, "lightning", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("target", EntityArgumentType.entities())
                .executes(this::hitTargetsWithLightning)
            )
        );
        
        CommandUtils.register(dispatcher, "extinguish", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("target", EntityArgumentType.entities())
                .executes(this::extinguishTargets)
            )
        );
        
        CommandUtils.register(dispatcher, "destroy", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.literal("chunk")
                .executes(this::destroyEntireChunk)
            )
            .then(CommandManager.literal("pos")
                .executes(this::destroyBelowPosition)
            )
        );
    }
    
    private int hitTargetsWithLightning(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(context, "target");
        int hit = EntityUtils.hitWithLightning(targets);
        source.sendFeedback(
            () -> Text.literal("Hit " + hit + " targets with lightning."),
            true
        );
        return hit;
    }
    
    private int extinguishTargets(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(context, "target");
        int num = EntityUtils.extinguish(targets);
        source.sendFeedback(
            () -> Text.literal("Extinguished " + num + " targets."),
            true
        );
        return num;
    }
    
    private int shrug(@NotNull CommandContext<ServerCommandSource> context) {
        return this.playerSendsMessageAndData(context.getSource(), MiscCommands.SHRUG);
    }
    private int shrugMessage(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MessageArgumentType.getSignedMessage(
            context,
            "message",
            text -> this.playerSendsMessageAndData(context.getSource(), text,MiscCommands.SHRUG)
        );
        return Command.SINGLE_SUCCESS;
    }
    private int flip(@NotNull CommandContext<ServerCommandSource> context) {
        return this.playerSendsMessageAndData(context.getSource(), MiscCommands.FLIP);
    }
    private int flipMessage(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MessageArgumentType.getSignedMessage(
            context,
            "message",
            text -> this.playerSendsMessageAndData(context.getSource(), text, MiscCommands.FLIP)
        );
        return Command.SINGLE_SUCCESS;
    }
    
    private int destroyEntireChunk(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        WorldChunk chunk = world.getWorldChunk(BlockPos.ofFloored(source.getPosition()));
        
        // Require that the player is in Creative mode
        if (source.getEntity() instanceof PlayerEntity player && !player.isCreative())
            return 0;
        
        ((LogicalWorld)world).addTickableEvent(new Carver(world, chunk));
        
        return Command.SINGLE_SUCCESS;
    }
    private int destroyBelowPosition(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        Entity entity = source.getEntityOrThrow();

        // Require that the player is in Creative mode
        if (entity instanceof PlayerEntity player && !player.isCreative())
            return 0;
        
        BlockHitResult lookingBlock = BlockUtils.getLookingBlock(source.getWorld(), entity);
        if (lookingBlock.getType() == HitResult.Type.MISS)
            return 0;
        
        ((LogicalWorld)world).addTickableEvent(new Carver(world, lookingBlock.getBlockPos()));
        
        return Command.SINGLE_SUCCESS;
    }
}
