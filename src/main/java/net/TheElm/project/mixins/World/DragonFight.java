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

import net.TheElm.project.config.SewingMachineConfig;
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

@Mixin(EnderDragonFight.class)
public abstract class DragonFight {
    
    @Shadow
    private ServerWorld world;
    @Shadow
    private ServerBossBar bossBar;
    @Shadow
    private int playerUpdateTimer;
    
    @Shadow private native void generateEndPortal(boolean boolean_1);
    @Shadow private native void generateNewEndGateway();
    @Shadow private native void updatePlayers();
    @Shadow private native BlockPattern.Result findEndPortal();
    
    private boolean sewingMachinePortalCheck = false;
    private int sewingMachineEndPlayers = 0;
    
    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    public void spawnStateChange(CallbackInfo callback) {
        int players = 0;
        if ( ( players = this.bossBar.getPlayers().size() ) < SewingMachineConfig.INSTANCE.DRAGON_PLAYERS.get() ) {
            // Mimic the primary tick function
            if (++this.playerUpdateTimer >= 20) {
                this.updatePlayers();
                this.playerUpdateTimer = 0;
            }
            
            // Update the players in the end
            if ( players != this.sewingMachineEndPlayers ) {
                NumberFormat formatter = NumberFormat.getInstance();
                
                // Show notice to players in The End
                TitleUtils.showPlayerAlert( this.world, new LiteralText( formatter.format(players) + " / " + formatter.format(SewingMachineConfig.INSTANCE.DRAGON_PLAYERS.get()) + " required players to fight the dragon." ));
                
                this.sewingMachineEndPlayers = players;
            }
            
            // Make sure the necessities exist
            if (!this.sewingMachinePortalCheck) {
                this.generateEndPortal(true);
                this.generateNewEndGateway();
                
                this.sewingMachinePortalCheck = true;
            }
            
            // Set not visible
            this.bossBar.setVisible( false );
            
            // Cancel the tick that allows the dragon to spawn
            callback.cancel();
        }
    }
    
    @Inject(at = @At("HEAD"), method = "createDragon", cancellable = true)
    public void dragonCreation(CallbackInfoReturnable<EnderDragonEntity> callback) {
        System.out.println( "Creating a dragon!" );
    }
    
}
