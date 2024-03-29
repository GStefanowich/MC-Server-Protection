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

package net.theelm.sewingmachine.protection;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.commands.abstraction.AbstractSewCommand;
import net.theelm.sewingmachine.events.ContainerAccessCallback;
import net.theelm.sewingmachine.events.PlayerModsCallback;
import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.events.RegionManageCallback;
import net.theelm.sewingmachine.events.RegionUpdateCallback;
import net.theelm.sewingmachine.events.PlayerTeleportCallback;
import net.theelm.sewingmachine.events.RegionNameCallback;
import net.theelm.sewingmachine.events.BlockBreakCallback;
import net.theelm.sewingmachine.events.BlockInteractionCallback;
import net.theelm.sewingmachine.events.BlockPlaceCallback;
import net.theelm.sewingmachine.events.TaxCollection;
import net.theelm.sewingmachine.interfaces.DamageEntityCallback;
import net.theelm.sewingmachine.interfaces.ItemUseCallback;
import net.theelm.sewingmachine.interfaces.LogicalWorld;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.interfaces.variables.EntityVariableFunction;
import net.theelm.sewingmachine.objects.MessageRegion;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.protection.commands.ClaimCommand;
import net.theelm.sewingmachine.protection.config.SewProtectionConfig;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.protection.events.BlockBreak;
import net.theelm.sewingmachine.protection.events.BlockInteraction;
import net.theelm.sewingmachine.protection.events.EntityAttack;
import net.theelm.sewingmachine.protection.events.ItemPlace;
import net.theelm.sewingmachine.protection.events.ItemUse;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.protection.interfaces.PlayerClaimData;
import net.theelm.sewingmachine.protection.interfaces.PlayerMovement;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.theelm.sewingmachine.protection.objects.ServerClaimCache;
import net.theelm.sewingmachine.protection.objects.signs.SignDeed;
import net.theelm.sewingmachine.protection.objects.signs.SignPlots;
import net.theelm.sewingmachine.protection.objects.ticking.ChunkOwnerUpdate;
import net.theelm.sewingmachine.protection.packets.ClaimChunkPacket;
import net.theelm.sewingmachine.protection.packets.ClaimCountPacket;
import net.theelm.sewingmachine.protection.packets.ClaimPermissionPacket;
import net.theelm.sewingmachine.protection.packets.ClaimQueryPacket;
import net.theelm.sewingmachine.protection.packets.ClaimRankPacket;
import net.theelm.sewingmachine.protection.packets.ClaimSettingPacket;
import net.theelm.sewingmachine.protection.packets.ClaimedChunkPacket;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils;
import net.theelm.sewingmachine.protection.utilities.ClaimPropertyUtils;
import net.theelm.sewingmachine.utilities.ChunkUtils;
import net.theelm.sewingmachine.utilities.EntityVariables;
import net.theelm.sewingmachine.utilities.ModUtils;
import net.theelm.sewingmachine.utilities.NetworkingUtils;
import net.theelm.sewingmachine.utilities.ShopSigns;
import net.theelm.sewingmachine.utilities.text.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Created on Jun 08 2023 at 11:58 PM.
 * By greg in sewingmachine
 */
public final class ServerCore implements ModInitializer, SewPlugin {
    @Override
    public void onInitialize() {
        // Create registry based listeners
        BlockBreak.register(BlockBreakCallback.TEST);
        BlockInteraction.register(BlockInteractionCallback.EVENT);
        EntityAttack.register(DamageEntityCallback.EVENT);
        ItemPlace.register(BlockPlaceCallback.EVENT);
        ItemUse.register(ItemUseCallback.EVENT);
        
        // Create a callback for when details about who owns an area is updated
        RegionUpdateCallback.EVENT.register((owner, refresh) -> {
            // Update the name in the claim cache
            ClaimantPlayer claim = ((PlayerClaimData) owner).getClaim();
            
            // Update claim details
            if (refresh) {
                claim.updateName();
            }
            
            // Notify players of change
            claim.visitors(player -> {
                PlayerMovement movement = ((PlayerMovement) player.networkHandler);
                movement.showPlayerNewLocation(player, player.getWorld().getWorldChunk(player.getBlockPos()));
            });
        });
        
        // Create a handler for claiming regions
        RegionManageCallback.HANDLER.register((world, player, region, claimed) -> {
            if (claimed != Boolean.FALSE) {
                if (!ClaimChunkUtils.canPlayerClaimSlices(world, region))
                    return false;
                if (claimed != null)
                    ClaimChunkUtils.claimSlices(world, player, region);
                return true;
            }
            
            ClaimChunkUtils.unclaimSlices(world, region);
            return true;
        });
        
        // Create a callback for getting a players whereabouts
        RegionNameCallback.EVENT.register((world, chunkPos, blockPos, entity, nameOnly, strict) -> {
            IClaimedChunk chunk = (IClaimedChunk) world.getChunk(chunkPos.x, chunkPos.z);
            
            // If the region is Spawn, return Spawn
            if (CoreMod.SPAWN_ID.equals(chunk.getOwnerId()))
                return Text.literal("Spawn")
                    .formatted(Formatting.AQUA);
            
            // If the chunk is a Town, return the Town
            ClaimantTown town = chunk.getTown();
            if (town != null)
                return town.getName();
            
            // If the position is unowned, return Wilderness
            UUID owner = chunk.getOwnerId(blockPos);
            if (owner == null) {
                if (strict && entity instanceof PlayerEntity player)
                    return ClaimChunkUtils.getPlayerWorldWilderness(player);
                return null;
            }
            
            ClaimCache claims = chunk.getClaimCache();
            ClaimantPlayer claim = claims.getPlayerClaim(owner);
            
            Text name = claim.getName(entity instanceof PlayerEntity player ? player.getUuid() : null);
            if (nameOnly)
                return name;
            
            return Text.literal("")
                .append(name)
                .append("'s claimed area");
        });
        
        // Allow teleporting to warps and other locations if the player is within Spawn
        PlayerTeleportCallback.TEST.register((server, player, uuid) -> {
            if (uuid == null)
                return ClaimChunkUtils.isPlayerWithinSpawn((ServerPlayerEntity) player);
            return ClaimChunkUtils.canPlayerWarpTo(server, player.getUuid(), uuid);
        });
        
        // Get town name
        EntityVariables.add("town", (EntityVariableFunction)(source, entity, casing) -> {
            ClaimantTown town = null;
            if (entity instanceof ServerPlayerEntity player) {
                ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
                if (claim != null)
                    town = claim.getTown();
            }
            return town == null ? TextUtils.literal() : town.getName();
        });
        
        // Sending out messages
        MessageRegion.add(new TownMessageRegion());
        
        // Container access permission
        ContainerAccessCallback.TEST.register((entity, world, pos) -> {
            if (
                entity instanceof ServerPlayerEntity player
                && ClaimChunkUtils.canPlayerLootChestsInChunk(player, pos)
            ) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
        
        // Town taxes on signs
        TaxCollection.EVENT.register((income, world, pos) -> {
            // Try getting the located town
            ClaimantTown town = ((IClaimedChunk)world.getChunk(pos))
                .getTown();
            if (town != null)
                income.addTax(town.getName(), town.getTaxRate());
        });
        
        // Send the player their claim data if they have the mod
        PlayerModsCallback.EVENT.register(player -> {
            if (!ModUtils.hasModule(player, "protection"))
                return;
            
            ClaimantPlayer claim = ((PlayerClaimData) player).getClaim();
            NetworkingUtils.send(player, new ClaimCountPacket(claim));
            
            // Send permissions
            for (ClaimPermissions permission : ClaimPermissions.values())
                NetworkingUtils.send(player, new ClaimPermissionPacket(permission, claim.getPermissionRankRequirement(permission)));
            
            // Send claim settings
            for (ClaimSettings setting : ClaimSettings.values())
                if (setting.isEnabled())
                    NetworkingUtils.send(player, new ClaimSettingPacket(setting, claim.getProtectedChunkSetting(setting)));
            
            // Send friends for friend settings
            MinecraftServer server = player.getServer();
            if (server != null) {
                for (UUID friend : claim.getFriends()) {
                    Text name = PlayerNameCallback.getName(server, friend);
                    NetworkingUtils.send(player, new ClaimRankPacket(friend, name, claim.getFriendRank(friend)));
                }
            }
        });
        
        // When a player updates their settings
        NetworkingUtils.serverReceiver(ClaimPermissionPacket.TYPE, (server, player, network, packet, sender)
            -> ClaimPropertyUtils.updatePermission(player, packet.permission(), packet.rank()));
        
        // When a player updates their permissions
        NetworkingUtils.serverReceiver(ClaimSettingPacket.TYPE, (server, player, network, packet, sender)
            -> ClaimPropertyUtils.updateSetting(player, packet.setting(), packet.enabled()));
        
        // When a player updates their friends
        NetworkingUtils.serverReceiver(ClaimRankPacket.TYPE, (server, player, network, packet, sender)
            -> ClaimPropertyUtils.updateRank(player, packet.player(), packet.rank()));
        
        // When the client queries who owns a chunk (Used for map overlay)
        NetworkingUtils.serverReceiver(ClaimQueryPacket.TYPE, (server, player, network, packet, sender) -> {
            // Verify that the player is not too far from the chunk
            ChunkPos queryPos = packet.chunkPos();
            if (!ChunkUtils.isPositionWithinSquareRange(player.getChunkPos(), 8, queryPos))
                return;
            
            ClaimantPlayer claim = null;
            ServerWorld world = player.getServerWorld();
            if (world.getChunk(queryPos.x, queryPos.z, ChunkStatus.FULL, false) instanceof IClaimedChunk claimedChunk)
                claim = claimedChunk.getOwner();
            
            // Send the information
            NetworkingUtils.send(player, new ClaimedChunkPacket(
                queryPos,
                claim == null ? null : claim.getId(),
                claim == null ? null : claim.getName(player)
            ));
        });
        
        // When the client requests to (un)claim a chunk
        NetworkingUtils.serverReceiver(ClaimChunkPacket.TYPE, (server, player, network, packet, sender) -> {
            // Verify that the player is not too far from the chunk
            ChunkPos queryPos = packet.chunkPos();
            if (!ChunkUtils.isPositionWithinSquareRange(player.getChunkPos(), 8, queryPos))
                return;
            
            // Get the claimCache from the server
            if (((ClaimsAccessor) server).getClaimManager() instanceof ServerClaimCache claimCache) {
                // Add a tickable event to the server
                ((LogicalWorld) player.getServerWorld()).addTickableEvent(ChunkOwnerUpdate.forPlayer(
                    claimCache,
                    player.getCommandSource(),
                    player.getUuid(),
                    packet.claimed()
                        ? ChunkOwnerUpdate.Mode.CLAIM : ChunkOwnerUpdate.Mode.UNCLAIM,
                    Arrays.asList(queryPos.getStartPos())
                ).setSilent().setVerify(true /* Must verify all claims */));
            }
        });
        
        ShopSigns.add(SignDeed::new);
        ShopSigns.add(SignPlots::new);
    }
    
    @Override
    public @NotNull AbstractSewCommand<?>[] getCommands() {
        return new AbstractSewCommand<?>[] {
            new ClaimCommand()
        };
    }
    
    @Override
    public @NotNull Optional<Class<?>> getConfigClass() {
        return Optional.of(SewProtectionConfig.class);
    }
    
    private final class TownMessageRegion extends MessageRegion {
        public TownMessageRegion() {
            super("town");
        }
        
        @Override
        public boolean enabled(@NotNull ServerCommandSource source) {
            return ClaimCommand.sourceInTown(source);
        }

        @Override
        public boolean enabled(@NotNull ServerPlayerEntity player) {
            return ClaimCommand.sourceInTown(player);
        }
        
        @Override
        public boolean broadcast(@NotNull ServerPlayerEntity player, @Nullable ServerPlayerEntity target, @NotNull Collection<ServerPlayerEntity> tags, @NotNull SignedMessage message) {
            ClaimantPlayer claimantPlayer = ((PlayerClaimData) player).getClaim();
            //return MessageClaimUtils.sendToTown(claimantPlayer.getTown(), tags, message);
            return false;
        }
    }
}
