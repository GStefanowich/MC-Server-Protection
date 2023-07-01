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
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.events.BlockPlaceCallback;
import net.theelm.sewingmachine.interfaces.MoneyHolder;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.base.objects.PlayerBackpack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.theelm.sewingmachine.interfaces.PvpEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements MoneyHolder, BackpackCarrier, PvpEntity {
    // Backpack
    private PlayerBackpack backpack = null;
    
    // Player inventory
    @Shadow public PlayerInventory inventory;
    @Shadow protected abstract void vanishCursedItems();
    
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    /**
     * After dropping the main inventory drop the backpack too
     * @param callback The Mixin Callback
     */
    @Inject(at = @At("TAIL"), method = "dropInventory", cancellable = true)
    public void onInventoryDrop(CallbackInfo callback) {
        PlayerBackpack backpack = this.getBackpack();
        if (backpack != null)
            backpack.dropAll();
    }
    
    /**
     * Stats of Mob Kills
     * @param serverWorld The world the target was killed in
     * @param livingEntity The entity that we killed
     * @param callback The Mixin callback
     */
    @Inject(at = @At("HEAD"), method = "onKilledOther", cancellable = true)
    public void onKilledTarget(ServerWorld serverWorld, LivingEntity livingEntity, CallbackInfoReturnable<Boolean> callback) {
        if (livingEntity instanceof EnderDragonEntity)
            callback.setReturnValue(true);
    }
    
    /*
     * Player Combat
     */
    
    private Long hitByOtherPlayerAt = null;
    
    @Inject(at = @At("TAIL"), method = "tick")
    public void onTick(CallbackInfo callback) {
        if ((!this.getWorld().isClient) && ((Entity) this) instanceof ServerPlayerEntity) {
            if (this.hitByOtherPlayerAt != null && (this.hitByOtherPlayerAt < System.currentTimeMillis() - (SewConfig.get(SewCoreConfig.PVP_COMBAT_SECONDS) * 1000))) {
                
                // Remove player from combat
                this.hitByOtherPlayerAt = null;
                
                // Send message about being out of combat
                this.sendMessage(
                    Text.literal("You are no longer in combat.").formatted(Formatting.GREEN)
                );
                
                // Clear players from the health bar
                ServerBossBar healthBar = ((PlayerData) this).getHealthBar(false);
                if (healthBar != null)
                    healthBar.clearPlayers();
            }
        }
    }
    
    /**
     * Set the attacker for PvP
     * @param attacker The attacker
     */
    @Override
    public void setAttacker(@Nullable LivingEntity attacker) {
        if (!this.getWorld().isClient) {
            if (attacker instanceof PlayerEntity) {
                // If player just entered combat
                if (this.hitByOtherPlayerAt == null)
                    this.sendMessage(Text.literal("You are now in combat.").formatted(Formatting.RED));
                
                // Set combat time to when hit
                this.hitByOtherPlayerAt = System.currentTimeMillis();
            }
        }
        
        super.setAttacker(attacker);
    }
    
    @Override
    public boolean inCombat() {
        return this.hitByOtherPlayerAt != null;
    }
    
    @Override
    public void resetCombat() {
        this.hitByOtherPlayerAt = null;
    }
    
    /**
     * If an Arrow was not found for Infinite Arrows, return a Regular Arrow
     * @param weapon
     * @param callback
     */
    @Inject(at = @At("RETURN"), method = "getProjectileType", cancellable = true)
    public void onCheckArrowType(@NotNull ItemStack weapon, @NotNull CallbackInfoReturnable<ItemStack> callback) {
        if ((weapon.getItem() instanceof RangedWeaponItem) && callback.getReturnValue().isEmpty() && (EnchantmentHelper.getLevel(Enchantments.INFINITY, weapon) > 0 ))
            callback.setReturnValue(new ItemStack(Items.ARROW));
    }
    
    /*
     * Tracked Data
     */
    @Inject(at = @At("RETURN"), method = "initDataTracker")
    public void onInitDataTracking(@NotNull CallbackInfo callback) {
        this.dataTracker.startTracking(MONEY, SewConfig.get(SewCoreConfig.STARTING_MONEY));
    }
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Save the players money
        tag.putInt(MoneyHolder.SAVE_KEY, this.getPlayerWallet());
        
        // Store the players backpack
        if (this.backpack != null) {
            tag.putInt("BackpackSize", this.backpack.getRows());
            tag.put("Backpack", this.backpack.getTags());
            
            NbtList pickupTags = this.backpack.getPickupTags();
            if (!pickupTags.isEmpty())
                tag.put("BackpackPickup", pickupTags);
        }
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Read the players money
        if (tag.contains(MoneyHolder.SAVE_KEY, NbtElement.NUMBER_TYPE))
            this.dataTracker.set( MONEY, tag.getInt( MoneyHolder.SAVE_KEY ) );
        
        // Read the players backpack
        if (tag.contains("BackpackSize", NbtElement.NUMBER_TYPE) && tag.contains("Backpack", NbtElement.LIST_TYPE)) {
            this.backpack = new PlayerBackpack((PlayerEntity)(LivingEntity)this, tag.getInt("BackpackSize"));
            this.backpack.readTags(tag.getList("Backpack", NbtElement.COMPOUND_TYPE));
            
            if (tag.contains("BackpackPickup", NbtElement.LIST_TYPE))
                this.backpack.readPickupTags(tag.getList("BackpackPickup", NbtElement.STRING_TYPE));
        } else {
            int startingBackpack = SewConfig.get(SewCoreConfig.BACKPACK_STARTING_ROWS);
            if ( startingBackpack > 0 )
                this.backpack = new PlayerBackpack((PlayerEntity)(LivingEntity)this, startingBackpack);
        }
    }
    
    /*
     * Money
     */
    @Override
    public int getPlayerWallet() {
        return this.dataTracker.get(MONEY);
    }
    
    /*
     * Player Backpack
     */
    @Override
    public @Nullable PlayerBackpack getBackpack() {
        return this.backpack;
    }
    @Override
    public void setBackpack(@Nullable PlayerBackpack backpack) {
        this.backpack = backpack == null || backpack.getPlayer() == (LivingEntity)this ? backpack : new PlayerBackpack((PlayerEntity)(LivingEntity)this, backpack);
    }
    @Inject(at = @At("TAIL"), method = "vanishCursedItems")
    public void onVanishCursedItems(CallbackInfo callback) {
        if (this.backpack == null)
            return;
        for (int i = 0; i < this.backpack.size(); i++) {
            ItemStack stack = this.backpack.getStack(i);
            if (!stack.isEmpty() && EnchantmentHelper.hasVanishingCurse(stack))
                this.backpack.removeStack(i);
        }
    }
    
    /*
     * Item placement
     */
    @Inject(at = @At("HEAD"), method = "canPlaceOn", cancellable = true)
    public void checkPlacement(BlockPos blockPos, Direction direction, ItemStack itemStack, CallbackInfoReturnable<Boolean> callback) {
        ActionResult result = BlockPlaceCallback.EVENT.invoker().interact((ServerPlayerEntity)(LivingEntity)this, this.getWorld(), blockPos, direction, itemStack);
        if (result != ActionResult.PASS)
            callback.setReturnValue(result == ActionResult.SUCCESS);
    }
}
