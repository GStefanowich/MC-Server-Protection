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
import net.TheElm.project.config.SewConfig;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    
    private boolean updateWarpPos(@Nullable BlockPos warpPos) {
        if (warpPos == null)
            return false;
        
        this.createWarpAt = warpPos;
        this.region = new Pair<>(
            new BlockPos( this.createWarpAt.getX() - 4, this.createWarpAt.getY() - 4, this.createWarpAt.getZ() - 3 ),
            new BlockPos( this.createWarpAt.getX() + 4, this.createWarpAt.getY() + 4, this.createWarpAt.getZ() + 6 )
        );
        
        return true;
    }
    
    public BlockPos getWarpPositionIn(final ServerWorld world) {
        int x = this.createWarpAt.getX();
        int z = this.createWarpAt.getZ();
        do {
            CoreMod.logInfo( "Finding a new warp position!" );
            this.updateWarpPos( new BlockPos( getRandom( x ), 256, getRandom( z ) ) );
        } while (!this.updateWarpPos(this.isValid( world, this.createWarpAt,50,true)));
        
        return this.createWarpAt;
    }
    public BlockPos getSafeTeleportPos(final World world) {
        BlockPos tpPos;
        
        int count = 1;
        int maxHeight = ( world.getDimension().hasSkyLight() ? 256 : this.createWarpAt.getY() + 10 );
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
            position - SewConfig.get(SewConfig.WARP_MAX_DISTANCE),
            position + SewConfig.get(SewConfig.WARP_MAX_DISTANCE)
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
        } while ( blockState.isAir() || blockState.getMaterial().isReplaceable() || ( mustBeUnowned && ( blockState.getMaterial() == Material.SNOW_LAYER || blockState.getMaterial() == Material.PLANT )));
        
        Material material = blockState.getMaterial();
        
        // Don't set up a warp in a liquid (Water/Lava) or in Fire or on top of Trees
        return (!material.isLiquid())
            && (!( blockState.getBlock() instanceof MushroomBlock ))
            && ( material != Material.FIRE )
            && ( material != Material.LEAVES )
            && ( material != Material.ICE )
            && ( material != Material.DENSE_ICE )
            && ( world.getBlockState( pos.up() ).isAir() ) ? pos : null;
    }
    
    public boolean build(final ServerPlayerEntity player, final ServerWorld world) {
        return this.build(player, world, true);
    }
    public boolean build(final ServerPlayerEntity player, final ServerWorld world, final boolean dropBlocks) {
        // Get the area of blocks to claim
        if (!ChunkUtils.canPlayerClaimSlices( world, this.region.getLeft(), this.region.getRight() ))
            return false;
        
        // Claim the defined slices in the name of Spawn
        ChunkUtils.claimSlices(world, CoreMod.spawnID, this.region.getLeft(), this.region.getRight());
        
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
            structure.addBlock(blockPos.up( 1 ), air);
            structure.addBlock(blockPos.up( 2 ), air);
            structure.addBlock(blockPos, light);
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
            structure.addBlock(blockPos, cmdBlock);
            structure.addEntity(blockPos, () -> {
                CommandBlockBlockEntity blockEntity = BlockEntityType.COMMAND_BLOCK.instantiate();
                if (blockEntity != null) {
                    blockEntity.getCommandExecutor()
                        .setCommand("spawn @a[x=" + this.createWarpAt.getX() + ",z=" + this.createWarpAt.getZ() + ",y=" + this.createWarpAt.getY() + ",distance=0..2]");
                    
                    CoreMod.logDebug("Setting the warp command block entity");
                }
                
                return blockEntity;
            });
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
            // Destroy the blocks where the waystone goes
            structure.destroy( dropBlocks );
            
            // Build the structure
            structure.build();
            
            // Play sounds
            structure.particlesSounds(ParticleTypes.HAPPY_VILLAGER, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 1.0f, 1.0f, 12,
                new BlockPos( this.createWarpAt.getX() - 1, this.createWarpAt.getY() + 3, this.createWarpAt.getZ() - 1 ),
                new BlockPos( this.createWarpAt.getX() + 1, this.createWarpAt.getY() + 2, this.createWarpAt.getZ() + 1 ),
                new BlockPos( this.createWarpAt.getX() + 1, this.createWarpAt.getY() + 2, this.createWarpAt.getZ() - 1 ),
                new BlockPos( this.createWarpAt.getX() - 1, this.createWarpAt.getY() + 1, this.createWarpAt.getZ() + 1 )
            );
        } catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
        
        CoreMod.logInfo("Completed constructing new " + structure.getName());
        
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
                warp = new Warp(
                    server.getWorld(NbtUtils.worldRegistryFromTag(playerNBT.get("playerWarpD"))),
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
        if (warp != null)
            WarpUtils.teleportPlayer( warp, player );
    }
    public static void teleportPlayer(@NotNull final Warp warp, @NotNull final ServerPlayerEntity player) {
        WarpUtils.teleportPlayer( warp.world, player, warp.warpPos );
    }
    public static void teleportPlayer(@NotNull final ServerWorld world, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos tpPos) {
        Entity bottom = player.getRootVehicle(),
            entity = player;
        
        // Spawn the particles (In the entities world)
        WarpUtils.teleportPoof(bottom);
        
        // Warp anything attached to the player
        WarpUtils.teleportFriendlies(world, player, tpPos);
        
        // If the entity can be teleported within the same world
        if (world == player.world || bottom == player)
            WarpUtils.teleportEntity(world, bottom, tpPos );
        else while (entity != null) {
            // Get the entity vehicle
            Entity vehicle = entity.getVehicle();
            
            WarpUtils.teleportEntity(world, entity, tpPos);
            
            // Teleport the vehicle
            entity = vehicle;
        }
    }
    public static void teleportEntity(@NotNull final World world, @NotNull Entity entity, @NotNull final BlockPos tpPos) {
        // Must be a ServerWorld
        if (world.isClient) return;
        
        // Get the chunks
        ChunkPos chunkPos = new ChunkPos( tpPos );
        
        // Get the X, Y, and Z
        double x = tpPos.getX() + 0.5D,
            y = tpPos.getY(),
            z = tpPos.getZ() + 0.5D;
        
        // Set the teleport ticket
        ((ServerWorld) world).getChunkManager().addTicket(ChunkTicketType.field_19347, chunkPos, 1, entity.getEntityId());
        
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity)entity;
            
            // Unbind player from entities
            player.stopRiding();
            if (player.isSleeping()) // Make sure that the player is not sleeping
                player.wakeUp(true, true);
            
            // Move the player
            if (world == player.world) {
                player.networkHandler.requestTeleport(x, y, z, player.yaw, player.pitch);
                player.networkHandler.syncWithPlayerPosition();
            } else {
                player.teleport(((ServerWorld) world), x, y, z, player.yaw, player.pitch);
            }
        } else {
            float i = MathHelper.wrapDegrees( entity.yaw );
            float j = MathHelper.wrapDegrees( entity.pitch );
            j = MathHelper.clamp(j, -90.0F, 90.0F);
            
            if (world == entity.world) {
                entity.refreshPositionAndAngles(x, y, z, i, j);
                entity.setHeadYaw( i );
            } else {
                entity.detach();
                Entity copyFrom = entity;
                entity = entity.getType().create( world );
                
                // Return if the entity failed to copy
                if (entity == null)
                    return;
                
                entity.copyFrom(copyFrom);
                entity.refreshPositionAndAngles(x, y, z, i,j);
                entity.setHeadYaw( i );
                ((ServerWorld) world).onDimensionChanged(entity);
                copyFrom.removed = true;
            }
        }
        
        // Change the entity to not falling
        if (!(entity instanceof LivingEntity) || !((LivingEntity)entity).isFallFlying()) {
            entity.setVelocity(entity.getVelocity().multiply(1.0D, 0.0D, 1.0D));
            entity.setOnGround(true);
        }
    }
    public static void teleportEntity(@NotNull final World world, @NotNull Entity entity) {
        WarpUtils.teleportEntity(world, entity, ServerCore.getSpawn(world));
    }
    public static void teleportEntity(@NotNull RegistryKey<World> dimension, @NotNull Entity entity) {
        WarpUtils.teleportEntity(ServerCore.getWorld(dimension), entity);
    }
    private static void teleportFriendlies(@NotNull final World world, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos tpPos) {
        BlockPos playerPos = player.getBlockPos();
        int x = playerPos.getX(),
            y = playerPos.getY(),
            z = playerPos.getZ();
        List<MobEntity> list = world.getNonSpectatingEntities(MobEntity.class, new Box((double)x - 7.0D, (double)y - 7.0D, (double)z - 7.0D, (double)x + 7.0D, (double)y + 7.0D, (double)z + 7.0D));
        Iterator<MobEntity> iterator = list.iterator();
        
        while (iterator.hasNext()) {
            MobEntity mob = iterator.next();
            boolean lead = (mob.getHoldingEntity() == player),
                ride = (mob.getVehicle() == player);
            
            if (lead || ride || (mob instanceof TameableEntity && (!((TameableEntity)mob).isSitting()))) {
                if (lead && (!ride))
                    WarpUtils.teleportPoof(mob);
                WarpUtils.teleportEntity(world, mob, tpPos);
            }
        }
    }
    private static void teleportPoof(final Entity entity) {
        final World world = entity.getEntityWorld();
        BlockPos blockPos = entity.getBlockPos();
        if ((world instanceof ServerWorld) && (!entity.isSpectator())) {
            world.playSound( null, blockPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 1.0f, 1.0f );
            EffectUtils.particleSwirl(ParticleTypes.WITCH, (ServerWorld) world, entity.getPos(), 10);
        }
    }
    public static BlockPos getWorldSpawn(@NotNull final ServerWorld world) {
        WorldProperties properties = world.getLevelProperties();
        return new BlockPos(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ());
    }
    
    public static class Warp {
        public final BlockPos warpPos;
        public final ServerWorld world;
        
        private Warp(ServerWorld world, BlockPos blockPos) {
            this.warpPos = blockPos;
            this.world = world;
        }
    }
}
