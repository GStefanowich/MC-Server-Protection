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

package net.TheElm.project.enums;

import net.TheElm.project.ServerCore;
import net.TheElm.project.interfaces.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum CompassDirections {
    SPAWN {
        @Override @NotNull
        public BlockPos getPos(ServerPlayerEntity player) {
            return ServerCore.getSpawn(player.getServerWorld());
        }
        @Override
        public CompassDirections getNext() { return WARP; }
    },
    WARP {
        @Override
        public @Nullable BlockPos getPos(ServerPlayerEntity player) {
            return ((PlayerData) player).getWarpPos();
        }
        @Override
        public CompassDirections getNext() { return BED; }
    },
    BED {
        @Override
        public @Nullable BlockPos getPos(ServerPlayerEntity player) {
            return SPAWN.getPos(player).equals(player.getSpawnPointPosition()) ? null : player.getSpawnPointPosition();
        }
        @Override
        public CompassDirections getNext() { return SPAWN; }
    };
    
    @Nullable
    public abstract BlockPos getPos(ServerPlayerEntity player);
    public abstract CompassDirections getNext();
    
    public Text text() {
        return new LiteralText(this.name()).formatted(Formatting.AQUA);
    }
}
