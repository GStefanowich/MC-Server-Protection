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
import net.TheElm.project.interfaces.LogicalWorld;
import net.TheElm.project.objects.DetachedTickable;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.IntUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class WorldSleep extends World implements LogicalWorld, ServerWorldAccess {
    private final @NotNull Queue<DetachedTickable> detachedTickableQueue = new ArrayDeque<>();
    private final @NotNull List<DetachedTickable> detachedEvents = new LinkedList<>();

    protected WorldSleep(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }


    @Shadow public native DynamicRegistryManager getRegistryManager();
    
    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "net/minecraft/server/world/ServerWorld.setTimeOfDay(J)V"), method = "tick")
    public void onTick(BooleanSupplier booleanSupplier, CallbackInfo callback) {
        long worldDay = IntUtils.timeToDays(this);
        long worldYear = worldDay / SewConfig.get(SewConfig.CALENDAR_DAYS);
        worldDay = worldDay - (worldYear * SewConfig.get(SewConfig.CALENDAR_DAYS));
        
        NumberFormat formatter = NumberFormat.getInstance();
        String year = CasingUtils.acronym(SewConfig.get(SewConfig.CALENDAR_YEAR_EPOCH), true);
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
    public void onSpawnMob(@NotNull Entity entity, @NotNull CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof ConstructableEntity constructableEntity) {
            Optional<UUID> chunkOwner;
            if ((chunkOwner = ChunkUtils.getPosOwner(this.toServerWorld(), entity.getBlockPos() )).isPresent())
                constructableEntity.setEntityOwner(chunkOwner.get());
            
            if (entity instanceof WitherEntity)
                CoreMod.logInfo("A new Wither Boss was summoned at " + MessageUtils.xyzToString(entity.getBlockPos()));
        }
    }
    
    @Inject(at = @At("RETURN"), method = "tickTime")
    public void onWorldTick(@NotNull CallbackInfo callback) {
        // Move any items from the queue
        DetachedTickable tickable;
        while ((tickable = this.detachedTickableQueue.poll()) != null)
            this.detachedEvents.add(tickable);
        
        // Iterate over the items in the event list
        Iterator<DetachedTickable> iterator = this.detachedEvents.iterator();
        while (iterator.hasNext()) {
            tickable = iterator.next();
            tickable.tick();
            if (tickable.isRemoved())
                iterator.remove();
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
    public @NotNull DetachedTickable addTickableEvent(@NotNull Predicate<DetachedTickable> predicate) {
        DetachedTickable tickable = new DetachedTickable((ServerWorld)(World) this, predicate);
        this.detachedTickableQueue.add(tickable);
        return tickable;
    }
}
