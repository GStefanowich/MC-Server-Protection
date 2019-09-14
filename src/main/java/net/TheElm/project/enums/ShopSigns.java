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
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.exceptions.NbtNotFoundException;
import net.TheElm.project.exceptions.NotEnoughMoneyException;
import net.TheElm.project.exceptions.ShopBuilderException;
import net.TheElm.project.interfaces.IClaimedChunk;
import net.TheElm.project.interfaces.PlayerData;
import net.TheElm.project.interfaces.ShopSignBlockEntity;
import net.TheElm.project.protections.BlockDistance;
import net.TheElm.project.protections.claiming.ClaimantPlayer;
import net.TheElm.project.protections.claiming.ClaimantTown;
import net.TheElm.project.utilities.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
import net.minecraft.world.dimension.DimensionType;
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
            if ( !SewingMachineConfig.INSTANCE.DO_MONEY.get() )
                return Either.right( true );
            
            if ((sign.getShopItem() == null) || (sign.getShopOwner() == null) || (sign.getShopItemCount() == null) || (sign.getShopItemPrice() == null))
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
                    if (player.inventory.countInInv(sign.getShopItem()) < sign.getShopItemCount())
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
                    
                    ClaimantPlayer permissions = ClaimantPlayer.get(sign.getShopOwner());
                    
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
            return SewingMachineConfig.INSTANCE.DO_MONEY.get();
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
            if ( !SewingMachineConfig.INSTANCE.DO_MONEY.get() )
                return Either.right( true );
            
            // Check if the attached chest exists
            if (CoreMod.spawnID.equals(sign.getShopOwner()) || ((chest = this.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.self_buy"));
                
                /*
                 * Check if chest is valid
                 */
                if ( chest != null ) {
                    chestInventory = InventoryUtils.getInventoryOf( player.getEntityWorld(), chest.getPos() );
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                        return Either.left(TranslatableServerSide.text(player, "shop.error.chest_open"));
                    // If there is not enough of item in chest
                    if (chest.countInInv(sign.getShopItem()) < sign.getShopItemCount())
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
                    
                    ClaimantPlayer permissions = ClaimantPlayer.get(sign.getShopOwner());
                    
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
            return SewingMachineConfig.INSTANCE.DO_MONEY.get();
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
            if ( !SewingMachineConfig.INSTANCE.DO_MONEY.get() )
                return Either.right( true );
            
            // Check if the attached chest exists
            if (CoreMod.spawnID.equals(sign.getShopOwner()) || ((chest = this.getAttachedChest( player.getEntityWorld(), signPos )) != null)) {
                if (player.getUuid().equals(sign.getShopOwner()))
                    return Either.left(new LiteralText("Cannot buy items from yourself."));
                
                /*
                 * Check if chest is valid
                 */
                if ( chest != null ) {
                    chestInventory = InventoryUtils.getInventoryOf( player.getEntityWorld(), chest.getPos() );
                    
                    // If the chest is open
                    if (ChestBlockEntity.getPlayersLookingInChestCount(player.getEntityWorld(), chest.getPos()) > 0)
                        return Either.left(new LiteralText("Cannot do that while chest is open."));
                    // If there is not enough of item in chest
                    if (chest.countInInv(sign.getShopItem()) < sign.getShopItemCount()) {
                        return Either.left(new LiteralText("Chest is out of " + sign.getShopItemDisplay() + "."));
                    }
                }
                
                // Give item to player from chest
                if (!InventoryUtils.chestToPlayer( player, signPos, chestInventory, player.inventory, sign.getShopItem(), sign.getShopItemCount(), true ))
                    return Either.left(new LiteralText("Chest is out of " + sign.getShopItemDisplay() + "."));
                
                ClaimantPlayer permissions = ClaimantPlayer.get(sign.getShopOwner());
                
                // Log the event
                CoreMod.logInfo( player.getName().asString() + " got " + NumberFormat.getInstance().format( sign.getShopItemCount() ) + " " + sign.getShopItemDisplay() + " from " + permissions.getName().asString() );
                
                return Either.right( true );
            }
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return SewingMachineConfig.INSTANCE.DO_MONEY.get();
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
            if ( !SewingMachineConfig.INSTANCE.DO_MONEY.get() )
                return Either.right( true );
            
            long playerHas = MoneyUtils.getPlayerMoney( player );
            player.sendMessage(TranslatableServerSide.text( player, "player.money",
                playerHas
            ));
            
            return Either.right( true );
        }
        @Override
        public boolean isEnabled() {
            return SewingMachineConfig.INSTANCE.DO_MONEY.get();
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
            WarpUtils.Warp warp = WarpUtils.getPlayerWarp( player.getUuid() );
            if ( warp == null ) {
                // Create new warp
                if ( WarpUtils.isPlayerCreating( player ) )
                    return Either.right( false );
                // Make a new warp
                return Either.right(this.generateNewWarp(player));
            } else {
                // Warp the player to their home
                this.teleportPlayer( warp.world, warp.warpPos, player );
            }
            return Either.right( true );
        }
        @Override
        public boolean isEnabled() {
            return SewingMachineConfig.INSTANCE.WARP_MAX_DISTANCE.get() > 0;
        }
        private boolean generateNewWarp(final ServerPlayerEntity player) {
            // Create a new warp point asynchronously
            new Thread(() -> {
                // Get world info
                final MinecraftServer server;
                if ((server = player.getServer()) == null)
                    return;
                final World world = server.getWorld(DimensionType.OVERWORLD);
                final BlockPos spawnPos = WarpUtils.getWorldSpawn( world );
                
                // Tell the player
                player.sendChatMessage(TranslatableServerSide.text(
                    player,
                    "warp.random.search"
                ), MessageType.CHAT);
                
                // Create warp
                WarpUtils newWarp = new WarpUtils( player, spawnPos );
                BlockPos warpToPos;
                
                while (((warpToPos = newWarp.getWarpPositionIn(world)) == null) || (!newWarp.build(player, world)));
                
                // Get the distance
                int distance = warpToPos.getManhattanDistance(spawnPos);
                
                // Build the return warp
                player.sendChatMessage(TranslatableServerSide.text(
                    player,
                    "warp.random.build"
                ), MessageType.CHAT);
                
                // Teleport the player
                BlockPos safeTeleportPos = newWarp.getSafeTeleportPos(world);
                this.teleportPlayer(world, safeTeleportPos, player);
                
                // Save the warp for later
                newWarp.save(world, safeTeleportPos, player);
                
                // Notify the player of their new location
                player.sendChatMessage(TranslatableServerSide.text(
                    player,
                    "warp.random.teleported",
                    distance
                ), MessageType.CHAT);
            }).start();
            return true;
        }
        private void teleportPlayer(@NotNull final World world, @NotNull final BlockPos warpPos, @NotNull final ServerPlayerEntity player) {
            WarpUtils.teleportPlayer( world, player, warpPos );
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
            BlockPos blockPos = sign.getPos();
            int x = blockPos.getX();
            int z = blockPos.getZ();
            if ((x % 16 == 0) || ((x + 1) % 16 == 0) || (z % 16 == 0) || ((z + 1) % 16 == 0))
                throw new ShopBuilderException(new LiteralText("Can't place waystones on the border of a chunk."));
            
            // Set the signs price
            Text priceText = new LiteralText("$" + SewingMachineConfig.INSTANCE.WARP_WAYSTONE_COST.get()).formatted(Formatting.DARK_BLUE);
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
                if (!MoneyUtils.takePlayerMoney(player, SewingMachineConfig.INSTANCE.WARP_WAYSTONE_COST.get()))
                    return Either.left(TranslatableServerSide.text(player, "shop.error.money_player"));
                
                if (!ChunkUtils.canPlayerBreakInChunk( player, signPos ))
                    return Either.left(new LiteralText("Can't build here"));
                
                (new Thread(() -> {
                    WarpUtils warp = new WarpUtils( player, signPos.down() );
                    if (!warp.build(player, player.getServerWorld())) {
                        // Notify the player
                        player.sendMessage(new LiteralText("Can't build that here").formatted(Formatting.RED));
                        
                        // Refund the player
                        MoneyUtils.givePlayerMoney(player, SewingMachineConfig.INSTANCE.WARP_WAYSTONE_COST.get());
                        
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
                    int maxWidth = SewingMachineConfig.INSTANCE.MAXIMUM_REGION_WIDTH.get();
                    int minWidth = SewingMachineConfig.INSTANCE.MINIMUM_REGION_WIDTH.get();
                    
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
            if (!(SewingMachineConfig.INSTANCE.DO_MONEY.get() && SewingMachineConfig.INSTANCE.DO_CLAIMS.get()))
                return Either.right( true );
            
            if ((sign.getFirstPos() == null) || (sign.getSecondPos() == null))
                return Either.left(new LiteralText("Invalid deed sign"));
            
            
            
            return Either.right( false );
        }
        @Override
        public boolean isEnabled() {
            return (SewingMachineConfig.INSTANCE.DO_MONEY.get() && SewingMachineConfig.INSTANCE.DO_CLAIMS.get());
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
