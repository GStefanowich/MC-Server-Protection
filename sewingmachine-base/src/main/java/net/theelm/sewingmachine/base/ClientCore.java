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

package net.theelm.sewingmachine.base;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.MinecraftServer;
import net.theelm.sewingmachine.base.objects.SewBasePackets;
import net.theelm.sewingmachine.utilities.KeybindUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ClientCore extends CoreMod implements ClientModInitializer {
    private static final KeyBinding backpackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.sewingmachine.backpack",
        InputUtil.Type.KEYSYM,
        InputUtil.GLFW_KEY_B,
        KeyBinding.INVENTORY_CATEGORY
    ));
    
    /*
     * Mod initializer
     */
    @Override
    public void onInitializeClient() {
        KeybindUtils.register(backpackKey, client -> {
            if (client.currentScreen == null)
                NetworkingUtils.send(client, SewBasePackets.BACKPACK_OPEN);
        });
    }
    
    @NotNull
    public static MinecraftClient get() {
        return CoreMod.getGameInstance()
            .right()
            .orElseThrow(() -> new RuntimeException("Called Client object from illegal position."));
    }
    
    @NotNull
    public static MinecraftServer getServer() {
        return Objects.requireNonNull(ClientCore.get().getServer());
    }
}
