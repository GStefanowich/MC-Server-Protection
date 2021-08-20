package net.TheElm.project.mixins.Entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragonEntity.class)
public abstract class Enderdragon extends MobEntity implements Monster {
    
    protected Enderdragon(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "net/minecraft/world/World.syncGlobalEvent(ILnet/minecraft/util/math/BlockPos;I)V"), method = "updatePostDeath")
    public void deathSoundPlay(@NotNull World world, int eventId, @NotNull BlockPos pos, int data) {
        // Play the global event only in the world
        world.syncWorldEvent(eventId, pos, data);
    }
    
}
