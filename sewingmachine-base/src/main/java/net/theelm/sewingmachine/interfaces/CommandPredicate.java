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

package net.theelm.sewingmachine.interfaces;

import net.minecraft.server.network.ServerPlayerEntity;
import net.theelm.sewingmachine.config.ConfigOption;
import net.theelm.sewingmachine.config.ConfigPredicate;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.events.CommandPermissionCallback;
import net.theelm.sewingmachine.permissions.PermissionNode;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Created on Aug 11 2021 at 8:56 PM.
 * By greg in SewingMachineMod
 */
@FunctionalInterface
public interface CommandPredicate extends Predicate<ServerCommandSource> {
    @NotNull CommandPredicate OP_LEVEL_1 = source -> source.hasPermissionLevel(OpLevels.SPAWN_PROTECTION);
    @NotNull CommandPredicate OP_LEVEL_2 = source -> source.hasPermissionLevel(OpLevels.CHEATING);
    @NotNull CommandPredicate OP_LEVEL_3 = source -> source.hasPermissionLevel(OpLevels.KICK_BAN_OP);
    @NotNull CommandPredicate OP_LEVEL_4 = source -> source.hasPermissionLevel(OpLevels.STOP);
    
    boolean testCommandSource(ServerCommandSource source);
    
    @Override
    default boolean test(ServerCommandSource source) {
        return this.testCommandSource(source);
    }
    
    default @NotNull CommandPredicate andEnabled(@NotNull ConfigOption<Boolean> configLevel) {
        return this.and(CommandPredicate.isEnabled(configLevel));
    }
    default @NotNull CommandPredicate and(CommandPredicate chain) {
        return (t) -> this.test(t) && chain.test(t);
    }
    default @NotNull CommandPredicate and(@NotNull ConfigOption<Integer> configLevel) {
        return this.and(CommandPredicate.opLevel(configLevel));
    }
    default @NotNull CommandPredicate and(final int level) {
        return this.and(CommandPredicate.opLevel(level));
    }
    default @NotNull CommandPredicate and(@NotNull final PermissionNode permission) {
        return this.and(CommandPredicate.node(permission));
    }
    default @NotNull CommandPredicate and(BooleanSupplier supplier) {
        return this.and(discard -> supplier.getAsBoolean());
    }
    default @NotNull CommandPredicate orEnabled(@NotNull ConfigOption<Boolean> configLevel) {
        return this.or(CommandPredicate.isEnabled(configLevel));
    }
    default @NotNull CommandPredicate or(CommandPredicate chain) {
        return (t) -> this.test(t) || chain.test(t);
    }
    default @NotNull CommandPredicate or(@NotNull ConfigOption<Integer> configLevel) {
        return this.or(CommandPredicate.opLevel(configLevel));
    }
    default @NotNull CommandPredicate or(final int level) {
        return this.or(CommandPredicate.opLevel(level));
    }
    default @NotNull CommandPredicate or(@NotNull final PermissionNode permission) {
        return this.or(CommandPredicate.node(permission));
    }
    default @NotNull CommandPredicate or(BooleanSupplier supplier) {
        return this.or(discard -> supplier.getAsBoolean());
    }
    
    static @NotNull CommandPredicate isEnabled(@NotNull ConfigOption<Boolean> config) {
        return new ConfigPredicate.BoolPredicate(config);
    }
    static @NotNull CommandPredicate opLevel(@NotNull ConfigOption<Integer> configLevel) {
        return source -> source.hasPermissionLevel(SewConfig.get(configLevel));
    }
    static @NotNull CommandPredicate opLevel(final int level) {
        switch (level) {
            case 1: return CommandPredicate.OP_LEVEL_1;
            case 2: return CommandPredicate.OP_LEVEL_2;
            case 3: return CommandPredicate.OP_LEVEL_3;
            default: return CommandPredicate.OP_LEVEL_4;
        }
    }
    static @NotNull CommandPredicate node(@NotNull final PermissionNode permission) {
        return source -> source.getEntity() instanceof ServerPlayerEntity player
            && CommandPermissionCallback.EVENT.invoker().hasPermission(player, permission);
    }
    static @NotNull CommandPredicate cast(CommandPredicate predicate) {
        return predicate;
    }
}
