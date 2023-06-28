package net.theelm.sewingmachine.objects;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.entry.RegistryEntry;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;

public class SawDamage extends DamageSource {
    public SawDamage(RegistryEntry<DamageType> type) {
        super(type);
    }
    
    @Override
    public Text getDeathMessage(LivingEntity livingEntity) {
        return TextUtils.literal()
            .append(livingEntity.getDisplayName())
            .append(" is now resting in pieces.");
    }
    
}
