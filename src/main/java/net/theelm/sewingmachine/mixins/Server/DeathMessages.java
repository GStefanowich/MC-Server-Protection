package net.theelm.sewingmachine.mixins.Server;

import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageTracker.class)
public abstract class DeathMessages {
    
    @Shadow @Final
    private LivingEntity entity;
    
    @Inject(at = @At("RETURN"), method = "getDeathMessage", cancellable = true)
    public void onFetchMessage(CallbackInfoReturnable<Text> callback) {
        if (this.entity instanceof PlayerEntity) {
            Text death = callback.getReturnValue();
            callback.setReturnValue(new LiteralText("On ")
                .append(MessageUtils.getWorldTime(this.entity.getEntityWorld()))
                .append(", ")
                .append(death));
        }
    }
    
}
