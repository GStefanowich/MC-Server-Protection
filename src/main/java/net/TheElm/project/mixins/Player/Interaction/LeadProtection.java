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

import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.EntityUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeashKnotEntity.class)
public abstract class LeadProtection extends AbstractDecorationEntity {
    
    protected LeadProtection(EntityType<? extends AbstractDecorationEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Inject(at = @At("HEAD"), method = "interact", cancellable = true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> callback) {
        if (ChunkUtils.canPlayerRideInChunk(player, this.getBlockPos()))
            return;
        
        WorldChunk chunk = this.getEntityWorld().getWorldChunk( this.getBlockPos() );
        
        // Display that this leash can't be removed
        TitleUtils.showPlayerAlert( player, Formatting.WHITE, TranslatableServerSide.text( player, "claim.block.locked",
            EntityUtils.getLockedName( this ),
            ( chunk == null ? new LiteralText( "unknown player" ).formatted(Formatting.LIGHT_PURPLE) : ((IClaimedChunk) chunk).getOwnerName( player, this.getBlockPos() ) )
        ));
        
        this.playSound(SoundEvents.ENTITY_LEASH_KNOT_BREAK,0.5f, 1f );
        callback.setReturnValue( false );
    }
    
}
