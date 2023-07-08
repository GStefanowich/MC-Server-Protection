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
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.MinecraftServer;
import net.theelm.sewingmachine.base.objects.inventory.BackpackScreen;
import net.theelm.sewingmachine.base.packets.PlayerBackpackDataPacket;
import net.theelm.sewingmachine.base.packets.PlayerBackpackOpenPacket;
import net.theelm.sewingmachine.base.packets.SewConfigPacket;
import net.theelm.sewingmachine.base.packets.SewHelloPacket;
import net.theelm.sewingmachine.events.TabRegisterEvent;
import net.theelm.sewingmachine.base.objects.inventory.BackpackTab;
import net.theelm.sewingmachine.base.objects.inventory.InventoryTab;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.utilities.KeybindUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;

import java.util.Map;

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
                NetworkingUtils.send(client, new PlayerBackpackOpenPacket());
        });
        
        // Get server modules
        NetworkingUtils.clientReceiver(SewHelloPacket.TYPE, (client, network, packet, sender) -> {
            // Match the client modules against the ones received by the server
            Map<String, String> emit = this.matchPluginMetadata(packet.modules());
            
            // Emit the modules that the client has
            NetworkingUtils.send(network, new SewHelloPacket(emit));
        });
        
        // Set the backpack data when received from the server
        NetworkingUtils.clientReceiver(PlayerBackpackDataPacket.TYPE, (client, network, packet, sender) -> {
            ((BackpackCarrier) client.player)
                .setBackpack(packet.getBackpack(client.player), false /* Nothing here to transmit */);
        });
        
        // Set handler for receiving the Config
        NetworkingUtils.clientReceiver(SewConfigPacket.TYPE, NetworkingUtils::empty);
        
        // Prefix tabs to make sure that they're first
        TabRegisterEvent.register(0, BackpackTab::new);
        TabRegisterEvent.register(0, InventoryTab::new);
        
        // Register the screen
        HandledScreens.register(CoreMod.BACKPACK, BackpackScreen::new);
    }
}
