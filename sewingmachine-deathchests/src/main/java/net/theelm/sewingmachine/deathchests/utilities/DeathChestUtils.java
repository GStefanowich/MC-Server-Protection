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

package net.theelm.sewingmachine.deathchests.utilities;

import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.deathchests.config.SewDeathConfig;
import net.theelm.sewingmachine.interfaces.BackpackCarrier;
import net.theelm.sewingmachine.deathchests.interfaces.PlayerCorpse;
import net.theelm.sewingmachine.base.objects.PlayerBackpack;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;

public final class DeathChestUtils {
    private DeathChestUtils() {}
    
    public static @Nullable BlockPos getChestPosition(@NotNull final World world, @NotNull final BlockPos deathPoint) {
        BlockPos out;
        
        // Get the max Y (Spawn Height)
        int maxY = SewConfig.get(SewDeathConfig.MAX_DEATH_ELEVATION);
        if (maxY < 0)
            maxY = world.getDimension().height();
        
        // Get the max upper height of the death spawn point
        int upper = Collections.min(Arrays.asList(world.getTopY(), deathPoint.getY() + maxY));
        
        // Get the max X/Z (Spawn Radius)
        int maxX = SewConfig.get(SewDeathConfig.MAX_DEATH_SCAN);
        int maxI = 1 + ((maxX * maxX) * 4) + (maxX * 4);
        
        for ( int y = deathPoint.getY(); y < upper; y++ ) {
            int x = 0;
            int z = 0;
            int dX = 0;
            int dZ = -1;
            
            for (int i = 0; i < maxI; i++) {
                BlockPos check = new BlockPos(x + deathPoint.getX(), y, z + deathPoint.getZ());
                if ((out = DeathChestUtils.isValid(world, check)) != null)
                    return out.down();
                if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
                    maxX = dX;
                    dX = -dZ;
                    dZ = maxX;
                }
                x += dX;
                z += dZ;
            }
        }
        
        return null;
    }
    private static @Nullable BlockPos isValid(@NotNull final World world, @NotNull final BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        
        // If AIR, A-O-KAY
        if (block.equals(Blocks.AIR) || block.equals(Blocks.CAVE_AIR) || (block instanceof SlabBlock && world.getBlockState(blockPos).get(SlabBlock.TYPE) == SlabType.BOTTOM))
            return blockPos;
        
        // If WATER, Sink
        if (block.equals(Blocks.WATER)) {
            BlockPos seaFloor = blockPos;
            do {
                seaFloor = seaFloor.down();
            } while ((world.getBlockState(seaFloor).getBlock().equals(Blocks.WATER)) || (world.getFluidState(seaFloor).getFluid() == Fluids.WATER));
            return seaFloor.up(); // Get the block ABOVE the sea floor
        }
        
        // Return NULL if no valid position was found
        return null;
    }
    
    public static boolean createDeathChestFor(@NotNull final PlayerEntity player, @NotNull BlockPos deathPos) {
        final PlayerInventory inventory = player.getInventory();
        final PlayerBackpack backpack = ((BackpackCarrier)player).getBackpack();
        final World world = player.getWorld();
        final DimensionType dimension = world.getDimension();
        
        final int elevation = deathPos.getY();
        final int minimum = dimension.minY();
        final int maximum = minimum + dimension.height();
        
        if (elevation < minimum) // If in the void
            deathPos = new BlockPos(deathPos.getX(), minimum + 1, deathPos.getZ());
        else if (elevation > maximum) // If over the void
            deathPos = new BlockPos(deathPos.getX(), maximum, deathPos.getZ());
        
        // Check if the ground is a liquid, and move up
        BlockState groundBlockState = world.getBlockState(deathPos);
        boolean skipGroundBlock = !(groundBlockState.isLiquid() && (!groundBlockState.getBlock().equals(Blocks.WATER)));
        
        final BlockPos chestPos;
        if (!skipGroundBlock) {
            // Shift up by one if block is a fluid (and require a dirt block)
            chestPos = deathPos.offset(Direction.UP); // If block is air (require a dirt block)
        } else {
            chestPos = deathPos;
            
            Block groundBlock = groundBlockState.getBlock();
            skipGroundBlock = !(groundBlock.equals(Blocks.AIR) || groundBlock.equals(Blocks.CAVE_AIR));
        }
        
        /*
         * Create our armor stand
         */
        
        // Create the armor stands entity
        ArmorStandEntity corpse = DeathChestUtils.createFakeCorpse(world, chestPos, player);
        
        /*
         * Add our items to the "Chest"
         */
        NbtList inventoryTag = DeathChestUtils.collectInventoryTags(inventory);
        NbtList backpackTag = DeathChestUtils.collectInventoryTags(backpack);
        
        // Set the contents of the item stand
        ((PlayerCorpse)corpse).setCorpseData(
            player.getUuid(),
            inventoryTag,
            backpackTag
        );
        
        // Backup data
        NbtCompound file = new NbtCompound();
        file.putLong("xp", player.totalExperience);
        file.put("inventory", inventoryTag);
        file.put("backpack", backpackTag);
        
        // TODO: Write "file" tag to disk to recover past deaths
        
        // Print the death chest coordinates
        if (SewConfig.get(SewDeathConfig.PRINT_DEATH_CHEST_LOC))
            player.sendMessage(TranslatableServerSide.text(player, "player.death_chest.location", Text.literal(chestPos.getX() + ", " + (chestPos.getY() + 1 ) + ", " + chestPos.getZ()).formatted(Formatting.AQUA)));
        CoreMod.logInfo("Death chest for " + player.getName().getString() + " spawned at " + MessageUtils.xyzToString(chestPos.offset(Direction.UP, 1)));
        
        // Add the entity to the world
        return ( skipGroundBlock || world.setBlockState(chestPos, Blocks.DIRT.getDefaultState()) )
            && world.spawnEntity(corpse) // Spawn the Armor stand into the World
            && corpse.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 1000000, 1, false, true )); // Apply a visual appearance to the Armor stand
    }
    public static void createDeathSnapshotFor(@NotNull final PlayerEntity player) {
        /*final String deathName = player.getEntityName() + System.currentTimeMillis() + "_" + MessageUtils.xyzToString(player.getBlockPos(), "_");
        MessageUtils.consoleToOps(Text.literal("Created new Death Chest for player ")
            .append(player.getDisplayName().shallowCopy())
            .append(" (" + deathName + ")"));*/
    }
    
    private static @NotNull ArmorStandEntity createFakeCorpse(@NotNull final World world, @NotNull final BlockPos chestPos, @NotNull final LivingEntity copyOf) {
        // Set the stands basic attributes (From tags)
        NbtCompound entityData = new NbtCompound();
        entityData.putBoolean("NoBasePlate", true);
        entityData.putBoolean("Invulnerable", true);
        entityData.putBoolean("Invisible", true);
        entityData.putBoolean("NoGravity", true);
        entityData.putInt("DisabledSlots", 2039583);
        
        // Create the armor stands entity
        ArmorStandEntity corpse = new ArmorStandEntity(
            world,
            chestPos.getX() + 0.5D,
            chestPos.getY() - (world.getBlockState(chestPos.up()).getBlock() instanceof SlabBlock ? 0 : 0.5D),
            chestPos.getZ() + 0.5D
        );
        
        // Apply data from the NbtCompound
        corpse.readCustomDataFromNbt(entityData);
        
        corpse.setInvulnerable(true);
        corpse.setNoGravity(true);
        corpse.setInvisible(true);
        
        // Set the stands basic attributes
        corpse.setPitch(copyOf.getPitch());
        corpse.setYaw(copyOf.getYaw());
        
        corpse.setCustomName(Text.literal("")
            .styled((s) -> {
                // Set our players name
                Text customName = copyOf.getDisplayName();
                TextColor color;
                if (customName == null || (color = customName.getStyle().getColor()) == null)
                    color = TextColor.fromFormatting(Formatting.GOLD);
                return s.withColor(color);
            }).append(copyOf.getDisplayName().copyContentOnly()).append("'s Corpse"));
        corpse.setCustomNameVisible(true);
        
        // Set the armor stands head
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        NbtCompound headData = head.getOrCreateNbt();
        
        String playerName = copyOf.getEntityName();
        headData.putString("SkullOwner", playerName);
        
        // Set the armor stands equipped item
        corpse.equipStack(EquipmentSlot.HEAD, head);
        corpse.setStackInHand(Hand.OFF_HAND, copyOf.getMainHandStack().copy());
        corpse.setStackInHand(Hand.MAIN_HAND, copyOf.getOffHandStack().copy());
        
        // Set the pose
        corpse.setHeadRotation(new EulerAngle(334.0f,37.0f,0.0f));
        corpse.setLeftArmRotation(new EulerAngle(325.0f,356.0f,265.0f));
        corpse.setRightArmRotation(new EulerAngle(0.0f,0.0f,82.0f));
        
        return corpse;
    }
    private static @NotNull NbtList collectInventoryTags(@Nullable Inventory inventory) {
        NbtList list = new NbtList();
        if (inventory != null) {
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.removeStack(i);
                if (stack.getItem().equals(Items.AIR))
                    continue;
                list.add(stack.writeNbt(new NbtCompound()));
            }
        }
        return list;
    }
}
