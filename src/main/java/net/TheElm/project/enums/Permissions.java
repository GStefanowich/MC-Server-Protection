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

import net.TheElm.project.CoreMod;
import net.TheElm.project.permissions.InflictableNode;
import net.TheElm.project.permissions.OtherInflictableNode;
import net.TheElm.project.permissions.PermissionNode;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class Permissions {
    private static final Set<PermissionNode> PERMISSIONS = new HashSet<>();
    
    public static final PermissionNode ALL_PERMISSIONS = addInflictable("*", "");
    
    /*
     * Self-Inflicting player permissions
     */
    
    public static final PermissionNode INTERACT_WORLD = addPermission("interact.world", "");
    public static final PermissionNode PLAYER_NICKNAME = addPermission("player.nick", "");
    
    public static final OtherInflictableNode PLAYER_GAMEMODE_SURVIVAL = addInflictableOther(GameMode.SURVIVAL);
    public static final OtherInflictableNode PLAYER_GAMEMODE_CREATIVE = addInflictableOther(GameMode.CREATIVE);
    public static final OtherInflictableNode PLAYER_GAMEMODE_ADVENTURE = addInflictableOther(GameMode.ADVENTURE);
    public static final OtherInflictableNode PLAYER_GAMEMODE_SPECTATOR = addInflictableOther(GameMode.SPECTATOR);
    
    public static final OtherInflictableNode PLAYER_GODMODE = addInflictableOther("player.cheat.god", "");
    public static final OtherInflictableNode PLAYER_FLY = addInflictableOther("player.cheat.fly", "");
    public static final OtherInflictableNode PLAYER_HEAL = addInflictableOther("player.cheat.heal", "");
    
    public static final InflictableNode PLAYER_REPAIR = addInflictable("player.cheat.repair", "");
    
    public static final PermissionNode LOCATE_PLAYERS = addPermission("player.cheat.locate", "");
    
    /*
     * Moderator permissions
     */
    
    public static final InflictableNode VANILLA_COMMAND_KICK = addInflictable("minecraft.command.kick", "");
    public static final PermissionNode VANILLA_COMMAND_KICK_EXEMPT = addPermission("minecraft.command.kick.exempt", "");
    public static final InflictableNode VANILLA_COMMAND_BAN = addInflictable("minecraft.command.ban", "");
    public static final InflictableNode VANILLA_COMMAND_UNBAN = addInflictable("minecraft.command.unban", "");
    public static final InflictableNode VANILLA_COMMAND_WHITELIST = addInflictable("minecraft.command.whitelist", "");
    
    public static final InflictableNode CHAT_COMMAND_MUTE = addInflictable("minecraft.chat.mute", "");
    public static final PermissionNode CHAT_COMMAND_MUTE_EXEMPT = addPermission("minecraft.chat.mute.exempt", "");
    
    public static PermissionNode addPermission(@NotNull String node, @NotNull String description) {
        return storePermission(new PermissionNode(node, description));
    }
    public static InflictableNode addInflictable(@NotNull String node, @NotNull String description) {
        return storePermission(new InflictableNode(node, description));
    }
    public static OtherInflictableNode addInflictableOther(@NotNull String node, @NotNull String description) {
        return storePermission(new OtherInflictableNode(node, description));
    }
    public static OtherInflictableNode addInflictableOther(@NotNull GameMode mode) {
        String name = mode.getName().toLowerCase();
        return addInflictableOther("player.gamemode." + name, "Allows players to update their gamemode to " + name + ".");
    }
    
    private static <T extends PermissionNode> T storePermission(T permission) {
        Permissions.PERMISSIONS.add(permission);
        if (permission.getDescription().isEmpty()) CoreMod.logDebug("Permission \"" + permission.getNode() + "\"'s description is empty.");
        return permission;
    }
    
    public static Stream<String> keys() {
        return PERMISSIONS.stream().map(PermissionNode::getNode).sorted();
    }
}
