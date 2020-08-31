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

import net.TheElm.project.commands.AdminCommands;
import net.TheElm.project.commands.BackpackCommand;
import net.TheElm.project.commands.ChatroomCommands;
import net.TheElm.project.commands.ClaimCommand;
import net.TheElm.project.commands.GameModesCommand;
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
import net.TheElm.project.commands.SpawnerCommand;
import net.TheElm.project.commands.TeleportEffectCommand;
import net.TheElm.project.commands.TeleportsCommand;
import net.TheElm.project.commands.WaystoneCommand;
import net.TheElm.project.commands.WhereCommand;
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.protections.events.BlockBreak;
import net.TheElm.project.protections.events.BlockInteraction;
import net.TheElm.project.protections.events.EntityAttack;
import net.TheElm.project.protections.events.ItemPlace;
import net.TheElm.project.protections.events.ItemUse;
import net.TheElm.project.protections.logging.EventLogger;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
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
import java.util.UUID;

public final class ServerCore extends CoreMod implements DedicatedServerModInitializer {
    
    /*
     * Mod initializer
     */
    @Override
    public void onInitializeServer() {
        super.initialize();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            CoreMod.logInfo( "Registering our commands." );
            AdminCommands.register(dispatcher);
            BackpackCommand.register(dispatcher);
            ChatroomCommands.register(dispatcher);
            ClaimCommand.register(dispatcher);
            GameModesCommand.register(dispatcher);
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
            TeleportsCommand.register(dispatcher);
            WaystoneCommand.register(dispatcher);
            WhereCommand.register(dispatcher);
            
            if ( CoreMod.isDebugging() )
                TeleportEffectCommand.register(dispatcher);
        });
        
        /*COMMANDS.register(false, AdminCommands::register);
        COMMANDS.register(false, BackpackCommand::register);
        COMMANDS.register(false, ChatroomCommands::register);
        COMMANDS.register(false, ClaimCommand::register);
        COMMANDS.register(false, GameModesCommand::register);
        COMMANDS.register(false, HoldingCommand::register);
        COMMANDS.register(false, LoggingCommand::register);
        COMMANDS.register(false, MiscCommands::register);
        COMMANDS.register(false, ModCommands::register);
        COMMANDS.register(false, ModsCommand::register);
        COMMANDS.register(false, MoneyCommand::register);
        COMMANDS.register(false, NickNameCommand::register);
        COMMANDS.register(false, PermissionCommand::register);
        COMMANDS.register(false, PlayerSpawnCommand::register);
        COMMANDS.register(false, RideCommand::register);
        COMMANDS.register(false, RulerCommand::register);
        COMMANDS.register(false, SpawnerCommand::register);
        COMMANDS.register(false, TeleportsCommand::register);
        COMMANDS.register(false, WaystoneCommand::register);
        COMMANDS.register(false, WhereCommand::register);*/
        
        // Debug commands
        /*if ( CoreMod.isDebugging() ) {
            COMMANDS.register(false, TeleportEffectCommand::register);
        }*/
        
        // Create registry based listeners
        BlockBreak.init();
        BlockInteraction.init();
        EntityAttack.init();
        ItemPlace.init();
        ItemUse.init();
        
        CoreMod.logInfo( "Initializing Database." );
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
            CoreMod.logInfo( "Finished loading." );
        } catch (IOException e) {
            CoreMod.logError("Error during startup", e);
        }
    }
    
    @NotNull
    public static MinecraftServer get() {
        return CoreMod.getGameInstance().left().orElseThrow(() -> new RuntimeException("Called Client object from illegal position."));
    }
    
    @Nullable
    public static ServerPlayerEntity getPlayer(UUID playerUUID) {
        return ServerCore.get().getPlayerManager().getPlayer( playerUUID );
    }
    
    public static @NotNull BlockPos getSpawn() {
        return ServerCore.getSpawn(World.OVERWORLD);
    }
    public static @NotNull BlockPos getSpawn(World world) {
        // Get the forced position of TheEnd
        if ((world instanceof ServerWorld) && (world.getRegistryKey() == World.END)) {
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
    public static @NotNull BlockPos getSpawn(RegistryKey<World> world) {
        return ServerCore.getSpawn(ServerCore.getWorld(world));
    }
    public static @NotNull World getWorld(RegistryKey<World> world) {
        return ServerCore.get().getWorld(world);
    }
    
}
