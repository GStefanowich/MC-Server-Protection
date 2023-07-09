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

package net.theelm.sewingmachine.chat.mixins.Chat;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.theelm.sewingmachine.chat.interfaces.Nicknamable;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created on Jun 09 2023 at 3:50 AM.
 * By greg in sewingmachine
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements Nicknamable {
    // Nickname
    private Text playerNickname = null;
    
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    
    /*
     * Override the players display name to their nick
     */
    @Inject(at = @At("HEAD"), method = "getDisplayName", cancellable = true)
    public void getPlayerNickname(CallbackInfoReturnable<Text> callback) {
        if (((LivingEntity)this) instanceof ServerPlayerEntity) {
            Text nickname = this.getPlayerNickname();
            if (nickname != null)
                callback.setReturnValue(nickname);
        }
    }
    
    /*
     * Player names (Saved and handled cross dimension)
     */
    
    @Override
    public void setPlayerNickname(@Nullable Text nickname) {
        this.playerNickname = nickname;
        ServerBossBar healthBar = ((PlayerData) this).getHealthBar();
        if (healthBar != null)
            healthBar.setName(Text.literal("Player ").append(this.getDisplayName()).formatted(Formatting.WHITE));
    }
    @Nullable @Override
    public Text getPlayerNickname() {
        if (this.playerNickname == null)
            return null;
        return FormattingUtils.deepCopy(this.playerNickname);
    }
}
