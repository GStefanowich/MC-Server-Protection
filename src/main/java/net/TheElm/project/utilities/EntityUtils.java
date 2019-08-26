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

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.vehicle.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public final class EntityUtils {
    
    private EntityUtils() {}
    
    // Default lock sound
    public static SoundEvent getDefaultLockSound() {
        return SoundEvents.BLOCK_WOODEN_DOOR_OPEN;
    }
    
    /*
     * Get Entity Sounds
     */
    public static SoundEvent getLockSound(Block block) {
        if ( block instanceof BarrelBlock )
            return SoundEvents.BLOCK_FENCE_GATE_CLOSE;
        if ( block instanceof LoomBlock || block instanceof CartographyTableBlock )
            return SoundEvents.UI_LOOM_SELECT_PATTERN;
        if ( block instanceof AnvilBlock || block instanceof GrindstoneBlock )
            return SoundEvents.BLOCK_ANVIL_PLACE;
        if ( block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock || block instanceof DropperBlock || block instanceof HopperBlock )
            return SoundEvents.BLOCK_IRON_DOOR_OPEN;
        if ( block instanceof BeaconBlock || block instanceof EnchantingTableBlock )
            return SoundEvents.ENTITY_PLAYER_LEVELUP;
        if ( block instanceof StonecutterBlock || block instanceof SmokerBlock )
            return SoundEvents.BLOCK_SCAFFOLDING_BREAK;
        if ( block instanceof FletchingTableBlock )
            return SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE;
        if ( block instanceof CraftingTableBlock )
            return SoundEvents.BLOCK_WOOD_BREAK;
        if ( block instanceof ShulkerBoxBlock )
            return SoundEvents.BLOCK_SHULKER_BOX_CLOSE;
        return EntityUtils.getDefaultLockSound();
    }
    @Nullable
    public static SoundEvent getLockSound(Entity entity) {
        // Locked blocks
        if (entity instanceof MinecartEntity || entity instanceof HopperMinecartEntity || entity instanceof FurnaceMinecartEntity)
            return SoundEvents.BLOCK_IRON_DOOR_OPEN;
        if (entity instanceof TntMinecartEntity)
            return SoundEvents.ENTITY_PARROT_IMITATE_CREEPER;
        if (entity instanceof ChestMinecartEntity || entity instanceof ArmorStandEntity)
            return EntityUtils.getDefaultLockSound();
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
        return null;
    }
    
    /*
     * Get Entity Names
     */
    public static Text getLockedName(Block block) {
        return new TranslatableText( block.getTranslationKey() )
            .formatted(Formatting.AQUA);
    }
    public static Text getLockedName(Entity entity) {
        return entity.getName().copy()
            .formatted(Formatting.AQUA);
    }
    
}
