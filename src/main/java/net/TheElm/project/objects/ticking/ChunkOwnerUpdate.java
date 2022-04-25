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

package net.TheElm.project.objects.ticking;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.commands.ClaimCommand;
import net.TheElm.project.exceptions.TranslationKeyException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.TickableContext;
import net.TheElm.project.interfaces.TickingAction;
import net.TheElm.project.protections.claiming.Claimant;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.DimensionUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created on Aug 25 2021 at 11:28 AM.
 * By greg in SewingMachineMod
 */
public class ChunkOwnerUpdate implements TickingAction {
    private final @NotNull ServerCommandSource source;
    private final @Nullable Claimant claimant;
    private final @NotNull ChunkOwnerUpdate.Mode mode;
    private final int initialSize;
    private final @NotNull Queue<BlockPos> positions = new ArrayDeque<>();
    private final @NotNull List<ChunkPos> changed = new ArrayList<>();
    
    private boolean verify = true;
    
    private ChunkOwnerUpdate(@NotNull ServerCommandSource source, @Nullable Claimant claimant, @NotNull Mode mode, @NotNull Collection<? extends BlockPos> positions) {
        this.source = source;
        this.claimant = claimant;
        this.mode = mode;
        this.initialSize = positions.size();
        this.positions.addAll(positions);
    }
    
    public ChunkOwnerUpdate setVerify(boolean force) {
        this.verify = force;
        return this;
    }
    public boolean getVerify() {
        return this.verify;
    }
    
    public int getInitialSize() {
        return this.initialSize;
    }
    public @NotNull ServerCommandSource getSource() {
        return this.source;
    }
    public @Nullable Claimant getClaimant() {
        return this.claimant;
    }
    
    @Override
    public boolean isCompleted(@NotNull TickableContext tickable) {
        // Check that a claimant was found (If not, just remove)
        if (this.claimant == null || tickable.isRemoved())
            return true;
        // Run every 2 ticks
        if (tickable.getTicks() % 2 != 0)
            return false;
        World world = tickable.getWorld();
        
        BlockPos claimPos = this.positions.poll();
        if (claimPos == null)
            return true;
        
        WorldChunk chunk = world.getWorldChunk(claimPos);
        
        // Run the chunk mode (Claim or unclaim)
        ActionResult result;
        try {
            result = this.mode.run(chunk, this);
            if (result == ActionResult.FAIL)
                throw this.mode.getException(this.source);
        } catch (TranslationKeyException e) {
            TranslatableServerSide.send(this.source, e.getKey());
            return true;
        } catch (CommandSyntaxException e) {
            this.source.sendFeedback(
                new LiteralText(e.getMessage()).formatted(Formatting.RED),
                false
            );
            return true;
        }
        
        // Success has passed, add the chunk position to overall
        this.changed.add(chunk.getPos());
        
        // Try closing the tickable
        boolean finished = this.positions.isEmpty();
        if (finished)
            this.finish();
        return finished;
    }
    
    public void finish() {
        assert this.claimant != null;
        
        // Get a list of all chunks
        int changed = this.changed.size();
        String log = this.changed.stream()
            .map(chunkPos -> "(" + MessageUtils.xzToString(chunkPos) + ")")
            .collect(Collectors.joining(", "));
        
        // Log the chunks that were changed
        CoreMod.logInfo(this.claimant.getName().getString() + " has " + this.mode.name().toLowerCase(Locale.ROOT) + "ed " + changed + " chunk(s): (" + log + ")");
        if (changed > 0)
            TranslatableServerSide.send(this.source, this.mode.getSuccessTranslation(), changed);
    }
    
    public static @NotNull ChunkOwnerUpdate forPlayer(@NotNull ClaimCache claimCache, @NotNull ServerCommandSource source, @NotNull UUID uuid, @NotNull Mode mode, @NotNull Collection<? extends BlockPos> positions) {
        return new ChunkOwnerUpdate(source, claimCache.getPlayerClaim(uuid), mode, positions);
    }
    public static @NotNull ChunkOwnerUpdate forTown(@NotNull ClaimCache claimCache, @NotNull ServerCommandSource source, @NotNull UUID uuid, @NotNull Mode mode, @NotNull Collection<? extends BlockPos> positions) {
        return new ChunkOwnerUpdate(source, claimCache.getTownClaim(uuid), mode, positions);
    }
    
    public enum Mode {
        CLAIM {
            @Override
            public ActionResult run(@NotNull WorldChunk worldChunk, @NotNull ChunkOwnerUpdate update) throws CommandSyntaxException, TranslationKeyException {
                ServerCommandSource source = update.getSource();
                
                // Check if within the world border
                WorldBorder border = worldChunk.getWorld()
                    .getWorldBorder();
                
                // If the chunk is outside of the world border
                ChunkPos chunkPos = worldChunk.getPos();
                if (!border.contains(chunkPos))
                    return ActionResult.PASS;
                
                IClaimedChunk chunk = (IClaimedChunk) worldChunk;
                Claimant claimant = Objects.requireNonNull(update.getClaimant());
                
                // Prevent claiming protected areas (IE: End Island)
                if (update.getVerify() && !claimant.isSpawn() && DimensionUtils.isWithinProtectedZone(worldChunk))
                    return ActionResult.FAIL;
                
                if (claimant instanceof ClaimantTown claimantTown) {
                    if (!Objects.equals(chunk.getOwnerId(), claimantTown.getOwnerId()))
                        return ActionResult.FAIL;
                    if (chunk.getTown() != null && chunk.getTown() != claimantTown)
                        throw ClaimCommand.CHUNK_ALREADY_OWNED.create(source);
                    
                    claimantTown.addToCount(worldChunk);
                    chunk.updateTownOwner(claimant.getId());
                } else if (claimant instanceof ClaimantPlayer claimantPlayer) {
                    // Check if it's available
                    if (update.getVerify() && !chunk.canPlayerClaim(claimantPlayer, update.getInitialSize() <= 1))
                        return ActionResult.FAIL;
                    
                    // Check if the chunk is owned by another player
                    if (chunk.getOwnerId() != null && !Objects.equals(chunk.getOwnerId(), claimant.getId()))
                        throw ClaimCommand.CHUNK_ALREADY_OWNED.create(source);
                    
                    // Update the information
                    claimant.addToCount(worldChunk);
                    chunk.updatePlayerOwner(claimant.getId());
                    
                    // Set the town too
                    ClaimantTown claimantTown = claimantPlayer.getTown();
                    if (claimantTown != null)
                        claimantTown.addToCount(worldChunk);
                }
                
                worldChunk.setNeedsSaving(true);
                return ActionResult.SUCCESS;
            }
            @Override
            public @NotNull String getSuccessTranslation() {
                return "claim.chunk.claimed";
            }
            @Override
            public @NotNull CommandSyntaxException getException(@NotNull CommandSource source) {
                return ClaimCommand.CHUNK_ALREADY_OWNED.create(source);
            }
        },
        UNCLAIM {
            @Override
            public ActionResult run(@NotNull WorldChunk worldChunk, @NotNull ChunkOwnerUpdate update) throws CommandSyntaxException, TranslationKeyException {
                IClaimedChunk chunk = (IClaimedChunk) worldChunk;
                Claimant claimant = Objects.requireNonNull(update.getClaimant());
                
                // Cleanup/Remove the players count
                if (update.getVerify() || Objects.equals(claimant.getId(), chunk.getOwnerId()))
                    claimant.removeFromCount(worldChunk);
                else if (!update.getVerify()) {
                    // If we aren't verifying the UNCLAIM, get the existing owner and subtract the chunk from them
                    ClaimCache claims = chunk.getClaimCache();
                    if (claims != null && chunk.getOwnerId() != null)
                        claims.getPlayerClaim(chunk.getOwnerId())
                            .removeFromCount(worldChunk);
                }
                
                // If the chunk is not owned
                if (update.getVerify() && chunk.getOwnerId() == null && update.getInitialSize() <= 1)
                    throw ClaimCommand.CHUNK_NOT_OWNED.create(update.getSource());
                
                // If the chunk is owned by another player
                if (update.getVerify() && chunk.getOwnerId() != null && !Objects.equals(claimant.getId(), chunk.getOwnerId()) )
                    return update.getInitialSize() > 1 ? ActionResult.PASS : ActionResult.FAIL;
                
                // Remove the towns count
                ClaimantTown town;
                if ((town = chunk.getTown()) != null)
                    town.removeFromCount(worldChunk);
                
                // Set the chunks owner
                chunk.updatePlayerOwner(null);
                
                worldChunk.setNeedsSaving(true);
                return ActionResult.SUCCESS;
            }
            @Override
            public @NotNull String getSuccessTranslation() {
                return "claim.chunk.unclaimed";
            }
            @Override
            public @NotNull CommandSyntaxException getException(@NotNull CommandSource source) {
                return ClaimCommand.CHUNK_NOT_OWNED_BY_PLAYER.create(source);
            }
        };
        
        public abstract ActionResult run(@NotNull WorldChunk worldChunk, @NotNull ChunkOwnerUpdate update) throws CommandSyntaxException, TranslationKeyException;
        public abstract @NotNull String getSuccessTranslation();
        public abstract @NotNull CommandSyntaxException getException(@NotNull CommandSource source);
    }
}
