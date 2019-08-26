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

import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.PlayerMovement;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.protections.claiming.ClaimedChunk;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.PlayerNameUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.SharedConstants;
import net.minecraft.client.network.packet.ChatMessageS2CPacket;
import net.minecraft.client.options.ChatVisibility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.ChatMessageC2SPacket;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.server.network.packet.VehicleMoveC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.sql.SQLException;
import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerInteraction implements ServerPlayPacketListener, PlayerMovement {
    
    /*
     * Shadow Methods
     */
    @Shadow public ClientConnection client;
    @Shadow private MinecraftServer server;
    @Shadow private ServerPlayerEntity player;
    @Shadow private int messageCooldown;
    
    @Shadow public native void sendPacket(Packet<?> packet);
    @Shadow public native void disconnect(Text text);
    @Shadow private native void executeCommand(String string);
    
    /*
     * Modified methods
     */
    
    @Inject(at = @At("RETURN"), method = "<init>")
    public void onPlayerConnect(MinecraftServer server, ClientConnection client, ServerPlayerEntity player, CallbackInfo callback) {
        CoreMod.PLAYER_LOCATIONS.put( player, null );
        
        // Initialize user claims from database
        ClaimantPlayer.get( player.getUuid() );
        
        if ( SewingMachineConfig.INSTANCE.DO_MONEY.get() ) {
            // Make sure the user has the default money
            try (MySQLStatement stmt = CoreMod.getSQL().prepare("INSERT IGNORE INTO `player_Data` ( `dataOwner`, `dataMoney` ) VALUES ( ?, ? );")) {
                
                stmt.addPrepared(player.getUuid())
                    .addPrepared(SewingMachineConfig.INSTANCE.STARTING_MONEY.get())
                    .executeUpdate();
                
            } catch (SQLException e) {
                CoreMod.logError(e);

            }
        }
    }
    
    @Inject(at = @At("TAIL"), method = "onPlayerMove")
    public void onPlayerMove(final PlayerMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer( player );
    }
    
    @Inject(at = @At("TAIL"), method = "onVehicleMove")
    public void onVehicleMove(final VehicleMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer( player );
    }
    
    @Inject(at = @At("RETURN"), method = "onDisconnected")
    public void onPlayerDisconnect(final CallbackInfo callback) {
        CoreMod.PLAYER_LOCATIONS.remove( this.player );
    }
    
    @Inject(at = @At("HEAD"), method = "onChatMessage", cancellable = true)
    public void onChatMessage(ChatMessageC2SPacket chatMessageC2SPacket, CallbackInfo callback) {
        if (!SewingMachineConfig.INSTANCE.CHAT_MODIFY.get())
            return;
        
        NetworkThreadUtils.forceMainThread(chatMessageC2SPacket, this, this.player.getServerWorld());
        if (this.player.getClientChatVisibility() == ChatVisibility.HIDDEN) {
            this.sendPacket(new ChatMessageS2CPacket((new TranslatableText("chat.cannotSend", new Object[0])).formatted(Formatting.RED)));
            
        } else {
            this.player.updateLastActionTime();
            String rawString = chatMessageC2SPacket.getChatMessage();
            rawString = StringUtils.normalizeSpace(rawString);
            
            for(int int_1 = 0; int_1 < rawString.length(); ++int_1) {
                if (!SharedConstants.isValidChar(rawString.charAt(int_1))) {
                    this.disconnect(new TranslatableText("multiplayer.disconnect.illegal_characters", new Object[0]));
                    return;
                }
            }
            
            if (rawString.startsWith("/")) {
                // For a command
                this.executeCommand( rawString );
                
            } else {
                // Create a chat message
                Text chatText = PlayerNameUtils.getPlayerChatDisplay( this.player )
                    .append(new LiteralText( ": " ).formatted(Formatting.GRAY))
                    .append(new LiteralText( rawString ).formatted(Formatting.WHITE));
                
                // Send the new chat message to all players
                this.server.getPlayerManager().broadcastChatMessage(chatText, false);
            }
            
            this.messageCooldown += 20;
            if (this.messageCooldown > 200 && !this.server.getPlayerManager().isOperator(this.player.getGameProfile())) {
                this.disconnect(new TranslatableText("disconnect.spam", new Object[0]));
            }
        }
        callback.cancel();
    }
    
    public void movedPlayer( final ServerPlayerEntity player ) {
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return;
        
        World world = player.getEntityWorld();
        BlockPos blockPos = player.getBlockPos();
        
        ClaimedChunk chunk = ClaimedChunk.convert( world, blockPos );
        
        if ( !CoreMod.PLAYER_LOCATIONS.containsKey( player ) ) {
            
            this.showPlayerNewLocation( player, chunk );
            
        } else {
            UUID playerLocation = CoreMod.PLAYER_LOCATIONS.get( player );
            UUID chunkOwner = ( chunk == null ? null : chunk.getOwner() );
            
            // If the location has changed
            if ( ( ( playerLocation != null ) && (!playerLocation.equals( chunkOwner )) ) || ( ( chunkOwner != null ) && (!chunkOwner.equals( playerLocation )) ) ) {
                this.showPlayerNewLocation( player, chunk );
            }
        }
    }
    
    public void showPlayerNewLocation(@NotNull final PlayerEntity player, final ClaimedChunk local) {
        if ( ( local == null ) || ( local.getOwner() == null ) ) {
            Text popupText = new LiteralText( SewingMachineConfig.INSTANCE.NAME_WILDERNESS.get() ).formatted( Formatting.GREEN )
                .append(
                    new LiteralText( " [" ).formatted(Formatting.RED)
                        .append( TranslatableServerSide.text( player, "claim.chunk.pvp" ) )
                        .append("]" )
                );
            
            // If the player is in the wilderness
            CoreMod.PLAYER_LOCATIONS.put((ServerPlayerEntity) player, null );
            TitleUtils.showPlayerAlert(player, Formatting.GREEN, popupText );
            
        } else {
            CoreMod.PLAYER_LOCATIONS.put((ServerPlayerEntity) player, local.getOwner());
            (new Thread(() -> {
                Text popupText = null;
                popupText = new LiteralText("Entering ")
                    .formatted(Formatting.WHITE);
                
                try {
                    
                    // If player is in spawn protection
                    if (local.getOwner().equals(CoreMod.spawnID)) {
                        popupText.append(
                            local.getOwnerName( player )
                        );
                        return;
                    }
                    
                    String landName = "homestead";
                    ClaimantTown town;
                    if ((town = local.getTown()) != null) {
                        landName = town.getTownType();
                    }
                    
                    if (town == null) {
                        // If player is in their own land (No Town)
                        if ((local.getOwner().equals(player.getUuid()))) {
                            popupText.append(new LiteralText("your " + landName).formatted(Formatting.DARK_GREEN));
                            return;
                        }
                        
                        // If player is in another players area (No Town)
                        popupText.append(local.getOwnerName( player ))
                            .append(new LiteralText("'s " + landName));
                        return;
                    }
                    
                    // If player is in another players town
                    popupText.append(town.getName(player.getUuid())); // Town name
                    if (!local.getOwner().equals(town.getOwner())) // Append the chunk owner (If not the towns)
                        popupText.append(" - ").append(local.getOwnerName( player ));
                    popupText.append( // Town type
                        new LiteralText(" (")
                            .append(new LiteralText(CasingUtils.Words(town.getTownType())).formatted(Formatting.DARK_AQUA))
                            .append(")")
                    );
                    
                } finally {
                    if (popupText != null) {
                        // Show that PvP is enabled
                        if (local.isSetting(ClaimSettings.PLAYER_COMBAT)) {
                            popupText.append(
                                new LiteralText(" [").formatted(Formatting.RED)
                                    .append(TranslatableServerSide.text(player, "claim.chunk.pvp"))
                                    .append("]")
                            );
                        }
                        
                        // Show the message to the player
                        TitleUtils.showPlayerAlert(player, Formatting.WHITE, popupText);
                    }
                }
            })).start();
        }
    }
    
}
