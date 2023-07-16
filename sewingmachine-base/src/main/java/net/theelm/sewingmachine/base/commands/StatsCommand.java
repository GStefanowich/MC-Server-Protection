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

package net.theelm.sewingmachine.base.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on Mar 08 2021 at 7:51 PM.
 * By greg in SewingMachineMod
 */
public final class StatsCommand implements SewCommand {
    private static final @NotNull SuggestionProvider<ServerCommandSource> KILLABLE_ENTITIES = (context, builder) -> CommandSource.suggestFromIdentifier(Registries.ENTITY_TYPE.stream().filter(t -> t.isSummonable() && !t.getSpawnGroup().equals(SpawnGroup.MISC)), builder, EntityType::getId, (entityType) -> {
        return Text.translatable(Util.createTranslationKey("entity", EntityType.getId(entityType)));
    });
    private static final @NotNull SuggestionProvider<ServerCommandSource> DESTROYED_ITEMS = (context, builder) -> CommandSource.suggestFromIdentifier(Registries.ITEM.stream().filter(t -> t.isDamageable()), builder, Registries.ITEM::getId, (item) -> {
        return Text.translatable(Util.createTranslationKey("entity", Registries.ITEM.getId(item)));
    });
    private static final @NotNull SuggestionProvider<ServerCommandSource> ITEMS = (context, builder) -> CommandSource.suggestFromIdentifier(Registries.ITEM.stream(), builder, Registries.ITEM::getId, (item) -> {
        return Text.translatable(Util.createTranslationKey("entity", Registries.ITEM.getId(item)));
    });
    private static final @NotNull SuggestionProvider<ServerCommandSource> BLOCKS = (context, builder) -> CommandSource.suggestFromIdentifier(Registries.BLOCK.stream(), builder, Registries.BLOCK::getId, (block) -> {
        return Text.translatable(Util.createTranslationKey("entity", Registries.BLOCK.getId(block)));
    });
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess access) {
        CommandUtils.register(dispatcher, "Stat", builder -> builder
            .then(this.itemCommandTree(access, "killed",
                this::getStatKilled,
                this::incStatKilled,
                this::decStatKilled
            ))
            .then(this.itemCommandTree(access, "mined",
                this::getStatMined,
                this::incStatMined,
                this::decStatMined
            ))
            .then(this.itemCommandTree(access, "broken",
                this::getStatBroken,
                this::incStatBroken,
                this::decStatBroken
            ))
            .then(this.itemCommandTree(access, "used",
                this::getStatUsed,
                this::incStatUsed,
                this::decStatUsed
            ))
            .then(this.itemCommandTree(access, "crafted",
                this::getStatCrafted,
                this::incStatCrafted,
                this::decStatCrafted
            ))
            .then(this.itemCommandTree(access, "pickedup",
                this::getStatPickedUp,
                this::incStatPickedUp,
                this::decStatPickedUp
            ))
            .then(this.itemCommandTree(access, "dropped",
                this::getStatDropped,
                this::incStatDropped,
                this::decStatDropped
            ))
        );
    }
    
    private LiteralArgumentBuilder<ServerCommandSource> itemCommandTree(
        CommandRegistryAccess access,
        String argument,
        Command<ServerCommandSource> execute,
        Command<ServerCommandSource> add,
        Command<ServerCommandSource> sub
    ) {
        return CommandManager.literal(argument)
            .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(access))
                .suggests(StatsCommand.ITEMS)
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                        .then(CommandManager.literal("add")
                            .executes(add)
                        )
                        .then(CommandManager.literal("subtract")
                            .executes(sub)
                        )
                    )
                )
                .executes(execute)
            );
    }
    
    private int getStatKilled(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityType<?> entity = this.getEntityType(context, "entity");
        int count = this.getStatOf(context.getSource(), Stats.KILLED, entity);
        
        return this.sendMessageAs(context.getSource(), Text.literal("I have killed ")
            .append(Text.literal("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(Text.translatable(entity.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private int incStatKilled(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.addToStatOf(
            player,
            Stats.KILLED,
            this.getEntityType(context, "entity"),
            difference
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int decStatKilled(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.removeFromStatOf(
            player,
            Stats.KILLED,
            this.getEntityType(context, "entity"),
            difference
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getStatMined(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = this.getItem(context, "block");
        if (!(item instanceof BlockItem blockItem))
            return 0;
        int count = this.getStatOf(context.getSource(), Stats.MINED, blockItem.getBlock());
        
        return this.sendMessageAs(context.getSource(), Text.literal("I have mined ")
            .append(Text.literal("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(Text.translatable(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private int incStatMined(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Item item = this.getItem(context, "block");
        
        source.sendFeedback(() -> {
            int difference = IntegerArgumentType.getInteger(context, "amount");
            if (!(item instanceof BlockItem blockItem))
                difference = 0;
            else {
                this.addToStatOf(
                    player,
                    Stats.MINED,
                    blockItem.getBlock(),
                    difference
                );
            }
            
            return Text.literal("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append(".");
        }, true);
        
        return Command.SINGLE_SUCCESS;
    }
    private int decStatMined(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Item item = this.getItem(context, "block");
        
        source.sendFeedback(() -> {
            int difference = IntegerArgumentType.getInteger(context, "amount");
            if (!(item instanceof BlockItem blockItem))
                difference = 0;
            else {
                this.removeFromStatOf(
                    player,
                    Stats.MINED,
                    blockItem.getBlock(),
                    difference
                );
            }
            
            return Text.literal("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append(".");
        }, true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getStatBroken(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = this.getItem(context, "item");
        int count = this.getStatOf(context.getSource(), Stats.BROKEN, item);
        
        return this.sendMessageAs(context.getSource(), Text.literal("I have broken ")
            .append(Text.literal("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(Text.translatable(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private int incStatBroken(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.addToStatOf(
            player,
            Stats.BROKEN,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int decStatBroken(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.removeFromStatOf(
            player,
            Stats.BROKEN,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getStatUsed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = this.getItem(context, "item");
        int count = this.getStatOf(context.getSource(), Stats.USED, item);
        
        return this.sendMessageAs(context.getSource(), Text.literal("I have used ")
            .append(Text.literal("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(Text.translatable(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private int incStatUsed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.addToStatOf(
            player,
            Stats.USED,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int decStatUsed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.removeFromStatOf(
            player,
            Stats.USED,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getStatCrafted(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = this.getItem(context, "item");
        int count = this.getStatOf(context.getSource(), Stats.CRAFTED, item);
        
        return this.sendMessageAs(context.getSource(), Text.literal("I have crafted ")
            .append(Text.literal("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(Text.translatable(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append(".")
        );
    }
    private int incStatCrafted(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.addToStatOf(
            player,
            Stats.CRAFTED,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int decStatCrafted(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        this.removeFromStatOf(
            player,
            Stats.CRAFTED,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getStatPickedUp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = this.getItem(context, "item");
        int count = this.getStatOf(context.getSource(), Stats.PICKED_UP, item);
        
        return this.sendMessageAs(context.getSource(), Text.literal("I have picked up ")
            .append(Text.literal("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(Text.translatable(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append(".")
        );
    }
    private int incStatPickedUp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        this.addToStatOf(
            player,
            Stats.PICKED_UP,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int decStatPickedUp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        this.removeFromStatOf(
            player,
            Stats.PICKED_UP,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int getStatDropped(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = this.getItem(context, "item");
        int count = this.getStatOf(context.getSource(), Stats.DROPPED, item);
        
        return this.sendMessageAs(context.getSource(), Text.literal("I have dropped ")
            .append(Text.literal("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(Text.translatable(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append(".")
        );
    }
    private int incStatDropped(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        this.addToStatOf(
            player,
            Stats.DROPPED,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private int decStatDropped(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        this.removeFromStatOf(
            player,
            Stats.DROPPED,
            this.getItem(context, "item"),
            difference
        );
        
        source.sendFeedback(
            () -> Text.literal("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."),
            true
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private <T> int getStatOf(@NotNull ServerCommandSource source, @NotNull StatType<T> type, @NotNull T stat) {
        return this.getStatOf(source.getPlayer(), type, stat);
    }
    private <T> int getStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull T stat) {
        return this.getStatOf(player, type, type.getOrCreateStat(stat));
    }
    private <T> int getStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull Stat<T> stat) {
        return player.getStatHandler()
            .getStat(stat);
    }
    private <T> void addToStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull T stat, int count) {
        player.increaseStat(type.getOrCreateStat(stat), count);
    }
    private <T> void removeFromStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull T key, int count) {
        Stat<T> stat = type.getOrCreateStat(key);
        
        int updated = this.getStatOf(player, type, stat)
            - count;
        
        player.resetStat(stat);
        if (updated > 0)
            player.increaseStat(stat, updated);
    }
    private int sendMessageAs(@NotNull ServerCommandSource source, @Nullable Text text) throws CommandSyntaxException {
        if (text == null)
            return 0;
        
        MinecraftServer server = source.getServer();
        PlayerManager players = server.getPlayerManager();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        throw new UnsupportedOperationException();
        
        // The chatroom to send the message in
        /*ChatRooms room = ((PlayerChat)player).getChatRoom();
        
        // Create a chat message
        Text chatText = MessageUtils.formatPlayerMessage(player, room, text);
        
        // Send the new chat message to the currently selected chat room
        MessageUtils.sendTo(room, player, chatText);
        
        return Command.SINGLE_SUCCESS;*/
    }
    
    private EntityType<?> getEntityType(@NotNull CommandContext<ServerCommandSource> context, @NotNull String key) throws CommandSyntaxException {
        Identifier identifier = RegistryEntryArgumentType.getSummonableEntityType(context, key)
            .registryKey()
            .getValue();
        return Registries.ENTITY_TYPE.getOrEmpty(identifier)
            .orElseThrow(ServerCommandSource.REQUIRES_ENTITY_EXCEPTION::create);
    }
    private Item getItem(@NotNull CommandContext<ServerCommandSource> context, @NotNull String key) throws CommandSyntaxException {
        return ItemStackArgumentType.getItemStackArgument(context, key)
            .getItem();
    }
}
