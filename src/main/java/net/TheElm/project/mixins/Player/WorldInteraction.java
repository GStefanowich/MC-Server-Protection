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
import net.TheElm.project.interfaces.Nicknamable;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.PlayerServerLanguage;
import net.TheElm.project.utilities.SleepUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class WorldInteraction extends PlayerEntity implements PlayerData, PlayerServerLanguage, Nicknamable {

    @Shadow private String clientLanguage;
    
    private BlockPos warpPos = null;
    private Text playerNickname = null;
    
    public WorldInteraction(World world_1, GameProfile gameProfile_1) {
        super(world_1, gameProfile_1);
    }
    
    @Nullable @Override
    public BlockPos getWarpPos() {
        return this.warpPos;
    }
    @Override
    public void setWarpPos(@Nullable BlockPos blockPos) {
        this.warpPos = blockPos;
    }
    
    /*
     * Player Sleeping Events
     */
    @Inject(at = @At("RETURN"), method = "trySleep")
    public void onBedEntered(final BlockPos blockPos, final CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> callback) {
        if (!SewingMachineConfig.INSTANCE.DO_SLEEP_VOTE.get())
            return;
        
        //System.out.println( "Try-Sleep" );
        
        // If player is unable to sleep, ignore
        if ( !this.isSleeping() )
            return;
        
        SleepUtils.entityBedToggle( this, this.isSleeping(), false );
    }
    
    @Override
    public void setPlayerSpawn(final BlockPos blockPos, final boolean overrideGlobal) {
        // If the spawn location is forced, ignore (Not a bed set)
        if (overrideGlobal)
            return;
        
        ServerPlayerEntity player = ((ServerPlayerEntity)(LivingEntity) this);
        // If the bed the player slept in is different 
        if ( !blockPos.equals(player.getSpawnPosition())) {
            // Don't show the command message again if the player clicked it
            if ( PlayerSpawnCommand.commandRanUUIDs.remove( player.getUuid() ) )
                return;
            
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
            
            CoreMod.logMessage("Player " + this.getName().asString() + " spawn updated to X " + blockPos.getX() + ", Z " + blockPos.getZ() + ", Y " + blockPos.getY());
        }
        super.setPlayerSpawn(blockPos, overrideGlobal);
    }
    
    @Inject(at = @At("RETURN"), method = "wakeUp")
    public void onBedEjected(final boolean sleepTimer, final boolean leftBed, final boolean updateSpawn, final CallbackInfo callback) {
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
    public String getClientLanguage() {
        return this.clientLanguage;
    }
    
    /*
     * Player Warp Point Events
     */
    @Inject(at = @At("TAIL"), method = "writeCustomDataToTag")
    public void onSavingData(CompoundTag tag, CallbackInfo callback) {
        // Save the player warp location for restarts
        if ( this.warpPos != null ) {
            tag.putInt("playerWarpX", this.warpPos.getX() );
            tag.putInt("playerWarpY", this.warpPos.getY() );
            tag.putInt("playerWarpZ", this.warpPos.getZ() );
        }
        if ( this.playerNickname != null ) {
            tag.putString( "PlayerNickname", Text.Serializer.toJson( this.playerNickname ));
        }
    }
    @Inject(at = @At("TAIL"), method = "readCustomDataFromTag")
    public void onReadingData(CompoundTag tag, CallbackInfo callback) {
        // Read the player warp location after restarting
        if ( tag.containsKey( "playerWarpX" ) && tag.containsKey( "playerWarpY" ) && tag.containsKey( "playerWarpZ" ) ) {
            this.warpPos = new BlockPos(
                tag.getInt("playerWarpX"),
                tag.getInt("playerWarpY"),
                tag.getInt("playerWarpZ")
            );
        }
        if (tag.containsKey("PlayerNickname", 8)) {
            this.playerNickname = Text.Serializer.fromJson( tag.getString("PlayerNickname") );
        }
    }
    @Inject(at = @At("TAIL"), method = "copyFrom")
    public void onCopyData(ServerPlayerEntity player, boolean alive, CallbackInfo callback) {
        // Copy the players warp over
        this.warpPos = ((PlayerData) player).getWarpPos();
        // Copy the players nick over
        this.playerNickname = ((Nicknamable) player).getPlayerNickname();
    }
    
    /*
     * Player names
     */
    @Override
    public void setPlayerNickname(@Nullable Text nickname) {
        this.playerNickname = nickname;
    }
    @Nullable @Override
    public Text getPlayerNickname() {
        return this.playerNickname;
    }
    
    @Inject(at = @At("HEAD"), method = "method_14206", cancellable = true)
    public void getServerlistDisplayName(CallbackInfoReturnable<Text> callback) {
        callback.setReturnValue( ((Nicknamable) this).getPlayerNickname() );
    }
}
