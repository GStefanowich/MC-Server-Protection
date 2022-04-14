package net.TheElm.project.mixins.Server;

import net.TheElm.project.config.SewConfig;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(DimensionType.class)
public class SaveFolder {
    
    /**
     * Use the same "Dimensions" folder for saving ALL worlds, not just extra worlds
     * @param worldRef World registry key
     * @param directory Root file directory
     */
    @Inject(at = @At("HEAD"), method = "getSaveDirectory", cancellable = true)
    private static void getSaveDirectory(RegistryKey<Path> worldRef, Path directory, CallbackInfoReturnable<Path> callback) {
        if (SewConfig.get(SewConfig.WORLD_DIMENSION_FOLDERS))
            callback.setReturnValue(directory.resolve("dimensions").resolve(worldRef.getValue().getNamespace()).resolve(worldRef.getValue().getPath()));
    }
    
}
