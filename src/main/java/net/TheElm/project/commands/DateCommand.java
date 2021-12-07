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
import com.mojang.brigadier.context.CommandContext;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.utilities.CasingUtils;
import net.TheElm.project.utilities.IntUtils;
import net.TheElm.project.utilities.text.MessageUtils;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

/**
 * Created on Mar 11 2021 at 7:34 PM.
 * By greg in SewingMachineMod
 */
public final class DateCommand {
    private DateCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        ServerCore.register(dispatcher, "Date", builder -> builder
            .executes(DateCommand::displayDate)
        );
    }
    
    /**
     * Display to the player the current day and year of the world
     * @param context Command context
     * @return Success
     */
    private static int displayDate(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        long worldDay = IntUtils.timeToDays(source.getWorld());
        long worldYear = worldDay / SewConfig.get(SewConfig.CALENDAR_DAYS);
        worldDay = worldDay - (worldYear * SewConfig.get(SewConfig.CALENDAR_DAYS));
        
        String year = CasingUtils.acronym(SewConfig.get(SewConfig.CALENDAR_YEAR_EPOCH), true);
        MutableText yearText = MessageUtils.formatNumber(worldYear);
        if (!year.isEmpty()) {
            yearText.append(" " + year);
            yearText.styled(MessageUtils.simpleHoverText(SewConfig.get(SewConfig.CALENDAR_YEAR_EPOCH)));
        }
        
        source.sendFeedback(new LiteralText("It is currently ")
            .formatted(Formatting.YELLOW)
            .append(MessageUtils.formatNumber("Day ", worldDay))
            .append(" of ")
            .append(yearText), false);
        return Command.SINGLE_SUCCESS;
    }
}
