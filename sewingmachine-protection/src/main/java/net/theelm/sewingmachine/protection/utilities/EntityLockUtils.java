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

package net.theelm.sewingmachine.protection.utilities;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.BlastFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CartographyTableBlock;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.FletchingTableBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SmithingTableBlock;
import net.minecraft.block.SmokerBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SalmonEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.theelm.sewingmachine.protection.interfaces.ClaimsAccessor;
import net.theelm.sewingmachine.protection.interfaces.IClaimedChunk;
import net.theelm.sewingmachine.utilities.EntityUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.ServerText;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class EntityLockUtils {
    private EntityLockUtils() {}
    
    // Default lock sound
    public static SoundEvent getDefaultLockSound() {
        return SoundEvents.BLOCK_WOODEN_DOOR_OPEN;
    }
    
    /*
     * Get Entity Sounds
     */
    public static @NotNull SoundEvent getLockSound(@NotNull Block block) {
        return EntityLockUtils.getLockSound(block, null, null);
    }
    public static @NotNull SoundEvent getLockSound(@NotNull Block block, @Nullable BlockState blockState, @Nullable BlockEntity blockEntity) {
        if (blockEntity != null) {
            if ( blockEntity instanceof BeehiveBlockEntity hive) {
                if (!hive.hasNoBees())
                    return SoundEvents.BLOCK_BEEHIVE_WORK;
                return (blockState != null && (BeehiveBlockEntity.getHoneyLevel(blockState) > 0) ? SoundEvents.BLOCK_BEEHIVE_DRIP : EntityLockUtils.getDefaultLockSound());
            }
        }
        if (block instanceof BarrelBlock)
            return SoundEvents.BLOCK_FENCE_GATE_CLOSE;
        if (block instanceof LoomBlock || block instanceof CartographyTableBlock)
            return SoundEvents.UI_LOOM_SELECT_PATTERN;
        if (block instanceof AnvilBlock || block instanceof GrindstoneBlock)
            return SoundEvents.BLOCK_ANVIL_PLACE;
        if (block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof DropperBlock || block instanceof HopperBlock)
            return SoundEvents.BLOCK_IRON_DOOR_OPEN;
        if (block instanceof BeaconBlock || block instanceof EnchantingTableBlock)
            return SoundEvents.ENTITY_PLAYER_LEVELUP;
        if (block instanceof StonecutterBlock || block instanceof SmokerBlock)
            return SoundEvents.BLOCK_SCAFFOLDING_BREAK;
        if (block instanceof FletchingTableBlock)
            return SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE;
        if (block instanceof CraftingTableBlock || block instanceof ChiseledBookshelfBlock)
            return SoundEvents.BLOCK_WOOD_BREAK;
        if (block instanceof ShulkerBoxBlock)
            return SoundEvents.BLOCK_SHULKER_BOX_CLOSE;
        if (block instanceof BeehiveBlock)
            return SoundEvents.BLOCK_BEEHIVE_DRIP;
        return EntityLockUtils.getDefaultLockSound();
    }
    public static @NotNull SoundEvent getLockSound(Entity entity) {
        // Locked blocks
        if (entity instanceof MinecartEntity || entity instanceof HopperMinecartEntity || entity instanceof FurnaceMinecartEntity)
            return SoundEvents.BLOCK_IRON_DOOR_OPEN;
        if (entity instanceof TntMinecartEntity)
            return SoundEvents.ENTITY_PARROT_IMITATE_CREEPER;
        if (entity instanceof ChestMinecartEntity)
            return EntityLockUtils.getDefaultLockSound();
        if (entity instanceof ArmorStandEntity || entity instanceof BoatEntity)
            return SoundEvents.BLOCK_FENCE_GATE_CLOSE;
        // Protective mobs
        if (entity instanceof WolfEntity)
            return SoundEvents.ENTITY_WOLF_GROWL;
        if (entity instanceof CatEntity)
            return SoundEvents.ENTITY_CAT_HISS;
        if (entity instanceof FoxEntity)
            return SoundEvents.ENTITY_FOX_SCREECH;
        // Villager
        if (entity instanceof IronGolemEntity)
            return SoundEvents.ENTITY_IRON_GOLEM_HURT;
        if (entity instanceof VillagerEntity)
            return SoundEvents.ENTITY_VILLAGER_NO;
        if (entity instanceof SnowGolemEntity)
            return SoundEvents.ENTITY_SNOW_GOLEM_AMBIENT;
        // Unhappy mobs
        if (entity instanceof CowEntity)
            return SoundEvents.ENTITY_COW_AMBIENT;
        if (entity instanceof DonkeyEntity)
            return SoundEvents.ENTITY_DONKEY_AMBIENT;
        if (entity instanceof LlamaEntity)
            return SoundEvents.ENTITY_LLAMA_ANGRY;
        if (entity instanceof MuleEntity)
            return SoundEvents.ENTITY_MULE_AMBIENT;
        if (entity instanceof HorseEntity)
            return SoundEvents.ENTITY_HORSE_ANGRY;
        if (entity instanceof PigEntity)
            return SoundEvents.ENTITY_PIG_AMBIENT;
        if (entity instanceof SheepEntity)
            return SoundEvents.ENTITY_SHEEP_AMBIENT;
        if (entity instanceof ChickenEntity)
            return SoundEvents.ENTITY_CHICKEN_AMBIENT;
        if (entity instanceof OcelotEntity)
            return SoundEvents.ENTITY_OCELOT_AMBIENT;
        if (entity instanceof ParrotEntity)
            return SoundEvents.ENTITY_PARROT_AMBIENT;
        if (entity instanceof RabbitEntity)
            return SoundEvents.ENTITY_RABBIT_AMBIENT;
        if (entity instanceof PandaEntity)
            return SoundEvents.ENTITY_PANDA_AMBIENT;
        if (entity instanceof PolarBearEntity)
            return SoundEvents.ENTITY_POLAR_BEAR_WARNING;
        // Aquatic
        if (entity instanceof TurtleEntity)
            return SoundEvents.ENTITY_TURTLE_AMBIENT_LAND;
        if (entity instanceof CodEntity)
            return SoundEvents.ENTITY_COD_AMBIENT;
        if (entity instanceof PufferfishEntity)
            return SoundEvents.ENTITY_PUFFER_FISH_AMBIENT;
        if (entity instanceof SalmonEntity)
            return SoundEvents.ENTITY_SALMON_AMBIENT;
        if (entity instanceof TropicalFishEntity)
            return SoundEvents.ENTITY_TROPICAL_FISH_AMBIENT;
        if (entity instanceof SquidEntity)
            return SoundEvents.ENTITY_SQUID_SQUIRT;
        if (entity instanceof DolphinEntity)
            return SoundEvents.ENTITY_DOLPHIN_AMBIENT;
        // Nether
        if (entity instanceof PiglinEntity)
            return SoundEvents.ENTITY_PIGLIN_AMBIENT;
        return EntityLockUtils.getDefaultLockSound();
    }
    
    /*
     * Get Lock Permissions
     */
    public static @Nullable ClaimPermissions getLockPermission(BlockEntity block) {
        if (block instanceof JukeboxBlockEntity)
            return ClaimPermissions.STORAGE;
        if (block instanceof LockableContainerBlockEntity || block instanceof ChiseledBookshelfBlockEntity)
            return ClaimPermissions.STORAGE;
        if (block instanceof BedBlockEntity)
            return ClaimPermissions.BEDS;
        if (block instanceof BrushableBlockEntity)
            return ClaimPermissions.BLOCKS;
        return null;
    }
    public static @Nullable ClaimPermissions getLockPermission(@NotNull Block block) {
        // Crafting Blocks
        if (block instanceof FletchingTableBlock)
            return ClaimPermissions.CRAFTING;
        if (block instanceof SmithingTableBlock)
            return ClaimPermissions.CRAFTING;
        if (block instanceof CraftingTableBlock)
            return ClaimPermissions.CRAFTING;
        if (block instanceof EnchantingTableBlock)
            return ClaimPermissions.CRAFTING;
        if (block instanceof GrindstoneBlock)
            return ClaimPermissions.CRAFTING;
        if (block instanceof LoomBlock)
            return ClaimPermissions.CRAFTING;
        if (block instanceof StonecutterBlock)
            return ClaimPermissions.CRAFTING;
        if (block instanceof CartographyTableBlock)
            return ClaimPermissions.CRAFTING;
        // Activation Blocks
        if (block instanceof NoteBlock)
            return ClaimPermissions.BLOCKS;
        if (block instanceof BedBlock)
            return ClaimPermissions.BEDS;
        // Storage Blocks
        if (block instanceof BeaconBlock)
            return ClaimPermissions.STORAGE;
        if (block instanceof AnvilBlock)
            return ClaimPermissions.STORAGE;
        if (block instanceof BellBlock)
            return ClaimPermissions.STORAGE;
        if (block instanceof LecternBlock)
            return ClaimPermissions.STORAGE;
        if (block instanceof FlowerPotBlock)
            return ClaimPermissions.STORAGE;
        if (block instanceof BeehiveBlock)
            return ClaimPermissions.STORAGE;
        return null;
    }
    
    /*
     * Get Entity Names
     */
    public static Text getLockedName(@NotNull Block block) {
        return Text.translatable(block.getTranslationKey())
            .formatted(Formatting.AQUA);
    }
    public static Text getLockedName(@NotNull Entity entity) {
        return entity.getName().copy()
            .formatted(Formatting.AQUA);
    }
    
    /**
     * Jiggle the lock on an entity
     * @param source The entity that the lock is attached to
     * @param player The player that jiggled the lock
     */
    public static void playLockSoundFromSource(@NotNull Entity source, @Nullable PlayerEntity player) {
        // Play sound
        source.playSound(EntityLockUtils.getLockSound(source), 1, 1);
        
        if (player != null) {
            Text owner;
            UUID uuid = EntityUtils.getOwner(source);
            if (uuid != null) {
                owner = ((ClaimsAccessor)player.getServer()).getClaimManager()
                    .getPlayerClaim(uuid)
                    .getName();
            } else {
                WorldChunk chunk = source.getWorld()
                    .getWorldChunk(source.getBlockPos());
                if (chunk != null)
                    owner = ((IClaimedChunk) chunk).getOwnerName(player, source.getBlockPos());
                else
                    owner = Text.literal("unknown player")
                        .formatted(Formatting.LIGHT_PURPLE);
            }
            
            // Display that this item can't be opened
            TitleUtils.showPlayerAlert(player, Formatting.WHITE, ServerText.translatable(player, "claim.block.locked",
                EntityLockUtils.getLockedName(source),
                owner
            ));
        }
    }
    
    /**
     * Jiggle the lock on a block
     * @param source The block that holds the lock
     * @param state The state of the block
     * @param player The player that jiggled the lock
     */
    public static void playLockSoundFromSource(@NotNull BlockEntity source, @NotNull BlockState state, PlayerEntity player) {
        World world = source.getWorld();
        BlockPos blockPos = source.getPos();
        Block block = state.getBlock();
        
        // Play sound
        world.playSound(null, blockPos, EntityLockUtils.getLockSound(block, state, source), SoundCategory.BLOCKS, 0.5f, 1f);
        
        if (player != null) {
            WorldChunk chunk = world.getWorldChunk(blockPos);
            
            // Display that this item can't be opened
            TitleUtils.showPlayerAlert(player, Formatting.WHITE, ServerText.translatable(player, "claim.block.locked",
                EntityLockUtils.getLockedName(block),
                (chunk == null ? Text.literal("unknown player").formatted(Formatting.LIGHT_PURPLE) : ((IClaimedChunk) chunk).getOwnerName(player, blockPos))
            ));
        }
    }
}
