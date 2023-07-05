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

package net.theelm.sewingmachine.protection.mixins.Server;

import com.mojang.datafixers.DataFixer;
import net.minecraft.util.ApiServices;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.storage.LevelStorage;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;

/**
 * Created on Aug 29 2021 at 11:52 AM.
 * By greg in SewingMachineMod
 */
@Mixin(MinecraftDedicatedServer.class)
public abstract class MinecraftDedicatedServerMixin extends MinecraftServer implements DedicatedServer {
    public MinecraftDedicatedServerMixin(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
        super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/dedicated/MinecraftDedicatedServer.getSpawnProtectionRadius()I"), method = "isSpawnProtected")
    public int getVanillaSpawnProtection(@NotNull MinecraftDedicatedServer server, ServerWorld world, BlockPos pos, PlayerEntity player) {
        if (SewConfig.get(SewProtectionConfig.DISABLE_VANILLA_PROTECTION))
            return 0;
        return server.getSpawnProtectionRadius();
    }
}
