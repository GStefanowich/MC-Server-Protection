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

import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerCorpse;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.UUID;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStand extends LivingEntity implements PlayerCorpse {
    
    private UUID corpsePlayerUUID = null;
    private ListTag corpsePlayerItems = null;
    
    protected ArmorStand(EntityType<? extends LivingEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    /*
     * Armor Stand Protections
     */
    
    @Inject(at = @At("HEAD"), method = "interactAt", cancellable = true)
    public void interactedWith(PlayerEntity player, Vec3d vec3d, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        // Armor Stand is a corpse
        if ( this.corpsePlayerUUID != null ) {
            // Return the items back to their owner
            if (player.getUuid().equals(this.corpsePlayerUUID)) {
                this.returnItemsToPlayer( player );
            } else {
                // Deny if the corpse does not belong to this player
                callback.setReturnValue(ActionResult.FAIL);
            }
            return;
        }
        
        // Player is in creative
        if ((player.isCreative() && SewingMachineConfig.INSTANCE.CLAIM_CREATIVE_BYPASS.get()) || player.isSpectator())
            return;
        
        // If player can loot armor stand
        if (ChunkUtils.canPlayerLootChestsInChunk(player, this.getBlockPos()))
            return;
        
        // Player sound
        this.playSound( EntityUtils.getLockSound( this ), 1, 1 );
        
        WorldChunk chunk = world.getWorldChunk( this.getBlockPos() );
        
        // Display that this item can't be opened
        TitleUtils.showPlayerAlert( player, Formatting.WHITE, TranslatableServerSide.text( player, "claim.block.locked",
            EntityUtils.getLockedName( this ),
            ( chunk == null ? new LiteralText( "unknown player" ).formatted(Formatting.LIGHT_PURPLE) : ((IClaimedChunk) chunk).getOwnerName( player ) )
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
            this.returnItemsToPlayer( player );
            return;
        }
        // Regular collision
        super.onPlayerCollision( player );
    }
    
    public void setCorpseData(UUID owner, ListTag inventory) {
        this.corpsePlayerUUID = owner;
        this.corpsePlayerItems = inventory;
    }
    private void giveCorpseItems(final PlayerEntity player) {
        World world = player.world;
        if ( this.corpsePlayerItems == null )
            return;
        
        // Get all of the items to give back
        Iterator<Tag> items = this.corpsePlayerItems.iterator();
        while (items.hasNext()) {
            // Create the item from the tag
            ItemStack itemStack = ItemStack.fromTag((CompoundTag) items.next());
            
            // Try equipping the item if the slot is available
            boolean equipped = false;
            EquipmentSlot slot = null;
            
            if (itemStack.getItem() instanceof ArmorItem) {
                // If armor
                slot = ((ArmorItem) itemStack.getItem()).getSlotType();
            } else if (itemStack.getItem().equals(Items.SHIELD)) {
                // If shield
                slot = EquipmentSlot.OFFHAND;
            }
            
            // If slot is set, equip it there (If allowed)
            if ((slot != null) && player.getEquippedStack( slot ).getItem() == Items.AIR) {
                player.setEquippedStack(slot, itemStack);
                equipped = true;
            }
            
            // Add to the inventory (If not equipped)
            if (!equipped)
                player.inventory.offerOrDrop(world, itemStack);
            
            // Remove from the iterator (Tag list)
            items.remove();
        }
        
        // Reset the tag list
        this.corpsePlayerItems = null;
    }
    private void returnItemsToPlayer(final PlayerEntity player) {
        // Give the items back to the player
        this.giveCorpseItems( player );
        BlockPos blockPos = this.getBlockPos().up();
        
        // Play sound
        player.playSound( SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f );
        
        // Spawn particles
        ((ServerWorld) this.world).spawnParticles( ParticleTypes.TOTEM_OF_UNDYING,
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
        this.destroy();
    }
    
    @Inject(at=@At("TAIL"), method = "writeCustomDataToTag")
    public void onSavingData(CompoundTag tag, CallbackInfo callback) {
        // Save the player warp location for restarts
        if ( this.corpsePlayerUUID != null ) {
            tag.putUuid("corpsePlayerUUID", this.corpsePlayerUUID);
            tag.put("corpsePlayerItems", this.corpsePlayerItems);
        }
    }
    @Inject(at=@At("TAIL"), method = "readCustomDataFromTag")
    public void onReadingData(CompoundTag tag, CallbackInfo callback) {
        if ( tag.hasUuid( "corpsePlayerUUID" ) ) {
            this.corpsePlayerUUID = tag.getUuid("corpsePlayerUUID");
            this.corpsePlayerItems = (ListTag) tag.getTag("corpsePlayerItems");
        }
    }
}
