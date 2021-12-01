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
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class TitleUtils {
    
    public static void showPlayerAlert(@NotNull final PlayerEntity player, final Formatting formatting, final Text... text) {
        TitleUtils.showPlayerAlert((ServerPlayerEntity) player, formatting, text );
    }
    public static void showPlayerAlert(@NotNull final ServerPlayerEntity player, final Formatting formatting, final Text... text) {
        player.sendMessage(TitleUtils.combineTextChunks(formatting, text), true);
    }
    
    public static void showPlayerAlert(@NotNull final ServerWorld world, Text... text) {
        Text builtText = TitleUtils.combineTextChunks( Formatting.YELLOW, text );
        
        CoreMod.logInfo( builtText );
        
        for (ServerPlayerEntity player : world.getPlayers())
            player.sendMessage(builtText, true);
    }
    public static void showPlayerAlert(@NotNull final MinecraftServer server, Text... text) {
        PlayerManager playerManager = server.getPlayerManager();
        
        Text builtText = TitleUtils.combineTextChunks(Formatting.YELLOW, text);
        CoreMod.logInfo(builtText);
        
        for (ServerPlayerEntity player : playerManager.getPlayerList())
            player.sendMessage(builtText, true);
    }
    
    public static void showPlayerTitle(@NotNull ServerPlayerEntity player, @NotNull String base, @NotNull String sub, Formatting... formatting) {
        ServerPlayNetworkHandler networkHandler = player.networkHandler;
        networkHandler.sendPacket(new ClearTitleS2CPacket(false));
        networkHandler.sendPacket(new TitleFadeS2CPacket(10, 40, 20));
        networkHandler.sendPacket(new SubtitleS2CPacket(new LiteralText(sub).formatted(formatting)));
        networkHandler.sendPacket(new TitleS2CPacket(new LiteralText(base)));
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
