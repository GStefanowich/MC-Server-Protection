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

package net.theelm.sewingmachine.utilities;

import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.interfaces.MoneyHolder;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.interfaces.variables.EntityVariableFunction;
import net.theelm.sewingmachine.interfaces.variables.VariableFunction;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.StyleApplicator;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created on Dec 20 2021 at 7:34 PM.
 * By greg in SewingMachineMod
 */
public class EntityVariables {
    private static final @NotNull Map<String, VariableFunction> VARIABLES = new HashMap<>();
    static {
        // Get the regular name
        EntityVariables.add("name", (source, message, room, casing) -> TextUtils.literal(source.getName(), casing));
        
        // Get the uuid
        EntityVariables.add("uuid", (EntityVariableFunction)(source, entity, casing) -> TextUtils.literal(entity.getUuidAsString(), casing));
        
        // Get the player balance
        EntityVariables.add("balance", (EntityVariableFunction)(source, entity, casing) -> TextUtils.literal(entity instanceof ServerPlayerEntity player ? ((MoneyHolder) player).getPlayerWallet() : 0));
        
        // Entity X, Y, Z position
        EntityVariables.add("x", (EntityVariableFunction)(source, entity, casing) -> TextUtils.literal(entity.getBlockX()));
        EntityVariables.add("y", (EntityVariableFunction)(source, entity, casing) -> TextUtils.literal(entity.getBlockY()));
        EntityVariables.add("z", (EntityVariableFunction)(source, entity, casing) -> TextUtils.literal(entity.getBlockZ()));
        
        // Get the entity biome
        EntityVariables.add("biome", (EntityVariableFunction)(source, entity, casing) -> {
            World world = entity.getWorld();
            RegistryKey<Biome> registryKey = world.getBiome(entity.getBlockPos()).getKey().isPresent()? world.getBiome(entity.getBlockPos()).getKey().get() : BiomeKeys.PLAINS;
            Identifier identifier = registryKey.getValue();
            return Text.translatable("biome." + identifier.getNamespace() + "." + identifier.getPath());
        });
        
        // Get the entity world
        EntityVariables.add("world", (EntityVariableFunction)(source, entity, casing)
            -> EntityVariables.worldText(entity.getWorld().getRegistryKey(), casing, true));
        EntityVariables.add("w", (EntityVariableFunction)(source, entity, casing)
            -> EntityVariables.worldText(entity.getWorld().getRegistryKey(), casing, false));
    }
    
    private static Text worldText(@NotNull RegistryKey<World> dimension, @NotNull CasingUtils.Casing casing, boolean showAsLong) {
        // Create the text
        MutableText longer = DimensionUtils.longDimensionName(dimension, casing);
        
        // Create the hover event
        StyleApplicator formatting = DimensionUtils.dimensionColor(dimension);
        return ( showAsLong ? longer : DimensionUtils.shortDimensionName(dimension, CasingUtils.Casing.UPPER)
            .styled(MessageUtils.simpleHoverText(longer.styled(formatting))))
            .styled(formatting);
    }
    
    public static @NotNull Set<Map.Entry<String, VariableFunction>> entrySet() {
        return EntityVariables.VARIABLES.entrySet();
    }
    public static @Nullable VariableFunction get(@NotNull String key) {
        return EntityVariables.VARIABLES.get(key);
    }
    public static void add(@NotNull String key, VariableFunction function) {
        EntityVariables.VARIABLES.put(key, function);
    }
}
