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

package net.TheElm.project.utilities;

import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.interfaces.PlayerCorpse;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;

public final class DeathChestUtils {
    
    private DeathChestUtils() {}
    
    @Nullable
    public static BlockPos getChestPosition(World world, BlockPos deathPoint) {
        BlockPos out = null;
        
        int tmp = SewingMachineConfig.INSTANCE.MAX_DEATH_SCAN.get();
        int maxI = 1 + ((tmp * tmp) * 4) + (tmp * 4);
        
        int upper = Collections.min(Arrays.asList( 256, deathPoint.getY() + SewingMachineConfig.INSTANCE.MAX_DEATH_ELEVATION.get() ));
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
                    tmp = dX;
                    dX = -dZ;
                    dZ = tmp;
                }
                x += dX;
                z += dZ;
            }
        }
        
        return out;
    }
    @Nullable
    private static BlockPos isValid(final World world, final BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        // If AIR, A-O-KAY
        if (block.equals(Blocks.AIR) || block.equals(Blocks.CAVE_AIR))
            return blockPos;
        // If WATER, Sink
        if (block.equals(Blocks.WATER)) {
            BlockPos seaFloor = blockPos;
            do {
                seaFloor = seaFloor.down();
            } while ((world.getBlockState(seaFloor).getBlock().equals(Blocks.WATER)) || (world.getFluidState(seaFloor).getFluid() == Fluids.WATER));
            return seaFloor.up(); // Get the block ABOVE the sea floor
        }
        return null;
    }
    public static boolean createDeathChestFor(final PlayerEntity player, BlockPos deathPos, final PlayerInventory inventory) {
        World world = player.world;
        
        if (deathPos.getY() < 0)
            deathPos = new BlockPos( deathPos.getX(), 0, deathPos.getZ() );
        
        final BlockPos chestPos;
        
        // Check if the ground is a liquid, and move up
        BlockState groundBlockState = world.getBlockState(deathPos);
        boolean skipGroundBlock = !(groundBlockState.getMaterial().isLiquid() && (!groundBlockState.getBlock().equals(Blocks.WATER)));
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
        ArmorStandEntity stand = new ArmorStandEntity(
            world,
            chestPos.getX() + 0.5D,
            chestPos.getY() - 0.5D,
            chestPos.getZ() + 0.5D
        );
        
        // Set the stands basic attributes (From tags)
        CompoundTag entityData = new CompoundTag();
        entityData.putBoolean("NoBasePlate", true);
        entityData.putBoolean("Invulnerable", true);
        entityData.putBoolean("Invisible", true);
        entityData.putBoolean("NoGravity", true);
        entityData.putInt("DisabledSlots", 2039583);
        
        stand.readCustomDataFromTag(entityData);
        
        // Set the stands basic attributes
        stand.pitch = player.pitch;
        stand.yaw = player.yaw;
        
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setInvisible(true);
        
        // Set our players name
        String playerName = player.getName().asString();
        stand.setCustomName(new LiteralText(playerName + "'s Corpse").formatted(Formatting.GOLD));
        stand.setCustomNameVisible(true);
        
        // Set the armor stands head
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag headData = head.getOrCreateTag();
        headData.putString("SkullOwner", playerName);
        
        // Set the armor stands equipped item
        stand.setEquippedStack(EquipmentSlot.HEAD, head);
        stand.setStackInHand( Hand.OFF_HAND, inventory.getMainHandStack().copy());
        stand.setStackInHand( Hand.MAIN_HAND, player.getOffHandStack().copy());
        
        // Set the pose
        stand.setHeadRotation(new EulerAngle(334.0f,37.0f,0.0f));
        stand.setLeftArmRotation(new EulerAngle(325.0f,356.0f,265.0f));
        stand.setRightArmRotation(new EulerAngle(0.0f,0.0f,82.0f));
        
        /*
         * Add our items to the "Chest"
         */
        ListTag itemsTag = new ListTag();
        for ( int i = 0; i < inventory.getInvSize(); i++ ) {
            ItemStack stack = inventory.getInvStack( i );
            if (stack.getItem().equals(Items.AIR))
                continue;
            CompoundTag itemTag = stack.toTag(new CompoundTag());
            itemsTag.add(itemTag);
        }
        
        // Set the contents of the item stand
        ((PlayerCorpse) stand).setCorpseData( player.getUuid(), itemsTag );
        
        // Print the death chest coordinates
        if (SewingMachineConfig.INSTANCE.PRINT_DEATH_CHEST_LOC.get()) {
            player.sendMessage(TranslatableServerSide.text(player, "player.death_chest.location", new LiteralText(chestPos.getX() + ", " + (chestPos.getY() + 1 ) + ", " + chestPos.getZ()).formatted(Formatting.AQUA)));
        }
        CoreMod.logInfo( "Death chest for " + playerName + " spawned at " + chestPos.getX() + ", " + (chestPos.getY() + 1) + ", " + chestPos.getZ() );
        
        // Add the entity to the world
        return ( skipGroundBlock || world.setBlockState( deathPos, Blocks.DIRT.getDefaultState() ) ) && world.spawnEntity( stand );
    }
    
}
