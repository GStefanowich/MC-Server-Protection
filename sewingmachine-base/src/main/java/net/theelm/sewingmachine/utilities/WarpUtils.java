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

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.FireBlock;
import net.minecraft.block.IceBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.ServerCore;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.events.PlayerTeleportCallback;
import net.theelm.sewingmachine.events.RegionManageCallback;
import net.theelm.sewingmachine.events.RegionNameCallback;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.objects.MaskSet;
import net.theelm.sewingmachine.protections.BlockRange;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class WarpUtils {
    public static final @NotNull String PRIMARY_DEFAULT_HOME = "Homestead";
    private static final @NotNull Set<UUID> GENERATING_PLAYERS = Collections.synchronizedSet(new HashSet<>());
    private static final @NotNull Map<String, Warp> EMPTY = Collections.unmodifiableMap(new HashMap<>());
    
    private final @NotNull String name;
    private final @NotNull ServerWorld world;
    private @Nullable BlockPos createWarpAt;
    private BlockRange region;
    
    public WarpUtils(@NotNull final String name, @NotNull final ServerPlayerEntity player, @NotNull ServerWorld world, @NotNull final BlockPos pos) {
        this.name = name;
        this.world = world;
        
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
    
    public boolean getNewWarpPositionIn() {
        int x = WarpUtils.getRandom(this.createWarpAt.getX());
        int z = WarpUtils.getRandom(this.createWarpAt.getZ());
        int height = this.world.getDimension().height();
        
        WarpUtils.logSearchBegin(this.world, x, z);
        this.updateWarpPos(new BlockPos(x, height, z));
        
        // Return if the warp is a valid position
        return this.updateWarpPos(WarpUtils.isValid(this.world, this.createWarpAt,height, 10, true));
    }
    public @Nullable BlockPos getLastWarpPositionIn() {
        return this.createWarpAt;
    }
    public BlockPos getSafeTeleportPos() {
        final BlockPos start = this.createWarpAt.up();
        BlockPos tpPos = start;
        
        // Get the max X/Z (Spawn Radius)
        int maxX = 4;
        int maxI = 1 + ((maxX * maxX) * 4) + (maxX * 4);
        
        int x = 0;
        int z = 0;
        int dX = 0;
        int dZ = -1;
        
        for (int i = 0; i < maxI; i++) {
            BlockPos check = new BlockPos(x + start.getX(), start.getY(), z + start.getZ());
            if ((tpPos = WarpUtils.isValid(this.world, check, 5, 10, true)) != null)
                return tpPos.up(2);
            if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
                maxX = dX;
                dX = -dZ;
                dZ = maxX;
            }
            x += dX;
            z += dZ;
        }
        
        CoreMod.logDebug("Could not get valid teleport position at " + MessageUtils.xyzToString(this.createWarpAt));
        return this.createWarpAt.up(2);
    }
    
    private static int getRandom(int position) {
        int random = ThreadLocalRandom.current().nextInt(
            position - SewConfig.get(SewCoreConfig.WARP_MAX_DISTANCE),
            position + SewConfig.get(SewCoreConfig.WARP_MAX_DISTANCE)
        );
        return ( 16 * Math.round(random >> 4) ) + 8;
    }
    
    private static @Nullable BlockPos isValid(@NotNull final World world, @NotNull final BlockPos startingPos, final int maxDown, final int maxUp, boolean verbose) {
        int count = 0;
        int steps = maxDown + maxUp;
        
        BlockPos pos = startingPos;
        BlockState blockState;
        do {
            if ( count > steps ) {
                if (verbose)
                    WarpUtils.logSearchFailure(world, startingPos, SearchFailures.VOID);
                return null;
            }
            
            pos = count++ > maxDown ? pos.up() : pos.down();
            blockState = world.getBlockState(pos);
            
        } while (blockState.isAir() || blockState.isReplaceable() || !blockState.isFullCube(world, pos));
        
        SearchFailures failure = WarpUtils.canPathThrough(world, blockState, pos);
        if (failure != null) {
            if (verbose)
                WarpUtils.logSearchFailure(world, startingPos, failure);
            return null;
        }
        
        return pos;
    }
    private static SearchFailures canPathThrough(@NotNull World world, @NotNull BlockState state, @NotNull BlockPos pos) {
        // Don't set up a warp in a liquid (Water/Lava)
        if (state.isLiquid())
            return SearchFailures.FLUID;
        
        // Don't set up a warp in Fire
        if (state.getBlock() instanceof FireBlock)
            return SearchFailures.FIRE;
        
        // Don't set up a warp on top of Trees
        if (state.getBlock() instanceof LeavesBlock || state.getBlock() instanceof MushroomBlock)
            return SearchFailures.TREE_COLLISION;
        
        // Don't spawn on top of ice blocks
        if (state.getBlock() instanceof IceBlock)
            return SearchFailures.ICE;
        
        // Not enough space for a player
        if (!BlockUtils.isHollowBlock(world.getBlockState(pos.up())) )
            return SearchFailures.SUFFOCATION;
        
        return null;
    }
    
    public boolean claimAndBuild(@NotNull final Runnable runnable) {
        return this.claimAndBuild(runnable, false);
    }
    public boolean claimAndBuild(@NotNull final Runnable runnable, final boolean dropBlocks) {
        // Get the area of blocks to claim and claim it
        if (!RegionManageCallback.tryClaim(this.world, CoreMod.SPAWN_ID, this.region))
            return false;
        
        return this.build(runnable, dropBlocks);
    }
    
    public boolean build(@NotNull final Runnable runnable) {
        return this.build(runnable, true);
    }
    public boolean build(@NotNull final Runnable runnable, final boolean dropBlocks) {
        Biome biome = this.world.getBiome(this.createWarpAt).value();
        RegistryKey<Biome> biomeKey = RegistryUtils.getFromRegistry(this.world.getServer(), RegistryKeys.BIOME, biome);
        
        // Create the structure
        StructureBuilderUtils structure = new StructureBuilderUtils(this.world, this.createWarpAt, biomeKey, "waystone");
        final BlockState air = structure.material.getAirBlock(this.world.getRegistryKey());
        
        // Light-source blocks
        final BlockState decLight = structure.material.getLightSourceBlock();
        final BlockState ovrLight = structure.material.getCoveringBlock();
        final BlockState undLight = structure.material.getSupportingBlock();
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
        final BlockState decJewel = structure.material.getDecoratingBlock();
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
        final BlockState decBelowPlate = structure.material.getMainBlock();
        BlockPos[] decBelowPlateBlocks = new BlockPos[]{
            this.createWarpAt
        };
        for ( BlockPos blockPos : decBelowPlateBlocks ) {
            structure.addBlock(blockPos.up( 1 ), air);
            structure.addBlock(blockPos.up( 2 ), air);
            structure.addBlock(blockPos, decBelowPlate);
        }
        
        // Bedrock blocks
        final BlockState decSupport = structure.material.getStructureBlock();
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
            structure.addEntity(blockPos, blockEntity -> {
                if (blockEntity instanceof CommandBlockBlockEntity commandBlockBlock) {
                    commandBlockBlock.getCommandExecutor()
                        .setCommand("spawn @a[x=" + this.createWarpAt.getX() + ",z=" + this.createWarpAt.getZ() + ",y=" + this.createWarpAt.getY() + ",distance=0..2]");
                    
                    CoreMod.logDebug("Setting the warp command block entity");
                }
                
                return blockEntity;
            });
        }
        
        // Pressure plate
        final BlockState plate = structure.material.getPressurePlateBlock();
        BlockPos[] pressurePlates = new BlockPos[]{
            this.createWarpAt.up()
        };
        for ( BlockPos blockPos : pressurePlates ) {
            structure.addBlock(blockPos, plate);
        }
        
        structure.add(runnable);
        structure.add(() -> structure.particlesSound(ParticleTypes.HAPPY_VILLAGER, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 1.0f, 1.0f, 12, this.createWarpAt.add(-1, 3, -1)));
        structure.add(() -> structure.particlesSound(ParticleTypes.HAPPY_VILLAGER, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 1.0f, 1.0f, 12, this.createWarpAt.add(1, 2, 1)));
        structure.add(() -> structure.particlesSound(ParticleTypes.HAPPY_VILLAGER, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 1.0f, 1.0f, 12, this.createWarpAt.add(1, 2, -1)));
        structure.add(() -> structure.particlesSound(ParticleTypes.HAPPY_VILLAGER, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 1.0f, 1.0f, 12, this.createWarpAt.add(-1, 1, 1)));
        
        ((LogicalWorld)this.world).addTickableEvent(tickable -> {
            if (tickable.isRemoved())
                return true;
            
            if (tickable.getTicks() % structure.getDelay() == 0) {
                // If any of the chunks that are used for blocks are still being generated
                if (structure.generating())
                    return false;
                
                // Destroy the blocks where the waystone goes
                if (structure.destroy(dropBlocks))
                    return false;
                
                // Build the structure
                if (structure.build())
                    return false;
            }
            if (tickable.getTicks() % (structure.getDelay() * 10) == 0 && !structure.hasBuild()) {
                if (structure.after())
                    return false;
            }
            
            boolean completed = !(structure.hasDestroy() || structure.hasBuild() || structure.hasRunnable());
            if (completed)
                CoreMod.logInfo("Completed constructing new '" + structure.getName() + "' in '" + DimensionUtils.dimensionIdentifier(this.world) + "'.");
            return completed;
        });
        
        return true;
    }
    
    public void save(@NotNull final BlockPos warpPos, @NotNull final ServerPlayerEntity player) {
        // Remove the player from the build list
        WarpUtils.GENERATING_PLAYERS.remove(player.getUuid());
        
        WarpUtils.Warp currentFavorite = WarpUtils.getWarp(player, null);
        boolean hasFavorite = (currentFavorite != null) && (!currentFavorite.name.equals(this.name));
        
        // Set the players warp position to save
        ((PlayerData) player).setWarp(new Warp(
            this.name,
            this.world,
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
            NbtCompound playerNBT;
            if ((playerNBT = NbtUtils.readOfflinePlayerData(uuid)) == null)
                return Collections.emptyMap();
            
            return WarpUtils.fromNBT(playerNBT);
        } catch (NbtNotFoundException ignored) {}
        
        return Collections.emptyMap();
    }
    public static @NotNull Map<String, WarpUtils.Warp> getWarps(@NotNull final PlayerEntity player) {
        if (player instanceof PlayerData playerData)
            return playerData.getWarps();
        return WarpUtils.EMPTY;
    }
    public static @NotNull Stream<WarpUtils.Warp> getWarpStream(@NotNull final PlayerEntity player) {
        return WarpUtils.getWarps(player)
            .values()
            .stream()
            .sorted((w1, w2) -> String.CASE_INSENSITIVE_ORDER.compare(w1.name, w2.name));
    }
    public static @NotNull Collection<String> getWarpNames(@NotNull final UUID uuid) {
        return WarpUtils.getWarpNames(WarpUtils.getWarps(uuid));
    }
    public static @NotNull Collection<String> getWarpNames(@NotNull final PlayerEntity player) {
        return WarpUtils.getWarpNames(WarpUtils.getWarps(player));
    }
    public static @NotNull Collection<String> getWarpNames(@NotNull Map<String, WarpUtils.Warp> warps) {
        return new MaskSet<>(s -> s.contains(" ") ? "\"" + s + "\"" : s, warps.keySet());
    }
    public static boolean validateName(@NotNull String name) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9 ]*");
        return pattern.matcher(name).matches();
    }
    
    public static @NotNull Map<String, WarpUtils.Warp> fromNBT(@NotNull NbtCompound mainNBT) {
        Map<String, WarpUtils.Warp> warps = new ConcurrentHashMap<>();
        
        // Read the player warp location after restarting
        if (mainNBT.contains("playerWarpX", NbtElement.NUMBER_TYPE) && mainNBT.contains("playerWarpY", NbtElement.NUMBER_TYPE) && mainNBT.contains("playerWarpZ", NbtElement.NUMBER_TYPE)) {
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
        } else if (mainNBT.contains("playerWarps", NbtElement.COMPOUND_TYPE)) {
            NbtCompound warpsNBT = mainNBT.getCompound("playerWarps");
            if (warpsNBT != null) {
                Set<String> warpNames = warpsNBT.getKeys();
                for (String warpName : warpNames) {
                    if (!warpsNBT.contains(warpName, NbtElement.COMPOUND_TYPE))
                        continue;
                    NbtCompound warpNBT = warpsNBT.getCompound(warpName);
                    if (warpNBT.contains("x", NbtElement.NUMBER_TYPE) && warpNBT.contains("y", NbtElement.NUMBER_TYPE) && warpNBT.contains("z", NbtElement.NUMBER_TYPE)) {
                        warps.put(warpName, new Warp(
                            warpName,
                            NbtUtils.worldRegistryFromTag(warpNBT.get("d")),
                            new BlockPos(
                                warpNBT.getInt("x"),
                                warpNBT.getInt("y"),
                                warpNBT.getInt("z")
                            ),
                            warpNBT.contains("fav", NbtElement.BYTE_TYPE) && warpNBT.getBoolean("fav")
                        ));
                    }
                }
            }
        }
        
        return warps;
    }
    public static @NotNull NbtCompound toNBT(@NotNull Map<String, WarpUtils.Warp> warps) {
        NbtCompound tag = new NbtCompound();
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
        if (entity instanceof ServerPlayerEntity serverPlayer)
            return WarpUtils.hasWarp(serverPlayer);
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
        return WarpUtils.GENERATING_PLAYERS.contains(player.getUuid());
    }
    public static Warp teleportEntityAndAttached(@NotNull final ServerPlayerEntity player, @Nullable String location) {
        Warp warp = WarpUtils.getWarp(player, location);
        if (warp != null)
            WarpUtils.teleportEntityAndAttached(player, warp);
        return warp;
    }
    public static void teleportEntityAndAttached(@NotNull final Entity entity, @NotNull final Warp warp) {
        if (entity instanceof ServerPlayerEntity player) {
            Text townName = null;
            
            MinecraftServer server = entity.getServer();
            if (server != null) {
                ServerWorld world = server.getWorld(warp.world);
                
                if (world != null)
                    townName = RegionNameCallback.EVENT.invoker()
                        .getName(world, warp.warpPos, null, true, false);
            }
            
            TitleUtils.showPlayerTitle(player, townName, Text.literal(warp.name).formatted(Formatting.AQUA));
        }
        WarpUtils.teleportEntityAndAttached(warp.world, entity, warp.warpPos);
    }
    public static void teleportEntityAndAttached(@NotNull final RegistryKey<World> dimension, @NotNull final Entity entity, @NotNull final BlockPos tpPos) {
        MinecraftServer server = entity.getServer();
        ServerWorld world = server.getWorld(dimension);
        if (world != null)
            WarpUtils.teleportEntityAndAttached(world, entity, tpPos);
    }
    public static void teleportEntityAndAttached(@NotNull final ServerWorld world, @NotNull final Entity entity, @NotNull final BlockPos tpPos) {
        Entity bottom = entity.getRootVehicle(),
            target = entity;
        
        // Spawn the particles (In the entities world)
        WarpUtils.teleportPoof(bottom, true);
        
        // Warp anything attached to the player
        WarpUtils.teleportFriendlies(world, entity, tpPos);
        
        // If the entity can be teleported within the same world
        if (world == entity.getWorld() || bottom == entity) {
            WarpUtils.teleportEntity(world, bottom, tpPos);
            
            // Show an effect in the new location
            if (!bottom.isSpectator())
                WarpUtils.teleportPoof(world, tpPos, false);
        } else while (target != null) {
            // Get the entity vehicle
            Entity vehicle = target.getVehicle();
            
            WarpUtils.teleportEntity(world, target, tpPos);
            
            // Teleport the vehicle
            target = vehicle;
        }
    }
    public static void teleportEntityAndAttached(@NotNull final ServerWorld world, @NotNull final Entity entity) {
        WarpUtils.teleportEntityAndAttached(world, entity, ServerCore.getSpawn(world));
    }
    public static void teleportEntityAndAttached(@NotNull final RegistryKey<World> world, @NotNull final Entity entity) {
        WarpUtils.teleportEntityAndAttached(ServerCore.getWorld(entity, world), entity);
    }
    public static void teleportEntity(@NotNull final RegistryKey<World> dimension, @NotNull Entity entity, @NotNull final BlockPos tpPos) {
        WarpUtils.teleportEntity(ServerCore.getWorld(entity, dimension), entity, tpPos);
    }
    public static void teleportEntity(@NotNull final ServerWorld world, @NotNull Entity entity, @NotNull final BlockPos tpPos) {
        // Must be a ServerWorld
        if (world.isClient || entity.isRemoved())
            return;
        
        // Get the chunks
        ChunkPos chunkPos = new ChunkPos(tpPos);
        
        // Get the X, Y, and Z
        double x = tpPos.getX() + 0.5D,
            y = tpPos.getY(),
            z = tpPos.getZ() + 0.5D;
        
        // Set the teleport ticket
        world.getChunkManager()
            .addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, entity.getId());
        
        // Reset velocity before teleporting
        entity.setVelocity(Vec3d.ZERO);
        entity.fallDistance = 0.0F;
        
        if (entity instanceof ServerPlayerEntity player) {
            // Unbind player from entities
            player.stopRiding();
            if (player.isSleeping()) // Make sure that the player is not sleeping
                player.wakeUp(true, true);
            
            // Move the player
            if (world == player.getWorld()) {
                player.networkHandler.requestTeleport(x, y, z, player.getYaw(), player.getPitch());
                player.networkHandler.syncWithPlayerPosition();
            } else {
                player.teleport(world, x, y, z, player.getYaw(), player.getPitch());
            }
        } else {
            float i = MathHelper.wrapDegrees(entity.getYaw());
            float j = MathHelper.wrapDegrees(entity.getPitch());
            j = MathHelper.clamp(j, -90.0F, 90.0F);
            
            if (world == entity.getWorld()) {
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
                copyFrom.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
            }
        }
        
        // Change the entity to not falling
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isFallFlying()) {
            entity.setVelocity(entity.getVelocity().multiply(1.0D, 0.0D, 1.0D));
            entity.setOnGround(true);
        }
    }
    public static void teleportEntity(@NotNull final ServerWorld world, @NotNull Entity entity) {
        WarpUtils.teleportEntity(world, entity, ServerCore.getSpawn(world));
    }
    public static void teleportEntity(@NotNull final RegistryKey<World> dimension, @NotNull Entity entity) {
        WarpUtils.teleportEntity(ServerCore.getWorld(entity, dimension), entity);
    }
    private static void teleportFriendlies(@NotNull final ServerWorld world, @NotNull final Entity entity, @NotNull final BlockPos tpPos) {
        // Get the ENTITY location (To get nearby mobs)
        BlockPos playerPos = entity.getBlockPos();
        int x = playerPos.getX(),
            y = playerPos.getY(),
            z = playerPos.getZ();
        
        // Get nearby mobs from the ENTITY world (Not 'world' = the world where being sent to)
        List<MobEntity> list = entity.getEntityWorld()
            .getNonSpectatingEntities(MobEntity.class, new Box((double)x - 7.0D, (double)y - 7.0D, (double)z - 7.0D, (double)x + 7.0D, (double)y + 7.0D, (double)z + 7.0D));
        Iterator<MobEntity> iterator = list.iterator();
        
        while (iterator.hasNext()) {
            MobEntity mob = iterator.next();
            
            // Check if the entity that is in the area is being held by our entity
            boolean lead = (!(entity instanceof LeashKnotEntity)) && (mob.getHoldingEntity() == entity),
                // Get if the mob is riding on the entity
                ride = (mob.getVehicle() == entity);
            
            if (lead || ride || WarpUtils.mobCanTeleportWith(mob, entity)) {
                if (lead && (!ride))
                    WarpUtils.teleportPoof(mob, true);
                WarpUtils.teleportEntity(world, mob, tpPos);
            }
        }
    }
    public static void teleportPoof(@NotNull final Entity entity, boolean departing) {
        if (!entity.isSpectator())
            WarpUtils.teleportPoof(entity.getEntityWorld(), entity.getBlockPos(), entity.getPos(), departing);
    }
    public static void teleportPoof(@Nullable final World world, @NotNull final BlockPos blockPos, boolean departing) {
        WarpUtils.teleportPoof(world, blockPos, Vec3d.ofBottomCenter(blockPos), departing);
    }
    private static void teleportPoof(@Nullable final World world, @NotNull final BlockPos blockPos, final Vec3d swirlPos, boolean departing) {
        if (world instanceof ServerWorld serverWorld) {
            world.playSound(null, blockPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 1.0f, 1.0f);
            EffectUtils.particleSwirl(ParticleTypes.WITCH, serverWorld, swirlPos, departing, 10);
        }
    }
    public static @NotNull BlockPos getWorldSpawn(@NotNull final ServerWorld world) {
        WorldProperties properties = world.getLevelProperties();
        return new BlockPos(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ());
    }
    
    private static boolean mobCanTeleportWith(@NotNull Entity target, @NotNull Entity with) {
        if (!(with instanceof PlayerEntity) || !(target instanceof TameableEntity tameable))
            return false;
        return tameable.isTamed() && !tameable.isSitting() && Objects.equals(with.getUuid(), tameable.getOwnerUuid());
    }
    
    public static CompletableFuture<Suggestions> buildSuggestions(@NotNull MinecraftServer server, @Nullable UUID untrusted, @NotNull ServerPlayerEntity warpOwner, @NotNull SuggestionsBuilder builder) {
        return WarpUtils.buildSuggestions(server, warpOwner.getUuid(), untrusted, WarpUtils.getWarps(warpOwner), builder);
    }
    public static CompletableFuture<Suggestions> buildSuggestions(@NotNull MinecraftServer server, @Nullable UUID untrusted, @NotNull UUID warpOwner, @NotNull SuggestionsBuilder builder) {
        return WarpUtils.buildSuggestions(server, warpOwner, untrusted, WarpUtils.getWarps(warpOwner), builder);
    }
    private static CompletableFuture<Suggestions> buildSuggestions(@NotNull MinecraftServer server, @NotNull UUID warpOwner, @Nullable UUID untrusted, @NotNull Map<String, Warp> warps, @NotNull SuggestionsBuilder builder) {
        String remainder = builder.getRemaining().toLowerCase(Locale.ROOT);
        
        boolean canViewCoordinates = untrusted != null && (warpOwner.equals(untrusted) || PlayerTeleportCallback.canTeleport(server, untrusted, warpOwner));
        for (Map.Entry<String, Warp> iterator : warps.entrySet()) {
            String name = iterator.getKey();
            if (name.contains(" "))
                name = TextUtils.quoteWrap(name);
            
            if (CommandSource.shouldSuggest(remainder, name.toLowerCase(Locale.ROOT))) {
                Warp warp = iterator.getValue();
                MutableText world;
                MutableText position;
                if (canViewCoordinates) {
                    position = MessageUtils.xyzToText(warp.warpPos);
                    world = DimensionUtils.longDimensionName(warp.world)
                        .styled(DimensionUtils.dimensionColor(warp.world));
                } else {
                    position = MessageUtils.dimensionToTextComponent(", ", 999, 999, 999, Formatting.AQUA, Formatting.OBFUSCATED);
                    world = Text.literal("").append(Text.literal("Server")
                        .formatted(Formatting.GRAY, Formatting.OBFUSCATED));
                }
                
                // Add the suggestion to the builder
                builder.suggest(name, world.append(Text.literal(" [").formatted(Formatting.WHITE)
                    .append(position)
                    .append("]")));
            }
        }
        
        return builder.buildFuture();
    }
    
    private static void logSearchBegin(@NotNull World world, int x, int z) {
        CoreMod.logInfo("Verifying warp position in '" + DimensionUtils.dimensionIdentifier(world) + "' at " + x + ", ~, " + z);
    }
    private static void logSearchFailure(@NotNull World world, @NotNull BlockPos pos, @Nullable SearchFailures failure) {
        CoreMod.logInfo("Failed to validate warp position in '" + DimensionUtils.dimensionIdentifier(world) + "' at " + pos.getX() + ", ~, " + pos.getZ() + " due to 'Reason: " + (failure == null ? "null" : failure.name()) + "'.");
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
        
        public @Nullable World getWorld(@Nullable World world) {
            return world == null ? null : (Objects.equals(world.getRegistryKey(), this.world) ? world : this.getWorld(world.getServer()));
        }
        public @Nullable World getWorld(@Nullable MinecraftServer server) {
            return server == null ? null : server.getWorld(this.world);
        }
        
        public boolean isIn(@NotNull World world) {
            return this.isIn(world.getRegistryKey());
        }
        public boolean isIn(@NotNull RegistryKey<World> world) {
            return Objects.equals(world, this.world);
        }
        
        public boolean isAt(@NotNull World world, @NotNull BlockPos pos) {
            return this.isAt(world.getRegistryKey(), pos);
        }
        public boolean isAt(@NotNull RegistryKey<World> world, @NotNull BlockPos pos) {
            return this.isIn(world) && Objects.equals(pos, this.warpPos);
        }
        public boolean isFavorite() {
            return this.favorite;
        }
        
        public int compare(@NotNull Warp warp) {
            int favorites = Boolean.compare(warp.favorite, this.favorite);
            return favorites == 0 ? String.CASE_INSENSITIVE_ORDER.compare(this.name, warp.name) : favorites;
        }
        
        public Warp copy(@NotNull String name) {
            return new Warp(name, this.world, this.warpPos, this.favorite);
        }
        public Warp copy() {
            return this.copy(this.name);
        }
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            
            tag.putInt("x", this.warpPos.getX());
            tag.putInt("y", this.warpPos.getY());
            tag.putInt("z", this.warpPos.getZ());
            
            tag.putString("d", NbtUtils.worldToTag(this.world));
            tag.putBoolean("fav", this.favorite);
            
            return tag;
        }
    }
    private enum SearchFailures {
        VOID,
        FLUID,
        FIRE,
        ICE,
        TREE_COLLISION,
        SUFFOCATION
    }
}
