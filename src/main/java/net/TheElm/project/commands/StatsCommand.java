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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.TheElm.project.ServerCore;
import net.TheElm.project.enums.ChatRooms;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.PlayerChat;
import net.TheElm.project.utilities.CommandUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.EntitySummonArgumentType;
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
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on Mar 08 2021 at 7:51 PM.
 * By greg in SewingMachineMod
 */
public final class StatsCommand {
    private StatsCommand() {}
    
    private static final @NotNull SuggestionProvider<ServerCommandSource> KILLABLE_ENTITIES = (context, builder) -> CommandSource.suggestFromIdentifier(Registry.ENTITY_TYPE.stream().filter(t -> t.isSummonable() && !t.getSpawnGroup().equals(SpawnGroup.MISC)), builder, EntityType::getId, (entityType) -> {
        return new TranslatableText(Util.createTranslationKey("entity", EntityType.getId(entityType)));
    });
    private static final @NotNull SuggestionProvider<ServerCommandSource> DESTROYED_ITEMS = (context, builder) -> CommandSource.suggestFromIdentifier(Registry.ITEM.stream().filter(t -> t.isDamageable()), builder, Registry.ITEM::getId, (item) -> {
        return new TranslatableText(Util.createTranslationKey("entity", Registry.ITEM.getId(item)));
    });
    private static final @NotNull SuggestionProvider<ServerCommandSource> ITEMS = (context, builder) -> CommandSource.suggestFromIdentifier(Registry.ITEM.stream(), builder, Registry.ITEM::getId, (item) -> {
        return new TranslatableText(Util.createTranslationKey("entity", Registry.ITEM.getId(item)));
    });
    private static final @NotNull SuggestionProvider<ServerCommandSource> BLOCKS = (context, builder) -> CommandSource.suggestFromIdentifier(Registry.BLOCK.stream(), builder, Registry.BLOCK::getId, (block) -> {
        return new TranslatableText(Util.createTranslationKey("entity", Registry.BLOCK.getId(block)));
    });
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "Stat", builder -> builder
            .then(CommandManager.literal("killed")
                .then(CommandManager.argument("entity", EntitySummonArgumentType.entitySummon())
                    .suggests(StatsCommand.KILLABLE_ENTITIES)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(CommandUtils.requires(OpLevels.CHEATING))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .then(CommandManager.literal("add")
                                .executes(StatsCommand::incStatKilled)
                            )
                            .then(CommandManager.literal("subtract")
                                .executes(StatsCommand::decStatKilled)
                            )
                        )
                    )
                    .executes(StatsCommand::getStatKilled)
                )
            )
            .then(CommandManager.literal("mined")
                .then(CommandManager.argument("block", ItemStackArgumentType.itemStack())
                    .suggests(StatsCommand.BLOCKS)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(CommandUtils.requires(OpLevels.CHEATING))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .then(CommandManager.literal("add")
                                .executes(StatsCommand::incStatMined)
                            )
                            .then(CommandManager.literal("subtract")
                                .executes(StatsCommand::decStatMined)
                            )
                        )
                    )
                    .executes(StatsCommand::getStatMined)
                )
            )
            .then(CommandManager.literal("broken")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                    .suggests(StatsCommand.DESTROYED_ITEMS)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(CommandUtils.requires(OpLevels.CHEATING))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .then(CommandManager.literal("add")
                                .executes(StatsCommand::incStatBroken)
                            )
                            .then(CommandManager.literal("subtract")
                                .executes(StatsCommand::decStatBroken)
                            )
                        )
                    )
                    .executes(StatsCommand::getStatBroken)
                )
            )
            .then(CommandManager.literal("used")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                    .suggests(StatsCommand.ITEMS)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(CommandUtils.requires(OpLevels.CHEATING))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .then(CommandManager.literal("add")
                                .executes(StatsCommand::incStatUsed)
                            )
                            .then(CommandManager.literal("subtract")
                                .executes(StatsCommand::decStatUsed)
                            )
                        )
                    )
                    .executes(StatsCommand::getStatUsed)
                )
            )
            .then(CommandManager.literal("crafted")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                    .suggests(StatsCommand.ITEMS)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(CommandUtils.requires(OpLevels.CHEATING))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .then(CommandManager.literal("add")
                                .executes(StatsCommand::incStatCrafted)
                            )
                            .then(CommandManager.literal("subtract")
                                .executes(StatsCommand::decStatCrafted)
                            )
                        )
                    )
                    .executes(StatsCommand::getStatCrafted)
                )
            )
            .then(CommandManager.literal("pickedup")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                    .suggests(StatsCommand.ITEMS)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(CommandUtils.requires(OpLevels.CHEATING))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .then(CommandManager.literal("add")
                                .executes(StatsCommand::incStatPickedUp)
                            )
                            .then(CommandManager.literal("subtract")
                                .executes(StatsCommand::decStatPickedUp)
                            )
                        )
                    )
                    .executes(StatsCommand::getStatPickedUp)
                )
            )
            .then(CommandManager.literal("dropped")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                    .suggests(StatsCommand.ITEMS)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .requires(CommandUtils.requires(OpLevels.CHEATING))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .then(CommandManager.literal("add")
                                .executes(StatsCommand::incStatDropped)
                            )
                            .then(CommandManager.literal("subtract")
                                .executes(StatsCommand::decStatDropped)
                            )
                        )
                    )
                    .executes(StatsCommand::getStatDropped)
                )
            )
        );
    }
    
    private static int getStatKilled(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityType<?> entity = StatsCommand.getEntityType(context, "entity");
        int count = StatsCommand.getStatOf(context.getSource(), Stats.KILLED, entity);
        
        return StatsCommand.sendMessageAs(context.getSource(), new LiteralText("I have killed ")
            .append(new LiteralText("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(new TranslatableText(entity.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private static int incStatKilled(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.addToStatOf(
            player,
            Stats.KILLED,
            StatsCommand.getEntityType(context, "entity"),
            difference
        );
        
        return Command.SINGLE_SUCCESS;
    }
    private static int decStatKilled(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.removeFromStatOf(
            player,
            Stats.KILLED,
            StatsCommand.getEntityType(context, "entity"),
            difference
        );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getStatMined(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = StatsCommand.getItem(context, "block");
        if (!(item instanceof BlockItem))
            return 0;
        int count = StatsCommand.getStatOf(context.getSource(), Stats.MINED, ((BlockItem) item).getBlock());
        
        return StatsCommand.sendMessageAs(context.getSource(), new LiteralText("I have mined ")
            .append(new LiteralText("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(new TranslatableText(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private static int incStatMined(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        Item item = StatsCommand.getItem(context, "block");
        if (!(item instanceof BlockItem))
            difference = 0;
        else {
            StatsCommand.addToStatOf(
                player,
                Stats.MINED,
                ((BlockItem) item).getBlock(),
                difference
            );
        }
        
        context.getSource()
            .sendFeedback(new LiteralText("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int decStatMined(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        Item item = StatsCommand.getItem(context, "block");
        if (!(item instanceof BlockItem))
            difference = 0;
        else {
            StatsCommand.removeFromStatOf(
                player,
                Stats.MINED,
                ((BlockItem) item).getBlock(),
                difference
            );
        }
        
        context.getSource()
            .sendFeedback(new LiteralText("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getStatBroken(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = StatsCommand.getItem(context, "item");
        int count = StatsCommand.getStatOf(context.getSource(), Stats.BROKEN, item);
        
        return StatsCommand.sendMessageAs(context.getSource(), new LiteralText("I have broken ")
            .append(new LiteralText("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(new TranslatableText(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private static int incStatBroken(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.addToStatOf(
            player,
            Stats.BROKEN,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int decStatBroken(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.removeFromStatOf(
            player,
            Stats.BROKEN,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getStatUsed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = StatsCommand.getItem(context, "item");
        int count = StatsCommand.getStatOf(context.getSource(), Stats.USED, item);
        
        return StatsCommand.sendMessageAs(context.getSource(), new LiteralText("I have used ")
            .append(new LiteralText("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(new TranslatableText(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private static int incStatUsed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.addToStatOf(
            player,
            Stats.USED,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int decStatUsed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.removeFromStatOf(
            player,
            Stats.USED,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getStatCrafted(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = StatsCommand.getItem(context, "item");
        int count = StatsCommand.getStatOf(context.getSource(), Stats.CRAFTED, item);
        
        return StatsCommand.sendMessageAs(context.getSource(), new LiteralText("I have crafted ")
            .append(new LiteralText("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(new TranslatableText(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private static int incStatCrafted(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.addToStatOf(
            player,
            Stats.CRAFTED,
            StatsCommand.getItem(context, "item"),
            difference
        );

        context.getSource()
            .sendFeedback(new LiteralText("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int decStatCrafted(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        StatsCommand.removeFromStatOf(
            player,
            Stats.CRAFTED,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getStatPickedUp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = StatsCommand.getItem(context, "item");
        int count = StatsCommand.getStatOf(context.getSource(), Stats.PICKED_UP, item);
        
        return StatsCommand.sendMessageAs(context.getSource(), new LiteralText("I have picked up ")
            .append(new LiteralText("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(new TranslatableText(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private static int incStatPickedUp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        StatsCommand.addToStatOf(
            player,
            Stats.PICKED_UP,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int decStatPickedUp(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        StatsCommand.removeFromStatOf(
            player,
            Stats.PICKED_UP,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int getStatDropped(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Item item = StatsCommand.getItem(context, "item");
        int count = StatsCommand.getStatOf(context.getSource(), Stats.DROPPED, item);
        
        return StatsCommand.sendMessageAs(context.getSource(), new LiteralText("I have dropped ")
            .append(new LiteralText("[x")
                .append(MessageUtils.formatNumber(count))
                .append(" ")
                .append(new TranslatableText(item.getTranslationKey()))
                .append("]")
                .formatted(Formatting.AQUA))
            .append("."));
    }
    private static int incStatDropped(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int difference = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        StatsCommand.addToStatOf(
            player,
            Stats.DROPPED,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Added ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" to ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    private static int decStatDropped(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int difference = IntegerArgumentType.getInteger(context, "amount");
        StatsCommand.removeFromStatOf(
            player,
            Stats.DROPPED,
            StatsCommand.getItem(context, "item"),
            difference
        );
        
        context.getSource()
            .sendFeedback(new LiteralText("Removed ")
                .formatted(Formatting.YELLOW)
                .append(MessageUtils.formatNumber(difference))
                .append(" from ")
                .append(player.getDisplayName())
                .append("."), true);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static <T> int getStatOf(@NotNull ServerCommandSource source, @NotNull StatType<T> type, @NotNull T stat) throws CommandSyntaxException {
        return StatsCommand.getStatOf(source.getPlayer(), type, stat);
    }
    private static <T> int getStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull T stat) {
        return StatsCommand.getStatOf(player, type, type.getOrCreateStat(stat));
    }
    private static <T> int getStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull Stat<T> stat) {
        return player.getStatHandler()
            .getStat(stat);
    }
    private static <T> void addToStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull T stat, int count) {
        player.increaseStat(type.getOrCreateStat(stat), count);
    }
    private static <T> void removeFromStatOf(@NotNull ServerPlayerEntity player, @NotNull StatType<T> type, @NotNull T key, int count) {
        Stat<T> stat = type.getOrCreateStat(key);
        
        int updated = StatsCommand.getStatOf(player, type, stat)
            - count;
        
        player.resetStat(stat);
        if (updated > 0)
            player.increaseStat(stat, updated);
    }
    private static int sendMessageAs(@NotNull ServerCommandSource source, @Nullable Text text) throws CommandSyntaxException {
        if (text == null)
            return 0;
        ServerPlayerEntity player = source.getPlayer();
        
        // The chatroom to send the message in
        ChatRooms room = ((PlayerChat)player).getChatRoom();
        
        // Create a chat message
        Text chatText = MessageUtils.formatPlayerMessage(player, room, text);
        
        // Send the new chat message to the currently selected chat room
        MessageUtils.sendTo(room, player, chatText);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static EntityType<?> getEntityType(@NotNull CommandContext<ServerCommandSource> context, @NotNull String key) throws CommandSyntaxException {
        Identifier identifier = EntitySummonArgumentType.getEntitySummon(context, key);
        return Registry.ENTITY_TYPE.getOrEmpty(identifier)
            .orElseThrow(ServerCommandSource.REQUIRES_ENTITY_EXCEPTION::create);
    }
    private static Item getItem(@NotNull CommandContext<ServerCommandSource> context, @NotNull String key) throws CommandSyntaxException {
        return ItemStackArgumentType.getItemStackArgument(context, key)
            .getItem();
    }
}
