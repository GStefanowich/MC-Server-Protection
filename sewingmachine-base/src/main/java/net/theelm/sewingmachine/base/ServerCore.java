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

package net.theelm.sewingmachine.base;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.base.objects.BalanceUpdateHandler;
import net.theelm.sewingmachine.base.objects.ItemFrameTransfer;
import net.theelm.sewingmachine.base.objects.signs.SignBackpack;
import net.theelm.sewingmachine.base.objects.signs.SignBalance;
import net.theelm.sewingmachine.base.objects.signs.SignGuide;
import net.theelm.sewingmachine.base.objects.signs.SignShopBuy;
import net.theelm.sewingmachine.base.objects.signs.SignShopFree;
import net.theelm.sewingmachine.base.objects.signs.SignShopSell;
import net.theelm.sewingmachine.base.objects.signs.SignWarp;
import net.theelm.sewingmachine.base.objects.signs.SignWaystone;
import net.theelm.sewingmachine.base.packets.PlayerBackpackOpenPacket;
import net.theelm.sewingmachine.base.packets.SewConfigPacket;
import net.theelm.sewingmachine.base.packets.SewHelloPacket;
import net.theelm.sewingmachine.base.commands.AdminCommands;
import net.theelm.sewingmachine.base.commands.BackpackCommand;
import net.theelm.sewingmachine.base.commands.DateCommand;
import net.theelm.sewingmachine.base.commands.FireworksCommand;
import net.theelm.sewingmachine.base.commands.GameModesCommand;
import net.theelm.sewingmachine.base.commands.GiveSelfCommand;
import net.theelm.sewingmachine.base.commands.HeadCommand;
import net.theelm.sewingmachine.base.commands.LoggingCommand;
import net.theelm.sewingmachine.base.commands.MiscCommands;
import net.theelm.sewingmachine.base.commands.ModsCommand;
import net.theelm.sewingmachine.base.commands.MoneyCommand;
import net.theelm.sewingmachine.base.commands.PlayerSpawnCommand;
import net.theelm.sewingmachine.base.commands.RideCommand;
import net.theelm.sewingmachine.base.commands.RulerCommand;
import net.theelm.sewingmachine.commands.ScoreboardModifyCommand;
import net.theelm.sewingmachine.base.commands.SpawnerCommand;
import net.theelm.sewingmachine.base.commands.DebugCommands;
import net.theelm.sewingmachine.base.commands.TeleportsCommand;
import net.theelm.sewingmachine.base.commands.WaystoneCommand;
import net.theelm.sewingmachine.base.commands.WhereCommand;
import net.theelm.sewingmachine.commands.WhitelistTreeCommand;
import net.theelm.sewingmachine.base.commands.WorldCommand;
import net.theelm.sewingmachine.commands.abstraction.AbstractSewCommand;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.BlockInteractionCallback;
import net.theelm.sewingmachine.events.NetworkHandlerCallback;
import net.theelm.sewingmachine.events.PlayerBalanceCallback;
import net.theelm.sewingmachine.events.PlayerModsCallback;
import net.theelm.sewingmachine.events.TaxCollection;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.theelm.sewingmachine.interfaces.ModUser;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.protections.logging.EventLogger;
import net.theelm.sewingmachine.utilities.DevUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.BlueMapUtils;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import net.theelm.sewingmachine.utilities.ShopSigns;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ServerCore extends CoreMod implements ModInitializer, SewPlugin {
    /*
     * Mod initializer
     */
    @Override
    public void onInitialize() {
        super.initialize();
        
        // Init Bluemap stuff
        BlueMapUtils.init();
        
        // Create registry based listeners
        ItemFrameTransfer.register(DamageEntityCallback.EVENT);
        
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
                CoreMod.logInfo("Skipping Database Initialization (Unused)");
            }
        } catch (SQLException e) {
            CoreMod.logInfo("Error executing MySQL Database setup.");
            
            throw new RuntimeException("Could not connect to database server.", e);
        }
        
        // Update the mod version in config
        try {
            SewConfig.set(SewBaseConfig.CONFIG_VERSION, ConfigOption.convertToJSON(CoreMod.getModVersion()));
            SewConfig.save();
            
            ShopSigns.add(SignBackpack::new);
            ShopSigns.add(SignBalance::new);
            ShopSigns.add(SignGuide::new);
            ShopSigns.add(SignShopBuy::new);
            ShopSigns.add(SignShopFree::new);
            ShopSigns.add(SignShopSell::new);
            ShopSigns.add(SignWarp::new);
            ShopSigns.add(SignWaystone::new);
            
            // Register shop signs
            BlockInteractionCallback.EVENT.register(ShopSignData::onSignInteract);
            
            // Register the server tax
            TaxCollection.EVENT.register((income, world, pos) -> {
                Integer tax = SewConfig.get(SewBaseConfig.SERVER_SALES_TAX);
                if (tax != null)
                    income.addTax(Text.literal("Spawn"), tax);
            });
            
            // Implement our money handler
            PlayerBalanceCallback.EVENT.register(new BalanceUpdateHandler());
            
            // Register the packet receiver for opening the backpack
            ServerPlayNetworking.registerGlobalReceiver(PlayerBackpackOpenPacket.TYPE, (packet, player, responseSender) -> InventoryUtils.openBackpack(player));
            
            // Send the hello packet
            NetworkHandlerCallback.READY.register((server, connection, player) -> {
                Map<String, String> modules = new HashMap<>();
                for (ModMetadata metadata : this.getPluginMetadata())
                    modules.put(metadata.getId(), metadata.getVersion().getFriendlyString());
                
                NetworkingUtils.send(player, new SewHelloPacket(modules));
            });
            
            // When (if) the client sends the Hello packet back with the synced mods
            NetworkingUtils.serverReceiver(SewHelloPacket.TYPE, (server, player, network, packet, sender) -> {
                Map<String, String> modules = this.matchPluginMetadata(packet.modules(), true);
                ((ModUser) player).setModded(modules.keySet());
                
                // Trigger events for receiving mod information
                PlayerModsCallback.EVENT.invoker()
                    .hasMods(player);
            });
            
            // Resend the backpack
            ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> InventoryUtils.resendBackpack(newPlayer));
            PlayerModsCallback.EVENT.register(player -> InventoryUtils.resendBackpack(player));
            PlayerModsCallback.EVENT.register(player -> NetworkingUtils.send(player, new SewConfigPacket()));
            
            // Alert the mod presence
            CoreMod.logInfo("Finished loading.");
        } catch (IOException e) {
            CoreMod.logError("Error during startup", e);
        }
    }
    
    @Override
    public @NotNull Optional<Class<?>> getConfigClass() {
        return Optional.of(SewBaseConfig.class);
    }
    
    @Override
    public @Nullable AbstractSewCommand<?>[] getCommands() {
        return new AbstractSewCommand<?>[]{
            new AdminCommands(),
            new BackpackCommand(),
            new DateCommand(),
            new FireworksCommand(),
            new GameModesCommand(),
            new GiveSelfCommand(),
            new HeadCommand(),
            //new HoldingCommand(),
            new LoggingCommand(),
            new MiscCommands(),
            new ModsCommand(),
            new MoneyCommand(),
            new PlayerSpawnCommand(),
            new RideCommand(),
            new RulerCommand(),
            new SpawnerCommand(),
            //new StatsCommand(),
            new TeleportsCommand(),
            new WaystoneCommand(),
            new WhereCommand(),
            new WhitelistTreeCommand(),
            new WorldCommand(),
            
            new ScoreboardModifyCommand(),
            
            DevUtils.isDebugging() ? new DebugCommands() : null
        };
    }
    
    public static @NotNull RegistryKey<World> defaultWorldKey() {
        return SewConfig.get(SewBaseConfig.DEFAULT_WORLD);
    }
    
    public static @NotNull BlockPos getSpawn(@NotNull World world) {
        // Get the forced position of TheEnd
        if ((world instanceof ServerWorld serverWorld) && (world.getRegistryKey() == World.END) && (!SewConfig.get(SewBaseConfig.WORLD_SPECIFIC_SPAWN))) {
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
}
