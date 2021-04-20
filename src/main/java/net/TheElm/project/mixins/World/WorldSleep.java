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

package net.TheElm.project.mixins.World;

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.interfaces.ConstructableEntity;
import net.TheElm.project.interfaces.SleepingWorld;
import net.TheElm.project.utilities.*;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class WorldSleep extends World implements SleepingWorld, ServerWorldAccess {
    // Shadows from ServerWorld
    @Shadow @Final private List<ServerPlayerEntity> players;
    @Shadow @Final private ServerWorldProperties worldProperties;
    @Shadow private boolean allPlayersSleeping;
    
    protected WorldSleep(MutableWorldProperties properties, RegistryKey<World> registryRef, final DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }
    
    @Shadow private native void resetWeather();
    @Shadow public native void setTimeOfDay(long timeOfDay);
    @Shadow public native DynamicRegistryManager getRegistryManager();
    
    @Inject(at = @At("HEAD"), method = "tick")
    public void onTick(BooleanSupplier booleanSupplier, CallbackInfo callback) {
        // If Naturally Sleeping, Disabled, or Not enough Percentage
        int sleepingPercentage;
        if ( (!this.getDimension().isBedWorking())
                || ( this.players.size() <= 0 ) // If nobody is online
                //|| this.allPlayersSleeping // If everyone is already sleeping (Vanilla default)
                || (!SewConfig.get(SewConfig.DO_SLEEP_VOTE)) // If sleep voting is disabled
                || ( SewConfig.get(SewConfig.SLEEP_PERCENT) <= 0 ) // If the required sleeping percentage is 0 (disabled)
                || ((sleepingPercentage = SleepUtils.getSleepingPercentage( this )) < SewConfig.get(SewConfig.SLEEP_PERCENT) )
        ) return;
        
        this.allPlayersSleeping = false;
        
        // Set the time back to day
        if (this.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
            long time = this.getTimeOfDay() + 24000L;
            this.setTimeOfDay(time - time % 24000L);
        }
        
        // Wake all players up
        this.players.stream().filter(LivingEntity::isSleeping).forEach((serverPlayerEntity_1) -> {
            serverPlayerEntity_1.wakeUp(false, false);
        });
        
        // Change the weather
        if (this.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
            boolean raining = this.properties.isRaining();
            boolean thunder = this.properties.isThundering();
            
            // If currently raining, end
            if (raining || thunder)
                this.resetWeather();
            else if (this.random.nextInt(40) <= 0) {
                // Random chance to start raining
                this.worldProperties.setClearWeatherTime(0);
                this.worldProperties.setRainTime(IntUtils.random(this.random, 300, 1200) * 20);
                this.worldProperties.setRaining(true);
                this.worldProperties.setThunderTime(0);
                this.worldProperties.setThundering(false);
            }
        }
        
        long worldDay = IntUtils.timeToDays(this);
        long worldYear = worldDay / SewConfig.get(SewConfig.CALENDAR_DAYS);
        worldDay = worldDay - (worldYear * SewConfig.get(SewConfig.CALENDAR_DAYS));
        
        NumberFormat formatter = NumberFormat.getInstance();
        String year = CasingUtils.Acronym(SewConfig.get(SewConfig.CALENDAR_YEAR_EPOCH), true);
        TitleUtils.showPlayerAlert((ServerWorld)(World) this,
            new LiteralText("Rise and shine! Day ")
                .append(formatter.format( worldDay ))
                .append(" of ")
                .append(formatter.format( worldYear ))
                .append(year.isEmpty() ? "" : " " + year)
                .append(" has begun.")
        );
    }
    
    @Inject(at = @At("TAIL"), method = "spawnEntity")
    public void onSpawnMob(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof ConstructableEntity) {
            Optional<UUID> chunkOwner;
            if ((chunkOwner = ChunkUtils.getPosOwner(this.toServerWorld(), entity.getBlockPos() )).isPresent())
                ((ConstructableEntity)entity).setEntityOwner( chunkOwner.get() );
            
            if (entity instanceof WitherEntity)
                CoreMod.logInfo("A new Wither Boss was summoned at " + MessageUtils.xyzToString(entity.getBlockPos()));
        }
    }
    
    @Inject(at = @At("TAIL"), method = "save")
    public void onWorldSave(@Nullable ProgressListener progressListener, boolean flush, boolean bl, CallbackInfo callback) {
        RegistryKey<World> key = this.getRegistryKey();
        
        // If the world should save it's properties to a different location
        if ((!key.equals(World.OVERWORLD)) && !bl && SewConfig.get(SewConfig.WORLD_SEPARATE_PROPERTIES)) {
            NbtUtils.writeWorldDat(
                this.toServerWorld(),
                this.getLevelProperties()
            );
        }
    }
    
    @Override
    public void updatePlayersSleeping() {
        this.allPlayersSleeping = false;
        
        if (!this.players.isEmpty()) {
            int spectators = 0;
            int sleeping = 0;
            
            Iterator players = this.players.iterator();
            
            while( players.hasNext() ) {
                ServerPlayerEntity serverPlayerEntity_1 = (ServerPlayerEntity)players.next();
                if (serverPlayerEntity_1.isSpectator()) {
                    ++spectators;
                } else if (serverPlayerEntity_1.isSleeping()) {
                    ++sleeping;
                }
            }
            
            // If ALL players are sleeping
            this.allPlayersSleeping = ( sleeping > 0 ) && ( sleeping >= ( this.players.size() - spectators ));
        }
    }
    
}
