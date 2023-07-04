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

package net.theelm.sewingmachine.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.base.config.SewCoreConfig;
import net.theelm.sewingmachine.blocks.entities.LecternGuideBlockEntity;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.interfaces.SewPlugin;
import net.theelm.sewingmachine.interfaces.ShopSignData;
import net.theelm.sewingmachine.utilities.BlockUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.GuideUtils;
import net.theelm.sewingmachine.utilities.InventoryUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ModCommands extends SewCommand {
    private final @NotNull List<SewPlugin> plugins;
    
    public ModCommands(@NotNull List<SewPlugin> plugins) {
        this.plugins = plugins;
    }
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess access) {
        CommandUtils.register(dispatcher, CoreMod.MOD_ID, (builder) -> {
            builder.requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.ADMIN_CLAIM_SHOPS))
                .then(CommandManager.literal("reload")
                    .requires(CommandPredicate.opLevel(OpLevels.STOP).or(PermissionNodes.ALL_PERMISSIONS))
                    .then(CommandManager.literal("config")
                        .requires(CommandPredicate.isEnabled(SewCoreConfig.HOT_RELOADING))
                        .executes(this::reloadConfig)
                    )
                )
                .then(CommandManager.literal("shops")
                    .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.ADMIN_CLAIM_SHOPS))
                    .then(CommandManager.literal("change")
                        .then(CommandManager.literal("item")
                            .then(CommandManager.literal("hand")
                                .executes(this::shopSignChangeItemToHand)
                            )
                            .then(CommandManager.literal("inventory")
                                .executes(this::shopSignChangeItemToContainer)
                            )
                            .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(access))
                                .executes(this::shopSignChangeItem)
                            )
                        )
                        .then(CommandManager.literal("owner")
                            .then(CommandManager.argument("owner", GameProfileArgumentType.gameProfile())
                                .suggests(CommandUtils::getAllPlayerNames)
                                .executes(this::shopSignChangeOwner)
                            )
                        )
                        .then(CommandManager.literal("trader")
                            .then(CommandManager.literal("source")
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                    .executes(this::shopSignChangeSoundSourceLocation)
                                )
                            )
                        )
                    )
                )
                .then(CommandManager.literal("guides")
                    .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                    .then(CommandManager.argument("book", StringArgumentType.string())
                        .suggests(((context, suggestionsBuilder) -> CommandSource.suggestMatching(GuideUtils.getBooks(), suggestionsBuilder)))
                        .then(CommandManager.literal("give")
                            .then(CommandManager.argument("target", EntityArgumentType.players())
                                .executes(this::givePlayersGuideBook)
                            )
                        )
                        .then(CommandManager.literal("set")
                            .executes(this::setLecternGuide)
                        )
                    )
                    .executes(this::unsetLecternGuide)
                );
                
                for (SewPlugin plugin : this.plugins)
                    plugin.updatePrimaryCommand(builder, access);
            }
        );
    }
    
    private int reloadConfig(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            // Reload the config
            SewConfig.reload();
            source.sendFeedback(
                () -> Text.literal("Config has been reloaded.").formatted(Formatting.GREEN),
                true
            );
            
            // Re-send the command-tree to all players
            CommandUtils.resendTree(source.getServer());
            
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendFeedback(
                () -> Text.literal("Failed to reload config, see console for errors.").formatted(Formatting.RED),
                true
            );
            CoreMod.logError( e );
            return -1;
        }
    }
    
    private int shopSignChangeOwner(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntityOrThrow();
        ServerWorld world = source.getWorld();
        
        BlockHitResult hitResult = BlockUtils.getLookingBlock(world, entity);
        if (hitResult.getType() == HitResult.Type.MISS) {
            source.sendError(Text.literal("Could not find targeted block."));
            return 0;
        }
        BlockPos signPos = hitResult.getBlockPos();
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // Get the target player
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, "owner");
        GameProfile targetPlayer = gameProfiles.stream().findAny()
            .orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData shop)) {
            source.sendError(Text.literal("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // Update the owner of the shop to the target
        shop.setShopOwner(targetPlayer.getId());
        
        // Re-Render the sign after updating the owner
        shop.renderSign();
        BlockUtils.markDirty(world, signPos);
        
        source.sendFeedback(
            () -> Text.literal("Updated shop owner.").formatted(Formatting.GREEN),
            false
        );
        return Command.SINGLE_SUCCESS;
    }
    private int shopSignChangeItemToHand(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        // Change the item to that of the offhand
        return this.shopSignChangeItem(context, player.getOffHandStack());
    }
    private int shopSignChangeItemToContainer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        
        BlockPos signPos = BlockPosArgumentType.getBlockPos(context, "pos");
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData shop)) {
            source.sendError(Text.literal("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // Get the entity as the shop sign
        if (Objects.equals(shop.getShopOwner(), CoreMod.SPAWN_ID)) {
            source.sendError(Text.literal("Cannot get attached containers of infinite Shop Signs."));
            return 0;
        }
        
        LootableContainerBlockEntity attachedChest = InventoryUtils.getAttachedChest(shop);
        Inventory inventory;
        if (attachedChest == null || (inventory = InventoryUtils.getInventoryOf(attachedChest)) == null) {
            source.sendError(Text.literal("Could not find attached storage container."));
            return 0;
        }
        
        return this.shopSignChangeItem(context, InventoryUtils.getFirstStack(inventory));
    }
    private int shopSignChangeItem(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ItemStackArgument item = ItemStackArgumentType.getItemStackArgument(context, "item");
        return this.shopSignChangeItem(context, item.createStack(1, false));
    }
    private int shopSignChangeItem(@NotNull CommandContext<ServerCommandSource> context, @NotNull ItemStack stack) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntityOrThrow();
        ServerWorld world = source.getWorld();
        
        BlockHitResult hitResult = BlockUtils.getLookingBlock(world, entity);
        if (hitResult.getType() == HitResult.Type.MISS) {
            source.sendError(Text.literal("Could not find targeted block."));
            return 0;
        }
        BlockPos signPos = hitResult.getBlockPos();
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData shop)) {
            source.sendError(Text.literal("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // If the item that is being set is AIR
        if (Items.AIR.equals(stack.getItem())) {
            source.sendError(Text.literal("Cannot change the Shop Sign to that item."));
            return 0;
        }
        
        // Update the item of the shop
        shop.setItem(stack);
        
        // Re-Render the sign after updating the owner
        shop.renderSign();
        BlockUtils.markDirty(world, signPos);
        
        source.sendFeedback(
            () -> Text.literal("Updated shop item to ")
                .formatted(Formatting.GREEN)
                .append(Text.translatable(shop.getShopItemTranslationKey()).formatted(Formatting.AQUA))
                .append("."),
            false
        );
        return Command.SINGLE_SUCCESS;
    }
    private int shopSignChangeSoundSourceLocation(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity entity = source.getEntityOrThrow();
        ServerWorld world = source.getWorld();
        
        BlockPos setSoundSource = BlockPosArgumentType.getBlockPos(context, "pos");
        
        BlockHitResult hitResult = BlockUtils.getLookingBlock(world, entity);
        if (hitResult.getType() == HitResult.Type.MISS) {
            source.sendError(Text.literal("Could not find targeted block."));
            return 0;
        }
        BlockPos signPos = hitResult.getBlockPos();
        BlockEntity blockEntity = world.getBlockEntity(signPos);
        
        // If the shop sign block was not found
        if (!(blockEntity instanceof ShopSignData shop)) {
            source.sendError(Text.literal("Block at that position is not a Shop Sign."));
            return 0;
        }
        
        // Set the sounce source of the shop
        shop.setSoundSourcePosition(setSoundSource);
        
        source.sendFeedback(
            () -> Text.literal("Updated sound location.").formatted(Formatting.GREEN),
            false
        );
        return Command.SINGLE_SUCCESS;
    }
    
    private int givePlayersGuideBook(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the name of the guide to create a lectern for
        String bookName = StringArgumentType.getString(context, "book");
        GuideUtils guide = GuideUtils.getBook(bookName);
        if (guide == null) {
            source.sendError(Text.literal("Could not find the guide book \"" + bookName + "\"."));
            return 0;
        }
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        for (ServerPlayerEntity player : players)
            player.getInventory().offerOrDrop(guide.newStack());
        
        source.sendFeedback(
            () -> Text.literal("Gave guide book \"")
                .append(Text.literal(bookName).formatted(Formatting.AQUA))
                .append("\"")
                .append(" to ")
                .append(MessageUtils.formatNumber(players.size(), Formatting.AQUA))
                .append(" players.")
                .formatted(Formatting.YELLOW),
            true
        );
        return players.size();
    }
    private int unsetLecternGuide(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.setLecternGuide(context, null);
    }
    private int setLecternGuide(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.setLecternGuide(context, StringArgumentType.getString(context, "book"));
    }
    private int setLecternGuide(@NotNull CommandContext<ServerCommandSource> context, @Nullable String bookName) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Either<LecternGuideBlockEntity, String> either = BlockUtils.getLecternBlockEntity(
            source.getWorld(),
            source.getEntityOrThrow(),
            LecternGuideBlockEntity.class, LecternGuideBlockEntity::new
        );
        Optional<LecternGuideBlockEntity> optionalLectern = either.left();
        Optional<String> error = either.right();
        
        if (error.isPresent())
            source.sendError(Text.literal(error.get()));
        else if (optionalLectern.isPresent()) {
            LecternGuideBlockEntity guideBlockEntity = optionalLectern.get();
            
            // Update the guide name
            guideBlockEntity.setGuide(bookName);
            
            source.sendFeedback(
                () -> this.bookFeedback(bookName).formatted(Formatting.YELLOW),
                true
            );
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
    private MutableText bookFeedback(@Nullable String bookName) {
        if (bookName == null)
            return Text.literal("Cleared guide book from lectern.");
        return Text.literal("Lectern updated to guide book \"")
            .append(Text.literal(bookName).formatted(Formatting.AQUA))
            .append("\".");
    }
}