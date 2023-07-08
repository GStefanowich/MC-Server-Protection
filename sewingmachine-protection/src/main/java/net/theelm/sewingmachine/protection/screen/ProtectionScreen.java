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

import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class ProtectionScreen extends SettingScreen {
    public ProtectionScreen(@NotNull Text title) {
        super(title);
    }
    
    @Override
    protected void addButtons(@NotNull SettingScreenListWidget list) {
        this.addTranslatedScreenButton(list, ProtectionClaimScreen::new, "claim_map");
        this.addTranslatedScreenButton(list, ProtectionSettingsScreen::new, "claim_settings");
        this.addTranslatedScreenButton(list, PermissionSettingsScreen::new, "claim_permissions");
        if (!this.client.isInSingleplayer() || this.client.isIntegratedServerRunning()) {
            this.addTranslatedScreenButton(list, RankSettingsScreen::new, "claim_ranks");
        }
    }
    
    private void addTranslatedScreenButton(@NotNull SettingScreenListWidget list, @NotNull Function<Text, SettingScreen> supplier, @NotNull String key) {
        this.addScreenButton(list, supplier, Text.translatable("ui.sew.settings." + key), Text.translatable("ui.sew.settings.tooltip." + key));
    }
    
    private void addScreenButton(@NotNull SettingScreenListWidget list, @NotNull Function<Text, SettingScreen> supplier, @NotNull Text text, @NotNull Text tooltip) {
        list.addScreenButton(text, tooltip, () -> {
            SettingScreen screen = supplier.apply(text);
            screen.parent = this;
            return screen;
        });
    }
}
