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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.exceptions.NbtNotFoundException;
import net.theelm.sewingmachine.exceptions.NotEnoughMoneyException;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.ColorUtils;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.MoneyUtils;
import net.theelm.sewingmachine.utilities.TitleUtils;
import net.theelm.sewingmachine.utilities.TranslatableServerSide;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class MoneyCommand {
    
    private static final ExceptionTranslatableServerSide NOT_ENOUGH_MONEY = TranslatableServerSide.exception("player.money.poor");
    private static final ExceptionTranslatableServerSide PLAYER_NOT_FOUND = TranslatableServerSide.exception("player.not_found");
    
    private static final ConfigOption<Integer> DEFAULT_STATE = SewConfig.STARTING_MONEY;
    
    private MoneyCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        /*
         * Player Pay
         */
        LiteralCommandNode<ServerCommandSource> pay = ServerCore.register(dispatcher, "pay", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.DO_MONEY))
            .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests( CommandUtils::getAllPlayerNames )
                    .executes( MoneyCommand::commandMoneyPay )
                )
            )
        );
        
        /*
         * Player Money Management
         */
        ServerCore.register(dispatcher, "money", builder -> builder
            .requires(CommandPredicate.isEnabled(SewConfig.DO_MONEY))
            // Admin GIVE money (Adds money)
            .then(CommandManager.literal("give")
                // If player is OP
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(MoneyCommand::commandAdminGive)
                    )
                )
            )
            // Admin TAKES money (Removes money)
            .then(CommandManager.literal("take")
                // If player is OP
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(MoneyCommand::commandAdminTake)
                    )
                )
            )
            // Admin SET money (Sets amount)
            .then(CommandManager.literal("set")
                // If player is OP
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer())
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(MoneyCommand::commandAdminSet)
                    )
                )
            )
            // Admin RESET money ()
            .then(CommandManager.literal("reset")
                // If player is OP
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .executes(MoneyCommand::commandAdminReset)
                )
            )
            // Player PAY money (Transfers money)
            .then(pay)
            
            // Player REQUEST money (Send player a request)
            .then(CommandManager.literal("request")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(MoneyCommand::commandMoneyRequest)
                    )
                )
            )
            // Player CHECKS money (Balance check)
            .executes(MoneyCommand::commandMoneyGet)
        );
    }
    
    /*
     * Admin commands
     */
    private static int commandAdminGive(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerCommandSource op = context.getSource();
        
        // Get the amount to give
        int amount = IntegerArgumentType.getInteger( context, "amount" );
        
        try {
            // Give the player the money
            if ( MoneyUtils.givePlayerMoney( target.getId(), amount ) ) {
                // Notify the command sender
                op.sendFeedback(new LiteralText("Gave ").formatted(Formatting.YELLOW)
                    .append(new LiteralText("$" + FormattingUtils.format(amount)).formatted(Formatting.GREEN))
                    .append(" to ")
                    .append(new LiteralText(target.getName()).formatted(Formatting.DARK_PURPLE))
                    .append("."),
                    true
                );
                
                // Notify the player
                MoneyCommand.tellPlayersTransaction(op.getServer(),null, target, amount);
            }
        } catch (NbtNotFoundException e) {
            throw PLAYER_NOT_FOUND.create(op);
            
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandAdminTake(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerCommandSource op = context.getSource();
        
        // Get the amount to take
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        try {
            // Take the players money
            if ( MoneyUtils.takePlayerMoney( target.getId(), amount ) ) {
                // Notify the command sender
                op.sendFeedback(new LiteralText("Took ").formatted(Formatting.YELLOW)
                    .append(new LiteralText("$" + FormattingUtils.format(amount)).formatted(Formatting.RED))
                    .append(" from ")
                    .append(new LiteralText( target.getName() ).formatted(Formatting.DARK_PURPLE))
                    .append("."),
                    true
                );
                
                // Notify the player
                MoneyCommand.tellPlayersTransaction(op.getServer(), null, target, -amount);
            }
        } catch (NbtNotFoundException e) {
            throw PLAYER_NOT_FOUND.create( op );
            
        } catch (NotEnoughMoneyException e) {
            e.printStackTrace();
            
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandAdminSet(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerCommandSource op = context.getSource();
        
        try {
            // Get the amount to set
            int balance = MoneyUtils.getPlayerMoney(target.getId());
            int amount = IntegerArgumentType.getInteger(context, "amount");
            
            // Set the players money
            if ( MoneyUtils.setPlayerMoney(target.getId(), amount) ) {
                // Notify the command sender
                op.sendFeedback(new LiteralText("Set money for ").formatted(Formatting.YELLOW)
                    .append(new LiteralText(target.getName()).formatted(Formatting.DARK_PURPLE))
                    .append(" to ")
                    .append(new LiteralText("$" + FormattingUtils.format(amount)).formatted(amount >= 0 ? Formatting.GREEN : Formatting.RED))
                    .append("."),
                    true
                );
                
                // Notify the player
                MoneyCommand.tellPlayersTransaction(op.getServer(), null, target, amount - balance);
            }
        } catch (NbtNotFoundException e) {
            throw PLAYER_NOT_FOUND.create(op);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandAdminReset(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerCommandSource op = context.getSource();
        
        try {
            float startingMoney = SewConfig.get(DEFAULT_STATE);
            
            MoneyUtils.setPlayerMoney( target.getId(), SewConfig.get(SewConfig.STARTING_MONEY) );
            op.sendFeedback( new LiteralText( "Set money for " ).formatted(Formatting.YELLOW)
                .append( new LiteralText( target.getName() ).formatted(Formatting.DARK_PURPLE) )
                .append( " to " )
                .append( new LiteralText( "$" + FormattingUtils.format(startingMoney)).formatted( startingMoney >= 0 ? Formatting.GREEN : Formatting.RED ) )
                .append( "." ),
                true
            );
        } catch (NbtNotFoundException e) {
            throw PLAYER_NOT_FOUND.create(op);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Player commands
     */
    private static int commandMoneyPay(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource commandSource = context.getSource();
        
        // Get the reference of the player to pay
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the amount to pay
        int amount = IntegerArgumentType.getInteger( context, "amount" );
        
        // Get our player reference
        ServerPlayerEntity player = commandSource.getPlayer();
        
        boolean took = false;
        boolean sent = false;
        try {
            if (took = MoneyUtils.takePlayerMoney(player, amount)) {
                // Give player money
                sent = MoneyUtils.givePlayerMoney(target.getId(), amount);
                
                // Alert players
                MoneyCommand.tellPlayersTransaction(commandSource.getServer(), player, target, amount);
            }
        } catch ( NbtNotFoundException e ) {
            throw PLAYER_NOT_FOUND.create(player);
            
        } catch ( NotEnoughMoneyException e ) {
            throw NOT_ENOUGH_MONEY.create(player);
            
        } finally {
            // Refund
            if (took && (!sent))
                MoneyUtils.givePlayerMoney(player, amount);
        }
        
        return MoneyCommand.commandMoneyGet(context);
    }
    
    private static int commandMoneyRequest(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the reference of the player to request money from
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile targetProfile = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get our player reference
        ServerPlayerEntity player = source.getPlayer();
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer( targetProfile.getId() );
        
        // Get the amount to request
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (target == null) {
            // Player not online
            throw MoneyCommand.PLAYER_NOT_FOUND.create(player);
        } else {
            // Send the pay request
            player.sendSystemMessage(new LiteralText("Sent request to ").append(ColorUtils.format(target.getDisplayName(), Formatting.AQUA)).formatted(Formatting.YELLOW), Util.NIL_UUID);
            target.sendMessage(
                new LiteralText("").formatted(Formatting.YELLOW)
                    .append(ColorUtils.format(player.getDisplayName(), Formatting.AQUA))
                    .append(" is requesting ")
                    .append(new LiteralText("$" + FormattingUtils.format(amount)).formatted(Formatting.AQUA))
                    .append(" from you. ")
                    .append(new LiteralText("Click Here").formatted(Formatting.BLUE, Formatting.BOLD).styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pay " + amount + " " + player.getName().getString()))))
                    .append(" to pay."),
                MessageType.CHAT,
                player.getUuid()
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandMoneyGet(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource commandSource = context.getSource();
        
        // Get our player reference
        ServerPlayerEntity player = commandSource.getPlayer();
        
        try {
            
            int playerHas = MoneyUtils.getPlayerMoney( player.getUuid() );
            player.sendSystemMessage(TranslatableServerSide.text( player, "player.money",
                playerHas
            ), Util.NIL_UUID);
            
        } catch (NbtNotFoundException e) {
            e.printStackTrace();
        }

        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Money adaptation
     */
    public static void tellPlayersTransaction(@NotNull MinecraftServer server, @Nullable ServerPlayerEntity payer, @NotNull GameProfile recipient, long amount ) {
        if (amount == 0) return;
        
        // Get the recipient and notify them if they are online
        PlayerManager playerManager = server.getPlayerManager();
        ServerPlayerEntity recipientEntity = playerManager.getPlayer(recipient.getId());
        if ( recipientEntity != null ) {
            if (payer == null) {
                TitleUtils.showPlayerAlert(recipientEntity, ( amount > 0 ? Formatting.GREEN : Formatting.RED ),
                    new LiteralText("You " + ( amount > 0 ? "received" : "lost" )),
                    new LiteralText(" $" + FormattingUtils.format(Math.abs( amount ))).formatted(Formatting.AQUA, Formatting.BOLD)
                );
            } else {
                TitleUtils.showPlayerAlert(recipientEntity, Formatting.GREEN,
                    new LiteralText("You received"),
                    new LiteralText(" $" + FormattingUtils.format(Math.abs( amount ))).formatted(Formatting.AQUA, Formatting.BOLD),
                    new LiteralText(" from "),
                    ColorUtils.format(payer.getName(), Formatting.DARK_PURPLE)
                );
            }
        }
    }
}
