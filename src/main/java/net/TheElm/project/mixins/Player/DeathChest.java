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

package net.TheElm.project.mixins.Player;

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.interfaces.BlockPlaceCallback;
import net.TheElm.project.interfaces.MoneyHolder;
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.objects.PlayerBackpack;
import net.TheElm.project.utilities.DeathChestUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class DeathChest extends LivingEntity implements MoneyHolder, BackpackCarrier {
    
    // Backpack
    private PlayerBackpack backpack = null;
    
    // Player inventory
    @Shadow public PlayerInventory inventory;
    @Shadow protected abstract void vanishCursedItems();
    
    protected DeathChest(EntityType<? extends LivingEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    /* 
     * If player drops inventory (At death, stop that!)
     */
    @Inject(at = @At("HEAD"), method = "dropInventory", cancellable = true)
    public void onInventoryDrop(CallbackInfo callback) {
        if (!SewingMachineConfig.INSTANCE.DO_DEATH_CHESTS.get())
            return;
        
        // Only do if we're not keeping the inventory, and the player is actually dead! (Death Chest!)
        if ((!this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) && (!this.isAlive())) {
            BlockPos chestPos;
            
            // Check if player is in combat
            if (SewingMachineConfig.INSTANCE.PVP_DISABLE_DEATH_CHEST.get() && (this.hitByOtherPlayerAt != null)) {
                // Tell the player that they didn't get a death chest
                this.sendMessage(new LiteralText("A death chest was not generated because you died in combat.").formatted(Formatting.RED));
                
                // Reset the hit by time
                this.hitByOtherPlayerAt = null;
                
                return;
            }
            
            // If the inventory is NOT empty, and we found a valid position for the death chest
            if ((!(InventoryUtils.isInvEmpty(this.inventory) && InventoryUtils.isInvEmpty(this.backpack))) && ((chestPos = DeathChestUtils.getChestPosition( this.getEntityWorld(), this.getBlockPos() )) != null)) {
                // Vanish cursed items
                this.vanishCursedItems();
                
                // If a death chest was successfully spawned
                if (DeathChestUtils.createDeathChestFor( (PlayerEntity)(LivingEntity) this, chestPos )) {
                    callback.cancel();
                }
            }
        }
    }
    
    /*
     * Player Combat
     */
    
    private Long hitByOtherPlayerAt = null;
    
    @Inject(at = @At("TAIL"), method = "tick")
    public void onTick(CallbackInfo callback) {
        if ((!this.world.isClient) && ((Entity) this) instanceof ServerPlayerEntity) {
            if (this.hitByOtherPlayerAt != null && (this.hitByOtherPlayerAt < System.currentTimeMillis() - (SewingMachineConfig.INSTANCE.PVP_COMBAT_SECONDS.get() * 1000))) {
                // Remove player from combat
                this.hitByOtherPlayerAt = null;
                
                this.sendMessage(new LiteralText("You are no longer in combat.").formatted(Formatting.YELLOW));
            }
        }
    }
    
    @Inject(at = @At("RETURN"), method = "damage")
    public void onDamage(DamageSource source, float damage, CallbackInfoReturnable<Boolean> callback) {
        if ((!this.world.isClient) && ((Entity) this) instanceof ServerPlayerEntity) {
            if (source.getAttacker() instanceof PlayerEntity && callback.getReturnValue()) {
                // If player just entered combat
                if (this.hitByOtherPlayerAt == null)
                    this.sendMessage(new LiteralText("You are now in combat.").formatted(Formatting.YELLOW));
                
                // Set combat time to when hit
                this.hitByOtherPlayerAt = System.currentTimeMillis();
            }
        }
    }
    
    @Inject(at = @At("RETURN"), method = "getArrowType", cancellable = true)
    public void onCheckArrowType(ItemStack weapon, CallbackInfoReturnable<ItemStack> callback) {
        if ((weapon.getItem() instanceof RangedWeaponItem) && callback.getReturnValue().isEmpty() && (EnchantmentHelper.getLevel(Enchantments.INFINITY, weapon) > 0 ))
            callback.setReturnValue(new ItemStack(Items.ARROW));
    }
    
    /* 
     * Override the players display name to their nick
     */
    @Inject(at = @At("HEAD"), method = "getDisplayName", cancellable = true)
    public void getPlayerNickname(CallbackInfoReturnable<Text> callback) {
        if ((((LivingEntity)this) instanceof ServerPlayerEntity) && (((Nicknamable)this).getPlayerNickname() != null))
            callback.setReturnValue(((Nicknamable) this).getPlayerNickname());
    }
    
    /*
     * Tracked Data
     */
    @Inject(at = @At("RETURN"), method = "initDataTracker")
    public void onInitDataTracking(CallbackInfo callback) {
        this.dataTracker.startTracking( MONEY, SewingMachineConfig.INSTANCE.STARTING_MONEY.get() );
    }
    @Inject(at = @At("TAIL"), method = "writeCustomDataToTag")
    public void onSavingData(CompoundTag tag, CallbackInfo callback) {
        // Save the players money
        tag.putInt( MoneyHolder.SAVE_KEY, this.getPlayerWallet() );
        
        // Store the players backpack
        if (this.backpack != null) {
            tag.putInt("BackpackSize", this.backpack.getRows());
            tag.put("Backpack", this.backpack.getTags());
            
            ListTag pickupTags = this.backpack.getPickupTags();
            if (!pickupTags.isEmpty())
                tag.put("BackpackPickup", pickupTags);
        }
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromTag")
    public void onReadingData(CompoundTag tag, CallbackInfo callback) {
        // Read the players money
        if (tag.contains( MoneyHolder.SAVE_KEY, NbtType.NUMBER ))
            this.dataTracker.set( MONEY, tag.getInt( MoneyHolder.SAVE_KEY ) );
    
        // Read the players backpack
        if (tag.contains("BackpackSize", NbtType.NUMBER) && tag.contains("Backpack", NbtType.LIST)) {
            this.backpack = new PlayerBackpack((PlayerEntity)(LivingEntity)this, tag.getInt("BackpackSize"));
            this.backpack.readTags(tag.getList("Backpack", NbtType.COMPOUND));
        
            if (tag.contains("BackpackPickup", NbtType.LIST))
                this.backpack.readPickupTags(tag.getList("BackpackPickup", NbtType.STRING));
        } else {
            int startingBackpack = SewingMachineConfig.INSTANCE.BACKPACK_STARTING_ROWS.get();
            if ( startingBackpack > 0 )
                this.backpack = new PlayerBackpack((PlayerEntity)(LivingEntity)this, Math.min(startingBackpack, 6));
        }
    }
    
    /*
     * Money
     */
    @Override
    public int getPlayerWallet() {
        return this.dataTracker.get( MONEY );
    }
    
    /*
     * Player Backpack
     */
    @Override
    public @Nullable PlayerBackpack getBackpack() {
        return this.backpack;
    }
    @Override
    public void setBackpack(PlayerBackpack backpack) {
        this.backpack = backpack;
    }
    @Inject(at = @At("TAIL"), method = "vanishCursedItems")
    public void onVanishCursedItems(CallbackInfo callback) {
        if (this.backpack == null)
            return;
        for (int i = 0; i < this.backpack.getInvSize(); i++) {
            ItemStack stack = this.backpack.getInvStack(i);
            if (!stack.isEmpty() && EnchantmentHelper.hasVanishingCurse(stack))
                this.backpack.removeInvStack(i);
        }
    }
    
    /*
     * Item placement
     */
    @Inject(at = @At("HEAD"), method = "canPlaceOn", cancellable = true)
    public void checkPlacement(BlockPos blockPos, Direction direction, ItemStack itemStack, CallbackInfoReturnable<Boolean> callback) {
        ActionResult result = BlockPlaceCallback.EVENT.invoker().interact((ServerPlayerEntity)(LivingEntity)this, this.world, blockPos, direction, itemStack);
        if (result != ActionResult.PASS)
            callback.setReturnValue(result == ActionResult.SUCCESS);
    }
    
}
