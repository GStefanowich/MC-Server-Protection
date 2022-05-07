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

package net.theelm.sewingmachine.mixins.World;

import net.theelm.sewingmachine.CoreMod;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.DragonLoot;
import net.theelm.sewingmachine.interfaces.BossLootableContainer;
import net.theelm.sewingmachine.protections.BlockRange;
import net.theelm.sewingmachine.utilities.BossLootRewards;
import net.theelm.sewingmachine.utilities.ChunkUtils;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.IntUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mixin(EnderDragonFight.class)
public abstract class DragonFight {
    
    private final Collection<UUID> seenPlayers = new HashSet<>();
    
    private boolean sewingMachineCheckDragon = false;
    private boolean sewingMachineDragonShouldExist = false;
    
    /*
     * Dragon Info
     */
    
    @Shadow private ServerWorld world;
    @Shadow private ServerBossBar bossBar;
    @Shadow private UUID dragonUuid;
    @Shadow private boolean dragonKilled;
    @Shadow private boolean previouslyKilled;
    
    /*
     * Players in The End
     */
    
    @Shadow private int playerUpdateTimer;
    private int sewingMachineEndPlayers = 0;
    @Shadow private native void updatePlayers();
    
    /*
     * Async Checks for Dragons/Portals
     */
    
    @Shadow private boolean doLegacyCheck;
    private boolean sewingMachineDoPortalCheck = true;
    private boolean sewingMachinePortalCleanup = true;
    @Shadow private native void convertFromLegacy();
    private void convertFromLegacy(boolean createPortal) {
        this.previouslyKilled = this.worldContainsEndPortal();
        if ((!this.previouslyKilled) && createPortal && (this.findEndPortal() == null)) {
            this.generateEndPortal(false);
        }
        
        List<? extends EnderDragonEntity> dragons;
        this.dragonKilled = (dragons = this.world.getAliveEnderDragons()).isEmpty();
        if (!this.dragonKilled) {
            EnderDragonEntity dragon = dragons.get(0);
            this.dragonUuid = dragon.getUuid();
            if (!this.previouslyKilled) {
                dragon.discard();
                this.dragonUuid = null;
            }
        }
        
        if (!this.previouslyKilled && this.dragonKilled) {
            this.dragonKilled = false;
        }
    }
    @Shadow private native void generateEndPortal(boolean boolean_1);
    @Shadow private native void generateNewEndGateway();
    @Shadow private native BlockPattern.Result findEndPortal();
    @Shadow private native boolean worldContainsEndPortal();
    @Shadow private native boolean loadChunks();
    
    /*
     * Methods
     */
    
    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    public void spawnStateChange(CallbackInfo callback) {
        int players = this.bossBar.getPlayers().size();
        
        // Check if a dragon already exists
        if ( !this.sewingMachineCheckDragon ) {
            this.sewingMachineDragonShouldExist = !this.world.getAliveEnderDragons().isEmpty();
            this.sewingMachineCheckDragon = true;
        }
        
        // Attempt to set the ability to spawn to TRUE
        if ( !this.sewingMachineDragonShouldExist )
            this.sewingMachineDragonShouldExist = ( players >= SewConfig.get(SewConfig.DRAGON_PLAYERS) );
        
        // If the ability to spawn is FALSE
        if ( !this.sewingMachineDragonShouldExist ) {
            // Mimic the primary tick function
            if ((++this.playerUpdateTimer) >= 20) {
                this.updatePlayers();
                this.playerUpdateTimer = 0;
            }
            
            // Update the players in the end
            if ( players != this.sewingMachineEndPlayers ) {
                CoreMod.logDebug("Changed players that are in The End");
                
                NumberFormat formatter = NumberFormat.getInstance();
                
                // Set health bar to not visible
                this.bossBar.setVisible( false );
                
                // Show notice to players in The End
                if ( !this.dragonKilled )
                    TitleUtils.showPlayerAlert( this.world, new LiteralText( formatter.format(players) + " / " + formatter.format(SewConfig.get(SewConfig.DRAGON_PLAYERS)) + " required players to fight the dragon." ));
                
                this.sewingMachineEndPlayers = players;
            }
            
            // Make sure the necessities exist
            if ((!this.bossBar.getPlayers().isEmpty()) && this.loadChunks()) {
                // Do the legacy dragon check
                if ( this.doLegacyCheck ) {
                    this.convertFromLegacy( false );
                    this.doLegacyCheck = false;
                }
                
                // Check that we have an open end portal
                if ( this.sewingMachineDoPortalCheck ) {
                    CoreMod.logDebug("Doing the Portal Check");
                    
                    // If an end portal doesn't already exist
                    if ((!this.worldContainsEndPortal()) && (this.findEndPortal() == null)) {
                        CoreMod.logDebug("Creating a new End Portal");
                        
                        // Create an OPEN portal
                        this.generateEndPortal(true);
                        
                        // Generate a single end gateway
                        this.generateNewEndGateway();
                    }
                    
                    // Portal has been checked
                    this.sewingMachineDoPortalCheck = false;
                }
            }
            
            // Cancel the tick that allows the dragon to spawn
            callback.cancel();
        }
        else if (this.sewingMachinePortalCleanup && this.loadChunks()) {
            // Close any open end portal
            this.generateEndPortal(false);
            
            // Cleanup has completed
            this.sewingMachinePortalCleanup = false;
            
            // Delay the continuation
            this.playerUpdateTimer = -100; // Set our tick count to -100 (6 seconds til dragon spawn)
            this.bossBar.clearPlayers();
            callback.cancel();
        }
    }
    
    @Inject(at = @At("RETURN"), method = "createDragon")
    public void dragonCreation(@NotNull CallbackInfoReturnable<EnderDragonEntity> callback) {
        EnderDragonEntity dragon = callback.getReturnValue();
        if (dragon != null)
            MessageUtils.sendToAll(new LiteralText("An Ender Dragon has arrived in ")
                .append(new LiteralText("The End").formatted(Formatting.GRAY))
                .append("! Visit to join in on the fight.")
                .formatted(Formatting.YELLOW, Formatting.ITALIC), EntityUtils::isNotFightingDragon);
    }
    
    @Inject(at = @At("TAIL"), method = "dragonKilled")
    public void dragonDestroyed(@NotNull EnderDragonEntity dragon, @NotNull CallbackInfo callback) {
        if (this.dragonUuid.equals(dragon.getUuid()))
            this.sewingMachineDragonShouldExist = false;
        final boolean giveLootReward = SewConfig.get(SewConfig.DRAGON_LOOT_END_ITEMS) || SewConfig.get(SewConfig.DRAGON_LOOT_RARE_BOOKS);
        
        // Notify players not participating in the fight
        int players = this.seenPlayers.size();
        MessageUtils.sendToAll(new LiteralText("An Ender Dragon has been eradicated by ")
            .append(new LiteralText((IntUtils.text(players)) + " player" + (players != 1 ? "s" : "")).formatted(Formatting.GOLD))
            .append("!")
            .formatted(Formatting.YELLOW, Formatting.ITALIC), EntityUtils::isNotFightingDragon);
        
        this.seenPlayers.stream()
            .map(uuid -> ServerCore.getPlayer(this.world.getServer(), uuid))
            .filter(Objects::nonNull)
            .filter((player) -> {
                // Increase the dragon kill statistic
                player.incrementStat(Stats.KILLED.getOrCreateStat(EntityType.ENDER_DRAGON));
                
                return giveLootReward && !BossLootRewards.DRAGON_LOOT.addLoot(player, DragonLoot.getReward());
            })
            .forEach(player -> player.sendSystemMessage(new LiteralText("You were not rewarded a drop from killing the Ender Dragon, your loot chest is full.")
                .formatted(Formatting.RED), Util.NIL_UUID));
        
        // Clear the list of seen players
        if ( this.world.getAliveEnderDragons().isEmpty() )
            this.seenPlayers.clear();
        
        if (giveLootReward)
            DragonFight.generateLootRewardContainer(this.world);
    }
    
    @Inject(at = @At("TAIL"), method = "updatePlayers")
    public void onUpdatePlayers(CallbackInfo callback) {
        List<? extends EnderDragonEntity> dragons = this.world.getAliveEnderDragons();
        if (!dragons.isEmpty()) {
            // Get all players
            this.bossBar.getPlayers().stream().map(Entity::getUuid)
                .forEachOrdered(this.seenPlayers::add);
            
            // Update the dragons health
            int count = Math.max(this.seenPlayers.size() - 1, 0),
                baseHealth = 200;
            float newMax = baseHealth + (count * SewConfig.get(SewConfig.DRAGON_ADDITIONAL_HEALTH));
            
            for (EnderDragonEntity dragon : dragons) {
                dragon.getMaxHealth();
                
                float currentMax = dragon.getMaxHealth();
                if (currentMax != newMax) {
                    float currentHealth = dragon.getHealth();
                    
                    dragon.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
                        .setBaseValue(newMax);
                    dragon.setHealth(currentHealth + (newMax - currentMax));
                }
            }
        }
    }
    
    @Inject(at = @At("TAIL"), method = "generateEndPortal")
    private void onGeneratePortal(boolean opened, CallbackInfo callback) {
        // Get the portal
        BlockPattern.Result portal = this.findEndPortal();
        if (portal != null) {
            // Get the positions for the portal
            /*BlockPos frontTopLeft = portal.getFrontTopLeft();
            BlockPos backBottomRight = frontTopLeft.offset(portal.getForwards(), portal.getWidth())
                .offset(Direction.WEST, portal.getWidth())
                .offset(portal.getUp().getOpposite(), portal.getHeight());
            
            // Claim the portal
            ChunkUtils.claimSlices(this.world, CoreMod.spawnID, frontTopLeft.up(2), backBottomRight);*/
        }
    }
    
    private static void generateLootRewardContainer(@NotNull ServerWorld world) {
        DragonFight.generateLootRewardContainer(world, world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, EndPortalFeature.ORIGIN));
    }
    private static void generateLootRewardContainer(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        if (DragonFight.isLootRewardContainer(world, pos.down()))
            return;
        
        world.setBlockState(pos, Blocks.BLACK_SHULKER_BOX.getDefaultState());
        
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            NbtCompound chestTag = blockEntity.createNbtWithIdentifyingData();
            chestTag.putString("BossLootContainer", BossLootRewards.DRAGON_LOOT.toString());
            blockEntity.readNbt(chestTag);
        }
        
        ChunkUtils.claimSlices(world, CoreMod.SPAWN_ID, BlockRange.of(pos));
    }
    private static boolean isLootRewardContainer(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof ShulkerBoxBlockEntity
            && ((BossLootableContainer)blockEntity).getBossLootIdentifier() != null;
    }
}
