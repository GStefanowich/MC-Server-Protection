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

package net.theelm.sewingmachine.base.mixins.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.registry.RegistryKey;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.commands.PlayerSpawnCommand;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.CompassDirections;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.interfaces.ModUser;
import net.theelm.sewingmachine.interfaces.MoneyHolder;
import net.theelm.sewingmachine.interfaces.PlayerData;
import net.theelm.sewingmachine.interfaces.PlayerServerLanguage;
import net.theelm.sewingmachine.utilities.EffectUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.SleepUtils;
import net.theelm.sewingmachine.utilities.WarpUtils;
import net.theelm.sewingmachine.utilities.nbt.NbtUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkCache;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements PlayerData, ModUser, PlayerServerLanguage {
    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Shadow private boolean notInAnyWorld;
    
    @Shadow private RegistryKey<World> spawnPointDimension;
    
    @Shadow public abstract ServerWorld getServerWorld();
    
    // Client language
    private Locale clientLanguage = Locale.ENGLISH;
    
    // Health bar
    private int healthTick = 0;
    @Nullable
    private ServerBossBar healthBar = null;
    
    // Join times
    private Long firstJoinedAt = null;
    private Long lastJoinedAt = null;
    
    // Warps
    private final @NotNull Map<String, WarpUtils.Warp> warps = new ConcurrentHashMap<>();
    
    // List of Sew Modules that the player has installed
    private final @NotNull List<String> clientModules = new ArrayList<>();
    
    // Portal locations
    private BlockPos overworldPortal = null;
    private BlockPos theNetherPortal = null;
    
    // Compass
    private CompassDirections compassDirection = CompassDirections.SPAWN;
    private BlockPos compassFocal = BlockPos.ORIGIN;
    
    @Inject(at = @At("RETURN"), method = "<init>*")
    public void onInitialize(CallbackInfo callback) {
        this.spawnPointDimension = SewConfig.get(SewCoreConfig.DEFAULT_WORLD);
    }
    
    public ServerPlayerEntityMixin(World world, BlockPos blockPos, float yaw, GameProfile gameProfile) {
        super(world, blockPos, yaw, gameProfile);
    }
    
    /*
     * Health Bar
     */
    
    @Inject(at = @At("TAIL"), method = "tick")
    public void onTick(CallbackInfo callback) {
        // Handle Health Bar
        if ((this.healthBar != null) && (!this.notInAnyWorld)) {
            // Get players from the health bar
            Collection<ServerPlayerEntity> cache = this.healthBar.getPlayers();
            if (!cache.isEmpty()) {
                // Get local players
                if (!this.isAlive())
                    this.healthBar.clearPlayers();
                else if ((++this.healthTick) >= 60) {
                    // Get the area around the player
                    Box searchRegion = new Box(
                        BlockPos.ofFloored(this.getX() + 20, this.getY() + 10, this.getZ() + 20),
                        BlockPos.ofFloored(this.getX() - 20, this.getY() - 10, this.getZ() - 20)
                    );
                    List<ServerPlayerEntity> players = this.getServerWorld()
                        .getEntitiesByClass(ServerPlayerEntity.class, searchRegion, (nearby) -> (!nearby.getUuid().equals(this.getUuid())));
                    
                    // Remove all locale players
                    List<ServerPlayerEntity> enemies = new ArrayList<>(cache);
                    enemies.removeAll(players);
                    
                    // Remove if not nearby
                    if (!enemies.isEmpty()) {
                        for (ServerPlayerEntity enemy : enemies)
                            this.healthBar.removePlayer(enemy);
                    }
                    
                    // Set the health percentage
                    this.updateHealthBar();
                    
                    // Reset the tick
                    this.healthTick = 0;
                }
            }
        }
        
        // Handle location finder
        if (this.sewTrailTargetPos != null) {
            if (this.sewTrailTicks > 0)
                this.sewTrailTicks--;
            else {
                if (this.sewTrailTargetPos.getSquaredDistance(this.getBlockPos()) > 100) {
                    Path path = this.findPathTo(this.sewTrailTargetPos, 3);
                    if (path != null)
                        EffectUtils.summonBreadcrumbs(ParticleTypes.FALLING_OBSIDIAN_TEAR, (ServerPlayerEntity)(Entity)this, path);
                } else this.sewTrailTargetPos = null;
                this.sewTrailTicks = ServerPlayerEntityMixin.NODE_TICKS;
            }
        }
    }
    @Inject(at = @At("TAIL"), method = "onDeath")
    public void onDeath(DamageSource damageSource, CallbackInfo callback) {
        if (this.healthBar != null) {
            this.healthBar.setPercent(0.0f);
            this.healthBar.clearPlayers();
        }
    }
    @Inject(at = @At("RETURN"), method = "damage")
    public void onDamage(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> callback) {
        if ((!this.getServerWorld().isClient)) {
            // Set the health percentage
            this.updateHealthBar();
            
            // Add the attacker to the healthbar
            if ((damageSource.getAttacker() instanceof ServerPlayerEntity serverPlayer) && (damageSource.getAttacker() != this))
                this.getHealthBar().addPlayer(serverPlayer);
        }
    }
    private void updateHealthBar() {
        // Get the health percentage
        EntityUtils.updateHealthBar(this, this.getHealthBar());
    }
    @Override
    public @Contract("true -> !null") ServerBossBar getHealthBar(boolean create) {
        if (this.healthBar != null || !create)
            return this.healthBar;
        return (this.healthBar = EntityUtils.createHealthBar(this));
    }
    
    /*
     * Server joins
     */
    
    public @Nullable Long getFirstJoinAt() {
        return this.firstJoinedAt;
    }
    public void updateFirstJoin() {
        this.firstJoinedAt = Calendar.getInstance().getTimeInMillis();
    }
    
    public @Nullable Long getLastJoinAt() {
        return this.lastJoinedAt;
    }
    public void updateLastJoin() {
        this.lastJoinedAt = Calendar.getInstance().getTimeInMillis();
    }
    
    /*
     * Warping
     */
    
    @Override
    public void setWarp(@NotNull WarpUtils.Warp warp) {
        this.warps.put(warp.name, warp);
    }
    @Override
    public void delWarp(@NotNull WarpUtils.Warp warp) {
        this.delWarp(warp.name);
    }
    @Override
    public void delWarp(@NotNull String name) {
        this.warps.remove(name);
    }
    @Override
    public @NotNull Map<String, WarpUtils.Warp> getWarps() {
        return this.warps;
    }
    
    /*
     * Mods
     */
    
    @Override
    public void setModded(Collection<String> modules) {
        this.clientModules.addAll(modules);
    }
    
    @Override
    public Collection<String> getModules() {
        return this.clientModules;
    }
    
    /*
     * Portal locations
     */
    
    public void setNetherPortal(@Nullable BlockPos portalPos) {
        this.theNetherPortal = portalPos;
    }
    public void setOverworldPortal(@Nullable BlockPos portalPos) {
        this.overworldPortal = portalPos;
    }
    public @Nullable BlockPos getNetherPortal() {
        return this.theNetherPortal;
    }
    public @Nullable BlockPos getOverworldPortal() {
        return this.overworldPortal;
    }
    
    @Inject(at = @At("HEAD"), method = "moveToWorld")
    public void onChangeDimension(@NotNull ServerWorld world, @NotNull final CallbackInfoReturnable<Entity> callback) {
        RegistryKey<World> dimension = world.getRegistryKey();
        
        if (SewConfig.get(SewCoreConfig.OVERWORLD_PORTAL_LOC) && dimension.equals(World.OVERWORLD)) this.setNetherPortal(this.getBlockPos());
        else if (SewConfig.get(SewCoreConfig.NETHER_PORTAL_LOC) && dimension.equals(World.NETHER)) this.setOverworldPortal(this.getBlockPos());
        
        // Clear all players on the healthbar when changing worlds
        if (this.healthBar != null)
            this.healthBar.clearPlayers();
    }
    
    /*
     * Player Sleeping Events
     */
    
    @Inject(at = @At("RETURN"), method = "trySleep")
    public void onBedEntered(final BlockPos blockPos, @NotNull final CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> callback) {
        // If player is unable to sleep, ignore
        if ( !this.isSleeping() )
            return;
        
        SleepUtils.entityBedToggle(this, this.isSleeping());
    }
    
    @Inject(at = @At("HEAD"), method = "setSpawnPoint", cancellable = true)
    public void onSetSpawnPoint(@NotNull final RegistryKey<World> dimension, @Nullable final BlockPos blockPos, final float angle, final boolean overrideGlobal, final boolean showPlayerMessage, CallbackInfo callback) {
        ServerPlayerEntity player = ((ServerPlayerEntity)(LivingEntity) this);
        
        // If the bed the player slept in is different 
        if (dimension.equals(World.OVERWORLD) && (!overrideGlobal) && (!blockPos.equals(player.getSpawnPointPosition()))) {
            
            // Verify that the player wants to update their spawn
            if ((!PlayerSpawnCommand.commandRanUUIDs.remove(player.getUuid())) && (player.getSpawnPointPosition() != null)) {
                // Let the player move their spawn
                player.sendMessage( Text.literal("You have an existing bed spawnpoint. To move your spawnpoint to this bed ").formatted(Formatting.YELLOW)
                    .append( Text.literal( "click here" )
                        // Format clickable
                        .formatted(Formatting.RED, Formatting.BOLD)
                        
                        // Add the click command
                        .styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + PlayerSpawnCommand.COMMAND_NAME.toLowerCase() + " " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ() )))
                    ).append( "." )
                );
                
                callback.cancel();
                return;
            }
            
            CoreMod.logInfo("Player " + this.getName().getString() + " spawn updated to X " + blockPos.getX() + ", Z " + blockPos.getZ() + ", Y " + blockPos.getY());
        }
    }
    
    @Inject(at = @At("RETURN"), method = "wakeUp")
    public void onBedEjected(final boolean sleepTimer, final boolean leftBed, final CallbackInfo callback) {
        // If player woke up naturally, ignore.
        if ( !leftBed )
            return;
        
        // Announce bed left
        SleepUtils.entityBedToggle(this, !leftBed);
    }
    
    /*
     * Connected players language
     */
    
    @Override
    public Locale getClientLanguage() {
        return this.clientLanguage;
    }
    @Inject(at = @At("HEAD"), method = "setClientSettings")
    private void onClientSettings(ClientSettingsC2SPacket packet, CallbackInfo callback) {
        this.clientLanguage = Locale.forLanguageTag(packet.language());
    }
    
    /*
     * Player Warp Point Events
     */
    
    @Inject(at = @At("TAIL"), method = "writeCustomDataToNbt")
    public void onSavingData(@NotNull NbtCompound tag, CallbackInfo callback) {
        // Save the player warp location for restarts
        tag.put("playerWarps", WarpUtils.toNBT(this.warps));
        
        // Store the first joined-at time
        if ( this.firstJoinedAt != null )
            tag.putLong("FirstJoinedAtTime", this.firstJoinedAt);
        
        // Store the last joined-at time
        if ( this.lastJoinedAt != null )
            tag.putLong("LastJoinedAtTime", this.lastJoinedAt);
        
        // Store where the player entered the nether at
        if ( this.overworldPortal != null )
            tag.put("LastPortalOverworld", NbtUtils.blockPosToTag(this.overworldPortal));
        
        // Store where the player exited the nether at
        if ( this.theNetherPortal != null )
            tag.put("LastPortalNether", NbtUtils.blockPosToTag(this.theNetherPortal));
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromNbt")
    public void onReadingData(@NotNull NbtCompound tag, CallbackInfo callback) {
        this.warps.putAll(WarpUtils.fromNBT(tag));
        
        // Get when first joined
        if (tag.contains("FirstJoinedAtTime", NbtElement.LONG_TYPE))
            this.firstJoinedAt = tag.getLong("FirstJoinedAtTime");
        
        // Get when last joined
        if (tag.contains("LastJoinedAtTime", NbtElement.LONG_TYPE))
            this.lastJoinedAt = tag.getLong("LastJoinedAtTime");
        
        // Get the entered overworld portal
        if (tag.contains("LastPortalOverworld", NbtElement.COMPOUND_TYPE))
            this.overworldPortal = NbtUtils.tagToBlockPos(tag.getCompound("LastPortalOverworld"));
        
        // Get the entered nether portal
        if (tag.contains("LastPortalNether", NbtElement.COMPOUND_TYPE))
            this.theNetherPortal = NbtUtils.tagToBlockPos(tag.getCompound("LastPortalNether"));
    }
    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void onCopyData(@NotNull ServerPlayerEntity player, boolean alive, CallbackInfo callback) {
        // Copy the users modules
        this.clientModules.addAll(((ModUser) player).getModules());
        
        // Copy the players warp over
        this.warps.putAll(((PlayerData) player).getWarps());
        
        // Copy the players balance
        this.dataTracker.set(MoneyHolder.MONEY, player.getDataTracker().get(MoneyHolder.MONEY));
        
        // Keep the portal location cross-dimension (Necessary!)
        this.setOverworldPortal(((PlayerData) player).getOverworldPortal());
        this.setNetherPortal(((PlayerData) player).getNetherPortal());
        
        // Keep when the player joined across deaths
        this.firstJoinedAt = ((PlayerData) player).getFirstJoinAt();
        this.lastJoinedAt = ((PlayerData) player).getLastJoinAt();
        
        // Compass directions
        this.setCompassDirection(((PlayerData) player).getCompass());
        
        // Copy the backpack inventory
        ((BackpackCarrier) this).setBackpack(
            ((BackpackCarrier)player).getBackpack(),
            false // Don't resend here because the ClientPlayerEntity hasn't been updated yet on the client
        );
    }
    
    /*
     * Temporary Ruler information (Never saves)
     */
    
    private BlockPos rulerA = null;
    private BlockPos rulerB = null;
    
    public void setRulerA(@Nullable BlockPos blockPos) {
        this.rulerA = blockPos;
    }
    public void setRulerB(@Nullable BlockPos blockPos) {
        this.rulerB = blockPos;
    }
    public @Nullable BlockPos getRulerA() {
        return this.rulerA;
    }
    public @Nullable BlockPos getRulerB() {
        return this.rulerB;
    }
    
    /*
     * Compasses
     */
    
    @Override
    public Pair<Text, BlockPos> cycleCompass() {
        CompassDirections next = this.compassDirection;
        Pair<Text, BlockPos> pos;
        do {
            next = next.getNext((ServerPlayerEntity)(PlayerEntity)this, this.compassFocal);
        } while ((pos = next.getPos((ServerPlayerEntity)(PlayerEntity)this, this.compassFocal)) == null);
        
        this.setCompassDirection(next, pos);
        return pos;
    }
    public CompassDirections getCompass() {
        return this.compassDirection;
    }
    public void setCompassDirection(@NotNull CompassDirections direction) {
        Pair<Text, BlockPos> marker = direction.getPos((ServerPlayerEntity)(PlayerEntity) this, BlockPos.ORIGIN);
        if (marker == null) this.cycleCompass();
        else this.setCompassDirection(direction, marker);
    }
    public void setCompassDirection(@NotNull CompassDirections direction, @NotNull Pair<Text, BlockPos> marker) {
        this.compassDirection = direction;
        this.compassFocal = marker.getRight();
        this.networkHandler.sendPacket(new PlayerSpawnPositionS2CPacket(this.compassFocal, 0.0F));
    }
    
    /*
     * Path finding to friends
     */
    
    private static final int NODE_TICKS = 30;
    private static final int NAV_DISTANCE = 30;
    
    private int sewTrailTicks = ServerPlayerEntityMixin.NODE_TICKS;
    private BlockPos sewTrailTargetPos = null;
    private MobEntity sewTrailInnerDemon = null;
    
    private final PathNodeMaker nodeMaker = new LandPathNodeMaker();
    private final PathNodeNavigator nodeNavigator = this.createPathNodeNavigator(ServerPlayerEntityMixin.NAV_DISTANCE);
    
    private @NotNull PathNodeNavigator createPathNodeNavigator(int range) {
        this.nodeMaker.setCanEnterOpenDoors(true);
        this.nodeMaker.setCanOpenDoors(true);
        this.nodeMaker.setCanSwim(true);
        
        return new PathNodeNavigator(this.nodeMaker, range);
    }
    @Override
    public @NotNull PathNodeNavigator getPathNodeNavigator() {
        return this.nodeNavigator;
    }
    @Override
    public @Nullable Path findPathToAny(@NotNull Set<BlockPos> positions, int range, boolean bl, int distance) {
        // If there is no path nodes
        if (positions.isEmpty())
            return null;
        
        // If the player is not within the world height
        if (this.getServerWorld().isOutOfHeightLimit(this.getBlockY()))
            return null;
        
        // Push the profiler
        this.getServerWorld()
            .getProfiler()
            .push("pathfind");
        
        if (this.sewTrailInnerDemon == null)
            this.sewTrailInnerDemon = new ZombieEntity(this.getServerWorld());
        this.sewTrailInnerDemon.setPos(this.getX(), this.getY(), this.getZ());
        
        float followRange = (float)this.sewTrailInnerDemon.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
        BlockPos selfPos = bl ? this.getBlockPos().up() : this.getBlockPos();
        int i = (int)(followRange + range);
        ChunkCache chunkCache = new ChunkCache(this.getServerWorld(), selfPos.add(-i, -i, -i), selfPos.add(i, i, i));
        Path path = this.getPathNodeNavigator()
            .findPathToAny(chunkCache, this.sewTrailInnerDemon, positions, followRange, distance, 1.0F);
        
        // Pop the profiler
        this.getServerWorld()
            .getProfiler()
            .pop();
        
        if (path != null && path.getTarget() != null)
            this.sewTrailTargetPos = path.getTarget();
        return path;
    }
}
