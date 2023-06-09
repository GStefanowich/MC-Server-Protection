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

import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.objects.WanderingTraderProfileCollection;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WanderingTraderManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.spawner.Spawner;
import net.minecraft.world.level.ServerWorldProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderManager.class)
public abstract class WanderingTraderManagerMixin implements Spawner {
    
    @Shadow @Final private ServerWorldProperties properties;
    @Shadow @Final private Random random;
    
    @Shadow
    private native void spawnLlama(@NotNull ServerWorld serverWorld, @NotNull WanderingTraderEntity wanderingTraderEntity, int i);
    @Shadow
    private native @Nullable BlockPos getNearbySpawnPos(@NotNull WorldView worldView, @NotNull BlockPos blockPos, int i);
    @Shadow
    private native boolean doesNotSuffocateAt(@NotNull BlockView blockView, @NotNull BlockPos blockPos);
    
    @Inject(at = @At("HEAD"), method = "trySpawn", cancellable = true)
    private void onAttemptTraderSpawn(@NotNull ServerWorld world, @NotNull CallbackInfoReturnable<Boolean> callback) {
        if (!SewConfig.get(SewCoreConfig.WANDERING_TRADER_FORCE_SPAWN))
            return;
        callback.setReturnValue( // Try spawning the wandering trader
            this.attemptWanderingTraderSpawn(world)
        );
    }
    
    public boolean attemptWanderingTraderSpawn(@NotNull ServerWorld world) {
        if (this.random.nextInt(10) != 0)
            return false;
        
        MinecraftServer server = world.getServer();
        RegistryKey<World> worldKey = SewConfig.get(SewCoreConfig.WANDERING_TRADER_FORCE_SPAWN_WORLD);
        
        // Get the world the trader should spawn in
        if (!world.getRegistryKey().equals(worldKey))
            world = server.getWorld(worldKey);
        if (world == null) {
            CoreMod.logError("Could not spawn \"Wandering Trader\", world \"" + worldKey.getValue() + "\" not found.");
            return true;
        }
        
        // Check if the server has players
        PlayerManager players = server.getPlayerManager();
        if (players.getPlayerList().isEmpty())
            return true;
        
        BlockPos forcedPos = SewConfig.get(SewCoreConfig.WANDERING_TRADER_FORCE_SPAWN_POS);
        BlockPos nearbyPos = this.getNearbySpawnPos(world, forcedPos, 4);
        if (nearbyPos != null && this.doesNotSuffocateAt(world, nearbyPos)) {
            // Create a wandering trader
            WanderingTraderEntity trader = EntityType.WANDERING_TRADER.spawn(
                world,
                null,
                entity -> entity.setCustomName(Text.literal("Wandering Trader").styled(style -> style.withColor(Formatting.GOLD))),
                nearbyPos,
                SpawnReason.EVENT,
                false,
                false
            );
            
            if (trader != null) {
                // Set the custom name as visible from anywhere
                trader.setCustomNameVisible(true);
                trader.setInvulnerable(true);
                
                // Spawn llamas around the trader
                for (int i = 0; i < 2; i++)
                    this.spawnLlama(world, trader, 4);
                
                // Set the ID
                this.properties.setWanderingTraderId(trader.getUuid());
                
                // Change when the trader will despawn
                trader.setDespawnDelay(48000);
                trader.setWanderTarget(forcedPos);
                trader.setPositionTarget(forcedPos, 16);
                
                // Light nearby campfire
                if (SewConfig.get(SewCoreConfig.WANDERING_TRADER_CAMPFIRES))
                    BlockUtils.igniteNearbyLightSources(world, trader.getBlockPos());
                
                // Announce the wandering trader
                if (SewConfig.get(SewCoreConfig.ANNOUNCE_WANDERING_TRADER))
                    EntityUtils.wanderingTraderArrival(trader);
                
                // Add the wandering trader to the players list
                server.getPlayerManager()
                    .sendToAll(new WanderingTraderProfileCollection(trader).getPacket(PlayerListS2CPacket.Action.ADD_PLAYER));
                
                return true;
            }
        }
        
        return false;
    }
}
