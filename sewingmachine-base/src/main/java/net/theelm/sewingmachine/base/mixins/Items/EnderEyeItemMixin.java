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

package net.theelm.sewingmachine.base.mixins.Items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

/**
 * Created on Jun 06 2021 at 11:25 PM.
 * By greg in SewingMachineMod
 */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin extends Item {
    public EnderEyeItemMixin(Settings settings) {
        super(settings);
    }
    
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;locateStructure(Lnet/minecraft/registry/tag/TagKey;Lnet/minecraft/util/math/BlockPos;IZ)Lnet/minecraft/util/math/BlockPos;"), method = "use")
    public BlockPos trySwapStructure(ServerWorld instance, TagKey<Structure> structureTag, BlockPos pos, int radius, boolean skipExistingChunks, World world, PlayerEntity user, Hand hand) {
        // The blockpos location that was found
        BlockPos location = null;
        
        ItemStack inHand = user.getStackInHand(hand);
        NbtCompound throwDat = inHand.getSubNbt("throw");
        
        if (throwDat == null)
            location = instance.locateStructure(structureTag, pos, radius, skipExistingChunks);
        else {
            int uses = 0;
            if (throwDat.contains("uses", NbtElement.NUMBER_TYPE))
                uses = throwDat.getInt("uses");
            else {
                // Destroy the item that has no more uses
                inHand.decrement(inHand.getCount());
                return null;
            }
            
            int strength = 0;
            NbtCompound baseDat = inHand.getOrCreateNbt();
            if (baseDat.contains("strength", NbtElement.NUMBER_TYPE))
                strength = baseDat.getInt("strength");
            
            // Try to locate the biome given in the NBT
            if (throwDat.contains("biome", NbtElement.STRING_TYPE)) {
                Identifier biomeId = new Identifier(throwDat.getString("biome"));
                Biome biome = world.getRegistryManager().get(RegistryKeys.BIOME).get(biomeId);
                if (biome != null)
                    location = Objects.requireNonNull(instance.locateBiome(biomeRegistryEntry ->
                        biomeRegistryEntry.value() == biome, pos, (radius * 10) * (6 + strength * 2), 32, 64)).getFirst();
            } else if (throwDat.contains("structure", NbtElement.STRING_TYPE)) {
                location = instance.locateStructure(structureTag, pos, (radius * 10) * (6 + strength * 2), skipExistingChunks);
            }
            
            // Update the remaining uses
            if (uses > 0 && location != null)
                throwDat.putInt("uses", uses - 1);
            else user.sendMessage(MutableText.of(new LiteralTextContent("Couldn't find any nearby location")).formatted(Formatting.RED), true);
        }
        
        return location;
    }
    
}
