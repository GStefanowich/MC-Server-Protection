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
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.utilities.DevUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Stream;

public final class AdminCommands {
    
    public static final @NotNull String FLIGHT = "Fly";
    public static final @NotNull String GOD = "God";
    public static final @NotNull String HEAL = "Heal";
    public static final @NotNull String FEED = "Feed";
    public static final @NotNull String REPAIR = "Repair";
    
    private static final ExceptionTranslatableServerSide PLAYERS_NOT_FOUND_EXCEPTION = TranslatableServerSide.exception("player.none_found");
    
    private AdminCommands() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the FLY command
        ServerCore.register(dispatcher, AdminCommands.FLIGHT, (builder) -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_FLY))
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_FLY.onOther()))
                .executes(AdminCommands::targetFlying)
            )
            .executes(AdminCommands::selfFlying)
        );
        
        // Register the GOD command
        ServerCore.register(dispatcher, AdminCommands.GOD, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_GODMODE))
            .then(CommandManager.argument( "target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_GODMODE.onOther()))
                .executes(AdminCommands::targetGod)
            )
            .executes(AdminCommands::selfGod)
        );
        
        // Register the HEAL command
        ServerCore.register(dispatcher, AdminCommands.HEAL, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_HEAL))
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_HEAL.onOther()))
                .executes(AdminCommands::targetHeal)
            )
            .executes(AdminCommands::selfHeal)
        );
        
        // Register the FEED command
        ServerCore.register(dispatcher, AdminCommands.FEED, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_FEED))
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_FEED.onOther()))
                .executes(AdminCommands::targetFeed)
            )
            .executes(AdminCommands::selfFeed)
        );
        
        // Register the HEAL command
        ServerCore.register(dispatcher, AdminCommands.REPAIR, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(Permissions.PLAYER_REPAIR))
            .executes(AdminCommands::selfRepair)
        );
        
        // Create DEBUG commands
        if (DevUtils.isDebugging()) {
            ServerCore.register(dispatcher, "Dragon Players", builder -> builder
                .then(CommandManager.argument("count", IntegerArgumentType.integer( 0 ))
                    .executes((context) -> {
                        SewConfig.set(SewConfig.DRAGON_PLAYERS, ConfigOption.convertToJSON(
                            IntegerArgumentType.getInteger( context, "count" )
                        ));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );
        }
    }
    
    private static int selfFlying(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        AdminCommands.toggleFlying(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int targetFlying(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0)
            throw PLAYERS_NOT_FOUND_EXCEPTION.create( source );
        return AdminCommands.toggleFlying(source, players.stream());
    }
    private static int toggleFlying(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                AdminCommands.toggleFlying(source, player);
            else TranslatableServerSide.send(source, "player.abilities.flying_other." + (AdminCommands.toggleFlying(null, player) ? "enabled" : "disabled"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private static boolean toggleFlying(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        PlayerAbilities abilities = player.getAbilities();
        
        // Toggle flying for the player
        abilities.allowFlying = !abilities.allowFlying;
        player.setNoGravity(false);
        
        // Tell the player
        if (source == null)
            TranslatableServerSide.send(player, "player.abilities.flying_self." + (abilities.allowFlying ? "enabled" : "disabled"));
        else TranslatableServerSide.send(source, "player.abilities.flying_self." + (abilities.allowFlying ? "enabled" : "disabled"));
        
        // If flying was turned off, stop the player mid-flight
        if (!abilities.allowFlying)
            abilities.flying = false;
        
        player.sendAbilitiesUpdate();
        return abilities.allowFlying;
    }
    
    private static int selfGod(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        AdminCommands.toggleGod(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int targetGod(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0 )
            throw PLAYERS_NOT_FOUND_EXCEPTION.create( source );
        return AdminCommands.toggleGod(source, players.stream());
    }
    private static int toggleGod(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                AdminCommands.toggleGod(source, player);
            else TranslatableServerSide.send(source, "player.abilities.godmode_other." + (AdminCommands.toggleGod(null, player) ? "enabled" : "disabled"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private static boolean toggleGod(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        PlayerAbilities abilities = player.getAbilities();
        
        // Toggle god mode for the player
        abilities.invulnerable = !abilities.invulnerable;
        player.sendAbilitiesUpdate();
        
        // Tell the player
        if (source == null)
            TranslatableServerSide.send(player, "player.abilities.godmode_self." + (abilities.invulnerable ? "enabled" : "disabled"));
        else TranslatableServerSide.send(source, "player.abilities.godmode_self." + (abilities.invulnerable ? "enabled" : "disabled"));
        
        return abilities.invulnerable;
    }
    
    private static int selfHeal(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        AdminCommands.healPlayer(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int targetHeal(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0)
            throw PLAYERS_NOT_FOUND_EXCEPTION.create(source);
        return AdminCommands.healPlayer(source, players.stream());
    }
    private static int healPlayer(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                AdminCommands.healPlayer(source, player);
            else TranslatableServerSide.send(source, (AdminCommands.healPlayer(null, player) ? "player.abilities.healed_other" : "player.abilities.healed_dead"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private static boolean healPlayer(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        boolean alive = player.isAlive();
        if (alive) {
            // Heal the player
            player.setHealth(player.getMaxHealth());
            
            // Tell the player
            if (source == null)
                TranslatableServerSide.send(player, "player.abilities.healed_self");
            else TranslatableServerSide.send(source, "player.abilities.healed_self");
        }
        return alive;
    }
    
    private static int selfFeed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        AdminCommands.feedPlayer(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int targetFeed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0)
            throw PLAYERS_NOT_FOUND_EXCEPTION.create(source);
        return AdminCommands.feedPlayer(source, players.stream());
    }
    private static int feedPlayer(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                AdminCommands.feedPlayer(source, player);
            else TranslatableServerSide.send(source, (AdminCommands.feedPlayer(null, player) ? "player.abilities.fed_other" : "player.abilities.fed_dead"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private static boolean feedPlayer(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        boolean alive;
        if (alive = player.isAlive()) {
            HungerManager hungerManager = player.getHungerManager();
            
            // Feed the player
            hungerManager.setFoodLevel(20);
            
            // Tell the player
            if (source == null)
                TranslatableServerSide.send(player, "player.abilities.fed_self");
            else TranslatableServerSide.send(source, "player.abilities.fed_self");
        }
        return alive;
    }
    
    private static int selfRepair(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        ItemStack stack = player.getMainHandStack();
        if (stack.isDamageable()) {
            stack.setDamage(0);
            
            return Command.SINGLE_SUCCESS;
        }
        return -1;
    }
    
}
