package net.TheElm.project.mixins.Entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherEntity.class)
public abstract class WitherBoss extends HostileEntity implements RangedAttackMob {

    protected WitherBoss(EntityType<? extends HostileEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/world/World.playGlobalEvent(ILnet/minecraft/util/math/BlockPos;I)V"), method = "mobTick")
    public void overrideWorldSound() {
        this.world.playLevelEvent( 1023, new BlockPos(this), 0 );
    }
    
}
