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

package net.TheElm.project.utilities;

import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.interfaces.MoneyHolder;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class MoneyUtils {
    
    private MoneyUtils() {}
    
    /*
     * Get player money
     */
    public static int getPlayerMoney(@NotNull UUID playerId) throws NbtNotFoundException {
        // Check if player is online
        ServerPlayerEntity player;
        if ((player = EntityUtils.getPlayer( playerId )) != null)
            return MoneyUtils.getPlayerMoney( player );
        
        // If not online
        CompoundTag tag = NbtUtils.readOfflinePlayerData( playerId );
        
        // Get the balance from the NBT tag
        return tag.getInt( MoneyHolder.SAVE_KEY );
    }
    public static int getPlayerMoney(@NotNull PlayerEntity player) {
        return player.getDataTracker().get(MoneyHolder.MONEY);
    }
    
    /*
     * Set player money
     */
    public static boolean setPlayerMoney(@NotNull UUID playerId, int amount) throws NbtNotFoundException {
        // Check if player is online
        ServerPlayerEntity player;
        if ((player = EntityUtils.getPlayer( playerId )) != null)
            return MoneyUtils.setPlayerMoney( player, amount );
        
        // If not online
        CompoundTag nbt = NbtUtils.readOfflinePlayerData( playerId );
        int balance = nbt.getInt( MoneyHolder.SAVE_KEY );
        if (amount < 0)
            return false;
        
        // If balance is unchanged, don't attempt saving
        if (amount == balance)
            return true;
        
        // Change the NBT tag
        nbt.putInt( MoneyHolder.SAVE_KEY, amount );
        
        return NbtUtils.writeOfflinePlayerData( playerId, nbt );
    }
    public static boolean setPlayerMoney(@NotNull PlayerEntity player, int amount) {
        player.getDataTracker().set(MoneyHolder.MONEY, amount);
        return true;
    }
    
    /*
     * Give player money
     */
    public static boolean givePlayerMoney(@NotNull UUID playerId, int amount) throws NbtNotFoundException {
        // Check if player is online
        ServerPlayerEntity player;
        if ((player = EntityUtils.getPlayer( playerId )) != null)
            return MoneyUtils.givePlayerMoney( player, amount );
        
        // If not online
        double updateTo = 0;
        CompoundTag nbt = NbtUtils.readOfflinePlayerData( playerId );
        int balance = nbt.getInt( MoneyHolder.SAVE_KEY );
        if (((updateTo = (double)(balance + amount))) > Integer.MAX_VALUE)
            return false;
        
        // If balance is unchanged, don't attempt saving
        if (updateTo == balance)
            return true;
        
        // Change the NBT tag
        nbt.putInt( MoneyHolder.SAVE_KEY, (int)updateTo );
        
        // Save the data
        return NbtUtils.writeOfflinePlayerData( playerId, nbt );
    }
    public static boolean givePlayerMoney(@NotNull PlayerEntity player, int amount) {
        DataTracker playerDataTracker = player.getDataTracker();
        
        // Update the amount the player has
        int updateTo = playerDataTracker.get(MoneyHolder.MONEY) + amount;
        playerDataTracker.set(MoneyHolder.MONEY, updateTo);
        
        // Done
        return true;
    }
    
    /*
     * Take player money
     */
    public static boolean takePlayerMoney(@NotNull UUID playerId, int amount) throws NbtNotFoundException, NotEnoughMoneyException {
        // Check if player is online
        ServerPlayerEntity player;
        if ((player = EntityUtils.getPlayer( playerId )) != null)
            return MoneyUtils.takePlayerMoney( player, amount );
        
        // If not online
        double updateTo = 0;
        CompoundTag nbt = NbtUtils.readOfflinePlayerData( playerId );
        int balance = nbt.getInt( MoneyHolder.SAVE_KEY );
        if (((updateTo = (double)(balance - amount))) < Integer.MIN_VALUE)
            return false;
        
        // If balance is unchanged, don't attempt saving
        if (updateTo == balance)
            return true;
        
        // Change the NBT tag
        nbt.putInt( MoneyHolder.SAVE_KEY, (int)updateTo );
        
        // Save the data
        return NbtUtils.writeOfflinePlayerData( playerId, nbt );
    }
    public static boolean takePlayerMoney(@NotNull PlayerEntity player, int amount) throws NotEnoughMoneyException {
        DataTracker playerDataTracker = player.getDataTracker();
        
        // Update the amount the player has
        int updateTo = 0, has = player.getDataTracker().get(MoneyHolder.MONEY);
        if ((updateTo = (has - amount)) < 0)
            throw new NotEnoughMoneyException();
        
        playerDataTracker.set(MoneyHolder.MONEY, updateTo);
        
        // Done
        return true;
    }
    
    
}
