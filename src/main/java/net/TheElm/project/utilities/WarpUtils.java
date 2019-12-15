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
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class WarpUtils {
    
    private static final Set<UUID> warpPlayers = Collections.synchronizedSet(new HashSet<>());
    
    private BlockPos createWarpAt;
    private Pair<BlockPos, BlockPos> region;
    
    public WarpUtils(final ServerPlayerEntity player, final BlockPos pos) {
        warpPlayers.add( player.getUuid() );
        this.updateWarpPos( pos );
    }
    
    private void updateWarpPos(BlockPos warpPos) {
        this.createWarpAt = warpPos;
        this.region = new Pair<>(
            new BlockPos( this.createWarpAt.getX() - 4, this.createWarpAt.getY() - 4, this.createWarpAt.getZ() - 3 ),
            new BlockPos( this.createWarpAt.getX() + 4, this.createWarpAt.getY() + 4, this.createWarpAt.getZ() + 6 )
        );
    }
    
    public BlockPos getWarpPositionIn(final World world) {
        int x = this.createWarpAt.getX();
        int z = this.createWarpAt.getZ();
        do {
            CoreMod.logInfo( "Finding a new warp position!" );
            this.updateWarpPos( new BlockPos( getRandom( x ), 256, getRandom( z ) ) );
        } while ((this.createWarpAt = this.isValid( world, this.createWarpAt,50,true ) ) == null);
        
        return this.createWarpAt;
    }
    public BlockPos getSafeTeleportPos(final World world) {
        BlockPos tpPos;
        
        int count = 1;
        int maxHeight = ( world.dimension.hasVisibleSky() ? 256 : this.createWarpAt.getY() + 10 );
        boolean negX = false;
        boolean negZ = false;
        
        do {
            int x = this.createWarpAt.getX() + ( 2 * count * ( negX ? -1 : 1 ));
            int z = this.createWarpAt.getZ() + ( 2 * count * ( negZ ? -1 : 1 ));
            
            tpPos = new BlockPos( x, maxHeight, z );
            
            if (!negX) {
                negX = true;
            } else {
                if (!negZ) {
                    negZ = true;
                } else {
                    negX = false;
                    negZ = false;
                    ++count;
                }
            }
            
        } while (( tpPos = this.isValid( world, tpPos, this.createWarpAt.getY() - 5,false ) ) == null );
        
        return tpPos.up( 2 );
    }
    
    private static int getRandom( int position ) {
        int random = ThreadLocalRandom.current().nextInt(
            position - SewingMachineConfig.INSTANCE.WARP_MAX_DISTANCE.get(),
            position + SewingMachineConfig.INSTANCE.WARP_MAX_DISTANCE.get()
        );
        return ( 16 * Math.round(random >> 4) ) + 8;
    }
    
    private BlockPos isValid(final World world, final BlockPos startingPos, final int minY, final boolean mustBeUnowned) {
        // If the chunks are claimed.
        if (mustBeUnowned && IClaimedChunk.isOwnedAround( world, this.createWarpAt, 5 ))
            return null;
        
        BlockPos pos = startingPos;
        BlockState blockState;
        do {
            if ( pos.getY() <= minY )
                return null;
            
            pos = pos.down();
            blockState = world.getBlockState( pos );
        } while ( blockState.isAir() || blockState.getMaterial().isReplaceable() || ( mustBeUnowned && ( blockState.getMaterial() == Material.SNOW || blockState.getMaterial() == Material.PLANT )));
        
        Material material = blockState.getMaterial();
        
        // Don't set up a warp in a liquid (Water/Lava) or in Fire or on top of Trees
        return (!material.isLiquid())
            && (!( blockState.getBlock() instanceof MushroomBlock ))
            && ( material != Material.FIRE )
            && ( material != Material.LEAVES )
            && ( material != Material.ICE )
            && ( material != Material.PACKED_ICE )
            && ( mustBeUnowned || world.getBlockState( pos.up() ).isAir() ) ? pos : null;
    }
    
    public boolean build(final ServerPlayerEntity player, final World world) {
        return this.build(player, world, true);
    }
    public boolean build(final ServerPlayerEntity player, final World world, final boolean dropBlocks) {
        // Get the area of blocks to claim
        if (!ChunkUtils.canPlayerClaimSlices( player.getServerWorld(), this.region.getLeft(), this.region.getRight() ))
            return false;
        
        // Claim the defined slices in the name of Spawn
        ChunkUtils.claimSlices( player.getServerWorld(), CoreMod.spawnID, this.region.getLeft(), this.region.getRight() );
        
        // Create the structure
        StructureBuilderUtils structure = new StructureBuilderUtils( world, "waystone" );
        
        final BlockState air = Blocks.AIR.getDefaultState();
        
        // Light-source blocks
        final BlockState light = Blocks.SEA_LANTERN.getDefaultState();
        BlockPos[] lightBlocks = new BlockPos[]{
            new BlockPos( this.createWarpAt.getX() + 1, this.createWarpAt.getY(), this.createWarpAt.getZ() + 1 ),
            new BlockPos( this.createWarpAt.getX() + 1, this.createWarpAt.getY(), this.createWarpAt.getZ() - 1 ),
            new BlockPos( this.createWarpAt.getX() - 1, this.createWarpAt.getY(), this.createWarpAt.getZ() + 1 ),
            new BlockPos( this.createWarpAt.getX() - 1, this.createWarpAt.getY(), this.createWarpAt.getZ() - 1 )
        };
        for ( BlockPos blockPos : lightBlocks ) {
            structure.addBlock( blockPos.up( 1 ), air );
            structure.addBlock( blockPos.up( 2 ), air );
            structure.addBlock( blockPos, light );
        }
        
        // Andesite blocks
        final BlockState andesite = Blocks.ANDESITE.getDefaultState();
        BlockPos[] andesiteBlocks = new BlockPos[]{
            this.createWarpAt.offset(Direction.NORTH),
            this.createWarpAt.offset(Direction.SOUTH),
            this.createWarpAt.offset(Direction.EAST),
            this.createWarpAt.offset(Direction.WEST)
        };
        for ( BlockPos blockPos : andesiteBlocks ) {
            structure.addBlock( blockPos.up( 1 ), air );
            structure.addBlock( blockPos.up( 2 ), air );
            structure.addBlock( blockPos, andesite );
        }
        
        // Diorite blocks
        final BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockPos[] dioriteBlocks = new BlockPos[]{
            this.createWarpAt
        };
        for ( BlockPos blockPos : dioriteBlocks ) {
            structure.addBlock( blockPos.up( 1 ), air );
            structure.addBlock( blockPos.up( 2 ), air );
            structure.addBlock( blockPos, diorite );
        }
        
        // Bedrock blocks
        final BlockState bedrock = Blocks.BEDROCK.getDefaultState();
        BlockPos[] bedrockBlocks = new BlockPos[]{
            this.createWarpAt.down( 2 ),
            new BlockPos( this.createWarpAt.getX() + 1, this.createWarpAt.getY() - 1, this.createWarpAt.getZ() ),
            new BlockPos( this.createWarpAt.getX(), this.createWarpAt.getY() - 1, this.createWarpAt.getZ() + 1 ),
            new BlockPos( this.createWarpAt.getX() - 1, this.createWarpAt.getY() - 1, this.createWarpAt.getZ() ),
            new BlockPos( this.createWarpAt.getX(), this.createWarpAt.getY() - 1, this.createWarpAt.getZ() - 1 )
        };
        for ( BlockPos blockPos : bedrockBlocks ) {
            structure.addBlock( blockPos, bedrock );
        }
        
        // Command blocks
        final BlockState cmdBlock = Blocks.COMMAND_BLOCK.getDefaultState();
        BlockPos[] commandBlocks = new BlockPos[]{
            this.createWarpAt.down()
        };
        for ( BlockPos blockPos : commandBlocks ) {
            // Place the command block
            CommandBlockBlockEntity cmdBlockEntity = BlockEntityType.COMMAND_BLOCK.instantiate();
            if (cmdBlockEntity != null) {
                cmdBlockEntity.getCommandExecutor().setCommand("spawn @a[x=" + this.createWarpAt.getX() + ",z=" + this.createWarpAt.getZ() + ",y=" + this.createWarpAt.getY() + ",distance=0..2]");
                
                // Place the block
                structure.addBlock(this.createWarpAt.down(), cmdBlock);
                structure.addEntity(this.createWarpAt.down(), cmdBlockEntity);
            }
        }
        
        // Pressure plate
        final BlockState plate = Blocks.STONE_PRESSURE_PLATE.getDefaultState();
        BlockPos[] pressurePlates = new BlockPos[]{
            this.createWarpAt.up()
        };
        for ( BlockPos blockPos : pressurePlates ) {
            structure.addBlock( blockPos, plate );
        }
        
        try {
            structure.destroy( dropBlocks );
            structure.build();
            structure.particlesSounds(ParticleTypes.HAPPY_VILLAGER, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 1.0f, 1.0f, 12,
                new BlockPos( this.createWarpAt.getX() - 1, this.createWarpAt.getY() + 3, this.createWarpAt.getZ() - 1 ),
                new BlockPos( this.createWarpAt.getX() + 1, this.createWarpAt.getY() + 2, this.createWarpAt.getZ() + 1 ),
                new BlockPos( this.createWarpAt.getX() + 1, this.createWarpAt.getY() + 2, this.createWarpAt.getZ() - 1 ),
                new BlockPos( this.createWarpAt.getX() - 1, this.createWarpAt.getY() + 1, this.createWarpAt.getZ() + 1 )
            );
        } catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
        
        return true;
    }
    
    public void save(final World world, final BlockPos warpPos, final ServerPlayerEntity player) {
        // Remove the player from the build list
        warpPlayers.remove( player.getUuid() );
        
        // Set the players warp position to save
        ((PlayerData) player).setWarpPos( warpPos );
        ((PlayerData) player).setWarpDimension( world );
        
        // Update the players Teleporting commands
        MinecraftServer server = player.getServer();
        if (server != null)
            server.getPlayerManager().sendCommandTree( player );
    }
    
    /*
     * Static checks
     */
    @Nullable
    public static Warp getWarp(final UUID uuid) {
        MinecraftServer server = ServerCore.get();
        
        // Read from the player
        ServerPlayerEntity player;
        if ((player = server.getPlayerManager().getPlayer( uuid )) != null)
            return WarpUtils.getWarp( player );
        
        Warp warp = null;
        try {
            // Read from the NBT file
            CompoundTag playerNBT;
            if ((playerNBT = NbtUtils.readOfflinePlayerData(uuid)) == null)
                return null;
            
            // Read the player warp location after restarting
            if (playerNBT.contains("playerWarpX") && playerNBT.contains("playerWarpY") && playerNBT.contains("playerWarpZ")) {
                int warpDimension = (playerNBT.contains("playerWarpD") ? playerNBT.getInt("playerWarpD") : 0);
                warp = new Warp(
                    server.getWorld(DimensionType.byRawId(warpDimension)),
                    new BlockPos(
                        playerNBT.getInt("playerWarpX"),
                        playerNBT.getInt("playerWarpY"),
                        playerNBT.getInt("playerWarpZ")
                    )
                );
            }
        } catch (NbtNotFoundException ignored) {}
        
        return warp;
    }
    @Nullable
    public static Warp getWarp(final ServerPlayerEntity player) {
        if ( ((PlayerData) player).getWarpPos() == null )
            return null;
        return new Warp(((PlayerData) player).getWarpWorld(), ((PlayerData) player).getWarpPos());
    }
    public static boolean hasWarp(final ServerCommandSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity)
            return WarpUtils.hasWarp((ServerPlayerEntity) entity);
        return false;
    }
    public static boolean hasWarp(final UUID uuid) {
        return WarpUtils.getWarp( uuid ) != null;
    }
    public static boolean hasWarp(final ServerPlayerEntity player) {
        return WarpUtils.getWarp( player ) != null;
    }
    public static boolean isPlayerCreating(final ServerPlayerEntity player) {
        return warpPlayers.contains( player.getUuid() );
    }
    public static void teleportPlayer(@NotNull ServerPlayerEntity player) {
        Warp warp = WarpUtils.getWarp( player );
        if (warp != null) WarpUtils.teleportPlayer( warp, player );
    }
    public static void teleportPlayer(@NotNull final Warp warp, @NotNull final ServerPlayerEntity player) {
        WarpUtils.teleportPlayer( warp.world, player, warp.warpPos );
    }
    public static void teleportPlayer(@NotNull final World world, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos tpPos) {
        // Get the chunks
        ChunkPos chunkPos = new ChunkPos( tpPos );
        
        // Load the chunk getting teleported to
        if (!world.isChunkLoaded( chunkPos.x, chunkPos.z ))
            world.getChunk( chunkPos.x, chunkPos.z, ChunkStatus.FULL, true );
        
        // Spawn the particles
        WarpUtils.teleportPoof( world, player );
        
        // Set the teleport ticket
        ((ServerChunkManager)world.getChunkManager()).addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getEntityId());
        
        // Unbind player from entities
        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true);
        }
        
        // Move the player
        if (world == player.world) {
            player.networkHandler.requestTeleport(tpPos.getX() + 0.5D, tpPos.getY(), tpPos.getZ() + 0.5D, player.yaw, player.pitch);
            player.networkHandler.syncWithPlayerPosition();
        } else if (world instanceof ServerWorld) {
            player.teleport((ServerWorld) world, tpPos.getX() + 0.5D, tpPos.getY(), tpPos.getZ() + 0.5D, player.yaw, player.pitch);
        }
    }
    private static void teleportPoof(final World world, final ServerPlayerEntity player) {
        BlockPos blockPos = player.getBlockPos();
        if (world instanceof ServerWorld) {
            world.playSound( null, blockPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 1.0f, 1.0f );
            ((ServerWorld) world).spawnParticles(ParticleTypes.POOF,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                50,
                0.0D,
                0.0D,
                0.0D,
                0.05D
            );
        }
    }
    public static BlockPos getWorldSpawn(@NotNull final World world) {
        LevelProperties properties = world.getLevelProperties();
        return new BlockPos( properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ() );
    }
    
    public static class Warp {
        public final BlockPos warpPos;
        public final World world;
        
        private Warp(World world, BlockPos blockPos) {
            this.warpPos = blockPos;
            this.world = world;
        }
    }
}
