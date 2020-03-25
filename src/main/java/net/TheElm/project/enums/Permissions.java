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

import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class Permissions {
    private static final HashMap<String, String> PERMISSIONS = new HashMap<>();
    
    public static final String ALL_PERMISSIONS = addPermission("*", "");
    
    public static final String PLAYER_NICKNAME = addPermission("player.nick", "");
    
    public static final String PLAYER_GAMEMODE_SURVIVAL = addPermission(GameMode.SURVIVAL);
    public static final String PLAYER_GAMEMODE_CREATIVE = addPermission(GameMode.CREATIVE);
    public static final String PLAYER_GAMEMODE_ADVENTURE = addPermission(GameMode.ADVENTURE);
    public static final String PLAYER_GAMEMODE_SPECTATOR = addPermission(GameMode.SPECTATOR);
    
    public static final String VANILLA_COMMAND_KICK = addPermission("minecraft.command.kick", "");
    public static final String VANILLA_COMMAND_BAN = addPermission("minecraft.command.ban", "");
    public static final String VANILLA_COMMAND_UNBAN = addPermission("minecraft.command.unban", "");
    public static final String VANILLA_COMMAND_WHITELIST = addPermission("minecraft.command.whitelist", "");
    
    public static String addPermission(@NotNull String node, @NotNull String description) {
        PERMISSIONS.put( node, description );
        return node;
    }
    public static String addPermission(@NotNull GameMode mode) {
        String name = mode.getName().toLowerCase();
        return addPermission("player.gamemode." + name, "Allows players to set their own gamemode to " + name + ".");
    }
    
    public static Stream<String> keys() {
        return PERMISSIONS.keySet().stream().sorted();
    }
    public static Set<Map.Entry<String, String>> pairs() {
        return PERMISSIONS.entrySet();
    }
}
