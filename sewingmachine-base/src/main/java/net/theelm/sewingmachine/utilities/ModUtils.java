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

package net.theelm.sewingmachine.utilities;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.interfaces.ModUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Created on Jul 03 2023 at 7:26 PM.
 * By greg in sewingmachine
 */
public final class ModUtils {
    private ModUtils() {}
    
    public static boolean hasModule(@NotNull PlayerEntity entity, @Nullable String module) {
        return entity instanceof ServerPlayerEntity player && ModUtils.hasModule(player, module);
    }
    public static boolean hasModule(@NotNull ServerPlayerEntity player, @Nullable String module) {
        if (module == null)
            return true;
        Collection<String> modules = ((ModUser) player).getModules();
        boolean modded = modules.contains(module);
        if (!modded)
            CoreMod.logDebug("Player is missing module \"" + module + "\"");
        return modded;
    }
}
