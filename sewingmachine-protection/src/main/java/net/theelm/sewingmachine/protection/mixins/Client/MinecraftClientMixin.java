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

package net.theelm.sewingmachine.protection.mixins.Client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.interfaces.NameCache;
import net.theelm.sewingmachine.protection.interfaces.ClientClaimData;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(MinecraftClient.class)
@Environment(EnvType.CLIENT)
public abstract class MinecraftClientMixin implements ClientClaimData, PlayerClaimData, NameCache {
    @Shadow @Nullable public ClientWorld world;
    @Shadow public abstract Session getSession();
    
    private final @NotNull Map<UUID, Text> namesCache = new HashMap<>();
    
    private @Nullable ClaimantPlayer playerClaims;
    private int claimedChunks;
    private int maximumChunks;
    
    @Inject(at = @At("RETURN"), method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V")
    private void onDisconnect(Screen screen, CallbackInfo callback) {
        this.namesCache.clear();
        
        // Reset claim information after disconnect
        this.playerClaims = null;
        this.claimedChunks = 0;
        this.maximumChunks = 0;
    }
    
    @Override
    public @NotNull ClaimantPlayer getClaim() {
        if (this.playerClaims == null) {
            UUID uuid = this.getSession()
                .getUuidOrNull();
            if (uuid == null)
                uuid = CoreMod.SPAWN_ID;
            this.playerClaims = new ClaimantPlayer(
                ((ClaimsAccessor) this.world).getClaimManager(),
                uuid
            );
        }
        return this.playerClaims;
    }
    
    @Override
    public boolean hasClaim() {
        return this.playerClaims != null;
    }
    
    @Override
    public @Nullable Text getPlayerName(@NotNull UUID uuid) {
        return this.namesCache.get(uuid);
    }
    
    @Override
    public void setPlayerName(@NotNull UUID uuid, @NotNull Text name) {
        this.namesCache.put(uuid, name);
    }
    
    @Override
    public int getMaxChunks() {
        return this.maximumChunks;
    }
    
    @Override
    public void setMaximumChunks(int maximum) {
        this.maximumChunks = maximum;
    }
    
    @Override
    public int getClaimedChunks() {
        return this.claimedChunks;
    }
    
    @Override
    public void setClaimedChunks(int claimed) {
        this.claimedChunks = claimed;
    }
}
