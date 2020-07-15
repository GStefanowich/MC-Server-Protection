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
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.ConstructableEntity;
import net.TheElm.project.interfaces.SleepingWorld;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.IntUtils;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.SleepUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
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
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class WorldSleep extends World implements SleepingWorld {
    
    // Shadows from ServerWorld
    @Shadow private List<ServerPlayerEntity> players;
    @Shadow private boolean allPlayersSleeping;
    @Shadow private native void resetWeather();
    
    protected WorldSleep(LevelProperties levelProperties_1, DimensionType dimensionType_1, BiFunction<World, Dimension, ChunkManager> biFunction_1, Profiler profiler_1, boolean boolean_1) {
        super(levelProperties_1, dimensionType_1, biFunction_1, profiler_1, boolean_1);
    }
    
    @Inject(at = @At("HEAD"), method = "tick")
    public void onTick(BooleanSupplier booleanSupplier, CallbackInfo callback) {
        // If Naturally Sleeping, Disabled, or Not enough Percentage
        int sleepingPercentage;
        if ( (!this.getDimension().canPlayersSleep()) // If non-sleeping dimension (End/Nether)
                || ( this.players.size() <= 0 ) // If nobody is online
                //|| this.allPlayersSleeping // If everyone is already sleeping (Vanilla default)
                || (!SewingMachineConfig.INSTANCE.DO_SLEEP_VOTE.get()) // If sleep voting is disabled
                || ( SewingMachineConfig.INSTANCE.SLEEP_PERCENT.get() <= 0 ) // If the required sleeping percentage is 0 (disabled)
                || ((sleepingPercentage = SleepUtils.getSleepingPercentage( this )) < SewingMachineConfig.INSTANCE.SLEEP_PERCENT.get() )
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
                this.properties.setClearWeatherTime(0);
                this.properties.setRainTime(IntUtils.random(this.random, 300, 1200) * 20);
                this.properties.setRaining(true);
                this.properties.setThunderTime(0);
                this.properties.setThundering(false);
            }
        }
        
        long worldDay = IntUtils.timeToDays(this);
        long worldYear = worldDay / SewingMachineConfig.INSTANCE.CALENDAR_DAYS.get();
        worldDay = worldDay - (worldYear * SewingMachineConfig.INSTANCE.CALENDAR_DAYS.get());
        
        NumberFormat formatter = NumberFormat.getInstance();
        String year = CasingUtils.Acronym(SewingMachineConfig.INSTANCE.CALENDAR_YEAR_EPOCH.get(), true);
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
            if ((chunkOwner = ChunkUtils.getPosOwner( this.getWorld(), entity.getBlockPos() )).isPresent())
                ((ConstructableEntity)entity).setEntityOwner( chunkOwner.get() );
            
            if (entity instanceof WitherEntity)
                CoreMod.logInfo("A new Wither Boss was summoned at " + MessageUtils.blockPosToString(entity.getBlockPos()));
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
