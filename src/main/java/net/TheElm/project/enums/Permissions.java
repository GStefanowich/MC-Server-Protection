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

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.TheElm.project.CoreMod;
import net.TheElm.project.commands.AdminCommands;
import net.TheElm.project.commands.WhereCommand;
import net.TheElm.project.permissions.InflictableNode;
import net.TheElm.project.permissions.OtherInflictableNode;
import net.TheElm.project.permissions.PermissionNode;
import net.TheElm.project.utilities.text.TextUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class Permissions {
    private static final Set<PermissionNode> PERMISSIONS = new HashSet<>();
    
    public static final @NotNull PermissionNode ALL_PERMISSIONS = addInflictable("*", "");
    
    /*
     * Self-Inflicting player permissions
     */
    
    public static final @NotNull PermissionNode INTERACT_WORLD = addPermission("interact.world", "Allows players to interact with the world");
    public static final @NotNull PermissionNode PLAYER_NICKNAME = addPermission("player.nick", "Allows players to set their own nickname");
    public static final @NotNull PermissionNode PLAYER_NICKNAME_COLOR = addPermission("player.nick.color", "Allows players to set their own nickname with a color");
    public static final @NotNull PermissionNode PLAYER_NICKNAME_COLOR_GRADIENT = addPermission("player.nick.color.gradient", "Allows players to set their own nickname using a color gradient");
    
    public static final @NotNull OtherInflictableNode PLAYER_GAMEMODE_SURVIVAL = addInflictableOther(GameMode.SURVIVAL);
    public static final @NotNull OtherInflictableNode PLAYER_GAMEMODE_CREATIVE = addInflictableOther(GameMode.CREATIVE);
    public static final @NotNull OtherInflictableNode PLAYER_GAMEMODE_ADVENTURE = addInflictableOther(GameMode.ADVENTURE);
    public static final @NotNull OtherInflictableNode PLAYER_GAMEMODE_SPECTATOR = addInflictableOther(GameMode.SPECTATOR);
    
    public static final @NotNull OtherInflictableNode PLAYER_GODMODE = addInflictableOther("player.cheat.god", "Allows god mode with /" + AdminCommands.GOD.toLowerCase(Locale.ROOT), "Allow using /" + AdminCommands.GOD.toLowerCase(Locale.ROOT) + " on other players");
    public static final @NotNull OtherInflictableNode PLAYER_FLY = addInflictableOther("player.cheat.fly", "Allows flying with /" + AdminCommands.FLIGHT.toLowerCase(Locale.ROOT), "Allows using /" + AdminCommands.FLIGHT.toLowerCase(Locale.ROOT) + " on other players");
    public static final @NotNull OtherInflictableNode PLAYER_HEAL = addInflictableOther("player.cheat.heal", "Allows healing self with /" + AdminCommands.HEAL.toLowerCase(Locale.ROOT), "Allows using /" + AdminCommands.HEAL.toLowerCase(Locale.ROOT) + " on other players");
    
    public static final @NotNull InflictableNode PLAYER_REPAIR = addInflictable("player.cheat.repair", "Allows players to use the /" + AdminCommands.REPAIR.toLowerCase(Locale.ROOT) + " command");
    
    public static final @NotNull PermissionNode LOCATE_PLAYERS = addPermission("player.cheat.locate", "Allows players to locate other players with /" + WhereCommand.NAME.toLowerCase(Locale.ROOT));
    
    /*
     * Moderator permissions
     */
    
    public static final @NotNull InflictableNode VANILLA_COMMAND_KICK = addInflictable("minecraft.command.kick", "Allows kicking players");
    public static final @NotNull PermissionNode VANILLA_COMMAND_KICK_EXEMPT = addPermission("minecraft.command.kick.exempt", "Exempts players from the /kick command");
    public static final @NotNull InflictableNode VANILLA_COMMAND_BAN = addInflictable("minecraft.command.ban", "Allows banning players");
    public static final @NotNull InflictableNode VANILLA_COMMAND_UNBAN = addInflictable("minecraft.command.unban", "Allows unbanning players");
    public static final @NotNull InflictableNode VANILLA_COMMAND_WHITELIST = addInflictable("minecraft.command.whitelist", "Allows adding players to the whitelist");
    
    public static final @NotNull InflictableNode CHAT_COMMAND_MUTE = addInflictable("minecraft.chat.mute", "Allows muting other players from talking in chat");
    public static final @NotNull PermissionNode CHAT_COMMAND_MUTE_EXEMPT = addPermission("minecraft.chat.mute.exempt", "Exempts player from being muted");
    
    public static @NotNull PermissionNode addPermission(@NotNull String node, @NotNull String description) {
        return storePermission(new PermissionNode(node, description));
    }
    public static @NotNull InflictableNode addInflictable(@NotNull String node, @NotNull String description) {
        return storePermission(new InflictableNode(node, description));
    }
    public static @NotNull OtherInflictableNode addInflictableOther(@NotNull String node, @NotNull String thisDescription, @NotNull String otherDescription) {
        return storePermission(new OtherInflictableNode(node, thisDescription, otherDescription));
    }
    public static @NotNull OtherInflictableNode addInflictableOther(@NotNull GameMode mode) {
        String name = mode.getName().toLowerCase();
        return addInflictableOther("player.gamemode." + name,
        "Allows players to update their gamemode to " + name + ".",
        "Allows players to update others gamemode to " + name + "."
        );
    }
    
    private static @NotNull <T extends PermissionNode> T storePermission(@NotNull T permission) {
        // Store the permission in the Set
        Permissions.PERMISSIONS.add(permission);
        
        // Warn if the description is empty
        if (permission.getDescription().isEmpty()) CoreMod.logDebug("Permission \"" + permission.getNode() + "\"'s description is empty.");
        
        // Store the secondary permission in the Set
        if (permission instanceof OtherInflictableNode)
            Permissions.storePermission(((OtherInflictableNode)permission).onOther());
        
        // Return the stored permission
        return permission;
    }
    public static @NotNull Stream<String> keys() {
        return Permissions.PERMISSIONS.stream().map(PermissionNode::getNode).sorted();
    }
    
    public static @NotNull CompletableFuture<Suggestions> getSuggestions(@NotNull SuggestionsBuilder builder) {
        Permissions.PERMISSIONS.forEach(node -> builder.suggest(node.isWildcard() ? TextUtils.quoteWrap(node.getNode()) : node.getNode(), new LiteralText(node.getDescription())));
        return builder.buildFuture();
    }
}
