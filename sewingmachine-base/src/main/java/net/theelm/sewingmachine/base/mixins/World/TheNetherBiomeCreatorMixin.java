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

package net.theelm.sewingmachine.base.mixins.World;

import net.minecraft.world.biome.TheNetherBiomeCreator;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TheNetherBiomeCreator.class)
public class TheNetherBiomeCreatorMixin {
    
    /*@Inject(at = @At("RETURN"), method = {"createNetherWastes", "createSoulSandValley", "createBasaltDeltas", "createCrimsonForest", "createWarpedForest"})
    private static void addSpawn(CallbackInfoReturnable<Biome> callback) {
        if (SewConfig.get(SewCoreConfig.PREVENT_NETHER_ENDERMEN)) {
            // Get the super
            Biome biome = callback.getReturnValue();
            if (biome != null) {
                SpawnSettings settings = biome.getSpawnSettings();
                
                // If enabled
                settings.getSpawnEntry(SpawnGroup.MONSTER).removeIf(entity -> entity.type.equals(EntityType.ENDERMAN));
            }
        }
    }*/
    
}
