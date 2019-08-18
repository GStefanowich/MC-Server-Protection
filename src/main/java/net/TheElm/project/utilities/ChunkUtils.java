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

import net.TheElm.project.CoreMod;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.enums.ClaimPermissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ChunkUtils {

    /**
     * Check the database if a user can perform an action within the specified chunk
     */
    public static boolean canPlayerDoInChunk(@NotNull ClaimPermissions perm, @NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( perm, player, ClaimedChunk.convert( player.getEntityWorld(), blockPos ) );
    }
    public static boolean canPlayerDoInChunk(@NotNull ClaimPermissions perm, @NotNull PlayerEntity player, @Nullable ClaimedChunk chunk) {
        if ( chunk == null )
            return false;
        return chunk.canUserDo( player.getUuid(), perm );
    }
    
    /**
     * Check the database if a user can ride entities within the specified chunk
     */
    public static boolean canPlayerRideInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.RIDING, player, blockPos );
    }
    
    /**
     * Check the database if a user can place/break blocks within the specified chunk
     */
    public static boolean canPlayerBreakInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.BLOCKS, player, blockPos );
    }
    
    /**
     * Check the database if a user can loot chests within the specified chunk
     */
    public static boolean canPlayerLootChestsInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.STORAGE, player, blockPos );
    }
    public static boolean canPlayerLootChestsInChunk(PlayerEntity player, ClaimedChunk chunk) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.STORAGE, player, chunk );
    }
    
    /**
     * Check the database if a user can  within the specified chunk
     */
    public static boolean canPlayerLootDropsInChunk(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.PICKUP, player, blockPos );
    }
    
    /**
     * Check the database if a user can interact with doors within the specified chunk
     */
    public static boolean canPlayerToggleDoor(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.DOORS, player, blockPos );
    }
    public static boolean canPlayerToggleDoor(PlayerEntity player, ClaimedChunk chunk) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.DOORS, player, chunk );
    }
    
    /**
     * Check the database if a user can interact with mobs within the specified chunk
     */
    public static boolean canPlayerInteractFriendlies(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.CREATURES, player, blockPos );
    }
    
    /**
     * Check the database if a user can harvest crops within the specified chunk
     */
    public static boolean canPlayerHarvestCrop(PlayerEntity player, BlockPos blockPos) {
        return ChunkUtils.canPlayerDoInChunk( ClaimPermissions.HARVEST, player, blockPos );
    }
    
    /*
     * Get data about where the player is
     */
    @Nullable
    public static UUID getPlayerLocation(@NotNull final ServerPlayerEntity player) {
        return CoreMod.PLAYER_LOCATIONS.get( player );
    }
    public static boolean isPlayerWithinSpawn(@NotNull final ServerPlayerEntity player) {
        return CoreMod.spawnID.equals(ChunkUtils.getPlayerLocation( player ));
    }
    
}
