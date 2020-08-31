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
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HorseEntity.class)
public abstract class HorseRide extends HorseBaseEntity {
    
    protected HorseRide(EntityType<? extends HorseBaseEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("HEAD"), method = "interactMob", cancellable = true)
    private void tryHorseMount(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> callback) {
        // If the player is in creative, allow
        if ((player.isCreative() && SewConfig.get(SewConfig.CLAIM_CREATIVE_BYPASS)) || player.isSpectator())
            return;
        
        // If player owns the horse, allow
        if (player.getUuid().equals(this.getOwnerUuid()))
            return;
        
        // If riding is allowed, allow
        if (ChunkUtils.canPlayerRideInChunk(player, this.getBlockPos()))
            return;
        
        Text owner;
        
        if ( this.getOwnerUuid() != null ) {
            // Get the name of the HORSES OWNER
            owner = ClaimantPlayer.get( this.getOwnerUuid() )
                .getName();
        } else {
            // Get the name of the CHUNK OWNER
            WorldChunk chunk = world.getWorldChunk( this.getBlockPos() );
            if ( chunk != null )
                owner = ((IClaimedChunk) chunk).getOwnerName( player, this.getBlockPos() );
            else
                owner = new LiteralText( "unknown player" )
                    .formatted(Formatting.LIGHT_PURPLE);
        }
        
        // Horse makes an angry sound at the player
        this.playSound( EntityUtils.getLockSound( this ),0.5f, 1f );
        
        // Display that this item can't be ridden
        TitleUtils.showPlayerAlert( player, Formatting.WHITE, TranslatableServerSide.text( player, "claim.block.locked",
            EntityUtils.getLockedName( this ),
            owner
        ));
        
        // Disallow
        callback.setReturnValue( false );
    }
    
}
