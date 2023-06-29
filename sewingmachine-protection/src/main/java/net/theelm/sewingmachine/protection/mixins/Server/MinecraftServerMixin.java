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

package net.theelm.sewingmachine.protection.mixins.Server;

import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.protection.claims.Claimant;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created on Apr 14 2022 at 3:05 PM.
 * By greg in SewingMachineMod
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements ClaimsAccessor {
    private ClaimCache sewingMachineClaimManager;

    /**
     * Save claim information when the server saves
     */
    @Inject(at = @At("RETURN"), method = "save")
    public void save(boolean silent, boolean boolean_2, boolean boolean_3, @NotNull CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            ClaimCache claims = ((ClaimsAccessor)this).getClaimManager();

            if (!silent) CoreMod.logInfo("Saving claim data");
            claims.getCaches()
                .forEach(Claimant::save);
        }
    }
    
    @Override
    public @NotNull ClaimCache getClaimManager() {
        if (this.sewingMachineClaimManager == null) {
            MinecraftServer server = (MinecraftServer) (Object) this;
            this.sewingMachineClaimManager = new ClaimCache(server, server.getOverworld());
        }
        return this.sewingMachineClaimManager;
    }
}
