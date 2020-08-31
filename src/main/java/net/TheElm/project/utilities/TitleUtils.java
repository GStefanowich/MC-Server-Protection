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

package net.TheElm.project.utilities;

import net.TheElm.project.CoreMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class TitleUtils {
    
    public static void showPlayerAlert(final PlayerEntity player, final Formatting formatting, final Text... text) {
        TitleUtils.showPlayerAlert( (ServerPlayerEntity) player, formatting, text );
    }
    public static void showPlayerAlert(final ServerPlayerEntity player, final Formatting formatting, final Text... text) {
        player.networkHandler.sendPacket(new TitleS2CPacket(
            Action.ACTIONBAR,
            combineTextChunks(formatting, text),
            1,
            20,
            1
        ));
    }
    
    public static void showPlayerAlert(@NotNull final ServerWorld world, Text... text) {
        Text builtText = combineTextChunks( Formatting.YELLOW, text );
        
        CoreMod.logInfo( builtText );
        
        for ( ServerPlayerEntity player : world.getPlayers() ) {
            player.networkHandler.sendPacket(new TitleS2CPacket(Action.CLEAR, null));
            player.networkHandler.sendPacket(new TitleS2CPacket(
                Action.ACTIONBAR,
                builtText,
                1,
                5,
                1
            ));
        }
    }
    public static void showPlayerAlert(@NotNull final MinecraftServer server, Text... text) {
        PlayerManager playerManager = server.getPlayerManager();
        
        Text builtText = combineTextChunks( Formatting.YELLOW, text );
        
        CoreMod.logInfo( builtText );
        
        playerManager.sendToAll(new TitleS2CPacket(Action.CLEAR, null));
        playerManager.sendToAll(new TitleS2CPacket(
            Action.ACTIONBAR,
            builtText,
            1,
            5,
            1
        ));
    }
    
    private static Text combineTextChunks(Formatting formatting, Text... text) {
        MutableText literalText = new LiteralText("")
            .formatted(formatting);
        for ( Text t : text ) {
            literalText.append( t );
        }
        return literalText;
    }
    
}
