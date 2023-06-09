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

package net.theelm.sewingmachine.protection.utilities;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.protection.claims.Claimant;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Created on Jun 09 2023 at 6:37 AM.
 * By greg in sewingmachine
 */
public final class ClaimNbtUtils {
    private ClaimNbtUtils() {}
    
    /*
     * Claims
     */
    public static @NotNull NbtCompound readClaimData(@NotNull Claimant.ClaimantType type, @NotNull UUID uuid) {
        File file = Paths.get(
            NbtUtils.levelNameFolder().toAbsolutePath().toString(),
            "sewing-machine",
            type.name().toLowerCase() + "_" + uuid.toString() + ".dat"
        ).toFile();
        
        if (!file.exists())
            return ClaimNbtUtils.emptyTag(type, uuid);
        
        try (FileInputStream stream = new FileInputStream( file )) {
            return NbtIo.readCompressed(stream);
            
        } catch (IOException e) {
            CoreMod.logError( "Error reading " + type.name() + " " + uuid );
            CoreMod.logError( e );
        }
        
        return ClaimNbtUtils.emptyTag(type, uuid);
    }
    public static boolean writeClaimData(@NotNull Claimant claimant) {
        File folder = new File(
            NbtUtils.levelNameFolder().toFile(),
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
        NbtCompound write = ClaimNbtUtils.emptyTag(claimant.getType(), claimant.getId());
        
        // Write the save data
        claimant.writeCustomDataToTag(write);
        
        // Don't write an empty file
        return write.isEmpty() || NbtUtils.writeBackupAndMove(file, write);
    }
    
    public static void assertExists(Claimant.ClaimantType type, UUID uuid) throws NbtNotFoundException {
        if (!ClaimNbtUtils.exists(type, uuid))
            throw new NbtNotFoundException( uuid );
    }
    public static boolean exists(@NotNull Claimant.ClaimantType type, @NotNull UUID uuid) {
        File file = Paths.get(
            NbtUtils.levelNameFolder().toAbsolutePath().toString(),
            "sewing-machine",
            type.name().toLowerCase() + "_" + uuid.toString() + ".dat"
        ).toFile();

        return file.exists();
    }
    private static @NotNull NbtCompound emptyTag(@NotNull Claimant.ClaimantType type, UUID uuid) {
        NbtCompound tag = new NbtCompound();
        tag.putString("type", type.name());
        tag.putUuid("iden", uuid);
        return tag;
    }
    
    /*
     * File Erasure
     */
    
    public static boolean delete(@NotNull Claimant claimant) {
        File file = Paths.get(
            NbtUtils.levelNameFolder().toAbsolutePath().toString(),
            "sewing-machine",
            claimant.getType().name().toLowerCase() + "_" + claimant.getId().toString() + ".dat"
        ).toFile();
        
        if (file.exists())
            return file.delete();
        return false;
    }
}
