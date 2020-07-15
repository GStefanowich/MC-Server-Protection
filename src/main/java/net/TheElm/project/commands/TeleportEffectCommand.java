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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.enums.DragonLoot;
import net.TheElm.project.utilities.BossLootRewards;
import net.TheElm.project.utilities.EffectUtils;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.ParticleArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

import java.util.Collection;
import java.util.Collections;

public class TeleportEffectCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("teleport-particles")
            .then(CommandManager.argument("particle", ParticleArgumentType.particle())
                .then(CommandManager.argument("target", EntityArgumentType.entities())
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(0, 100))
                        .executes(TeleportEffectCommand::ExecuteCount)
                    )
                    .executes(TeleportEffectCommand::ExecuteTarget)
                )
                .executes(TeleportEffectCommand::ExecuteParticle)
            )
        );
        
        dispatcher.register(CommandManager.literal("dragon-loot")
            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 54))
                .executes((context) -> GiveLootAmount(
                    context.getSource(),
                    IntegerArgumentType.getInteger(context, "amount")
                ))
            )
            .executes((context) -> GiveLootAmount(
                context.getSource(),
                1
            ))
        );
    }
    
    private static int ExecuteParticle(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Execute(
            Collections.singleton(context.getSource().getEntity()),
            ParticleArgumentType.getParticle(context, "particle"),
            1
        );
    }
    private static int ExecuteTarget(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Execute(
            EntityArgumentType.getEntities(context, "target"),
            ParticleArgumentType.getParticle(context, "particle"),
            1
        );
    }
    private static int ExecuteCount(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return Execute(
            EntityArgumentType.getEntities(context, "target"),
            ParticleArgumentType.getParticle(context, "particle"),
            IntegerArgumentType.getInteger(context, "count")
        );
    }
    private static <E extends Entity, P extends ParticleEffect> int Execute(Collection<E> entities, P particle, int count) {
        for(E entity : entities) {
            if (entity instanceof LivingEntity)
                EffectUtils.particleSwirl(particle, (LivingEntity) entity, count);
            else 
                EffectUtils.particleSwirl(particle, (ServerWorld) entity.getEntityWorld(), entity.getPos(), count);
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private static int GiveLootAmount(ServerCommandSource source, int count) throws CommandSyntaxException {
        for (int i = 0; i < count; i++) {
            ItemStack reward = DragonLoot.createReward();
            
            if (reward != null) {
                if (!BossLootRewards.DRAGON_LOOT.addLoot(source.getPlayer().getUuid(), reward)) {
                    source.sendError(new LiteralText("Could not add loot reward, chest is full."));
                    return 0;
                }
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
}
