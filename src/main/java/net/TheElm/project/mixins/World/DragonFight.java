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

package net.TheElm.project.mixins.World;

import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.DragonLoot;
import net.TheElm.project.utilities.BossLootRewards;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Mixin(EnderDragonFight.class)
public abstract class DragonFight {
    
    private final Collection<UUID> seenPlayers = new HashSet<>();
    
    private boolean sewingMachineCheckDragon = false;
    private boolean sewingMachineDragonShouldExist = false;
    
    /*
     * Dragon Info
     */
    
    @Shadow private ServerWorld world;
    @Shadow private ServerBossBar bossBar;
    @Shadow private UUID dragonUuid;
    @Shadow private boolean dragonKilled;
    @Shadow private boolean previouslyKilled;
    
    /*
     * Players in The End
     */
    
    @Shadow private int playerUpdateTimer;
    private int sewingMachineEndPlayers = 0;
    @Shadow private native void updatePlayers();
    
    /*
     * Async Checks for Dragons/Portals
     */
    
    @Shadow private boolean doLegacyCheck;
    private boolean sewingMachineDoPortalCheck = true;
    private boolean sewingMachinePortalCleanup = true;
    @Shadow private native void convertFromLegacy();
    private void convertFromLegacy(boolean createPortal) {
        this.previouslyKilled = this.worldContainsEndPortal();
        if ((!this.previouslyKilled) && createPortal && (this.findEndPortal() == null)) {
            this.generateEndPortal(false);
        }
        
        List<EnderDragonEntity> dragons;
        this.dragonKilled = (dragons = this.world.getAliveEnderDragons()).isEmpty();
        if (!this.dragonKilled) {
            EnderDragonEntity dragon = dragons.get(0);
            this.dragonUuid = dragon.getUuid();
            if (!this.previouslyKilled) {
                dragon.remove();
                this.dragonUuid = null;
            }
        }
        
        if (!this.previouslyKilled && this.dragonKilled) {
            this.dragonKilled = false;
        }
    }
    @Shadow private native void generateEndPortal(boolean boolean_1);
    @Shadow private native void generateNewEndGateway();
    @Shadow private native BlockPattern.Result findEndPortal();
    @Shadow private native boolean worldContainsEndPortal();
    @Shadow private native boolean loadChunks();
    
    /*
     * Methods
     */
    
    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    public void spawnStateChange(CallbackInfo callback) {
        int players = this.bossBar.getPlayers().size();
        
        // Check if a dragon already exists
        if ( !this.sewingMachineCheckDragon ) {
            this.sewingMachineDragonShouldExist = !this.world.getAliveEnderDragons().isEmpty();
            this.sewingMachineCheckDragon = true;
        }
        
        // Attempt to set the ability to spawn to TRUE
        if ( !this.sewingMachineDragonShouldExist )
            this.sewingMachineDragonShouldExist = ( players >= SewingMachineConfig.INSTANCE.DRAGON_PLAYERS.get() );
        
        // If the ability to spawn is FALSE
        if ( !this.sewingMachineDragonShouldExist ) {
            // Mimic the primary tick function
            if ((++this.playerUpdateTimer) >= 20) {
                this.updatePlayers();
                this.playerUpdateTimer = 0;
            }
            
            // Update the players in the end
            if ( players != this.sewingMachineEndPlayers ) {
                CoreMod.logDebug("Changed players that are in The End");
                
                NumberFormat formatter = NumberFormat.getInstance();
                
                // Set health bar to not visible
                this.bossBar.setVisible( false );
                
                // Show notice to players in The End
                if ( !this.dragonKilled )
                    TitleUtils.showPlayerAlert( this.world, new LiteralText( formatter.format(players) + " / " + formatter.format(SewingMachineConfig.INSTANCE.DRAGON_PLAYERS.get()) + " required players to fight the dragon." ));
                
                this.sewingMachineEndPlayers = players;
            }
            
            // Make sure the necessities exist
            if ((!this.bossBar.getPlayers().isEmpty()) && this.loadChunks()) {
                // Do the legacy dragon check
                if ( this.doLegacyCheck ) {
                    this.convertFromLegacy( false );
                    this.doLegacyCheck = false;
                }
                
                // Check that we have an open end portal
                if ( this.sewingMachineDoPortalCheck ) {
                    CoreMod.logDebug("Doing the Portal Check");
                    
                    // If an end portal doesn't already exist
                    if ((!this.worldContainsEndPortal()) && (this.findEndPortal() == null)) {
                        CoreMod.logDebug("Creating a new End Portal");
                        
                        // Create an OPEN portal
                        this.generateEndPortal(true);
                        
                        // Generate a single end gateway
                        this.generateNewEndGateway();
                    }
                    
                    // Portal has been checked
                    this.sewingMachineDoPortalCheck = false;
                }
            }
            
            // Cancel the tick that allows the dragon to spawn
            callback.cancel();
        }
        else if (this.sewingMachinePortalCleanup && this.loadChunks()) {
            // Close any open end portal
            this.generateEndPortal(false);
            
            // Cleanup has completed
            this.sewingMachinePortalCleanup = false;
            
            // Delay the continuation
            this.playerUpdateTimer = -100; // Set our tick count to -100 (6 seconds til dragon spawn)
            this.bossBar.clearPlayers();
            callback.cancel();
        }
    }
    
    @Inject(at = @At("RETURN"), method = "createDragon")
    public void dragonCreation(CallbackInfoReturnable<EnderDragonEntity> callback) {
        EnderDragonEntity dragon = callback.getReturnValue();
        if (dragon != null)
            MessageUtils.sendToAll(new LiteralText("A new Ender Dragon has arrived in The End.")
                .formatted(Formatting.YELLOW, Formatting.ITALIC), EntityUtils::isNotInTheEnd);
    }
    
    @Inject(at = @At("TAIL"), method = "dragonKilled")
    public void dragonDestroyed(EnderDragonEntity dragon, CallbackInfo callback) {
        if (this.dragonUuid.equals(dragon.getUuid()))
            this.sewingMachineDragonShouldExist = false;
        
        MessageUtils.consoleToOps(new LiteralText("An Ender Dragon was slain by " + this.seenPlayers.size() + " player(s)."));
        
        for (UUID playerId : this.seenPlayers) {
            if (!BossLootRewards.DRAGON_LOOT.addLoot(playerId, DragonLoot.createReward())) {
                ServerPlayerEntity player = ServerCore.getPlayer(playerId);
                if (player != null)
                    player.sendMessage(new LiteralText("You were not rewarded a drop from killing the Ender Dragon, your loot chest is full.")
                        .formatted(Formatting.RED));
            }
        }
        
        // Clear the list of seen players
        if ( this.world.getAliveEnderDragons().isEmpty() )
            this.seenPlayers.clear();
        
        if (SewingMachineConfig.INSTANCE.DRAGON_LOOT_END_ITEMS.get() || SewingMachineConfig.INSTANCE.DRAGON_LOOT_RARE_BOOKS.get()) {
            BlockPos chestPos = this.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, EndPortalFeature.ORIGIN);
            this.world.setBlockState(chestPos, Blocks.BLACK_SHULKER_BOX.getDefaultState());
            BlockEntity blockEntity = this.world.getBlockEntity(chestPos);
            if (blockEntity != null) {
                CompoundTag chestTag = blockEntity.toTag(new CompoundTag());
                chestTag.putString("BossLootContainer", BossLootRewards.DRAGON_LOOT.toString());
                
                blockEntity.fromTag(chestTag);
            }
        }
    }
    
    @Inject(at = @At("TAIL"), method = "updatePlayers")
    public void onUpdatePlayers(CallbackInfo callback) {
        List<EnderDragonEntity> dragons = this.world.getAliveEnderDragons();
        if (!dragons.isEmpty()) {
            // Get all players
            this.bossBar.getPlayers().stream().map(Entity::getUuid)
                .forEachOrdered(this.seenPlayers::add);
            
            // Update the dragons health
            int count = Math.max(this.seenPlayers.size() - 1, 0),
                baseHealth = 200;
            float newMax = baseHealth + (count * SewingMachineConfig.INSTANCE.DRAGON_ADDITIONAL_HEALTH.get());
            
            for (EnderDragonEntity dragon : dragons) {
                dragon.getMaximumHealth();
                
                float currentMax = dragon.getMaximumHealth();
                if (currentMax != newMax) {
                    float currentHealth = dragon.getHealth();
                    
                    dragon.getAttributeInstance(EntityAttributes.MAX_HEALTH)
                        .setBaseValue(newMax);
                    dragon.setHealth(currentHealth + (newMax - currentMax));
                }
            }
        }
    }
    
    @Inject(at = @At("TAIL"), method = "generateEndPortal")
    public void onGeneratePortal(boolean opened, CallbackInfo callback) {
        // Get the portal
        BlockPattern.Result portal = this.findEndPortal();
        if (portal != null) {
            // Get the positions for the portal
            /*BlockPos frontTopLeft = portal.getFrontTopLeft();
            BlockPos backBottomRight = frontTopLeft.offset(portal.getForwards(), portal.getWidth())
                .offset(Direction.WEST, portal.getWidth())
                .offset(portal.getUp().getOpposite(), portal.getHeight());
            
            // Claim the portal
            ChunkUtils.claimSlices(this.world, CoreMod.spawnID, frontTopLeft.up(2), backBottomRight);*/
        }
    }
}
