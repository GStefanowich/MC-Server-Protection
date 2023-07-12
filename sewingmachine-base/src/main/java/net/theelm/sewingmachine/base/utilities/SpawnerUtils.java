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

package net.theelm.sewingmachine.base.utilities;

import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SpawnerUtils {
    private SpawnerUtils() {}
    
    public static @NotNull NbtCompound getDisplay(@NotNull MobSpawnerBlockEntity spawner) {
        return SpawnerUtils.getDisplay(spawner.createNbt());
    }
    
    public static @NotNull NbtCompound getDisplay(@NotNull ItemStack stack) {
        return SpawnerUtils.getDisplay(stack.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY));
    }
    
    public static @NotNull NbtCompound getDisplay(@NotNull NbtCompound entityData) {
        NbtCompound displayTag = new NbtCompound();
        NbtList loreTag = new NbtList();
        NbtUtils.add(loreTag, Text.literal("Contains:"));
        
        // Get the list of SpawnPotentials (Has fallback)
        NbtList spawnPotentials = entityData.getList("SpawnPotentials", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : spawnPotentials) {
            if (element instanceof NbtCompound spawnPotential) {
                NbtCompound data = spawnPotential.getCompound("data");
                NbtCompound entity = data.getCompound("entity");
                String id = entity.getString("id");
                if (!id.isEmpty()) {
                    Optional<EntityType<?>> optional = EntityType.get(id);
                    optional.ifPresent(type -> NbtUtils.add(loreTag, Text.literal("  ")
                        .append(Text.translatable(type.getTranslationKey())
                        .formatted(type.getSpawnGroup().isPeaceful() ? Formatting.GOLD : Formatting.RED))));
                }
            }
        }
        
        // Update the lore tag
        if (!loreTag.isEmpty())
            displayTag.put("Lore", loreTag);
        
        return displayTag;
    }
    
    public static int getCount(@NotNull MobSpawnerBlockEntity spawner) {
        NbtCompound base = spawner.createNbt();
        NbtList list = base.getList("SpawnPotentials", NbtElement.COMPOUND_TYPE);
        if (!list.isEmpty())
            return list.size();
        String mainId = SpawnerUtils.getEntityId(base.getCompound("SpawnData"));
        return mainId.isEmpty() ? 0 : 1;
    }
    
    /**
     * Get the Entity ID from NBT Data
     * @param compound
     * @return
     */
    private static @NotNull String getEntityId(@NotNull NbtCompound compound) {
        NbtCompound entity = compound.getCompound("entity");
        return entity.getString("id");
    }
    
    /**
     * After loading a Spawner, fix up it's NBT for the latest version
     * @param nbt
     */
    public static void fixupNbt(@NotNull NbtCompound nbt) {
        NbtList entityIds = nbt.getList("EntityIds", NbtElement.STRING_TYPE);
        if (entityIds.isEmpty())
            return;
        NbtCompound blockEntity = NbtUtils.getOrCreateSubNbt(nbt, BlockItem.BLOCK_ENTITY_TAG_KEY);
        
        // The main SpawnData value
        NbtCompound spawnData = null;
        
        // Reset and update SpawnPotentials
        blockEntity.remove("SpawnPotentials");
        NbtList spawnPotentials = NbtUtils.getOrCreateSubNbt(blockEntity, "SpawnPotentials", NbtList.class);
        
        for (NbtElement element : entityIds) {
            if (element instanceof NbtString entityId) {
                NbtCompound spawnPotential = new NbtCompound();
                spawnPotential.putInt("weight", 1);
                
                NbtCompound data = NbtUtils.getOrCreateSubNbt(spawnPotential, "data");
                NbtCompound entity = NbtUtils.getOrCreateSubNbt(data, "entity");
                entity.put("id", entityId);
                
                spawnPotentials.add(spawnPotential);
                
                // Set the first SpawnData
                if (spawnData == null)
                    spawnData = data;
            }
        }
        
        if (spawnData != null)
            blockEntity.put("SpawnData", spawnData);
        
        // Update the Lore display
        NbtCompound display = SpawnerUtils.getDisplay(blockEntity);
        if (display.isEmpty())
            nbt.remove("display");
        else nbt.put("display", display);
        
        nbt.remove("EntityIds");
    }
    
    public static boolean hasEntity(@NotNull ItemStack stack, @NotNull RegistryKey<EntityType<?>> key) {
        return SpawnerUtils.hasEntity(stack, key.getValue());
    }
    public static boolean hasEntity(@NotNull ItemStack stack, @NotNull EntityType<?> type) {
        return SpawnerUtils.hasEntity(stack, EntityType.getId(type));
    }
    public static boolean hasEntity(@NotNull ItemStack stack, @NotNull Identifier identifier) {
        NbtCompound block = NbtUtils.getOrCreateSubNbt(stack, BlockItem.BLOCK_ENTITY_TAG_KEY);
        NbtList spawnPotentials = NbtUtils.getOrCreateSubNbt(block, "SpawnPotentials", NbtList.class);
        if (!spawnPotentials.isEmpty()) {
            String id = identifier.toString();
            
            for (NbtElement element : spawnPotentials) {
                if (
                    element instanceof NbtCompound spawnPotential
                        && Objects.equals(
                        spawnPotential.getCompound("data").getCompound("entity").getString("id"),
                        id
                    )
                ) return true;
            }
        }
        
        return false;
    }
    
    public static boolean addEntity(@NotNull ItemStack stack, @NotNull RegistryKey<EntityType<?>> key, int weight) {
        return SpawnerUtils.addEntity(stack, key.getValue(), weight);
    }
    public static boolean addEntity(@NotNull ItemStack stack, @NotNull EntityType<?> type, int weight) {
        return SpawnerUtils.addEntity(stack, EntityType.getId(type), weight);
    }
    public static boolean addEntity(@NotNull ItemStack stack, @NotNull Identifier identifier, int weight) {
        if (!Objects.equals(stack.getItem(), Items.SPAWNER))
            return false;
        
        // Add mob to the list
        NbtCompound base = stack.getOrCreateNbt();
        NbtCompound block = NbtUtils.getOrCreateSubNbt(base, BlockItem.BLOCK_ENTITY_TAG_KEY);
        NbtCompound spawn = NbtUtils.getOrCreateSubNbt(block, "SpawnData");
        NbtCompound entity = NbtUtils.getOrCreateSubNbt(spawn, "entity");
        
        String id = identifier.toString();
        entity.put("id", NbtString.of(id));
        
        NbtList spawnPotentials = NbtUtils.getOrCreateSubNbt(block, "SpawnPotentials", NbtList.class);
        NbtCompound spawnPotential = NbtUtils.getOrCreateSubNbt(spawnPotentials, NbtCompound.class, entry
            -> entry.getCompound("data").getCompound("entity").getString("id").equals(id));
        if (spawnPotential.getInt("weight") != weight) {
            spawnPotential.put("data", spawn);
            spawnPotential.putInt("weight", weight);
            
            base.put("display", SpawnerUtils.getDisplay(stack));
            return true;
        }
        return false;
    }
    
    public static boolean removeEntity(@NotNull ItemEntity entity, int pos) {
        return SpawnerUtils.removeEntity(entity.getStack(), pos);
    }
    public static boolean removeEntity(@NotNull ItemStack stack, int pos) {
        if (!Objects.equals(stack.getItem(), Items.SPAWNER))
            return false;
        
        NbtCompound base = stack.getOrCreateNbt();
        NbtCompound block = NbtUtils.getOrCreateSubNbt(base, BlockItem.BLOCK_ENTITY_TAG_KEY);
        NbtList spawnPotentials = NbtUtils.getOrCreateSubNbt(block, "SpawnPotentials", NbtList.class);
        
        // Fail if already empty
        if (spawnPotentials.size() <= 1)
            return false;
        
        // Try removing the value at the given pos
        boolean removed = spawnPotentials.remove(pos) != null;
        
        // If the list is empty, remove the current SpawnData
        if (spawnPotentials.isEmpty())
            block.remove("SpawnData");
        
        if (removed)
            base.put("display", SpawnerUtils.getDisplay(stack));
        
        return removed;
    }
}
