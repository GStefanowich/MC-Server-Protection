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

package net.theelm.sewingmachine.utilities.mod;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.objects.SewModules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
    
    public static @NotNull FabricLoader getFabric() {
        return FabricLoader.getInstance();
    }
    public static @NotNull ModContainer getMod(@NotNull String id) {
        return Sew.getFabric()
            .getModContainer(id)
            .orElseThrow(() -> new RuntimeException("Could not find mod container \"" + id + "\""));
    }
    public static boolean isClient() {
        return Sew.tryGetClient()
            .isPresent();
    }
    public static boolean isServer() {
        return Sew.tryGetServer()
            .isPresent() || Sew.getFabric().getEnvironmentType() == EnvType.SERVER;
    }
    
    /**
     * Server assets
     */
    
    /**
     * Gets the MinecraftServer
     *   If running as the server, will return the DedicatedMinecraftServer
     *   If running as the client, will return the IntegratedMinecraftServer
     * @return
     */
    public static @NotNull MinecraftServer getServer() {
        return Sew.tryGetServer()
            .orElseThrow(() -> new RuntimeException("Could not access game server here."));
    }
    
    /**
     * Gets the MinecraftServer
     *   If running as the server, will return the DedicatedMinecraftServer
     *   If running as the client, will return the IntegratedMinecraftServer
     * @return
     */
    public static @NotNull Optional<MinecraftServer> tryGetServer() {
        FabricLoader fabric = Sew.getFabric();
        Object instance = fabric.getGameInstance();
        return switch (fabric.getEnvironmentType()) {
            case CLIENT -> SewClient.tryGetServer();
            case SERVER -> instance instanceof MinecraftServer server ? Optional.of(server) : Optional.empty();
        };
    }
    
    public static @NotNull MinecraftServer getServer(@NotNull PlayerEntity player) {
        return Objects.requireNonNull(player.getServer());
    }
    
    public static @Nullable ServerPlayerEntity getPlayer(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        return server.getPlayerManager()
            .getPlayer(uuid);
    }
    
    /**
     * Client assets
     */
    
    /**
     * Gets the MinecraftServer
     *   If running as the server, will Exception
     *   If running as the client, will return the IntegratedMinecraftServer
     * @return
     */
    public static @NotNull MinecraftClient getClient() {
        return Sew.tryGetClient()
            .orElseThrow(() -> new RuntimeException("Could not access game client here."));
    }
    
    public static @NotNull Optional<MinecraftClient> tryGetClient() {
        FabricLoader fabric = Sew.getFabric();
        Object instance = fabric.getGameInstance();
        return switch (fabric.getEnvironmentType()) {
            case CLIENT -> instance instanceof MinecraftClient client ? Optional.of(client) : Optional.empty();
            case SERVER -> Optional.empty();
        };
    }
}
