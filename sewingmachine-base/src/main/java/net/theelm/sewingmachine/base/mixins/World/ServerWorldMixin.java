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

package net.theelm.sewingmachine.base.mixins.World;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.interfaces.TickableContext;
import net.theelm.sewingmachine.objects.DetachedTickableContext;
import net.theelm.sewingmachine.utilities.CasingUtils;
import net.theelm.sewingmachine.utilities.IntUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements LogicalWorld, ServerWorldAccess {
    private final @NotNull Queue<DetachedTickableContext> detachedTickableQueue = new ArrayDeque<>();
    private final @NotNull List<DetachedTickableContext> detachedEvents = new LinkedList<>();

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "net/minecraft/server/world/ServerWorld.setTimeOfDay(J)V"), method = "tick")
    public void onTick(BooleanSupplier booleanSupplier, CallbackInfo callback) {
        long worldDay = IntUtils.timeToDays(this);
        long worldYear = worldDay / SewConfig.get(SewBaseConfig.CALENDAR_DAYS);
        worldDay = worldDay - (worldYear * SewConfig.get(SewBaseConfig.CALENDAR_DAYS));
        
        NumberFormat formatter = NumberFormat.getInstance();
        String year = CasingUtils.acronym(SewConfig.get(SewBaseConfig.CALENDAR_YEAR_EPOCH), true);
        TitleUtils.showPlayerAlert((ServerWorld)(World) this,
            Text.literal("Rise and shine! Day ")
                .append(formatter.format( worldDay ))
                .append(" of ")
                .append(formatter.format( worldYear ))
                .append(year.isEmpty() ? "" : " " + year)
                .append(" has begun.")
        );
    }
    
    @Inject(at = @At("RETURN"), method = "tickTime")
    public void onWorldTick(@NotNull CallbackInfo callback) {
        // Move any items from the queue
        DetachedTickableContext tickable;
        while ((tickable = this.detachedTickableQueue.poll()) != null)
            this.detachedEvents.add(tickable);
        
        // Iterate over the items in the event list
        Iterator<DetachedTickableContext> iterator = this.detachedEvents.iterator();
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
        if ((!key.equals(World.OVERWORLD)) && !bl && SewConfig.get(SewBaseConfig.WORLD_SEPARATE_PROPERTIES)) {
            NbtUtils.writeWorldDat(
                this.toServerWorld(),
                this.getLevelProperties()
            );
        }
    }
    
    @Override
    public @NotNull DetachedTickableContext addTickableEvent(@NotNull Predicate<TickableContext> predicate) {
        DetachedTickableContext tickable = new DetachedTickableContext((ServerWorld)(World) this, predicate);
        this.detachedTickableQueue.add(tickable);
        return tickable;
    }
}
