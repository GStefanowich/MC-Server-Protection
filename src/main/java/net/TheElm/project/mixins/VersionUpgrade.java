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

package net.TheElm.project.mixins;

import com.mojang.serialization.Codec;
import net.TheElm.project.CoreMod;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created on Dec 02 2021 at 6:21 AM.
 * By greg in SewingMachineMod
 */
@Mixin(VersionedChunkStorage.class)
public class VersionUpgrade {
    private static final String sewingMachineSerializationPlayer = "sewingMachineOwnerUUID";
    private static final String sewingMachineSerializationTown = "sewingMachineTownUUID";
    private static final String sewingMachineSerializationSlices = "sewingMachineOwnerSlices";
    
    private int c = 0;
    
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtCompound;remove(Ljava/lang/String;)V"), method = "updateChunkNbt")
    public void onUpdateChunkNbt(RegistryKey<World> worldKey, Supplier<PersistentStateManager> persistentStateManagerFactory, NbtCompound nbt, Optional<RegistryKey<Codec<? extends ChunkGenerator>>> generatorCodecKey, CallbackInfoReturnable<NbtCompound> callback) {
        /*if (this.c++ == 0)
            CoreMod.logError(new Exception("Stacktrace Upgrading"));*/
        
        /*NbtCompound context = nbt.getCompound("__context");
        if (context == null || context.isEmpty())
            return;*/
        /*NbtCompound tag = new NbtCompound();
        if (nbt.contains(sewingMachineSerializationPlayer))
            tag.put(sewingMachineSerializationPlayer, nbt.get(sewingMachineSerializationPlayer));
        if (nbt.contains(sewingMachineSerializationTown))
            tag.put(sewingMachineSerializationTown, nbt.get(sewingMachineSerializationTown));
        if (nbt.contains(sewingMachineSerializationSlices)) {
            NbtElement element = nbt.get(sewingMachineSerializationSlices);
            if ((!(element instanceof NbtList list)) || !list.isEmpty())
                tag.put(sewingMachineSerializationSlices, element);
        }
        
        if (!tag.isEmpty())
            System.out.println("Checking chunk update NBT " + tag);*/
    }
}
