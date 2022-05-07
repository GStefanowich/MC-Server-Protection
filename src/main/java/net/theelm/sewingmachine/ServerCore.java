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

package net.theelm.sewingmachine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.theelm.sewingmachine.commands.AdminCommands;
import net.theelm.sewingmachine.commands.BackpackCommand;
import net.theelm.sewingmachine.commands.ChatroomCommands;
import net.theelm.sewingmachine.commands.ClaimCommand;
import net.theelm.sewingmachine.commands.DateCommand;
import net.theelm.sewingmachine.commands.FireworksCommand;
import net.theelm.sewingmachine.commands.GameModesCommand;
import net.theelm.sewingmachine.commands.GiveSelfCommand;
import net.theelm.sewingmachine.commands.HeadCommand;
import net.theelm.sewingmachine.commands.HoldingCommand;
import net.theelm.sewingmachine.commands.LoggingCommand;
import net.theelm.sewingmachine.commands.MiscCommands;
import net.theelm.sewingmachine.commands.ModCommands;
import net.theelm.sewingmachine.commands.ModsCommand;
import net.theelm.sewingmachine.commands.MoneyCommand;
import net.theelm.sewingmachine.commands.NickNameCommand;
import net.theelm.sewingmachine.commands.PermissionCommand;
import net.theelm.sewingmachine.commands.PlayerSpawnCommand;
import net.theelm.sewingmachine.commands.RideCommand;
import net.theelm.sewingmachine.commands.RulerCommand;
import net.theelm.sewingmachine.commands.ScoreboardCommand;
import net.theelm.sewingmachine.commands.SpawnerCommand;
import net.theelm.sewingmachine.commands.StatsCommand;
import net.theelm.sewingmachine.commands.TagUserCommand;
import net.theelm.sewingmachine.commands.DebugCommands;
import net.theelm.sewingmachine.commands.TeleportsCommand;
import net.theelm.sewingmachine.commands.WaystoneCommand;
import net.theelm.sewingmachine.commands.WhereCommand;
import net.theelm.sewingmachine.commands.WhitelistTree;
import net.theelm.sewingmachine.commands.WorldCommand;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protections.events.BlockBreak;
import net.theelm.sewingmachine.protections.events.BlockInteraction;
import net.theelm.sewingmachine.protections.events.EntityAttack;
import net.theelm.sewingmachine.protections.events.ItemPlace;
import net.theelm.sewingmachine.protections.events.ItemUse;
import net.theelm.sewingmachine.protections.logging.EventLogger;
import net.theelm.sewingmachine.utilities.DevUtils;
import net.theelm.sewingmachine.utilities.MapUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.SharedConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class ServerCore extends CoreMod implements DedicatedServerModInitializer {
    /*
     * Mod initializer
     */
    @Override
    public void onInitializeServer() {
        super.initialize();
        
        /*StringBuilder builder = new StringBuilder();
        BlockPosChainLink cube = new BlockPosChainLink(new ChunkPos(0, 0))
            .addChunk(new ChunkPos(0, 1))
            .addChunk(new ChunkPos(1, 0))
            .addChunk(new ChunkPos(1, 1));
        BlockPosChainLink chain = new BlockPosChainLink(new ChunkPos(1, 2))
            .addChain(cube);
        
        System.out.println("Size: " + chain.size());
        chain.forEach(link -> {
            builder.append("(" + link.pos.getX() + "," + link.pos.getZ() + ")");
        });
        System.out.println(builder);*/
        
        if ( DevUtils.isDebugging() )
            SharedConstants.isDevelopment = true;
        
        // Register the server commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            CoreMod.logInfo("Registering our commands.");
            AdminCommands.register(dispatcher);
            BackpackCommand.register(dispatcher);
            ChatroomCommands.register(dispatcher);
            ClaimCommand.register(dispatcher);
            DateCommand.register(dispatcher);
            FireworksCommand.register(dispatcher);
            GameModesCommand.register(dispatcher);
            GiveSelfCommand.register(dispatcher);
            HeadCommand.register(dispatcher);
            HoldingCommand.register(dispatcher);
            LoggingCommand.register(dispatcher);
            MiscCommands.register(dispatcher);
            ModCommands.register(dispatcher);
            ModsCommand.register(dispatcher);
            MoneyCommand.register(dispatcher);
            NickNameCommand.register(dispatcher);
            PermissionCommand.register(dispatcher);
            PlayerSpawnCommand.register(dispatcher);
            RideCommand.register(dispatcher);
            RulerCommand.register(dispatcher);
            SpawnerCommand.register(dispatcher);
            StatsCommand.register(dispatcher);
            TagUserCommand.register(dispatcher);
            TeleportsCommand.register(dispatcher);
            WaystoneCommand.register(dispatcher);
            WhereCommand.register(dispatcher);
            WhitelistTree.register(dispatcher);
            WorldCommand.register(dispatcher);
            
            ScoreboardCommand.modify(dispatcher);
            
            if ( DevUtils.isDebugging() )
                DebugCommands.register(dispatcher);
        });
        
        // Create registry based listeners
        BlockBreak.init();
        BlockInteraction.init();
        EntityAttack.init();
        ItemPlace.init();
        ItemUse.init();
        
        MapUtils.init();
        
        CoreMod.logInfo("Initializing Database.");
        try {
            // Initialize the database
            if (CoreMod.initDB()) {
                CoreMod.logInfo("Database initialization finished");
                
                // Clear out old logs
                EventLogger.doCleanup();
                
                // Start the logger
                EventLogger.start();
            } else {
                CoreMod.logInfo( "Skipping Database Initialization (Unused)" );
            }
        } catch (SQLException e) {
            CoreMod.logInfo( "Error executing MySQL Database setup." );
            
            throw new RuntimeException( "Could not connect to database server.", e );
        }
        
        // Update the mod version in config
        try {
            SewConfig.set(SewConfig.CONFIG_VERSION, ConfigOption.convertToJSON(CoreMod.getModVersion()));
            SewConfig.save();
            
            // Alert the mod presence
            CoreMod.logInfo("Finished loading.");
        } catch (IOException e) {
            CoreMod.logError("Error during startup", e);
        }
    }
    
    public static @NotNull MinecraftServer get() {
        return CoreMod.getGameInstance()
            .left()
            .orElseGet(ClientCore::getServer);
    }
    public static @NotNull MinecraftServer get(@NotNull PlayerEntity player) {
        return Objects.requireNonNull(player.getServer());
    }
    public static @Nullable ServerPlayerEntity getPlayer(@NotNull MinecraftServer server, @NotNull UUID uuid) {
        return server.getPlayerManager().getPlayer(uuid);
    }
    
    public static @NotNull RegistryKey<World> defaultWorldKey() {
        return SewConfig.get(SewConfig.DEFAULT_WORLD);
    }
    
    public static LiteralCommandNode<ServerCommandSource> register(@NotNull final CommandDispatcher<ServerCommandSource> dispatcher, @NotNull final String command, @NotNull final Consumer<LiteralArgumentBuilder<ServerCommandSource>> consumer) {
        return ServerCore.register(dispatcher, command, command, consumer);
    }
    public static LiteralCommandNode<ServerCommandSource> register(@NotNull final CommandDispatcher<ServerCommandSource> dispatcher, @NotNull final String command, @NotNull final String descriptive, @NotNull final Consumer<LiteralArgumentBuilder<ServerCommandSource>> consumer) {
        final String display = command.toLowerCase(Locale.ROOT);
        
        // Build the literal using the name
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal(command.toLowerCase().replace(" ", "-"));
        
        // Apply the builder
        consumer.accept(builder);
        
        // Register the command
        LiteralCommandNode<ServerCommandSource> node = dispatcher.register(builder);
        
        // Log the command registration
        CoreMod.logDebug("- Registered " + (descriptive.isEmpty() || descriptive.equalsIgnoreCase(command) ? "/" + display : descriptive.toLowerCase(Locale.ROOT) + " [/" + display + "]") + " command");
        
        return node;
    }
    
    public static @NotNull BlockPos getSpawn(@NotNull World world) {
        // Get the forced position of TheEnd
        if ((world instanceof ServerWorld serverWorld) && (world.getRegistryKey() == World.END) && (!SewConfig.get(SewConfig.WORLD_SPECIFIC_SPAWN))) {
            BlockPos pos = serverWorld.getSpawnPos();
            // Only if the forced position is set
            if (pos != null)
                return pos;
        }
        
        // Get the level properties
        WorldProperties properties = world.getLevelProperties();
        
        // Reset the blockpos using the properties
        return new BlockPos(
            properties.getSpawnX(),
            properties.getSpawnY(),
            properties.getSpawnZ()
        );
    }
    public static @NotNull BlockPos getSpawn(@NotNull MinecraftServer server, @NotNull RegistryKey<World> world) {
        return ServerCore.getSpawn(ServerCore.getWorld(server, world));
    }
    public static @NotNull ServerWorld getWorld(@NotNull Entity entity, @NotNull RegistryKey<World> key) {
        return ServerCore.getWorld(Objects.requireNonNull(entity.getServer()), key);
    }
    public static @NotNull ServerWorld getWorld(@NotNull MinecraftServer server, @NotNull RegistryKey<World> key) {
        Optional<ServerWorld> world = Optional.ofNullable(server.getWorld(key));
        if (!world.isPresent())
            world = Optional.ofNullable(server.getWorld(World.OVERWORLD));
        return world.orElseThrow(NullPointerException::new);
    }
    
    public static boolean isIntegratedServer() {
        return ServerCore.get() instanceof IntegratedServer;
    }
    public static boolean isDedicatedServer() {
        return ServerCore.get() instanceof MinecraftDedicatedServer;
    }
}
