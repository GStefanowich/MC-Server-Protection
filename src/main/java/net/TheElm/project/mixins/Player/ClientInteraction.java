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
import net.TheElm.project.utilities.MessageUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ClientInteraction implements ServerPlayPacketListener, PlayerMovement, PlayerData {
    
    /*
     * Shadow Methods
     */
    @Shadow @Final public ClientConnection connection;
    @Shadow @Final private MinecraftServer server;
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
     * Health Bars
     */
    @NotNull
    public ServerBossBar getHealthBar() {
        return ((PlayerData)this.player).getHealthBar();
    }
    
    /*
     * Modified methods
     */
    
    // On connect
    @Inject(at = @At("RETURN"), method = "<init>")
    public void onPlayerConnect(MinecraftServer server, ClientConnection client, ServerPlayerEntity player, CallbackInfo callback) {
        // Set the players position as in the wilderness
        CoreMod.PLAYER_LOCATIONS.put( player, null );
        
        // Initialize user claims from database
        this.playerClaimData = ( SewingMachineConfig.INSTANCE.DO_CLAIMS.get() ? ClaimantPlayer.get( player.getUuid() ) : null );
        
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
            
            // Give the player all of the games recipes
            if (SewingMachineConfig.INSTANCE.START_WITH_RECIPES.get()) {
                Collection<Recipe<?>> recipes = server.getRecipeManager().values();
                this.player.unlockRecipes( recipes );
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
                    player.sendMessage(new LiteralText("You were given $")
                        .formatted(Formatting.YELLOW)
                        .append(new LiteralText(NumberFormat.getInstance().format(allowance)).formatted(Formatting.AQUA, Formatting.BOLD))
                        .append(" for logging in today!"));
                }
            }
        }
        
        // Always update join time to NOW
        ((PlayerData) player).updateLastJoin();
    }
    
    // On player move
    @Inject(at = @At("TAIL"), method = "onPlayerMove")
    public void onPlayerMove(final PlayerMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer( this.player );
    }
    
    // On vehicle move
    @Inject(at = @At("TAIL"), method = "onVehicleMove")
    public void onVehicleMove(final VehicleMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer( this.player );
    }
    
    // When player leaves
    @Inject(at = @At("RETURN"), method = "onDisconnected")
    public void onPlayerDisconnect(final CallbackInfo callback) {
        // Clear the players location from the cache
        // (Will show location again when logged back in)
        CoreMod.PLAYER_LOCATIONS.remove( this.player );
        
        // Remove players from the health bar when disconnecting
        // (Don't have floating health bars remaining on-screen)
        this.getHealthBar().clearPlayers();
    }
    
    // Change the chat format
    @Inject(at = @At(value = "INVOKE", target = "net/minecraft/server/PlayerManager.broadcastChatMessage(Lnet/minecraft/text/Text;Z)V"), method = "onChatMessage", cancellable = true)
    public void onChatMessage(ChatMessageC2SPacket chatMessageC2SPacket, CallbackInfo callback) {
        if (!SewingMachineConfig.INSTANCE.CHAT_MODIFY.get())
            return;
        
        try {
            // Parse the users message
            String rawString = StringUtils.normalizeSpace(chatMessageC2SPacket.getChatMessage());
            
            // The chatroom to send the message in
            ChatRooms room = ((PlayerChat)this.player).getChatRoom();
            
            if ((room != ChatRooms.TOWN) && ((PlayerChat)this.player).isMuted()) {
                this.player.sendMessage(TranslatableServerSide.text(
                    this.player,
                    "chat.muted",
                    room
                ).formatted(Formatting.RED));
                return;
            }
            
            // Create a chat message
            Text chatText = MessageUtils.formatPlayerMessage( this.player, room, rawString );
            
            // Send the new chat message to the currently selected chat room
            MessageUtils.sendTo(room, this.player, chatText);
        } finally {
            // Cancel the original formatting
            callback.cancel();
        }
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
                
                ClaimantPlayer owner  = ClaimantPlayer.get( locationOwner );
                
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
                        if (claimedChunk.isSetting(playerPos, ClaimSettings.PLAYER_COMBAT)) {
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
