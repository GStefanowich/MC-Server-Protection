package net.TheElm.project.mixins.Entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnderDragonEntity.class)
public abstract class Enderdragon extends MobEntity implements Monster {
    
    protected Enderdragon(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }
    
    
    
}
