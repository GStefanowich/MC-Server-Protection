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

package net.TheElm.project.mixins.Player;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.TheElm.project.CoreMod;
import net.TheElm.project.commands.PlayerSpawnCommand;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.enums.CompassDirections;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.MoneyHolder;
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.PlayerPermissions;
import net.TheElm.project.interfaces.PlayerServerLanguage;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.ranks.PlayerRank;
import net.TheElm.project.utilities.NbtUtils;
import net.TheElm.project.utilities.RankUtils;
import net.TheElm.project.utilities.SleepUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class WorldInteraction extends PlayerEntity implements PlayerData, PlayerPermissions, PlayerServerLanguage, Nicknamable, PlayerChat {
    
    @Shadow public ServerPlayerInteractionManager interactionManager;
    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Shadow private String clientLanguage;
    @Shadow private boolean notInAnyWorld;
    
    // Health bar
    private int healthTick = 0;
    @Nullable
    private ServerBossBar healthBar = null;
    
    // Join times
    private Long firstJoinedAt = null;
    private Long lastJoinedAt = null;
    
    // Warps
    private Integer warpDimension = null;
    private BlockPos warpPos = null;
    
    // Nickname
    private Text playerNickname = null;
    
    // Portal locations
    private BlockPos overworldPortal = null;
    private BlockPos theNetherPortal = null;
    
    // Compass
    private CompassDirections compassDirections = CompassDirections.SPAWN;
    
    public WorldInteraction(World world_1, GameProfile gameProfile_1) {
        super(world_1, gameProfile_1);
    }
    
    @Inject(at = @At("RETURN"), method = "<init>*")
    public void onInitialize(CallbackInfo callback) {
        // Set the player to "adventure"-like if they are not allowed to modify the world
        if (!RankUtils.hasPermission(this, Permissions.INTERACT_WORLD)) {
            this.abilities.allowModifyWorld = false;
            this.abilities.invulnerable = true;
            this.sendAbilitiesUpdate();
        }
    }
    
    /*
     * Health Bar
     */
    @Inject(at = @At("TAIL"), method = "tick")
    public void onTick(CallbackInfo callback) {
        if ((this.healthBar != null) && (!this.notInAnyWorld) && ((++this.healthTick) >= 60)) {
            // Get players from the health bar
            Set<ServerPlayerEntity> enemies = new HashSet<>(this.healthBar.getPlayers());
            
            // Remove players from the healthbar
            if (!enemies.isEmpty()) {
                // Get the area around the player
                Box searchRegion = new Box(
                    new BlockPos(this.getX() + 20, this.getY() + 10, this.getZ() + 20),
                    new BlockPos(this.getX() - 20, this.getY() - 10, this.getZ() - 20)
                );
                
                // Get local players
                if (!this.isAlive())
                    this.healthBar.clearPlayers();
                else {
                    List<ServerPlayerEntity> players = this.world.getEntities(ServerPlayerEntity.class, searchRegion, (nearby) -> (!nearby.getUuid().equals(this.getUuid())));
                    
                    // Remove all locale players
                    enemies.removeAll(players);
                    
                    // Remove if not nearby
                    if (!enemies.isEmpty()) {
                        for (ServerPlayerEntity enemy : enemies) this.healthBar.removePlayer(enemy);
                    }
                }
                
                // Set the health percentage
                this.updateHealthBar();
            }
            
            // Reset the tick
            this.healthTick = 0;
        }
    }
    @Inject(at = @At("TAIL"), method = "onDeath")
    public void onDeath(DamageSource damageSource, CallbackInfo callback) {
        if (this.healthBar != null) {
            this.healthBar.setPercent(0.0f);
            this.healthBar.clearPlayers();
        }
    }
    @Inject(at = @At("HEAD"), method = "shouldDamagePlayer", cancellable = true)
    public void shouldDamage(PlayerEntity entity, CallbackInfoReturnable<Boolean> callback) {
        IClaimedChunk chunk = (IClaimedChunk) this.world.getWorldChunk(this.getBlockPos());
        
        // If player hurt themselves
        if ( this == entity )
            return;
        
        // If PvP is off, disallow
        if ( !chunk.isSetting(this.getBlockPos(), ClaimSettings.PLAYER_COMBAT) )
            callback.setReturnValue(false);
    }
    @Inject(at = @At("RETURN"), method = "damage")
    public void onDamage(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> callback) {
        if ((!this.world.isClient)) {
            // Set the health percentage
            this.updateHealthBar();
            
            // Add the attacker to the healthbar
            if ((damageSource.getAttacker() instanceof PlayerEntity) && (damageSource.getAttacker() != this))
                this.getHealthBar().addPlayer((ServerPlayerEntity) damageSource.getAttacker());
        }
    }
    private void updateHealthBar() {
        // Get the health percentage
        float percentage = this.getHealth() / this.getMaximumHealth();
        
        if (percentage != this.getHealthBar().getPercent()) {
            // Update the bar
            this.getHealthBar().setPercent( percentage );
            
            // Update the color of the bar
            this.getHealthBar().setColor(
                percentage >= 0.6 ? BossBar.Color.GREEN
                    : ( percentage >= 0.3 ? BossBar.Color.YELLOW
                        : BossBar.Color.RED
                    )
            );
        }
    }
    @NotNull
    public ServerBossBar getHealthBar() {
        if (this.healthBar != null)
            return this.healthBar;
        // Create the health bar
        return (this.healthBar = new ServerBossBar(
            new LiteralText("Player ").append(this.getDisplayName().formatted(Formatting.AQUA)).formatted(Formatting.WHITE),
            BossBar.Color.RED,
            BossBar.Style.NOTCHED_10
        ));
    }
    
    /*
     * Claims
     */
    
    @Override
    public ClaimantPlayer getClaim() {
        return ((PlayerData)this.networkHandler).getClaim();
    }
    
    /*
     * Ranks
     */
    
    @Override
    public PlayerRank[] getRanks() {
        return ((PlayerPermissions)this.interactionManager).getRanks();
    }
    @Override
    public void resetRanks() {
        ((PlayerPermissions)this.interactionManager).resetRanks();
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
    
    public @Nullable Integer getWarpDimensionId() {
        if (this.getWarpPos() == null)
            return null;
        if (this.warpDimension == null)
            this.warpDimension = 0;
        return this.warpDimension;
    }
    @Override
    public @Nullable ServerWorld getWarpWorld() {
        return this.getServer().getWorld(DimensionType.byRawId( this.warpDimension ));
    }
    @Override
    public @Nullable BlockPos getWarpPos() {
        return this.warpPos;
    }
    @Override
    public void setWarpPos(@Nullable BlockPos blockPos) {
        if (blockPos == null)
            this.warpDimension = null;
        this.warpPos = blockPos;
    }
    @Override
    public void setWarpDimension(@NotNull World world) {
        this.warpDimension = world.dimension.getType().getRawId();
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
    @Nullable
    public BlockPos getNetherPortal() {
        return this.theNetherPortal;
    }
    @Nullable
    public BlockPos getOverworldPortal() {
        return this.overworldPortal;
    }
    
    @Inject(at = @At("HEAD"), method = "changeDimension")
    public void onChangeDimension(DimensionType dimensionType, CallbackInfoReturnable<Entity> callback) {
        if (SewingMachineConfig.INSTANCE.OVERWORLD_PORTAL_LOC.get() && dimensionType == DimensionType.OVERWORLD) this.setNetherPortal(this.getBlockPos());
        else if (SewingMachineConfig.INSTANCE.NETHER_PORTAL_LOC.get() && dimensionType == DimensionType.THE_NETHER) this.setOverworldPortal(this.getBlockPos());
    }
    
    /*
     * Player Sleeping Events
     */
    
    @Inject(at = @At("RETURN"), method = "trySleep")
    public void onBedEntered(final BlockPos blockPos, final CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> callback) {
        if (!SewingMachineConfig.INSTANCE.DO_SLEEP_VOTE.get())
            return;
        
        // If player is unable to sleep, ignore
        if ( !this.isSleeping() )
            return;
        
        SleepUtils.entityBedToggle( this, this.isSleeping(), false );
    }
    
    @Override
    public void setPlayerSpawn(final BlockPos blockPos, final boolean overrideGlobal, final boolean showPlayerMessage) {
        ServerPlayerEntity player = ((ServerPlayerEntity)(LivingEntity) this);
        // If the bed the player slept in is different 
        if ((!overrideGlobal) && (!blockPos.equals(player.getSpawnPosition()))) {
            // Don't show the command message again if the player clicked it
            if ( PlayerSpawnCommand.commandRanUUIDs.remove( player.getUuid() ) ) {
                super.setPlayerSpawn(blockPos, overrideGlobal, showPlayerMessage);
                return;
            }
            
            if (player.getSpawnPosition() != null) {
                // Let the player move their spawn
                player.sendMessage( new LiteralText( "You have an existing bed spawnpoint. To move your spawnpoint to this bed " ).formatted(Formatting.YELLOW)
                    .append( new LiteralText( "click here" )
                        // Format clickable
                        .formatted(Formatting.RED, Formatting.BOLD)
                        // Add the click command
                        .styled((style) -> style.setClickEvent(new ClickEvent( ClickEvent.Action.RUN_COMMAND, "/" + PlayerSpawnCommand.commandName + " " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ() )))
                    )
                    .append( "." )
                );
                
                return;
            }
            
            CoreMod.logInfo("Player " + this.getName().asString() + " spawn updated to X " + blockPos.getX() + ", Z " + blockPos.getZ() + ", Y " + blockPos.getY());
        }
        super.setPlayerSpawn(blockPos, overrideGlobal, showPlayerMessage);
    }
    
    @Inject(at = @At("RETURN"), method = "wakeUp")
    public void onBedEjected(final boolean sleepTimer, final boolean leftBed, final CallbackInfo callback) {
        if (!SewingMachineConfig.INSTANCE.DO_SLEEP_VOTE.get())
            return;
        
        // If player woke up naturally, ignore.
        if ( !leftBed )
            return;
        
        // Announce bed left
        SleepUtils.entityBedToggle( this, !leftBed, false );
    }
    
    /*
     * Connected players language
     */
    public Locale getClientLanguage() {
        return Locale.forLanguageTag( this.clientLanguage );
    }
    
    /*
     * Player Warp Point Events
     */
    @Inject(at = @At("TAIL"), method = "writeCustomDataToTag")
    public void onSavingData(CompoundTag tag, CallbackInfo callback) {
        // Save the player warp location for restarts
        if (( this.warpPos != null ) && ( this.getWarpDimensionId() != null )) {
            tag.putInt("playerWarpX", this.warpPos.getX());
            tag.putInt("playerWarpY", this.warpPos.getY());
            tag.putInt("playerWarpZ", this.warpPos.getZ());
            tag.putInt("playerWarpD", this.getWarpDimensionId());
        }
        
        // Store the players nickname
        if ( this.playerNickname != null )
            tag.putString("PlayerNickname", Text.Serializer.toJson(this.playerNickname));
        
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
        
        tag.putBoolean("chatMuted", this.isMuted());
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromTag")
    public void onReadingData(CompoundTag tag, CallbackInfo callback) {
        // Read the player warp location after restarting
        if ( tag.contains( "playerWarpX" ) && tag.contains( "playerWarpY" ) && tag.contains( "playerWarpZ" ) ) {
            this.warpPos = new BlockPos(
                tag.getInt("playerWarpX"),
                tag.getInt("playerWarpY"),
                tag.getInt("playerWarpZ")
            );
            if ( tag.contains( "playerWarpD" ) )
                this.warpDimension = tag.getInt("playerWarpD");
        }
        
        // Get the nickname
        if (tag.contains("PlayerNickname", NbtType.STRING))
            this.playerNickname = Text.Serializer.fromJson(tag.getString("PlayerNickname"));
        
        // Get when first joined
        if (tag.contains("FirstJoinedAtTime", NbtType.LONG))
            this.firstJoinedAt = tag.getLong("FirstJoinedAtTime");
        
        // Get when last joined
        if (tag.contains("LastJoinedAtTime", NbtType.LONG))
            this.lastJoinedAt = tag.getLong("LastJoinedAtTime");
        
        // Get the entered overworld portal
        if (tag.contains("LastPortalOverworld", NbtType.COMPOUND))
            this.overworldPortal = NbtUtils.tagToBlockPos(tag.getCompound("LastPortalOverworld"));
        
        // Get the entered nether portal
        if (tag.contains("LastPortalNether", NbtType.COMPOUND))
            this.theNetherPortal = NbtUtils.tagToBlockPos(tag.getCompound("LastPortalNether"));
        
        // Read if player is muted
        if (tag.contains("chatMuted", NbtType.BYTE))
            this.toggleMute(tag.getBoolean("chatMuted"));
    }
    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void onCopyData(ServerPlayerEntity player, boolean alive, CallbackInfo callback) {
        // Copy the players warp over
        this.warpPos = ((PlayerData) player).getWarpPos();
        this.warpDimension = ((PlayerData) player).getWarpDimensionId();
        
        // Copy the players nick over
        this.playerNickname = ((Nicknamable) player).getPlayerNickname();
        
        // Copy the players balance
        this.dataTracker.set(MoneyHolder.MONEY, player.getDataTracker().get(MoneyHolder.MONEY));
        
        // Keep the chat room cross-dimension
        this.setChatRoom(((PlayerChat) player).getChatRoom());
        
        // Keep the portal location cross-dimension (Necessary!)
        this.setOverworldPortal(((PlayerData) player).getOverworldPortal());
        this.setNetherPortal(((PlayerData) player).getNetherPortal());
        
        // Keep when the player joined across deaths
        this.firstJoinedAt = ((PlayerData) player).getFirstJoinAt();
        this.lastJoinedAt = ((PlayerData) player).getLastJoinAt();
        
        // Compass directions
        this.setCompassDirection(((PlayerData) player).getCompass());
        
        // Copy the backpack inventory
        ((BackpackCarrier)this).setBackpack(
            ((BackpackCarrier)player).getBackpack()
        );
    }
    
    /*
     * Player names (Saved and handled cross dimension)
     */
    @Override
    public void setPlayerNickname(@Nullable Text nickname) {
        this.playerNickname = nickname;
    }
    @Nullable @Override
    public Text getPlayerNickname() {
        if (this.playerNickname == null)
            return null;
        return this.playerNickname.deepCopy();
    }
    @Inject(at = @At("HEAD"), method = "method_14206", cancellable = true)
    public void getServerlistDisplayName(CallbackInfoReturnable<Text> callback) {
        callback.setReturnValue(((Nicknamable) this).getPlayerNickname());
    }
    
    /*
     * Chat Rooms (Handled cross dimension)
     */
    @Override @NotNull
    public ChatRooms getChatRoom() {
        return ((PlayerChat)this.interactionManager).getChatRoom();
    }
    @Override
    public void setChatRoom(@NotNull ChatRooms room) {
        ((PlayerChat)this.interactionManager).setChatRoom( room );
    }
    
    @Override
    public boolean toggleMute() {
        return ((PlayerChat)this.interactionManager).toggleMute();
    }
    @Override
    public boolean toggleMute(boolean muted) {
        return ((PlayerChat)this.interactionManager).toggleMute(muted);
    }
    @Override
    public boolean toggleMute(GameProfile player) {
        return ((PlayerChat)this.interactionManager).toggleMute( player );
    }
    @Override
    public boolean isMuted() {
        return ((PlayerChat)this.interactionManager).isMuted();
    }
    @Override
    public boolean isMuted(GameProfile player) {
        return ((PlayerChat)this.interactionManager).isMuted( player );
    }
    
    /*
     * Temporary Ruler information (Never saves)
     */
    BlockPos rulerA = null;
    BlockPos rulerB = null;
    
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
    public CompassDirections cycleCompass() {
        CompassDirections next = this.compassDirections;
        BlockPos pos;
        do {
            next = next.getNext();
        } while ((pos = next.getPos( (ServerPlayerEntity)(PlayerEntity)this )) == null);
        
        this.setCompassDirection( next, pos );
        return next;
    }
    public CompassDirections getCompass() {
        return this.compassDirections;
    }
    public void setCompassDirection(@NotNull CompassDirections direction) {
        BlockPos blockPos = direction.getPos( (ServerPlayerEntity)(PlayerEntity) this );
        if (blockPos == null) this.cycleCompass();
        else this.setCompassDirection(direction, blockPos);
    }
    public void setCompassDirection(@NotNull CompassDirections direction, @NotNull BlockPos blockPos) {
        this.compassDirections = direction;
        this.networkHandler.sendPacket(new PlayerSpawnPositionS2CPacket( blockPos ));
    }
}
