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
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
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
    
    public static @NotNull File levelNameFolder() {
        return Paths.get(ServerCore.get().getSaveProperties().getLevelName()).toFile();
    }
    public static @NotNull File worldSaveFolder(@NotNull RegistryKey<World> world) {
        return DimensionType.getSaveDirectory(world, NbtUtils.levelNameFolder());
    }
    public static @NotNull File worldSaveFile(@NotNull RegistryKey<World> world) {
        return new File(NbtUtils.worldSaveFolder(world), WorldSavePath.LEVEL_DAT.getRelativePath());
    }
    public static @NotNull File playerDataFile(@NotNull UUID uuid) {
        return Paths.get(
            NbtUtils.levelNameFolder().getAbsolutePath(),
            WorldSavePath.PLAYERDATA.getRelativePath(),
            uuid.toString() + ".dat"
        ).toFile();
    }
    
    /*
     * Player Data
     */
    public static CompoundTag readOfflinePlayerData(@NotNull UUID uuid) throws NbtNotFoundException {
        return NbtUtils.readOfflinePlayerData(uuid, false);
    }
    public static CompoundTag readOfflinePlayerData(@NotNull UUID uuid, boolean locking) throws NbtNotFoundException {
        File file = NbtUtils.playerDataFile(uuid);
        
        if (!file.exists()) {
            CoreMod.logError("Cannot read offline player data \"" + uuid.toString() + "\"; Path (" + file.getAbsolutePath() + ") does not exist. Never joined the server?");
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
        return NbtUtils.writeBackupAndMove(
            NbtUtils.playerDataFile(uuid),
            tag
        );
    }
    
    /*
     * Claims
     */
    public static @NotNull CompoundTag readClaimData(@NotNull Claimant.ClaimantType type, @NotNull UUID uuid) {
        File file = Paths.get(
            NbtUtils.levelNameFolder().getAbsolutePath(),
            "sewing-machine",
            type.name().toLowerCase() + "_" + uuid.toString() + ".dat"
        ).toFile();
        
        if (!file.exists())
            return NbtUtils.emptyTag(type, uuid);
        
        try (FileInputStream stream = new FileInputStream( file )) {
            return NbtIo.readCompressed(stream);
            
        } catch (IOException e) {
            CoreMod.logError( "Error reading " + type.name() + " " + uuid );
            CoreMod.logError( e );
        }
        
        return NbtUtils.emptyTag(type, uuid);
    }
    public static boolean writeClaimData(@NotNull Claimant claimant) {
        File folder = new File(
            NbtUtils.levelNameFolder(),
            "sewing-machine"
        );
        
        // If the directories don't exist
        if ((!folder.exists()) && (!folder.mkdirs()))
            return false;
        
        File file = new File(
            folder,
            claimant.getType().name().toLowerCase() + "_" + claimant.getId().toString() + ".dat"
        );
        
        // Create an empty tag
        CompoundTag write = NbtUtils.emptyTag(claimant.getType(), claimant.getId());
        
        // Write the save data
        claimant.writeCustomDataToTag(write);
        
        // Don't write an empty file
        return write.isEmpty() || NbtUtils.writeBackupAndMove(file, write);
    }
    
    /*
     * Additional World DAT
     */
    public static boolean readWorldDat(@NotNull ServerWorld world, @NotNull ServerWorldProperties properties) {
        File file = NbtUtils.worldSaveFile(world.getRegistryKey());
        
        if (!file.exists())
            return false;
        
        try (FileInputStream stream = new FileInputStream(file)) {
            NbtUtils.applyWorldDatFromTag(world, NbtIo.readCompressed(stream), properties);
            return true;
            
        } catch (IOException e) {
            CoreMod.logError( e );
        }
        
        return false;
    }
    private static void applyWorldDatFromTag(@NotNull ServerWorld world, @NotNull CompoundTag tag, @NotNull ServerWorldProperties properties) {
        CompoundTag data = tag.getCompound("Data");
        
        properties.setGameMode(GameMode.byId(data.getInt("GameType")));
        properties.setClearWeatherTime(data.getInt("clearWeatherTime"));
        properties.setRainTime(data.getInt("rainTime"));
        properties.setThunderTime(data.getInt("thunderTime"));
        
        properties.setSpawnX(data.getInt("SpawnX"));
        properties.setSpawnY(data.getInt("SpawnY"));
        properties.setSpawnZ(data.getInt("SpawnZ"));
        properties.setSpawnAngle(data.getFloat("SpawnAngle"));
        
        properties.setTimeOfDay(data.getLong("DayTime"));
        properties.setRaining(data.getBoolean("raining"));
        properties.setThundering(data.getBoolean("thundering"));
        
        WorldBorder border = world.getWorldBorder();
        border.setCenter(
            data.getDouble("BorderCenterX"),
            data.getDouble("BorderCenterZ")
        );
        border.setSize(data.getDouble("BorderSize"));
        border.interpolateSize(
            data.getDouble("BorderSize"),
            data.getDouble("BorderSizeLerpTarget"),
            data.getLong("BorderSizeLerpTime")
        );
        border.setBuffer(data.getDouble("BorderSafeZone"));
        border.setDamagePerBlock(data.getDouble("BorderDamagePerBlock"));
        border.setWarningBlocks(data.getInt("BorderWarningBlocks"));
        border.setWarningTime(data.getInt("BorderWarningTime"));
    }
    public static boolean writeWorldDat(@NotNull ServerWorld world, @NotNull WorldProperties properties) {
        return NbtUtils.writeBackupAndMove(
            NbtUtils.worldSaveFile(world.getRegistryKey()),
            NbtUtils.writeWorldDatToTag(world, properties)
        );
    }
    private static @NotNull CompoundTag writeWorldDatToTag(@NotNull ServerWorld world, @NotNull WorldProperties properties) {
        CompoundTag data = new CompoundTag();
        
        if (properties instanceof ServerWorldProperties) {
            ServerWorldProperties serverProperties = (ServerWorldProperties)properties;
            
            data.putInt("GameType", serverProperties.getGameMode().getId());
            data.putInt("clearWeatherTime", serverProperties.getClearWeatherTime());
            data.putInt("rainTime", serverProperties.getRainTime());
            data.putInt("thunderTime", serverProperties.getThunderTime());
        }
        
        data.putInt("SpawnX", properties.getSpawnX());
        data.putInt("SpawnY", properties.getSpawnY());
        data.putInt("SpawnZ", properties.getSpawnZ());
        data.putFloat("SpawnAngle", properties.getSpawnAngle());
        
        data.putLong("DayTime", properties.getTimeOfDay());
        data.putLong("LastPlayed", Util.getEpochTimeMs());
        data.putBoolean("raining", properties.isRaining());
        data.putBoolean("thundering", properties.isThundering());
        
        WorldBorder border = world.getWorldBorder();
        border.write().toTag(data);
        
        data.put("GameRules", properties.getGameRules().toNbt());
        
        CompoundTag out = new CompoundTag();
        out.put("Data", data);
        return out;
    }
    
    private static boolean writeBackupAndMove(@NotNull File file, @NotNull CompoundTag tag) {
        String fileName = file.getName();
        int indexOf = fileName.indexOf('.');
        if (indexOf < 0)
            return false;
        try {
            String path = file.getAbsolutePath();
            File directory = new File(path.substring(0, path.length() - fileName.length()));
    
            String filePrefix = fileName.substring(0, indexOf);
            String fileType = fileName.substring(indexOf);
            
            File tmp = File.createTempFile(fileType, filePrefix, directory);
            NbtIo.writeCompressed(tag, tmp);
            File backup = new File(directory, fileName + "_old");
            File newF = new File(directory, fileName);
            
            Util.backupAndReplace(newF, tmp, backup);
            return true;
        } catch (IOException e) {
            CoreMod.logError(e);
        }
        
        return false;
    }
    
    public static void assertExists(Claimant.ClaimantType type, UUID uuid) throws NbtNotFoundException {
        if (!NbtUtils.exists(type, uuid))
            throw new NbtNotFoundException( uuid );
    }
    public static boolean exists(@NotNull Claimant.ClaimantType type, @NotNull UUID uuid) {
        File file = Paths.get(
            NbtUtils.levelNameFolder().getAbsolutePath(),
            "sewing-machine",
            type.name().toLowerCase() + "_" + uuid.toString() + ".dat"
        ).toFile();
        
        return file.exists();
    }
    private static @NotNull CompoundTag emptyTag(@NotNull Claimant.ClaimantType type, UUID uuid) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putUuid("iden", uuid);
        return tag;
    }
    
    /*
     * Simplifications
     */
    public static @NotNull CompoundTag blockPosToTag(@NotNull BlockPos blockPos) {
        return NbtUtils.blockPosToTag(null, blockPos, null);
    }
    public static @NotNull CompoundTag blockPosToTag(@NotNull WorldPos blockPointer) {
        return NbtUtils.blockPosToTag(blockPointer, null);
    }
    public static @NotNull CompoundTag blockPosToTag(@NotNull WorldPos blockPointer, @Nullable Direction direction) {
        return NbtUtils.blockPosToTag(blockPointer.getWorld(), blockPointer.getBlockPos(), direction);
    }
    public static @NotNull CompoundTag blockPosToTag(@Nullable World world, @Nullable BlockPos blockPos) {
        return NbtUtils.blockPosToTag(world, blockPos, null);
    }
    public static @NotNull CompoundTag blockPosToTag(@Nullable World world, @Nullable BlockPos blockPos, @Nullable Direction direction) {
        CompoundTag compound = new CompoundTag();
        if (world != null)
            NbtUtils.worldToTag(compound, world);
        if (blockPos != null)
            NbtUtils.blockPosToTag(compound, blockPos);
        if (direction != null)
            NbtUtils.directionToTag(compound, direction);
        return compound;
    }
    private static @NotNull CompoundTag blockPosToTag(@NotNull CompoundTag compound, @NotNull BlockPos blockPos) {
        compound.putInt("x", blockPos.getX());
        compound.putInt("y", blockPos.getY());
        compound.putInt("z", blockPos.getZ());
        return compound;
    }
    private static @NotNull CompoundTag worldToTag(@NotNull CompoundTag compound, @NotNull World world) {
        compound.putString("world", NbtUtils.worldToTag(world));
        return compound;
    }
    public static String worldToTag(@NotNull World world) {
        return NbtUtils.worldToTag(world.getRegistryKey());
    }
    public static String worldToTag(@NotNull RegistryKey<World> world) {
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
    private static @NotNull CompoundTag directionToTag(@NotNull CompoundTag compound, @NotNull Direction direction) {
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
    
    public static boolean hasUUID(@Nullable CompoundTag tag, @NotNull String key) {
        if (tag == null)
            return false;
        return (tag.contains(key + "Most", NbtType.LONG) && tag.contains(key + "Least", NbtType.LONG)) || tag.contains(key, NbtType.INT_ARRAY);
    }
    public static @Nullable UUID getUUID(@Nullable CompoundTag tag, @NotNull String key) {
        if (tag != null) {
            // Check for old 64-bit ints
            final String kMost = key + "Most";
            final String kLeast = key + "Least";
            if (tag.contains(kMost, NbtType.LONG) && tag.contains(kLeast, NbtType.LONG)) {
                long most = tag.getLong(kMost);
                long least = tag.getLong(kLeast);
                return new UUID(most, least);
            }
            // Get 32-bit int array
            if (tag.contains(key, NbtType.INT_ARRAY))
                return tag.getUuid(key);
        }
        return null;
    }
    
    /*
     * File Erasure
     */
    public static boolean delete(Claimant claimant) {
        File file = Paths.get(
            NbtUtils.levelNameFolder().getAbsolutePath(),
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
