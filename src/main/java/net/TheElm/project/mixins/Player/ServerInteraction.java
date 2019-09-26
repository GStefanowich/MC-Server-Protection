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
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.enums.ClaimSettings;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.PlayerMovement;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.LegacyConverter;
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerInteraction implements ServerPlayPacketListener, PlayerMovement, PlayerChat, PlayerData {
    
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
     * Claims
     */
    private ClaimantPlayer playerClaimData = null;
    
    @Override
    public ClaimantPlayer getClaim() {
        return this.playerClaimData;
    }
    
    /*
     * Chat Handlers
     */
    private ChatRooms chatRoom = ChatRooms.GLOBAL;
    
    @Override @NotNull
    public ChatRooms getChatRoom() {
        return this.chatRoom;
    }
    @Override
    public void setChatRoom(@NotNull ChatRooms room) {
        // Set the chat room
        this.chatRoom = room;
        
        // Tell the player
        player.sendMessage(TranslatableServerSide.text(player, "chat.change." + room.name().toLowerCase()));
    }
    
    /*
     * Modified methods
     */
    
    @Inject(at = @At("RETURN"), method = "<init>")
    public void onPlayerConnect(MinecraftServer server, ClientConnection client, ServerPlayerEntity player, CallbackInfo callback) {
        // Check if the LegacyConverter is running
        if (LegacyConverter.running()) {
            this.disconnect(new LiteralText("The server is currently updating!"));
            return;
        }
        
        // Set the players position as in the wilderness
        CoreMod.PLAYER_LOCATIONS.put( player, null );
        
        // Initialize user claims from database
        this.playerClaimData = ClaimantPlayer.get( player.getUuid() );
        
        // Check if server has been joined before
        if (((PlayerData) player).getFirstJoinAt() == null) {
            // Get starting money
            int startingMoney = SewingMachineConfig.INSTANCE.STARTING_MONEY.get();
            
            // Give the player the starting amount
            if ( SewingMachineConfig.INSTANCE.DO_MONEY.get() && (startingMoney > 0))
                MoneyUtils.givePlayerMoney( player, startingMoney );
            
            // Give the player the starting items
            for (Map.Entry<Item, Integer> item : SewingMachineConfig.INSTANCE.STARTING_ITEMS.get().entrySet()) {
                ItemStack stack = new ItemStack( item.getKey() );
                stack.setCount( item.getValue() );
                
                player.inventory.offerOrDrop( player.world, stack );
            }
            
            // Set first join for later referencing
            ((PlayerData) player).updateFirstJoin();
        } else {
            Long lastJoin;
            int allowance = SewingMachineConfig.INSTANCE.DAILY_ALLOWANCE.get();
            
            // If we should give a daily allowance
            if ((allowance > 0) && ((lastJoin = ((PlayerData) player).getLastJoinAt()) != null)) {
                
                // Get the timestamp of the start of today
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfDay = calendar.getTimeInMillis();
                
                // If players last join was before the start of TODAY
                if (lastJoin <= startOfDay) {
                    // Give that player money
                    MoneyUtils.givePlayerMoney(player, allowance);
                    
                    // Tell them they were awarded money
                    player.sendMessage(new LiteralText("You were given $").formatted(Formatting.YELLOW).append(new LiteralText(NumberFormat.getInstance().format(allowance)).formatted(Formatting.AQUA, Formatting.BOLD)).append(" for logging in today!"));
                }
            }
        }
        
        // Always update join time to NOW
        ((PlayerData) player).updateLastJoin();
    }
    
    @Inject(at = @At("TAIL"), method = "onPlayerMove")
    public void onPlayerMove(final PlayerMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer( this.player );
    }
    
    @Inject(at = @At("TAIL"), method = "onVehicleMove")
    public void onVehicleMove(final VehicleMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer( this.player );
    }
    
    @Inject(at = @At("RETURN"), method = "onDisconnected")
    public void onPlayerDisconnect(final CallbackInfo callback) {
        CoreMod.PLAYER_LOCATIONS.remove( this.player );
    }
    
    @Inject(at = @At(value = "INVOKE", target = "net/minecraft/server/PlayerManager.broadcastChatMessage(Lnet/minecraft/text/Text;Z)V"), method = "onChatMessage", cancellable = true)
    public void onChatMessage(ChatMessageC2SPacket chatMessageC2SPacket, CallbackInfo callback) {
        if (!SewingMachineConfig.INSTANCE.CHAT_MODIFY.get())
            return;
        
        // Parse the users message
        String rawString = StringUtils.normalizeSpace(chatMessageC2SPacket.getChatMessage());
        
        // Create a chat message
        Text chatText = MessageUtils.formatPlayerMessage( this.player, this.getChatRoom(), rawString );
        
        // Send the new chat message to the currently selected chat room
        switch (this.getChatRoom()) {
            // Local message
            case LOCAL: {
                MessageUtils.sendToLocal( this.player.world, this.player.getBlockPos(), chatText );
                break;
            }
            // Global message
            case GLOBAL: {
                MessageUtils.sendToAll( chatText );
                break;
            }
            // Message to the players town
            case TOWN: {
                ClaimantPlayer claimantPlayer = ((PlayerData) this.player).getClaim();
                MessageUtils.sendToTown( claimantPlayer.getTown(), chatText );
                break;
            }
        }
        
        // Cancel the original
        callback.cancel();
    }
    
    public void movedPlayer( final ServerPlayerEntity player ) {
        if (!SewingMachineConfig.INSTANCE.DO_CLAIMS.get())
            return;
        
        World world = player.getEntityWorld();
        BlockPos blockPos = player.getBlockPos();
        
        WorldChunk chunk = world.getWorldChunk( blockPos );
        if ( !CoreMod.PLAYER_LOCATIONS.containsKey( player ) ) {
            this.showPlayerNewLocation( player, chunk );
            
        } else {
            UUID playerLocation = CoreMod.PLAYER_LOCATIONS.get( player );
            UUID chunkOwner = ( chunk == null ? null : ((IClaimedChunk) chunk).getOwner( blockPos ) );
            
            // If the location has changed
            if ( ( ( playerLocation != null ) && (!playerLocation.equals( chunkOwner )) ) || ( ( chunkOwner != null ) && (!chunkOwner.equals( playerLocation )) ) ) {
                this.showPlayerNewLocation( player, chunk );
            }
        }
    }
    
    public void showPlayerNewLocation(@NotNull final PlayerEntity player, @Nullable final WorldChunk local) {
        BlockPos playerPos = player.getBlockPos();
        UUID locationOwner;
        
        if ( ( local == null ) || ((locationOwner = ((IClaimedChunk) local).getOwner( playerPos )) == null ) ) {
            Text popupText = ChunkUtils.getPlayerWorldWilderness( player )
                .append(
                    new LiteralText( " [" ).formatted(Formatting.RED)
                        .append( TranslatableServerSide.text( player, "claim.chunk.pvp" ) )
                        .append("]" )
                );
            
            // If the player is in the wilderness
            CoreMod.PLAYER_LOCATIONS.put((ServerPlayerEntity) player, null );
            TitleUtils.showPlayerAlert(player, Formatting.GREEN, popupText );
            
        } else {
            CoreMod.PLAYER_LOCATIONS.put((ServerPlayerEntity) player, locationOwner);
            (new Thread(() -> {
                IClaimedChunk claimedChunk = (IClaimedChunk) local;
                Text popupText = null;
                popupText = new LiteralText("Entering ")
                    .formatted(Formatting.WHITE);
                
                ClaimantPlayer owner; // Get the owner (Shouldn't be null unless error occurs, ignore)
                if ((owner = ClaimantPlayer.get( locationOwner )) == null)
                    return;
                
                try {
                    // If player is in spawn protection
                    if (locationOwner.equals(CoreMod.spawnID)) {
                        popupText.append(
                            owner.getName( player )
                        );
                        return;
                    }
                    
                    String landName = "homestead";
                    ClaimantTown town;
                    if ((town = claimedChunk.getTown()) != null) {
                        landName = town.getTownType();
                    }
                    
                    if (town == null) {
                        // If player is in their own land (No Town)
                        if ((locationOwner.equals(claimedChunk.getOwner())) && (locationOwner.equals(player.getUuid()))) {
                            popupText.append(new LiteralText("your " + landName).formatted(Formatting.DARK_GREEN));
                            return;
                        }
                        
                        // If player is in another players area (No Town)
                        popupText.append(owner.getName( player ))
                            .append(new LiteralText("'s " + landName));
                        return;
                    }
                    
                    // If player is in another players town
                    popupText.append(town.getName(player.getUuid())); // Town name
                    if (!locationOwner.equals(town.getOwner())) // Append the chunk owner (If not the towns)
                        popupText.append(" - ").append(claimedChunk.getOwnerName( player ));
                    popupText.append( // Town type
                        new LiteralText(" (")
                            .append(new LiteralText(CasingUtils.Words(town.getTownType())).formatted(Formatting.DARK_AQUA))
                            .append(")")
                    );
                    
                } finally {
                    if (popupText != null) {
                        // Show that PvP is enabled
                        if (claimedChunk.isSetting(ClaimSettings.PLAYER_COMBAT)) {
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
