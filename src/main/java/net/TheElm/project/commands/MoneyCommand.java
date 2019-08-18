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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.TheElm.project.CoreMod;
import net.TheElm.project.MySQL.MySQLStatement;
import net.TheElm.project.config.SewingMachineConfig;
import net.TheElm.project.utilities.CommandUtilities;
import net.TheElm.project.utilities.TitleUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.UUID;

public final class MoneyCommand {
    
    private static final long DEFAULT_STATE = SewingMachineConfig.INSTANCE.STARTING_MONEY.get();
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (!SewingMachineConfig.INSTANCE.DO_MONEY.get())
            return;
        
        /*
         * Player Pay
         */
        LiteralCommandNode<ServerCommandSource> pay = dispatcher.register( CommandManager.literal( "pay" )
            .then( CommandManager.argument( "amount", IntegerArgumentType.integer( 0 ) )
                .then( CommandManager.argument( "player", GameProfileArgumentType.gameProfile() )
                    .suggests( CommandUtilities::getAllPlayerNames )
                    .executes( MoneyCommand::commandMoneyPay )
                )
            )
        );
        CoreMod.logDebug( "- Registered Pay command" );
        
        /*
         * Player Money Management
         */
        dispatcher.register( CommandManager.literal("money" )
            // Admin GIVE money (Adds money)
            .then( CommandManager.literal("give" )
                // If player is OP
                .requires((resource) -> resource.hasPermissionLevel( 1 ))
                .then( CommandManager.argument( "amount", IntegerArgumentType.integer( 0 ) )
                    .then( CommandManager.argument( "player", GameProfileArgumentType.gameProfile() )
                        .suggests( CommandUtilities::getAllPlayerNames )
                        .executes( MoneyCommand::commandAdminGive )
                    )
                )
            )
            // Admin TAKES money (Removes money)
            .then( CommandManager.literal( "take" )
                // If player is OP
                .requires((resource) -> resource.hasPermissionLevel( 1 ))
                .then( CommandManager.argument( "amount", IntegerArgumentType.integer( 0 ) )
                    .then( CommandManager.argument( "player", GameProfileArgumentType.gameProfile() )
                        .suggests( CommandUtilities::getAllPlayerNames )
                        .executes( MoneyCommand::commandAdminTake )
                    )
                )
            )
            // Admin SET money (Sets amount)
            .then( CommandManager.literal("set" )
                // If player is OP
                .requires((resource) -> resource.hasPermissionLevel( 1 ))
                .then( CommandManager.argument( "amount", IntegerArgumentType.integer() )
                    .then( CommandManager.argument( "player", GameProfileArgumentType.gameProfile() )
                        .suggests( CommandUtilities::getAllPlayerNames )
                        .executes( MoneyCommand::commandAdminSet )
                    )
                )
            )
            // Admin RESET money ()
            .then( CommandManager.literal( "reset" )
                // If player is OP
                .requires((resource) -> resource.hasPermissionLevel( 1 ))
                .then( CommandManager.argument( "player", GameProfileArgumentType.gameProfile() )
                    .suggests( CommandUtilities::getAllPlayerNames )
                    .executes( MoneyCommand::commandAdminReset )
                )
            )
            // Player PAY money (Transfers money)
            .then( CommandManager.literal("pay" )
                .redirect( pay )
            )
            // Player REQUEST money (Send player a request)
            .then( CommandManager.literal( "request" )
                .then( CommandManager.argument( "amount", IntegerArgumentType.integer( 0 ) )
                    .then( CommandManager.argument( "player", GameProfileArgumentType.gameProfile() )
                        .suggests( CommandUtilities::getAllPlayerNames )
                        .executes( MoneyCommand::commandMoneyRequest )
                    )
                )
            )
            // Player CHECKS money (Balance check)
            .executes(MoneyCommand::commandMoneyGet)
        );
        CoreMod.logDebug( "- Registered Money command" );
    }
    
    /*
     * Admin commands
     */
    private static int commandAdminGive(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerPlayerEntity op = context.getSource().getPlayer();
        
        // Get the amount to give
        long amount = IntegerArgumentType.getInteger( context, "amount" );
        
        try {
            MoneyCommand.databaseGivePlayerMoney( target.getId(), amount );
            op.sendMessage( new LiteralText( "Gave " ).formatted(Formatting.YELLOW)
                .append( new LiteralText( "$" + NumberFormat.getInstance().format( amount ) ).formatted(Formatting.GREEN) )
                .append( " to " )
                .append( new LiteralText( target.getName() ).formatted(Formatting.DARK_PURPLE) )
                .append( "." )
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandAdminTake(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerPlayerEntity op = context.getSource().getPlayer();
        
        // Get the amount to take
        long amount = IntegerArgumentType.getInteger( context, "amount" );
        
        try {
            if ( MoneyCommand.databaseTakePlayerMoney( target.getId(), amount ) ) {
                op.sendMessage( new LiteralText( "Took " ).formatted(Formatting.YELLOW)
                    .append( new LiteralText( "$" + NumberFormat.getInstance().format( amount ) ).formatted(Formatting.RED) )
                    .append( " from " )
                    .append( new LiteralText( target.getName() ).formatted(Formatting.DARK_PURPLE) )
                    .append( "." )
                );
            } else {
                
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandAdminSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerPlayerEntity op = context.getSource().getPlayer();
        
        // Get the amount to set
        long amount = IntegerArgumentType.getInteger( context, "amount" );
        
        try {
            MoneyCommand.databaseGivePlayerMoney( target.getId(), amount, true );
            op.sendMessage( new LiteralText( "Set money for " ).formatted(Formatting.YELLOW)
                .append( new LiteralText( target.getName() ).formatted(Formatting.DARK_PURPLE) )
                .append( " to " )
                .append( new LiteralText( "$" + NumberFormat.getInstance().format( amount ) ).formatted( amount >= 0 ? Formatting.GREEN : Formatting.RED ) )
                .append( "." )
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandAdminReset(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // Get the reference of the player to modify
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the running player
        ServerPlayerEntity op = context.getSource().getPlayer();
        
        try {
            MoneyCommand.databaseGivePlayerMoney( target.getId(), 0, true );
            op.sendMessage( new LiteralText( "Set money for " ).formatted(Formatting.YELLOW)
                .append( new LiteralText( target.getName() ).formatted(Formatting.DARK_PURPLE) )
                .append( " to " )
                .append( new LiteralText( "$" + NumberFormat.getInstance().format( DEFAULT_STATE ) ).formatted( DEFAULT_STATE >= 0 ? Formatting.GREEN : Formatting.RED ) )
                .append( "." )
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Player commands
     */
    
    private static int commandMoneyPay(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource commandSource = context.getSource();
        
        // Get the reference of the player to pay
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get the amount to pay
        int amount = IntegerArgumentType.getInteger( context, "amount" );
        
        // Get our player reference
        ServerPlayerEntity player = commandSource.getPlayer();
        
        try {
            if (MoneyCommand.databaseTakePlayerMoney( player.getUuid(), amount )) {
                // Give player money
                MoneyCommand.databaseGivePlayerMoney( target.getId(), amount );
                
                // Alert players
                MoneyCommand.tellPlayersTransaction( player, target, amount );
            } else {
                player.sendMessage(new LiteralText("You do not have enough money.").formatted(Formatting.RED));
            }
        } catch ( SQLException e ) {
            CoreMod.logError( e );
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandMoneyRequest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource commandSource = context.getSource();
        
        // Get the reference of the player to request money from
        Collection<GameProfile> argumentType = GameProfileArgumentType.getProfileArgument( context, "player" );
        GameProfile target = argumentType.stream().findAny().orElseThrow(GameProfileArgumentType.UNKNOWN_PLAYER_EXCEPTION::create);
        
        // Get our player reference
        ServerPlayerEntity player = commandSource.getPlayer();
        
        // Get the amount to request
        int amount = IntegerArgumentType.getInteger( context, "amount" );
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int commandMoneyGet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource commandSource = context.getSource();
        
        // Get our player reference
        ServerPlayerEntity player = commandSource.getPlayer();
        
        try {
            
            long playerHas = MoneyCommand.checkPlayerMoney( player.getUuid() );
            player.sendMessage(TranslatableServerSide.text( player, "player.money",
                playerHas
            ));
            
        } catch (SQLException e) {
            CoreMod.logError( e );
            
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /*
     * Money adaptation
     */
    public static long checkPlayerMoney(UUID uuid) throws SQLException {
        try (MySQLStatement stmt = CoreMod.getSQL().prepare("SELECT `dataMoney` FROM `player_Data` WHERE `dataOwner` = ?;", false)) {
            try ( ResultSet rs = stmt.addPrepared(uuid)
                .executeStatement(false) ) {
                
                while (rs.next()) {
                    return rs.getLong("dataMoney");
                }
            }
        }
        
        return 0;
    }
    public static void tellPlayersTransaction( ServerPlayerEntity payer, GameProfile recipient, long amount ) {
        MinecraftServer server = payer.getServer();
        if ( server == null )
            return;
        
        // Get the recipient and notify them if they are online
        PlayerManager playerManager = server.getPlayerManager();
        ServerPlayerEntity recipientEntity = playerManager.getPlayer( recipient.getId() );
        if ( recipientEntity != null ) {
            TitleUtils.showPlayerAlert( recipientEntity, Formatting.YELLOW,
                new LiteralText( "You received " ),
                new LiteralText( "$" + NumberFormat.getInstance().format( amount ) ).formatted(Formatting.BLUE, Formatting.BOLD),
                new LiteralText( " from " ),
                payer.getName().formatted(Formatting.DARK_PURPLE)
            );
        }
    }
    
    /*
     * Methods to update the database
     */
    public static void databaseGivePlayerMoney(UUID playerUUID, Number giveAmount) throws SQLException {
        MoneyCommand.databaseGivePlayerMoney(playerUUID, giveAmount, false);
    }
    public static void databaseGivePlayerMoney(UUID playerUUID, Number giveAmount, boolean set) throws SQLException {
        if ((giveAmount.longValue() <= 0) && (!set))
            return;
        CoreMod.getSQL().prepare("INSERT INTO `player_Data` ( `dataOwner`, `dataMoney` ) VALUES ( ?, ? ) ON DUPLICATE KEY UPDATE `dataMoney` =" + (set ? "" : " `dataMoney` +") + " VALUES( `dataMoney` );", false)
            .addPrepared(playerUUID)
            .addPrepared(giveAmount)
            .executeUpdate(true);
    }
    public static boolean databaseTakePlayerMoney(UUID playerUUID, Number takeAmount) throws SQLException {
        if ( takeAmount.longValue() <= 0 )
            return true;
        try ( MySQLStatement stmt = CoreMod.getSQL().prepare("UPDATE `player_Data` SET `dataMoney` = `dataMoney` - ? WHERE `dataOwner` = ?;", false) ) {
            int result = stmt
                .addPrepared(takeAmount)
                .addPrepared(playerUUID)
                .executeUpdate(true);
            return ( result > 0 );
        } catch ( SQLException e ){
            if ( e.getErrorCode() == 1690 )
                return false;
            throw e;
        }
    }
}
