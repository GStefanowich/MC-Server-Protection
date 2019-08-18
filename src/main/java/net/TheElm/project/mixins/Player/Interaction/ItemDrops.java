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

import com.mojang.authlib.GameProfile;
import net.TheElm.project.interfaces.PlayerServerLanguage;
import net.minecraft.container.ContainerListener;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ItemDrops extends PlayerEntity implements ContainerListener, PlayerServerLanguage {
    
    @Shadow private String clientLanguage;
    
    public ItemDrops(World world_1, GameProfile gameProfile_1) {
        super(world_1, gameProfile_1);
    }
    
    public String getClientLanguage() {
        return this.clientLanguage;
    }
    
    @Inject(at = @At("RETURN"), method = "dropItem")
    private void onItemDrop(ItemStack itemStack, boolean boolean1, boolean boolean2, CallbackInfoReturnable<ItemEntity> callback) {
        ItemEntity entity = callback.getReturnValue();
        if ( ( entity != null ) && ( this.getUuid() != null ) )
            entity.setOwner( this.getUuid() );
    }
    
}
