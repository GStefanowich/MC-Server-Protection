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

package net.TheElm.project.utilities;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.objects.WorldPos;
import net.TheElm.project.protections.claiming.Claimant;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.datafixer.NbtOps;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class NbtUtils {
    
    private NbtUtils() {}
    
    public static @NotNull File worldFolder() {
        // TODO: Verify the world folder name
        return new File(CoreMod.getFabric().getGameDirectory(),
            ServerCore.get().getName());
    }
    
    /*
     * Player Data
     */
    public static CompoundTag readOfflinePlayerData(UUID uuid) throws NbtNotFoundException {
        return NbtUtils.readOfflinePlayerData(uuid, false);
    }
    public static CompoundTag readOfflinePlayerData(UUID uuid, boolean locking) throws NbtNotFoundException {
        File file = Paths.get(
            worldFolder().getAbsolutePath(),
            "playerdata",
            uuid.toString() + ".dat"
        ).toFile();
        
        if (!file.exists()) {
            CoreMod.logError( "Cannot read offline player data \"" + uuid.toString() + "\"; Path does not exist. Never joined the server?" );
            throw new NbtNotFoundException( uuid );
        }
        
        try (FileInputStream stream = new FileInputStream(file)) {
            // Lock the file
            if (locking) stream.getChannel().tryLock();
            // Read from the file
            return NbtIo.readCompressed(stream);
        } catch (IOException e) {
            CoreMod.logError( e );
        }
        
        throw new NbtNotFoundException( uuid );
    }
    public static boolean writeOfflinePlayerData(UUID uuid, CompoundTag tag) {
        return NbtUtils.writeOfflinePlayerData( uuid, tag, true );
    }
    public static boolean writeOfflinePlayerData(UUID uuid, CompoundTag tag, boolean locking) {
        File file = Paths.get(
            worldFolder().getAbsolutePath(),
            "playerdata",
            uuid.toString() + ".dat"
        ).toFile();
        
        try {
            if ((!file.exists()) && (!file.createNewFile())) {
                CoreMod.logError( "Cannot write player data \"" + uuid.toString() + "\"; Failed to create new player file. Check write permissions?" );
                return false;
            }
            
            try (FileOutputStream stream = new FileOutputStream(file)) {
                // Lock the file
                if (locking) stream.getChannel().lock();
                // Write to the file
                NbtIo.writeCompressed(tag, stream);
                return true;
            }
        } catch (IOException e) {
            CoreMod.logError( e );
        }
        
        return false;
    }
    
    /*
     * Claims
     */
    public static @NotNull CompoundTag readClaimData(Claimant.ClaimantType type, UUID uuid) {
        File file = Paths.get(
            worldFolder().getAbsolutePath(),
            "sewing-machine",
            type.name().toLowerCase() + "_" + uuid.toString() + ".dat"
        ).toFile();
        
        if (!file.exists())
            return emptyTag( type, uuid );
        
        try (FileInputStream stream = new FileInputStream( file )) {
            
            return NbtIo.readCompressed( stream );
            
        } catch (IOException e) {
            CoreMod.logError( "Error reading " + type.name() + " " + uuid );
            CoreMod.logError( e );
        }
        
        return emptyTag( type, uuid );
    }
    public static boolean writeClaimData(@NotNull Claimant claimant) {
        File folder = new File(
            worldFolder(),
            "sewing-machine"
        );
        
        // If the directories don't exist
        if ((!folder.exists()) && (!folder.mkdirs()))
            return false;
        
        File file = new File(
            folder,
            claimant.getType().name().toLowerCase() + "_" + claimant.getId().toString() + ".dat"
        );
        
        try (FileOutputStream stream = new FileOutputStream(file)) {
            // Create an empty tag
            CompoundTag write = emptyTag( claimant.getType(), claimant.getId() );
            
            // Write the save data
            claimant.writeCustomDataToTag( write );
            
            // Don't write an empty file
            if (write.isEmpty())
                return true;
            
            // Save to file
            NbtIo.writeCompressed( write, stream );
            return true;
            
        } catch (IOException e) {
            CoreMod.logError( e );
        }
        
        return false;
    }
    
    public static void assertExists(Claimant.ClaimantType type, UUID uuid) throws NbtNotFoundException {
        if (!NbtUtils.exists(type, uuid))
            throw new NbtNotFoundException( uuid );
    }
    public static boolean exists(Claimant.ClaimantType type, UUID uuid) {
        File file = Paths.get(
            worldFolder().getAbsolutePath(),
            "sewing-machine",
            type.name().toLowerCase() + "_" + uuid.toString() + ".dat"
        ).toFile();
        
        return file.exists();
    }
    private static CompoundTag emptyTag(Claimant.ClaimantType type, UUID uuid) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putUuid("iden", uuid);
        return tag;
    }
    
    /*
     * Simplifications
     */
    public static CompoundTag blockPosToTag(@NotNull BlockPos blockPos) {
        return NbtUtils.blockPosToTag(null, blockPos, null);
    }
    public static CompoundTag blockPosToTag(@NotNull WorldPos blockPointer) {
        return NbtUtils.blockPosToTag(blockPointer, null);
    }
    public static CompoundTag blockPosToTag(@NotNull WorldPos blockPointer, @Nullable Direction direction) {
        return NbtUtils.blockPosToTag(blockPointer.getWorld(), blockPointer.getBlockPos(), direction);
    }
    public static CompoundTag blockPosToTag(@Nullable World world, @Nullable BlockPos blockPos) {
        return NbtUtils.blockPosToTag(world, blockPos, null);
    }
    public static CompoundTag blockPosToTag(@Nullable World world, @Nullable BlockPos blockPos, @Nullable Direction direction) {
        CompoundTag compound = new CompoundTag();
        if (world != null)
            NbtUtils.worldToTag(compound, world);
        if (blockPos != null)
            NbtUtils.blockPosToTag(compound, blockPos);
        if (direction != null)
            NbtUtils.directionToTag(compound, direction);
        return compound;
    }
    private static CompoundTag blockPosToTag(@NotNull CompoundTag compound, @NotNull BlockPos blockPos) {
        compound.putInt("x", blockPos.getX());
        compound.putInt("y", blockPos.getY());
        compound.putInt("z", blockPos.getZ());
        return compound;
    }
    private static CompoundTag worldToTag(@NotNull CompoundTag compound, @NotNull World world) {
        compound.putString("world", NbtUtils.worldToTag(world.getRegistryKey()));
        return compound;
    }
    public static String worldToTag(World world) {
        return NbtUtils.worldToTag(world.getRegistryKey());
    }
    public static String worldToTag(RegistryKey<World> world) {
        return world.getValue().toString();
    }
    public static @NotNull World worldFromTag(@Nullable Tag nbt) {
        return ServerCore.getWorld(NbtUtils.worldRegistryFromTag(nbt));
    }
    public static @NotNull RegistryKey<World> worldRegistryFromTag(@Nullable Tag nbt) {
        if (nbt != null) {
            if (nbt instanceof AbstractNumberTag)
                return LegacyConverter.getWorldFromId(((AbstractNumberTag) nbt).getByte());
            DataResult<RegistryKey<World>> worlds = World.CODEC.parse(NbtOps.INSTANCE, nbt);
            return worlds.resultOrPartial(CoreMod::logError)
                .orElse(World.OVERWORLD);
        }
        return World.OVERWORLD;
    }
    private static CompoundTag directionToTag(@NotNull CompoundTag compound, @NotNull Direction direction) {
        compound.putInt("direction", direction.getId());
        return compound;
    }
    
    public static @Nullable BlockPos tagToBlockPos(@Nullable CompoundTag compound) {
        if (compound != null) {
            if (compound.contains("x", NbtType.NUMBER) && compound.contains("y", NbtType.NUMBER) && compound.contains("z", NbtType.NUMBER))
                return new BlockPos(compound.getDouble("x"), compound.getDouble("y"), compound.getDouble("z"));
        }
        return null;
    }
    public static @Nullable WorldPos tagToWorldPos(@Nullable CompoundTag compound) {
        if ((compound != null) && compound.contains("world", NbtType.STRING)) {
            RegistryKey<World> dimension = RegistryKey.of(Registry.DIMENSION, new Identifier(compound.getString("world")));
            BlockPos blockPos = NbtUtils.tagToBlockPos(compound);
            if ( blockPos != null )
                return new WorldPos(dimension, blockPos);
        }
        return null;
    }
    public static @Nullable Direction tagToDirection(@Nullable CompoundTag compound) {
        if (compound != null && compound.contains("direction", NbtType.NUMBER))
            return Direction.byId(compound.getInt("direction"));
        return null;
    }
    
    /*
     * Lists
     */
    public static @NotNull <T> ListTag toList(Collection<T> collection, Function<T, String> function) {
        ListTag list = new ListTag();
        for (T obj : collection)
            list.add(StringTag.of(function.apply(obj)));
        return list;
    }
    public static @NotNull <T> Collection<T> fromList(ListTag list, Function<String, T> function) {
        List<T> collection = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            T obj = function.apply(list.getString(i));
            if (obj != null)
                collection.add(obj);
        }
        return collection;
    }
    
    /*
     * File Erasure
     */
    public static boolean delete(Claimant claimant) {
        File file = Paths.get(
            worldFolder().getAbsolutePath(),
            "sewing-machine",
            claimant.getType().name().toLowerCase() + "_" + claimant.getId().toString() + ".dat"
        ).toFile();
        
        if (file.exists())
            return file.delete();
        return false;
    }
    
    /*
     * Spawner Lore
     */
    public static CompoundTag getSpawnerDisplay(ListTag entityIdTag) {
        // Create the lore tag
        ListTag loreTag = new ListTag();
        for (Tag entityTag : entityIdTag) {
            // Get the mob entity name
            String mobTag = entityTag.asString();
        
            Optional<EntityType<?>> entityType = EntityType.get( mobTag );
            EntityType<?> entity;
            if ((entity = entityType.orElse( null )) != null) {
                // Create the display of mobs
                JsonObject lore = new JsonObject();
                lore.addProperty("translate", entity.getTranslationKey());
                lore.addProperty("color", (entity.getSpawnGroup().isAnimal() ? Formatting.GOLD : Formatting.RED).getName());
                loreTag.add(StringTag.of(lore.toString()));
            }
        }
        
        // Update the lore tag
        CompoundTag displayTag = new CompoundTag();
        displayTag.put("Lore", loreTag);
        return displayTag;
    }
    
}
