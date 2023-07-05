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

package net.theelm.sewingmachine.protection.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.screens.SettingScreen;
import net.theelm.sewingmachine.screens.SettingScreenListWidget;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ProtectionScreen extends SettingScreen {
    public ProtectionScreen() {
        super(Text.literal("Protections"));
    }
    
    @Override
    protected void addButtons(@NotNull SettingScreenListWidget list) {
        list.addScreenButton(Text.literal("Claim Settings"), Text.literal("Settings for your Player claim"), () -> {
            SettingScreen screen = new ProtectionSettingsScreen();
            screen.parent = this;
            return screen;
        });
        list.addScreenButton(Text.literal("Claim Permissions"), Text.literal("Permissions for your Player claim"), () -> {
            SettingScreen screen = new PermissionSettingsScreen();
            screen.parent = this;
            return screen;
        });
        if (!this.client.isInSingleplayer() || this.client.isIntegratedServerRunning()) {
            list.addScreenButton(Text.literal("Claim Ranks"), Text.literal("The permissions for other players"), () -> {
                SettingScreen screen = new RankSettingsScreen();
                screen.parent = this;
                return screen;
            });
        }
    }
}
