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
import net.TheElm.project.interfaces.ClaimsAccessor;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.SpawnerMob;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class TamedLead extends LivingEntity implements SpawnerMob {
    
    private boolean wasFromSpawner = false;
    
    protected TamedLead(EntityType<? extends AnimalEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Nullable
    private UUID getOwnerUuid() {
        if ( ((LivingEntity) this) instanceof TameableEntity tameableEntity )
            return tameableEntity.getOwnerUuid();
        if ( ((LivingEntity) this) instanceof HorseBaseEntity horseBaseEntity )
            return horseBaseEntity.getOwnerUuid();
        return null;
    }
    
    @Override
    public boolean checkIfFromSpawner() {
        return this.wasFromSpawner;
    }
    
    /**
     * Control attaching of leashes to this mob
     * @param player The entity to attach to
     * @param hand The hand the player interacted with
     * @param callback The callback
     */
    @Inject(at = @At("HEAD"), method = "interactWithItem", cancellable = true)
    private void onAttachLeash(@NotNull final PlayerEntity player, @NotNull final Hand hand, @NotNull final CallbackInfoReturnable<ActionResult> callback) {
        // If player is in creative mode, bypass permissions
        if ((player.isCreative() && SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS)) || player.isSpectator())
            return;
        
        // If player is the owner of the entity
        if ((this.getOwnerUuid() != null) && (player.getUuid().equals(this.getOwnerUuid())))
            return;
        
        // If player can interact with tameable
        if (ChunkUtils.canPlayerInteractFriendlies(player, this.getBlockPos()))
            return;
        
        ActionResult result;
        ItemStack stack = player.getStackInHand(hand);
        
        if (!(stack.getItem() == Items.LEAD || stack.getItem() == Items.NAME_TAG))
            result = ActionResult.PASS;
        else {
            result = ActionResult.CONSUME;
            
            Text owner;
            if ( this.getOwnerUuid() != null ) {
                // Get the name of the HORSES OWNER
                owner = ((ClaimsAccessor)this.getServer()).getClaimManager()
                    .getPlayerClaim(this.getOwnerUuid())
                    .getName();
            } else {
                // Get the name of the CHUNK OWNER
                WorldChunk chunk = this.getEntityWorld().getWorldChunk(this.getBlockPos());
                if ( chunk != null )
                    owner = ((IClaimedChunk) chunk).getOwnerName( player, this.getBlockPos() );
                else
                    owner = new LiteralText( "unknown player" )
                        .formatted(Formatting.LIGHT_PURPLE);
            }
            
            // Horse makes an angry sound at the player
            this.playSound(EntityUtils.getLockSound(this),0.5f, 1f);

            // Display that this item can't be opened
            TitleUtils.showPlayerAlert(player, Formatting.WHITE, TranslatableServerSide.text(player, "claim.block.locked",
                EntityUtils.getLockedName(this),
                owner
            ));
            
            // Make sure the client knows that they are not leashing
            if (player instanceof ServerPlayerEntity && stack.getItem() == Items.LEAD) {
                EntityUtils.resendInventory(player);
                
                ((ServerPlayerEntity) player).networkHandler.sendPacket(
                    new EntityAttachS2CPacket(this, null)
                );
            }
        }
        
        // Decline allowing leading
        callback.setReturnValue(result);
    }
    
    /**
     * Try to dismount mobs out of vehicles
     * @param player The player that interacted with this mob
     * @param hand The hand the player uses
     * @param callback The callback
     */
    @Inject(at = @At("HEAD"), method = "interactMob", cancellable = true)
    private void onInteractMob(@NotNull PlayerEntity player, @NotNull Hand hand, @NotNull final CallbackInfoReturnable<ActionResult> callback) {
        // If the player is not sneaking or is holding an item
        if (!player.isSneaking() || !player.getStackInHand(hand).isEmpty())
            return;
        Entity vehicle;
        if ((vehicle = this.getVehicle()) != null) {
            // Prevent removing baby striders from their parents
            if (((LivingEntity) this) instanceof StriderEntity && this.isBaby() && vehicle instanceof StriderEntity)
                return;
            // If mob is riding a player (Ie; a parrot) don't let other players dismount it
            if (vehicle instanceof PlayerEntity && player != vehicle)
                return;
            
            this.stopRiding();
            callback.setReturnValue(ActionResult.SUCCESS);
        }
    }
    
    /**
     * When the mob is initialized
     * We want to know if it is from a spawner
     * @param world World spawned in
     * @param difficulty Area difficulty
     * @param spawnReason Cause of spawning
     * @param entityData Current entity data
     * @param entityNbt Current entity nbt
     * @param callback Return type
     */
    @Inject(at = @At("HEAD"), method = "initialize")
    private void onInitializeMob(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt, CallbackInfoReturnable<EntityData> callback) {
        if (spawnReason == SpawnReason.SPAWNER || spawnReason == SpawnReason.SPAWN_EGG)
            this.wasFromSpawner = true;
    }
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Save the players money
        if (this.checkIfFromSpawner())
            tag.putBoolean("FromSpawner", this.checkIfFromSpawner());
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(@NotNull NbtCompound tag, @NotNull CallbackInfo callback) {
        // Read the players money
        if (tag.contains("FromSpawner", NbtElement.NUMBER_TYPE))
            this.wasFromSpawner = tag.getBoolean("FromSpawner");
    }
}
