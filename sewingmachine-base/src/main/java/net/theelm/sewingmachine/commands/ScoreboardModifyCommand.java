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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.theelm.sewingmachine.base.CoreMod;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.FormattingUtils;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on Jun 27 2021 at 2:48 PM.
 * By greg in SewingMachineMod
 */
public class ScoreboardModifyCommand extends SewCommand {
    public static final String COMMAND_NAME = "scoreboard";
    public static final String COMMAND_OBJECTIVES = "objectives";
    private static final EntityType<?> ENTITY_TYPE = EntityType.ARMOR_STAND;
    
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandNode<ServerCommandSource> root = dispatcher.getRoot().getChild(COMMAND_NAME);
        CommandNode<ServerCommandSource> objectives = null;
        
        // Get the "scoreboard" base command
        if (root == null) {
            root = dispatcher.register(CommandManager.literal(COMMAND_NAME));
        } else objectives = root.getChild(COMMAND_OBJECTIVES);
        
        // Add the "objectives" subcommand if it does not exist
        if (objectives == null) {
            objectives = CommandManager.literal(COMMAND_OBJECTIVES).build();
            root.addChild(objectives);
        }
        
        LiteralArgumentBuilder<ServerCommandSource> armorStands = CommandManager.literal("newdisplay")
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("objective", ScoreboardObjectiveArgumentType.scoreboardObjective())
                .then(CommandManager.argument("count", IntegerArgumentType.integer(0, 100))
                    .executes(this::generateNumStands)
                )
                .executes(this::generateStands)
            );
        objectives.addChild(armorStands.build());
    }
    
    private int generateStands(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return this.generateStands(context, 3);
    }
    private int generateNumStands(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(context, "count");
        try {
            return this.generateStands(context, count);
        } catch (Exception e) {
            CoreMod.logError(e);
            return 0;
        }
    }
    private int generateStands(@NotNull CommandContext<ServerCommandSource> context, int places) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        Vec3d position = source.getPosition();
        
        ScoreboardObjective objective = ScoreboardObjectiveArgumentType.getObjective(context, "objective");
        ServerScoreboard scoreboard = source.getServer()
            .getScoreboard();
        
        List<ScoreboardPlayerScore> scores = new ArrayList<>(scoreboard.getAllPlayerScores(objective));
        Collections.reverse(scores);
        
        NbtCompound bottom = null, previous = null;
        for (int i = places; i >= 0; i--) {
            NbtCompound rider = i == 0 ? this.generateTitle(objective) : this.generateRider(i, scores.size() < i ? null : scores.get(i - 1));
            NbtList passengers = new NbtList();
            passengers.add(rider);
            
            if (i == places)
                bottom = previous = rider;
            
            previous.put("Passengers", passengers);
            previous = rider;
        }
        
        // If none was created, exit safely
        if (bottom == null)
            return 0;
        
        // Load the data from the generated tag
        Entity e = EntityType.loadEntityWithPassengers(bottom, world, (entity) -> {
            entity.refreshPositionAndAngles(position.x, position.y, position.z, entity.getYaw(), entity.getPitch());
            return entity;
        });
        
        // Create the entity and its passengers
        return e != null && world.spawnNewEntityAndPassengers(e) ? Command.SINGLE_SUCCESS : 0;
    }
    private @NotNull NbtCompound generateTitle(@NotNull ScoreboardObjective objective) {
        return this.generateRider(FormattingUtils.deepCopy(objective.getDisplayName())
            .formatted(Formatting.AQUA));
    }
    private @NotNull NbtCompound generateRider(int count, @Nullable ScoreboardPlayerScore score) {
        final MutableText right = Text.literal(": ").formatted(Formatting.WHITE);
        if (score == null) {
            right.append(Text.literal("Nobody")
                .formatted(Formatting.GRAY));
        } else {
            right.append(Text.literal(score.getPlayerName())
                .formatted(Formatting.AQUA))
                .append(" (")
                .append(MessageUtils.formatNumber(score.getScore(), Formatting.GOLD))
                .append(")");
        }
        
        return this.generateRider(Text.literal(count + this.ending(count)).formatted(Formatting.GOLD)
            .append(right));
    }
    private @NotNull NbtCompound generateRider(@NotNull Text display) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("NoAI", true);
        tag.putBoolean("Silent", true);
        tag.putBoolean("NoGravity", true);
        tag.putBoolean("Invulnerable", true);
        tag.putBoolean("Invisible", true);
        tag.putString("id", this.entityId().toString());
        tag.putInt("DisabledSlots", 2039583);
        tag.putBoolean("Small", true);
        
        // Append the CustomName
        tag.putString("CustomName", Text.Serializer.toJson(display));
        tag.putBoolean("CustomNameVisible", true);
        
        return tag;
    }
    private @NotNull Identifier entityId() {
        return Registries.ENTITY_TYPE.getId(ScoreboardModifyCommand.ENTITY_TYPE);
    }
    private @NotNull String ending(int pos) {
        final String th = "th";
        String val = String.valueOf(pos);
        int length = val.length();
        if (length < 2 || val.charAt(length - 2) != '1') {
            switch (val.charAt(length - 1)) {
                case '1': return "st";
                case '2': return "nd";
                case '3': return "rd";
                default: break;
            }
        }
        return th;
    }
}
