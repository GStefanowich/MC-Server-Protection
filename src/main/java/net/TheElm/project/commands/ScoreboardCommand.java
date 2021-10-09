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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.TheElm.project.CoreMod;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.interfaces.CommandPredicate;
import net.TheElm.project.utilities.FormattingUtils;
import net.TheElm.project.utilities.text.MessageUtils;
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
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on Jun 27 2021 at 2:48 PM.
 * By greg in SewingMachineMod
 */
public class ScoreboardCommand {
    public static final String COMMAND_NAME = "scoreboard";
    public static final String COMMAND_OBJECTIVES = "objectives";
    private static final EntityType<?> ENTITY_TYPE = EntityType.ARMOR_STAND;
    
    private ScoreboardCommand() {}
    
    public static void modify(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
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
                    .executes(ScoreboardCommand::generateNumStands)
                )
                .executes(ScoreboardCommand::generateStands)
            );
        objectives.addChild(armorStands.build());
    }
    
    private static int generateStands(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return ScoreboardCommand.generateStands(context, 3);
    }
    private static int generateNumStands(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(context, "count");
        try {
            return ScoreboardCommand.generateStands(context, count);
        } catch (Exception e) {
            CoreMod.logError(e);
            return 0;
        }
    }
    private static int generateStands(@NotNull CommandContext<ServerCommandSource> context, int places) throws CommandSyntaxException {
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
            NbtCompound rider = i == 0 ? ScoreboardCommand.generateTitle(objective) : ScoreboardCommand.generateRider(i, scores.size() < i ? null : scores.get(i - 1));
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
        return e != null && world.shouldCreateNewEntityWithPassenger(e) ? Command.SINGLE_SUCCESS : 0;
    }
    private static @NotNull NbtCompound generateTitle(@NotNull ScoreboardObjective objective) {
        return ScoreboardCommand.generateRider(FormattingUtils.deepCopy(objective.getDisplayName())
            .formatted(Formatting.AQUA));
    }
    private static @NotNull NbtCompound generateRider(int count, @Nullable ScoreboardPlayerScore score) {
        final MutableText right = new LiteralText(": ").formatted(Formatting.WHITE);
        if (score == null) {
            right.append(new LiteralText("Nobody")
                .formatted(Formatting.GRAY));
        } else {
            right.append(new LiteralText(score.getPlayerName())
                .formatted(Formatting.AQUA))
                .append(" (")
                .append(MessageUtils.formatNumber(score.getScore(), Formatting.GOLD))
                .append(")");
        }
        
        return ScoreboardCommand.generateRider(new LiteralText(count + ScoreboardCommand.ending(count)).formatted(Formatting.GOLD)
            .append(right));
    }
    private static @NotNull NbtCompound generateRider(@NotNull Text display) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("NoAI", true);
        tag.putBoolean("Silent", true);
        tag.putBoolean("NoGravity", true);
        tag.putBoolean("Invulnerable", true);
        tag.putBoolean("Invisible", true);
        tag.putString("id", ScoreboardCommand.entityId().toString());
        tag.putInt("DisabledSlots", 2039583);
        tag.putBoolean("Small", true);
        
        // Append the CustomName
        tag.putString("CustomName", Text.Serializer.toJson(display));
        tag.putBoolean("CustomNameVisible", true);
        
        return tag;
    }
    private static @NotNull Identifier entityId() {
        return Registry.ENTITY_TYPE.getId(ENTITY_TYPE);
    }
    private static @NotNull String ending(int pos) {
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
