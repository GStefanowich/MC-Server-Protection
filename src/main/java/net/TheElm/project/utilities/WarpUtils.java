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
import net.TheElm.project.commands.ClaimCommand;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.level.LevelProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class WarpUtils {
    
    private static final Set<UUID> warpPlayers = Collections.synchronizedSet(new HashSet<>());
    
    private BlockPos createWarpAt;
    
    public WarpUtils(final ServerPlayerEntity player, final BlockPos pos) {
        warpPlayers.add( player.getUuid() );
        this.createWarpAt = pos;
    }
    
    public BlockPos getWarpPositionIn(final BlockView view) {
        int x = this.createWarpAt.getX();
        int z = this.createWarpAt.getZ();
        do {
            CoreMod.logMessage( "Finding a new warp position!" );
            this.createWarpAt = new BlockPos( getRandom( x ), 256, getRandom( z ) );
        } while ((this.createWarpAt = this.isValid( view, this.createWarpAt,50,true ) ) == null);
        
        return this.createWarpAt;
    }
    public BlockPos getSafeTeleportPos(final BlockView view) {
        BlockPos tpPos;
        
        int count = 1;
        boolean negX = false;
        boolean negZ = false;
        
        do {
            int x = this.createWarpAt.getX() + ( 2 * count * ( negX ? -1 : 1 ));
            int z = this.createWarpAt.getZ() + ( 2 * count * ( negZ ? -1 : 1 ));
            
            tpPos = new BlockPos( x, 256, z );
            
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
            
        } while (( tpPos = this.isValid( view, tpPos, this.createWarpAt.getY() - 5,false ) ) == null );
        
        return tpPos.up( 2 );
    }
    
    private static int getRandom( int position ) {
        int random = ThreadLocalRandom.current().nextInt(
            position - SewingMachineConfig.INSTANCE.WARP_MAX_DISTANCE.get(),
            position + SewingMachineConfig.INSTANCE.WARP_MAX_DISTANCE.get()
        );
        return ( 16 * Math.round(random >> 4) ) + 8;
    }
    
    private BlockPos isValid(final BlockView view, final BlockPos startingPos, final int minY, final boolean mustBeUnowned) {
        // If the chunks are claimed.
        if (mustBeUnowned && ClaimedChunk.isOwnedAround( this.createWarpAt, 5 ))
            return null;
        
        BlockPos pos = startingPos;
        BlockState blockState;
        do {
            if ( pos.getY() <= minY )
                return null;
            
            pos = pos.down();
            blockState = view.getBlockState( pos );
        } while ( blockState.isAir() || ( mustBeUnowned && ( blockState.getMaterial() == Material.SNOW || blockState.getMaterial() == Material.PLANT )));
        
        Material material = blockState.getMaterial();
        
        // Don't set up a warp in a liquid (Water/Lava) or in Fire or on top of Trees
        return (!material.isLiquid())
            && (!( blockState.getBlock() instanceof MushroomBlock ))
            && ( material != Material.FIRE )
            && ( material != Material.LEAVES )
            && ( material != Material.ICE )
            && ( material != Material.PACKED_ICE )
            && ( mustBeUnowned || view.getBlockState( pos.up() ).isAir() ) ? pos : null;
    }
    
    public boolean build(final ServerPlayerEntity player, final World world) {
        return this.build(player, world, true);
    }
    public boolean build(final ServerPlayerEntity player, final World world, final boolean dropBlocks) {
        // Spawn position
        BlockPos spawnPos = WarpUtils.getWorldSpawn( world );
        
        // Claim the chunk in the name of Spawn
        if ( !ClaimCommand.tryClaimChunkAt( CoreMod.spawnID, player, this.createWarpAt ) )
            return false; // Return false (Try again!)
        
        StructureBuilderUtils structure = new StructureBuilderUtils( world,"waystone" );
        
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
        } catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
        
        return true;
    }
    
    public void save(final BlockPos warpPos, final ServerPlayerEntity player) {
        // Remove the player from the build list
        warpPlayers.remove( player.getUuid() );
        
        // Set the players warp position to save
        ((PlayerData) player).setWarpPos( warpPos );
        
        // Update the players Teleporting commands
        MinecraftServer server = player.getServer();
        if (server != null)
            server.getPlayerManager().sendCommandTree( player );
    }
    
    /*
     * Static checks
     */
    @Nullable
    public static BlockPos getPlayerWarp(final ServerPlayerEntity player) {
        return ((PlayerData) player).getWarpPos();
    }
    public static boolean isPlayerCreating(final ServerPlayerEntity player) {
        return warpPlayers.contains( player.getUuid() );
    }
    public static void teleportPlayer(@NotNull final World world, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos tpPos) {
        // Get the chunks
        ChunkPos chunkPos = new ChunkPos( tpPos );
        
        // Spawn the particles
        WarpUtils.teleportPoof( world, player );
        
        // Set the teleport ticket
        ((ServerChunkManager)world.getChunkManager()).addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getEntityId());
        
        // Unbind player from entities
        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true, false);
        }
        
        // Move the player
        if (world == player.world) {
            player.networkHandler.requestTeleport(tpPos.getX() + 0.5D, tpPos.getY(), tpPos.getZ() + 0.5D, player.yaw, player.pitch);
        } else if (world instanceof ServerWorld) {
            player.teleport((ServerWorld) world, tpPos.getX() + 0.5D, tpPos.getY(), tpPos.getZ() + 0.5D, player.yaw, player.pitch);
        }
    }
    private static void teleportPoof(final World world, final ServerPlayerEntity player) {
        BlockPos blockPos = player.getBlockPos();
        if (world instanceof ServerWorld) {
            ((ServerWorld) world).spawnParticles(ParticleTypes.POOF,
                blockPos.getX() + 0.5D,
                blockPos.getY() + 1.0D,
                blockPos.getZ() + 0.5D,
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
    
}
