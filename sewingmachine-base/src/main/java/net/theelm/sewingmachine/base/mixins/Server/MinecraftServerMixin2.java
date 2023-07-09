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

package net.theelm.sewingmachine.base.mixins.Server;

import com.google.common.collect.ImmutableList;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.objects.DynamicLevelProperties;
import net.theelm.sewingmachine.objects.SewModules;
import net.theelm.sewingmachine.protections.logging.EventLogger;
import net.theelm.sewingmachine.utilities.DimensionUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin2 extends ReentrantThreadExecutor<ServerTask> implements CommandOutput, AutoCloseable {
    
    @Shadow @Final
    protected LevelStorage.Session session;
    @Shadow @Final
    private Map<RegistryKey<World>, ServerWorld> worlds;
    @Shadow @Final
    private Executor workerExecutor;
    @Shadow
    protected SaveProperties saveProperties;
    
    @Shadow public abstract @Nullable ServerWorld getWorld(RegistryKey<World> key);
    
    public MinecraftServerMixin2(String string_1) {
        super(string_1);
    }
    
    /**
     * Override what world is loaded when the server first starts
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "prepareStartRegion")
    private ServerWorld onPreparingStartRegion(@NotNull MinecraftServer server, WorldGenerationProgressListener worldGenerationProgressListener) {
        RegistryKey<World> registryKey = SewConfig.get(SewBaseConfig.DEFAULT_WORLD);
        Identifier identifier = registryKey.getValue();
        ServerWorld world = server.getWorld(registryKey);
        if (world == null)
            throw new NullPointerException("Could not find default world \"" + identifier.getNamespace() + ":" + identifier.getPath() + "\", please update " + SewModules.MODULE + "/config.json");
        return world;
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/Registry;getEntrySet()Ljava/util/Set;"), method = "createWorlds")
    private Set<Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions>> onSettingUpWorlds(Registry<DimensionOptions> registry, WorldGenerationProgressListener worldGenListener) {
        Set<Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions>> worlds = registry.getEntrySet();
        
        // If we're doing world-specific gamerules and options
        if (SewConfig.get(SewBaseConfig.WORLD_SEPARATE_PROPERTIES)) {
            Iterator<Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions>> iterator = worlds.iterator();
            GeneratorOptions genOptions = this.saveProperties.getGeneratorOptions();
            
            ServerWorld primary = this.getWorld(World.OVERWORLD);
            
            while( iterator.hasNext() ) {
                Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry = iterator.next();
                RegistryKey<DimensionOptions> registryKey = entry.getKey();
                if (registryKey != DimensionOptions.OVERWORLD) {
                    RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, registryKey.getValue());
                    
                    // Create a dynamic world properties file
                    ServerWorld world;
                    DynamicLevelProperties worldProperties = new DynamicLevelProperties(this.saveProperties, this.saveProperties.getMainWorldProperties());
                    this.worlds.put(key, world = new ServerWorld(
                        (MinecraftServer)(ReentrantThreadExecutor)this,
                        this.workerExecutor,
                        this.session,
                        worldProperties,
                        key,
                        entry.getValue(),
                        worldGenListener,
                        this.saveProperties.isDebugWorld(),
                        BiomeAccess.hashSeed(genOptions.getSeed()),
                        ImmutableList.of(),
                        false,
                        primary.getRandomSequences()
                    ));
                    
                    // Add the world border listener
                    DimensionUtils.addWorldBorderListener(world);
                    
                    // Load the levels properties
                    if (!NbtUtils.readWorldDat(world, worldProperties.getDynamicPropertyHandler()) && SewConfig.get(SewBaseConfig.WORLD_SPECIFIC_WORLD_BORDER))
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
