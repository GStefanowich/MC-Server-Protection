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
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.ClaimPermissions;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.ShopSignBlockEntity;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public final class EntityUtils {
    
    private EntityUtils() {}
    
    // Default lock sound
    public static SoundEvent getDefaultLockSound() {
        return SoundEvents.BLOCK_WOODEN_DOOR_OPEN;
    }
    
    /*
     * Get Entity Sounds
     */
    public static SoundEvent getLockSound(@NotNull Block block) {
        return EntityUtils.getLockSound(block, null, null);
    }
    public static SoundEvent getLockSound(@NotNull Block block, @Nullable BlockState blockState, @Nullable BlockEntity blockEntity) {
        if (blockEntity != null) {
            if ( blockEntity instanceof BeehiveBlockEntity) {
                BeehiveBlockEntity hive = ((BeehiveBlockEntity) blockEntity);
                if (!hive.hasNoBees())
                    return SoundEvents.BLOCK_BEEHIVE_WORK;
                return (blockState != null && (BeehiveBlockEntity.getHoneyLevel(blockState) > 0) ? SoundEvents.BLOCK_BEEHIVE_DRIP : EntityUtils.getDefaultLockSound());
            }
        }
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
        if ( block instanceof BeehiveBlock )
            return SoundEvents.BLOCK_BEEHIVE_DRIP;
        return EntityUtils.getDefaultLockSound();
    }
    public static @Nullable SoundEvent getLockSound(Entity entity) {
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
        // Nether
        if (entity instanceof PiglinEntity)
            return SoundEvents.ENTITY_PIGLIN_AMBIENT;
        return null;
    }
    
    /*
     * Get Lock Permissions
     */
    public static @Nullable ClaimPermissions getLockPermission(BlockEntity block) {
        if (block instanceof JukeboxBlockEntity)
            return ClaimPermissions.STORAGE;
        if ( block instanceof LockableContainerBlockEntity )
            return ClaimPermissions.STORAGE;
        return null;
    }
    public static @Nullable ClaimPermissions getLockPermission(@NotNull Block block) {
        // Crafting Blocks
        if ( block instanceof FletchingTableBlock )
            return ClaimPermissions.CRAFTING;
        if ( block instanceof SmithingTableBlock)
            return ClaimPermissions.CRAFTING;
        if ( block instanceof CraftingTableBlock )
            return ClaimPermissions.CRAFTING;
        if ( block instanceof EnchantingTableBlock )
            return ClaimPermissions.CRAFTING;
        if ( block instanceof GrindstoneBlock )
            return ClaimPermissions.CRAFTING;
        if ( block instanceof LoomBlock )
            return ClaimPermissions.CRAFTING;
        if ( block instanceof StonecutterBlock )
            return ClaimPermissions.CRAFTING;
        if ( block instanceof CartographyTableBlock )
            return ClaimPermissions.CRAFTING;
        // Activation Blocks
        if ( block instanceof NoteBlock )
            return ClaimPermissions.BLOCKS;
        // Storage Blocks
        if ( block instanceof BeaconBlock )
            return ClaimPermissions.STORAGE;
        if ( block instanceof AnvilBlock )
            return ClaimPermissions.STORAGE;
        if ( block instanceof BellBlock )
            return ClaimPermissions.STORAGE;
        if ( block instanceof LecternBlock )
            return ClaimPermissions.STORAGE;
        if ( block instanceof FlowerPotBlock )
            return ClaimPermissions.STORAGE;
        if ( block instanceof BeehiveBlock )
            return ClaimPermissions.STORAGE;
        return null;
    }
    
    /*
     * Get Shop Permissions
     */
    public static boolean isValidShopContainer(BlockEntity block) {
        return (block instanceof ChestBlockEntity || block instanceof BarrelBlockEntity);
    }
    public static @Nullable ShopSignBlockEntity getAttachedShopSign(World world, BlockPos storagePos) {
        Set<BlockPos> searchForSigns = new HashSet<>(Collections.singletonList(
            storagePos.up()
        ));
        
        BlockState storageState = world.getBlockState( storagePos );
        BlockEntity storageEntity = world.getBlockEntity( storagePos );
        
        // If is storage block
        if (isValidShopContainer( storageEntity )) {
            // Chest if chest is a double block
            if ((storageEntity instanceof ChestBlockEntity) && (storageState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE)) {
                Direction facing = storageState.get(ChestBlock.FACING);
                Direction othersDirection = storageState.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
                BlockPos otherPos = storagePos.offset(othersDirection);
                
                // All sides of the double chest
                searchForSigns.addAll(Arrays.asList(
                    otherPos.up(),
                    storagePos.offset(facing),
                    otherPos.offset(facing),
                    storagePos.offset(facing.getOpposite()),
                    otherPos.offset(facing.getOpposite()),
                    storagePos.offset(othersDirection.getOpposite()),
                    otherPos.offset(othersDirection)
                ));
                
            } else {
                searchForSigns.addAll(Arrays.asList(
                    storagePos.offset(Direction.NORTH),
                    storagePos.offset(Direction.EAST),
                    storagePos.offset(Direction.SOUTH),
                    storagePos.offset(Direction.WEST)
                ));
            }
            
            // For all search positions
            for (BlockPos searchPos : searchForSigns) {
                BlockEntity blockEntity = world.getBlockEntity(searchPos);
                if (!(blockEntity instanceof SignBlockEntity))
                    continue;
                ShopSignBlockEntity shopSign = (ShopSignBlockEntity) blockEntity;
                if (shopSign.getShopType() != null)
                    return shopSign;
            }
        }
        
        return null;
    }
    
    /*
     * Get Entity Names
     */
    public static Text getLockedName(@NotNull Block block) {
        return new TranslatableText( block.getTranslationKey() )
            .formatted(Formatting.AQUA);
    }
    public static Text getLockedName(@NotNull Entity entity) {
        return entity.getName().copy()
            .formatted(Formatting.AQUA);
    }
    
    /*
     * Generic Entities
     */
    public static @Nullable UUID getUUID(@Nullable Entity entity) {
        if (entity == null) return null;
        return entity.getUuid();
    }
    
    /*
     * Player methods
     */
    public static void resendInventory(@NotNull PlayerEntity player) {
        if (player instanceof ServerPlayerEntity)
            EntityUtils.resendInventory((ServerPlayerEntity)player);
    }
    public static void resendInventory(@NotNull ServerPlayerEntity player) {
        player.updateCursorStack();
    }
    
    /*
     * Get Online Players
     */
    public static @Nullable ServerPlayerEntity getPlayer(@NotNull String username) {
        // If the player is online
        return ServerCore.get()
            .getPlayerManager()
            .getPlayer(username);
    }
    public static @Nullable ServerPlayerEntity getPlayer(@NotNull UUID playerId) {
        // If the player is online
        return ServerCore.get()
            .getPlayerManager()
            .getPlayer(playerId);
    }
    
    /*
     * Wandering Traders
     */
    public static void wanderingTraderArrival(@NotNull WanderingTraderEntity trader) {
        MessageUtils.sendToAll(new LiteralText("The ")
            .formatted(Formatting.YELLOW)
            .append(new LiteralText("Wandering Trader").formatted(Formatting.BOLD, Formatting.BLUE))
            .append(" has arrived at ")
            .append(EntityUtils.getEntityRegionName(trader))
            .append("."));
        EntityUtils.wanderingTraderTimeRemaining(trader);
    }
    public static void wanderingTraderTimeRemaining(@NotNull WanderingTraderEntity trader) {
        if (trader.getDespawnDelay() <= 0)
            return;
        MessageUtils.sendToAll(new LiteralText("The ")
            .formatted(Formatting.YELLOW, Formatting.ITALIC)
            .append(new LiteralText("Wandering Trader").formatted(Formatting.BOLD, Formatting.BLUE))
            .append(" will depart in ")
            .append(MessageUtils.formatNumber((double)trader.getDespawnDelay() / 1200, " minutes"))
            .append("."));
    }
    public static void wanderingTraderDeparture(@NotNull WanderingTraderEntity trader) {
        MessageUtils.sendToAll(new LiteralText("The ")
            .formatted(Formatting.YELLOW)
            .append(new LiteralText("Wandering Trader").formatted(Formatting.BOLD, Formatting.BLUE))
            .append(" has departed from ")
            .append(EntityUtils.getEntityRegionName(trader))
            .append("."));
    }
    public static Text getEntityRegionName(@NotNull Entity entity) {
        World world = entity.getEntityWorld();
        IClaimedChunk chunk = (IClaimedChunk) world.getWorldChunk(entity.getBlockPos());
        
        if (CoreMod.SPAWN_ID.equals(chunk.getOwner()))
            return new LiteralText("Spawn")
                .formatted(Formatting.AQUA);
        
        ClaimantTown town;
        if ((town = chunk.getTown()) != null)
            return town.getName()
                .styled(style -> style.withHoverEvent(town.getHoverText()).withColor(Formatting.AQUA));
        
        if (chunk.getOwner() != null)
            return new LiteralText("")
                .append(chunk.getOwnerName()
                    .formatted(Formatting.AQUA))
                .append("'s");
        
        return MessageUtils.xyzToText(entity.getBlockPos())
            .formatted(Formatting.AQUA);
    }
    
    /*
     * World
     */
    public static boolean isInOverworld(@NotNull Entity player) {
        return EntityUtils.isIn(player, World.OVERWORLD);
    }
    public static boolean isNotInOverworld(@NotNull Entity player) {
        return !EntityUtils.isInOverworld(player);
    }
    public static boolean isInNether(@NotNull Entity player) {
        return EntityUtils.isIn(player, World.NETHER);
    }
    public static boolean isNotInNether(@NotNull Entity player) {
        return !EntityUtils.isInNether(player);
    }
    public static boolean isInTheEnd(@NotNull Entity player) {
        return EntityUtils.isIn(player, World.END);
    }
    public static boolean isNotInTheEnd(@NotNull Entity player) {
        return !EntityUtils.isInTheEnd(player);
    }
    private static boolean isIn(@NotNull Entity player, RegistryKey<World> world) {
        return player.world.getRegistryKey()
            .equals(world);
    }
    
    /*
     * Disconnect Players
     */
    public static void kickAllPlayers() {
        EntityUtils.kickAllPlayers( null );
    }
    public static void kickAllPlayers(@Nullable Text reason) {
        MinecraftServer server = ServerCore.get();
        PlayerManager manager = server.getPlayerManager();
        if (reason == null)
            manager.disconnectAllPlayers();
        else new ArrayList<>(manager.getPlayerList()).forEach((player) -> player.networkHandler.disconnect( reason ));
    }
    
    public static final class Naming {
        private Naming() {}
        
        private static final NamedVillagers DESERT = new DesertVillagers();
        private static final NamedVillagers JUNGLE = new JungleVillagers();
        private static final NamedVillagers PLAINS = new PlainsVillagers();
        private static final NamedVillagers SAVANNA = new SavannaVillagers();
        private static final NamedVillagers SNOW = new SnowVillagers();
        private static final NamedVillagers SWAMP = new SwampVillagers();
        private static final NamedVillagers TAIGA = new TaigaVillagers();
        
        public static @Nullable Text create(Random random, VillagerData data, int maxSyllables) {
            int syllables = IntUtils.random(random, 2, maxSyllables);
            
            VillagerType type = data.getType();
            NamedVillagers schema;
            if (type.equals(VillagerType.DESERT))
                schema = Naming.DESERT;
            else if (type.equals(VillagerType.JUNGLE))
                schema = Naming.JUNGLE;
            else if (type.equals(VillagerType.PLAINS))
                schema = Naming.PLAINS;
            else if (type.equals(VillagerType.SAVANNA))
                schema = Naming.SAVANNA;
            else if (type.equals(VillagerType.SNOW))
                schema = Naming.SNOW;
            else if (type.equals(VillagerType.SWAMP))
                schema = Naming.SWAMP;
            else if (type.equals(VillagerType.TAIGA))
                schema = Naming.TAIGA;
            else return null;
            
            // Get a set of syllables
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < syllables; i++)
                name.append(schema.get(random, i + 1));
            
            // Convert the name to a Text object
            return new LiteralText(CasingUtils.Sentence(name.toString()));
        }
        
        private static abstract class NamedVillagers {
            private final String[] syllables;
            private final int prefixes;
            private final int suffixes;
            
            protected NamedVillagers(String[] prefix, String[] any, String[] suffix) {
                this.syllables = Stream.of(prefix, any, suffix)
                    .flatMap(Arrays::stream)
                    .toArray(String[]::new);
                this.prefixes = prefix.length;
                this.suffixes = suffix.length;
            }
            protected NamedVillagers(String[] prefix, String[] any) {
                this(prefix, any, new String[0]);
            }
            protected NamedVillagers(String[] list) {
                this(new String[0], list);
            }
            
            public @NotNull String get(Random random, int nth) {
                if (this.syllables.length == 0)
                    return "";
                int min, max;
                
                if ((nth <= 1) && ((this.syllables.length - suffixes) != 0)) {
                    min = 0;
                    max = this.syllables.length - suffixes;
                } else {
                    min = prefixes;
                    max = this.syllables.length;
                }
                
                return this.syllables[IntUtils.random(random, min + 1, max + 1) - 1];
            }
        }
        private static final class DesertVillagers extends NamedVillagers {
            public DesertVillagers() {
                super(new String[] {
                    "ha",
                    "ge",
                    "he"
                }, new String[] {
                    "do",
                    "to",
                    "ga"
                }, new String[] {
                    "ph",
                    "ru",
                    "rt",
                    "hn",
                    "lm"
                });
            }
        }
        private static final class JungleVillagers extends NamedVillagers {
            public JungleVillagers() {
                super(new String[] {
                    "bo"
                }, new String[] {
                    "jo",
                    "ja",
                    "je",
                    "me",
                    "le"
                }, new String[] {
                    "ru",
                    "ul",
                    "e",
                    "ff",
                    "ge"
                });
            }
        }
        private static final class PlainsVillagers extends NamedVillagers {
            public PlainsVillagers() {
                super(new String[] {
                    "fo",
                    "tre",
                    "ou"
                }, new String[] {
                    "ar",
                    "an",
                    "ee",
                    "ur"
                }, new String[] {
                    "b",
                    "a",
                    "o",
                    "di",
                    "wl",
                    "to"
                });
            }
        }
        private static final class SavannaVillagers extends NamedVillagers {
            public SavannaVillagers() {
                super(new String[] {
                    "gr",
                    "ge"
                }, new String[] {
                    "ee",
                    "eg",
                    "ag"
                }, new String[] {
                    "ve",
                    "o",
                    "off"
                });
            }
        }
        private static final class SnowVillagers extends NamedVillagers {
            public SnowVillagers() {
                super(new String[] {
                    "he",
                    "ka",
                    "co",
                    "wa"
                }, new String[] {
                    "in",
                    "di"
                }, new String[] {
                    "le",
                    "ne",
                    "na",
                    "gl"
                });
            }
        }
        private static final class SwampVillagers extends NamedVillagers {
            public SwampVillagers() {
                super(new String[] {
                    "gh",
                    "ca",
                    "pa"
                }, new String[] {
                    "ma",
                    "ou"
                }, new String[] {
                    "ost",
                    "am"
                });
            }
        }
        private static final class TaigaVillagers extends NamedVillagers {
            public TaigaVillagers() {
                super(new String[] {
                    "za",
                    "an",
                    "le"
                }, new String[] {
                    "oo",
                    "ee",
                    "wa"
                }, new String[] {
                    "ch",
                    "tt"
                });
            }
        }
    }
}
