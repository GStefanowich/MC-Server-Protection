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
import net.TheElm.project.CoreMod;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.protections.claiming.Claimant;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

public final class NbtUtils {
    
    private NbtUtils() {}
    
    @NotNull
    public static File worldFolder() {
        return new File(CoreMod.getFabric().getGameDirectory(),
            CoreMod.getServer().getLevelName());
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
            FileLock lock = ( locking ? stream.getChannel().tryLock() : null );
            try {
                // Read from the file
                return NbtIo.readCompressed(stream);
            } finally {
                // If locking is enabled, release the lock
                if (locking) lock.release();
            }
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
        
        if (!file.exists()) {
            CoreMod.logError( "Cannot read offline player data \"" + uuid.toString() + "\"; Path does not exist. Never joined the server?" );
            return false;
        }
        
        try (FileOutputStream stream = new FileOutputStream(file)) {
            // Lock the file
            FileLock lock = ( locking ? stream.getChannel().tryLock() : null );
            try {
                // Write to the file
                NbtIo.writeCompressed(tag, stream);
                return true;
            } finally {
                // If locking is enabled, release the lock
                if (locking) lock.release();
            }
        } catch (IOException e) {
            CoreMod.logError( e );
        }
        
        return false;
    }
    
    /*
     * Claims
     */
    @NotNull
    public static CompoundTag readClaimData(Claimant.ClaimantType type, UUID uuid) {
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
                lore.addProperty("color", (entity.getCategory().isAnimal() ? Formatting.GOLD : Formatting.RED).getName());
                loreTag.add(new StringTag(lore.toString()));
            }
        }
        
        // Update the lore tag
        CompoundTag displayTag = new CompoundTag();
        displayTag.put("Lore", loreTag);
        return displayTag;
    }
}
