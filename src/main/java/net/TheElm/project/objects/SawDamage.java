package net.TheElm.project.objects;

import net.TheElm.project.utilities.text.TextUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;

public class SawDamage extends DamageSource {
    
    public static DamageSource SAW_BLADE = new SawDamage();
    
    protected SawDamage() {
        super("stoneCutter");
        this.setUnblockable();
    }
    
    public Text getDeathMessage(LivingEntity livingEntity) {
        return TextUtils.literal()
            .append(livingEntity.getDisplayName())
            .append(" is now resting in pieces.");
    }
    
}
