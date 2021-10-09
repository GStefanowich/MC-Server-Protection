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

package net.TheElm.project;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.TheElm.project.blocks.entities.LecternGuideBlockEntity;
import net.TheElm.project.blocks.entities.LecternWarpsBlockEntity;
import net.TheElm.project.commands.AdminCommands;
import net.TheElm.project.commands.BackpackCommand;
import net.TheElm.project.commands.ChatroomCommands;
import net.TheElm.project.commands.ClaimCommand;
import net.TheElm.project.commands.DateCommand;
import net.TheElm.project.commands.FireworksCommand;
import net.TheElm.project.commands.GameModesCommand;
import net.TheElm.project.commands.GiveSelfCommand;
import net.TheElm.project.commands.HeadCommand;
import net.TheElm.project.commands.HoldingCommand;
import net.TheElm.project.commands.LoggingCommand;
import net.TheElm.project.commands.MiscCommands;
import net.TheElm.project.commands.ModCommands;
import net.TheElm.project.commands.ModsCommand;
import net.TheElm.project.commands.MoneyCommand;
import net.TheElm.project.commands.NickNameCommand;
import net.TheElm.project.commands.PermissionCommand;
import net.TheElm.project.commands.PlayerSpawnCommand;
import net.TheElm.project.commands.RideCommand;
import net.TheElm.project.commands.RulerCommand;
import net.TheElm.project.commands.ScoreboardCommand;
import net.TheElm.project.commands.SpawnerCommand;
import net.TheElm.project.commands.StatsCommand;
import net.TheElm.project.commands.TagUserCommand;
import net.TheElm.project.commands.DebugCommands;
import net.TheElm.project.commands.TeleportsCommand;
import net.TheElm.project.commands.WaystoneCommand;
import net.TheElm.project.commands.WhereCommand;
import net.TheElm.project.commands.WorldCommand;
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.protections.claiming.LinkedPointChunk;
import net.TheElm.project.protections.events.BlockBreak;
import net.TheElm.project.protections.events.BlockInteraction;
import net.TheElm.project.protections.events.EntityAttack;
import net.TheElm.project.protections.events.ItemPlace;
import net.TheElm.project.protections.events.ItemUse;
import net.TheElm.project.protections.logging.EventLogger;
import net.TheElm.project.utilities.DevUtils;
import net.TheElm.project.utilities.MapUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
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
import net.minecraft.util.registry.Registry;
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
    public static BlockEntityType<LecternGuideBlockEntity> GUIDE_BLOCK_ENTITY;
    public static BlockEntityType<LecternWarpsBlockEntity> WARPS_BLOCK_ENTITY;
    
    /*
     * Mod initializer
     */
    @Override
    public void onInitializeServer() {
        super.initialize();
        
        if ( DevUtils.isDebugging() )
            SharedConstants.isDevelopment = true;
        
        // Register our server-side block entity
        ServerCore.GUIDE_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, CoreMod.modIdentifier("guide_lectern"), FabricBlockEntityTypeBuilder.create(LecternGuideBlockEntity::new, Blocks.LECTERN).build(null));
        ServerCore.WARPS_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, CoreMod.modIdentifier("warps_lectern"), FabricBlockEntityTypeBuilder.create(LecternWarpsBlockEntity::new, Blocks.LECTERN).build(null));
        
        // Register the server commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            CoreMod.logInfo( "Registering our commands." );
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
                CoreMod.checkLegacyDatabase();
                
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
        
        /*LinkedPointChunk[] chunks = new LinkedPointChunk[] {
            LinkedPointChunk.of(0, 0),
            LinkedPointChunk.of(0, 1)
        };
        
        LinkedPointChunk chunk = chunks[0];
        System.out.println("Connected: " + chunk.setLinked(chunks[1]));
        
        for (LinkedPointChunk link : chunks) {
            for (BlockPos point : link.gatherPoints()) {
                System.out.println(MessageUtils.xzToString(point));
            }
        }*/
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
        if ((world instanceof ServerWorld) && (world.getRegistryKey() == World.END) && (!SewConfig.get(SewConfig.WORLD_SPECIFIC_SPAWN))) {
            BlockPos pos = ((ServerWorld)world).getSpawnPos();
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
    
    public static void markDirty(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        world.getChunkManager().markForUpdate(pos);
    }
    public static void markDirty(@NotNull World world, @NotNull BlockPos pos) {
        if (world instanceof ServerWorld)
            ServerCore.markDirty((ServerWorld) world, pos);
    }
    
    public static boolean isIntegratedServer() {
        return ServerCore.get() instanceof IntegratedServer;
    }
    public static boolean isDedicatedServer() {
        return ServerCore.get() instanceof MinecraftDedicatedServer;
    }
    
}
