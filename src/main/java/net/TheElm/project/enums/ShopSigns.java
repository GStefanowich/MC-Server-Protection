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

package net.TheElm.project.enums;

import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import net.TheElm.project.CoreMod;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.exceptions.ShopBuilderException;
import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.interfaces.ClaimsAccessor;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.LogicalWorld;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.ShopSignData;
import net.TheElm.project.objects.PlayerBackpack;
import net.TheElm.project.objects.ShopStats;
import net.TheElm.project.objects.ticking.ClaimCache;
import net.TheElm.project.objects.ticking.WaystoneSearch;
import net.TheElm.project.protections.BlockRange;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.ColorUtils;
import net.TheElm.project.utilities.DimensionUtils;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.GuideUtils;
import net.TheElm.project.utilities.IntUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.ShopSignBuilder;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.WarpUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.TheElm.project.utilities.text.StyleApplicator;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Objects;

public enum ShopSigns {
    /*
     * Chest is BUYING
     */
    SELL(ShopSignData.APPLICATOR_RED) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            if ((!signBuilder.textMatchItem(creator, signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
                throw new ShopBuilderException(new LiteralText("Sign not formatted correctly.").formatted(Formatting.RED));
            
            LootableContainerBlockEntity container = null;
            if (!( creator.isCreative() || ((container = InventoryUtils.getAttachedChest( signBuilder )) != null)))
                throw new ShopBuilderException(new LiteralText("Could not find storage for sign.").formatted(Formatting.RED));
            
            if (container != null && !ChunkUtils.canPlayerLootChestsInChunk(creator, container.getPos()))
                throw new ShopBuilderException(new LiteralText("Missing permission to access that container.").formatted(Formatting.RED));
            
            // Parse the owner of the sign
            signBuilder.textParseOwner(signBuilder.getLines()[3], creator);
            
            ShopSigns.printCompletedSign(creator, signBuilder, SewConfig.get(SewConfig.SERVER_SALES_TAX));
            return this.renderSign(signBuilder);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            // Second row Count - Item Name
            shop.setSignLine(1, shop.textParseItem());
            // Third Row - Price
            shop.setSignLine(2,
            new LiteralText("for ").formatted(Formatting.BLACK)
                  .append(new LiteralText("$" + shop.getShopItemPrice()).styled(ShopSignData.APPLICATOR_GREEN))
            );
            // Fourth Row - Owner
            shop.setSignLine(3,
                shop.textParseOwner().formatted(Formatting.DARK_GRAY)
            );
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            LootableContainerBlockEntity chest = null;
            Inventory chestInventory = null;
            
            // If shops disabled
            if ( !SewConfig.get(SewConfig.DO_MONEY) )
                return Either.right(true);
            
            // These should NOT be null
            if (((sign.getShopItem()) == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null) || (sign.getShopItemDisplay() == null))
                return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
            
            // Check if the attached chest exists
            if (CoreMod.SPAWN_ID.equals(sign.getShopOwner()) || ((chest = InventoryUtils.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.self_sell"));
                /*
                 * Check if chest is valid
                 */
                if ((chest != null)) {
                    chestInventory = InventoryUtils.getInventoryOf(player.getEntityWorld(), chest.getPos());
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                        return Either.left(TranslatableServerSide.text(player, "shop.error.chest_open"));
                    
                    // If player does not have any of item
                    if (InventoryUtils.getInventoryCount(player.getInventory(), sign::itemMatchPredicate) < sign.getShopItemCount())
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_player", sign.getShopItemDisplay()));
                }
                /*
                 * Transfer the items from chest to player
                 */
                try {
                    // Take shop keepers money
                    if (!(sign.getShopOwner().equals(CoreMod.SPAWN_ID) || MoneyUtils.takePlayerMoney(sign.getShopOwner(), sign.getShopItemPrice())))
                        return Either.left(TranslatableServerSide.text(player, "shop.error.money_chest"));
                    
                    // Put players item into chest
                    if (!InventoryUtils.playerToChest(player, signPos, player.getInventory(), chestInventory, sign::itemMatchPredicate, sign.getShopItemCount(), true)) {
                        // Refund the shopkeeper
                        if (!(sign.getShopOwner().equals(CoreMod.SPAWN_ID)))
                            MoneyUtils.givePlayerMoney(sign.getShopOwner(), sign.getShopItemPrice());
                        
                        // Error message
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_player", sign.getShopItemDisplay()));
                    }
                    
                    // Give player money for item
                    MoneyUtils.givePlayerMoney(player, sign.getShopItemPrice());
                    
                    // Get shop owner
                    ClaimCache claimCache = ((ClaimsAccessor)server).getClaimManager();
                    ClaimantPlayer permissions = claimCache.getPlayerClaim(sign.getShopOwner());
                    
                    // Tell the player
                    TitleUtils.showPlayerAlert(
                        player,
                        Formatting.YELLOW,
                        new LiteralText("You sold "),
                        new LiteralText(FormattingUtils.format( sign.getShopItemCount() ) + " ").formatted(Formatting.AQUA),
                        new TranslatableText(sign.getShopItemTranslationKey()).formatted(Formatting.AQUA),
                        new LiteralText(" to "),
                        permissions.getName().formatted(Formatting.AQUA)
                    );
                    
                    // Log the event
                    CoreMod.logInfo(player.getName().getString() + " sold " + FormattingUtils.format(sign.getShopItemCount()) + " " + sign.getShopItemIdentifier() + " for $" + FormattingUtils.format(sign.getShopItemPrice()) + " to " + permissions.getName().getString());
                    player.increaseStat(ShopStats.SHOP_TYPE_SOLD.getOrCreateStat(sign.getShopItem()), sign.getShopItemCount());
                    
                    return Either.right(true);
                    
                } catch ( NbtNotFoundException e ) {
                    CoreMod.logError( "Failed to give " + sign.getShopItemPrice() + " money to \"" + sign.getShopOwner() + "\" (Maybe they haven't joined the server?)." );
                    // If a database problem occurs
                    return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
                } catch ( NotEnoughMoneyException e ) {
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_chest"));
                }
            }
            return Either.right(false);
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.DO_MONEY) && SewConfig.get(SewConfig.SHOP_SIGNS);
        }
    },
    /*
     * Chest is SELLING
     */
    BUY(ShopSignData.APPLICATOR_GREEN) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            if ((!signBuilder.textMatchItem(creator, signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
                throw new ShopBuilderException(new LiteralText("Sign not formatted correctly."));
            
            LootableContainerBlockEntity container = null;
            if (!( creator.isCreative() || ((container = InventoryUtils.getAttachedChest( signBuilder )) != null)))
                throw new ShopBuilderException(new LiteralText("Could not find storage for sign."));
            
            if (container != null && !ChunkUtils.canPlayerLootChestsInChunk(creator, container.getPos()))
                throw new ShopBuilderException(new LiteralText("Missing permission to access that container."));
            
            // Update the sign to FREE (Because cost is 0)
            if ( signBuilder.getShopItemPrice() == 0 )
                return FREE.formatSign(signBuilder, creator);
            
            // Parse the owner of the sign
            signBuilder.textParseOwner(signBuilder.getLines()[3], creator);
            
            ShopSigns.printCompletedSign(creator, signBuilder, SewConfig.get(SewConfig.SERVER_SALES_TAX));
            return this.renderSign(signBuilder);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            // Second row Count - Item Name
            shop.setSignLine(1, shop.textParseItem());
            
            // Third Row - Price
            shop.setSignLine(2,
                new LiteralText("for ").formatted(Formatting.BLACK)
                    .append(new LiteralText("$" + shop.getShopItemPrice()).styled(ShopSignData.APPLICATOR_RED))
            );
            
            // Fourth Row - Owner
            shop.setSignLine(3,
                shop.textParseOwner().formatted(Formatting.DARK_GRAY)
            );
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            LootableContainerBlockEntity chest = null;
            Inventory chestInventory = null;
            
            // If shops disabled
            if ( !this.isEnabled() )
                return Either.right(true);
            
            // Check if the attached chest exists
            if (CoreMod.SPAWN_ID.equals(sign.getShopOwner()) || ((chest = InventoryUtils.getAttachedChest(player.getEntityWorld(), signPos)) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.self_buy"));
                
                // These should NOT be null
                if (((sign.getShopItem()) == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null) || (sign.getShopItemDisplay() == null))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
                
                /*
                 * Check if chest is valid
                 */
                if ( chest != null ) {
                    chestInventory = InventoryUtils.getInventoryOf(player.getEntityWorld(), chest.getPos());
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                        return Either.left(TranslatableServerSide.text(player, "shop.error.chest_open"));
                    
                    // If there is not enough of item in chest
                    if (chestInventory == null || InventoryUtils.getInventoryCount(chestInventory, sign::itemMatchPredicate) < sign.getShopItemCount())
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_chest", sign.getShopItemDisplay()));
                }
                
                try {
                    // Take the players money
                    if (!MoneyUtils.takePlayerMoney(player, sign.getShopItemPrice()))
                        return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                    
                    // Give item to player from chest
                    if (!InventoryUtils.chestToPlayer(player, signPos, chestInventory, player.getInventory(), sign::itemMatchPredicate, sign.getShopItemCount(), true, sign::createItemStack)) {
                        // Refund the player
                        MoneyUtils.givePlayerMoney(player, sign.getShopItemPrice());
                        
                        // Error message
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_chest", sign.getShopItemDisplay()));
                    }
                    
                    player.playSound( SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f );
                    
                    // Give the shop keeper money
                    if (!sign.getShopOwner().equals(CoreMod.SPAWN_ID)) {
                        try {
                            MoneyUtils.givePlayerMoney(sign.getShopOwner(), sign.getShopItemPrice());
                        } catch (NbtNotFoundException e) {
                            CoreMod.logError("Failed to give " + sign.getShopItemPrice() + " money to \"" + sign.getShopOwner() + "\" (Maybe they haven't joined the server?).");
                        }
                    }
                    
                    // Get the shop owner
                    ClaimCache claimCache = ((ClaimsAccessor)server).getClaimManager();
                    ClaimantPlayer permissions = claimCache.getPlayerClaim(sign.getShopOwner());
                    
                    // Tell the player
                    TitleUtils.showPlayerAlert(
                        player,
                        Formatting.YELLOW,
                        new LiteralText("You bought "),
                        new LiteralText(FormattingUtils.format( sign.getShopItemCount() ) + " ").formatted(Formatting.AQUA),
                        new TranslatableText(sign.getShopItemTranslationKey()).formatted(Formatting.AQUA),
                        new LiteralText(" from "),
                        permissions.getName().formatted(Formatting.AQUA)
                    );
                    
                    // Log the event
                    CoreMod.logInfo(player.getName().getString() + " bought " + FormattingUtils.format( sign.getShopItemCount() ) + " " + sign.getShopItemIdentifier() + " for $" + FormattingUtils.format( sign.getShopItemPrice() ) + " from " + permissions.getName().asString() );
                    player.increaseStat(ShopStats.SHOP_TYPE_BOUGHT.getOrCreateStat(sign.getShopItem()), sign.getShopItemCount());
                    
                    return Either.right(true);
                    
                } catch (NotEnoughMoneyException e) {
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                }
            }
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.DO_MONEY) && SewConfig.get(SewConfig.SHOP_SIGNS);
        }
    },
    /*
     * Chest is FREE
     */
    FREE(ShopSignData.APPLICATOR_GREEN) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            if (!signBuilder.textMatchItem(creator, signBuilder.getLines()[1]))
                throw new ShopBuilderException(new LiteralText("Sign not formatted correctly."));
            
            LootableContainerBlockEntity container = null;
            if (!( creator.isCreative() || ((container = InventoryUtils.getAttachedChest( signBuilder )) != null)))
                throw new ShopBuilderException(new LiteralText("Could not find storage for sign."));
            
            if (container != null && !ChunkUtils.canPlayerLootChestsInChunk(creator, container.getPos()))
                throw new ShopBuilderException(new LiteralText("Missing permission to access that container."));
            
            // Parse the owner of the sign
            signBuilder.textParseOwner(signBuilder.getLines()[3], creator);
            
            return this.renderSign(signBuilder);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            // Second row Count - Item Name
            shop.setSignLine(1, shop.textParseItem());
            // Third Row - Price
            shop.setSignLine(2,
                new LiteralText("for ").formatted(Formatting.BLACK)
                    .append(new LiteralText("Free").styled(ShopSignData.APPLICATOR_GREEN))
                    .append("!")
            );
            // Fourth Row - Owner
            shop.setSignLine(3,
                shop.textParseOwner().formatted(Formatting.DARK_GRAY)
            );
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            LootableContainerBlockEntity chest = null;
            Inventory chestInventory = null;
            
            // If shops disabled
            if ( !SewConfig.get(SewConfig.DO_MONEY) )
                return Either.right( true );
            
            // Check if the attached chest exists
            if (CoreMod.SPAWN_ID.equals(sign.getShopOwner()) || ((chest = InventoryUtils.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(new LiteralText("Cannot buy items from yourself."));
                
                // These should NOT be null
                if ((sign.getShopItem() == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemDisplay() == null))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
                
                /*
                 * Check if chest is valid
                 */
                if ( chest != null ) {
                    chestInventory = InventoryUtils.getInventoryOf(player.getEntityWorld(), chest.getPos());
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                        return Either.left(new LiteralText("Cannot do that while chest is open."));
                    // If there is not enough of item in chest
                    if (chestInventory == null || InventoryUtils.getInventoryCount(chestInventory, sign::itemMatchPredicate) < sign.getShopItemCount())
                        return Either.left(new LiteralText("Chest is out of " + sign.getShopItemDisplay() + "."));
                }
                
                // Give item to player from chest
                if (!InventoryUtils.chestToPlayer(player, signPos, chestInventory, player.getInventory(), sign::itemMatchPredicate, sign.getShopItemCount(), true ))
                    return Either.left(new LiteralText("Chest is out of " + sign.getShopItemDisplay() + "."));
                
                ClaimCache claimCache = ((ClaimsAccessor)server).getClaimManager();
                ClaimantPlayer permissions = claimCache.getPlayerClaim(sign.getShopOwner());
                
                // Log the event
                CoreMod.logInfo(player.getName().getString() + " got " + FormattingUtils.format( sign.getShopItemCount() ) + " " + sign.getShopItemDisplay() + " from " + permissions.getName().getString());
                
                return Either.right( true );
            }
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.DO_MONEY) && SewConfig.get(SewConfig.SHOP_SIGNS);
        }
    },
    /*
     * Check player balance
     */
    BALANCE(Formatting.GOLD) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder sign, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            // Set the sign owner to SPAWN
            sign.setShopOwner(CoreMod.SPAWN_ID);
            return this.renderSign(sign);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            shop.setSignLine(1, new LiteralText(""));
            shop.setSignLine(2, new LiteralText(""));
            shop.setSignLine(3, new LiteralText(""));
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            // If shops disabled
            if ( !SewConfig.get(SewConfig.DO_MONEY) )
                return Either.right( true );
            
            long playerHas = MoneyUtils.getPlayerMoney( player );
            player.sendMessage(TranslatableServerSide.text( player, "player.money",
                playerHas
            ), MessageType.SYSTEM, CoreMod.SPAWN_ID);
            
            return Either.right( true );
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.DO_MONEY);
        }
    },
    /*
     * Teleport the player to a random location around the map
     */
    WARP(Formatting.DARK_PURPLE) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder sign, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            // Break if not in creative mode
            if (!creator.isCreative())
                return false;
            
            // Set the sign owner to SPAWN
            sign.setShopOwner(CoreMod.SPAWN_ID);
            
            return this.renderSign(sign);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            shop.setSignLine(1, new LiteralText("Teleport to ").formatted(Formatting.DARK_GRAY)
                .append(new LiteralText("Biome").formatted(Formatting.OBFUSCATED, Formatting.RED))
            );
            shop.setSignLine(2, new LiteralText(""));
            shop.setSignLine(3, new LiteralText(""));
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            WarpUtils.Warp warp = WarpUtils.getWarp(player.getUuid(), null);
            if ( warp == null ) {
                // Create new warp
                if ( WarpUtils.isPlayerCreating( player ) )
                    return Either.right(false);
                // Make a new warp
                return Either.right(this.generateNewWarp(player));
            } else {
                // Warp the player to their home
                WarpUtils.teleportEntityAndAttached(player, warp);
            }
            return Either.right(true);
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.WARP_MAX_DISTANCE) > 0;
        }
        private boolean generateNewWarp(@NotNull final ServerPlayerEntity player) {
            MinecraftServer server = player.getServer();
            RegistryKey<World> registryKey = SewConfig.get(SewConfig.WARP_DIMENSION);
            
            ServerWorld world = server.getWorld(registryKey);
            if (world == null)
                world = player.getWorld();
            
            ((LogicalWorld)world).addTickableEvent(new WaystoneSearch(
                world,
                player
            ));
            return true;
        }
    },
    /*
     * Move the players warp
     */
    WAYSTONE(Formatting.DARK_PURPLE) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder sign, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            // Set the signs price
            sign.setTradePrice(SewConfig.get(SewConfig.WARP_WAYSTONE_COST));
            
            String name;
            Text nameText = sign.getSignLine(1);
            if (nameText == null || (name = nameText.getString()) == null || name.isEmpty())
                name = WarpUtils.PRIMARY_DEFAULT_HOME;
            else {
                if (!IntUtils.between(1, name.length(), 15))
                    throw new ShopBuilderException(TranslatableServerSide.text(creator, "warp.notice.name.too_long", 15));
                
                if (!WarpUtils.validateName(name))
                    throw new ShopBuilderException(TranslatableServerSide.text(creator, "warp.notice.name.invalid"));
            }
            
            // Set the sign owner to SPAWN
            sign.setShopOwner(CoreMod.SPAWN_ID);
            return this.renderSign(sign);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            Text name = shop.getSignLine(1);
            if (name == null)
                return false;

            Text priceText = new LiteralText("$" + FormattingUtils.format(shop.getShopItemPrice())).formatted(Formatting.DARK_BLUE);
            
            // Set the text for the sign
            shop.setSignLine(1, new LiteralText(name.getString()).formatted(Formatting.GREEN));
            shop.setSignLine(2, new LiteralText("Build here").formatted(Formatting.BLACK));
            shop.setSignLine(3, new LiteralText("for ").formatted(Formatting.BLACK).append(priceText));
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            try {
                ServerWorld world = player.getWorld();
                if (DimensionUtils.isOutOfBuildLimitVertically(world, signPos) || DimensionUtils.isWithinProtectedZone(world, signPos) || !ChunkUtils.canPlayerBreakInChunk(player, signPos))
                    return Either.left(new LiteralText("Can't build that here"));
                
                final String warpName = sign.getSignLine(1)
                    .getString();
                
                if (WarpUtils.getWarps(player).size() >= SewConfig.get(SewConfig.WARP_WAYSTONES_ALLOWED) && (WarpUtils.getWarp(player, warpName) == null))
                    return Either.left(new LiteralText("Too many waystones. Can't build any more."));
                
                if (!MoneyUtils.takePlayerMoney(player, SewConfig.get(SewConfig.WARP_WAYSTONE_COST)))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                
                WarpUtils warp = new WarpUtils(warpName, player, player.getWorld(),signPos.down());
                if (!warp.claimAndBuild(() -> warp.save(warp.getSafeTeleportPos(), player))) {
                    // Notify the player
                    player.sendMessage(
                        new LiteralText("Can't build that here").formatted(Formatting.RED),
                        MessageType.SYSTEM,
                        CoreMod.SPAWN_ID
                    );
                    
                    // Refund the player
                    MoneyUtils.givePlayerMoney(player, SewConfig.get(SewConfig.WARP_WAYSTONE_COST));
                }
                
                return Either.right(true);
            } catch (NotEnoughMoneyException e) {
                return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
            }
        }
    },
    /*
     * Allow players to sell chunks/region in their towns
     */
    DEED(Formatting.DARK_GRAY) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            SignBlockEntity sign = builder.getSign();
            
            // Get the cost for the DEED
            if (!builder.textMatchPrice(builder.getLines()[2]))
                throw new ShopBuilderException(new LiteralText("Sign is missing a cost"));
            
            // These should NOT be null
            if (sign.getWorld() == null)
                throw new ShopBuilderException(TranslatableServerSide.text(creator, "shop.error.database"));
            
            WorldChunk chunk = sign.getWorld().getWorldChunk( sign.getPos() );
            ClaimantTown town = null;
            
            // Get the deed type
            String deedType = ( builder.getLines()[1].asString().equalsIgnoreCase("region") ? "region" : "chunk" );
            
            // Handle the deed type
            switch (deedType) {
                case "chunk": {
                    // Check that the sign is within a town
                    if ((chunk == null) || ((town = ((IClaimedChunk) chunk).getTown()) == null))
                        throw new ShopBuilderException(new LiteralText("Deed sign must be placed within a town."));
                    
                    // Check who placed the sign
                    if (!(creator.getUuid().equals(town.getOwner())) && creator.getUuid().equals(((IClaimedChunk) chunk).getOwner()))
                        throw new ShopBuilderException(new LiteralText("Deed signs may only be placed in chunks belonging to the town owner, by the town owner."));
                    
                    sign.setTextOnRow(1, new LiteralText(CasingUtils.sentence(deedType)));
                    break;
                }
                case "region": {
                    BlockPos firstPos;
                    BlockPos secondPos;
                    
                    // Check that the region is defined
                    if (((firstPos = ((PlayerData) creator).getRulerA()) == null) || ((secondPos = ((PlayerData) creator).getRulerB()) == null))
                        throw new ShopBuilderException(new LiteralText("Deed sign must be within a valid region. Use \"").append(new LiteralText("/ruler").formatted(Formatting.AQUA)).append("\" command to select two points"));
                    
                    // Check that the region contains the sign
                    BlockRange region = BlockRange.between(firstPos, secondPos);
                    if (!region.isWithin(sign.getPos()))
                        throw new ShopBuilderException(new LiteralText("Deed sign must be within a valid region. Use \"").append(new LiteralText("/ruler").formatted(Formatting.AQUA)).append("\" command to select two points"));
                    
                    // Clear ruler area
                    ((PlayerData) creator).setRulerA(null);
                    ((PlayerData) creator).setRulerB(null);
                    
                    // Validate the minimum and maximum widths
                    int maxWidth = SewConfig.get(SewConfig.MAXIMUM_REGION_WIDTH);
                    int minWidth = SewConfig.get(SewConfig.MINIMUM_REGION_WIDTH);
                    
                    // Check the size of the region
                    if ((maxWidth > 0) && ((region.getNorthSouth() > maxWidth) || (region.getEastWest() > maxWidth)))
                        throw new ShopBuilderException(new LiteralText("Deed region is too large."));
                    if ((region.getNorthSouth() < minWidth) || (region.getEastWest() < minWidth))
                        throw new ShopBuilderException(new LiteralText("Deed region is too small."));
                    
                    // Update the sign to display the width
                    sign.setTextOnRow(1, region.displayDimensions());
                    builder.regionPositioning(firstPos, secondPos);
                    break;
                }
                default:
                    return false;
            }
            
            // Update the text on the sign
            sign.setTextOnRow( 2, new LiteralText("for ").formatted(Formatting.BLACK).append(
                new LiteralText("$" + builder.getShopItemPrice()).formatted(Formatting.DARK_BLUE)
            ));
            sign.setTextOnRow( 3, (town == null ? new LiteralText("") : town.getName()).formatted(Formatting.DARK_GRAY));
            
            // Set the sign owner
            builder.setShopOwner(creator.getUuid());
            return this.renderSign(builder);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            // If shops disabled
            if (!(SewConfig.get(SewConfig.DO_MONEY) && SewConfig.get(SewConfig.DO_CLAIMS)))
                return Either.right( true );
            
            if ((sign.getFirstPos() == null) || (sign.getSecondPos() == null))
                return Either.left(new LiteralText("Invalid deed sign"));
            
            
            
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return (SewConfig.get(SewConfig.DO_MONEY) && SewConfig.get(SewConfig.DO_CLAIMS));
        }
    },
    /*
     * Allow players to buy chunk claims
     */
    PLOTS(Formatting.GREEN) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            // Break if not in creative mode
            if (!creator.isCreative())
                return false;
            
            // Parse the String from the sign
            if ((!builder.textMatchCount(builder.getLines()[1])) || (!builder.textMatchPrice(builder.getLines()[2])))
                return false;
            
            // Set the sign owner to SPAWN
            builder.setShopOwner(CoreMod.SPAWN_ID);
            
            return this.renderSign(builder);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            NumberFormat formatter = NumberFormat.getInstance();
            
            shop.setSignLine(1, new LiteralText(formatter.format(shop.getShopItemCount()) + " chunks"));
            shop.setSignLine(2,
            new LiteralText("for ").formatted(Formatting.BLACK)
                .append(new LiteralText("$" + shop.getShopItemPrice()).formatted(Formatting.DARK_BLUE))
            );
            shop.setSignLine(3, new LiteralText(""));
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull ServerPlayerEntity player, @NotNull BlockPos signPos, ShopSignData sign) {
            // These should NOT be null
            if ((sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null))
                return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
            
            // If shops disabled
            if ( !this.isEnabled() )
                return Either.right( true );
            
            ClaimantPlayer claim = ((PlayerData) player).getClaim();
            
            try {
                // Take the players money
                if (!MoneyUtils.takePlayerMoney(player, sign.getShopItemPrice()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                
                if ((SewConfig.get(SewConfig.PLAYER_CLAIM_BUY_LIMIT) > 0) && ((claim.getMaxChunkLimit() + sign.getShopItemCount()) > SewConfig.get(SewConfig.PLAYER_CLAIM_BUY_LIMIT)))
                    return Either.left(new TranslatableText("Can't buy any more of that."));
                
                // Increase the players chunk count
                ((PlayerData) player).getClaim().increaseMaxChunkLimit( sign.getShopItemCount() );
                
                // Log the transaction
                CoreMod.logInfo( player.getName().getString() + " bought " + FormattingUtils.format( sign.getShopItemCount() ) + " chunks for $" + FormattingUtils.format( sign.getShopItemPrice() ) );
                
            } catch (NotEnoughMoneyException e) {
                return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
            }
            
            player.sendMessage(new LiteralText("Chunks claimed ").formatted(Formatting.YELLOW)
                .append(new LiteralText(FormattingUtils.format(claim.getCount())).formatted(Formatting.AQUA))
                .append(" / ")
                .append(new LiteralText(FormattingUtils.format(claim.getMaxChunkLimit())).formatted(Formatting.AQUA)),
                MessageType.SYSTEM,
                CoreMod.SPAWN_ID
            );
            
            return Either.right( true );
        }
        @Override
        public boolean isEnabled() {
            return (SewConfig.get(SewConfig.DO_MONEY) && SewConfig.get(SewConfig.DO_CLAIMS) && (SewConfig.get(SewConfig.PLAYER_CLAIM_BUY_LIMIT) != 0));
        }
    },
    /*
     * Player guide books
     */
    GUIDES(Formatting.DARK_GREEN) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            if (creator.isCreative()) {
                // Set the sign owner
                builder.setShopOwner(CoreMod.SPAWN_ID);
                
                return this.renderSign(builder);
            }
            return false;
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            Text guideName = shop.getSignLine(1);
            if (guideName == null)
                return false;
            
            shop.setSignLine(1, new LiteralText(guideName.getString()));
            shop.setSignLine(2, new LiteralText(""));
            shop.setSignLine(3, new LiteralText(""));
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign) {
            // Get the guides title
            String bookRawTitle = sign.getSignLine(1).getString();
            
            // Get the guidebook
            GuideUtils guide;
            try {
                if ((guide = GuideUtils.getBook(bookRawTitle.toLowerCase())) == null)
                    return Either.right(false);
            } catch (JsonSyntaxException e) {
                CoreMod.logError( e );
                return Either.left(new LiteralText("An error occurred getting that guide."));
            }
            
            // Give the player the book
            player.giveItemStack(guide.newStack());
            player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return Either.right(true);
        }
    },
    /*
     * Player backpacks
     */
    BACKPACK(Formatting.YELLOW) {
        @Override
        public boolean formatSign(@NotNull final ShopSignBuilder builder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException {
            if (!creator.isCreative())
                return false;
            
            // Parse the String from the sign
            if ((!builder.textMatchCount(builder.getLines()[1])) || (!builder.textMatchPrice(builder.getLines()[2])))
                return false;
            
            if ((builder.getShopItemCount() % 9 != 0 ) || builder.getShopItemCount() > 54)
                throw new ShopBuilderException(new LiteralText("Backpack size must be multiples of 9, no larger than 54."));
            
            // Set the sign owner to SPAWN
            builder.setShopOwner(CoreMod.SPAWN_ID);
            
            // Render the sign
            return this.renderSign(builder);
        }
        @Override
        public boolean renderSign(@NotNull ShopSignData shop) {
            NumberFormat formatter = NumberFormat.getInstance();
            
            shop.setSignLine(1, new LiteralText(formatter.format(shop.getShopItemCount()) + " slots"));
            shop.setSignLine(2,
            new LiteralText("for ").formatted(Formatting.BLACK)
                .append(new LiteralText("$" + shop.getShopItemPrice()).formatted(Formatting.DARK_BLUE))
            );
            shop.setSignLine(3, new LiteralText(""));
            
            return super.renderSign(shop);
        }
        @Override
        public Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull ServerPlayerEntity player, @NotNull BlockPos signPos, ShopSignData sign) {
            // These should NOT be null
            if ((sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null))
                return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
            
            // Get the number of rows in the backpack
            int newPackRows = sign.getShopItemCount() / 9;
            
            // If shops disabled
            if ( !this.isEnabled() )
                return Either.right( true );
            
            BackpackCarrier backpackCarrier = (BackpackCarrier) player;
            PlayerBackpack backpack = backpackCarrier.getBackpack();
            
            try {
                // Check the current backpack size
                if ((backpack != null) && (newPackRows <= backpack.getRows()))
                    return Either.left(TranslatableServerSide.text(player, "backpack.no_downsize"));
                
                // If backpacks must be purchased in order
                if ( SewConfig.get(SewConfig.BACKPACK_SEQUENTIAL) ) {
                    int currentRows = ( backpack == null ? 0 : backpack.getRows() );
                    if ((newPackRows - 1) > currentRows)
                        return Either.left(TranslatableServerSide.text(player, "backpack.need_previous"));
                }
                
                // Take the players money
                if (!MoneyUtils.takePlayerMoney(player, sign.getShopItemPrice()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                
                backpackCarrier.setBackpack(( backpack == null ?
                    new PlayerBackpack(player, newPackRows)
                    : new PlayerBackpack(backpack, newPackRows)
                ));
                
                player.sendMessage(new LiteralText("Backpack size is now ").formatted(Formatting.YELLOW)
                    .append(new LiteralText(FormattingUtils.format( sign.getShopItemCount() )).formatted(Formatting.AQUA)),
                    MessageType.SYSTEM,
                    CoreMod.SPAWN_ID
                );
                
                // Log the transaction
                CoreMod.logInfo(player.getName().getString() + " bought a " + FormattingUtils.format(sign.getShopItemCount()) + " slot backpack for $" + FormattingUtils.format(sign.getShopItemPrice()));
                
            } catch (NotEnoughMoneyException e) {
                return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
            }
            
            return Either.right( true );
        }
        @Override
        public boolean isEnabled() {
            return (SewConfig.get(SewConfig.DO_MONEY) && SewConfig.get(SewConfig.ALLOW_BACKPACKS));
        }
    };
    
    private final @NotNull StyleApplicator applicator;
    
    ShopSigns(@NotNull String color) {
        this(ColorUtils.getRawTextColor(color));
    }
    ShopSigns(@NotNull Formatting color) {
        this(TextColor.fromFormatting(color));
    }
    ShopSigns(@Nullable TextColor color) {
        this(new StyleApplicator(color == null ? Objects.requireNonNull(TextColor.fromFormatting(Formatting.WHITE)) : color));
    }
    ShopSigns(@NotNull StyleApplicator applicator) {
        this.applicator = applicator;
    }
    
    public boolean isEnabled() { return true; }
    
    public final @NotNull StyleApplicator getApplicator() {
        return this.applicator;
    }
    public static @Nullable ShopSigns valueOf(@NotNull Text text) {
        String str = text.getString();
        if ( str.startsWith( "[" ) && str.endsWith( "]" ) ) {
            str = str.substring(1, str.length() - 1).toUpperCase();
            try {
                return ShopSigns.valueOf(str);
            } catch ( IllegalArgumentException e ) {
                return null;
            }
        }
        return null;
    }
    
    public static int printCompletedSign(@NotNull final ServerPlayerEntity player, @NotNull final ShopSignBuilder signBuilder, final int serverTaxPerc) {
        int returnVal = signBuilder.getShopItemPrice();
        MutableText output = new LiteralText("Created new shop for ").formatted(Formatting.YELLOW)
            .append(MessageUtils.formatNumber(signBuilder.getShopItemCount()))
            .append(" ")
            .append(MessageUtils.formatObject(signBuilder.getShopItem()));
        
        // Try getting the located town
        ServerWorld world = player.getWorld();
        ClaimantTown town = ((IClaimedChunk)world.getChunk(signBuilder.getSign().getPos()))
            .getTown();
        int townTaxVal = (town == null ? 0 : signBuilder.getShopItemPrice() * (town.getTaxRate() / 100));
        int serverTaxVal = signBuilder.getShopItemPrice() * (serverTaxPerc / 100);
        
        if (serverTaxVal > 0) {
            output.append("\n| Server tax is ")
                .append(MessageUtils.formatNumber(serverTaxPerc))
                .append("% ($")
                .append(MessageUtils.formatNumber(serverTaxVal))
                .append(")");
            returnVal -= serverTaxVal;
        } else if (serverTaxPerc > 0) {
            output.append("\n| Server taxes do not apply.");
        }
        
        if (townTaxVal > 0) {
            output.append("\n| Server tax is ")
                .append(MessageUtils.formatNumber(0))
                .append("% ($")
                .append(MessageUtils.formatNumber(townTaxVal))
                .append(")");
            returnVal -= townTaxVal;
        } else if (serverTaxVal > 0) {
            output.append("\n| Town taxes do not apply.");
        }
        
        output.append("\n| Recipient gets $")
            .append(MessageUtils.formatNumber(returnVal))
            .append((serverTaxVal > 0 || townTaxVal > 0) ? " after taxes." : ".");
        
        player.sendMessage(output, false);
        return returnVal;
    }
    
    public abstract Either<Text, Boolean> onInteract(@NotNull MinecraftServer server, @NotNull final ServerPlayerEntity player, @NotNull final BlockPos signPos, final ShopSignData sign);
    public abstract boolean formatSign(@NotNull final ShopSignBuilder signBuilder, @NotNull final ServerPlayerEntity creator) throws ShopBuilderException;
    public boolean renderSign(@NotNull final ShopSignData shop) {
        shop.removeEditor();
        
        // Set the first row formatting
        shop.setSignLine(0, new LiteralText("[" + this.name() + "]" ).styled(this.getApplicator()));
        return true;
    }
}
