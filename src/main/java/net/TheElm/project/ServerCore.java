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

import net.TheElm.project.commands.*;
import net.TheElm.project.protections.BlockBreak;
import net.TheElm.project.protections.BlockInteraction;
import net.TheElm.project.protections.EntityAttack;
import net.TheElm.project.protections.ItemInteraction;
import net.TheElm.project.utilities.LoggingUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;

import java.sql.SQLException;

public final class ServerCore extends CoreMod implements DedicatedServerModInitializer {
    
    /*
     * Mod initializer
     */
    @Override
    public void onInitializeServer() {
        CoreMod.logInfo( "Sewing Machine utilities mod is starting." );
        
        CommandRegistry REGISTRY = CommandRegistry.INSTANCE;
        
        CoreMod.logInfo( "Registering our commands." );
        REGISTRY.register(false, AdminCommands::register );
        REGISTRY.register(false, ChatroomCommands::register );
        REGISTRY.register(false, ClaimCommand::register );
        REGISTRY.register(false, GameModesCommand::register );
        REGISTRY.register(false, HoldingCommand::register );
        REGISTRY.register(false, LoggingCommand::register );
        REGISTRY.register(false, MiscCommands::register );
        REGISTRY.register(false, ModsCommand::register );
        REGISTRY.register(false, MoneyCommand::register );
        REGISTRY.register(false, NickNameCommand::register );
        REGISTRY.register(false, PlayerSpawnCommand::register );
        REGISTRY.register(false, RulerCommand::register );
        REGISTRY.register(false, TeleportsCommand::register );
        REGISTRY.register(false, WaystoneCommand::register );
        
        // Create registry based listeners
        BlockBreak.init();
        BlockInteraction.init();
        EntityAttack.init();
        ItemInteraction.init();
        
        CoreMod.logInfo( "Initializing Database." );
        try {
            // Initialize the database
            if (CoreMod.initDB()) {
                CoreMod.checkLegacyDatabase();
                
                CoreMod.logInfo("Database initialization finished");
                
                // Clear out old logs
                LoggingUtils.doCleanup();
            } else {
                CoreMod.logInfo( "Skipping Database Initialization (Unused)" );
            }
        } catch (SQLException e) {
            CoreMod.logInfo( "Error executing MySQL Database setup." );

            throw new RuntimeException( "Could not connect to database server.", e );
        }

        // Alert the mod presence
        CoreMod.logInfo( "Finished loading." );
    }
    
}
