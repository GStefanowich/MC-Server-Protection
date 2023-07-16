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

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
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
import net.theelm.sewingmachine.utilities.ServerText;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class MoneyCommand implements SewCommand {
    private static final ExceptionTranslatableServerSide NOT_ENOUGH_MONEY = ServerText.exception("player.money.poor");
    private static final ExceptionTranslatableServerSide PLAYER_NOT_FOUND = ServerText.exception("player.not_found");
    
    private static final ConfigOption<Integer> DEFAULT_STATE = SewBaseConfig.STARTING_MONEY;
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        /*
         * Player Pay
         */
        LiteralCommandNode<ServerCommandSource> pay = CommandUtils.register(dispatcher, "pay", builder -> builder
            .requires(CommandPredicate.isEnabled(SewBaseConfig.DO_MONEY))
            .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .executes(this::commandMoneyPay)
                )
            )
        );
        
        /*
         * Player Money Management
         */
        CommandUtils.register(dispatcher, "money", builder -> builder
            .requires(CommandPredicate.isEnabled(SewBaseConfig.DO_MONEY))
            // Admin GIVE money (Adds money)
            .then(CommandManager.literal("give")
                // If player is OP
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(this::commandAdminGive)
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
                        .executes(this::commandAdminTake)
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
                        .executes(this::commandAdminSet)
                    )
                )
            )
            // Admin RESET money ()
            .then(CommandManager.literal("reset")
                // If player is OP
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .suggests(CommandUtils::getAllPlayerNames)
                    .executes(this::commandAdminReset)
                )
            )
            // Player PAY money (Transfers money)
            .then(pay)
            
            // Player REQUEST money (Send player a request)
            .then(CommandManager.literal("request")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                        .suggests(CommandUtils::getAllPlayerNames)
                        .executes(this::commandMoneyRequest)
                    )
                )
            )
            // Player CHECKS money (Balance check)
            .executes(this::commandMoneyGet)
        );
    }
    
    /*
     * Admin commands
     */
    private int commandAdminGive(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerCommandSource op = context.getSource();
        
        // Get the amount to give
        int amount = IntegerArgumentType.getInteger( context, "amount" );
        
        try {
            // Give the player the money
            if ( MoneyUtils.givePlayerMoney(target.getId(), amount) ) {
                // Notify the command sender
                op.sendFeedback(
                    () -> Text.literal("Gave ").formatted(Formatting.YELLOW)
                        .append(Text.literal("$" + FormattingUtils.format(amount)).formatted(Formatting.GREEN))
                        .append(" to ")
                        .append(Text.literal(target.getName()).formatted(Formatting.DARK_PURPLE))
                        .append("."),
                    true
                );
                
                // Notify the player
                this.tellPlayersTransaction(op.getServer(),null, target, amount);
            }
        } catch (NbtNotFoundException e) {
            throw PLAYER_NOT_FOUND.create(op);
            
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int commandAdminTake(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
                op.sendFeedback(
                    () -> Text.literal("Took ").formatted(Formatting.YELLOW)
                        .append(Text.literal("$" + FormattingUtils.format(amount)).formatted(Formatting.RED))
                        .append(" from ")
                        .append(Text.literal( target.getName() ).formatted(Formatting.DARK_PURPLE))
                        .append("."),
                    true
                );
                
                // Notify the player
                this.tellPlayersTransaction(op.getServer(), null, target, -amount);
            }
        } catch (NbtNotFoundException e) {
            throw PLAYER_NOT_FOUND.create( op );
            
        } catch (NotEnoughMoneyException e) {
            e.printStackTrace();
            
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int commandAdminSet(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
                op.sendFeedback(
                    () -> Text.literal("Set money for ").formatted(Formatting.YELLOW)
                        .append(Text.literal(target.getName()).formatted(Formatting.DARK_PURPLE))
                        .append(" to ")
                        .append(Text.literal("$" + FormattingUtils.format(amount)).formatted(amount >= 0 ? Formatting.GREEN : Formatting.RED))
                        .append("."),
                    true
                );
                
                // Notify the player
                this.tellPlayersTransaction(op.getServer(), null, target, amount - balance);
            }
        } catch (NbtNotFoundException e) {
            throw PLAYER_NOT_FOUND.create(op);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int commandAdminReset(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerCommandSource op = context.getSource();
        
        try {
            float startingMoney = SewConfig.get(DEFAULT_STATE);
            
            MoneyUtils.setPlayerMoney(target.getId(), SewConfig.get(SewBaseConfig.STARTING_MONEY) );
            op.sendFeedback(
                () -> Text.literal( "Set money for " ).formatted(Formatting.YELLOW)
                    .append( Text.literal( target.getName() ).formatted(Formatting.DARK_PURPLE) )
                    .append( " to " )
                    .append( Text.literal( "$" + FormattingUtils.format(startingMoney)).formatted( startingMoney >= 0 ? Formatting.GREEN : Formatting.RED ) )
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
    private int commandMoneyPay(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource commandSource = context.getSource();
        
        // Get the reference of the player to pay
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the amount to pay
        int amount = IntegerArgumentType.getInteger( context, "amount" );
        
        // Get our player reference
        ServerPlayerEntity player = commandSource.getPlayerOrThrow();
        
        boolean took = false;
        boolean sent = false;
        try {
            if (took = MoneyUtils.takePlayerMoney(player, amount)) {
                // Give player money
                sent = MoneyUtils.givePlayerMoney(target.getId(), amount);
                
                // Alert players
                this.tellPlayersTransaction(commandSource.getServer(), player, target, amount);
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
        
        return this.commandMoneyGet(context);
    }
    
    private int commandMoneyRequest(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get the reference of the player to request money from
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile targetProfile = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get our player reference
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerPlayerEntity target = source.getServer()
            .getPlayerManager()
            .getPlayer(targetProfile.getId());
        
        // Get the amount to request
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (target == null) {
            // Player not online
            throw MoneyCommand.PLAYER_NOT_FOUND.create(player);
        } else {
            // Send the pay request
            player.sendMessage(Text.literal("Sent request to ").append(ColorUtils.format(target.getDisplayName(), Formatting.AQUA)).formatted(Formatting.YELLOW));
            target.sendMessage(
                Text.literal("").formatted(Formatting.YELLOW)
                    .append(ColorUtils.format(player.getDisplayName(), Formatting.AQUA))
                    .append(" is requesting ")
                    .append(Text.literal("$" + FormattingUtils.format(amount)).formatted(Formatting.AQUA))
                    .append(" from you. ")
                    .append(Text.literal("Click Here").formatted(Formatting.BLUE, Formatting.BOLD).styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pay " + amount + " " + player.getName().getString()))))
                    .append(" to pay.")
            );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int commandMoneyGet(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Get our player reference
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        try {
            
            int playerHas = MoneyUtils.getPlayerMoney(player.getUuid());
            player.sendMessage(ServerText.text(player, "player.money",
                playerHas
            ));
            
        } catch (NbtNotFoundException e) {
            e.printStackTrace();
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Money adaptation
     */
    private void tellPlayersTransaction(@NotNull MinecraftServer server, @Nullable ServerPlayerEntity payer, @NotNull GameProfile recipient, long amount) {
        if (amount == 0) return;
        
        // Get the recipient and notify them if they are online
        PlayerManager playerManager = server.getPlayerManager();
        ServerPlayerEntity recipientEntity = playerManager.getPlayer(recipient.getId());
        if ( recipientEntity != null ) {
            if (payer == null) {
                TitleUtils.showPlayerAlert(recipientEntity, ( amount > 0 ? Formatting.GREEN : Formatting.RED ),
                    Text.literal("You " + ( amount > 0 ? "received" : "lost" )),
                    Text.literal(" $" + FormattingUtils.format(Math.abs( amount ))).formatted(Formatting.AQUA, Formatting.BOLD)
                );
            } else {
                TitleUtils.showPlayerAlert(recipientEntity, Formatting.GREEN,
                    Text.literal("You received"),
                    Text.literal(" $" + FormattingUtils.format(Math.abs( amount ))).formatted(Formatting.AQUA, Formatting.BOLD),
                    Text.literal(" from "),
                    ColorUtils.format(payer.getName(), Formatting.DARK_PURPLE)
                );
            }
        }
    }
}
