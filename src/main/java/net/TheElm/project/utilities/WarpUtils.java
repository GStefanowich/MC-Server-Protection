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

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.objects.MaskSet;
import net.TheElm.project.protections.BlockRange;
import net.TheElm.project.utilities.nbt.NbtUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.TextUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.command.CommandSource;
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
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public final class WarpUtils {
    public static final String PRIMARY_DEFAULT_HOME = "Homestead";
    private static final Set<UUID> GENERATING_PLAYERS = Collections.synchronizedSet(new HashSet<>());
    
    private final @NotNull String name;
    private BlockPos createWarpAt;
    private BlockRange region;
    
    public WarpUtils(@NotNull final String name, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos pos) {
        this.name = name;
        
        WarpUtils.GENERATING_PLAYERS.add(player.getUuid());
        this.updateWarpPos(pos);
    }
    
    private boolean updateWarpPos(@Nullable BlockPos warpPos) {
        if (warpPos == null)
            return false;
        
        this.createWarpAt = warpPos;
        this.region = BlockRange.between(
            new BlockPos(this.createWarpAt.getX() - 4, this.createWarpAt.getY() - 4, this.createWarpAt.getZ() - 3),
            new BlockPos(this.createWarpAt.getX() + 4, this.createWarpAt.getY() + 4, this.createWarpAt.getZ() + 6)
        );
        
        return true;
    }
    
    public BlockPos getWarpPositionIn(@NotNull final ServerWorld world) {
        int x = this.createWarpAt.getX();
        int z = this.createWarpAt.getZ();
        do {
            CoreMod.logInfo("Finding a new warp position!");
            this.updateWarpPos( new BlockPos( getRandom( x ), 256, getRandom( z ) ) );
        } while (!this.updateWarpPos(this.isValid( world, this.createWarpAt,50,true)));
        
        return this.createWarpAt;
    }
    public BlockPos getSafeTeleportPos(@NotNull final World world) {
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
    
    private static int getRandom(int position) {
        int random = ThreadLocalRandom.current().nextInt(
            position - SewConfig.get(SewConfig.WARP_MAX_DISTANCE),
            position + SewConfig.get(SewConfig.WARP_MAX_DISTANCE)
        );
        return ( 16 * Math.round(random >> 4) ) + 8;
    }
    
    private @Nullable BlockPos isValid(@NotNull final World world, @NotNull final BlockPos startingPos, final int minY, final boolean mustBeUnowned) {
        // If the chunks are claimed.
        if (mustBeUnowned && IClaimedChunk.isOwnedAround(world, this.createWarpAt, 5))
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
    
    public boolean claimAndBuild(@NotNull final ServerPlayerEntity player, @NotNull final ServerWorld world) {
        return this.claimAndBuild(player, world, false);
    }
    public boolean claimAndBuild(@NotNull final ServerPlayerEntity player, @NotNull final ServerWorld world, final boolean dropBlocks) {
        // Get the area of blocks to claim
        if (!ChunkUtils.canPlayerClaimSlices(world, this.region))
            return false;

        // Claim the defined slices in the name of Spawn
        ChunkUtils.claimSlices(world, CoreMod.SPAWN_ID, this.region);
        
        return this.build(player, world, dropBlocks);
    }
    
    public boolean build(@NotNull final ServerPlayerEntity player, @NotNull final ServerWorld world) {
        return this.build(player, world, true);
    }
    public boolean build(@NotNull final ServerPlayerEntity player, @NotNull final ServerWorld world, final boolean dropBlocks) {
        Biome biome = world.getBiome(this.createWarpAt);
        RegistryKey<Biome> biomeKey = RegistryUtils.getFromRegistry(world.getServer(), Registry.BIOME_KEY, biome);
        
        // Create the structure
        StructureBuilderUtils structure = new StructureBuilderUtils(world, "waystone");
        StructureBuilderUtils.StructureBuilderMaterial material = structure.forBiome(world.getRegistryKey(), biomeKey);
        
        final BlockState air = material.getAirBlock(world.getRegistryKey());
        
        // Light-source blocks
        final BlockState decLight = material.getLightSourceBlock();
        final BlockState ovrLight = material.getCoveringBlock();
        final BlockState undLight = material.getSupportingBlock();
        BlockPos[] decLightBlocks = new BlockPos[] {
            new BlockPos(this.createWarpAt.getX() + 1, this.createWarpAt.getY(), this.createWarpAt.getZ() + 1),
            new BlockPos(this.createWarpAt.getX() + 1, this.createWarpAt.getY(), this.createWarpAt.getZ() - 1),
            new BlockPos(this.createWarpAt.getX() - 1, this.createWarpAt.getY(), this.createWarpAt.getZ() + 1),
            new BlockPos(this.createWarpAt.getX() - 1, this.createWarpAt.getY(), this.createWarpAt.getZ() - 1)
        };
        for ( BlockPos blockPos : decLightBlocks ) {
            structure.addBlock(blockPos.up(1), ovrLight == null ? air : ovrLight);
            structure.addBlock(blockPos.up(2), air);
            structure.addBlock(blockPos, decLight);
            if (undLight != null)
                structure.addBlock(blockPos.down(), undLight);
        }
        
        // Andesite blocks
        final BlockState decJewel = material.getDecoratingBlock();
        BlockPos[] decJewelBlocks = new BlockPos[]{
            this.createWarpAt.offset(Direction.NORTH),
            this.createWarpAt.offset(Direction.SOUTH),
            this.createWarpAt.offset(Direction.EAST),
            this.createWarpAt.offset(Direction.WEST)
        };
        for ( BlockPos blockPos : decJewelBlocks ) {
            structure.addBlock(blockPos.up(1), air);
            structure.addBlock(blockPos.up(2), air);
            structure.addBlock(blockPos, decJewel);
        }
        
        // Diorite blocks
        final BlockState decBelowPlate = material.getMainBlock();
        BlockPos[] decBelowPlateBlocks = new BlockPos[]{
            this.createWarpAt
        };
        for ( BlockPos blockPos : decBelowPlateBlocks ) {
            structure.addBlock(blockPos.up( 1 ), air);
            structure.addBlock(blockPos.up( 2 ), air);
            structure.addBlock(blockPos, decBelowPlate);
        }
        
        // Bedrock blocks
        final BlockState decSupport = material.getStructureBlock();
        BlockPos[] bedrockBlocks = new BlockPos[]{
            this.createWarpAt.down(2),
            new BlockPos(this.createWarpAt.getX() + 1, this.createWarpAt.getY() - 1, this.createWarpAt.getZ()),
            new BlockPos(this.createWarpAt.getX(), this.createWarpAt.getY() - 1, this.createWarpAt.getZ() + 1),
            new BlockPos(this.createWarpAt.getX() - 1, this.createWarpAt.getY() - 1, this.createWarpAt.getZ()),
            new BlockPos(this.createWarpAt.getX(), this.createWarpAt.getY() - 1, this.createWarpAt.getZ() - 1)
        };
        for ( BlockPos blockPos : bedrockBlocks ) {
            structure.addBlock(blockPos, decSupport);
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
        final BlockState plate = material.getPressurePlateBlock();
        BlockPos[] pressurePlates = new BlockPos[]{
            this.createWarpAt.up()
        };
        for ( BlockPos blockPos : pressurePlates ) {
            structure.addBlock(blockPos, plate);
        }
        
        try {
            // Destroy the blocks where the waystone goes
            structure.destroy(dropBlocks);
            
            // Build the structure
            structure.build();
            
            // Play sounds
            structure.particlesSounds(ParticleTypes.HAPPY_VILLAGER, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 1.0f, 1.0f, 12,
                new BlockPos(this.createWarpAt.getX() - 1, this.createWarpAt.getY() + 3, this.createWarpAt.getZ() - 1),
                new BlockPos(this.createWarpAt.getX() + 1, this.createWarpAt.getY() + 2, this.createWarpAt.getZ() + 1),
                new BlockPos(this.createWarpAt.getX() + 1, this.createWarpAt.getY() + 2, this.createWarpAt.getZ() - 1),
                new BlockPos(this.createWarpAt.getX() - 1, this.createWarpAt.getY() + 1, this.createWarpAt.getZ() + 1)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException( e );
        }
        
        CoreMod.logInfo("Completed constructing new " + structure.getName());
        
        return true;
    }
    
    public void save(@NotNull final World world, @NotNull final BlockPos warpPos, @NotNull final ServerPlayerEntity player) {
        // Remove the player from the build list
        WarpUtils.GENERATING_PLAYERS.remove(player.getUuid());
        
        WarpUtils.Warp currentFavorite = WarpUtils.getWarp(player, null);
        boolean hasFavorite = (currentFavorite != null) && (!currentFavorite.name.equals(this.name));
        
        // Set the players warp position to save
        ((PlayerData) player).setWarp(new Warp(
            this.name,
            world,
            warpPos,
            !hasFavorite
        ));
        
        // Update the players Teleporting commands
        MinecraftServer server = player.getServer();
        if (server != null)
            server.getPlayerManager().sendCommandTree(player);
    }
    
    /*
     * Static checks
     */
    public static @Nullable Warp getWarp(@NotNull final UUID uuid, @Nullable final String location) {
        MinecraftServer server = ServerCore.get();
        
        // Read from the player
        ServerPlayerEntity player;
        if ((player = server.getPlayerManager().getPlayer(uuid)) != null)
            return WarpUtils.getWarp(player, location);
        return WarpUtils.fromMapByName(WarpUtils.getWarps(uuid), location);
    }
    public static @Nullable Warp getWarp(@NotNull final ServerPlayerEntity player, @Nullable final String location) {
        /*if ( ((PlayerData) player).getWarpPos() == null )
            return null;
        return new Warp("", ((PlayerData) player).getWarpWorld(), ((PlayerData) player).getWarpPos());*/
        return WarpUtils.fromMapByName(WarpUtils.getWarps(player), location);
    }
    public static @NotNull Map<String, WarpUtils.Warp> getWarps(@NotNull final UUID uuid) {
        MinecraftServer server = ServerCore.get();
        
        // Read from the player
        ServerPlayerEntity player;
        if ((player = server.getPlayerManager().getPlayer(uuid)) != null)
            return WarpUtils.getWarps(player);
        
        try {
            // Read from the NBT file
            CompoundTag playerNBT;
            if ((playerNBT = NbtUtils.readOfflinePlayerData(uuid)) == null)
                return Collections.emptyMap();
            
            return WarpUtils.fromNBT(playerNBT);
        } catch (NbtNotFoundException ignored) {}
        
        return Collections.emptyMap();
    }
    public static @NotNull Map<String, WarpUtils.Warp> getWarps(@NotNull final ServerPlayerEntity player) {
        return ((PlayerData)player).getWarps();
    }
    public static @NotNull Collection<String> getWarpNames(@NotNull final UUID uuid) {
        return WarpUtils.getWarpNames(WarpUtils.getWarps(uuid));
    }
    public static @NotNull Collection<String> getWarpNames(@NotNull final ServerPlayerEntity player) {
        return WarpUtils.getWarpNames(WarpUtils.getWarps(player));
    }
    public static @NotNull Collection<String> getWarpNames(@NotNull Map<String, WarpUtils.Warp> warps) {
        return new MaskSet<>(s -> s.contains(" ") ? "\"" + s + "\"" : s, warps.keySet());
    }
    public static boolean validateName(@NotNull String name) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9 ]*");
        return pattern.matcher(name).matches();
    }
    
    public static @NotNull Map<String, WarpUtils.Warp> fromNBT(@NotNull CompoundTag mainNBT) {
        Map<String, WarpUtils.Warp> warps = new ConcurrentHashMap<>();
        
        // Read the player warp location after restarting
        if (mainNBT.contains("playerWarpX", NbtType.NUMBER) && mainNBT.contains("playerWarpY", NbtType.NUMBER) && mainNBT.contains("playerWarpZ", NbtType.NUMBER)) {
            warps.put(WarpUtils.PRIMARY_DEFAULT_HOME, new Warp(
                WarpUtils.PRIMARY_DEFAULT_HOME,
                NbtUtils.worldRegistryFromTag(mainNBT.get("playerWarpD")),
                new BlockPos(
                    mainNBT.getInt("playerWarpX"),
                    mainNBT.getInt("playerWarpY"),
                    mainNBT.getInt("playerWarpZ")
                ),
                true
            ));
        } else if (mainNBT.contains("playerWarps", NbtType.COMPOUND)) {
            CompoundTag warpsNBT = mainNBT.getCompound("playerWarps");
            if (warpsNBT != null) {
                Set<String> warpNames = warpsNBT.getKeys();
                for (String warpName : warpNames) {
                    if (!warpsNBT.contains(warpName, NbtType.COMPOUND))
                        continue;
                    CompoundTag warpNBT = warpsNBT.getCompound(warpName);
                    if (warpNBT.contains("x", NbtType.NUMBER) && warpNBT.contains("y", NbtType.NUMBER) && warpNBT.contains("z", NbtType.NUMBER)) {
                        warps.put(warpName, new Warp(
                            warpName,
                            NbtUtils.worldRegistryFromTag(mainNBT.get("d")),
                            new BlockPos(
                                warpNBT.getInt("x"),
                                warpNBT.getInt("y"),
                                warpNBT.getInt("z")
                            ),
                            warpNBT.contains("fav", NbtType.BYTE) && warpNBT.getBoolean("fav")
                        ));
                    }
                }
            }
        }
        
        return warps;
    }
    public static @NotNull CompoundTag toNBT(@NotNull Map<String, WarpUtils.Warp> warps) {
        CompoundTag tag = new CompoundTag();
        warps.forEach((name, warp) -> tag.put(name, warp.toTag()));
        return tag;
    }
    public static @Nullable WarpUtils.Warp fromMapByName(@NotNull final Map<String, WarpUtils.Warp> map, @Nullable final String name) {
        if (map.isEmpty())
            return null;
        else if (name != null)
            return map.get(name);
        else return map.values()
            .stream().filter(warp -> warp.favorite)
            .findFirst().orElse(null);
    }
    
    public static boolean hasWarp(@NotNull final ServerCommandSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity)
            return WarpUtils.hasWarp((ServerPlayerEntity) entity);
        return false;
    }
    public static boolean hasWarp(@NotNull final UUID uuid) {
        return WarpUtils.hasWarp(uuid, null);
    }
    public static boolean hasWarp(@NotNull final UUID uuid, @Nullable final String name) {
        return WarpUtils.getWarp(uuid, name) != null;
    }
    public static boolean hasWarp(@NotNull final ServerPlayerEntity player) {
        return WarpUtils.hasWarp(player, null);
    }
    public static boolean hasWarp(@NotNull final ServerPlayerEntity player, @Nullable final String name) {
        return WarpUtils.getWarp(player, name) != null;
    }
    
    public static boolean isPlayerCreating(@NotNull final ServerPlayerEntity player) {
        return WarpUtils.GENERATING_PLAYERS.contains( player.getUuid() );
    }
    public static void teleportPlayer(@NotNull ServerPlayerEntity player, @Nullable String location) {
        Warp warp = WarpUtils.getWarp(player, location);
        if (warp != null)
            WarpUtils.teleportPlayer( warp, player );
    }
    public static void teleportPlayer(@NotNull final Warp warp, @NotNull final ServerPlayerEntity player) {
        WarpUtils.teleportPlayer(warp.world, player, warp.warpPos);
    }
    public static void teleportPlayer(@NotNull final RegistryKey<World> dimension, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos tpPos) {
        MinecraftServer server = player.getServer();
        ServerWorld world = server.getWorld(dimension);
        if (world != null)
            WarpUtils.teleportPlayer(world, player, tpPos);
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
            WarpUtils.teleportEntity(world, bottom, tpPos);
        else while (entity != null) {
            // Get the entity vehicle
            Entity vehicle = entity.getVehicle();
            
            WarpUtils.teleportEntity(world, entity, tpPos);
            
            // Teleport the vehicle
            entity = vehicle;
        }
    }
    public static void teleportEntity(@NotNull final ServerWorld world, @NotNull Entity entity, @NotNull final BlockPos tpPos) {
        // Must be a ServerWorld
        if (world.isClient || entity.removed)
            return;
        
        // Get the chunks
        ChunkPos chunkPos = new ChunkPos(tpPos);
        
        // Get the X, Y, and Z
        double x = tpPos.getX() + 0.5D,
            y = tpPos.getY(),
            z = tpPos.getZ() + 0.5D;
        
        // Set the teleport ticket
        world.getChunkManager()
            .addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, entity.getEntityId());
        
        // Reset velocity before teleporting
        entity.setVelocity(Vec3d.ZERO);
        entity.fallDistance = 0.0F;
        
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
                player.teleport(world, x, y, z, player.yaw, player.pitch);
            }
        } else {
            float i = MathHelper.wrapDegrees(entity.yaw);
            float j = MathHelper.wrapDegrees(entity.pitch);
            j = MathHelper.clamp(j, -90.0F, 90.0F);
            
            if (world == entity.world) {
                entity.refreshPositionAndAngles(x, y, z, i, j);
                entity.setHeadYaw(i);
            } else {
                entity.detach();
                Entity copyFrom = entity;
                entity = entity.getType()
                    .create(world);
                
                // Return if the entity failed to copy
                if (entity == null)
                    return;
                
                entity.copyFrom(copyFrom);
                entity.refreshPositionAndAngles(x, y, z, i,j);
                entity.setHeadYaw( i );
                world.onDimensionChanged(entity);
                copyFrom.removed = true;
            }
        }
        
        // Change the entity to not falling
        if (!(entity instanceof LivingEntity) || !((LivingEntity)entity).isFallFlying()) {
            entity.setVelocity(entity.getVelocity().multiply(1.0D, 0.0D, 1.0D));
            entity.setOnGround(true);
        }
    }
    public static void teleportEntity(@NotNull final ServerWorld world, @NotNull Entity entity) {
        WarpUtils.teleportEntity(world, entity, ServerCore.getSpawn(world));
    }
    public static void teleportEntity(@NotNull RegistryKey<World> dimension, @NotNull Entity entity) {
        WarpUtils.teleportEntity(ServerCore.getWorld(dimension), entity);
    }
    private static void teleportFriendlies(@NotNull final ServerWorld world, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos tpPos) {
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
    private static void teleportPoof(@NotNull final Entity entity) {
        final World world = entity.getEntityWorld();
        BlockPos blockPos = entity.getBlockPos();
        if ((world instanceof ServerWorld) && (!entity.isSpectator())) {
            world.playSound( null, blockPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 1.0f, 1.0f );
            EffectUtils.particleSwirl(ParticleTypes.WITCH, (ServerWorld) world, entity.getPos(), 10);
        }
    }
    public static @NotNull BlockPos getWorldSpawn(@NotNull final ServerWorld world) {
        WorldProperties properties = world.getLevelProperties();
        return new BlockPos(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ());
    }
    
    public static CompletableFuture<Suggestions> buildSuggestions(@Nullable UUID untrusted, @NotNull ServerPlayerEntity warpOwner, @NotNull SuggestionsBuilder builder) {
        return WarpUtils.buildSuggestions(warpOwner.getUuid(), untrusted, WarpUtils.getWarps(warpOwner), builder);
    }
    public static CompletableFuture<Suggestions> buildSuggestions(@Nullable UUID untrusted, @NotNull UUID warpOwner, @NotNull SuggestionsBuilder builder) {
        return WarpUtils.buildSuggestions(warpOwner, untrusted, WarpUtils.getWarps(warpOwner), builder);
    }
    private static CompletableFuture<Suggestions> buildSuggestions(@NotNull UUID warpOwner, @Nullable UUID untrusted, @NotNull Map<String, Warp> warps, @NotNull SuggestionsBuilder builder) {
        String remainder = builder.getRemaining().toLowerCase(Locale.ROOT);
        
        boolean canViewCoordinates = untrusted != null && (warpOwner.equals(untrusted) || ChunkUtils.canPlayerWarpTo(untrusted, warpOwner));
        for (Map.Entry<String, Warp> iterator : warps.entrySet()) {
            String name = iterator.getKey();
            if (name.contains(" "))
                name = TextUtils.quoteWrap(name);
            
            if (CommandSource.method_27136(remainder, name.toLowerCase(Locale.ROOT))) {
                Warp warp = iterator.getValue();
                MutableText world;
                MutableText position;
                if (canViewCoordinates) {
                    position = MessageUtils.xyzToText(warp.warpPos);
                    world = DimensionUtils.longDimensionName(warp.world)
                        .styled(DimensionUtils.dimensionColor(warp.world));
                } else {
                    position = MessageUtils.dimensionToTextComponent(", ", 999, 999, 999, Formatting.AQUA, Formatting.OBFUSCATED);
                    world = new LiteralText("").append(new LiteralText("Server")
                        .formatted(Formatting.GRAY, Formatting.OBFUSCATED));
                }
                
                // Add the suggestion to the builder
                builder.suggest(name, world.append(new LiteralText(" [").formatted(Formatting.WHITE)
                    .append(position)
                    .append("]")));
            }
        }
        
        return builder.buildFuture();
    }
    
    public static class Warp {
        public final @NotNull String name;
        public final @NotNull BlockPos warpPos;
        public final @NotNull RegistryKey<World> world;
        public boolean favorite;
        
        public Warp(@NotNull String name, @NotNull World world, @NotNull BlockPos blockPos, boolean favorite) {
            this(name, world.getRegistryKey(), blockPos, favorite);
        }
        public Warp(@NotNull String name, @NotNull RegistryKey<World> world, @NotNull BlockPos blockPos, boolean favorite) {
            this.name = name;
            this.warpPos = blockPos;
            this.world = world;
            this.favorite = favorite;
        }
        
        public Warp copy(@NotNull String name) {
            return new Warp(name, this.world, this.warpPos, this.favorite);
        }
        public Warp copy() {
            return this.copy(this.name);
        }
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            
            tag.putInt("x", this.warpPos.getX());
            tag.putInt("y", this.warpPos.getY());
            tag.putInt("z", this.warpPos.getZ());
            
            tag.putString("d", NbtUtils.worldToTag(this.world));
            tag.putBoolean("fav", this.favorite);
            
            return tag;
        }
    }
}
