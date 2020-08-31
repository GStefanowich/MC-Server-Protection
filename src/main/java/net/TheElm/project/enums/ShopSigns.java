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
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.exceptions.ShopBuilderException;
import net.TheElm.project.interfaces.BackpackCarrier;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.ShopSignBlockEntity;
import net.TheElm.project.objects.PlayerBackpack;
import net.TheElm.project.protections.BlockDistance;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.TheElm.project.utilities.GuideUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.TheElm.project.utilities.MoneyUtils;
import net.TheElm.project.utilities.ShopSignBuilder;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.TheElm.project.utilities.WarpUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public enum ShopSigns {
    /*
     * Chest is BUYING
     */
    SELL( Formatting.DARK_RED ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign( signBuilder, creator);
            
            if ((!signBuilder.textMatchItem(creator, signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
                throw new ShopBuilderException(new LiteralText("Sign not formatted correctly.").formatted(Formatting.RED));
            
            LootableContainerBlockEntity container = null;
            if (!( creator.isCreative() || ((container = this.getAttachedChest( signBuilder )) != null)))
                throw new ShopBuilderException(new LiteralText("Could not find storage for sign.").formatted(Formatting.RED));
            
            if (container != null && !ChunkUtils.canPlayerLootChestsInChunk(creator, container.getPos()))
                throw new ShopBuilderException(new LiteralText("Missing permission to access that container.").formatted(Formatting.RED));
            
            // Second row Count - Item Name
            signBuilder.getSign().setTextOnRow(1,
                new LiteralText(signBuilder.itemSize() + " ").formatted(Formatting.BLACK)
                    .append(new TranslatableText(Registry.ITEM.get(signBuilder.getItem()).getTranslationKey()).formatted(Formatting.DARK_AQUA))
            );
            // Third Row - Price
            signBuilder.getSign().setTextOnRow(2,
                new LiteralText("for ").formatted(Formatting.BLACK)
                    .append(new LiteralText("$" + signBuilder.shopPrice()).formatted(Formatting.DARK_BLUE))
            );
            // Fourth Row - Owner
            signBuilder.getSign().setTextOnRow(3,
                signBuilder.textParseOwner(signBuilder.getLines()[3], creator).formatted(Formatting.DARK_GRAY)
            );
            
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
            LootableContainerBlockEntity chest = null;
            Inventory chestInventory = null;
            
            // If shops disabled
            if ( !SewConfig.get(SewConfig.DO_MONEY) )
                return Either.right( true );
            
            // These should NOT be null
            if ((sign.getShopItem() == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null) || (sign.getShopItemDisplay() == null))
                return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
            
            // Check if the attached chest exists
            if (CoreMod.spawnID.equals(sign.getShopOwner()) || ((chest = this.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.self_sell"));
                /*
                 * Check if chest is valid
                 */
                if ((chest != null)) {
                    chestInventory = InventoryUtils.getInventoryOf( player.getEntityWorld(), chest.getPos() );
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount( player.getEntityWorld(), chest.getPos() ) > 0)
                        return Either.left(TranslatableServerSide.text(player, "shop.error.chest_open"));
                    // If player does not have any of item
                    if (player.inventory.count(sign.getShopItem()) < sign.getShopItemCount())
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_player", sign.getShopItemDisplay()));
                }
                /*
                 * Transfer the items from chest to player
                 */
                try {
                    // Take shop keepers money
                    if (!(sign.getShopOwner().equals(CoreMod.spawnID) || MoneyUtils.takePlayerMoney(sign.getShopOwner(), sign.getShopItemPrice())))
                        return Either.left(TranslatableServerSide.text(player, "shop.error.money_chest"));
                    
                    // Put players item into chest
                    if (!InventoryUtils.playerToChest( player, signPos, player.inventory, chestInventory, sign.getShopItem(), sign.getShopItemCount(), true )) {
                        // Refund the shopkeeper
                        if (!(sign.getShopOwner().equals(CoreMod.spawnID))) {
                            MoneyUtils.givePlayerMoney(sign.getShopOwner(), sign.getShopItemPrice());
                        }
                        
                        // Error message
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_player", sign.getShopItemDisplay()));
                    }
                    
                    // Give player money for item
                    MoneyUtils.givePlayerMoney(player, sign.getShopItemPrice());
                    
                    // Get shop owner
                    ClaimantPlayer permissions = ClaimantPlayer.get( sign.getShopOwner() );
                    
                    // Tell the player
                    TitleUtils.showPlayerAlert(
                        player,
                        Formatting.YELLOW,
                        new LiteralText("You sold "),
                        new LiteralText(NumberFormat.getInstance().format( sign.getShopItemCount() ) + " ").formatted(Formatting.AQUA),
                        new TranslatableText(sign.getShopItem().getTranslationKey()).formatted(Formatting.AQUA),
                        new LiteralText(" to "),
                        permissions.getName().formatted(Formatting.AQUA)
                    );
                    
                    // Log the event
                    CoreMod.logInfo( player.getName().asString() + " sold " + NumberFormat.getInstance().format( sign.getShopItemCount() ) + " " + sign.getShopItemDisplay().asString() + " for $" + NumberFormat.getInstance().format( sign.getShopItemPrice() ) + " to " + permissions.getName().asString() );
                    
                    return Either.right( true );
                    
                } catch ( NbtNotFoundException e ) {
                    CoreMod.logError( "Failed to give " + sign.getShopItemPrice() + " money to \"" + sign.getShopOwner() + "\" (Maybe they haven't joined the server?)." );
                    // If a database problem occurs
                    return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
                } catch ( NotEnoughMoneyException e ) {
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_chest"));
                }
            }
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.DO_MONEY);
        }
    },
    /*
     * Chest is SELLING
     */
    BUY( Formatting.DARK_BLUE ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign( signBuilder, creator);
            
            if ((!signBuilder.textMatchItem(creator, signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
                throw new ShopBuilderException(new LiteralText("Sign not formatted correctly."));
            
            LootableContainerBlockEntity container = null;
            if (!( creator.isCreative() || ((container = this.getAttachedChest( signBuilder )) != null)))
                throw new ShopBuilderException(new LiteralText("Could not find storage for sign."));
            
            if (container != null && !ChunkUtils.canPlayerLootChestsInChunk(creator, container.getPos()))
                throw new ShopBuilderException(new LiteralText("Missing permission to access that container."));
            
            SignBlockEntity sign = signBuilder.getSign();
            if ( signBuilder.shopPrice() == 0 ) {
                // Update the sign to FREE
                sign.setTextOnRow( 0, new LiteralText("[FREE]").formatted(FREE.getFormatting()));
                return FREE.formatSign( signBuilder, creator);
            }
            
            // Second row Count - Item Name
            sign.setTextOnRow(1,
                new LiteralText(signBuilder.itemSize() + " ").formatted(Formatting.BLACK)
                    .append(new TranslatableText(Registry.ITEM.get(signBuilder.getItem()).getTranslationKey()).formatted(Formatting.DARK_AQUA))
            );
            // Third Row - Price
            sign.setTextOnRow(2,
                new LiteralText("for ").formatted(Formatting.BLACK)
                    .append(new LiteralText("$" + signBuilder.shopPrice()).formatted(Formatting.DARK_BLUE))
            );
            // Fourth Row - Owner
            sign.setTextOnRow(3,
                signBuilder.textParseOwner(signBuilder.getLines()[3], creator).formatted(Formatting.DARK_GRAY)
            );
            
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
            LootableContainerBlockEntity chest = null;
            Inventory chestInventory = null;
            
            // If shops disabled
            if ( !this.isEnabled() )
                return Either.right( true );
            
            // Check if the attached chest exists
            if (CoreMod.spawnID.equals(sign.getShopOwner()) || ((chest = this.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.self_buy"));
                
                // These should NOT be null
                if ((sign.getShopItem() == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null) || (sign.getShopItemDisplay() == null))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
                
                /*
                 * Check if chest is valid
                 */
                if ( chest != null ) {
                    chestInventory = InventoryUtils.getInventoryOf( player.getEntityWorld(), chest.getPos() );
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                        return Either.left(TranslatableServerSide.text(player, "shop.error.chest_open"));
                    // If there is not enough of item in chest
                    if (chest.count(sign.getShopItem()) < sign.getShopItemCount())
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_chest", sign.getShopItemDisplay()));
                }
                
                try {
                    // Take the players money
                    if (!MoneyUtils.takePlayerMoney(player, sign.getShopItemPrice()))
                        return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                    
                    // Give item to player from chest
                    if (!InventoryUtils.chestToPlayer( player, signPos, chestInventory, player.inventory, sign.getShopItem(), sign.getShopItemCount(), true )) {
                        // Refund the player
                        MoneyUtils.givePlayerMoney(player, sign.getShopItemPrice());
                        
                        // Error message
                        return Either.left(TranslatableServerSide.text(player, "shop.error.stock_chest", sign.getShopItemDisplay()));
                    }
                    
                    player.playSound( SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f );
                    
                    // Give the shop keeper money
                    if (!sign.getShopOwner().equals(CoreMod.spawnID)) {
                        try {
                            MoneyUtils.givePlayerMoney(sign.getShopOwner(), sign.getShopItemPrice());
                        } catch (NbtNotFoundException e) {
                            CoreMod.logError( "Failed to give " + sign.getShopItemPrice() + " money to \"" + sign.getShopOwner() + "\" (Maybe they haven't joined the server?)." );
                        }
                    }
                    
                    // Get the shop owner
                    ClaimantPlayer permissions = ClaimantPlayer.get(sign.getShopOwner());
    
                    // Tell the player
                    TitleUtils.showPlayerAlert(
                        player,
                        Formatting.YELLOW,
                        new LiteralText("You bought "),
                        new LiteralText(NumberFormat.getInstance().format( sign.getShopItemCount() ) + " ").formatted(Formatting.AQUA),
                        new TranslatableText(sign.getShopItem().getTranslationKey()).formatted(Formatting.AQUA),
                        new LiteralText(" from "),
                        permissions.getName().formatted(Formatting.AQUA)
                    );
                    
                    // Log the event
                    CoreMod.logInfo( player.getName().asString() + " bought " + NumberFormat.getInstance().format( sign.getShopItemCount() ) + " " + sign.getShopItemDisplay().asString() + " for $" + NumberFormat.getInstance().format( sign.getShopItemPrice() ) + " from " + permissions.getName().asString() );
                    
                    return Either.right( true );
                    
                } catch (NotEnoughMoneyException e) {
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                }
            }
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.DO_MONEY);
        }
    },
    /*
     * Chest is FREE
     */
    FREE( Formatting.DARK_BLUE ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign( signBuilder, creator);
            
            if (!signBuilder.textMatchItem(creator, signBuilder.getLines()[1]))
                throw new ShopBuilderException(new LiteralText("Sign not formatted correctly."));
            
            LootableContainerBlockEntity container = null;
            if (!( creator.isCreative() || ((container = this.getAttachedChest( signBuilder )) != null)))
                throw new ShopBuilderException(new LiteralText("Could not find storage for sign."));
            
            if (container != null && !ChunkUtils.canPlayerLootChestsInChunk(creator, container.getPos()))
                throw new ShopBuilderException(new LiteralText("Missing permission to access that container."));
            
            // Second row Count - Item Name
            signBuilder.getSign().setTextOnRow(1,
                new LiteralText(signBuilder.itemSize() + " ").formatted(Formatting.BLACK)
                    .append(new TranslatableText(Registry.ITEM.get(signBuilder.getItem()).getTranslationKey()).formatted(Formatting.DARK_AQUA))
            );
            // Third Row - Price
            signBuilder.getSign().setTextOnRow(2,
                new LiteralText("for ").formatted(Formatting.BLACK)
                    .append(new LiteralText("Free").formatted(Formatting.DARK_BLUE))
                    .append("!")
            );
            // Fourth Row - Owner
            signBuilder.getSign().setTextOnRow(3,
                signBuilder.textParseOwner(signBuilder.getLines()[3], creator).formatted(Formatting.DARK_GRAY)
            );
            
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
            
            LootableContainerBlockEntity chest = null;
            Inventory chestInventory = null;
            
            // If shops disabled
            if ( !SewConfig.get(SewConfig.DO_MONEY) )
                return Either.right( true );
            
            // Check if the attached chest exists
            if (CoreMod.spawnID.equals(sign.getShopOwner()) || ((chest = this.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(new LiteralText("Cannot buy items from yourself."));
    
                // These should NOT be null
                if ((sign.getShopItem() == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemDisplay() == null))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.database"));
                
                /*
                 * Check if chest is valid
                 */
                if ( chest != null ) {
                    chestInventory = InventoryUtils.getInventoryOf( player.getEntityWorld(), chest.getPos() );
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                        return Either.left(new LiteralText("Cannot do that while chest is open."));
                    // If there is not enough of item in chest
                    if (chest.count(sign.getShopItem()) < sign.getShopItemCount()) {
                        return Either.left(new LiteralText("Chest is out of " + sign.getShopItemDisplay() + "."));
                    }
                }
                
                // Give item to player from chest
                if (!InventoryUtils.chestToPlayer( player, signPos, chestInventory, player.inventory, sign.getShopItem(), sign.getShopItemCount(), true ))
                    return Either.left(new LiteralText("Chest is out of " + sign.getShopItemDisplay() + "."));
                
                ClaimantPlayer permissions = ClaimantPlayer.get( sign.getShopOwner() );
                
                // Log the event
                CoreMod.logInfo( player.getName().asString() + " got " + NumberFormat.getInstance().format( sign.getShopItemCount() ) + " " + sign.getShopItemDisplay() + " from " + permissions.getName().asString() );
                
                return Either.right( true );
            }
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.DO_MONEY);
        }
    },
    /*
     * Check player balance
     */
    BALANCE( Formatting.GOLD ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign(signBuilder, creator);
            
            // Reset the other lines
            SignBlockEntity sign = signBuilder.getSign();
            sign.setTextOnRow( 1, new LiteralText(""));
            sign.setTextOnRow( 2, new LiteralText(""));
            sign.setTextOnRow( 3, new LiteralText(""));
            
            // Set the sign owner to SPAWN
            signBuilder.shopOwner( CoreMod.spawnID );
            
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
            // If shops disabled
            if ( !SewConfig.get(SewConfig.DO_MONEY) )
                return Either.right( true );
            
            long playerHas = MoneyUtils.getPlayerMoney( player );
            player.sendMessage(TranslatableServerSide.text( player, "player.money",
                playerHas
            ), MessageType.SYSTEM, ServerCore.spawnID);
            
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
    WARP( Formatting.DARK_PURPLE ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign(signBuilder, creator);
            
            // Break if not in creative mode
            if (!creator.isCreative())
                return false;
            
            SignBlockEntity sign = signBuilder.getSign();
            sign.setTextOnRow( 1, new LiteralText("Teleport to ").formatted(Formatting.DARK_GRAY)
                .append( new LiteralText( "Biome" ).formatted(Formatting.OBFUSCATED, Formatting.RED) )
            );
            sign.setTextOnRow( 2, new LiteralText(""));
            sign.setTextOnRow( 3, new LiteralText(""));
            
            // Set the sign owner to SPAWN
            signBuilder.shopOwner( CoreMod.spawnID );
            
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
            WarpUtils.Warp warp = WarpUtils.getWarp( player.getUuid() );
            if ( warp == null ) {
                // Create new warp
                if ( WarpUtils.isPlayerCreating( player ) )
                    return Either.right( false );
                // Make a new warp
                return Either.right(this.generateNewWarp(player));
            } else {
                // Warp the player to their home
                WarpUtils.teleportPlayer( warp.world, player, warp.warpPos );
            }
            return Either.right( true );
        }
        @Override
        public boolean isEnabled() {
            return SewConfig.get(SewConfig.WARP_MAX_DISTANCE) > 0;
        }
        private boolean generateNewWarp(final ServerPlayerEntity player) {
            // Create a new warp point asynchronously
            new Thread(() -> {
                // Get world info
                final MinecraftServer server;
                if ((server = player.getServer()) == null)
                    return;
                final ServerWorld world = server.getWorld(World.OVERWORLD);
                final BlockPos spawnPos = WarpUtils.getWorldSpawn(world);
                
                // Tell the player
                player.sendMessage(TranslatableServerSide.text(
                    player,
                    "warp.random.search"
                ), MessageType.SYSTEM, ServerCore.spawnID);
                
                // Create warp
                WarpUtils newWarp = new WarpUtils( player, spawnPos );
                BlockPos warpToPos;
                
                while (((warpToPos = newWarp.getWarpPositionIn(world)) == null) || (!newWarp.build(player, world)));
                
                // Get the distance
                int distance = warpToPos.getManhattanDistance(spawnPos);
                
                // Build the return warp
                player.sendMessage(TranslatableServerSide.text(
                    player,
                    "warp.random.build"
                ), MessageType.SYSTEM, ServerCore.spawnID);
                
                // Teleport the player
                BlockPos safeTeleportPos = newWarp.getSafeTeleportPos(world);
                WarpUtils.teleportPlayer(world, player, safeTeleportPos);
                
                // Save the warp for later
                newWarp.save(world, safeTeleportPos, player);
                
                // Notify the player of their new location
                player.sendMessage(TranslatableServerSide.text(
                    player,
                    "warp.random.teleported",
                    distance
                ), MessageType.SYSTEM, ServerCore.spawnID);
            }).start();
            return true;
        }
    },
    /*
     * Move the players warp
     */
    WAYSTONE( Formatting.DARK_PURPLE ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign(signBuilder, creator);
            
            SignBlockEntity sign = signBuilder.getSign();
            
            // Get sign position
            /*BlockPos blockPos = sign.getPos();
            int x = blockPos.getX();
            int z = blockPos.getZ();
            if ((x % 16 == 0) || ((x + 1) % 16 == 0) || (z % 16 == 0) || ((z + 1) % 16 == 0))
                throw new ShopBuilderException(new LiteralText("Can't place waystones on the border of a chunk."));*/
            
            // Set the signs price
            Text priceText = new LiteralText("$" + SewConfig.get(SewConfig.WARP_WAYSTONE_COST)).formatted(Formatting.DARK_BLUE);
            signBuilder.textMatchPrice(priceText);
            
            // Set the text for the sign
            sign.setTextOnRow(1, new LiteralText("Set your warp").formatted(Formatting.BLACK));
            sign.setTextOnRow(2, new LiteralText("to this chunk").formatted(Formatting.BLACK));
            sign.setTextOnRow(3, new LiteralText("for ").formatted(Formatting.BLACK).append(priceText));
            
            // Set the sign owner to SPAWN
            signBuilder.shopOwner(CoreMod.spawnID);
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
            try {
                if (!MoneyUtils.takePlayerMoney(player, SewConfig.get(SewConfig.WARP_WAYSTONE_COST)))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                
                if (!ChunkUtils.canPlayerBreakInChunk( player, signPos ))
                    return Either.left(new LiteralText("Can't build here"));
                
                (new Thread(() -> {
                    WarpUtils warp = new WarpUtils( player, signPos.down() );
                    if (!warp.build(player, player.getServerWorld())) {
                        // Notify the player
                        player.sendMessage(
                            new LiteralText("Can't build that here").formatted(Formatting.RED),
                            MessageType.SYSTEM,
                            ServerCore.spawnID
                        );
                        
                        // Refund the player
                        MoneyUtils.givePlayerMoney(player, SewConfig.get(SewConfig.WARP_WAYSTONE_COST));
                        
                        // Cancel the build
                        return;
                    }
                    warp.save(player.getServerWorld(), warp.getSafeTeleportPos( player.getEntityWorld() ), player);
                })).start();
                return Either.right( true );
                
            } catch (NotEnoughMoneyException e) {
                return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
            }
        }
    },
    /*
     * Allow players to sell chunks/region in their towns
     */
    DEED( Formatting.DARK_GRAY ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign(signBuilder, creator);
            
            SignBlockEntity sign = signBuilder.getSign();
            
            // Get the cost for the DEED
            if (!signBuilder.textMatchPrice(signBuilder.getLines()[2]))
                throw new ShopBuilderException(new LiteralText("Sign is missing a cost"));
            
            // These should NOT be null
            if (sign.getWorld() == null)
                throw new ShopBuilderException(TranslatableServerSide.text(creator, "shop.error.database"));
            
            WorldChunk chunk = sign.getWorld().getWorldChunk( sign.getPos() );
            ClaimantTown town = null;
            
            // Get the deed type
            String deedType = ( signBuilder.getLines()[1].asString().equalsIgnoreCase("region") ? "region" : "chunk" );
            
            // Handle the deed type
            switch (deedType) {
                case "chunk": {
                    // Check that the sign is within a town
                    if ((chunk == null) || ((town = ((IClaimedChunk) chunk).getTown()) == null))
                        throw new ShopBuilderException(new LiteralText("Deed sign must be placed within a town."));
                    
                    // Check who placed the sign
                    if (!(creator.getUuid().equals(town.getOwner())) && creator.getUuid().equals(((IClaimedChunk) chunk).getOwner()))
                        throw new ShopBuilderException(new LiteralText("Deed signs may only be placed in chunks belonging to the town owner, by the town owner."));
    
                    sign.setTextOnRow( 1, new LiteralText(CasingUtils.Sentence( deedType )));
                    break;
                }
                case "region": {
                    BlockPos firstPos;
                    BlockPos secondPos;
                    
                    // Check that the region is defined
                    if (((firstPos = ((PlayerData) creator).getRulerA()) == null) || ((secondPos = ((PlayerData) creator).getRulerB()) == null))
                        throw new ShopBuilderException(new LiteralText("Deed sign must be within a valid region. Use \"").append(new LiteralText("/ruler").formatted(Formatting.AQUA)).append("\" command to select two points"));
                    
                    // Check that the region contains the sign
                    BlockDistance region = new BlockDistance( firstPos, secondPos );
                    if (!region.isWithin( sign.getPos() ))
                        throw new ShopBuilderException(new LiteralText("Deed sign must be within a valid region. Use \"").append(new LiteralText("/ruler").formatted(Formatting.AQUA)).append("\" command to select two points"));
                    
                    // Clear ruler area
                    ((PlayerData) creator).setRulerA( null );
                    ((PlayerData) creator).setRulerB( null );
                    
                    // Validate the minimum and maximum widths
                    int maxWidth = SewConfig.get(SewConfig.MAXIMUM_REGION_WIDTH);
                    int minWidth = SewConfig.get(SewConfig.MINIMUM_REGION_WIDTH);
                    
                    // Check the size of the region
                    if ((maxWidth > 0) && ((region.getNorthSouth() > maxWidth) || (region.getEastWest() > maxWidth)))
                        throw new ShopBuilderException(new LiteralText("Deed region is too large."));
                    if ((region.getNorthSouth() < minWidth) || (region.getEastWest() < minWidth))
                        throw new ShopBuilderException(new LiteralText("Deed region is too small."));
                    
                    // Update the sign to display the width
                    sign.setTextOnRow( 1, region.displayDimensions());
                    signBuilder.regionPositioning(firstPos, secondPos);
                    break;
                }
                default:
                    return false;
            }
            
            // Update the text on the sign
            sign.setTextOnRow( 2, new LiteralText("for ").formatted(Formatting.BLACK).append(
                new LiteralText("$" + signBuilder.shopPrice()).formatted(Formatting.DARK_BLUE)
            ));
            sign.setTextOnRow( 3, (town == null ? new LiteralText("") : town.getName()).formatted(Formatting.DARK_GRAY));
            
            // Set the sign owner
            signBuilder.shopOwner(creator.getUuid());
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
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
    PLOTS( Formatting.GREEN ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign(signBuilder, creator);
            
            // Break if not in creative mode
            if (!creator.isCreative())
                return false;
            
            // Parse the String from the sign
            if ((!signBuilder.textMatchCount(signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
                return false;
            
            NumberFormat formatter = NumberFormat.getInstance();
            
            SignBlockEntity sign = signBuilder.getSign();
            sign.setTextOnRow( 1, new LiteralText(formatter.format(signBuilder.itemSize()) + " chunks"));
            sign.setTextOnRow( 2,
                new LiteralText("for ").formatted(Formatting.BLACK)
                    .append(new LiteralText("$" + signBuilder.shopPrice()).formatted(Formatting.DARK_BLUE))
            );
            sign.setTextOnRow( 3, new LiteralText(""));
            
            // Set the sign owner to SPAWN
            signBuilder.shopOwner( CoreMod.spawnID );
            
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(ServerPlayerEntity player, BlockPos signPos, ShopSignBlockEntity sign) {
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
                CoreMod.logInfo( player.getName().asString() + " bought " + NumberFormat.getInstance().format( sign.getShopItemCount() ) + " chunks for $" + NumberFormat.getInstance().format( sign.getShopItemPrice() ) );
                
            } catch (NotEnoughMoneyException e) {
                return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
            }
            
            player.sendMessage(new LiteralText("Chunks claimed ").formatted(Formatting.YELLOW)
                .append(new LiteralText(NumberFormat.getInstance().format( claim.getCount() )).formatted(Formatting.AQUA))
                .append(" / ")
                .append(new LiteralText(NumberFormat.getInstance().format( claim.getMaxChunkLimit() )).formatted(Formatting.AQUA)),
                MessageType.SYSTEM,
                ServerCore.spawnID
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
    GUIDES( Formatting.DARK_GREEN ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign(signBuilder, creator);
            
            SignBlockEntity sign = signBuilder.getSign();
            
            if (creator.isCreative()) {
                // Set the sign owner
                signBuilder.shopOwner(CoreMod.spawnID);
                
                String guideName = CasingUtils.Sentence(signBuilder.getLines()[1].asString());
                
                sign.setTextOnRow(1, new LiteralText(guideName));
                sign.setTextOnRow(2, new LiteralText(""));
                sign.setTextOnRow(3, new LiteralText(""));
                
                return true;
            }
            return false;
        }
        @Override
        public Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign) {
            // Get the guides title
            String bookRawTitle = sign.getSignLine( 1 ).asString();
            
            // Get the guidebook
            GuideUtils guide;
            try {
                if ((guide = GuideUtils.getBook(bookRawTitle.toLowerCase())) == null)
                    return Either.right( false );
            } catch (JsonSyntaxException e) {
                CoreMod.logError( e );
                return Either.left(new LiteralText("An error occurred getting that guide."));
            }
            
            // Create the object
            ItemStack book = new ItemStack( Items.WRITTEN_BOOK );
            CompoundTag nbt = book.getOrCreateTag();
            
            // Write the guide data to NBT
            guide.writeCustomDataToTag( nbt );
            
            // Give the player the book
            player.giveItemStack( book );
            player.playSound( SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f );
            return Either.right( true );
        }
    },
    /*
     * Player backpacks
     */
    BACKPACK( Formatting.YELLOW ) {
        @Override
        public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
            super.formatSign(signBuilder, creator);
            
            if (!creator.isCreative())
                return false;
            
            // Parse the String from the sign
            if ((!signBuilder.textMatchCount(signBuilder.getLines()[1])) || (!signBuilder.textMatchPrice(signBuilder.getLines()[2])))
                return false;
            
            if ((signBuilder.itemSize() % 9 != 0 ) || signBuilder.itemSize() > 54)
                throw new ShopBuilderException(new LiteralText("Backpack size must be multiples of 9, no larger than 54."));
            
            NumberFormat formatter = NumberFormat.getInstance();
            
            SignBlockEntity sign = signBuilder.getSign();
            sign.setTextOnRow( 1, new LiteralText(formatter.format(signBuilder.itemSize()) + " slots"));
            sign.setTextOnRow( 2,
                new LiteralText("for ").formatted(Formatting.BLACK)
                    .append(new LiteralText("$" + signBuilder.shopPrice()).formatted(Formatting.DARK_BLUE))
            );
            sign.setTextOnRow( 3, new LiteralText(""));
            
            // Set the sign owner to SPAWN
            signBuilder.shopOwner( CoreMod.spawnID );
            
            return true;
        }
        @Override
        public Either<Text, Boolean> onInteract(ServerPlayerEntity player, BlockPos signPos, ShopSignBlockEntity sign) {
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
                    .append(new LiteralText(NumberFormat.getInstance().format( sign.getShopItemCount() )).formatted(Formatting.AQUA)),
                    MessageType.SYSTEM,
                    ServerCore.spawnID
                );
                
                // Log the transaction
                CoreMod.logInfo( player.getName().asString() + " bought a " + NumberFormat.getInstance().format( sign.getShopItemCount() ) + " slot backpack for $" + NumberFormat.getInstance().format( sign.getShopItemPrice() ) );
                
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
    
    private final Formatting[] formattings;
    
    ShopSigns(Formatting... formatting) {
        this.formattings = formatting;
    }
    
    public boolean isEnabled() { return true; }
    
    public final Formatting[] getFormatting() {
        return this.formattings;
    }
    public final LootableContainerBlockEntity getAttachedChest(final World world, final BlockPos signPos) {
        List<BlockPos> checkPositions = new ArrayList<>();
        
        // Add the blockPos BELOW the sign
        checkPositions.add( signPos.offset(Direction.DOWN, 1) );
        
        // Add the blockPos BEHIND the sign
        BlockState signBlockState = world.getBlockState( signPos );
        if ( signBlockState.getBlock() instanceof WallSignBlock ) {
            Direction signFacing = signBlockState.get(HorizontalFacingBlock.FACING).getOpposite();
            checkPositions.add(signPos.offset(signFacing, 1));
        }
        
        for ( BlockPos blockPos : checkPositions ) {
            BlockEntity checkBlockEntity = world.getBlockEntity( blockPos );
            if ( checkBlockEntity instanceof ChestBlockEntity || checkBlockEntity instanceof BarrelBlockEntity ) {
                return (LootableContainerBlockEntity) checkBlockEntity;
            }
        }
        return null;
    }
    protected final LootableContainerBlockEntity getAttachedChest(@NotNull final ShopSignBuilder signBuilder) {
        return this.getAttachedChest( signBuilder.getSign().getWorld(), signBuilder.getSign().getPos() );
    }
    protected final boolean hasAttachedChest(@NotNull final ShopSignBuilder signBuilder) {
        return ( this.getAttachedChest( signBuilder ) != null );
    }
    
    @Nullable
    public static ShopSigns valueOf(Text text) {
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
    
    public abstract Either<Text, Boolean> onInteract(final ServerPlayerEntity player, final BlockPos signPos, final ShopSignBlockEntity sign);
    public boolean formatSign(final ShopSignBuilder signBuilder, final ServerPlayerEntity creator) throws ShopBuilderException {
        SignBlockEntity sign = signBuilder.getSign();
        
        // Set the first row formatting
        sign.setTextOnRow( 0, new LiteralText( "[" + this.name() + "]" ).formatted(this.getFormatting()) );
        
        return true;
    }
}
