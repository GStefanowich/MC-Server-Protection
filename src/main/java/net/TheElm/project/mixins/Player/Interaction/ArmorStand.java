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

package net.TheElm.project.mixins.Player.Interaction;

import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.ArmorStandPose;
import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerCorpse;
import net.TheElm.project.objects.PlayerBackpack;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStand extends LivingEntity implements PlayerCorpse {
    
    private UUID corpsePlayerUUID = null;
    private NbtList corpsePlayerItems = null;
    private NbtList corpsePlayerBackpack = null;
    
    @Shadow public native boolean shouldShowArms();
    @Shadow private native void setShowArms( boolean show );
    
    @Shadow public native boolean shouldHideBasePlate();
    @Shadow private native void setHideBasePlate(boolean bl);
    
    protected ArmorStand(EntityType<? extends LivingEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    /*
     * Armor Stand Modifications
     */
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/block/Block.dropStack(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V"), method = "breakAndDropItem")
    private void onDropSelf(World world, BlockPos blockPos, ItemStack itemStack) {
        boolean showArms = this.shouldShowArms();
        boolean hidePlate = this.shouldHideBasePlate();
        
        if (showArms || hidePlate) {
            NbtCompound name = itemStack.getOrCreateSubNbt("display");
            NbtCompound enti = itemStack.getOrCreateSubNbt("EntityTag");
            enti.putBoolean("ShowArms", showArms);
            enti.putBoolean("NoBasePlate", hidePlate);
            
            NbtList lore = new NbtList();
            lore.add(NbtString.of("{\"text\":\""
                + String.join(", ", Arrays.asList(
                    ( hidePlate ? "No base" : "Base" ),
                    ( showArms ? "Arms" : "No Arms" )
                ))
                + "\",\"color\":\"gray\"}"));
            
            name.put("Lore", lore);
        }
        
        Block.dropStack(world, blockPos, itemStack);
    }
    
    /*
     * Armor Stand Protections
     */
    
    @Inject(at = @At("HEAD"), method = "interactAt", cancellable = true)
    public void onPlayerInteract(PlayerEntity player, Vec3d vec3d, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        // Armor Stand is a corpse
        if (this.corpsePlayerUUID != null) {
            // Return the items back to their owner
            if (EntityUtils.canEntityTakeDeathChest(player, this.corpsePlayerUUID)) {
                this.returnItemsToPlayer(player);
            } else {
                // Deny if the corpse does not belong to this player
                callback.setReturnValue(ActionResult.FAIL);
            }
            return;
        }
        
        // Player is in creative
        if ((player.isCreative() && SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS))
            || player.isSpectator()
            // If player can loot armor stand
            || ChunkUtils.canPlayerLootChestsInChunk(player, this.getBlockPos()))
        {
            ItemStack handStack = player.getStackInHand( hand );
            
            if (!this.shouldHideBasePlate()) {
                if (player.isSneaking() && handStack.isEmpty() && (vec3d.y < 0.5D)) {
                    this.setHideBasePlate(true);
                    
                    this.dropStack(new ItemStack(Items.SMOOTH_STONE_SLAB, 1));
                    
                    callback.setReturnValue(ActionResult.SUCCESS);
                    return;
                }
            } else if (handStack.getItem().equals(Items.SMOOTH_STONE_SLAB)) {
                handStack.decrement(1);
                this.setHideBasePlate(false);
                
                callback.setReturnValue(ActionResult.SUCCESS);
                return;
            }
            
            // Take away the arms
            if (this.shouldShowArms()) {
                if (!player.isSneaking() && handStack.getItem().equals(Items.STICK)) {
                    // Get the next pose after the current
                    ArmorStandPose pose = ArmorStandPose.getCurrent(this)
                        .next();
                    
                    // Apply that pose
                    ArmorStandPose.apply(pose, this);
                    
                    // Tell the player
                    player.sendMessage(new LiteralText(CasingUtils.words(pose.name()
                        .replace('_', ' '))).formatted(Formatting.YELLOW), true);
                    
                    // Eat the interaction
                    callback.setReturnValue(ActionResult.SUCCESS);
                    return;
                } else if (player.isSneaking() && handStack.isEmpty()) {
                    this.setShowArms(false);
                    
                    ItemStack mainHand = this.getStackInHand(Hand.MAIN_HAND);
                    ItemStack offHand = this.getStackInHand(Hand.OFF_HAND);
                    
                    // Remove items from hands
                    if (!mainHand.isEmpty()) {
                        this.dropStack(mainHand);
                        this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    }
                    if (!offHand.isEmpty()) {
                        this.dropStack(offHand);
                        this.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                    }
                    
                    // Drop the arms on the ground
                    this.dropStack(new ItemStack(Items.STICK, 2));
                    
                    callback.setReturnValue(ActionResult.SUCCESS);
                    return;
                }
            }
            
            // Add arms
            else if (handStack.getItem().equals(Items.STICK) && (handStack.getCount() >= 2)) {
                handStack.decrement(2);
                this.setShowArms(true);
                
                callback.setReturnValue(ActionResult.SUCCESS);
                return;
            }
            
            return;
        }
        
        // Player sound
        this.playSound(EntityUtils.getLockSound(this), 1, 1);
        
        WorldChunk chunk = this.world.getWorldChunk(this.getBlockPos());
        
        // Display that this item can't be opened
        TitleUtils.showPlayerAlert(player, Formatting.WHITE, TranslatableServerSide.text( player, "claim.block.locked",
            EntityUtils.getLockedName(this),
            (chunk == null ? new LiteralText("unknown player").formatted(Formatting.LIGHT_PURPLE) : ((IClaimedChunk) chunk).getOwnerName(player, this.getBlockPos()))
        ));
        
        callback.setReturnValue(ActionResult.FAIL);
    }
    
    /*
     * Player Death Chests
     */
    
    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // If the corpse belongs to the player
        if ( (this.corpsePlayerUUID != null) && player.getUuid().equals(this.corpsePlayerUUID)) {
            this.returnItemsToPlayer(player);
            return;
        }
        // Regular collision
        super.onPlayerCollision(player);
    }
    
    @Override
    public void setCorpseData(UUID owner, NbtList inventory, NbtList backpack) {
        this.corpsePlayerUUID = owner;
        this.corpsePlayerItems = inventory;
        this.corpsePlayerBackpack = backpack;
    }
    private void giveCorpseItems(@NotNull final PlayerEntity player) {
        Iterator<NbtElement> items;
        
        // Get all of the items to give back
        if (this.corpsePlayerItems != null) {
            items = this.corpsePlayerItems.iterator();
            while (items.hasNext()) {
                // Create the item from the tag
                ItemStack itemStack = ItemStack.fromNbt((NbtCompound) items.next());
                
                // Try equipping the item if the slot is available
                EquipmentSlot slot = null;
                
                if (itemStack.getItem() instanceof ArmorItem && !EnchantmentHelper.hasBindingCurse(itemStack)) {
                    // If armor
                    slot = ((ArmorItem) itemStack.getItem()).getSlotType();
                } else if (itemStack.getItem().equals(Items.SHIELD)) {
                    // If shield
                    slot = EquipmentSlot.OFFHAND;
                }
                
                // If slot is set, equip it there (If allowed)
                if ((slot != null) && player.getEquippedStack(slot).getItem() == Items.AIR)
                    player.equipStack(slot, itemStack);
                else // Add to the inventory (If not equipped)
                    player.getInventory().offerOrDrop(itemStack);
                
                // Remove from the iterator (Tag list)
                items.remove();
            }
        }
        
        // Get all of the backpack items back
        if (this.corpsePlayerBackpack != null) {
            items = this.corpsePlayerBackpack.iterator();
            while (items.hasNext()) {
                // Create the item from the tag
                ItemStack itemStack = ItemStack.fromNbt((NbtCompound) items.next());
                PlayerBackpack backpack = ((BackpackCarrier) player).getBackpack();
                
                // Attempt to put items into the backpack
                if ((backpack == null) || (!backpack.insertStack(itemStack)))
                    player.getInventory().offerOrDrop(itemStack);
                
                // Remove from the iterator (Tag list)
                items.remove();
            }
        }
    }
    private void returnItemsToPlayer(@NotNull final PlayerEntity player) {
        // Give the items back to the player
        this.giveCorpseItems(player);
        
        if (((this.corpsePlayerItems == null) || this.corpsePlayerItems.isEmpty()) && ((this.corpsePlayerBackpack == null) || this.corpsePlayerBackpack.isEmpty())) {
            BlockPos blockPos = this.getBlockPos().up();
            
            // Play sound
            player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
            
            // Spawn particles
            ((ServerWorld) this.world).spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                blockPos.getX() + 0.5D,
                blockPos.getY() + 1.0D,
                blockPos.getZ() + 0.5D,
                150,
                1.0D,
                0.5D,
                1.0D,
                0.3D
            );
            
            // Remove the armor stand
            this.discard();
        }
    }
    
    @Inject(at=@At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(NbtCompound tag, CallbackInfo callback) {
        // Save the player warp location for restarts
        if ( this.corpsePlayerUUID != null ) {
            tag.putUuid("corpsePlayerUUID", this.corpsePlayerUUID);
            if ((this.corpsePlayerItems != null) && (!this.corpsePlayerItems.isEmpty()))
                tag.put("corpsePlayerItems", this.corpsePlayerItems);
            if ((this.corpsePlayerBackpack != null) && (!this.corpsePlayerBackpack.isEmpty()))
                tag.put("corpsePlayerBackpack", this.corpsePlayerBackpack);
        }
    }
    @Inject(at=@At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(NbtCompound tag, CallbackInfo callback) {
        if ( NbtUtils.hasUUID(tag, "corpsePlayerUUID") ) {
            this.corpsePlayerUUID = NbtUtils.getUUID(tag, "corpsePlayerUUID");
            if (tag.contains("corpsePlayerItems", NbtElement.LIST_TYPE))
                this.corpsePlayerItems = tag.getList("corpsePlayerItems", NbtElement.COMPOUND_TYPE);
            if (tag.contains("corpsePlayerBackpack", NbtElement.LIST_TYPE))
                this.corpsePlayerBackpack = tag.getList("corpsePlayerBackpack", NbtElement.COMPOUND_TYPE);
        }
    }
}
