package net.TheElm.project.mixins.Server;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.File;

@Mixin(DimensionType.class)
public class SaveFolder {
    
    // TODO: Check if this Mixin SHOULD be applied
    
    @Overwrite
    public static File getSaveDirectory(RegistryKey<World> worldRef, File root) {
        return new File(root, "dimensions/" + worldRef.getValue().getNamespace() + "/" + worldRef.getValue().getPath());
    }
    
}
