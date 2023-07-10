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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.enums.PermissionNodes;
import net.theelm.sewingmachine.exceptions.ExceptionTranslatableServerSide;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.CommandUtils;
import net.theelm.sewingmachine.utilities.DevUtils;
import net.theelm.sewingmachine.utilities.ServerText;
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

public final class AdminCommands extends SewCommand {
    
    public static final @NotNull String FLIGHT = "Fly";
    public static final @NotNull String GOD = "God";
    public static final @NotNull String HEAL = "Heal";
    public static final @NotNull String FEED = "Feed";
    public static final @NotNull String REPAIR = "Repair";
    
    private static final ExceptionTranslatableServerSide PLAYERS_NOT_FOUND_EXCEPTION = ServerText.exception("player.none_found");
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        // Register the FLY command
        CommandUtils.register(dispatcher, AdminCommands.FLIGHT, (builder) -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_FLY))
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_FLY.onOther()))
                .executes(this::targetFlying)
            )
            .executes(this::selfFlying)
        );
        
        // Register the GOD command
        CommandUtils.register(dispatcher, AdminCommands.GOD, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GODMODE))
            .then(CommandManager.argument( "target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_GODMODE.onOther()))
                .executes(this::targetGod)
            )
            .executes(this::selfGod)
        );
        
        // Register the HEAL command
        CommandUtils.register(dispatcher, AdminCommands.HEAL, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_HEAL))
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_HEAL.onOther()))
                .executes(this::targetHeal)
            )
            .executes(this::selfHeal)
        );
        
        // Register the FEED command
        CommandUtils.register(dispatcher, AdminCommands.FEED, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_FEED))
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_FEED.onOther()))
                .executes(this::targetFeed)
            )
            .executes(this::selfFeed)
        );
        
        // Register the HEAL command
        CommandUtils.register(dispatcher, AdminCommands.REPAIR, builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING).or(PermissionNodes.PLAYER_REPAIR))
            .executes(this::selfRepair)
        );
        
        // Create DEBUG commands
        if (DevUtils.isDebugging()) {
            CommandUtils.register(dispatcher, "Dragon Players", builder -> builder
                .then(CommandManager.argument("count", IntegerArgumentType.integer( 0 ))
                    .executes((context) -> {
                        SewConfig.set(SewBaseConfig.DRAGON_PLAYERS, ConfigOption.convertToJSON(
                            IntegerArgumentType.getInteger( context, "count" )
                        ));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );
        }
    }
    
    private int selfFlying(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        this.toggleFlying(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private int targetFlying(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0)
            throw PLAYERS_NOT_FOUND_EXCEPTION.create(source);
        return this.toggleFlying(source, players.stream());
    }
    private int toggleFlying(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                this.toggleFlying(source, player);
            else ServerText.send(source, "player.abilities.flying_other." + (this.toggleFlying(null, player) ? "enabled" : "disabled"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private boolean toggleFlying(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        PlayerAbilities abilities = player.getAbilities();
        
        // Toggle flying for the player
        abilities.allowFlying = !abilities.allowFlying;
        player.setNoGravity(false);
        
        // Tell the player
        if (source == null)
            ServerText.send(player, "player.abilities.flying_self." + (abilities.allowFlying ? "enabled" : "disabled"));
        else ServerText.send(source, "player.abilities.flying_self." + (abilities.allowFlying ? "enabled" : "disabled"));
        
        // If flying was turned off, stop the player mid-flight
        if (!abilities.allowFlying)
            abilities.flying = false;
        
        player.sendAbilitiesUpdate();
        return abilities.allowFlying;
    }
    
    private int selfGod(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        this.toggleGod(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private int targetGod(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0 )
            throw PLAYERS_NOT_FOUND_EXCEPTION.create( source );
        return this.toggleGod(source, players.stream());
    }
    private int toggleGod(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                this.toggleGod(source, player);
            else ServerText.send(source, "player.abilities.godmode_other." + (this.toggleGod(null, player) ? "enabled" : "disabled"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private boolean toggleGod(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        PlayerAbilities abilities = player.getAbilities();
        
        // Toggle god mode for the player
        abilities.invulnerable = !abilities.invulnerable;
        player.sendAbilitiesUpdate();
        
        // Tell the player
        if (source == null)
            ServerText.send(player, "player.abilities.godmode_self." + (abilities.invulnerable ? "enabled" : "disabled"));
        else ServerText.send(source, "player.abilities.godmode_self." + (abilities.invulnerable ? "enabled" : "disabled"));
        
        return abilities.invulnerable;
    }
    
    private int selfHeal(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        this.healPlayer(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private int targetHeal(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0)
            throw PLAYERS_NOT_FOUND_EXCEPTION.create(source);
        return this.healPlayer(source, players.stream());
    }
    private int healPlayer(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                this.healPlayer(source, player);
            else ServerText.send(source, (this.healPlayer(null, player) ? "player.abilities.healed_other" : "player.abilities.healed_dead"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private boolean healPlayer(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        boolean alive = player.isAlive();
        if (alive) {
            // Heal the player
            player.setHealth(player.getMaxHealth());
            
            // Tell the player
            if (source == null)
                ServerText.send(player, "player.abilities.healed_self");
            else ServerText.send(source, "player.abilities.healed_self");
        }
        return alive;
    }
    
    private int selfFeed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        this.feedPlayer(source, source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private int targetFeed(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0)
            throw PLAYERS_NOT_FOUND_EXCEPTION.create(source);
        return this.feedPlayer(source, players.stream());
    }
    private int feedPlayer(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> {
            if (source.getEntity() == player)
                this.feedPlayer(source, player);
            else ServerText.send(source, (this.feedPlayer(null, player) ? "player.abilities.fed_other" : "player.abilities.fed_dead"), player.getDisplayName());
        });
        return Command.SINGLE_SUCCESS;
    }
    private boolean feedPlayer(@Nullable ServerCommandSource source, @NotNull ServerPlayerEntity player) {
        boolean alive;
        if (alive = player.isAlive()) {
            HungerManager hungerManager = player.getHungerManager();
            
            // Feed the player
            hungerManager.setFoodLevel(20);
            
            // Tell the player
            if (source == null)
                ServerText.send(player, "player.abilities.fed_self");
            else ServerText.send(source, "player.abilities.fed_self");
        }
        return alive;
    }
    
    private int selfRepair(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
