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

package net.theelm.sewingmachine.chat.interfaces;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.chat.enums.ChatRooms;
import net.theelm.sewingmachine.utilities.CasingUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Dec 20 2021 at 11:26 PM.
 * By greg in SewingMachineMod
 */
@FunctionalInterface
public interface EntityChatFunction extends ChatFunction {
    Text parseVar(@NotNull ServerCommandSource source, @NotNull LivingEntity entity, @NotNull CasingUtils.Casing casing);
    @Override
    default Text parseVar(@NotNull ServerCommandSource source, @NotNull ChatRooms chatRoom, @NotNull Text chatMessage, @NotNull CasingUtils.Casing casing) {
        return this.parseVar(source, (LivingEntity) source.getEntity(), casing);
    }
    @Override
    default boolean canBeParsed(ServerCommandSource source) {
        return source.getEntity() instanceof LivingEntity;
    }
}
