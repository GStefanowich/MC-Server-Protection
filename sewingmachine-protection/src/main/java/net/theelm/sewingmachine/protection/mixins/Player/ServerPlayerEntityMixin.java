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

package net.theelm.sewingmachine.protection.mixins.Player;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.objects.PlayerVisitor;
import net.theelm.sewingmachine.protection.interfaces.PlayerTravel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Created on Jun 09 2023 at 4:34 AM.
 * By greg in sewingmachine
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements PlayerTravel {
    @Shadow public abstract ServerWorld getServerWorld();
    @Shadow public ServerPlayNetworkHandler networkHandler;
    
    private PlayerVisitor visitor = null;
    
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }
    
    @Override
    public ClaimantPlayer getClaim() {
        return ((PlayerTravel)this.networkHandler).getClaim();
    }
    
    @Override
    public @Nullable PlayerVisitor getLocation() {
        return this.visitor;
    }
    
    @Override
    public @NotNull PlayerVisitor updateLocation(@NotNull UUID uuid) {
        return this.swap(new PlayerVisitor((ServerPlayerEntity)(Object)this, uuid));
    }
    
    @Override
    public @NotNull PlayerVisitor updateLocation() {
        return this.swap(new PlayerVisitor((ServerPlayerEntity)(Object)this));
    }
    
    @Inject(at = @At("HEAD"), method = "shouldDamagePlayer", cancellable = true)
    public void shouldDamage(PlayerEntity entity, CallbackInfoReturnable<Boolean> callback) {
        IClaimedChunk chunk = (IClaimedChunk) this.getServerWorld().getWorldChunk(this.getBlockPos());
        
        // If player hurt themselves
        if ( this == entity )
            return;
        
        // If PvP is off, disallow
        if ( !chunk.isSetting(this.getBlockPos(), ClaimSettings.PLAYER_COMBAT) )
            callback.setReturnValue(false);
    }
    
    private PlayerVisitor swap(PlayerVisitor visitor) {
        if (this.visitor != null)
            this.visitor.exit();
        return this.visitor = visitor;
    }
}
