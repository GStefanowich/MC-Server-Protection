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

package net.theelm.sewingmachine.permissions.mixins.Server;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.permissions.objects.PlayerRank;
import net.theelm.sewingmachine.permissions.utilities.RankUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Created on Jun 28 2023 at 10:09 PM.
 * By greg in sewingmachine
 */
@Mixin(value = MinecraftServer.class, priority = 0)
public class MinecraftServerMixin {
    @Inject(at = @At("RETURN"), method = "createMetadataPlayers")
    public void getCustomPlayers(@NotNull CallbackInfoReturnable<ServerMetadata.Players> callback) {
        ServerMetadata.Players players = callback.getReturnValue();
        List<GameProfile> profiles = players == null ? null : players.sample();
        if (profiles == null)
            return;
        
        for (int i = 0; i < profiles.size(); i++) {
            GameProfile profile = profiles.get(i);
            String name = profile.getName();
            
            // If the player has any rank
            for (PlayerRank rank : RankUtils.getPlayerRanks(profile)) {
                Text display = rank.getDisplay();
                
                // Try to get the players first rank and append it to their name
                if (display != null) {
                    name += " [" + TextUtils.legacyConvert(display) + "]";
                    break;
                }
            }
            
            // Update the value with a new profile
            profiles.set(i, new GameProfile(
                profile.getId(),
                name
            ));
        }
    }
}
