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
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.DragonLoot;
import net.TheElm.project.mixins.Server.ServerWorldAccessor;
import net.TheElm.project.objects.rewards.WeightedReward;
import net.TheElm.project.utilities.BossLootRewards;
import net.TheElm.project.utilities.EffectUtils;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.world.WanderingTraderManager;
import net.minecraft.world.spawner.Spawner;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DebugCommands {
    private DebugCommands() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "Teleport Particles", builder -> builder
            .then(CommandManager.argument("particle", ParticleEffectArgumentType.particleEffect())
                .then(CommandManager.argument("target", EntityArgumentType.entities())
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(0, 100))
                        .executes(DebugCommands::executeCount)
                    )
                    .executes(DebugCommands::executeTarget)
                )
                .executes(DebugCommands::executeParticle)
            )
        );
        
        ServerCore.register(dispatcher, "Tick Wandering Trader", builder -> builder
            .then(CommandManager.argument("force", BoolArgumentType.bool())
                .executes(DebugCommands::forceTraderSpawn)
            )
            .executes(DebugCommands::forceTrader)
        );
        
        ServerCore.register(dispatcher, "Dragon Loot", builder -> builder
            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 54))
                .executes((context) -> giveLootAmount(
                    context.getSource(),
                    IntegerArgumentType.getInteger(context, "amount")
                ))
            )
            .executes((context) -> giveLootAmount(
                context.getSource(),
                1
            ))
        );
    }
    
    private static int executeParticle(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return execute(
            Collections.singleton(context.getSource().getEntity()),
            ParticleEffectArgumentType.getParticle(context, "particle"),
            1
        );
    }
    private static int executeTarget(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return execute(
            EntityArgumentType.getEntities(context, "target"),
            ParticleEffectArgumentType.getParticle(context, "particle"),
            1
        );
    }
    private static int executeCount(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return execute(
            EntityArgumentType.getEntities(context, "target"),
            ParticleEffectArgumentType.getParticle(context, "particle"),
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private static <E extends Entity, P extends ParticleEffect> int execute(@NotNull Collection<E> entities, P particle, int count) {
        for(E entity : entities) {
            if (entity instanceof LivingEntity livingEntity)
                EffectUtils.particleSwirl(particle, livingEntity, true, count);
            else if (entity.getWorld() instanceof ServerWorld world)
                EffectUtils.particleSwirl(particle, world, entity.getPos(), true,count);
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private static int giveLootAmount(@NotNull ServerCommandSource source, int count) throws CommandSyntaxException {
        for (int i = 0; i < count; i++) {
            WeightedReward reward = DragonLoot.getReward();
            if (reward != null && !BossLootRewards.DRAGON_LOOT.addLoot(source.getPlayer(), reward)) {
                source.sendError(new LiteralText("Could not add loot reward, chest is full."));
                return 0;
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int forceTrader(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        int spawn = DebugCommands.forceTraderSpawn(context, false);
        source.sendFeedback(new LiteralText(spawn > 0 ? "Spawned wandering trader" : "Failed to spawn wandering trader").formatted(spawn > 0 ? Formatting.GREEN : Formatting.RED), false);
        
        return spawn;
    }
    private static int forceTraderSpawn(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        int spawn = DebugCommands.forceTraderSpawn(context, BoolArgumentType.getBool(context, "force"));
        source.sendFeedback(new LiteralText(spawn > 0 ? "Spawned wandering trader" : "Failed to spawn wandering trader").formatted(spawn > 0 ? Formatting.GREEN : Formatting.RED), false);
        
        return spawn;
    }
    private static int forceTraderSpawn(@NotNull CommandContext<ServerCommandSource> context, boolean force) {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerWorld world = server.getOverworld();
        
        Optional<WanderingTraderManager> optional = ((ServerWorldAccessor)world).getSpawners()
            .stream()
            .filter(spawner -> spawner instanceof WanderingTraderManager)
            .map(spawner -> (WanderingTraderManager)spawner)
            .findFirst();
        if (optional.isEmpty())
            return 0;
        
        WanderingTraderManager manager = optional.get();
        int spawn;
        
        if (!force)
            spawn = manager.spawn(world, false, false);
        else do {
            spawn = manager.spawn(world, false, false);
        } while (spawn <= 0);
        
        return spawn;
    }
}
