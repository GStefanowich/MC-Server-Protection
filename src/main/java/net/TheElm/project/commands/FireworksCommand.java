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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.objects.fireworks.FireworkSchemes;
import net.TheElm.project.utilities.IntUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Created on Aug 25 2021 at 2:30 AM.
 * By greg in SewingMachineMod
 */
public final class FireworksCommand {
    private FireworksCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "Fireworks", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.literal("spawner")
                .then(CommandManager.literal("add")
                )
                .then(CommandManager.literal("remove")
                )
            )
            .then(CommandManager.literal("event")
                .then(CommandManager.literal("create")
                )
                .then(CommandManager.literal("remove")
                )
                .then(CommandManager.literal("trigger")
                )
            )
            .then(CommandManager.literal("launch")
                .executes(FireworksCommand::catcher)
            )
        );
    }
    
    private static int catcher(@NotNull CommandContext<ServerCommandSource> context) {
        try {
            return FireworksCommand.launchFirework(context);
        } catch (Exception e) {
            CoreMod.logError(e);
        }
        return 0;
    }
    
    private static int launchFirework(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Vec3d pos = source.getPosition();
        ServerWorld world = source.getWorld();
        FireworkRocketEntity rocketEntity = new FireworkRocketEntity(EntityType.FIREWORK_ROCKET, world);
        
        FireworkSchemes.Scheme scheme = FireworkSchemes.USA;
        
        Random random = new Random();
        NbtCompound base = new NbtCompound(),
            item = new NbtCompound(),
            tag = new NbtCompound(),
            fireworks = new NbtCompound();
        
        pos = pos.add(
            random.nextInt(40) - 20,
            8,
            random.nextInt(40) - 20
        );
        
        NbtList explosions = new NbtList();
        int explCount = IntUtils.random(random, 1, 4);
        for (int i = 0; i <= explCount; i++)
            explosions.add(scheme.generate(random));
        
        item.putString("id", "minecraft:firework_rocket");
        item.putInt("Count", 1);
        item.put("tag", tag);
        
        base.putInt("Life", 0);
        base.putInt("LifeTime", IntUtils.random(random, 15, 40));
        base.put("FireworksItem", item);
        
        tag.put("Fireworks", fireworks);
        
        fireworks.putInt("Flight", IntUtils.random(random, 1, 3));
        fireworks.put("Explosions", explosions);
        
        rocketEntity.readCustomDataFromNbt(base);
        rocketEntity.updatePosition(pos.getX(), pos.getY(), pos.getZ());
        rocketEntity.setVelocity(random.nextGaussian() * 0.001D, 0.05D, random.nextGaussian() * 0.001D);
        world.spawnEntity(rocketEntity);
        
        return Command.SINGLE_SUCCESS;
    }
}
