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

package net.theelm.sewingmachine.utilities.nbt;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.objects.WorldPos;
import net.theelm.sewingmachine.utilities.LegacyConverter;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import net.theelm.sewingmachine.utilities.mod.Sew;
import net.theelm.sewingmachine.utilities.mod.SewServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class NbtUtils {
    private NbtUtils() {}
    
    public static @NotNull Path levelNameFolder() {
        return NbtUtils.levelNameFolder(SewServer.get());
    }
    public static @NotNull Path levelNameFolder(@NotNull MinecraftServer server) {
        String level = server.getSaveProperties().getLevelName();
        if (Sew.isClient())
            return Paths.get("saves", level);
        return Paths.get(level);
    }
    public static @NotNull Path worldSaveFolder(@NotNull MinecraftServer server, @NotNull RegistryKey<World> world) {
        return DimensionType.getSaveDirectory(world, NbtUtils.levelNameFolder(server));
    }
    public static @NotNull File worldSaveFile(@NotNull MinecraftServer server, @NotNull RegistryKey<World> world) {
        return new File(NbtUtils.worldSaveFolder(server, world).toFile(), WorldSavePath.LEVEL_DAT.getRelativePath());
    }
    public static @NotNull File playerDataFile(@NotNull UUID uuid) {
        return Paths.get(
            NbtUtils.levelNameFolder().toAbsolutePath().toString(),
            WorldSavePath.PLAYERDATA.getRelativePath(),
            uuid.toString() + ".dat"
        ).toFile();
    }
    
    /*
     * Player Data
     */
    public static NbtCompound readOfflinePlayerData(@NotNull UUID uuid) throws NbtNotFoundException {
        return NbtUtils.readOfflinePlayerData(uuid, false);
    }
    public static NbtCompound readOfflinePlayerData(@NotNull UUID uuid, boolean locking) throws NbtNotFoundException {
        File file = NbtUtils.playerDataFile(uuid);
        
        if (!file.exists()) {
            CoreMod.logError("Cannot read offline player data \"" + uuid + "\"; Path (" + file.getAbsolutePath() + ") does not exist. Never joined the server?");
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
    public static boolean writeOfflinePlayerData(UUID uuid, NbtCompound tag) {
        return NbtUtils.writeBackupAndMove(
            NbtUtils.playerDataFile(uuid),
            tag
        );
    }
    
    /*
     * Additional World DAT
     */
    public static boolean readWorldDat(@NotNull ServerWorld world, @NotNull ServerWorldProperties properties) {
        File file = NbtUtils.worldSaveFile(world.getServer(), world.getRegistryKey());
        
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
    private static void applyWorldDatFromTag(@NotNull ServerWorld world, @NotNull NbtCompound tag, @NotNull ServerWorldProperties properties) {
        NbtCompound data = tag.getCompound("Data");
        
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
        border.setSafeZone(data.getDouble("BorderSafeZone"));
        border.setDamagePerBlock(data.getDouble("BorderDamagePerBlock"));
        border.setWarningBlocks(data.getInt("BorderWarningBlocks"));
        border.setWarningTime(data.getInt("BorderWarningTime"));
    }
    public static boolean writeWorldDat(@NotNull ServerWorld world, @NotNull WorldProperties properties) {
        return NbtUtils.writeBackupAndMove(
            NbtUtils.worldSaveFile(world.getServer(), world.getRegistryKey()),
            NbtUtils.writeWorldDatToTag(world, properties)
        );
    }
    private static @NotNull NbtCompound writeWorldDatToTag(@NotNull ServerWorld world, @NotNull WorldProperties properties) {
        NbtCompound data = new NbtCompound();
        
        if (properties instanceof ServerWorldProperties serverProperties) {
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
        border.write().writeNbt(data);
        
        data.put("GameRules", properties.getGameRules().toNbt());
        
        NbtCompound out = new NbtCompound();
        out.put("Data", data);
        return out;
    }
    
    public static boolean writeBackupAndMove(@NotNull File file, @NotNull NbtCompound tag) {
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
    
    /*
     * Simplifications
     */
    public static @NotNull NbtCompound blockPosToTag(@NotNull BlockPos blockPos) {
        return NbtUtils.blockPosToTag(null, blockPos, null);
    }
    public static @NotNull NbtCompound blockPosToTag(@NotNull WorldPos blockPointer) {
        return NbtUtils.blockPosToTag(blockPointer, null);
    }
    public static @NotNull NbtCompound blockPosToTag(@NotNull WorldPos blockPointer, @Nullable Direction direction) {
        return NbtUtils.blockPosToTag(blockPointer.getWorld(), blockPointer.getPos(), direction);
    }
    public static @NotNull NbtCompound blockPosToTag(@Nullable World world, @Nullable BlockPos blockPos) {
        return NbtUtils.blockPosToTag(world, blockPos, null);
    }
    public static @NotNull NbtCompound blockPosToTag(@Nullable World world, @Nullable BlockPos blockPos, @Nullable Direction direction) {
        NbtCompound compound = new NbtCompound();
        if (world != null)
            NbtUtils.worldToTag(compound, world);
        if (blockPos != null)
            NbtUtils.blockPosToTag(compound, blockPos);
        if (direction != null)
            NbtUtils.directionToTag(compound, direction);
        return compound;
    }
    private static @NotNull NbtCompound blockPosToTag(@NotNull NbtCompound compound, @NotNull BlockPos blockPos) {
        compound.putInt("x", blockPos.getX());
        compound.putInt("y", blockPos.getY());
        compound.putInt("z", blockPos.getZ());
        return compound;
    }
    private static @NotNull NbtCompound worldToTag(@NotNull NbtCompound compound, @NotNull World world) {
        compound.putString("world", NbtUtils.worldToTag(world));
        return compound;
    }
    public static String worldToTag(@NotNull World world) {
        return NbtUtils.worldToTag(world.getRegistryKey());
    }
    public static String worldToTag(@NotNull RegistryKey<World> world) {
        return world.getValue().toString();
    }
    public static @NotNull RegistryKey<World> worldRegistryFromTag(@Nullable NbtElement nbt) {
        if (nbt != null) {
            if (nbt instanceof AbstractNbtNumber abstractNbtNumber)
                return LegacyConverter.getWorldFromId(abstractNbtNumber.byteValue());
            DataResult<RegistryKey<World>> worlds = World.CODEC.parse(NbtOps.INSTANCE, nbt);
            return worlds.resultOrPartial(CoreMod::logError)
                .orElse(World.OVERWORLD);
        }
        return World.OVERWORLD;
    }
    private static @NotNull NbtCompound directionToTag(@NotNull NbtCompound compound, @NotNull Direction direction) {
        compound.putInt("direction", direction.getId());
        return compound;
    }
    
    public static @Nullable BlockPos tagToBlockPos(@Nullable NbtCompound compound) {
        if (compound != null) {
            if (compound.contains("x", NbtElement.NUMBER_TYPE) && compound.contains("y", NbtElement.NUMBER_TYPE) && compound.contains("z", NbtElement.NUMBER_TYPE))
                return BlockPos.ofFloored(compound.getDouble("x"), compound.getDouble("y"), compound.getDouble("z"));
        }
        return null;
    }
    public static @Nullable WorldPos tagToWorldPos(@Nullable NbtCompound compound) {
        if ((compound != null) && compound.contains("world", NbtElement.STRING_TYPE)) {
            RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, new Identifier(compound.getString("world")));
            BlockPos blockPos = NbtUtils.tagToBlockPos(compound);
            if ( blockPos != null )
                return new WorldPos(dimension, blockPos);
        }
        return null;
    }
    public static @Nullable Direction tagToDirection(@Nullable NbtCompound compound) {
        if (compound != null && compound.contains("direction", NbtElement.NUMBER_TYPE))
            return Direction.byId(compound.getInt("direction"));
        return null;
    }
    
    /*
     * Lists
     */
    public static @NotNull <T> NbtList toList(@NotNull Collection<T> collection, @NotNull Function<T, String> function) {
        NbtList list = new NbtList();
        for (T obj : collection)
            list.add(NbtString.of(function.apply(obj)));
        return list;
    }
    public static @NotNull <T> Collection<T> fromList(@NotNull NbtList list, @NotNull Function<String, T> function) {
        List<T> collection = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            T obj = function.apply(list.getString(i));
            if (obj != null)
                collection.add(obj);
        }
        return collection;
    }
    
    public static boolean hasUUID(@Nullable NbtCompound tag, @NotNull String key) {
        if (tag == null)
            return false;
        return (tag.contains(key + "Most", NbtElement.LONG_TYPE) && tag.contains(key + "Least", NbtElement.LONG_TYPE)) || tag.contains(key, NbtElement.INT_ARRAY_TYPE);
    }
    public static @Nullable UUID getUUID(@Nullable NbtCompound tag, @NotNull String key) {
        if (tag != null) {
            // Check for old 64-bit ints
            final String kMost = key + "Most";
            final String kLeast = key + "Least";
            if (tag.contains(kMost, NbtElement.LONG_TYPE) && tag.contains(kLeast, NbtElement.LONG_TYPE)) {
                long most = tag.getLong(kMost);
                long least = tag.getLong(kLeast);
                return new UUID(most, least);
            }
            // Get 32-bit int array
            if (tag.contains(key, NbtElement.INT_ARRAY_TYPE))
                return tag.getUuid(key);
        }
        return null;
    }
    public static @NotNull <T> NbtReader<T> tryGet(@Nullable NbtCompound tag, @NotNull NbtGet<T> nbtType, @NotNull String key, @NotNull Consumer<T> consumer) {
        return new NbtReaderContext<>(tag, nbtType, consumer)
            .run(key);
    }
    
    public static void withSpawnerEntities(final @NotNull NbtCompound base, final @NotNull Consumer<NbtString> consumer) {
        NbtList list = base.getList("SpawnPotentials", NbtElement.COMPOUND_TYPE);
        
        // Save to the item (The mob)
        if (list.isEmpty())
            NbtUtils.withInnerSpawnerData(base.getCompound("SpawnData"), consumer);
        else for (NbtElement tag : list)
            NbtUtils.withInnerSpawnerData(((NbtCompound) tag), consumer);
    }
    private static void withInnerSpawnerData(final @NotNull NbtCompound base, final @NotNull Consumer<NbtString> consumer) {
        NbtCompound data = base.contains("data", NbtElement.COMPOUND_TYPE) ? base.getCompound("data") : base;
        
        // Get entity details
        if (!data.contains("entity", NbtElement.COMPOUND_TYPE))
            return;
        NbtCompound entity = data.getCompound("entity");
        
        // Get entity Id
        if (!entity.contains("id", NbtElement.STRING_TYPE))
            return;
        String entityId = entity.getString("id");
        if (entityId != null && !entityId.isEmpty())
            consumer.accept(NbtString.of(entityId));
    }
    
    /*
     * Spawner Lore
     */
    public static @NotNull NbtCompound getSpawnerDisplay(@NotNull NbtList entityIdTag) {
        // Create the lore tag
        NbtList loreTag = new NbtList();
        for (NbtElement entityTag : entityIdTag) {
            // Get the mob entity name
            String mobTag = entityTag.asString();
            
            Optional<EntityType<?>> entityType = EntityType.get(mobTag);
            EntityType<?> entity;
            if ((entity = entityType.orElse( null )) != null) {
                // Create the display of mobs
                JsonObject lore = new JsonObject();
                lore.addProperty("translate", entity.getTranslationKey());
                lore.addProperty("color", (entity.getSpawnGroup().isPeaceful() ? Formatting.GOLD : Formatting.RED).getName());
                loreTag.add(NbtString.of(lore.toString()));
            }
        }
        
        // Update the lore tag
        NbtCompound displayTag = new NbtCompound();
        displayTag.put("Lore", loreTag);
        return displayTag;
    }
    
    /*
     * Enchantment Tags
     */
    public static @NotNull NbtList enchantsToTag(@NotNull Map<Enchantment, Integer> enchantments) {
        NbtList list = new NbtList();
        enchantments.entrySet().stream()
            .map(entry -> {
                NbtCompound tag = new NbtCompound();
                tag.putString("id", String.valueOf(Registries.ENCHANTMENT.getId(entry.getKey())));
                tag.putInt("lvl", entry.getValue());
                return tag;
            }).forEach(list::add);
        
        return list;
    }
    public static @NotNull Map<Enchantment, Integer> enchantsFromTag(@NotNull NbtList tag) {
        return EnchantmentHelper.fromNbt(tag);
    }
    public static boolean enchantsEquals(@NotNull final Map<Enchantment, Integer> first, @NotNull final Map<Enchantment, Integer> second) {
        if (first.isEmpty() && second.isEmpty())
            return true;
        if (first.size() != second.size())
            return false;
        return first.entrySet().stream()
            .allMatch(e -> Objects.equals(e.getValue(), second.get(e.getKey())));
    }
}
