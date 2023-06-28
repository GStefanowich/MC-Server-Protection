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

import net.theelm.sewingmachine.events.PlayerNameCallback;
import net.theelm.sewingmachine.interfaces.WhitelistedPlayer;
import net.theelm.sewingmachine.utilities.text.MessageUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created on Apr 27 2022 at 12:32 AM.
 * By greg in SewingMachineMod
 */
public final class WhitelistTree implements Comparable<WhitelistTree> {
    private final @NotNull List<WhitelistTree> invited = new ArrayList<>();
    private final @NotNull WhitelistedPlayer player;
    private UUID invitedBy;
    
    private WhitelistTree(@NotNull WhitelistedPlayer player) {
        this.player = player;
        this.invitedBy = player.getInvitedBy();
    }
    
    public @NotNull UUID getUUID() {
        return this.player.getUUID();
    }
    public @NotNull Text getName() {
        return Text.literal(this.player.getName());
    }
    public @NotNull Text getName(@NotNull MinecraftServer server) {
        return this.getName(server, false);
    }
    public @NotNull Text getName(@NotNull MinecraftServer server, boolean pretty) {
        Text name = pretty ? PlayerNameCallback.getName(server, this.getUUID()) : this.getName();
        if (name instanceof MutableText mutable && name.getStyle().isEmpty())
            return mutable.formatted(Formatting.GOLD);
        return name;
    }
    public @Nullable UUID getInvitedBy() {
        return this.invitedBy;
    }
    public void resetInvited() {
        this.invitedBy = null;
    }
    public boolean hasInvited() {
        return !this.invited.isEmpty();
    }
    
    public boolean addInvited(@NotNull WhitelistTree tree) {
        return this.invited.add(tree);
    }
    
    public int count() {
        return this.invited.size();
    }
    public int totalCount() {
        int total = this.count();
        for (WhitelistTree tree : this.invited)
            total += tree.totalCount();
        return total;
    }
    
    public MutableText print(@NotNull ServerCommandSource source, boolean pretty) {
        return this.print(source, pretty, 0);
    }
    public MutableText print(@NotNull ServerCommandSource source, boolean pretty, int depth) {
        String prefix = StringUtils.repeat(' ', depth);
        
        MutableText text = Text.literal("\n" + prefix)
            .append(MessageUtils.formatNumber(depth, Formatting.GRAY))
            .append("|  ")
            .append(this.getName(source.getServer(), pretty));
        
        if (this.hasInvited())
            text.append(" invited [")
                .append(MessageUtils.formatNumber(this.count(), Formatting.AQUA))
                .append("]..");
        
        // Sort the list of invites
        Collections.sort(this.invited);
        
        Iterator<WhitelistTree> iterator = this.invited.listIterator();
        for (int i = 0; iterator.hasNext(); i++) {
            WhitelistTree tree = iterator.next();
            
            if (tree.hasInvited() || i == 0)
                text.append(tree.print(source, pretty, depth + 1));
            else {
                text.append(" ")
                    .append(tree.getName(source.getServer(), pretty));
            }
            
            if (iterator.hasNext() && !tree.hasInvited())
                text.append(",");
        }
        
        return text;
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof WhitelistTree tree && Objects.equals(tree.getUUID(), this.getUUID());
    }
    
    @Override
    public int hashCode() {
        return this.player.getUUID()
            .hashCode();
    }
    
    @Override
    public int compareTo(@NotNull WhitelistTree tree) {
        return Integer.compare(this.totalCount(), tree.totalCount());
    }
}
