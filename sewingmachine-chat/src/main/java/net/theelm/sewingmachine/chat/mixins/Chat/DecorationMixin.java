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

package net.theelm.sewingmachine.chat.mixins.Chat;

import net.minecraft.network.message.MessageType;
import net.minecraft.text.Decoration;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created on Jul 13 2023 at 3:26 PM.
 * By greg in sewingmachine
 */
@Mixin(Decoration.class)
public class DecorationMixin {
    @Inject(at = @At("RETURN"), method = "apply")
    public void onApply(Text content, MessageType.Parameters params, CallbackInfoReturnable<Text> callback) {
        System.out.println("[Decoration::apply] " + callback.getReturnValue().getString());
    }
    
    @Inject(at = @At("RETURN"), method = "collectArguments")
    public void onCollectArguments(Text content, MessageType.Parameters params, CallbackInfoReturnable<Text[]> callback) {
        Text[] texts = callback.getReturnValue();
        for (int i = 0; i < texts.length; i++) {
            Text text = texts[i];
            System.out.println("[Decoration::collectArguments." + i + "] " + text.getString());
        }
    }
    
    @Inject(at = @At("RETURN"), method = "translationKey")
    public void onGetTranslationKey(CallbackInfoReturnable<String> callback) {
        System.out.println("[Decoration::translationKey]" + callback.getReturnValue());
    }
}
