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

package net.TheElm.project.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.blocks.entities.LecternGuideBlockEntity;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.interfaces.ShopSignData;
import net.TheElm.project.utilities.BlockUtils;
import net.TheElm.project.utilities.CommandUtils;
import net.TheElm.project.utilities.GuideUtils;
import net.TheElm.project.utilities.InventoryUtils;
import net.TheElm.project.utilities.RankUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class ModCommands {
    private ModCommands() {
    }
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, CoreMod.MOD_ID, (builder) -> builder
            .requires(CommandPredicate.opLevel(OpLevels.STOP))
            .then(CommandManager.literal("reload")
                .then(CommandManager.literal("config")
                    .executes(ModCommands::reloadConfig)
                )
                .then(CommandManager.literal("permissions")
                    .requires(CommandPredicate.isEnabled(SewConfig.HANDLE_PERMISSIONS))
                    .executes(ModCommands::reloadPermissions)
                )
            )
            .then(CommandManager.literal("shops")
                .then(CommandManager.literal("change")
                    .then(CommandManager.literal("item")
                        .then(CommandManager.literal("hand")
                            .executes(ModCommands::shopSignChangeItemToHand)
                        )
                        .then(CommandManager.literal("inventory")
                            .executes(ModCommands::shopSignChangeItemToContainer)
                        )
                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                            .executes(ModCommands::shopSignChangeItem)
                        )
                    )
                    .then(CommandManager.literal("owner")
                        .then(CommandManager.argument("owner", GameProfileArgumentType.gameProfile())
                            .suggests(CommandUtils::getAllPlayerNames)
                            .executes(ModCommands::shopSignChangeOwner)
                        )
                    )
                    .then(CommandManager.literal("trader")
                        .then(CommandManager.literal("source")
                            .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(ModCommands::shopSignChangeSoundSourceLocation)
                            )
                        )
                    )
                )
            )
            .then(CommandManager.literal("guides")
                .then(CommandManager.argument("book", StringArgumentType.string())
                    .suggests(((context, suggestionsBuilder) -> CommandSource.suggestMatching(GuideUtils.getBooks(), suggestionsBuilder)))
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("target", EntityArgumentType.players())
                            .executes(ModCommands::givePlayersGuideBook)
                        )
                    )
                    .then(CommandManager.literal("set")
                        .executes(ModCommands::setLecternGuide)
                    )
                )
                .executes(ModCommands::unsetLecternGuide)
            )
        );
    }
    
    private static int reloadConfig(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            // Reload the config
            SewConfig.reload();
            source.sendFeedback(new LiteralText("Config has been reloaded.").formatted(Formatting.GREEN), true);
            
            // Re-send the command-tree to all players
            ModCommands.reloadCommandTree(source.getMinecraftServer(), false);
            
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendFeedback(new LiteralText("Failed to reload config, see console for errors.").formatted(Formatting.RED), true);
            CoreMod.logError( e );
            return -1;
        }
    }
    
    private static int reloadPermissions(@NotNull CommandContext<ServerCommandSource> context) {
        boolean success = RankUtils.reload();
        ServerCommandSource source = context.getSource();
        
        if (!success)
            source.sendFeedback(new LiteralText("Failed to reload permissions, see console for errors").formatted(Formatting.RED), true);
        else{
            ModCommands.reloadCommandTree(source.getMinecraftServer(), true);
            source.sendFeedback(new LiteralText("Permissions file has been reloaded").formatted(Formatting.GREEN), true);
        }
        
        return success ? Command.SINGLE_SUCCESS : -1;
    }
    
    private static void reloadCommandTree(@NotNull MinecraftServer server, boolean reloadPermissions) {
        PlayerManager playerManager = server.getPlayerManager();
        
        // Clear permissions
        if (reloadPermissions)
            RankUtils.clearRanks();

        // Resend the player the command tree
        for (ServerPlayerEntity player : playerManager.getPlayerList())
            playerManager.sendCommandTree(player);
    }
    
    private static int shopSignChangeOwner(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntityOrThrow();
        ServerWorld world = source.getWorld();
        
        BlockHitResult hitResult = BlockUtils.getLookingBlock(world, entity);
        if (hitResult.getType() == HitResult.Type.MISS) {
            source.sendError(new LiteralText("Could not find targeted block."));
            return 0;
        }
        BlockPos signPos = hitResult.getBlockPos();
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, "owner");
        GameProfile targetPlayer = gameProfiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData)) {
            source.sendError(new LiteralText("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // Get the entity as the shop sign
        ShopSignData shop = (ShopSignData) blockEntity;
        
        // Update the owner of the shop to the target
        shop.setShopOwner(targetPlayer.getId());
        
        // Re-Render the sign after updating the owner
        shop.renderSign();
        ServerCore.markDirty(world, signPos);
        
        source.sendFeedback(new LiteralText("Updated shop owner.").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }
    private static int shopSignChangeItemToHand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Change the item to that of the offhand
        return ModCommands.shopSignChangeItem(context, player.getOffHandStack());
    }
    private static int shopSignChangeItemToContainer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        BlockPos signPos = BlockPosArgumentType.getBlockPos(context, "pos");
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData)) {
            source.sendError(new LiteralText("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // Get the entity as the shop sign
        ShopSignData shop = (ShopSignData) blockEntity;
        if (Objects.equals(shop.getShopOwner(), CoreMod.SPAWN_ID)) {
            source.sendError(new LiteralText("Cannot get attached containers of infinite Shop Signs."));
            return 0;
        }
        
        LootableContainerBlockEntity attachedChest = InventoryUtils.getAttachedChest(shop);
        Inventory inventory;
        if (attachedChest == null || (inventory = InventoryUtils.getInventoryOf(attachedChest)) == null) {
            source.sendError(new LiteralText("Could not find attached storage container."));
            return 0;
        }
        
        return ModCommands.shopSignChangeItem(context, InventoryUtils.getFirstStack(inventory));
    }
    private static int shopSignChangeItem(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStackArgument item = ItemStackArgumentType.getItemStackArgument(context, "item");
        return ModCommands.shopSignChangeItem(context, item.createStack(1, false));
    }
    private static int shopSignChangeItem(@NotNull CommandContext<ServerCommandSource> context, @NotNull ItemStack stack) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntityOrThrow();
        ServerWorld world = source.getWorld();
        
        BlockHitResult hitResult = BlockUtils.getLookingBlock(world, entity);
        if (hitResult.getType() == HitResult.Type.MISS) {
            source.sendError(new LiteralText("Could not find targeted block."));
            return 0;
        }
        BlockPos signPos = hitResult.getBlockPos();
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData)) {
            source.sendError(new LiteralText("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // If the item that is being set is AIR
        if (Items.AIR.equals(stack.getItem())) {
            source.sendError(new LiteralText("Cannot change the Shop Sign to that item."));
            return 0;
        }
        
        // Get the entity as the shop sign
        ShopSignData shop = (ShopSignData) blockEntity;
        
        // Update the item of the shop
        shop.setItem(stack);
        
        // Re-Render the sign after updating the owner
        shop.renderSign();
        ServerCore.markDirty(world, signPos);
        
        source.sendFeedback(new LiteralText("Updated shop item to ")
            .formatted(Formatting.GREEN)
            .append(new TranslatableText(shop.getShopItemTranslationKey()).formatted(Formatting.AQUA))
            .append("."), false);
        return Command.SINGLE_SUCCESS;
    }
    private static int shopSignChangeSoundSourceLocation(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntityOrThrow();
        ServerWorld world = source.getWorld();
        
        BlockPos setSoundSource = BlockPosArgumentType.getBlockPos(context, "pos");
        
        BlockHitResult hitResult = BlockUtils.getLookingBlock(world, entity);
        if (hitResult.getType() == HitResult.Type.MISS) {
            source.sendError(new LiteralText("Could not find targeted block."));
            return 0;
        }
        BlockPos signPos = hitResult.getBlockPos();
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData)) {
            source.sendError(new LiteralText("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // Get the entity as the shop sign
        ShopSignData shop = (ShopSignData) blockEntity;
        shop.setSoundSourcePosition(setSoundSource);
        
        source.sendFeedback(new LiteralText("Updated sound location.").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }
    
    private static int givePlayersGuideBook(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the name of the guide to create a lectern for
        String bookName = StringArgumentType.getString(context, "book");
        GuideUtils guide = GuideUtils.getBook(bookName);
        if (guide == null) {
            source.sendError(new LiteralText("Could not find the guide book \"" + bookName + "\"."));
            return 0;
        }
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        for (ServerPlayerEntity player : players)
            player.inventory.offerOrDrop(player.world, guide.newStack());
        
        source.sendFeedback(new LiteralText("Gave guide book \"")
            .append(new LiteralText(bookName).formatted(Formatting.AQUA))
            .append("\"")
            .append(" to ")
            .append(MessageUtils.formatNumber(players.size(), Formatting.AQUA))
            .append(" players.")
            .formatted(Formatting.YELLOW), true);
        return players.size();
    }
    private static int unsetLecternGuide(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return ModCommands.setLecternGuide(context, null);
    }
    private static int setLecternGuide(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return ModCommands.setLecternGuide(context, StringArgumentType.getString(context, "book"));
    }
    private static int setLecternGuide(@NotNull CommandContext<ServerCommandSource> context, @Nullable String bookName) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Either<LecternGuideBlockEntity, String> either = BlockUtils.getLecternBlockEntity(
            source.getWorld(),
            source.getEntityOrThrow(),
            LecternGuideBlockEntity.class, LecternGuideBlockEntity::new
        );
        Optional<LecternGuideBlockEntity> optionalLectern = either.left();
        Optional<String> error = either.right();
        
        if (error.isPresent())
            source.sendError(new LiteralText(error.get()));
        else if (optionalLectern.isPresent()) {
            LecternGuideBlockEntity guideBlockEntity = optionalLectern.get();
            
            // Update the guide name
            guideBlockEntity.setGuide(bookName);
            
            source.sendFeedback(ModCommands.bookFeedback(bookName).formatted(Formatting.YELLOW), true);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
    private static MutableText bookFeedback(@Nullable String bookName) {
        if (bookName == null)
            return new LiteralText("Cleared guide book from lectern.");
        return new LiteralText("Lectern updated to guide book \"")
            .append(new LiteralText(bookName).formatted(Formatting.AQUA))
            .append("\".");
    }
}