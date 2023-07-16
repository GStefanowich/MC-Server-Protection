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

package net.theelm.sewingmachine.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.commands.WhitelistTree;
import net.theelm.sewingmachine.commands.abstraction.SewCommand;
import net.theelm.sewingmachine.interfaces.WhitelistedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created on Jun 09 2023 at 2:17 AM.
 * By greg in sewingmachine
 */
public final class WhitelistTreeCommand implements SewCommand {
    @Override
    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher, @NotNull CommandRegistryAccess registry) {
        CommandNode<ServerCommandSource> whitelist = dispatcher.getRoot()
            .getChild("whitelist");
        if (whitelist == null) return;
        
        whitelist.addChild(CommandManager.literal("tree")
            .then(CommandManager.argument("pretty", BoolArgumentType.bool())
                .executes(this::prettyPrintTree)
            )
            .executes(this::printTree)
            .build()
        );
    }
    
    private int prettyPrintTree(@NotNull CommandContext<ServerCommandSource> context) {
        return this.printTree(context, BoolArgumentType.getBool(context, "pretty"));
    }
    private int printTree(@NotNull CommandContext<ServerCommandSource> context) {
        return this.printTree(context, false);
    }
    private int printTree(@NotNull CommandContext<ServerCommandSource> context, boolean pretty) {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        Collection<WhitelistEntry> whitelist = server.getPlayerManager()
            .getWhitelist()
            .values();
        
        List<WhitelistTree> main = new ArrayList<>();
        Map<UUID, WhitelistTree> all = whitelist.stream()
            .map(entry -> (WhitelistedPlayer)entry)
            .collect(Collectors.toMap(WhitelistedPlayer::getUUID, WhitelistTree::new));
        
        // Clean up circular invites
        cleanup:
        for (Map.Entry<UUID, WhitelistTree> entry : all.entrySet()) {
            final WhitelistTree self = entry.getValue();
            WhitelistTree tree = self;
            
            int i = 0;
            do {
                UUID inviteeId = tree.getInvitedBy();
                if (inviteeId == null)
                    continue cleanup;

                tree = all.get(inviteeId);
                if (tree == null || tree.getInvitedBy() == null)
                    continue cleanup;

                if (Objects.equals(self.getUUID(), tree.getInvitedBy())) {
                    self.resetInvited();
                    tree.resetInvited();
                }
            } while(i++ < 20);
        }
        
        // Add each entry into the output
        for (Map.Entry<UUID, WhitelistTree> entry : all.entrySet()) {
            WhitelistTree tree = entry.getValue();
            WhitelistTree invitee;

            UUID inviteeId = tree.getInvitedBy();
            if (inviteeId == null || ((invitee = all.get(inviteeId)) == null))
                main.add(tree);
            else
                invitee.addInvited(tree);
        }
        
        source.sendFeedback(
            () -> {
                MutableText text = Text.literal("Whitelist: ");
                if (!main.isEmpty()) {
                    Collections.sort(main);
    
                    for (WhitelistTree tree : main) {
                        text.append(tree.print(source, pretty));
                    }
                }
                
                return text;
            },
            false
        );
        return whitelist.size();
    }
}
