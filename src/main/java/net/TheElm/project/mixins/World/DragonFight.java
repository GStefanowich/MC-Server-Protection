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
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.List;
import java.util.UUID;

@Mixin(EnderDragonFight.class)
public abstract class DragonFight {
    
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
            this.generateEndPortal( false );
            
            // Cleanup has completed
            this.sewingMachinePortalCleanup = false;
            
            // Delay the continuation
            this.playerUpdateTimer = -100; // Set our tick count to -100 (6 seconds til dragon spawn)
            this.bossBar.clearPlayers();
            callback.cancel();
        }
    }
    
    @Inject(at = @At("TAIL"), method = "createDragon")
    public void dragonCreation(CallbackInfoReturnable<EnderDragonEntity> callback) {
        EnderDragonEntity dragon = callback.getReturnValue();
        if (dragon != null)
            CoreMod.logInfo( "A new Ender Dragon has been spawned at " + MessageUtils.blockPosToString( dragon.getBlockPos() ));
    }
    
    @Inject(at = @At("TAIL"), method = "dragonKilled")
    public void dragonDestroyed(EnderDragonEntity dragon, CallbackInfo callback) {
        if (this.dragonUuid.equals(dragon.getUuid()))
            this.sewingMachineDragonShouldExist = false;
        CoreMod.logInfo( "An Ender Dragon was killed" );
    }
    
}
