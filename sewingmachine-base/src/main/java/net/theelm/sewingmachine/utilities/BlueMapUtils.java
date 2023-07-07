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

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.base.CoreMod;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Created on Mar 14 2021 at 3:37 PM.
 * By greg in SewingMachineMod
 */
public final class BlueMapUtils {
    private BlueMapUtils() {}
    public static void init() {}
    
    public static @NotNull Queue<Consumer<BlueMapAPI>> RUN = new ArrayDeque<>();
    
    static {
        // When the API is made available
        BlueMapAPI.onEnable((api) -> {
            CoreMod.logInfo("BlueMap Integration detected");
            
            Consumer<BlueMapAPI> consumer;
            while ((consumer = BlueMapUtils.RUN.poll()) != null)
                consumer.accept(api);
        });
    }
    
    public static Optional<BlueMapAPI> getBlueMap() {
        return BlueMapAPI.getInstance();
    }
    public static Optional<BlueMapMap> getWorldMap(@NotNull RegistryKey<World> worldKey) {
        return BlueMapUtils.getBlueMap().map(blueMapAPI -> {
            Identifier identifier = worldKey.getValue();
            String search = "world/dimensions/" + identifier.getNamespace() + "/" + identifier.getPath();
            for (BlueMapMap map : blueMapAPI.getMaps()) {
                BlueMapWorld world = map.getWorld();
                if (world.getSaveFolder().endsWith(search))
                    return map;
            }
            CoreMod.logError("Could not find BlueMapWorld \"" + search + "\"");
            return null;
        });
    }
}
