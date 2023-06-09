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

package net.theelm.sewingmachine.protection.mixins.Player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.objects.PlayerVisitor;
import net.theelm.sewingmachine.protection.interfaces.PlayerTravel;
import net.theelm.sewingmachine.protection.interfaces.PlayerMovement;
import net.theelm.sewingmachine.protection.utilities.ChunkUtils;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.utilities.CasingUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements PlayerMovement {
    @Shadow @Final private MinecraftServer server;
    @Shadow public ServerPlayerEntity player;
    
    // On player move
    @Inject(at = @At("TAIL"), method = "onPlayerMove")
    public void onPlayerMove(final PlayerMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer(this.player);
    }
    
    // On vehicle move
    @Inject(at = @At("TAIL"), method = "onVehicleMove")
    public void onVehicleMove(final VehicleMoveC2SPacket movement, final CallbackInfo callback) {
        this.movedPlayer(this.player);
    }
    
    @Override
    public void movedPlayer(@NotNull final ServerPlayerEntity player) {
        if (!SewConfig.get(SewCoreConfig.DO_CLAIMS))
            return;
        
        World world = player.getEntityWorld();
        BlockPos blockPos = player.getBlockPos();
        
        PlayerVisitor location = ((PlayerTravel) player).getLocation();
        WorldChunk chunk = world.getWorldChunk(blockPos);
        
        if (location == null)
            this.showPlayerNewLocation(player, chunk);
        else {
            UUID playerLocation = location.get();
            UUID chunkOwner = ( chunk == null ? null : ((IClaimedChunk) chunk).getOwnerId( blockPos ) );

            // If the location has changed
            if ((( playerLocation != null ) && (!playerLocation.equals( chunkOwner )) ) || ( ( chunkOwner != null ) && (!chunkOwner.equals(playerLocation))))
                this.showPlayerNewLocation(player, chunk);
        }
        
        /*if ( !CoreMod.PLAYER_LOCATIONS.containsKey(player) ) {
            this.showPlayerNewLocation(player, chunk);
            
        } else {
            UUID playerLocation = CoreMod.PLAYER_LOCATIONS.get(player);
            UUID chunkOwner = ( chunk == null ? null : ((IClaimedChunk) chunk).getOwnerId( blockPos ) );
            
            // If the location has changed
            if ((( playerLocation != null ) && (!playerLocation.equals( chunkOwner )) ) || ( ( chunkOwner != null ) && (!chunkOwner.equals(playerLocation)))) {
                this.showPlayerNewLocation(player, chunk);
            }
        }*/
    }

    @Override
    public void showPlayerNewLocation(@NotNull final PlayerEntity player, @Nullable final Chunk local) {
        BlockPos playerPos = player.getBlockPos();
        UUID locationOwner;

        if (( local == null ) || ((locationOwner = ((IClaimedChunk) local).getOwnerId(playerPos)) == null )) {
            MutableText popupText = ChunkUtils.getPlayerWorldWilderness(player)
                .append(
                    Text.literal(" [").formatted(Formatting.RED)
                        .append(TranslatableServerSide.text(player, "claim.chunk.pvp"))
                        .append("]")
                );

            // If the player is in the wilderness
            //CoreMod.PLAYER_LOCATIONS.put((ServerPlayerEntity) player, null);
            ((PlayerTravel)player).updateLocation();
            TitleUtils.showPlayerAlert(player, Formatting.GREEN, popupText);

        } else {
            //CoreMod.PLAYER_LOCATIONS.put((ServerPlayerEntity) player, locationOwner);
            PlayerVisitor visitor = ((PlayerTravel) player).updateLocation(locationOwner);
            IClaimedChunk claimedChunk = (IClaimedChunk) local;
            MutableText popupText = Text.literal("Entering ")
                .formatted(Formatting.WHITE);

            ClaimantPlayer owner = ((ClaimsAccessor)this.server).getClaimManager()
                .getPlayerClaim(locationOwner);

            owner.addVisitor(visitor);

            try {
                // If player is in spawn protection
                if (locationOwner.equals(CoreMod.SPAWN_ID)) {
                    popupText.append(
                        owner.getName(player)
                    );
                    return;
                }

                String landName = "homestead";
                ClaimantTown town;
                if ((town = claimedChunk.getTown()) != null) {
                    landName = town.getTownType();

                    town.addVisitor(visitor);
                } else {
                    // If player is in their own land (No Town)
                    if ((locationOwner.equals(claimedChunk.getOwnerId())) && (locationOwner.equals(player.getUuid()))) {
                        popupText.append(Text.literal("your " + landName).formatted(Formatting.DARK_GREEN));
                        return;
                    }

                    // If player is in another players area (No Town)
                    popupText.append(owner.getName(player))
                        .append(Text.literal("'s " + landName));
                    return;
                }

                // If player is in another players town
                popupText.append(town.getName(player.getUuid())); // Town name
                if (!locationOwner.equals(town.getOwnerId())) // Append the chunk owner (If not the towns)
                    popupText.append(" - ").append(claimedChunk.getOwnerName(player));
                popupText.append( // Town type
                    Text.literal(" (")
                        .append(Text.literal(CasingUtils.words(town.getTownType())).formatted(Formatting.DARK_AQUA))
                        .append(")")
                );

            } finally {
                if (popupText != null) {
                    // Show that PvP is enabled
                    if (claimedChunk.isSetting(playerPos, ClaimSettings.PLAYER_COMBAT)) {
                        popupText.append(
                            Text.literal(" [").formatted(Formatting.RED)
                                .append(TranslatableServerSide.text(player, "claim.chunk.pvp"))
                                .append("]")
                        );
                    }

                    // Show the message to the player
                    TitleUtils.showPlayerAlert(player, Formatting.WHITE, popupText);
                }
            }
        }
    }
}
