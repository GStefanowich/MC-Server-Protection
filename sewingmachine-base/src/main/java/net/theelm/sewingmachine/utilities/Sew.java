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

import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.objects.SewModules;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created on Jul 01 2023 at 2:26 PM.
 * By greg in sewingmachine
 */
public final class Sew {
    private Sew() {}
    
    /*
     * Mod Assets
     */
    
    public static @NotNull Identifier modIdentifier(@NotNull String key) {
        return new Identifier(SewModules.MODULE + ":" + key);
    }
    
    /*
     * Configurations
     */
    
    public static @NotNull File getConfDir() throws RuntimeException {
        // Get the directory
        final File config = Sew.getFabric()
            .getConfigDirectory();
        final File dir = new File(config, SewModules.MODULE);
        // Make sure the directory exists
        if (!(dir.exists() || dir.mkdirs()))
            throw new RuntimeException("Error accessing the config");
        // Return the directory
        return dir;
    }
    
    /*
     * Fabric Elements
     */
    
    public static Either<MinecraftServer, MinecraftClient> getGameInstance() {
        Object instance = Sew.getFabric()
            .getGameInstance();
        if (instance instanceof MinecraftServer server)
            return Either.left(server);
        if (instance instanceof MinecraftClient client)
            return Either.right(client);
        throw new RuntimeException("Could not access game instance.");
    }
    
    /**
     * Gets the game instance, preferring the Server
     * @return
     */
    public static Either<MinecraftServer, MinecraftClient> getServerInstance() {
        Object instance = Sew.getFabric()
            .getGameInstance();
        if (instance instanceof MinecraftClient client) {
            MinecraftServer server = client.getServer();
            if (server != null)
                return Either.left(server);
            return Either.right(client);
        }
        if (instance instanceof MinecraftServer server)
            return Either.left(server);
        throw new RuntimeException("Could not access game instance.");
    }
    
    public static @NotNull FabricLoader getFabric() {
        return FabricLoader.getInstance();
    }
    public static @NotNull ModContainer getMod(@NotNull String id) {
        return Sew.getFabric()
            .getModContainer(id)
            .orElseThrow(() -> new RuntimeException("Could not find mod container \"" + id + "\""));
    }
    public static boolean isClient() {
        return Sew.getFabric().getEnvironmentType() == EnvType.CLIENT;
    }
    public static boolean isServer() {
        return Sew.getFabric().getEnvironmentType() == EnvType.SERVER;
    }
}
