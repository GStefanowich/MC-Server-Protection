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

package net.TheElm.project.mixins.Server;

import com.google.common.collect.ImmutableList;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.objects.DynamicLevelProperties;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.protections.logging.EventLogger;
import net.TheElm.project.utilities.DimensionUtils;
import net.TheElm.project.utilities.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.snooper.SnooperListener;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class Save extends ReentrantThreadExecutor<ServerTask> implements SnooperListener, CommandOutput, AutoCloseable, Runnable {
    
    @Shadow @Final
    protected LevelStorage.Session session;
    @Shadow @Final
    private Map<RegistryKey<World>, ServerWorld> worlds;
    @Shadow @Final
    private Executor workerExecutor;
    @Shadow
    protected SaveProperties saveProperties;
    
    public Save(String string_1) {
        super(string_1);
    }
    
    /**
     * Save claim information when the server saves
     */
    @Inject(at = @At("RETURN"), method = "save")
    public void save(boolean silent, boolean boolean_2, boolean boolean_3, @NotNull CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            if (!silent) CoreMod.logInfo("Saving claimed player data");
            CoreMod.getCacheStream(ClaimantPlayer.class)
                .forEach(Claimant::save);
            
            if (!silent) CoreMod.logInfo("Saving claimed town data");
            CoreMod.getCacheStream(ClaimantTown.class)
                .forEach(Claimant::save);
        }
    }
    
    /**
     * Override what world is loaded when the server first starts
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "prepareStartRegion")
    private ServerWorld onPreparingStartRegion(MinecraftServer server, WorldGenerationProgressListener worldGenerationProgressListener) {
        return server.getWorld(SewConfig.get(SewConfig.DEFAULT_WORLD));
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/util/registry/SimpleRegistry.getEntries()Ljava/util/Set;"), method = "createWorlds")
    private Set<Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions>> onSettingUpWorlds(SimpleRegistry<DimensionOptions> registry, WorldGenerationProgressListener worldGenListener) {
        Set<Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions>> worlds = registry.getEntries();
        if (SewConfig.get(SewConfig.WORLD_SEPARATE_PROPERTIES)) {
            Iterator<Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions>> iterator = worlds.iterator();
            GeneratorOptions genOptions = this.saveProperties.getGeneratorOptions();
            
            while( iterator.hasNext() ) {
                Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry = iterator.next();
                RegistryKey<DimensionOptions> registryKey = entry.getKey();
                if (registryKey != DimensionOptions.OVERWORLD) {
                    RegistryKey<World> key = RegistryKey.of(Registry.DIMENSION, registryKey.getValue());
                    
                    // Get dimension options
                    DimensionOptions options = entry.getValue();
                    DimensionType dimensionType = options.getDimensionType();
                    ChunkGenerator chunkGenerator = options.getChunkGenerator();
                    
                    // Create a dynamic world properties file
                    ServerWorld world;
                    DynamicLevelProperties worldProperties = new DynamicLevelProperties(this.saveProperties, this.saveProperties.getMainWorldProperties());
                    this.worlds.put(key, world = new ServerWorld(
                        (MinecraftServer)(ReentrantThreadExecutor)this,
                        this.workerExecutor,
                        this.session,
                        worldProperties,
                        key,
                        dimensionType,
                        worldGenListener,
                        chunkGenerator,
                        genOptions.isDebugWorld(),
                        BiomeAccess.hashSeed(genOptions.getSeed()),
                        ImmutableList.of(),
                        false
                    ));
                    
                    // Add the world border listener
                    DimensionUtils.addWorldBorderListener(world);
                    
                    // Load the levels properties
                    if (!NbtUtils.readWorldDat(world, worldProperties.getDynamicPropertyHandler()) && SewConfig.get(SewConfig.WORLD_SPECIFIC_WORLD_BORDER))
                        worldProperties.setWorldBorder(WorldBorder.DEFAULT_BORDER);
                }
            }
            
            return Collections.emptySet();
        }
        return worlds;
    }
    
    /**
     * 
     * @param callback Mixin Callback
     */
    @Inject(at = @At("TAIL"), method = "shutdown")
    public void shutdown(CallbackInfo callback) {
        EventLogger.stop();
    }
    
}
