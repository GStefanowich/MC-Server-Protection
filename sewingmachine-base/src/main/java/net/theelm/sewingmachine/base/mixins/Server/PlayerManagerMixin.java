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

import com.mojang.authlib.GameProfile;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.PlayerManager;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.base.objects.WanderingTraderPacket;
import net.theelm.sewingmachine.utilities.DimensionUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.TeamUtils;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    
    @Shadow @Final
    private MinecraftServer server;
    
    /**
     * Prevent players from joining the server if an update is running
     */
    @Inject(at = @At("RETURN"), method = "onPlayerConnect")
    public void afterOnPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo callback) {
        TeamUtils.applyTeams(player);
        
        // Get the world that the wandering trader spawns in
        ServerWorld world = this.server.getWorld(SewConfig.get(SewBaseConfig.WANDERING_TRADER_FORCE_SPAWN_WORLD));
        if (world != null) {
            // Check if the wandering trader is still in the world
            UUID uuid = EntityUtils.getWanderingTraderId(this.server);
            Entity entity = world.getEntity(uuid);
            if (entity != null)
                player.networkHandler.sendPacket(WanderingTraderPacket.of(entity));
        }
    }
    
    @Inject(at = @At("HEAD"), method = "setMainWorld", cancellable = true)
    public void beforeSetMainWorld(ServerWorld world, CallbackInfo callback) {
        if (!SewConfig.get(SewBaseConfig.WORLD_SPECIFIC_WORLD_BORDER))
            return;
        DimensionUtils.addWorldBorderListener(world);
        callback.cancel();
    }
    
    @Inject(at = @At("TAIL"), method = "sendWorldInfo")
    public void afterSendWorldInfo(@NotNull ServerPlayerEntity player, ServerWorld world, CallbackInfo callback) {
        if (player.networkHandler != null) {
            // Send all of the players abilities when world changed
            player.sendAbilitiesUpdate();
            
            // Send the player their XP information
            player.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
            
            // Resend the backpack after changing worlds since it doesn't copy
            InventoryUtils.resendBackpack(player);
        }
    }
    
    /**
     * Send the player the data about the world THEY ARE IN, not THE MAIN WORLD
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "sendWorldInfo")
    public ServerWorld onSendPlayerWorldData(@NotNull MinecraftServer server, ServerPlayerEntity player, ServerWorld world) {
        return world;
    }
    
    /**
     * Override the "Default World" when players are created
     * @param server The server
     * @param profile Game profile
     * @return The default world
     */
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "createPlayer")
    public ServerWorld getFirstDefaultWorld(@NotNull MinecraftServer server, @NotNull GameProfile profile) {
        return server.getWorld(SewConfig.get(SewBaseConfig.DEFAULT_WORLD));
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/MinecraftServer.getOverworld()Lnet/minecraft/server/world/ServerWorld;"), method = "respawnPlayer")
    public ServerWorld getRespawnDefaultWorld(@NotNull MinecraftServer server, @NotNull ServerPlayerEntity player, boolean alive) {
        return server.getWorld(SewConfig.get(SewBaseConfig.DEFAULT_WORLD));
    }
    
    /**
     * Override the "Default Starting World" where players spawn at
     * @param connection The connection
     * @param player The player
     * @return Return the default world
     */
    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;OVERWORLD:Lnet/minecraft/registry/RegistryKey;"), method = "onPlayerConnect")
    public RegistryKey<World> getStartingDefaultWorld(@NotNull ClientConnection connection, @NotNull ServerPlayerEntity player) {
        return SewConfig.get(SewBaseConfig.DEFAULT_WORLD);
    }
    
    // TODO: Respawn particles?
    /*@Redirect(at = @At(value = "INVOKE", target = "net/minecraft/server/network/ServerPlayerEntity.onSpawn()V"), method = "respawnPlayer")
    public void respawnPlayer(@NotNull ServerPlayerEntity respawned, @NotNull ServerPlayerEntity player, boolean alive) {
        if (!alive && !respawned.notInAnyWorld)
            EffectUtils.particleSwirl(ParticleTypes.HAPPY_VILLAGER, respawned, false, 10);
        respawned.onSpawn();
    }*/
}
