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
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.theelm.sewingmachine.permissions.objects.PlayerRank;
import net.theelm.sewingmachine.permissions.utilities.RankUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerListS2CPacket.Entry.class, priority = 0)
public abstract class PlayerListEntryMixin {
    @Shadow @Final private GameProfile profile;
    
    @Inject(at = @At("RETURN"), method = "displayName", cancellable = true)
    public void getDisplayName(CallbackInfoReturnable<Text> callback) {
        MutableText display = TextUtils.mutable(callback.getReturnValue());
        
        for (PlayerRank rank : RankUtils.getPlayerRanks(this.profile)) {
            Text name = rank.getDisplay();
            if (name != null) {
                // Open bracket
                display.append(Text.literal(" [").formatted(Formatting.WHITE)
                    .append(display)
                    .append("]"));
                break; // Only append one
            }
        }
        
        // Set the return value
        callback.setReturnValue(display);
    }
}
