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

package net.TheElm.project.mixins.Commands;

import net.TheElm.project.config.SewConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.WorldBorderCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Created on Dec 21 2021 at 1:33 PM.
 * By greg in SewingMachineMod
 */
@Mixin(WorldBorderCommand.class)
public class WorldBorder {
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "executeBuffer")
    private static ServerWorld onExecuteBuffer(MinecraftServer server, ServerCommandSource source, float distance) {
        return WorldBorder.getWorld(server, source);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "executeDamage")
    private static ServerWorld onExecuteDamage(MinecraftServer server, ServerCommandSource source, float damagePerBlock) {
        return WorldBorder.getWorld(server, source);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "executeWarningTime")
    private static ServerWorld onExecuteWarningTime(MinecraftServer server, ServerCommandSource source, int time) {
        return WorldBorder.getWorld(server, source);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "executeWarningDistance")
    private static ServerWorld onExecuteWarningDistance(MinecraftServer server, ServerCommandSource source, int distance) {
        return WorldBorder.getWorld(server, source);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "executeGet")
    private static ServerWorld onExecuteGet(MinecraftServer server, ServerCommandSource source) {
        return WorldBorder.getWorld(server, source);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "executeCenter")
    private static ServerWorld onExecuteCenter(MinecraftServer server, ServerCommandSource source, Vec2f pos) {
        return WorldBorder.getWorld(server, source);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "executeSet")
    private static ServerWorld onExecuteSet(MinecraftServer server, ServerCommandSource source, double distance, long time) {
        return WorldBorder.getWorld(server, source);
    }
    
    private static ServerWorld getWorld(MinecraftServer server, ServerCommandSource source) {
        if (SewConfig.get(SewConfig.WORLD_SPECIFIC_WORLD_BORDER))
            return source.getWorld();
        return server.getOverworld();
    }
}
