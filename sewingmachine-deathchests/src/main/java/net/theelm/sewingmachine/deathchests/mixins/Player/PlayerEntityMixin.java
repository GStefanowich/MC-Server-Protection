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

package net.theelm.sewingmachine.deathchests.mixins.Player;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.base.objects.PlayerBackpack;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.deathchests.config.SewDeathConfig;
import net.theelm.sewingmachine.deathchests.utilities.DeathChestUtils;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.interfaces.PvpEntity;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerEntity.class, priority = 1)
public abstract class PlayerEntityMixin extends LivingEntity implements BackpackCarrier, PvpEntity {
    @Shadow protected abstract void vanishCursedItems();
    
    @Shadow @Final private PlayerInventory inventory;
    
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    
    /**
     * Check if we should spawn a death chest for the player BEFORE vanilla drops any of the players items
     * @param callback The Mixin Callback
     */
    @Inject(at = @At("HEAD"), method = "dropInventory", cancellable = true)
    public void onInventoryDrop(CallbackInfo callback) {
        boolean keepInventory = this.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
        PlayerBackpack backpack = this.getBackpack();
        
        // Drop the backpack if we're not using death chests (And keep inventory is off)
        if (!keepInventory) {
            DeathChestUtils.createDeathSnapshotFor((PlayerEntity)(LivingEntity) this);

            // Drop the contents of the backpack (Only if the player HAS one)
            if (backpack != null)
                backpack.dropAll(true);
        }
        
        // Only do if we're not keeping the inventory, and the player is actually dead! (Death Chest!)
        if (!keepInventory && !this.isAlive()) {
            DeathChestUtils.createDeathSnapshotFor((PlayerEntity)(LivingEntity) this);
            BlockPos chestPos;
            
            // Check if player is in combat
            if (SewConfig.get(SewDeathConfig.PVP_DISABLE_DEATH_CHEST) && this.inCombat()) {
                // Drop the backpack as well as the inventory (Only if the player HAS one)
                if (backpack != null)
                    backpack.dropAll(true);
                
                MutableText chestMessage = (this.getPrimeAdversary() instanceof PlayerEntity attacker) ?
                    Text.literal("A death chest was not generated because you were killed by ")
                        .append(MessageUtils.equipmentToText(attacker))
                        .append(".")
                    : Text.literal("A death chest was not generated because you died while in combat.");
                
                // Tell the player that they didn't get a death chest
                this.sendMessage(
                    chestMessage.formatted(Formatting.RED)
                );
                
                // Reset the hit by time
                this.resetCombat();
                return;
            }
            
            // If the inventory is NOT empty, and we found a valid position for the death chest
            if ((!(InventoryUtils.isInvEmpty(this.inventory) && InventoryUtils.isInvEmpty(backpack))) && ((chestPos = DeathChestUtils.getChestPosition(this.getEntityWorld(), this.getBlockPos() )) != null)) {
                // Vanish cursed items
                this.vanishCursedItems();
                
                // If a death chest was successfully spawned
                if (DeathChestUtils.createDeathChestFor((PlayerEntity)(LivingEntity) this, chestPos))
                    callback.cancel();
            }
        }
    }
}
