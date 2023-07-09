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

package net.theelm.sewingmachine.utilities;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.random.Random;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.interfaces.SpawnerMob;
import net.theelm.sewingmachine.base.mixins.Server.ServerWorldAccessor;
import net.theelm.sewingmachine.utilities.mod.SewServer;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import net.minecraft.world.level.ServerWorldProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class EntityUtils {
    public static final int DEFAULT_DESPAWN_TICKS = 6000;
    
    private EntityUtils() {}
    
    public static boolean hasClientBlockData(@NotNull BlockState blockState) {
        Block block = blockState.getBlock();
        if (!blockState.hasBlockEntity())
            return false;
        return blockState.isIn(BlockTags.SIGNS)
            || block == Blocks.PLAYER_HEAD
            || block == Blocks.PLAYER_WALL_HEAD;
    }
    
    /**
     * Check if an Entity is allowed to access the contents of another players Death Chest
     * @param entity The Entity
     * @param deathChestUUID The UUID of the player who owns the death chest
     * @return If the 'entity' is granted access
     */
    public static boolean canEntityTakeDeathChest(@Nullable Entity entity, @NotNull UUID deathChestUUID) {
        return (entity instanceof ServerPlayerEntity player)
            && (player.getUuid().equals(deathChestUUID)
                || player.hasPermissionLevel(OpLevels.KICK_BAN_OP)
                || Permissions.check(player, PermissionNodes.INTERACT_OTHER_DEATHCHEST.getNode()));
    }
    
    /*
     * Get Shop Permissions
     */
    public static boolean isValidShopContainer(@Nullable BlockEntity block) {
        return (block instanceof ChestBlockEntity || block instanceof BarrelBlockEntity) || (block instanceof ShulkerBoxBlockEntity);
    }
    public static @Nullable ShopSignData getAttachedShopSign(@NotNull World world, @NotNull BlockPos storagePos) {
        Set<BlockPos> searchForSigns = new HashSet<>(Collections.singletonList(
            storagePos.up()
        ));
        
        BlockState storageState = world.getBlockState(storagePos);
        BlockEntity storageEntity = world.getBlockEntity(storagePos);
        
        // If is storage block
        if (isValidShopContainer(storageEntity)) {
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
                ShopSignData shopSign = (ShopSignData) blockEntity;
                if (shopSign.getShopType() != null)
                    return shopSign;
            }
        }
        
        return null;
    }
    
    /*
     * Boss Bars
     */
    public static @NotNull BossBar.Color getHealthColor(@NotNull LivingEntity entity) {
        return EntityUtils.getHealthColor(entity.getHealth() / entity.getMaxHealth());
    }
    public static @NotNull BossBar.Color getHealthColor(float percentage) {
        if (percentage >= 0.6)
            return BossBar.Color.GREEN;
        if (percentage >= 0.3)
            return BossBar.Color.YELLOW;
        return BossBar.Color.RED;
    }
    public static void updateHealthBar(@NotNull LivingEntity entity, @NotNull BossBar bossBar) {
        float percentage = entity.getHealth() / entity.getMaxHealth();
        if (percentage != bossBar.getPercent()) {
            // Update the bar
            bossBar.setPercent(percentage);
            
            // Update the color of the bar
            bossBar.setColor(EntityUtils.getHealthColor(percentage));
        }
    }
    public static @NotNull ServerBossBar createHealthBar(@NotNull LivingEntity entity) {
        float percentage = entity.getHealth() / entity.getMaxHealth();
        
        MutableText name = null;
        if (entity instanceof PlayerEntity)
            name = Text.literal("Player ")
                .append(entity.getDisplayName())
                .formatted(Formatting.WHITE);
        else name = FormattingUtils.deepCopy(entity.getDisplayName());
        
        // Create the health bar
        ServerBossBar healthBar = new ServerBossBar(
            name,
            EntityUtils.getHealthColor(percentage),
            BossBar.Style.NOTCHED_10 // Set the notches to the hearts count
        );
        
        // Set the health bar percentage
        healthBar.setPercent(percentage);
        
        return healthBar;
    }
    
    /*
     * Mob spawners
     */
    public static boolean canBeSpawnered(@NotNull Entity entity) {
        if (entity instanceof SpawnerMob spawnerMob && spawnerMob.checkIfFromSpawner())
            return false;
        return EntityUtils.canBeSpawnered(entity.getType());
    }
    public static <T extends Entity> boolean canBeSpawnered(@NotNull EntityType<T> type) {
        return type.isSummonable() && !EntityUtils.preventSpawnered(type);
    }
    public static <T extends Entity> boolean preventSpawnered(@NotNull EntityType<T> type) {
        if (EntityUtils.cannotBeSpawnered(type))
            return true;
        List<EntityType<?>> blacklist = SewConfig.get(SewBaseConfig.SPAWNER_ABSORB_BLACKLIST);
        for (EntityType<?> blacklisted : blacklist) {
            if (Objects.equals(blacklisted, type))
                return true;
        }
        
        return false;
    }
    public static <T extends Entity> boolean cannotBeSpawnered(@NotNull EntityType<T> type) {
        return type == EntityType.ELDER_GUARDIAN
            || type == EntityType.WITHER_SKULL
            || type == EntityType.DRAGON_FIREBALL;
    }
    
    public static boolean areBreedable(@NotNull Entity entityA, @NotNull Entity entityB) {
        if (entityA == entityB)
            return false;
        if (entityA.getClass() != entityB.getClass())
            return false;
        if (entityA instanceof AnimalEntity animalA && entityB instanceof AnimalEntity animalB) {
            if (animalA.getBreedingAge() != 0 || animalB.getBreedingAge() != 0)
                return false;
            if (!animalA.canEat() || !animalB.canEat())
                return false;
            if (animalA.isAiDisabled() || animalB.isAiDisabled())
                return false;
        }
        return true;
    }
    
    /*
     * Generic Entities
     */
    public static @Nullable UUID getUUID(@Nullable Entity entity) {
        if (entity == null) return null;
        return entity.getUuid();
    }
    
    public static @Nullable UUID getOwner(@Nullable Entity entity) {
        if (entity == null)
            return null;
        if (entity instanceof TameableEntity tameable)
            return tameable.getOwnerUuid();
        if (entity instanceof AbstractHorseEntity horse)
            return horse.getOwnerUuid();
        return null;
    }
    
    /*
     * Get Online Players
     */
    public static @Nullable ServerPlayerEntity getPlayer(@NotNull String username) {
        // If the player is online
        return SewServer.get()
            .getPlayerManager()
            .getPlayer(username);
    }
    public static @Nullable ServerPlayerEntity getPlayer(@NotNull UUID playerId) {
        // If the player is online
        return SewServer.get()
            .getPlayerManager()
            .getPlayer(playerId);
    }
    
    public static @NotNull PlayerAbilities modifiedAbilities(@NotNull PlayerEntity player, @NotNull Consumer<PlayerAbilities> consumer) {
        PlayerAbilities playerAbilities = player.getAbilities();
        PlayerAbilities copiedAbilities = new PlayerAbilities();
        NbtCompound serialized = new NbtCompound();
        playerAbilities.writeNbt(serialized);
        copiedAbilities.readNbt(serialized);
        
        consumer.accept(copiedAbilities);
        
        return copiedAbilities;
    }
    
    /*
     * Wandering Traders
     */
    public static void wanderingTraderArrival(@NotNull WanderingTraderEntity trader) {
        MessageUtils.sendToAll(Text.literal("The ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal("Wandering Trader").formatted(Formatting.BOLD, Formatting.BLUE))
            .append(" has arrived at ")
            .append(EntityUtils.getEntityRegionName(trader))
            .append("."));
        EntityUtils.wanderingTraderTimeRemaining(trader);
    }
    public static void wanderingTraderTimeRemaining(@NotNull WanderingTraderEntity trader) {
        if (trader.getDespawnDelay() <= 0)
            return;
        MessageUtils.sendToAll(Text.literal("The ")
            .formatted(Formatting.YELLOW, Formatting.ITALIC)
            .append(Text.literal("Wandering Trader").formatted(Formatting.BOLD, Formatting.BLUE))
            .append(" will depart in ")
            .append(MessageUtils.formatNumber((double)trader.getDespawnDelay() / 1200, " minutes"))
            .append("."));
    }
    public static void wanderingTraderDeparture(@NotNull WanderingTraderEntity trader) {
        BlockUtils.extinguishNearbyLightSources((ServerWorld) trader.getWorld(), trader.getBlockPos());
        MessageUtils.sendToAll(Text.literal("The ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal("Wandering Trader").formatted(Formatting.BOLD, Formatting.BLUE))
            .append(" has departed from ")
            .append(EntityUtils.getEntityRegionName(trader))
            .append("."));
    }
    public static @NotNull Text wanderingTraderDepartureTime(@NotNull WanderingTraderEntity trader) {
        int despawn = trader.getDespawnDelay();
        
        return (despawn >= 1200 ? Text.literal((despawn / 1200) + "m") : Text.literal((despawn / 20) + "s"))
            .formatted(Formatting.AQUA);
    }
    public static boolean isEntityWanderingTrader(@NotNull Entity entity) {
        return Objects.equals(entity.getUuid(), EntityUtils.getWanderingTraderId(entity.getServer()));
    }
    public static @Nullable UUID getWanderingTraderId(@NotNull World world) {
        return EntityUtils.getWanderingTraderId(world.getServer());
    }
    public static @Nullable UUID getWanderingTraderId(@Nullable MinecraftServer server) {
        if (server == null)
            return null;
        
        // Wandering Trader ID is only stored in the overworld
        ServerWorld overworld = server.getOverworld();
        ServerWorldProperties properties = ((ServerWorldAccessor)overworld).getProperties();
        return properties.getWanderingTraderId();
    }
    
    @Deprecated(forRemoval = true)
    public static Text getEntityRegionName(@NotNull Entity entity) {
        // Moved to EntityLockUtils
        return Text.of("");
    }
    
    /*
     * World
     */
    public static boolean isInOverworld(@NotNull Entity entity) {
        return EntityUtils.isIn(entity, World.OVERWORLD);
    }
    public static boolean isNotInOverworld(@NotNull Entity entity) {
        return !EntityUtils.isInOverworld(entity);
    }
    public static boolean isInNether(@NotNull Entity entity) {
        return EntityUtils.isIn(entity, World.NETHER);
    }
    public static boolean isNotInNether(@NotNull Entity entity) {
        return !EntityUtils.isInNether(entity);
    }
    public static boolean isInTheEnd(@NotNull Entity entity) {
        return EntityUtils.isIn(entity, World.END);
    }
    public static boolean isNotInTheEnd(@NotNull Entity entity) {
        return !EntityUtils.isInTheEnd(entity);
    }
    public static boolean isNotFightingDragon(@NotNull Entity entity) {
        if (EntityUtils.isNotInTheEnd(entity))
            return true;
        double x = entity.getX();
        double z = entity.getZ();
        return  (x > 200 || x < -200 || z > 200 || z < -200);
    }
    private static boolean isIn(@NotNull Entity player, RegistryKey<World> world) {
        return player.getWorld()
            .getRegistryKey()
            .equals(world);
    }
    
    /*
     * Misc
     */
    public static int hitWithLightning(@NotNull Collection<? extends Entity> targets) {
        int i = 0;
        for (Entity entity : targets)
            if (EntityUtils.hitWithLightning(entity))
                i++;
        return i;
    }
    public static <T extends Entity> boolean hitWithLightning(@NotNull T entity) {
        if (entity.getEntityWorld() instanceof ServerWorld world && !entity.isSpectator()) {
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.setPos(entity.getX(), entity.getY(), entity.getZ());
            
            world.spawnEntity(lightning);
            return true;
        }
        return false;
    }
    
    public static int extinguish(@NotNull Collection<? extends Entity> targets) {
        int i = 0;
        for (Entity entity : targets)
            if (EntityUtils.extinguish(entity))
                i++;
        return i;
    }
    public static <T extends Entity> boolean extinguish(@NotNull T entity) {
        if (entity.getEntityWorld() instanceof ServerWorld) {
            if (entity.isOnFire() && !entity.isSpectator()) {
                entity.setOnFireFor(0);
                entity.setFireTicks(0);
                return true;
            }
        }
        return false;
    }
    
    /*
     * Disconnect Players
     */
    
    public static void kickAllPlayers() {
        EntityUtils.kickAllPlayers(null);
    }
    public static void kickAllPlayers(@Nullable Text reason) {
        MinecraftServer server = SewServer.get();
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
            return Text.literal(CasingUtils.sentence(name.toString()));
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
