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

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.theelm.sewingmachine.CoreMod;
import net.theelm.sewingmachine.MySQL.MySQLStatement;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.ClaimPermissions;
import net.theelm.sewingmachine.enums.ClaimRanks;
import net.theelm.sewingmachine.enums.ClaimSettings;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.interfaces.MoneyHolder;
import net.theelm.sewingmachine.protections.claiming.ClaimantPlayer;
import net.theelm.sewingmachine.protections.claiming.ClaimantTown;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LegacyConverter {
    
    public static @NotNull RegistryKey<World> getWorldFromId(int id) {
        return LegacyConverter.getWorldFromId(NbtInt.of(id));
    }
    public static @NotNull RegistryKey<World> getWorldFromId(NbtInt id) {
        DataResult<RegistryKey<World>> worlds = DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, id));
        return worlds.resultOrPartial(CoreMod::logError).orElse(World.OVERWORLD);
    }
    
}
