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

package net.theelm.sewingmachine.base.mixins.Player;

import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.NetworkHandlerCallback;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements ServerPlayPacketListener, PlayerData {
    /*
     * Shadow Methods
     */
    @Shadow @Final private MinecraftServer server;
    @Shadow private ServerPlayerEntity player;
    
    /*
     * Health Bars
     */
    public @Contract("true -> !null") ServerBossBar getHealthBar(boolean create) {
        return ((PlayerData)this.player).getHealthBar(create);
    }
    
    /*
     * Modified methods
     */
    
    // On connect
    @Inject(at = @At("RETURN"), method = "<init>")
    public void onPlayerConnect(MinecraftServer server, ClientConnection connection, ServerPlayerEntity player, CallbackInfo callback) {
        // Send out the READY event
        NetworkHandlerCallback.READY.invoker()
            .ready(server, connection, player);
        
        // Check if server has been joined before
        if (((PlayerData) player).getFirstJoinAt() == null) {
            // Get starting money
            int startingMoney = SewConfig.get(SewCoreConfig.STARTING_MONEY);
            
            // Give the player the starting amount
            if ( SewConfig.get(SewCoreConfig.DO_MONEY) && (startingMoney > 0))
                MoneyUtils.givePlayerMoney(player, startingMoney);
            
            // Give the player the starting items
            for (Map.Entry<Item, Integer> item : SewConfig.get(SewCoreConfig.STARTING_ITEMS).entrySet()) {
                ItemStack stack = new ItemStack(item.getKey());
                stack.setCount(item.getValue());
                
                player.getInventory()
                    .offerOrDrop(stack);
            }
            
            // Give the player all of the games recipes
            if (SewConfig.get(SewCoreConfig.START_WITH_RECIPES)) {
                Collection<Recipe<?>> recipes = server.getRecipeManager().values();
                this.player.unlockRecipes(recipes);
            }
            
            // Set first join for later referencing
            ((PlayerData) player).updateFirstJoin();
        } else {
            Long lastJoin;
            int allowance = SewConfig.get(SewCoreConfig.DAILY_ALLOWANCE);
            
            // If we should give a daily allowance
            if ((allowance > 0) && ((lastJoin = ((PlayerData) player).getLastJoinAt()) != null)) {
                
                // Get the timestamp of the start of today
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfDay = calendar.getTimeInMillis();
                
                // If players last join was before the start of TODAY
                if (lastJoin <= startOfDay) {
                    // Give that player money
                    MoneyUtils.givePlayerMoney(player, allowance);
                    
                    // Tell them they were awarded money
                    player.sendMessage(
                        Text.literal("You were given $")
                            .formatted(Formatting.YELLOW)
                            .append(Text.literal(FormattingUtils.format(allowance)).formatted(Formatting.AQUA, Formatting.BOLD))
                            .append(" for logging in today!")
                    );
                }
            }
        }
        
        // Always update join time to NOW
        ((PlayerData) player).updateLastJoin();
    }
    
    // When player leaves
    @Inject(at = @At("RETURN"), method = "onDisconnected")
    public void onPlayerDisconnect(final CallbackInfo callback) {
        // Remove players from the health bar when disconnecting
        // (Don't have floating health bars remaining on-screen)
        this.getHealthBar().clearPlayers();
    }
}
